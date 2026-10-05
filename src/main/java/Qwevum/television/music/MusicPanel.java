package Qwevum.television.music;

import Qwevum.television.music.lavaplayer.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The "MUSIC PANEL" message posted whenever a song starts, with buttons to control the player.
 * Only the newest panel in a server is live: posting a new one deletes the previous one.
 */
public class MusicPanel {
    public static final String BUTTON_PREFIX = "music:";
    public static final int VOLUME_STEP = 10;
    public static final int MAX_VOLUME = 150;

    private static final Consumer<Throwable> IGNORE = (error) -> {};

    private final GuildMusicManager musicManager;
    private final JDA jda;
    private final AtomicReference<PanelMessage> message = new AtomicReference<>();
    // Bumped on every show/finish so a slow send can't replace a newer panel
    private final AtomicLong version = new AtomicLong();
    private volatile long channelId;

    public MusicPanel(GuildMusicManager musicManager, JDA jda) {
        this.musicManager = musicManager;
        this.jda = jda;
    }

    /** Sets the text channel the panel is posted in, normally the one the last !play came from. */
    public void setChannel(long channelId) {
        this.channelId = channelId;
    }

    public boolean isCurrent(long messageId) {
        final PanelMessage current = this.message.get();
        return current != null && current.messageId() == messageId;
    }

    /** Posts a fresh panel for the track and deletes the old one. */
    public void show(AudioTrack track) {
        final GuildMessageChannel channel = channel(this.channelId);

        if (channel == null || !channel.canTalk()) {
            return;
        }

        final long showVersion = this.version.incrementAndGet();

        channel.sendMessageEmbeds(embed(track)).setComponents(buttons()).queue((sent) -> {
            if (showVersion != this.version.get()) {
                sent.delete().queue(null, IGNORE);
                return;
            }

            final PanelMessage old = this.message.getAndSet(new PanelMessage(channel.getIdLong(), sent.getIdLong()));
            final GuildMessageChannel oldChannel = old == null ? null : channel(old.channelId());

            if (oldChannel != null) {
                oldChannel.deleteMessageById(old.messageId()).queue(null, IGNORE);
            }
        });
    }

    /** Updates the live panel after something changed outside of its buttons, like !repeat. */
    public void refresh() {
        final AudioTrack track = this.musicManager.audioPlayer.getPlayingTrack();
        final PanelMessage current = this.message.get();
        final GuildMessageChannel channel = current == null ? null : channel(current.channelId());

        if (track == null || channel == null) {
            return;
        }

        channel.editMessageEmbedsById(current.messageId(), embed(track)).setComponents(buttons()).queue(null, IGNORE);
    }

    /** Posts a one-off message in the panel's channel, like a track failing to play. */
    public void notice(MessageEmbed embed) {
        final GuildMessageChannel channel = channel(this.channelId);

        if (channel != null && channel.canTalk()) {
            channel.sendMessageEmbeds(embed).queue(null, IGNORE);
        }
    }

    /** Turns the live panel into a plain message without buttons. */
    public void finish(String text) {
        this.version.incrementAndGet();

        final PanelMessage current = this.message.getAndSet(null);
        final GuildMessageChannel channel = current == null ? null : channel(current.channelId());

        if (channel == null) {
            return;
        }

        final MessageEmbed embed = MusicEmbeds.base()
                .setAuthor("MUSIC PANEL", null, this.jda.getSelfUser().getEffectiveAvatarUrl())
                .setDescription(text)
                .build();

        channel.editMessageEmbedsById(current.messageId(), embed).setComponents().queue(null, IGNORE);
    }

    public MessageEmbed embed(AudioTrack track) {
        final AudioPlayer player = this.musicManager.audioPlayer;
        final AudioTrackInfo info = track.getInfo();
        final Object requester = track.getUserData();

        final String status = String.format("Volume %d%% • Loop %s • AutoPlay %s • %d queued",
                player.getVolume(),
                this.musicManager.scheduler.repeating ? "On" : "Off",
                this.musicManager.scheduler.autoPlay ? "On" : "Off",
                this.musicManager.scheduler.queue.size());

        return MusicEmbeds.base()
                .setAuthor("MUSIC PANEL", null, this.jda.getSelfUser().getEffectiveAvatarUrl())
                .setDescription((player.isPaused() ? "⏸️ " : "💿 ") + MusicEmbeds.link(info))
                .addField("👤 Requested By", requester instanceof Long id ? "<@" + id + ">" : "`AutoPlay`", true)
                .addField("⏱️ Music Duration", MusicEmbeds.code(MusicEmbeds.duration(track)), true)
                .addField("🎙️ Music Author", MusicEmbeds.code(info.author), true)
                .setThumbnail(info.artworkUrl)
                .setFooter(status)
                .build();
    }

    public List<ActionRow> buttons() {
        final AudioPlayer player = this.musicManager.audioPlayer;
        final boolean paused = player.isPaused();

        return List.of(
                ActionRow.of(
                        button("down", "Down", "🔉", false).withDisabled(player.getVolume() <= 0),
                        button("back", "Back", "⏮️", false),
                        paused ? button("pause", "Resume", "▶️", true) : button("pause", "Pause", "⏸️", false),
                        button("skip", "Skip", "⏭️", false),
                        button("up", "Up", "🔊", false).withDisabled(player.getVolume() >= MAX_VOLUME)),
                ActionRow.of(
                        button("shuffle", "Shuffle", "🔀", false),
                        button("loop", "Loop", "🔁", this.musicManager.scheduler.repeating),
                        button("stop", "Stop", "⏹️", false),
                        button("autoplay", "AutoPlay", "♾️", this.musicManager.scheduler.autoPlay),
                        button("playlist", "Playlist", "📜", false)));
    }

    /** Buttons that are switched on are shown in green. */
    private static Button button(String action, String label, String emoji, boolean active) {
        return Button.of(active ? ButtonStyle.SUCCESS : ButtonStyle.SECONDARY,
                BUTTON_PREFIX + action, label, Emoji.fromUnicode(emoji));
    }

    private GuildMessageChannel channel(long channelId) {
        return channelId == 0 ? null : this.jda.getChannelById(GuildMessageChannel.class, channelId);
    }

    private record PanelMessage(long channelId, long messageId) {}
}
