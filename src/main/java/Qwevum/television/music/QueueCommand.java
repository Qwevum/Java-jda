package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.PlayerManager;
import Qwevum.television.util.TimeFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class QueueCommand implements ICommand {
    private static final int MAX_SHOWN = 20;

    @Override
    public void handle(CommandContext ctx) {
        final List<AudioTrack> trackList = new ArrayList<>(PlayerManager.getInstance().getMusicManager(ctx.getGuild()).scheduler.queue);

        if (trackList.isEmpty()) {
            ctx.getChannel().sendMessage("The queue is currently empty").queue();
            return;
        }

        final StringBuilder message = new StringBuilder("**Current Queue:**\n");
        int shown = 0;

        for (AudioTrack track : trackList) {
            final AudioTrackInfo info = track.getInfo();
            final String line = String.format("#%d `%s by %s` [`%s`]\n",
                    shown + 1, info.title, info.author, TimeFormat.format(track.getDuration()));

            // Stay well under Discord's 2000 character limit, leaving room for the "more" line
            if (shown == MAX_SHOWN || message.length() + line.length() > Message.MAX_CONTENT_LENGTH - 50) {
                break;
            }

            message.append(line);
            shown++;
        }

        if (trackList.size() > shown) {
            message.append("And `").append(trackList.size() - shown).append("` more...");
        }

        ctx.getChannel().sendMessage(message.toString()).queue();
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
