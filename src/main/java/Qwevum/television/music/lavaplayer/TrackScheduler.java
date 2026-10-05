package Qwevum.television.music.lavaplayer;

import Qwevum.television.music.MusicEmbeds;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class TrackScheduler extends AudioEventAdapter {
    private static final int HISTORY_SIZE = 25;
    // Pressing Back later than this into a song restarts it instead of going to the previous one
    private static final long RESTART_THRESHOLD_MS = 5_000;
    // Sources whose songs can be looked up on YouTube when they won't play
    private static final Set<String> FALLBACK_SOURCES = Set.of("soundcloud", "spotify", "applemusic");

    public final AudioPlayer player;
    public final BlockingDeque<AudioTrack> queue = new LinkedBlockingDeque<>();
    public volatile boolean repeating = false;
    public volatile boolean autoPlay = false;

    private final GuildMusicManager musicManager;
    private final AudioPlayerManager playerManager;
    private final Deque<AudioTrack> history = new ArrayDeque<>();
    // Tracks that failed to play, so AutoPlay doesn't keep picking them
    private final Set<String> failed = new LinkedHashSet<>();
    private volatile boolean goingBack = false;
    // A SoundCloud track that couldn't be streamed while its YouTube replacement is being looked up
    private volatile AudioTrack fallingBack;

    public TrackScheduler(GuildMusicManager musicManager, AudioPlayerManager playerManager) {
        this.musicManager = musicManager;
        this.player = musicManager.audioPlayer;
        this.playerManager = playerManager;
    }

    /** Plays the track right away when nothing is playing, otherwise queues it. Returns true if it started. */
    public boolean queue(AudioTrack track) {
        if (this.player.startTrack(track, true)) {
            return true;
        }

        this.queue.offer(track);
        return false;
    }

    /** Moves on to the next queued track, or AutoPlay's pick when the queue is empty. */
    public void skip() {
        this.fallingBack = null;
        final AudioTrack current = this.player.getPlayingTrack();
        final AudioTrack next = this.queue.poll();

        if (next != null) {
            this.player.startTrack(next, false);
            return;
        }

        this.player.stopTrack();
        queueEnded(current);
    }

    /** Restarts the current track, or goes to the previous one near its start. Returns false if neither is possible. */
    public boolean back() {
        final AudioTrack current = this.player.getPlayingTrack();
        final AudioTrack previous;

        synchronized (this.history) {
            previous = current != null && current.getPosition() > RESTART_THRESHOLD_MS ? null : this.history.pollLast();
        }

        if (previous == null) {
            if (current == null || !current.isSeekable()) {
                return false;
            }

            current.setPosition(0);
            return true;
        }

        if (current != null) {
            this.queue.addFirst(copy(current));
            // Replacing the current track would otherwise put it straight back into the history
            this.goingBack = true;
        }

        this.player.startTrack(copy(previous), false);
        return true;
    }

    /** Shuffles the queue and returns how many tracks it holds. */
    public int shuffle() {
        final List<AudioTrack> tracks = new ArrayList<>();

        this.queue.drainTo(tracks);
        Collections.shuffle(tracks);
        this.queue.addAll(tracks);

        return tracks.size();
    }

    /** Stops playback and resets the queue, history and player settings. */
    public void clear() {
        this.fallingBack = null;
        this.repeating = false;
        this.autoPlay = false;
        this.queue.clear();
        this.player.setPaused(false);
        this.player.stopTrack();

        synchronized (this.history) {
            this.history.clear();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        this.musicManager.panel.show(track);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        synchronized (this.failed) {
            if (this.failed.add(track.getIdentifier()) && this.failed.size() > HISTORY_SIZE) {
                this.failed.remove(this.failed.iterator().next());
            }
        }

        // Many SoundCloud songs from record labels are only offered as encrypted streams, which LavaPlayer
        // can't play. Those (and Spotify/Apple Music songs played through SoundCloud) can come from YouTube.
        final String source = track.getSourceManager() == null ? "" : track.getSourceManager().getSourceName();
        final boolean canUseYoutube = FALLBACK_SOURCES.contains(source);

        if (canUseYoutube && PlayerManager.getInstance().isYoutubeReady()) {
            this.fallingBack = track;
            playFromYoutube(track);
            return;
        }

        this.musicManager.panel.notice(MusicEmbeds.error("I couldn't play " + MusicEmbeds.link(track.getInfo()) + ", so I skipped it."));
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (this.goingBack) {
            this.goingBack = false;
        } else if (endReason != AudioTrackEndReason.LOAD_FAILED) {
            remember(track);
        }

        // The YouTube search decides what plays next
        if (!endReason.mayStartNext || track == this.fallingBack) {
            return;
        }

        // Repeating a song that can't play would retry it forever
        if (this.repeating && endReason != AudioTrackEndReason.LOAD_FAILED) {
            this.player.startTrack(copy(track), false);
            return;
        }

        playNext(track);
    }

    private void playNext(AudioTrack lastTrack) {
        final AudioTrack next = this.queue.poll();

        if (next != null) {
            this.player.startTrack(next, false);
        } else {
            queueEnded(lastTrack);
        }
    }

    /** Looks for a song that couldn't be streamed on YouTube and plays that instead. */
    private void playFromYoutube(AudioTrack failedTrack) {
        final AudioTrackInfo info = failedTrack.getInfo();

        this.playerManager.loadItemOrdered(this.musicManager, "ytsearch:" + info.author + " " + info.title, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                play(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                play(playlist.getTracks().isEmpty() ? null : playlist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                play(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                play(null);
            }

            private void play(AudioTrack replacement) {
                // Skip or stop was used while the search ran
                if (fallingBack != failedTrack) {
                    return;
                }

                fallingBack = null;

                if (replacement == null) {
                    musicManager.panel.notice(MusicEmbeds.error("I couldn't stream " + MusicEmbeds.link(info)
                            + " or find it on YouTube, so I skipped it."));

                    if (player.getPlayingTrack() == null) {
                        playNext(failedTrack);
                    }
                    return;
                }

                replacement.setUserData(failedTrack.getUserData());
                musicManager.panel.notice(MusicEmbeds.info("I couldn't stream " + MusicEmbeds.link(info)
                        + " from its source, so I'm playing it from YouTube instead."));

                if (!player.startTrack(replacement, true)) {
                    queue.addFirst(replacement);
                }
            }
        });
    }

    private void queueEnded(AudioTrack lastTrack) {
        if (this.autoPlay && lastTrack != null) {
            autoPlay(lastTrack);
        } else {
            this.musicManager.panel.finish("The queue has finished. Add more songs with `!play`.");
        }
    }

    /**
     * Like YouTube Music's radio and Spotify's autoplay: plays something new from the YouTube Mix of the
     * last song someone asked for, which YouTube fills with similar songs from other artists too. Seeding
     * from that song instead of AutoPlay's own picks keeps the radio from drifting into other genres; once
     * its Mix runs out, the latest song is used. Without YouTube, plays more by the same artist from SoundCloud.
     */
    private void autoPlay(AudioTrack lastTrack) {
        if (!PlayerManager.getInstance().isYoutubeReady()) {
            load("scsearch:" + lastTrack.getInfo().author, (results) -> pickSimilar(results, lastTrack, this::autoPlayFinished));
            return;
        }

        final AudioTrack seed = lastRequested(lastTrack);

        loadMix(seed, (mix) -> pickSimilar(mix, lastTrack, () -> {
            if (seed == lastTrack) {
                autoPlayFinished();
            } else {
                loadMix(lastTrack, (latestMix) -> pickSimilar(latestMix, lastTrack, this::autoPlayFinished));
            }
        }));
    }

    /** The most recent song someone queued themselves, or the given one if AutoPlay picked all of them. */
    private AudioTrack lastRequested(AudioTrack lastTrack) {
        if (lastTrack.getUserData() instanceof Long) {
            return lastTrack;
        }

        synchronized (this.history) {
            final Iterator<AudioTrack> newestFirst = this.history.descendingIterator();

            while (newestFirst.hasNext()) {
                final AudioTrack track = newestFirst.next();

                if (track.getUserData() instanceof Long) {
                    return track;
                }
            }
        }

        return lastTrack;
    }

    /** Loads the YouTube Mix of a song, finding its YouTube copy first if it's from somewhere else. */
    private void loadMix(AudioTrack track, Consumer<List<AudioTrack>> results) {
        if (track.getSourceManager() != null && track.getSourceManager().getSourceName().equals("youtube")) {
            load(mixUrl(track.getIdentifier()), results);
            return;
        }

        final AudioTrackInfo info = track.getInfo();

        // A normal search finds the official video, whose Mix has better-known artists than YouTube Music's
        load("ytsearch:" + info.author + " " + info.title, (found) -> {
            if (found.isEmpty()) {
                results.accept(List.of());
            } else {
                load(mixUrl(found.get(0).getIdentifier()), results);
            }
        });
    }

    private static String mixUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId + "&list=RD" + videoId;
    }

    private void autoPlayFinished() {
        this.musicManager.panel.finish("AutoPlay couldn't find any new similar songs. The queue has finished.");
    }

    private void pickSimilar(List<AudioTrack> candidates, AudioTrack lastTrack, Runnable nothingFound) {
        // The user may have turned AutoPlay off or queued something while the search ran
        if (!this.autoPlay || this.player.getPlayingTrack() != null) {
            return;
        }

        final List<AudioTrackInfo> recent = new ArrayList<>();

        synchronized (this.history) {
            this.history.forEach((it) -> recent.add(it.getInfo()));
        }

        this.queue.forEach((it) -> recent.add(it.getInfo()));
        recent.add(lastTrack.getInfo());

        final List<String> failedIdentifiers;

        synchronized (this.failed) {
            failedIdentifiers = new ArrayList<>(this.failed);
        }

        final int index = AutoPlayPicker.pick(candidates.stream().map(AudioTrack::getInfo).toList(),
                lastTrack.getInfo(), recent, failedIdentifiers);

        if (index == -1) {
            nothingFound.run();
            return;
        }

        final AudioTrack choice = candidates.get(index);

        // No requester marks it as picked by AutoPlay
        choice.setUserData(null);
        queue(choice);
    }

    /** Loads a link or search and hands over its tracks, or an empty list if nothing was found. */
    private void load(String identifier, Consumer<List<AudioTrack>> results) {
        this.playerManager.loadItemOrdered(this.musicManager, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                results.accept(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                results.accept(playlist.getTracks());
            }

            @Override
            public void noMatches() {
                results.accept(List.of());
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                results.accept(List.of());
            }
        });
    }

    private void remember(AudioTrack track) {
        synchronized (this.history) {
            final AudioTrack last = this.history.peekLast();

            // Repeating a song shouldn't fill the history with copies of it
            if (last != null && last.getIdentifier().equals(track.getIdentifier())) {
                return;
            }

            this.history.addLast(track);

            if (this.history.size() > HISTORY_SIZE) {
                this.history.removeFirst();
            }
        }
    }

    /** A track can only be played once, so replaying needs a fresh copy that keeps the requester. */
    private static AudioTrack copy(AudioTrack track) {
        final AudioTrack clone = track.makeClone();
        clone.setUserData(track.getUserData());
        return clone;
    }
}
