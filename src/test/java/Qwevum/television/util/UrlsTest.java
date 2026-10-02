package Qwevum.television.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlsTest {

    @Test
    void acceptsWebLinks() {
        assertTrue(Urls.isHttpUrl("https://soundcloud.com/artist/track"));
        assertTrue(Urls.isHttpUrl("http://example.com/song.mp3"));
    }

    @Test
    void treatsPlainTextAsSearch() {
        assertFalse(Urls.isHttpUrl("despacito"));
        assertFalse(Urls.isHttpUrl("never gonna give you up"));
        assertFalse(Urls.isHttpUrl("/etc/passwd"));
        assertFalse(Urls.isHttpUrl("file:///etc/passwd"));
    }
}
