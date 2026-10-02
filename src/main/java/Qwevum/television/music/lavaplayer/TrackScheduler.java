package Qwevum.television.music.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class TrackScheduler extends AudioEventAdapter {
    private static final int HISTORY_SIZE = 25;
    // Pressing Back later than this into a song restarts it instead of going to the previous one
    private static final long RESTART_THRESHOLD_MS = 5_000;

    public final AudioPlayer player;
    public final BlockingDeque<AudioTrack> queue = new LinkedBlockingDeque<>();
    public volatile boolean repeating = false;
    public volatile boolean autoPlay = false;

    private final GuildMusicManager musicManager;
    private final AudioPlayerManager playerManager;
    private final Deque<AudioTrack> history = new ArrayDeque<>();
    private volatile boolean goingBack = false;

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
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (this.goingBack) {
            this.goingBack = false;
        } else if (endReason != AudioTrackEndReason.LOAD_FAILED) {
            remember(track);
        }

        if (!endReason.mayStartNext) {
            return;
        }

        if (this.repeating) {
            this.player.startTrack(copy(track), false);
            return;
        }

        final AudioTrack next = this.queue.poll();

        if (next != null) {
            this.player.startTrack(next, false);
        } else {
            queueEnded(track);
        }
    }

    private void queueEnded(AudioTrack lastTrack) {
        if (this.autoPlay && lastTrack != null) {
            autoPlay(lastTrack);
        } else {
            this.musicManager.panel.finish("The queue has finished. Add more songs with `!play`.");
        }
    }

    /** Searches SoundCloud for more songs by the last track's artist and plays one that wasn't played recently. */
    private void autoPlay(AudioTrack lastTrack) {
        final String author = lastTrack.getInfo().author;

        this.playerManager.loadItemOrdered(this.musicManager, "scsearch:" + author, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                pick(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                pick(playlist.getTracks());
            }

            @Override
            public void noMatches() {
                pick(List.of());
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                pick(List.of());
            }

            private void pick(List<AudioTrack> results) {
                // The user may have turned AutoPlay off or queued something while the search ran
                if (!autoPlay || player.getPlayingTrack() != null) {
                    return;
                }

                final List<String> recent = recentIdentifiers();
                final AudioTrack choice = results.stream()
                        .filter((it) -> !recent.contains(it.getIdentifier()))
                        .findFirst()
                        .orElse(null);

                if (choice == null) {
                    musicManager.panel.finish("AutoPlay couldn't find anything new by `" + author + "`. The queue has finished.");
                    return;
                }

                // No requester marks it as picked by AutoPlay
                choice.setUserData(null);
                queue(choice);
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

    private List<String> recentIdentifiers() {
        final List<String> identifiers = new ArrayList<>();

        synchronized (this.history) {
            this.history.forEach((it) -> identifiers.add(it.getIdentifier()));
        }

        this.queue.forEach((it) -> identifiers.add(it.getIdentifier()));
        return identifiers;
    }

    /** A track can only be played once, so replaying needs a fresh copy that keeps the requester. */
    private static AudioTrack copy(AudioTrack track) {
        final AudioTrack clone = track.makeClone();
        clone.setUserData(track.getUserData());
        return clone;
    }
}
