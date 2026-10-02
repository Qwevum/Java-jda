package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;

public class LeaveCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) {
        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());

        musicManager.stop("I left the voice channel.");

        ctx.getGuild().getAudioManager().closeAudioConnection();

        ctx.getChannel().sendMessageEmbeds(MusicEmbeds.info("👋 I have left the voice channel.")).queue();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp() {
        return "Leaves voice channel that the bot is in.";
    }
}
