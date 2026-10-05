package Qwevum.television;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.command.commands.*;
import Qwevum.television.music.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    private final List<ICommand> commands = new ArrayList<>();

    public CommandManager() {
        addCommand(new PingCommand());
        addCommand(new HelpCommand(this));
        addCommand(new MemeCommand());
        addCommand(new WebhookCommand());
        addCommand(new JokeCommand());
        addCommand(new JoinCommand());
        addCommand(new PlayCommand());
        addCommand(new StopCommand());
        addCommand(new NowPlayingCommand());
        addCommand(new SkipCommand());
        addCommand(new QueueCommand());
        addCommand(new RepeatCommand());
        addCommand(new LeaveCommand());
        addCommand(new EventWaiterCommand());
    }

    private void addCommand(ICommand cmd) {
        boolean nameFound = this.commands.stream().anyMatch((it) -> it.getName().equalsIgnoreCase(cmd.getName()));

        if (nameFound) {
            throw new IllegalArgumentException("A command with this name is already present.");
        }

        commands.add(cmd);
    }

    public List<ICommand> getCommands() {
        return commands;
    }

    public ICommand getCommand(String search) {
        String searchLower = search.toLowerCase();

        for (ICommand cmd : this.commands) {
            if (cmd.getName().equals(searchLower) || cmd.getAliases().contains(searchLower)) {
                return cmd;
            }
        }

        return null;
    }

    void handle(MessageReceivedEvent event, String prefix) {
        ParsedCommand parsed = parse(event.getMessage().getContentRaw(), prefix);

        if (parsed == null) {
            return;
        }

        ICommand cmd = this.getCommand(parsed.name());

        if (cmd == null) {
            return;
        }

        event.getChannel().sendTyping().queue();

        try {
            cmd.handle(new CommandContext(event, parsed.args()));
        } catch (Exception e) {
            LOGGER.error("Command {} failed", cmd.getName(), e);
            event.getChannel().sendMessage("Something went wrong while running that command.").queue();
        }
    }

    /**
     * Splits a message like "!play some song" into the command name and its arguments.
     * Returns null when the message doesn't start with the prefix or has no command name.
     */
    static ParsedCommand parse(String raw, String prefix) {
        if (!raw.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }

        String withoutPrefix = raw.substring(prefix.length()).trim();

        if (withoutPrefix.isEmpty()) {
            return null;
        }

        String[] split = withoutPrefix.split("\\s+");
        List<String> args = Arrays.asList(split).subList(1, split.length);

        return new ParsedCommand(split[0].toLowerCase(), args);
    }

    record ParsedCommand(String name, List<String> args) {}
}
