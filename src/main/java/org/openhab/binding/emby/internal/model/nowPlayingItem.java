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
public class nowPlayingItem {

    @SerializedName("Name")
    private String name;
    @SerializedName("OriginalTitle")
    private String originalTitle;
    @SerializedName("Id")
    private String id;
    @SerializedName("RunTimeTicks")
    private BigDecimal runTimeTicks;
    @SerializedName("Overview")
    private String overview;
    @SerializedName("SeasonId")
    private String seasonId;

    String getName() {
        return name;

    }

    String getSeasonId() {

        return this.seasonId;
    }

    String getOriginalTitle() {
        return originalTitle;

    }

    /**
     * @return the media source id of the now playing item
     */
    String getId() {
        return this.id;

    }

    /**
     * @return the total runtime ticks of the currently playing item
     */
    BigDecimal getRunTimeTicks() {
        return runTimeTicks;

    }

    String getOverview() {
        return overview;

    }

}
