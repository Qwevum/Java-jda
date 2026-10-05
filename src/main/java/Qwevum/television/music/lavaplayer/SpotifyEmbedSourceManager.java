package Qwevum.television.music.lavaplayer;

import com.github.topi314.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrack;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrackResolver;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plays Spotify track, album and playlist links without Spotify API credentials, by reading the song names
 * from Spotify's public embed page. Each song is looked up on YouTube or SoundCloud when it starts playing.
 * Only used when SPOTIFY_CLIENT_ID/SECRET aren't set; with them, LavaSrc's full Spotify source is used.
 */
public class SpotifyEmbedSourceManager extends MirroringAudioSourceManager {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://open\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(?:embed/)?(track|album|playlist)/([A-Za-z0-9]+)");
    private static final Pattern DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.+?)</script>", Pattern.DOTALL);

    public SpotifyEmbedSourceManager(Function<Void, AudioPlayerManager> audioPlayerManager, MirroringAudioTrackResolver resolver) {
        super(audioPlayerManager, resolver);
    }

    @Override
    public String getSourceName() {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        final Matcher matcher = URL_PATTERN.matcher(reference.identifier);

        if (!matcher.find()) {
            return null;
        }

        final String type = matcher.group(1);
        final JsonBrowser entity;

        try {
            entity = fetchEntity(type, matcher.group(2));
        } catch (IOException e) {
            throw new FriendlyException("Couldn't load that from Spotify.", FriendlyException.Severity.SUSPICIOUS, e);
        }

        if (type.equals("track")) {
            final List<String> artists = new ArrayList<>();
            entity.get("artists").values().forEach((artist) -> artists.add(artist.get("name").text()));
            final JsonBrowser images = entity.get("visualIdentity").get("image");

            return track(entity.get("name").text(), String.join(", ", artists), entity.get("duration").asLong(0),
                    entity.get("id").text(), images.index(images.values().size() - 1).get("url").text());
        }

        final List<AudioTrack> tracks = new ArrayList<>();

        for (final JsonBrowser item : entity.get("trackList").values()) {
            final String uri = item.get("uri").text();

            // Local files and podcast episodes can't be looked up
            if (uri == null || !uri.startsWith("spotify:track:")) {
                continue;
            }

            tracks.add(track(item.get("title").text(), item.get("subtitle").text(), item.get("duration").asLong(0),
                    uri.substring("spotify:track:".length()), null));
        }

        if (tracks.isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new BasicAudioPlaylist(entity.get("name").text(), tracks, null, false);
    }

    private AudioTrack track(String title, String author, long duration, String id, String artworkUrl) {
        final AudioTrackInfo info = new AudioTrackInfo(title, author, duration, id, false,
                "https://open.spotify.com/track/" + id, artworkUrl, null);
        return new SpotifyEmbedTrack(info, this);
    }

    private JsonBrowser fetchEntity(String type, String id) throws IOException {
        final HttpGet request = new HttpGet("https://open.spotify.com/embed/" + type + "/" + id);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36");

        try (CloseableHttpResponse response = getHttpInterface().execute(request)) {
            HttpClientTools.assertSuccessWithContent(response, "Spotify embed page");

            final Matcher matcher = DATA_PATTERN.matcher(EntityUtils.toString(response.getEntity()));

            if (!matcher.find()) {
                throw new IOException("Spotify's embed page has no song data");
            }

            final JsonBrowser entity = JsonBrowser.parse(matcher.group(1)).get("props").get("pageProps").get("state").get("data").get("entity");

            if (entity.isNull()) {
                throw new IOException("Spotify's embed page has no song data");
            }

            return entity;
        }
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, java.io.DataOutput output) {
        // The track's info holds everything needed to rebuild it
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new SpotifyEmbedTrack(trackInfo, this);
    }

    /** A Spotify song that plays from whatever the resolver finds for it on YouTube or SoundCloud. */
    private static class SpotifyEmbedTrack extends MirroringAudioTrack {
        SpotifyEmbedTrack(AudioTrackInfo info, SpotifyEmbedSourceManager sourceManager) {
            super(info, null, null, null, null, null, false, sourceManager);
        }

        @Override
        protected InternalAudioTrack createAudioTrack(AudioTrackInfo info, SeekableInputStream stream) {
            return new Mp3AudioTrack(info, stream);
        }

        @Override
        protected AudioTrack makeShallowClone() {
            return new SpotifyEmbedTrack(this.trackInfo, (SpotifyEmbedSourceManager) this.sourceManager);
        }
    }
}
