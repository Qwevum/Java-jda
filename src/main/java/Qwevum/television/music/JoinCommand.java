package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class JoinCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        final MessageChannel channel = ctx.getChannel();

        if (!DaveSupport.isAvailable()) {
            channel.sendMessage(DaveSupport.unavailableMessage()).queue();
            return;
        }

        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();

        if (selfVoiceState != null && selfVoiceState.inAudioChannel()) {
            channel.sendMessage("I'm already in a voice channel").queue();
            return;
        }

        final GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();

        if (memberVoiceState == null || !memberVoiceState.inAudioChannel()) {
            channel.sendMessage("You need to be in a voice channel for this command to work").queue();
            return;
        }

        final AudioChannel memberChannel = memberVoiceState.getChannel();

        if (!self.hasPermission(memberChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) {
            channel.sendMessage("I don't have permission to join and speak in that voice channel").queue();
            return;
        }

        ctx.getGuild().getAudioManager().openAudioConnection(memberChannel);
        channel.sendMessageFormat("Connecting to `🔊 %s`", memberChannel.getName()).queue();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getHelp() {
        return "Makes the bot join your voice channel";
    }
}
