package com.yeyamo_mobile.api.culture_service.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class YouTubeUrlValidatorTest {
 @Test void normalizesSupportedUrls(){String id="dQw4w9WgXcQ";String canonical="https://www.youtube-nocookie.com/embed/"+id;assertEquals(canonical,YouTubeUrlValidator.normalize("https://www.youtube.com/watch?v="+id+"&utm_source=test"));assertEquals(canonical,YouTubeUrlValidator.normalize("https://youtu.be/"+id));assertEquals(canonical,YouTubeUrlValidator.normalize(canonical));}
 @Test void rejectsUnsafeOrAmbiguousUrls(){assertInvalid("http://youtube.com/watch?v=dQw4w9WgXcQ");assertInvalid("https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ");assertInvalid("javascript:alert(1)");assertInvalid("https://user:secret@youtube.com/watch?v=dQw4w9WgXcQ");assertInvalid("https://youtube.com/watch?v=short");}
 private void assertInvalid(String value){CultureException error=assertThrows(CultureException.class,()->YouTubeUrlValidator.normalize(value));assertEquals("INVALID_YOUTUBE_URL",error.getCode());}
}
