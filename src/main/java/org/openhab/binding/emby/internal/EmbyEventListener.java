/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.emby.internal;

import java.util.EventListener;

import org.eclipse.smarthome.core.library.types.RawType;
import org.openhab.binding.emby.internal.model.EmbyPlayStateModel;
import org.openhab.binding.emby.internal.protocol.EmbyConnection;

/**
 * Interface which has to be implemented by a class in order to get status
 * updates from a {@link EmbyConnection}
 *
 * @author Zachary Christiansen- Initiation Contribution
 */
public interface EmbyEventListener extends EventListener {
    public enum EmbyState {
        PLAY,
        PAUSE,
        END,
        STOP,
        REWIND,
        FASTFORWARD
    }

    public enum EmbyPlaylistState {
        ADD,
        ADDED,
        INSERT,
        REMOVE,
        REMOVED,
        CLEAR
    }

    void updateConnectionState(boolean connected);

    void updateScreenSaverState(boolean screenSaveActive);

    // void updatePlaylistState(EmbyPlaylistState playlistState);

    // void updateVolume(int volume);

    void updatePlayerState(EmbyState state);

    void updateMuted(boolean muted);

    void updateTitle(String title);

    void updateShowTitle(String title);

    // void updateAlbum(String album);

    // void updateArtistList(List<String> artistList);

    void updateMediaType(String mediaType);

    // void updateGenreList(List<String> genreList);

    // void updatePVRChannel(final String channel);

    void updateThumbnail(RawType thumbnail);

    void updateFanart(RawType fanart);

    void updateCurrentTime(long currentTime);

    void updateCurrentTimePercentage(double currentTimePercentage);

    void updateDuration(long duration);

    void updatePrimaryImageURL(String imageURL);

    public void handleEvent(EmbyPlayStateModel playstate, String hostname, int embyport);
}
