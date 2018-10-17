/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.emby.internal.model;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;

/**
 * embyPlayState - part of the model for the json object received from the server
 *
 * @author Zachary Christiansen
 *
 */
public class embyPlayState {

    @SerializedName("PositionTicks")
    private BigDecimal positionTicks;
    @SerializedName("CanSeek")
    private boolean canSeek;
    @SerializedName("IsPaused")
    private boolean isPaused;
    @SerializedName("IsMuted")
    private boolean isMuted;
    @SerializedName("VolumeLevel")
    private Integer volumeLevel;
    @SerializedName("MediaSourceId")
    private String mediaSoureId;
    @SerializedName("PlayMethod")
    private String playMethod;
    @SerializedName("repeatMode")
    private String repeatMode;

    /**
     * @return the current position in the playback of the now playing item, can be compared to the total
     *         runtimeticks
     *         to get percentage played
     */
    BigDecimal getPositionTicks() {
        return positionTicks;
    }

    boolean getPaused() {
        return isPaused;
    }

    boolean getIsMuted() {
        return isMuted;
    }

    /**
     * @return the item id of the now playing item
     */
    String getMediaSourceID() {
        return mediaSoureId;

    }

}
