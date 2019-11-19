/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.vallox.internal.mapper;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;

/**
 * Class for ON/OFF channels.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class BooleanChannel extends ValloxChannel {

    public byte bitMask;
    public String superChannel;

    public BooleanChannel(byte bitMask, String superChannel) {
        this.bitMask = bitMask;
        this.superChannel = superChannel;
    }

    @Override
    public String getSuperChannel() {
        return superChannel;
    }

    public byte getMask() {
        return bitMask;
    }

    @Override
    public byte getVariable() {
        return ChannelMapper.getVariable(superChannel);
    }

    @Override
    public State convertToState(Byte value) {
        boolean on = ((value & bitMask) != 0);
        State result = on ? OnOffType.ON : OnOffType.OFF;
        return result;
    }

    /**
     * Class for channel holding adjustment interval value
     *
     * @author Miika Jukka - Initial contributor
     */
    @SuppressWarnings("null")
    @NonNullByDefault
    public static class AdjustmentInterval extends BooleanChannel {

        public AdjustmentInterval(byte bitMask, String superChannel) {
            super(bitMask, superChannel);
        }

        @Override
        public byte getVariable() {
            return ChannelMapper.getVariable(superChannel);
        }

        @Override
        public String getSuperChannel() {
            return superChannel;
        }

        @Override
        public State convertToState(Byte value) {
            int result = (int) value & bitMask;
            return new DecimalType(result);
        }
    }
}
