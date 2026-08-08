package com.yeyamo_mobile.api.media_service.application;
import java.util.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
import com.yeyamo_mobile.api.media_service.domain.model.MediaUsageType;

/**
 * Validates uploaded content against declared MIME type, magic bytes and size limits.
 *
 * <p>Extended to support AUDIO, DOCUMENT and CERTIFICATE in addition to
 * the original IMAGE and VIDEO types.  All size limits are configurable.</p>
 *
 * <p><b>Magic byte checks:</b>
 * <ul>
 *   <li>IMAGE — JPEG (ff d8), PNG (89 50 4e 47), WEBP (RIFF…WEBP)</li>
 *   <li>VIDEO — MP4/MOV (ftyp box), WebM (1a 45 df a3)</li>
 *   <li>AUDIO — MP3 (ff fb / ff f3 / ff f2 / ID3), OGG (4f 67 67 53), FLAC (66 4c 61 43), M4A (ftyp)</li>
 *   <li>DOCUMENT / CERTIFICATE — PDF (25 50 44 46)</li>
 * </ul>
 * </p>
 */
@Component
public class MediaContentPolicy{

 // ---- MIME sets -----------------------------------------------------------
 private static final Set<String> IMAGES  = Set.of("image/jpeg","image/png","image/webp");
 private static final Set<String> VIDEOS  = Set.of("video/mp4","video/webm","video/quicktime");
 private static final Set<String> AUDIOS  = Set.of("audio/mpeg","audio/mp3","audio/ogg","audio/flac","audio/mp4","audio/x-m4a","audio/wav","audio/x-wav");
 private static final Set<String> DOCS    = Set.of("application/pdf");

 // ---- Limits (bytes) ------------------------------------------------------
 private final long maxImage, maxVideo, maxAudio, maxDocument, maxCertificate;

 /** Maximum duration in seconds for AUDIO files (enforced from metadata). */
 private final long maxAudioDurationSeconds;

 @org.springframework.beans.factory.annotation.Autowired
 public MediaContentPolicy(
   @Value("${media.upload.max-image-bytes:10485760}")    long image,
   @Value("${media.upload.max-video-bytes:104857600}")   long video,
   @Value("${media.upload.max-audio-bytes:52428800}")    long audio,
   @Value("${media.upload.max-document-bytes:52428800}") long document,
   @Value("${media.upload.max-certificate-bytes:10485760}") long certificate,
   @Value("${media.upload.max-audio-duration-seconds:3600}") long maxAudioDurationSeconds){
  maxImage=image; maxVideo=video; maxAudio=audio;
  maxDocument=document; maxCertificate=certificate;
  this.maxAudioDurationSeconds=maxAudioDurationSeconds;
 }

 /** Backwards-compat constructor used by existing tests (2-arg form). */
 public MediaContentPolicy(long image,long video){
  this(image,video,52428800L,52428800L,10485760L,3600L);
 }

 // ---- Public API ----------------------------------------------------------

 /**
  * Validates the declared content-type + byte-level size before reading the body.
  */
 public void validateDeclaredSize(String contentType, long size){
  MediaType mt = typeFromMime(contentType);
  long max = limitFor(mt);
  if(size<=0||size>max)
   throw new MediaException("INVALID_MEDIA_SIZE","Media size is out of range for "+mt);
 }

 /**
  * Full validation: MIME → magic bytes → size → returns resolved {@link MediaType}.
  */
 public MediaType validate(String contentType, byte[] bytes){
  MediaType mt = typeFromMime(contentType);
  long max = limitFor(mt);
  if(bytes.length==0||bytes.length>max)
   throw new MediaException("INVALID_MEDIA_SIZE","Media size is out of range for "+mt);
  checkMagic(mt, contentType, bytes);
  return mt;
 }

 /**
  * Validates that the {@code usageType} is compatible with the resolved {@link MediaType}.
  * Returns the effective MediaType — PDF resolves to DOCUMENT by default but can be
  * coerced to CERTIFICATE when usageType is CERTIFICATE_DOCUMENT.
  */
 public MediaType resolveEffectiveType(MediaType resolvedType, MediaUsageType usageType){
  if(usageType==null) return resolvedType;
  MediaType required = usageType.allowedType();
  // Special case: PDF can serve as both DOCUMENT and CERTIFICATE
  boolean pdfGroup = (resolvedType==MediaType.DOCUMENT||resolvedType==MediaType.CERTIFICATE)
                  && (required==MediaType.DOCUMENT||required==MediaType.CERTIFICATE);
  if(!pdfGroup && required!=resolvedType)
   throw new MediaException("USAGE_TYPE_MISMATCH",
    "usageType "+usageType+" requires "+required+", got "+resolvedType);
  return required; // e.g. CERTIFICATE_DOCUMENT → CERTIFICATE
 }

 /**
  * @deprecated Use {@link #resolveEffectiveType(MediaType, MediaUsageType)} instead.
  */
 public void validateUsageType(MediaType resolvedType, MediaUsageType usageType){
  resolveEffectiveType(resolvedType, usageType);
 }

 /**
  * Validates DOCUMENT/CERTIFICATE MIME is in the whitelist.
  */
 public void validateDocumentMime(String contentType){
  String t = lower(contentType);
  if(!DOCS.contains(t))
   throw new MediaException("UNSUPPORTED_DOCUMENT_TYPE","Only PDF documents are accepted");
 }

 /** Returns the maximum allowed duration in seconds for audio uploads. */
 public long getMaxAudioDurationSeconds(){ return maxAudioDurationSeconds; }

 // ---- Internal helpers ----------------------------------------------------

 private MediaType typeFromMime(String contentType){
  String t = lower(contentType);
  if(IMAGES.contains(t)) return MediaType.IMAGE;
  if(VIDEOS.contains(t)) return MediaType.VIDEO;
  if(AUDIOS.contains(t)) return MediaType.AUDIO;
  // PDF is both DOCUMENT and CERTIFICATE depending on usageType.
  // Default to DOCUMENT; the upload path overrides via usageType.
  if(DOCS.contains(t))   return MediaType.DOCUMENT;
  throw new MediaException("UNSUPPORTED_MEDIA_TYPE",
   "Supported: JPEG, PNG, WEBP, MP4, WEBM, MOV, MP3, OGG, FLAC, M4A, WAV, PDF");
 }

 private long limitFor(MediaType mt){
  return switch(mt){
   case IMAGE       -> maxImage;
   case VIDEO       -> maxVideo;
   case AUDIO       -> maxAudio;
   case DOCUMENT    -> maxDocument;
   case CERTIFICATE -> maxCertificate;
  };
 }

 private void checkMagic(MediaType mt, String contentType, byte[] b){
  boolean ok = switch(mt){
   case IMAGE       -> imageMagic(b);
   case VIDEO       -> videoMagic(b);
   case AUDIO       -> audioMagic(b);
   case DOCUMENT,
        CERTIFICATE -> pdfMagic(b);
  };
  if(!ok) throw new MediaException("INVALID_MEDIA_CONTENT",
   "File signature does not match declared content-type "+contentType);
 }

 // Magic byte matchers
 private boolean imageMagic(byte[] b){
  return b.length>12&&(
   ((b[0]&255)==0xff&&(b[1]&255)==0xd8) ||              // JPEG
   ((b[0]&255)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G')|| // PNG
   (b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P')); // WEBP
 }

 private boolean videoMagic(byte[] b){
  return b.length>12&&(
   (b[4]=='f'&&b[5]=='t'&&b[6]=='y'&&b[7]=='p') ||       // MP4/MOV
   ((b[0]&255)==0x1a&&(b[1]&255)==0x45&&(b[2]&255)==0xdf&&(b[3]&255)==0xa3)); // WebM
 }

 private boolean audioMagic(byte[] b){
  if(b.length<4) return false;
  int b0=b[0]&255, b1=b[1]&255, b2=b[2]&255, b3=b[3]&255;
  // ID3 tag (MP3)
  if(b0=='I'&&b1=='D'&&b2=='3') return true;
  // MP3 sync word (ff fb / ff f3 / ff f2)
  if(b0==0xff&&(b1==0xfb||b1==0xf3||b1==0xf2)) return true;
  // OGG (4f 67 67 53 = OggS)
  if(b0=='O'&&b1=='g'&&b2=='g'&&b3=='S') return true;
  // FLAC (66 4c 61 43 = fLaC)
  if(b0=='f'&&b1=='L'&&b2=='a'&&b3=='C') return true;
  // RIFF WAV
  if(b0=='R'&&b1=='I'&&b2=='F'&&b3=='F'&&b.length>12&&b[8]=='W'&&b[9]=='A'&&b[10]=='V'&&b[11]=='E') return true;
  // M4A — ftyp box like MP4
  if(b.length>8&&b[4]=='f'&&b[5]=='t'&&b[6]=='y'&&b[7]=='p') return true;
  return false;
 }

 private boolean pdfMagic(byte[] b){
  // %PDF
  return b.length>4&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F';
 }

 private String lower(String s){ return s==null?"":s.toLowerCase(java.util.Locale.ROOT); }
}
