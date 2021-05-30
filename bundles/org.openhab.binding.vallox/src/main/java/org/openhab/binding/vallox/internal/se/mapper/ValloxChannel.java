/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.vallox.internal.se.mapper;

import java.util.Collection;
import java.util.Collections;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Abstract base class for different channels.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public abstract class ValloxChannel {

    /**
     * Channel as byte
     */
    private byte variable;

    /**
     * The unit used with channel
     */
    private @Nullable Unit<?> unit;

    /**
     * Create new instance with zero byte
     */
    public ValloxChannel() {
        this(0x00, null);
    }

    /**
     * Create unitless channel with variable
     *
     * @param variable channels byte value
     */
    public ValloxChannel(int variable) {
        this((byte) variable, null);
    }

    /**
     * Create channel with specified unit and variable
     *
     * @param variable channels byte value
     * @param unit the unit used
     */
    public ValloxChannel(int variable, @Nullable Unit<?> unit) {
        this.variable = (byte) variable;
        this.unit = unit;
    }

    /**
     * Get channels byte value
     *
     * @return variable channel as byte
     */
    public byte getVariable() {
        return variable;
    }

    /**
     * Get channels unit
     *
     * @return unit
     */
    public @Nullable Unit<?> getUnit() {
        return unit;
    }

    /**
     * Get channels parent channel
     *
     * @return parent channel
     */
    public String getParentChannel() {
        return "";
    }

    /**
     * Convert channel value to state
     *
     * @param value the value to convert
     * @return value converted to state
     */
    public State convertToState(byte value) {
        return UnDefType.NULL;
    }

    /**
     * Convert channel state to byte.
     *
     * @param value the value to convert
     * @return the converted value
     */
    public byte convertFromState(byte value) {
        return value;
    }

    /**
     * Get collection of sub channels.
     *
     * @return list of sub channels
     */
    public Collection<String> getSubChannels() {
        return Collections.emptyList();
    }
}
