package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class NowPlayingCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        final AudioTrack track = PlayerManager.getInstance().getMusicManager(ctx.getGuild()).audioPlayer.getPlayingTrack();

        if (track == null) {
            ctx.getChannel().sendMessage("There is no track playing currently").queue();
            return;
        }

        final AudioTrackInfo info = track.getInfo();

        ctx.getChannel().sendMessageFormat("Now playing `%s` by `%s` (Link: <%s>)", info.title, info.author, info.uri).queue();
    }

    @Override
    public String getName() {
        return "nowplaying";
    }

    @Override
    public String getHelp() {
        return "Shows the currently playing song.";
    }
}
