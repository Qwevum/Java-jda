# 📺 Television Discord Bot

**Television** is a feature-rich Discord bot built in Java that provides entertainment, utility, and music functionality. With commands ranging from memes and jokes to music streaming via LavaPlayer, Television enhances your server with a little bit of everything.

---

## ✨ Features

### 🎮 Fun & Utility Commands
- `!help` – List all available commands
- `!joke` – Get a random joke
- `!meme` – Fetch a meme from Reddit
- `!ping` – Check bot latency
- `!minecraft` – Minecraft server status checker
- `!webhook` – Utility for working with Discord webhooks

### 🎵 Music Commands (LavaPlayer)
- `!join` – Joins the voice channel
- `!leave` – Leaves the voice channel
- `!play <url|search>` – Plays a track or searches YouTube
- `!queue` – Shows current music queue
- `!skip` – Skips the current song
- `!stop` – Stops playback
- `!repeat` – Toggles repeat mode
- `!nowplaying` – Shows the currently playing track

---

## 🔗 Invite the Bot

[Click here to add Television to your server](https://discord.com/oauth2/authorize?client_id=949168736189939792)

---

## 🚀 Getting Started on Your Own

### Requirements
- Java 17+
- Maven or Gradle
- Discord Bot Token
- A .env file formatted as such:<br>
<code>TOKEN=your-bot-token<br>
PREFIX=!<br>
OWNER_ID=your-discord-user-id<br>
WEBHOOK_URL=https://discordapp.com/api/webhooks/976714367141826580/Emcsep0AI7Xlj8PYPHuABa7cdcPGNjzo0DtEdkwFY6Sjq-Nm7iEhZI74Z0PdREcX7TyU
</code><br>
NEVER SHARE YOUR .env FILE ON ANY PUBLIC REPOSITORY, MAKE SURE YOU ADD IT TO YOUR .gitignore.


### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/television-bot.git
cd television-bot
