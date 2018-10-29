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
import java.util.concurrent.ScheduledExecutorService;

import org.openhab.binding.emby.internal.EmbyBridgeListener;
import org.openhab.binding.emby.internal.model.EmbyPlayStateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EmbyConnection provides an API for accessing a Emby device.
 *
 * @author Zachary Christiansen- Initial Contribution
 */

public class EmbyConnection implements EmbyClientSocketEventListener {

    private final Logger logger = LoggerFactory.getLogger(EmbyConnection.class);

    private int refreshRate;
    private String hostname;
    private int embyport;
    private String deviceId;
    private URI wsUri;
    private String apiKey;
    private EmbyClientSocket socket;

    private final EmbyBridgeListener listener;

    public EmbyConnection(EmbyBridgeListener listener) {
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

    @Override
    public void handleEvent(EmbyPlayStateModel playstate) {
        logger.debug("Received event from EMBY server passing it to the bridge handler with hostname {} and port {}",
                hostname, embyport);
        this.listener.handleEvent(playstate, this.hostname, this.embyport);
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
            return true;
        }
    }

    public String getConnectionName() {
        return wsUri.toString();
    }

}
