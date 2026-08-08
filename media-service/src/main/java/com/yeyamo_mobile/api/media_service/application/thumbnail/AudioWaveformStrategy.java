package com.yeyamo_mobile.api.media_service.application.thumbnail;

import com.yeyamo_mobile.api.media_service.domain.model.MediaType;

/**
 * "Thumbnail" strategy for AUDIO files.
 *
 * <p>Audio files do not have a visual thumbnail, but the processing pipeline
 * calls the same interface.  This implementation returns a constant 1×1 transparent
 * PNG as a sentinel so the rest of the upload flow (thumbnail key, ThumbnailStatus.READY)
 * is satisfied without special-casing AUDIO everywhere.</p>
 *
 * <p>In a full pipeline this would be replaced by a waveform image generator
 * (e.g. via ffmpeg {@code showwavespic} filter).  That is indicated by the
 * {@code waveformSupported()} flag.</p>
 */
public class AudioWaveformStrategy implements ThumbnailStrategy {

    /** Minimal 1×1 transparent PNG (68 bytes). */
    private static final byte[] SENTINEL_PNG = {
        (byte)0x89,'P','N','G','\r','\n',(byte)0x1a,'\n',
        0,0,0,13,'I','H','D','R',0,0,0,1,0,0,0,1,8,6,0,0,0,
        (byte)0x1f,(byte)0x15,(byte)0xc4,(byte)0x89,
        0,0,0,11,'I','D','A','T',8,(byte)0xd7,99,(byte)0xf8,(byte)0xcf,
        0,0,0,2,0,1,(byte)0xe2,33,(byte)0xbc,51,
        0,0,0,0,'I','E','N','D',(byte)0xae,'B',96,(byte)0x82
    };

    @Override
    public boolean supports(MediaType type) {
        return type == MediaType.AUDIO;
    }

    @Override
    public Thumbnail generate(byte[] original, String contentType) {
        // Sentinel thumbnail — no actual waveform extraction in MVP
        return new Thumbnail(SENTINEL_PNG, "image/png", 1, 1, null);
    }

    /** Whether a real waveform image can be generated in this environment. */
    public boolean waveformSupported() {
        return false; // set to true and override generate() when ffmpeg is available
    }
}
