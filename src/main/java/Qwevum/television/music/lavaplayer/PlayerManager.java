package Qwevum.television.music.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());

            return guildMusicManager;
        });
    }

    public void loadAndPlay(MessageChannel channel, Guild guild, String trackUrl) {
        final GuildMusicManager musicManager = this.getMusicManager(guild);

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                sendAdded(track);
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
                    AudioTrack track = playlist.getSelectedTrack() != null ? playlist.getSelectedTrack() : tracks.get(0);
                    musicManager.scheduler.queue(track);
                    sendAdded(track);
                    return;
                }

                channel.sendMessageFormat("Adding to queue: `%d` tracks from playlist `%s`", tracks.size(), playlist.getName()).queue();

                for (final AudioTrack track : tracks) {
                    musicManager.scheduler.queue(track);
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("I couldn't find anything for that.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                LOGGER.warn("Failed to load {}", trackUrl, exception);
                channel.sendMessage("I couldn't play that: " + exception.getMessage()).queue();
            }

            private void sendAdded(AudioTrack track) {
                channel.sendMessageFormat("Adding to queue: `%s` by `%s`", track.getInfo().title, track.getInfo().author).queue();
            }
        });
    }

    public void shutdown() {
        this.audioPlayerManager.shutdown();
    }
}
