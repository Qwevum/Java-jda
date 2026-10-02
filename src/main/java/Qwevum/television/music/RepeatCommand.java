package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;

public class RepeatCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        final boolean newRepeating = !musicManager.scheduler.repeating;

        musicManager.scheduler.repeating = newRepeating;

        musicManager.panel.refresh();

        ctx.getChannel().sendMessageEmbeds(MusicEmbeds.info(
                "🔁 The player has been set to **" + (newRepeating ? "repeating" : "not repeating") + "**")).queue();
    }

    @Override
    public String getName() {
        return "repeat";
    }

    @Override
    public String getHelp() {
        return "Loops the current song";
    }
}
