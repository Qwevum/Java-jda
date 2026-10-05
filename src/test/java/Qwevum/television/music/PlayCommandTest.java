package Qwevum.television.music;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayCommandTest {

    private static String identifier(String text) {
        return PlayCommand.toIdentifier(List.of(text.split(" ")));
    }

    @Test
    void linksAreKept() {
        assertEquals("https://open.spotify.com/track/4PTG3Z6ehGkBFwjybzWkR8",
                identifier("https://open.spotify.com/track/4PTG3Z6ehGkBFwjybzWkR8"));
    }

    @Test
    void plainSearchUsesSoundCloud() {
        assertEquals("scsearch:love forever", identifier("love forever"));
    }

    @Test
    void prefixPicksTheSource() {
        assertEquals("spsearch:never gonna give you up", identifier("sp: never gonna give you up"));
        assertEquals("amsearch:never gonna give you up", identifier("AM:never gonna give you up"));
        assertEquals("ytsearch:despacito", identifier("youtube: despacito"));
        assertEquals("scsearch:despacito", identifier("sc: despacito"));
    }

    @Test
    void wordsWithoutColonAreASearch() {
        assertEquals("scsearch:am i wrong", identifier("am i wrong"));
        assertEquals("scsearch:re: stacks", identifier("re: stacks"));
    }

    @Test
    void prefixWithoutSearchIsRejected() {
        assertNull(identifier("sp:"));
    }
}
