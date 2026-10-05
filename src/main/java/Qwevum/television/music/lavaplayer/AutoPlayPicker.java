package Qwevum.television.music.lavaplayer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chooses AutoPlay's next song from a list of similar songs (a YouTube Mix), the way streaming apps'
 * autoplay does: something new that fits the mood, preferably by a different artist than the last song.
 */
final class AutoPlayPicker {
    private static final long MIN_LENGTH_MS = 60_000;
    // Leaves out DJ sets, full albums and hour-long "mixes" that YouTube Mixes sometimes contain
    private static final long MAX_LENGTH_MS = 10 * 60_000;

    // "Max Styler and Nu Moda Records", "CamelPhat, Josh Gigante", "Kuuda - Topic" -> the first artist
    private static final Pattern ARTIST_SEPARATOR = Pattern.compile(
            "(?i)\\s*(,|&|\\band\\b|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\bwith\\b| - topic|vevo$)\\s*");
    // "(feat. Kuuda)", "[Official Video]"
    private static final Pattern BRACKETS = Pattern.compile("[(\\[][^)\\]]*[)\\]]");

    private AutoPlayPicker() {}

    /**
     * Returns the index of the song to play next, or -1 if none fits.
     *
     * @param recent songs played recently or already queued, which shouldn't come up again
     * @param failedIdentifiers songs that couldn't be played earlier
     */
    static int pick(List<AudioTrackInfo> candidates, AudioTrackInfo lastTrack, Collection<AudioTrackInfo> recent,
                    Collection<String> failedIdentifiers) {
        final String lastArtist = mainArtist(lastTrack.author);
        int sameArtist = -1;

        for (int i = 0; i < candidates.size(); i++) {
            final AudioTrackInfo candidate = candidates.get(i);

            if (!fits(candidate, recent, failedIdentifiers)) {
                continue;
            }

            if (!byArtist(candidate, lastArtist)) {
                return i;
            }

            if (sameArtist == -1) {
                sameArtist = i;
            }
        }

        // Only songs by the same artist are left, which is still better than stopping
        return sameArtist;
    }

    private static boolean fits(AudioTrackInfo candidate, Collection<AudioTrackInfo> recent, Collection<String> failedIdentifiers) {
        if (candidate.isStream || candidate.length < MIN_LENGTH_MS || candidate.length > MAX_LENGTH_MS
                || failedIdentifiers.contains(candidate.identifier)) {
            return false;
        }

        final String title = normalize(candidate.title);

        for (final AudioTrackInfo played : recent) {
            if (played.identifier.equals(candidate.identifier)) {
                return false;
            }

            // The same song from another upload, e.g. "Max Styler - Love Forever (Official Video)" vs "Love Forever"
            final String playedTitle = normalize(played.title);

            if (playedTitle.length() >= 4 && title.contains(playedTitle)) {
                return false;
            }
        }

        return true;
    }

    private static boolean byArtist(AudioTrackInfo candidate, String artist) {
        return !artist.isEmpty() && normalize(candidate.author + " " + candidate.title).contains(artist);
    }

    static String mainArtist(String author) {
        if (author == null) {
            return "";
        }

        return normalize(ARTIST_SEPARATOR.split(author.trim(), 2)[0]);
    }

    /** Lowercase letters and digits only, without anything in brackets. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return BRACKETS.matcher(text).replaceAll("").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
