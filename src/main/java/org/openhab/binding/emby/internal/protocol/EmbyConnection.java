/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.emby.internal.protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.cache.ExpiringCacheMap;
import org.eclipse.smarthome.core.library.types.RawType;
import org.openhab.binding.emby.internal.EmbyEventListener;
import org.openhab.binding.emby.internal.EmbyEventListener.EmbyPlaylistState;
import org.openhab.binding.emby.internal.EmbyEventListener.EmbyState;
import org.openhab.binding.emby.internal.model.EmbyPlayStateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * EmbyConnection provides an API for accessing a Kodi device.
 *
 * @author Zachary Christiansen- Initial Contribution
 */

// This is copied from the kodi bnding. I have commendted out a bunch of that code.
// If i expand the binding i will want to add that funcunality back in
public class EmbyConnection implements EmbyClientSocketEventListener {

    private final Logger logger = LoggerFactory.getLogger(EmbyConnection.class);

    // 0 = STOP or -1 = PLAY BACKWARDS are valid as well, but we don't want use them for FAST FORWARD or REWIND speeds
    private static final List<Integer> SPEEDS = Arrays
            .asList(new Integer[] { -32, -16, -8, -4, -2, 1, 2, 4, 8, 16, 32 });
    private static final ExpiringCacheMap<String, RawType> IMAGE_CACHE = new ExpiringCacheMap<>(
            TimeUnit.MINUTES.toMillis(15));
    private static final ExpiringCacheMap<String, JsonElement> REQUEST_CACHE = new ExpiringCacheMap<>(
            TimeUnit.MINUTES.toMillis(5));

    private int refreshRate;
    private String hostname;
    private int embyport;
    private String deviceId;
    private URI wsUri;
    private URI imageUri;
    private String apiKey;
    private EmbyClientSocket socket;

    private int volume = 0;
    private EmbyState currentState = EmbyState.STOP;
    private EmbyPlaylistState currentPlaylistState = EmbyPlaylistState.CLEAR;

    private final EmbyEventListener listener;

    public EmbyConnection(EmbyEventListener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void onConnectionClosed() {
        listener.updateConnectionState(false);
    }

    @Override
    public synchronized void onConnectionOpened() {
        listener.updateConnectionState(true);

        socket.callMethodString("SessionsStart", "0," + Integer.toString(this.refreshRate));
    }

    public synchronized void connect(String hostname, int port, String deviceId, String apiKey,
            ScheduledExecutorService scheduler, int refreshRate) {
        this.hostname = hostname;
        this.deviceId = deviceId;
        this.apiKey = apiKey;
        this.embyport = port;
        this.refreshRate = refreshRate;
        try {
            close();

            wsUri = new URI("ws", null, hostname, port, null, "api_key=" + apiKey, null);
            socket = new EmbyClientSocket(this, wsUri, scheduler);
            checkConnection();
        } catch (URISyntaxException e) {
            logger.error("exception during constructing URI host={}, port={}", hostname, port, e);
        }
    }

    public synchronized void close() {
        if (socket != null && socket.isConnected()) {
            socket.close();
        }
    }

    private JsonArray getJsonArray(String[] values) {
        JsonArray result = new JsonArray();
        for (String param : values) {
            result.add(new JsonPrimitive(param));
        }
        return result;
    }

    private List<String> convertFromArrayToList(JsonArray data) {
        List<String> list = new ArrayList<>();
        for (JsonElement element : data) {
            list.add(element.getAsString());
        }
        return list;
    }

    public EmbyState getState() {
        return currentState;
    }

    private void updateState(EmbyState state) {
        // sometimes get a Pause immediately after a Stop - so just ignore
        if (currentState.equals(EmbyState.STOP) && state.equals(EmbyState.PAUSE)) {
            return;
        }
        listener.updatePlayerState(state);
        // if this is a Stop then clear everything else
        if (state == EmbyState.STOP) {
            // listener.updateAlbum("");
            listener.updateTitle("");
            listener.updateShowTitle("");
            // listener.updateArtistList(null);
            listener.updateMediaType("");
            // listener.updateGenreList(null);
            // listener.updatePVRChannel("");
            listener.updateThumbnail(null);
            listener.updateFanart(null);
            listener.updateCurrentTimePercentage(-1);
            listener.updateCurrentTime(-1);
            listener.updateDuration(-1);
        }
        // keep track of our current state
        currentState = state;
    }

    @Override
    public void handleEvent(EmbyPlayStateModel playstate) {
        // check the deviceId of this handler against the deviceId of the event to see if it matches
        if (playstate.compareDeviceId(deviceId)) {

            logger.debug("the deviceId for: {} matches the deviceId of the thing so we will update stringUrl",
                    playstate.getDeviceName());

            try {
                URI imageURI = playstate.getPrimaryImageURL(this.apiKey, this.hostname, this.embyport);

                if (imageURI.getHost().equals("NotPlaying")) {
                    updateState(EmbyState.END);
                    this.listener.updatePrimaryImageURL("");
                    this.listener.updateShowTitle("");
                } else {

                    this.listener.updatePrimaryImageURL(imageURI.toString());
                    this.listener.updateMuted(playstate.getEmbyMuteSate());
                    this.listener.updateShowTitle(playstate.getNowPlayingName());
                    this.listener.updateCurrentTime(playstate.getNowPlayingTime().longValue());
                    this.listener.updateDuration(playstate.getNowPlayingTotalTime().longValue());
                }

            } catch (URISyntaxException e) {
                logger.debug("unable to create image url for: {} due to exception: {} ", playstate.getDeviceName(),
                        e.toString());
            }

            if (playstate.getEmbyPlayStatePausedState()) {
                updateState(EmbyState.PAUSE);
            } else {
                updateState(EmbyState.PLAY);

            }

        }

        else {

            logger.debug("{} does not equal {} the event is for device named: {} ", playstate.getDeviceId(),
                    this.deviceId, playstate.getDeviceName());
        }

    }

    public boolean checkConnection() {
        if (!socket.isConnected()) {
            logger.debug("checkConnection: try to connect to Kodi {}", wsUri);
            try {
                socket.open();
                return socket.isConnected();
            } catch (Exception e) {
                logger.error("exception during connect to {}", wsUri, e);
                socket.close();
                return false;
            }
        } else {
            // Ping Kodi with the get version command. This prevents the idle
            // timeout on the websocket.
            return true;
        }
    }

    public String getConnectionName() {
        return wsUri.toString();
    }

}
