package Qwevum.television.command;

import me.duncte123.botcommons.commands.ICommandContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class CommandContext implements ICommandContext {
    private final MessageReceivedEvent event;
    private final List<String> args;

    public CommandContext(MessageReceivedEvent event, List<String> args) {
        this.event = event;
        this.args = args;
    }

    @Override
    public Guild getGuild() {
        return ICommandContext.super.getGuild();
    }

    @Override
    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    /*@Override
    public Guild getGuild() {
        return this.getEvent().getGuild();
    }

    @Override
    public GuildMessageReceivedEvent getEvent() {
        return this.event;
    }*/

    public List<String> getArgs() {
        return this.args;
    }
}
