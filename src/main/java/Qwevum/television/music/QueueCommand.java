package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.PlayerManager;

public class QueueCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) {
        ctx.getChannel().sendMessageEmbeds(MusicEmbeds.queue(PlayerManager.getInstance().getMusicManager(ctx.getGuild()))).queue();
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp() {
        return "shows the queued up songs.";
    }
}
