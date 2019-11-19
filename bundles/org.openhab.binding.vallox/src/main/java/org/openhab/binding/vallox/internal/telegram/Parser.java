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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.vallox.internal.cache.CacheMap;
import org.openhab.binding.vallox.internal.mapper.ChannelMapper;
import org.openhab.binding.vallox.internal.mapper.MultipleValueChannel;
import org.openhab.binding.vallox.internal.mapper.TemperatureChannel;
import org.openhab.binding.vallox.internal.mapper.ValloxChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Parser} class parses telegrams.
 *
 * @author Miika Jukka - Initial Contribution
 */
@NonNullByDefault
public class Parser {

    private final static Logger logger = LoggerFactory.getLogger(Parser.class);

    private static Map<String, State> channelsToUpdate = new HashMap<>();

    /**
     * Process telegram and return a map of channels to update
     */
    public static Map<String, State> process(String channelID, Byte arg, CacheMap cache) {
        channelsToUpdate.clear();

        ValloxChannel valloxChannel = ChannelMapper.getValloxChannel(channelID);

        if (valloxChannel instanceof MultipleValueChannel) {
            Collection<String> subchannels = valloxChannel.getList();
            for (String channel : subchannels) {
                ValloxChannel vc = ChannelMapper.getValloxChannel(channel);
                State state = vc.convertToState(arg);
                channelsToUpdate.put(channel, state);
            }

        } else if (valloxChannel instanceof TemperatureChannel) {
            State state = valloxChannel.convertToState(arg);
            channelsToUpdate.put(channelID, state);
            calculateEfficiencies(cache);

        } else if (channelID.contains("Status#CO2")) {
            calculateCO2(cache);

        } else if (channelID.contains("Setting#CO2SetPoint")) {
            calculateCO2SetPoint(cache);

        } else {

            State state = valloxChannel.convertToState(arg);
            channelsToUpdate.put(channelID, state);

        }
        logger.debug("{}", channelsToUpdate);
        return channelsToUpdate;
    }

    /**
     *
     * case SUSPEND: 0x91
     * // C02 communication starts: no tx allowed!
     *
     * case RESUME: 0x8F
     * // C02 communication ends: tx allowed!
     *
     **/

    /**
     * Efficiency calculator
     *
     * @param cache
     */
    public static void calculateEfficiencies(CacheMap cache) {

        try {
            if (!cache.containsTemperatures()) {
                return;
            }
            int tempInside = Converter.temperatureToInt(cache.getValue((byte) 0x34));
            int tempOutside = Converter.temperatureToInt(cache.getValue((byte) 0x32));
            int tempExhaust = Converter.temperatureToInt(cache.getValue((byte) 0x33));
            int tempIncoming = Converter.temperatureToInt(cache.getValue((byte) 0x35));

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

    private static void calculateCO2(CacheMap cache) {

        try {
            if (!cache.containsCO2()) {
                logger.debug("Skipping CO2 calculation. Not enough values in cache.");
                return;
            }
            int co2High = Byte.toUnsignedInt(cache.getValue((byte) 0x2B));
            int co2Low = Byte.toUnsignedInt(cache.getValue((byte) 0x2C));

            String co2AsString = Converter.byteToHex((byte) co2High) + Converter.byteToHex((byte) co2Low);
            int parsed = Integer.parseInt(co2AsString, 16);
            channelsToUpdate.put("Status#CO2", new DecimalType(parsed));

        } catch (Exception e) {
            logger.debug("Exception caught while merging co2", e);
        }
    }

    private static void calculateCO2SetPoint(CacheMap cache) {

        try {
            if (!cache.containsCO2SetPoint()) {
                logger.debug("Skipping CO2 set point calculation. Not enough values in cache.");
                return;
            }
            int cO2High = Byte.toUnsignedInt(cache.getValue((byte) 0xB3));
            int cO2Low = Byte.toUnsignedInt(cache.getValue((byte) 0xB4));

            String cO2AsString = Converter.byteToHex((byte) cO2High) + Converter.byteToHex((byte) cO2Low);
            int hexAsInt = Integer.parseInt(cO2AsString, 16);
            channelsToUpdate.put("Setting#CO2SetPoint", new DecimalType(hexAsInt));

        } catch (Exception e) {
            logger.debug("Exception caught while merging co2 set point", e);
        }
    }
}