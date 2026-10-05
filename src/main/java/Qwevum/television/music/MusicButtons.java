package Qwevum.television.music;

import Qwevum.television.music.lavaplayer.GuildMusicManager;
import Qwevum.television.music.lavaplayer.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

/** Handles presses on the music panel's buttons. */
public final class MusicButtons {
    private MusicButtons() {}

    public static void handle(ButtonInteractionEvent event) {
        final String id = event.getComponentId();
        final Guild guild = event.getGuild();

        if (!id.startsWith(MusicPanel.BUTTON_PREFIX) || guild == null) {
            return;
        }

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
        final String action = id.substring(MusicPanel.BUTTON_PREFIX.length());

        // Old panels (from earlier songs or before a restart) lose their buttons
        if (!musicManager.panel.isCurrent(event.getMessageIdLong())) {
            event.editComponents().queue();
            event.getHook().sendMessageEmbeds(MusicEmbeds.error("This music panel is no longer active.")).setEphemeral(true).queue();
            return;
        }

        if (action.equals("playlist")) {
            event.replyEmbeds(MusicEmbeds.queue(musicManager)).setEphemeral(true).queue();
            return;
        }

        final String problem = VoiceChecks.sameChannelProblem(guild.getSelfMember(), event.getMember());

        if (problem != null) {
            reject(event, problem);
            return;
        }

        final AudioPlayer player = musicManager.audioPlayer;
        final AudioTrack current = player.getPlayingTrack();

        if (current == null) {
            reject(event, "There is no track playing currently.");
            return;
        }

        switch (action) {
            case "down" -> player.setVolume(Math.max(0, player.getVolume() - MusicPanel.VOLUME_STEP));
            case "up" -> player.setVolume(Math.min(MusicPanel.MAX_VOLUME, player.getVolume() + MusicPanel.VOLUME_STEP));
            case "pause" -> player.setPaused(!player.isPaused());
            case "loop" -> musicManager.scheduler.repeating = !musicManager.scheduler.repeating;
            case "autoplay" -> musicManager.scheduler.autoPlay = !musicManager.scheduler.autoPlay;
            case "shuffle" -> {
                if (musicManager.scheduler.shuffle() < 2) {
                    reject(event, "There need to be at least 2 songs in the queue to shuffle.");
                    return;
                }
            }
            case "back" -> {
                if (!musicManager.scheduler.back()) {
                    reject(event, "There is no earlier song to go back to.");
                    return;
                }

                // Going back to an earlier song posts a new panel by itself
                event.deferEdit().queue();
                return;
            }
            case "skip" -> {
                // The next song posts a new panel, or the panel closes when the queue is done
                event.deferEdit().queue();
                musicManager.scheduler.skip();
                return;
            }
            case "stop" -> {
                event.deferEdit().queue();
                musicManager.stop("⏹️ The music was stopped by " + event.getUser().getAsMention() + ".");
                return;
            }
            default -> {
                reject(event, "That button doesn't do anything.");
                return;
            }
        }

        event.editMessageEmbeds(musicManager.panel.embed(current)).setComponents(musicManager.panel.buttons()).queue();
    }

    private static void reject(ButtonInteractionEvent event, String message) {
        event.replyEmbeds(MusicEmbeds.error(message)).setEphemeral(true).queue();
    }
}
