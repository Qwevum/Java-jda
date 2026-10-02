package Qwevum.television.command;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class CommandContext {
    private final MessageReceivedEvent event;
    private final List<String> args;

    public CommandContext(MessageReceivedEvent event, List<String> args) {
        this.event = event;
        this.args = args;
    }

    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    public List<String> getArgs() {
        return this.args;
    }

    public MessageChannel getChannel() {
        return this.event.getChannel();
    }

    public Guild getGuild() {
        return this.event.getGuild();
    }

    public Member getMember() {
        return this.event.getMember();
    }

    public Member getSelfMember() {
        return this.getGuild().getSelfMember();
    }

    public User getAuthor() {
        return this.event.getAuthor();
    }

    public Message getMessage() {
        return this.event.getMessage();
    }

    public JDA getJDA() {
        return this.event.getJDA();
    }
}
