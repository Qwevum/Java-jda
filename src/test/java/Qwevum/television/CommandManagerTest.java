package Qwevum.television;

import Qwevum.television.command.ICommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {

    @Test
    void parsesCommandNameAndArgs() {
        CommandManager.ParsedCommand parsed = CommandManager.parse("!play never gonna give", "!");

        assertEquals("play", parsed.name());
        assertEquals(List.of("never", "gonna", "give"), parsed.args());
    }

    @Test
    void ignoresExtraWhitespace() {
        CommandManager.ParsedCommand parsed = CommandManager.parse("!PLAY   two  spaces ", "!");

        assertEquals("play", parsed.name());
        assertEquals(List.of("two", "spaces"), parsed.args());
    }

    @Test
    void prefixIsCaseInsensitive() {
        CommandManager.ParsedCommand parsed = CommandManager.parse("TV!ping", "tv!");

        assertEquals("ping", parsed.name());
        assertTrue(parsed.args().isEmpty());
    }

    @Test
    void returnsNullWithoutPrefixOrCommand() {
        assertNull(CommandManager.parse("hello !ping", "!"));
        assertNull(CommandManager.parse("!", "!"));
        assertNull(CommandManager.parse("!   ", "!"));
    }

    @Test
    void findsCommandsByNameAndAlias() {
        CommandManager manager = new CommandManager();

        ICommand help = manager.getCommand("help");
        assertNotNull(help);
        assertSame(help, manager.getCommand("CMDS"));
        assertNull(manager.getCommand("doesnotexist"));
    }
}
