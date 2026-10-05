package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class NowPlayingCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final AudioTrack track = musicManager.audioPlayer.getPlayingTrack();

        if (track == null) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("There is no track playing currently.")).queue();
            return;
        }

        // Brings the music panel back down to the bottom of this channel
        musicManager.panel.setChannel(ctx.getChannel().getIdLong());
        musicManager.panel.show(track);
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp() {
        return "Shows the music panel for the current song.";
    }
}
