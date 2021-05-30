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

import java.util.Arrays;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vallox.internal.se.ValloxSEConstants;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.State;

/**
 * Class for temperature channels.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class TemperatureChannel extends ValloxChannel {

    /**
     * Create new instance.
     *
     * @param variable channel as byte
     * @param unit the unit used with this channel
     */
    public TemperatureChannel(int variable, Unit<?> unit) {
        super(variable, unit);
    }

    @Override
    public State convertToState(byte value) {
        int index = Byte.toUnsignedInt(value);
        return new QuantityType<Temperature>(ValloxSEConstants.TEMPERATURE_MAPPING[index], SIUnits.CELSIUS);
    }

    @Override
    public byte convertFromState(byte state) {
        return (byte) Arrays.binarySearch(ValloxSEConstants.TEMPERATURE_MAPPING, state);
    }
}
