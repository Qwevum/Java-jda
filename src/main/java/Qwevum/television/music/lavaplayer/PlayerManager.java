package Qwevum.television.music.lavaplayer;

import Qwevum.television.Config;
import Qwevum.television.music.MusicEmbeds;
import com.github.topi314.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topi314.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidVrWithThumbnail;
import dev.lavalink.youtube.clients.AndroidWithThumbnail;
import dev.lavalink.youtube.clients.ClientOptions;
import dev.lavalink.youtube.clients.IosWithThumbnail;
import dev.lavalink.youtube.clients.MusicWithThumbnail;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.WebWithThumbnail;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlayerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerManager.class);
    private static final PlayerManager INSTANCE = new PlayerManager();

    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    private final boolean youtubeReady;
    private final boolean spotifySearchReady;

    private PlayerManager() {
        final String youtubeOauth = Config.get("YOUTUBE_OAUTH", "");
        final String spotifyId = Config.get("SPOTIFY_CLIENT_ID", "");
        final String spotifySecret = Config.get("SPOTIFY_CLIENT_SECRET", "");

        // Each source is set up on its own, so one that fails to start doesn't take the others down with it
        this.youtubeReady = register("YouTube", () -> createYoutubeSource(youtubeOauth));

        // Spotify and Apple Music don't host audio themselves: their songs are played from a YouTube or
        // SoundCloud match. YouTube finds the exact recording by its ISRC code.
        final String[] providers = this.youtubeReady
                ? new String[] {"ytsearch:\"%ISRC%\"", "ytsearch:%QUERY%", "scsearch:%QUERY%"}
                : new String[] {"scsearch:%QUERY%"};

        register("Apple Music", () -> new AppleMusicSourceManager(null, "us", (unused) -> this.audioPlayerManager,
                new DefaultMirroringAudioTrackResolver(providers)));

        // Spotify search needs a (free) Spotify developer app. Without one, Spotify links still work by
        // reading the song names from Spotify's public embed pages.
        if (!spotifyId.isBlank() && !spotifySecret.isBlank()) {
            this.spotifySearchReady = register("Spotify", () -> new SpotifySourceManager(spotifyId, spotifySecret, null, "US",
                    (unused) -> this.audioPlayerManager, new DefaultMirroringAudioTrackResolver(providers)));
        } else {
            this.spotifySearchReady = false;
            register("Spotify links", () -> new SpotifyEmbedSourceManager((unused) -> this.audioPlayerManager,
                    new DefaultMirroringAudioTrackResolver(providers)));
        }

        // Only remote sources (SoundCloud, Bandcamp, direct links, ...). The local file source was
        // removed so people can't make the bot open files on the server. LavaPlayer's own YouTube
        // source is left out because it no longer works.
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager,
                com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
    }

    private boolean register(String name, Supplier<AudioSourceManager> source) {
        try {
            this.audioPlayerManager.registerSourceManager(source.get());
            return true;
        } catch (Exception | LinkageError e) {
            LOGGER.error("Couldn't set up {}, it won't be available", name, e);
            return false;
        }
    }

    /**
     * YouTube through youtube-source. Each client is used for what it currently does best: Android and iOS
     * for playback, Android VR for playlists and Web for searching (YouTube refuses the others' streams).
     * YOUTUBE_OAUTH (see .env.example) adds a signed-in TV client, for hosts YouTube blocks.
     */
    private static YoutubeAudioSourceManager createYoutubeSource(String oauth) {
        // YouTube rejects Android and iOS searches and playlist requests
        final ClientOptions playbackOnly = new ClientOptions();
        playbackOnly.setSearching(false);
        playbackOnly.setPlaylistLoading(false);

        final ClientOptions noPlayback = new ClientOptions();
        noPlayback.setPlayback(false);

        final List<Client> clients = new ArrayList<>(List.of(new MusicWithThumbnail()));

        // The TV client only plays songs when signed in
        if (!oauth.isBlank()) {
            clients.add(new Tv());
        }

        clients.addAll(List.of(new AndroidWithThumbnail(playbackOnly), new IosWithThumbnail(playbackOnly),
                new AndroidVrWithThumbnail(noPlayback), new WebWithThumbnail(noPlayback)));

        final YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(true, clients.toArray(Client[]::new));

        if (oauth.equalsIgnoreCase("new")) {
            // Logs a code to enter at google.com/device, then the refresh token to put in .env
            youtube.useOauth2(null, false);
        } else if (!oauth.isBlank()) {
            youtube.useOauth2(oauth, true);
        }

        return youtube;
    }

    public boolean isYoutubeReady() {
        return this.youtubeReady;
    }

    /** Whether "sp:" searches work, which needs Spotify API credentials (links work either way). */
    public boolean isSpotifySearchReady() {
        return this.spotifySearchReady;
    }

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

            return guildMusicManager;
        });
    }

    public void loadAndPlay(MessageChannel channel, Guild guild, String trackUrl, long requesterId) {
        final GuildMusicManager musicManager = this.getMusicManager(guild);

        // The music panel follows whichever channel music was last requested in
        musicManager.panel.setChannel(channel.getIdLong());

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                add(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                final List<AudioTrack> tracks = playlist.getTracks();

                if (tracks.isEmpty()) {
                    noMatches();
                    return;
                }

                // A search returns its results as a "playlist"; only queue the best match
                if (playlist.isSearchResult()) {
                    add(playlist.getSelectedTrack() != null ? playlist.getSelectedTrack() : tracks.get(0));
                    return;
                }

                channel.sendMessageEmbeds(MusicEmbeds.base()
                        .setAuthor("Added playlist to queue")
                        .setDescription(String.format("Added `%d` tracks from **%s**",
                                tracks.size(), MarkdownSanitizer.escape(playlist.getName())))
                        .build()).queue();

                for (final AudioTrack track : tracks) {
                    track.setUserData(requesterId);
                    musicManager.scheduler.queue(track);
                }

                musicManager.panel.refresh();
            }

            @Override
            public void noMatches() {
                channel.sendMessageEmbeds(MusicEmbeds.error("I couldn't find anything for that.")).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                LOGGER.warn("Failed to load {}", trackUrl, exception);
                channel.sendMessageEmbeds(MusicEmbeds.error("I couldn't play that: " + exception.getMessage())).queue();
            }

            private void add(AudioTrack track) {
                track.setUserData(requesterId);

                // A track that starts right away gets the music panel instead of an "added" message
                if (!musicManager.scheduler.queue(track)) {
                    channel.sendMessageEmbeds(MusicEmbeds.added(track, musicManager.scheduler.queue.size())).queue();
                    musicManager.panel.refresh();
                }
            }
        });
    }

    public void shutdown() {
        this.audioPlayerManager.shutdown();
    }
}
