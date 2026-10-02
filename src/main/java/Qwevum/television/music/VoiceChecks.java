package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

/** The "are we in the same voice channel?" checks shared by the music commands. */
final class VoiceChecks {
    private VoiceChecks() {}

    /**
     * Returns true when both the bot and the author are in the same voice channel.
     * Otherwise tells the author what's wrong and returns false.
     */
    static boolean inSameChannel(CommandContext ctx) {
        final MessageChannel channel = ctx.getChannel();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

        if (selfVoiceState == null || !selfVoiceState.inAudioChannel()) {
            channel.sendMessage("I need to be in a voice channel for this to work.").queue();
            return false;
        }

        final GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();

        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            channel.sendMessage("You need to be in a voice channel for this command to work.").queue();
            return false;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            channel.sendMessage("You need to be in the same voice channel as me for this to work.").queue();
            return false;
        }

        return true;
    }
}
