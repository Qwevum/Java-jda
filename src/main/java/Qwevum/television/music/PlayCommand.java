package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.PlayerManager;
import Qwevum.television.util.Urls;

public class PlayCommand implements ICommand {

    @Override
    public void handle(CommandContext ctx) {
        if (ctx.getArgs().isEmpty()) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("Correct usage is `!play <SoundCloud link or search>`")).queue();
            return;
        }

        if (!DaveSupport.isAvailable()) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error(DaveSupport.unavailableMessage())).queue();
            return;
        }

        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        String link = String.join(" ", ctx.getArgs());

        if (!Urls.isHttpUrl(link)) {
            link = "scsearch:" + link;
        }

        PlayerManager.getInstance().loadAndPlay(ctx.getChannel(), ctx.getGuild(), link, ctx.getAuthor().getIdLong());
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp() {
        return "Plays a song\n" +
                "Usage: `!play <SoundCloud link or search>`";
    }
}
