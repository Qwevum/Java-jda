package Qwevum.television.music.lavaplayer;

import Qwevum.television.music.MusicPanel;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;

public class GuildMusicManager {
    public final AudioPlayer audioPlayer;

    public final TrackScheduler scheduler;

    public final MusicPanel panel;

    private final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager manager, Guild guild) {
        this.audioPlayer = manager.createPlayer();
        this.panel = new MusicPanel(this, guild.getJDA());
        this.scheduler = new TrackScheduler(this, manager);
        this.audioPlayer.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    /** Stops the music, clears the queue and closes the music panel with the given message. */
    public void stop(String reason) {
        this.scheduler.clear();
        this.panel.finish(reason);
    }

    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }
}
