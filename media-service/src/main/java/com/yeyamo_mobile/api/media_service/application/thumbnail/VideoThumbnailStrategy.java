package com.yeyamo_mobile.api.media_service.application.thumbnail;
import java.nio.file.*;import java.time.Duration;import java.util.concurrent.TimeUnit;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
@Component
public class VideoThumbnailStrategy implements ThumbnailStrategy{
 private final String executable;private final Duration timeout;
 public VideoThumbnailStrategy(@Value("${media.video.ffmpeg-path:ffmpeg}")String executable,@Value("${media.video.ffmpeg-timeout-seconds:20}")long seconds){this.executable=executable;timeout=Duration.ofSeconds(seconds);}
 public boolean supports(MediaType type){return type==MediaType.VIDEO;}
 public Thumbnail generate(byte[] original,String contentType){Path input=null,output=null;try{input=Files.createTempFile("yeyamo-video-",".bin");output=Files.createTempFile("yeyamo-thumb-",".jpg");Files.write(input,original);
  Process process=new ProcessBuilder(executable,"-hide_banner","-loglevel","error","-y","-ss","00:00:01","-i",input.toString(),"-frames:v","1","-vf","scale=480:-2",output.toString()).redirectErrorStream(true).start();
  if(!process.waitFor(timeout.toSeconds(),TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("FFmpeg thumbnail timeout");}
  if(process.exitValue()!=0||Files.size(output)==0)throw new IllegalStateException("FFmpeg could not generate video thumbnail");
  return new Thumbnail(Files.readAllBytes(output),"image/jpeg",null,null,null);
 }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Video thumbnail interrupted",e);}
 catch(Exception e){throw new IllegalStateException("Video thumbnail generation unavailable",e);}
 finally{try{if(input!=null)Files.deleteIfExists(input);if(output!=null)Files.deleteIfExists(output);}catch(Exception ignored){}}}
}
