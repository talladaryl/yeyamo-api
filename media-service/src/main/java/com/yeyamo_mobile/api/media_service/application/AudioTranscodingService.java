package com.yeyamo_mobile.api.media_service.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Optional pipeline that transcodes audio to AAC/M4A format compatible with
 * mobile players (iOS, Android).
 *
 * <p>The service is <em>best-effort</em>: if FFmpeg is not installed or
 * transcoding fails the original file is kept and the failure is logged.
 * The upload never fails due to transcoding issues.</p>
 *
 * <p>Enable by setting {@code media.audio.transcode.enabled=true}.
 * Requires FFmpeg ≥ 4 on the server PATH (or configured via
 * {@code media.audio.ffmpeg-path}).</p>
 */
@Service
public class AudioTranscodingService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscodingService.class);

    private final boolean enabled;
    private final String ffmpegPath;
    private final long timeoutSeconds;

    public AudioTranscodingService(
            @Value("${media.audio.transcode.enabled:false}") boolean enabled,
            @Value("${media.audio.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${media.audio.ffmpeg-timeout-seconds:60}") long timeoutSeconds) {
        this.enabled = enabled;
        this.ffmpegPath = ffmpegPath;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Attempt to transcode the supplied audio bytes to AAC/M4A.
     *
     * @param original     raw bytes of the original audio file
     * @param contentType  declared MIME type of the original
     * @return transcoded bytes as AAC/M4A, or the original bytes if transcoding
     *         is disabled or failed
     */
    public TranscodeResult transcode(byte[] original, String contentType) {
        if (!enabled) {
            return new TranscodeResult(original, contentType, false, null);
        }

        Path input = null;
        Path output = null;
        try {
            input  = Files.createTempFile("yeyamo-audio-in-",  ".bin");
            output = Files.createTempFile("yeyamo-audio-out-", ".m4a");
            Files.write(input, original);

            Process process = new ProcessBuilder(
                    ffmpegPath,
                    "-hide_banner", "-loglevel", "error",
                    "-y",
                    "-i", input.toString(),
                    "-vn",               // no video stream
                    "-c:a", "aac",       // AAC codec
                    "-b:a", "128k",      // 128 kbps
                    "-movflags", "+faststart",
                    output.toString()
            ).redirectErrorStream(true).start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg transcoding timed out after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0 || Files.size(output) == 0) {
                throw new IllegalStateException("FFmpeg exited with code " + process.exitValue());
            }

            byte[] transcoded = Files.readAllBytes(output);
            log.info("Audio transcoded: {} bytes ({}) → {} bytes (audio/mp4)",
                    original.length, contentType, transcoded.length);

            return new TranscodeResult(transcoded, "audio/mp4", true, null);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Audio transcoding interrupted — using original: {}", e.getMessage());
            return new TranscodeResult(original, contentType, false, e.getMessage());
        } catch (Exception e) {
            log.warn("Audio transcoding failed — using original: {}", e.getMessage());
            return new TranscodeResult(original, contentType, false, e.getMessage());
        } finally {
            quietDelete(input);
            quietDelete(output);
        }
    }

    public boolean isEnabled() { return enabled; }

    // -------------------------------------------------------------------------

    private void quietDelete(Path p) {
        if (p != null) {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
        }
    }

    /**
     * Result of a transcoding attempt.
     *
     * @param bytes          final bytes to store (original or transcoded)
     * @param contentType    MIME type of the final bytes
     * @param transcoded     true if transcoding actually ran and succeeded
     * @param failureReason  non-null if transcoding failed (original bytes returned)
     */
    public record TranscodeResult(
            byte[] bytes,
            String contentType,
            boolean transcoded,
            String failureReason
    ) {}
}
