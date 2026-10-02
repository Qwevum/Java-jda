package Qwevum.television.music.lavaplayer;

import Qwevum.television.music.MusicEmbeds;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerManager.class);
    private static final PlayerManager INSTANCE = new PlayerManager();

    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    private PlayerManager() {
        // Only remote sources (SoundCloud, Bandcamp, direct links, ...). The local file source was
        // removed so people can't make the bot open files on the server.
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
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
