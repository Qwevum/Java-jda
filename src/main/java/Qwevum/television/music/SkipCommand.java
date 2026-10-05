package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class SkipCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());

        if (musicManager.audioPlayer.getPlayingTrack() == null) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("There is no track playing currently.")).queue();
            return;
        }

        final AudioTrack skipped = musicManager.audioPlayer.getPlayingTrack();

        musicManager.scheduler.skip();
        ctx.getChannel().sendMessageEmbeds(MusicEmbeds.info("⏭️ Skipped " + MusicEmbeds.link(skipped.getInfo()))).queue();
    }

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getHelp() {
        return "skips the current track.";
    }
}
