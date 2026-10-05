# 📺 Television Discord Bot

**Television** is a Discord bot built in Java with [JDA](https://github.com/discord-jda/JDA) that provides entertainment, utility, and music functionality. With commands ranging from memes and jokes to music streaming via [LavaPlayer](https://github.com/lavalink-devs/lavaplayer), Television enhances your server with a little bit of everything.

---

## ✨ Features

### 🎮 Fun & Utility Commands
- `!help` – List all available commands (`!help <command>` for details)
- `!joke` – Get a random joke
- `!meme` – Fetch a random meme
- `!ping` – Check bot latency
- `!webhook <message>` – Repost your message through a webhook with your name and avatar
- `!eventwaiter` – Small demo that waits for someone to react

### 🎵 Music Commands (LavaPlayer)
- `!join` – Joins your voice channel
- `!leave` – Leaves the voice channel
- `!play <link or search>` – Plays a YouTube, SoundCloud, Spotify or Apple Music link (songs, playlists and albums), or searches SoundCloud. Pick where to search with `yt:`, `sc:`, `sp:` or `am:`, e.g. `!play am: blinding lights`. Spotify and Apple Music songs play from their YouTube match, as do SoundCloud songs that SoundCloud won't stream (many record-label uploads). Other direct links like Bandcamp or `.mp3` URLs also work
- `!queue` – Shows the current music queue
- `!skip` – Skips the current song
- `!stop` – Stops playback and clears the queue
- `!repeat` – Toggles repeat mode
- `!nowplaying` – Moves the music panel for the current track to the bottom of the chat

Whenever a song starts, the bot posts a **music panel** embed showing who requested it, how long it is
and who made it, with buttons for volume down/up, back, pause/resume, skip, shuffle, loop, stop,
AutoPlay (when the queue runs out, keeps playing similar songs from the last song's YouTube Mix, like YouTube Music's radio or Spotify's autoplay) and the playlist. Only
people in the bot's voice channel can use the buttons.

> **Why Java 25?** Since March 2026 Discord only accepts end-to-end encrypted (DAVE) voice
> connections. The library that implements this ([JDAVE](https://github.com/MinnDevelopment/jdave))
> requires Java 25, so the whole bot is built for and runs on Java 25.

---

## 🔗 Invite the Bot

[Click here to add Television to your server](https://discord.com/oauth2/authorize?client_id=949168736189939792)

---

## 🚀 Running It Yourself

### Requirements
- Java 25
- A Discord bot token
- Gradle is **not** required: the included Gradle wrapper (`gradlew` / `gradlew.bat`) downloads it

### 1. Set up the bot in the Discord Developer Portal
In [your application](https://discord.com/developers/applications) → **Bot**, turn on these
**Privileged Gateway Intents**:
- **Server Members Intent** (welcome/goodbye messages)
- **Message Content Intent** (reading `!commands`)

Without these the bot will fail to connect with a "disallowed intents" error.

### 2. Create your `.env` file
Copy `.env.example` to `.env` and fill it in:

```
TOKEN=your-bot-token
PREFIX=!
OWNER_ID=your-discord-user-id
WEBHOOK_URL=https://discord.com/api/webhooks/...
```

**Never share your `.env` file or commit it.** It is already listed in `.gitignore`.
The values can also be provided as normal environment variables instead.

**Spotify search (optional):** Spotify links work out of the box, but `!play sp: <search>` needs a free
app from the [Spotify developer dashboard](https://developer.spotify.com/dashboard). Put its client ID
and secret in `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET`.

**YouTube sign-in (optional):** YouTube works without an account from home connections. Hosting
providers' servers are often blocked by YouTube ("Sign in to confirm you're not a bot"). If that happens,
link a spare Google account (YouTube may limit accounts used by bots): set `YOUTUBE_OAUTH=new`, start
the bot, enter the code it prints at <https://www.google.com/device>, then replace `new` with the
refresh token it prints.

### 3. Build
```bash
git clone https://github.com/Qwevum/Java-jda.git
cd Java-jda
./gradlew build        # Windows: gradlew.bat build
```

This runs the tests and creates `build/libs/Television.jar`, a single jar with everything included.

In IntelliJ you can also run the **Gradle → Tasks → build → build** task, or run `Television.main` directly.

### 4. Run
```bash
java -jar build/libs/Television.jar
```

Run it from the folder that contains your `.env` file.

---

## ☁️ Hosting on a Pterodactyl panel (e.g. Cybrancee)

1. Build the jar as above.
2. In the panel's **File Manager**, upload `build/libs/Television.jar` and your `.env` file.
3. On the **Startup** page:
   - Set the jar file name to `Television.jar`.
   - Pick the **Java 25** Docker Image. The bot won't start on older Java versions.
4. Restart the server from the **Console** page. You should see `... is ready in N servers`.

To update the bot later, rebuild and upload the new `Television.jar` over the old one, then restart.

**Memory:** the bot uses roughly 100–250 MB. If your plan is small (e.g. 384 MB) and the startup
command can be edited, adding `-Xmx256M` before `-jar` keeps Java comfortably inside the limit.

---

## 🧱 Project Structure

```
src/main/java/Qwevum/television/
├── Television.java          Entry point: logs in to Discord and registers the listener
├── Config.java              Reads settings from .env / environment variables
├── Listener.java            Handles Discord events (messages, members joining/leaving)
├── CommandManager.java      Registers commands, parses "!name args" and runs the right one
├── command/
│   ├── ICommand.java        Interface every command implements
│   ├── CommandContext.java  The message, channel, guild and args passed to a command
│   └── commands/            Fun & utility commands
├── music/                   Music commands
│   ├── DaveSupport.java     Sets up encrypted (DAVE) voice, which Discord requires
│   ├── VoiceChecks.java     Shared "are we in the same voice channel" checks
│   └── lavaplayer/          LavaPlayer glue: player per server, queue, audio sending
└── util/                    Small helpers (embeds, HTTP/JSON, time formatting, URLs)
```

---

## 🛠️ Built With
- [JDA 6](https://github.com/discord-jda/JDA) – Discord API wrapper
- [JDAVE](https://github.com/MinnDevelopment/jdave) – Discord voice encryption (DAVE)
- [LavaPlayer](https://github.com/lavalink-devs/lavaplayer) – Audio loading and playback
- [dotenv-java](https://github.com/cdimascio/dotenv-java) – `.env` file support
- [Logback](https://logback.qos.ch/) – Logging
- [Gradle](https://gradle.org/) with the [Shadow plugin](https://gradleup.com/shadow/) – Builds the single runnable jar
