/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.supportv7.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

/**
 * Abstraction of common playback operations of media items, such as play,
 * seek, etc. Used by PlaybackManager as a backend to handle actual playback
 * of media items.
 */
public abstract class Player {
    private static final String TAG = "SampleMediaRoutePlayer";
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_PLAY_PENDING = 1;
    protected static final int STATE_READY = 2;
    protected static final int STATE_PLAYING = 3;
    protected static final int STATE_PAUSED = 4;

    private static final long PLAYBACK_ACTIONS = PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY;

    protected Callback mCallback;
    protected MediaSessionCompat mMediaSession;
    protected MediaSessionCallback mSessionCallback;

    public abstract boolean isRemotePlayback();
    public abstract boolean isQueuingSupported();

    public abstract void connect(RouteInfo route);
    public abstract void release();

    // basic operations that are always supported
    public abstract void play(final PlaylistItem item);
    public abstract void seek(final PlaylistItem item);
    public abstract void getStatus(final PlaylistItem item, final boolean update);
    public abstract void pause();
    public abstract void resume();
    public abstract void stop();

    // advanced queuing (enqueue & remove) are only supported
    // if isQueuingSupported() returns true
    public abstract void enqueue(final PlaylistItem item);
    public abstract PlaylistItem remove(String iid);

    // track info for current media item
    public void updateTrackInfo() {}
    public String getDescription() { return ""; }
    public Bitmap getSnapshot() { return null; }

    // presentation display
    public void updatePresentation() {}

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public static Player create(Context context, RouteInfo route) {
        Player player;
        if (route != null && route.supportsControlCategory(
                MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
            player = new RemotePlayer(context);
        } else if (route != null) {
            player = new LocalPlayer.SurfaceViewPlayer(context);
        } else {
            player = new LocalPlayer.OverlayPlayer(context);
        }
        player.initMediaSession(context);
        player.connect(route);
        return player;
    }

    public MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    protected void updateMetadata() {
        MediaMetadataCompat.Builder bob = new MediaMetadataCompat.Builder();
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, getDescription());
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Subtitle of the thing");
        bob.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                "Description of the thing");
        bob.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, getSnapshot());
        mMediaSession.setMetadata(bob.build());
    }

    protected void publishState(int state) {
        PlaybackStateCompat.Builder bob = new PlaybackStateCompat.Builder();
        bob.setActions(PLAYBACK_ACTIONS);
        switch (state) {
            case STATE_PLAYING:
                bob.setState(PlaybackStateCompat.STATE_PLAYING, -1, 1);
                break;
            case STATE_READY:
            case STATE_PAUSED:
                bob.setState(PlaybackStateCompat.STATE_PAUSED, -1, 0);
                break;
            case STATE_IDLE:
                bob.setState(PlaybackStateCompat.STATE_STOPPED, -1, 0);
                break;
        }
        PlaybackStateCompat pbState = bob.build();
        Log.d(TAG, "Setting state to " + pbState);
        mMediaSession.setPlaybackState(pbState);
        if (state != STATE_IDLE) {
            mMediaSession.setActive(true);
        } else {
            mMediaSession.setActive(false);
        }
    }

    private void initMediaSession(Context context) {
        mSessionCallback = new MediaSessionCallback();
        mMediaSession = new MediaSessionCompat(context, "Support7Demos");
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(mSessionCallback);
        updateMetadata();
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            resume();
        }

        @Override
        public void onPause() {
            pause();
        }
    }

    public interface Callback {
        void onError();
        void onCompletion();
        void onPlaylistChanged();
        void onPlaylistReady();
    }
}