package Qwevum.television.music;

import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.util.TimeFormat;
import Qwevum.television.util.Urls;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.util.ArrayList;
import java.util.List;

/** The embeds every music command and the music panel reply with, so they all look alike. */
public final class MusicEmbeds {
    public static final int COLOR = 0x5865F2;
    private static final int ERROR_COLOR = 0xED4245;
    private static final int MAX_QUEUE_SHOWN = 20;

    private MusicEmbeds() {}

    public static EmbedBuilder base() {
        return new EmbedBuilder().setColor(COLOR);
    }

    public static MessageEmbed info(String text) {
        return base().setDescription(text).build();
    }

    public static MessageEmbed error(String text) {
        return new EmbedBuilder().setColor(ERROR_COLOR).setDescription("❌ " + text).build();
    }

    public static MessageEmbed added(AudioTrack track, int position) {
        final AudioTrackInfo info = track.getInfo();

        return base()
                .setAuthor("Added to queue")
                .setDescription(link(info))
                .addField("Music Author", code(info.author), true)
                .addField("Music Duration", duration(track), true)
                .addField("Position", code("#" + position), true)
                .setThumbnail(info.artworkUrl)
                .build();
    }

    public static MessageEmbed queue(GuildMusicManager musicManager) {
        final AudioTrack current = musicManager.audioPlayer.getPlayingTrack();
        final List<AudioTrack> tracks = new ArrayList<>(musicManager.scheduler.queue);
        final EmbedBuilder embed = base().setAuthor("Music Queue");

        if (current == null && tracks.isEmpty()) {
            return embed.setDescription("The queue is empty. Add a song with `!play`.").build();
        }

        final StringBuilder description = new StringBuilder();

        if (current != null) {
            description.append("**Now playing**\n").append(link(current.getInfo()))
                    .append(" [`").append(duration(current)).append("`]\n\n");
        }

        if (tracks.isEmpty()) {
            description.append("Nothing else is queued.");
        } else {
            description.append("**Up next**\n");
        }

        int shown = 0;

        for (AudioTrack track : tracks) {
            final String line = String.format("`%d.` %s [`%s`]\n", shown + 1, link(track.getInfo()), duration(track));

            // Leave room under Discord's description limit for the "more" line
            if (shown == MAX_QUEUE_SHOWN || description.length() + line.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - 50) {
                break;
            }

            description.append(line);
            shown++;
        }

        if (tracks.size() > shown) {
            description.append("And `").append(tracks.size() - shown).append("` more...");
        }

        long total = tracks.stream().filter((it) -> !it.getInfo().isStream).mapToLong(AudioTrack::getDuration).sum();

        return embed.setDescription(description)
                .setFooter(tracks.size() + " queued • " + TimeFormat.human(total) + " total")
                .build();
    }

    /** The title as a clickable link when the track has a web address. */
    public static String link(AudioTrackInfo info) {
        final String title = "**" + MarkdownSanitizer.escape(info.title).replace("[", "(").replace("]", ")") + "**";

        return info.uri != null && Urls.isHttpUrl(info.uri) ? "[" + title + "](" + info.uri + ")" : title;
    }

    public static String duration(AudioTrack track) {
        return track.getInfo().isStream ? "LIVE" : TimeFormat.human(track.getDuration());
    }

    /** Wraps text in an inline code block, which can't contain backticks. */
    public static String code(String text) {
        return text == null || text.isBlank() ? "`Unknown`" : "`" + text.replace('`', '\'') + "`";
    }
}
