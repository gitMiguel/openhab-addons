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
package org.openhab.binding.vallox.internal.telegram;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.vallox.internal.ValloxBindingConstants;

/**
 * The {@link Telegram} class holds telegram state and data.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class Telegram {

    public enum TelegramState {

        // States for received telegram
        EMPTY("Empty telegram"),
        NOT_FOR_US("Telegram not for us"),
        OK("Telegram received OK"),
        ACK("ACK byte received"),
        CRC_ERROR("CRC checksum failed"),
        CORRUPTED("Telegram is corrupted"),
        NOT_DOMAIN("First byte is not domain byte"),
        SUSPEND("Stop all traffic"),
        RESUME("Resume normal use"),

        // States for telegrams to send
        POLL("Poll"),
        COMMAND("Command");

        public final String stateDetails;

        private TelegramState(String stateDetails) {
            this.stateDetails = stateDetails;
        }
    }

    public TelegramState state;
    public byte[] bytes = new byte[6];

    public Telegram() {
        this.state = TelegramState.EMPTY;
    }

    public Telegram(TelegramState state) {
        this.state = state;
    }

    public Telegram(TelegramState state, byte ackByte) {
        this.state = state;
        this.bytes[0] = ackByte;
    }

    public Telegram(TelegramState state, byte[] telegram) {
        this.state = state;
        this.bytes = telegram;
    }

    public Telegram(TelegramState state, byte sender, byte receiver, byte command, byte arg, byte checksum) {
        this.state = state;
        bytes[0] = ValloxBindingConstants.DOMAIN;
        bytes[1] = sender;
        bytes[2] = receiver;
        bytes[3] = command;
        bytes[4] = arg;
        bytes[5] = checksum;
    }

    /**
     * Helper method to get state details as string
     */
    public String stateDetails() {
        return state.stateDetails;
    }

    /**
     * Return telegram converted to string.
     */
    @Override
    public String toString() {
        return HexUtils.bytesToHex(bytes, "-");
    }
}
