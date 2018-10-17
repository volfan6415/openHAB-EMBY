/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
\
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.emby.internal;

import static org.openhab.binding.emby.EmbyBindingConstants.*;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.measure.Unit;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.RewindFastforwardType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.emby.internal.protocol.EmbyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EmbyHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Zachary Christiansen - Initial contribution
 */

public class EmbyHandler extends BaseThingHandler implements EmbyEventListener {

    private final Logger logger = LoggerFactory.getLogger(EmbyHandler.class);

    private EmbyConfiguration config;
    private final EmbyConnection connection;
    private ScheduledFuture<?> connectionCheckerFuture;
    private String callbackIpAddress = null;

    public EmbyHandler(Thing thing, String hostAddress, String port) {
        super(thing);
        connection = new EmbyConnection(this);
        callbackIpAddress = hostAddress + ":" + port;
        logger.debug("The callback ip address is: {}", callbackIpAddress);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (CHANNEL_1.equals(channelUID.getId())) {
        // if (command instanceof RefreshType) {
        // TODO: handle data refresh
        // }

        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information:
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        // }
    }

    private int getIntConfigParameter(String key, int defaultValue) {
        Object obj = this.getConfig().get(key);
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            return Integer.parseInt(obj.toString());
        }
        return defaultValue;
    }

    @Override
    public void initialize() {
        config = getConfigAs(EmbyConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        // Example for background initialization:
        scheduler.execute(() -> {
            try {
                String host = getConfig().get(HOST_PARAMETER).toString();
                if (host == null || host.isEmpty()) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "No network address specified");
                } else {
                    connection.connect(host, getIntConfigParameter(WS_PORT_PARAMETER, 8096),
                            (String) this.getConfig().get(DEVICE_ID), (String) this.getConfig().get(API_KEY), scheduler,
                            getIntConfigParameter(REFRESH_PARAMETER, 10000));

                    connectionCheckerFuture = scheduler.scheduleWithFixedDelay(() -> {
                        if (connection.checkConnection()) {
                            // updateFavoriteChannelStateDescription();
                            // updatePVRChannelStateDescription(PVR_TV, CHANNEL_PVR_OPEN_TV);
                            // updatePVRChannelStateDescription(PVR_RADIO, CHANNEL_PVR_OPEN_RADIO);
                        } else {
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                    "Connection could not be established");
                        }
                    }, 1, getIntConfigParameter(REFRESH_PARAMETER, 10), TimeUnit.SECONDS);

                }
            } catch (Exception e) {
                logger.debug("error during opening connection: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
            }

        });

    }

    @Override
    public void updateConnectionState(boolean connected) {
        if (connected) {
            updateStatus(ThingStatus.ONLINE);
            try {

            } catch (Exception e) {
                logger.debug("error during reading version: {}", e.getMessage(), e);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No connection established");
        }
    }

    @Override
    public void updateScreenSaverState(boolean screenSaveActive) {
    }

    @Override
    public void updatePlayerState(EmbyState state) {
        switch (state) {
            case PLAY:
                updateState(CHANNEL_CONTROL, PlayPauseType.PLAY);
                updateState(CHANNEL_STOP, OnOffType.OFF);
                break;
            case PAUSE:
                updateState(CHANNEL_CONTROL, PlayPauseType.PAUSE);
                updateState(CHANNEL_STOP, OnOffType.OFF);
                break;
            case STOP:
            case END:
                updateState(CHANNEL_CONTROL, PlayPauseType.PAUSE);
                updateState(CHANNEL_STOP, OnOffType.ON);
                break;
            case FASTFORWARD:
                updateState(CHANNEL_CONTROL, RewindFastforwardType.FASTFORWARD);
                updateState(CHANNEL_STOP, OnOffType.OFF);
                break;
            case REWIND:
                updateState(CHANNEL_CONTROL, RewindFastforwardType.REWIND);
                updateState(CHANNEL_STOP, OnOffType.OFF);
                break;
        }
    }

    @Override
    public void updateMuted(boolean muted) {
        if (muted) {
            updateState(CHANNEL_MUTE, OnOffType.ON);
        } else {
            updateState(CHANNEL_MUTE, OnOffType.OFF);
        }
    }

    @Override
    public void updateTitle(String title) {
        updateState(CHANNEL_TITLE, createStringState(title));
    }

    @Override
    public void updatePrimaryImageURL(String imageUrl) {
        updateState(CHANNEL_IMAGEURL, createStringState(imageUrl));
    }

    @Override
    public void updateShowTitle(String title) {
        updateState(CHANNEL_SHOWTITLE, createStringState(title));
    }

    @Override
    public void updateMediaType(String mediaType) {
        updateState(CHANNEL_MEDIATYPE, createStringState(mediaType));
    }

    @Override
    public void updateThumbnail(RawType thumbnail) {
        updateState(CHANNEL_THUMBNAIL, createImageState(thumbnail));
    }

    @Override
    public void updateFanart(RawType fanart) {
        updateState(CHANNEL_FANART, createImageState(fanart));
    }

    @Override
    public void updateCurrentTime(long currentTime) {
        updateState(CHANNEL_CURRENTTIME, createQuantityState(currentTime, SmartHomeUnits.SECOND));
    }

    @Override
    public void updateCurrentTimePercentage(double currentTimePercentage) {
        updateState(CHANNEL_CURRENTTIMEPERCENTAGE, createQuantityState(currentTimePercentage, SmartHomeUnits.PERCENT));
    }

    @Override
    public void updateDuration(long duration) {
        updateState(CHANNEL_DURATION, createQuantityState(duration, SmartHomeUnits.SECOND));
    }

    /**
     * Wrap the given String in a new {@link StringType} or returns {@link UnDefType#UNDEF} if the String is empty.
     */
    private State createStringState(String string) {
        if (string == null || string.isEmpty()) {
            return UnDefType.UNDEF;
        } else {
            return new StringType(string);
        }
    }

    /**
     * Wrap the given list of Strings in a new {@link StringType} or returns {@link UnDefType#UNDEF} if the list of
     * Strings is empty.
     */
    private State createStringListState(List<String> list) {
        if (list == null || list.isEmpty()) {
            return UnDefType.UNDEF;
        } else {
            return createStringState(list.stream().collect(Collectors.joining(", ")));
        }
    }

    /**
     * Wrap the given RawType and return it as {@link State} or return {@link UnDefType#UNDEF} if the RawType is null.
     */
    private State createImageState(RawType image) {
        if (image == null) {
            return UnDefType.UNDEF;
        } else {
            return image;
        }
    }

    private State createQuantityState(Number value, Unit<?> unit) {
        return (value == null) ? UnDefType.UNDEF : new QuantityType<>(value, unit);
    }

}
