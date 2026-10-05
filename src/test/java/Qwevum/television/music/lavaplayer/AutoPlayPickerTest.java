package Qwevum.television.music.lavaplayer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoPlayPickerTest {
    private static final AudioTrackInfo LAST = song("Love Forever (feat. Kuuda)", "Max Styler", "sc1", 219_000);

    private static AudioTrackInfo song(String title, String author, String id, long length) {
        return new AudioTrackInfo(title, author, length, id, false, null, null, null);
    }

    private static int pick(List<AudioTrackInfo> candidates, List<AudioTrackInfo> recent, List<String> failed) {
        return AutoPlayPicker.pick(candidates, LAST, recent, failed);
    }

    @Test
    void prefersAnotherArtist() {
        final List<AudioTrackInfo> mix = List.of(
                song("Max Styler - I Know You Want To", "Max Styler", "a", 194_000),
                song("Max Styler, Vintage Culture - Freaky 1", "Max Styler and Vintage Culture", "b", 231_000),
                song("CamelPhat, Josh Gigante - So Good (feat. Kuuda)", "Josh Gigante", "c", 281_000));

        assertEquals(2, pick(mix, List.of(LAST), List.of()));
    }

    @Test
    void skipsTheSameSongFromAnotherUpload() {
        final List<AudioTrackInfo> mix = List.of(
                song("Max Styler - Love Forever (feat. Kuuda) [Official Video]", "Max Styler and Nu Moda Records", "yt1", 219_000),
                song("ILLENIUM & HAYLA - In My Arms (KREAM Remix)", "KREAM", "d", 218_000));

        assertEquals(1, pick(mix, List.of(LAST), List.of()));
    }

    @Test
    void skipsLiveSetsStreamsAndShortClips() {
        final List<AudioTrackInfo> mix = List.of(
                song("Tiësto Live Mexico City", "Tiësto", "e", 3_600_000),
                new AudioTrackInfo("24/7 house radio", "Radio", Long.MAX_VALUE, "f", true, null, null, null),
                song("Teaser", "Someone", "g", 30_000),
                song("Titanium (Future Rave Remix)", "David Guetta", "h", 197_000));

        assertEquals(3, pick(mix, List.of(LAST), List.of()));
    }

    @Test
    void skipsRecentAndFailedSongs() {
        final AudioTrackInfo played = song("Breathe (Remix)", "The Stickmen Project", "i", 125_000);
        final List<AudioTrackInfo> mix = List.of(played,
                song("So Good", "Josh Gigante", "j", 281_000),
                song("In My Arms", "KREAM", "k", 218_000));

        assertEquals(2, pick(mix, List.of(LAST, played), List.of("j")));
    }

    @Test
    void fallsBackToTheSameArtist() {
        final List<AudioTrackInfo> mix = List.of(song("Max Styler - You & Me", "Max Styler and Nu Moda Records", "l", 215_000));

        assertEquals(0, pick(mix, List.of(LAST), List.of()));
    }

    @Test
    void returnsMinusOneWhenNothingFits() {
        assertEquals(-1, pick(List.of(song("Love Forever", "Max Styler", "sc1", 219_000)), List.of(LAST), List.of()));
    }

    @Test
    void mainArtistIsTheFirstName() {
        assertEquals("maxstyler", AutoPlayPicker.mainArtist("Max Styler and Nu Moda Records"));
        assertEquals("camelphat", AutoPlayPicker.mainArtist("CamelPhat, Josh Gigante"));
        assertEquals("kuuda", AutoPlayPicker.mainArtist("Kuuda - Topic"));
    }
}
