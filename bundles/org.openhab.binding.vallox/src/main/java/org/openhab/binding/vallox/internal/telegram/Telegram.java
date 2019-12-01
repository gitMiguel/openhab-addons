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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.vallox.internal.ValloxBindingConstants;
import org.openhab.binding.vallox.internal.cache.ValloxExpiringCacheMap;
import org.openhab.binding.vallox.internal.mapper.ChannelMapper;
import org.openhab.binding.vallox.internal.mapper.MultipleValueChannel;
import org.openhab.binding.vallox.internal.mapper.TemperatureChannel;
import org.openhab.binding.vallox.internal.mapper.ValloxChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Telegram} class holds telegram state, data and parse methods.
 *
 * @author Hauke Fuhrmann - Initial contribution
 * @author Miika Jukka - Rewrite
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

    private final Logger logger = LoggerFactory.getLogger(Telegram.class);
    private static Map<String, State> channelsToUpdate = new HashMap<>();
    public TelegramState state;
    public byte[] bytes = new byte[6];

    /**
     * Create new instance
     */
    public Telegram(TelegramState state) {
        this.state = state;
    }

    public Telegram(TelegramState state, byte singleByte) {
        this.state = state;
        this.bytes[0] = singleByte;
    }

    public Telegram(TelegramState state, byte[] telegram) {
        this.state = state;
        this.bytes = telegram;
    }

    public Telegram(TelegramState state, byte sender, byte receiver, byte variable, byte value, byte checksum) {
        this.state = state;
        bytes[0] = ValloxBindingConstants.DOMAIN;
        bytes[1] = sender;
        bytes[2] = receiver;
        bytes[3] = variable;
        bytes[4] = value;
        bytes[5] = checksum;
    }

    /**
     * Get sender of this telegram
     *
     * @return sender
     */
    public Byte getSender() {
        return bytes[1];
    }

    /**
     * Get receiver of this telegram
     *
     * @return receiver
     */
    public Byte getReceiver() {
        return bytes[2];
    }

    /**
     * Get variable of this telegram
     *
     * @return variable
     */
    public Byte getVariable() {
        return bytes[3];
    }

    /**
     * Get value of this telegram
     *
     * @return value
     */
    public Byte getValue() {
        return bytes[4];
    }

    /**
     * Get checksum of this telegram
     *
     * @return checksum
     */
    public Byte getCheksum() {
        return bytes[5];
    }

    /**
     * Get state details as string
     */
    public String stateDetails() {
        return state.stateDetails;
    }

    /**
     * Get telegram as string.
     */
    @Override
    public String toString() {
        return HexUtils.bytesToHex(bytes, "-");
    }

    /**
     * Process telegram and return a map of channels to update
     */
    public Map<String, State> parse(String channelID, ValloxExpiringCacheMap cache) {
        channelsToUpdate.clear();
        ValloxChannel valloxChannel = ChannelMapper.getValloxChannel(channelID);
        if (valloxChannel instanceof MultipleValueChannel) {
            Collection<String> subchannels = valloxChannel.getSubChannels();
            for (String channel : subchannels) {
                ValloxChannel vc = ChannelMapper.getValloxChannel(channel);
                State state = vc.convertToState(getValue());
                channelsToUpdate.put(channel, state);
            }
        } else if (valloxChannel instanceof TemperatureChannel) {
            State state = valloxChannel.convertToState(getValue());
            channelsToUpdate.put(channelID, state);
            calculateEfficiencies(cache);
        } else if (channelID.contains("Status#CO2")) {
            calculateCO2(cache);
        } else if (channelID.contains("Setting#CO2SetPoint")) {
            calculateCO2SetPoint(cache);
        } else {
            State state = valloxChannel.convertToState(getValue());
            channelsToUpdate.put(channelID, state);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Channels parsed from telegram {}", channelsToUpdate);
        }
        return channelsToUpdate;
    }

    /**
     * Calculate efficiencies from measured temperatures. This is specified by Vallox.
     *
     * @param cache the cache where temperatures are fetched
     */
    public void calculateEfficiencies(ValloxExpiringCacheMap cache) {
        try {
            if (!cache.containsTemperatures()) {
                return;
            }
            int tempInside = ValloxBindingConstants.TEMPERATURE_MAPPING[Byte
                    .toUnsignedInt(cache.getValue((byte) 0x34))];
            int tempOutside = ValloxBindingConstants.TEMPERATURE_MAPPING[Byte
                    .toUnsignedInt(cache.getValue((byte) 0x32))];
            int tempExhaust = ValloxBindingConstants.TEMPERATURE_MAPPING[Byte
                    .toUnsignedInt(cache.getValue((byte) 0x33))];
            int tempIncoming = ValloxBindingConstants.TEMPERATURE_MAPPING[Byte
                    .toUnsignedInt(cache.getValue((byte) 0x35))];
            int maxPossible = tempInside - tempOutside;
            if (maxPossible <= 0) {
                channelsToUpdate.put("Efficiency#InEfficiency", new DecimalType(100));
                channelsToUpdate.put("Efficiency#OutEfficiency", new DecimalType(100));
                channelsToUpdate.put("Efficiency#AvgEfficiency", new DecimalType(100));
            }
            if (maxPossible > 0) {
                double inEfficiency = (tempIncoming - tempOutside) * 100.0 / maxPossible;
                channelsToUpdate.put("Efficiency#InEfficiency", new DecimalType(inEfficiency));
                double outEfficiency = (tempInside - tempExhaust) * 100.0 / maxPossible;
                channelsToUpdate.put("Efficiency#OutEfficiency", new DecimalType(outEfficiency));
                double averageEfficiency = (inEfficiency + outEfficiency) / 2;
                channelsToUpdate.put("Efficiency#AverageEfficiency", new DecimalType(averageEfficiency));
            }
        } catch (Exception e) {
            logger.debug("Exception caught while calculating efficiencies", e);
        }
    }

    /**
     * Calculate measure CO2 value from 2 bytes. Both needs to be in cache.
     *
     * @param cache the cache where values are fetched.
     */
    private void calculateCO2(ValloxExpiringCacheMap cache) {
        try {
            if (!cache.containsCO2()) {
                logger.debug("Skipping CO2 calculation. Not enough values in cache.");
                channelsToUpdate.put("Status#CO2", UnDefType.UNDEF);
                return;
            }
            byte co2High = cache.getValue((byte) 0x2B);
            byte co2Low = cache.getValue((byte) 0x2C);
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] { co2High, co2Low });
            channelsToUpdate.put("Status#CO2", new DecimalType(byteBuffer.getShort()));
        } catch (Exception e) {
            logger.debug("Exception caught while calculating co2 {}", e.getMessage());
            channelsToUpdate.put("Status#CO2", UnDefType.UNDEF);
        }
    }

    /**
     * Calculate CO2 set point value from 2 bytes. Both needs to be in cache.
     *
     * @param cache the cache where values are fetched.
     */
    private void calculateCO2SetPoint(ValloxExpiringCacheMap cache) {
        try {
            if (!cache.containsCO2SetPoint()) {
                logger.debug("Skipping CO2 set point calculation. Not enough values in cache.");
                channelsToUpdate.put("Status#CO2SetPoint", UnDefType.UNDEF);
                return;
            }
            byte high = cache.getValue((byte) 0xB3);
            byte low = cache.getValue((byte) 0xB4);
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] { high, low });
            channelsToUpdate.put("Setting#CO2SetPoint", new DecimalType(byteBuffer.getShort()));
        } catch (Exception e) {
            logger.debug("Exception caught while calculating co2 set point {}", e.getMessage());
            channelsToUpdate.put("Status#CO2SetPoint", UnDefType.UNDEF);
        }
    }
}
