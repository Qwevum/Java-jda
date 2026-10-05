package Qwevum.television.music;

import Qwevum.television.command.CommandContext;
import Qwevum.television.command.ICommand;
import Qwevum.television.music.lavaplayer.PlayerManager;
import Qwevum.television.util.Urls;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayCommand implements ICommand {
    private static final String USAGE = "`!play <link or search>` or `!play <yt:|sc:|sp:|am:> <search>`";
    // "sp: never gonna give you up" picks where to search; the colon keeps titles like "am i wrong" a normal search
    private static final Pattern SOURCE_PREFIX = Pattern.compile("^(\\w+):\\s*(.*)$", Pattern.DOTALL);

    private static final Map<String, String> SEARCH_PREFIXES = Map.of(
            "yt", "ytsearch:", "youtube", "ytsearch:",
            "sc", "scsearch:", "soundcloud", "scsearch:",
            "sp", "spsearch:", "spotify", "spsearch:",
            "am", "amsearch:", "apple", "amsearch:", "applemusic", "amsearch:");

    @Override
    public void handle(CommandContext ctx) {
        if (ctx.getArgs().isEmpty()) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("Correct usage is " + USAGE)).queue();
            return;
        }

        if (!DaveSupport.isAvailable()) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error(DaveSupport.unavailableMessage())).queue();
            return;
        }

        final PlayerManager playerManager = PlayerManager.getInstance();
        final String identifier = toIdentifier(ctx.getArgs());

        if (identifier == null) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("Tell me what to search for, like `!play sc: never gonna give you up`")).queue();
            return;
        }

        if (identifier.startsWith("spsearch:") && !playerManager.isSpotifySearchReady()) {
            ctx.getChannel().sendMessageEmbeds(MusicEmbeds.error("Spotify search isn't set up on this bot, but Spotify links work. "
                    + "The bot owner can add `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` to enable it (see the README).")).queue();
            return;
        }

        if (!VoiceChecks.inSameChannel(ctx)) {
            return;
        }

        playerManager.loadAndPlay(ctx.getChannel(), ctx.getGuild(), identifier, ctx.getAuthor().getIdLong());
    }

    /**
     * Turns the command's words into something LavaPlayer can load: links are kept as they are, and searches
     * go to the source named before a colon, or by default SoundCloud.
     * Returns null when only a source was given without anything to search for.
     */
    static String toIdentifier(List<String> args) {
        final String text = String.join(" ", args).trim();

        if (Urls.isHttpUrl(text)) {
            return text;
        }

        final Matcher matcher = SOURCE_PREFIX.matcher(text);
        final String prefix = matcher.matches() ? SEARCH_PREFIXES.get(matcher.group(1).toLowerCase(Locale.ROOT)) : null;

        if (prefix != null) {
            final String query = matcher.group(2).trim();
            return query.isEmpty() ? null : prefix + query;
        }

        return "scsearch:" + text;
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getHelp() {
        return "Plays a song from YouTube, SoundCloud, Spotify or Apple Music\n" +
                "Usage: " + USAGE + "\n" +
                "`yt:` YouTube, `sc:` SoundCloud, `sp:` Spotify, `am:` Apple Music";
    }
}
