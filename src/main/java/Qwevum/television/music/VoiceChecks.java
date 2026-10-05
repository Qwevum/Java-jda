package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;

/** The "are we in the same voice channel?" checks shared by the music commands and panel buttons. */
final class VoiceChecks {
    private VoiceChecks() {}

    /**
     * Returns true when both the bot and the author are in the same voice channel.
     * Otherwise tells the author what's wrong and returns false.
     */
    static boolean inSameChannel(CommandContext ctx) {
        final String problem = sameChannelProblem(ctx.getSelfMember(), ctx.getMember());

        if (problem != null) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error(problem)).queue();
            return false;
        }

        return true;
    }

    /** Explains why the member can't control the music, or returns null when they can. */
    static String sameChannelProblem(Member self, Member member) {
        final GuildVoiceState selfVoiceState = self.getVoiceState();

        if (selfVoiceState == null || !selfVoiceState.inAudioChannel()) {
            return "I need to be in a voice channel for this to work.";
        }

        final GuildVoiceState memberVoiceState = member == null ? null : member.getVoiceState();

        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            return "You need to be in a voice channel for this command to work.";
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            return "You need to be in the same voice channel as me for this to work.";
        }

        return null;
    }
}
