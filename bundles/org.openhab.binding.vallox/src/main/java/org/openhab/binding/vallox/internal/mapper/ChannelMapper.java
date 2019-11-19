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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ChannelMapper} maps all channel information together.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ChannelMapper {

    private static final String CHANNEL_GROUP_FAN = "FanControl#";
    private static final String CHANNEL_GROUP_TEMPERATURE = "Temperature#";
    private static final String CHANNEL_GROUP_EFFICIENCY = "Efficiency#";
    private static final String CHANNEL_GROUP_SETTINGS = "Setting#";
    private static final String CHANNEL_GROUP_STATUS = "Status#";
    private static final String CHANNEL_GROUP_MAINTENANCE = "Maintenance#";
    private static final String CHANNEL_GROUP_ALARM = "Alarm#";

    @SuppressWarnings("serial")
    private static final Map<String, ValloxChannel> VALLOXSE = Collections
            .unmodifiableMap(new HashMap<String, ValloxChannel>() {
                {
            // @formatter:off
                    // FanControls
                    put(CHANNEL_GROUP_FAN + "FanSpeed", new FanChannel((byte) 0x29));
                    put(CHANNEL_GROUP_FAN + "FanSpeedMax", new FanChannel((byte) 0xA5));
                    put(CHANNEL_GROUP_FAN + "FanSpeedMin", new FanChannel((byte) 0xA9));
                    put(CHANNEL_GROUP_FAN + "DCFanInputAdjustment", new IntegerChannel((byte) 0xB0));
                    put(CHANNEL_GROUP_FAN + "DCFanOutputAdjustment", new IntegerChannel((byte) 0xB1));
                    put(CHANNEL_GROUP_FAN + "SupplyFanState", new BooleanChannel((byte) 0x08, "ioPortMultiPurpose2"));
                    put(CHANNEL_GROUP_FAN + "ExhaustFanState", new BooleanChannel((byte) 0x20, "ioPortMultiPurpose2"));

                    // Temperatures
                    put(CHANNEL_GROUP_TEMPERATURE + "TempInside", new TemperatureChannel((byte) 0x34));
                    put(CHANNEL_GROUP_TEMPERATURE + "TempOutside", new TemperatureChannel((byte) 0x32));
                    put(CHANNEL_GROUP_TEMPERATURE + "TempExhaust", new TemperatureChannel((byte) 0x33));
                    put(CHANNEL_GROUP_TEMPERATURE + "TempIncoming", new TemperatureChannel((byte) 0x35));

                    // Efficiencies
                    put(CHANNEL_GROUP_EFFICIENCY + "InEfficiency", new IntegerChannel((byte) 0x00));
                    put(CHANNEL_GROUP_EFFICIENCY + "OutEfficiency", new IntegerChannel((byte) 0x00));
                    put(CHANNEL_GROUP_EFFICIENCY + "AverageEfficiency", new IntegerChannel((byte) 0x00));

                    // Settings
                    put(CHANNEL_GROUP_SETTINGS + "PowerState", new BooleanChannel((byte) 0x01, "select"));
                    put(CHANNEL_GROUP_SETTINGS + "CO2AdjustState", new BooleanChannel((byte) 0x02, "select"));
                    put(CHANNEL_GROUP_SETTINGS + "HumidityAdjustState", new BooleanChannel((byte) 0x04, "select"));
                    put(CHANNEL_GROUP_SETTINGS + "PostHeatingState", new BooleanChannel((byte) 0x08, "select"));
                    put(CHANNEL_GROUP_SETTINGS + "HrcBypassThreshold", new TemperatureChannel((byte) 0xAF));
                    put(CHANNEL_GROUP_SETTINGS + "InputFanStopThreshold", new TemperatureChannel((byte) 0xA8));
                    put(CHANNEL_GROUP_SETTINGS + "PostHeatingSetPoint", new TemperatureChannel((byte) 0xA4));
                    put(CHANNEL_GROUP_SETTINGS + "PreHeatingSetPoint", new TemperatureChannel((byte) 0xA7));
                    put(CHANNEL_GROUP_SETTINGS + "CO2SetPoint", new IntegerChannel((byte) 0x00));
                    put(CHANNEL_GROUP_SETTINGS + "CO2SetPointHigh", new IntegerChannel((byte) 0xB3));
                    put(CHANNEL_GROUP_SETTINGS + "CO2SetPointLow", new IntegerChannel((byte) 0xB4));
                    put(CHANNEL_GROUP_SETTINGS + "CascadeAdjust", new BooleanChannel((byte) 0x80, "program1"));
                    put(CHANNEL_GROUP_SETTINGS + "AdjustmentIntervalMinutes", new BooleanChannel.AdjustmentInterval((byte) 0x0F, "program1"));
                    put(CHANNEL_GROUP_SETTINGS + "MaxSpeedLimitMode", new BooleanChannel((byte) 0x01, "program2"));
                    put(CHANNEL_GROUP_SETTINGS + "BasicHumidityLevel", new IntegerChannel.Humidity((byte) 0xAE));
                    put(CHANNEL_GROUP_SETTINGS + "BoostSwitchMode", new BooleanChannel((byte) 0x20, "program1"));
                    put(CHANNEL_GROUP_SETTINGS + "RadiatorType", new BooleanChannel((byte) 0x40, "program1"));
                    put(CHANNEL_GROUP_SETTINGS + "ActivateFirePlaceBooster", new BooleanChannel((byte) 0x20, "flags6"));
                    put(CHANNEL_GROUP_SETTINGS + "AutomaticHumidityLevelSeekerState", new BooleanChannel((byte) 0x10, "program1"));
                    put(CHANNEL_GROUP_SETTINGS + "PreHeatingState", new BooleanChannel((byte) 0x80, "flags5"));

                    // Status
                    put(CHANNEL_GROUP_STATUS + "Humidity", new IntegerChannel.Humidity((byte) 0x2A));
                    put(CHANNEL_GROUP_STATUS + "HumiditySensor1", new IntegerChannel.Humidity((byte) 0x2F));
                    put(CHANNEL_GROUP_STATUS + "HumiditySensor2", new IntegerChannel.Humidity((byte) 0x30));
                    put(CHANNEL_GROUP_STATUS + "CO2", new IntegerChannel((byte) 0x00));
                    put(CHANNEL_GROUP_STATUS + "CO2High", new IntegerChannel((byte) 0x2B));
                    put(CHANNEL_GROUP_STATUS + "CO2Low", new IntegerChannel((byte) 0x2C));
                    put(CHANNEL_GROUP_STATUS + "PostHeatingIndicator", new BooleanChannel((byte) 0x20, "select"));
                    put(CHANNEL_GROUP_STATUS + "InstalledCO2Sensors", new StringChannel((byte) 0x2D));
                    put(CHANNEL_GROUP_STATUS + "PreHeatingOn", new BooleanChannel((byte) 0x10, "ioPortMultiPurpose2"));
                    put(CHANNEL_GROUP_STATUS + "PostHeatingOn", new BooleanChannel((byte) 0x20, "ioPortMultiPurpose1"));
                    put(CHANNEL_GROUP_STATUS + "DamperMotorPosition", new BooleanChannel((byte) 0x02, "ioPortMultiPurpose2"));
                    put(CHANNEL_GROUP_STATUS + "FirePlaceBoosterSwitch", new BooleanChannel((byte) 0x40, "ioPortMultiPurpose2"));
                    put(CHANNEL_GROUP_STATUS + "IncomingCurrent", new IntegerChannel((byte) 0x2E));
                    put(CHANNEL_GROUP_STATUS + "SlaveMasterIndicator", new BooleanChannel((byte) 0x80, "flags4"));
                    put(CHANNEL_GROUP_STATUS + "PostHeatingTargetValue", new TemperatureChannel((byte) 0x57));
                    put(CHANNEL_GROUP_STATUS + "FirePlaceBoosterOn", new BooleanChannel((byte) 0x40, "flags6"));
                    put(CHANNEL_GROUP_STATUS + "FirePlaceBoosterCounter", new IntegerChannel((byte) 0x79));
                    put(CHANNEL_GROUP_STATUS + "RemoteControlOn", new BooleanChannel((byte) 0x10, "flags6"));

                    // Maintenance
                    put(CHANNEL_GROUP_MAINTENANCE + "FilterGuardIndicator", new BooleanChannel((byte) 0x10, "select"));
                    put(CHANNEL_GROUP_MAINTENANCE + "ServiceReminderIndicator", new BooleanChannel((byte) 0x80, "select"));
                    put(CHANNEL_GROUP_MAINTENANCE + "MaintenanceMonthCounter", new IntegerChannel((byte) 0xAB));
                    put(CHANNEL_GROUP_MAINTENANCE + "ServiceReminder", new IntegerChannel((byte) 0xA6));

                    // Alarm
                    put(CHANNEL_GROUP_ALARM + "FaultIndicator", new BooleanChannel((byte) 0x40, "select"));
                    put(CHANNEL_GROUP_ALARM + "FaultSignalRelayClosed", new BooleanChannel((byte) 0x04, "ioPortMultiPurpose2"));
                    put(CHANNEL_GROUP_ALARM + "CO2Alarm", new BooleanChannel((byte) 0x40, "flags2"));
                    put(CHANNEL_GROUP_ALARM + "HrcFreezingAlarm", new BooleanChannel((byte) 0x80, "flags2"));
                    put(CHANNEL_GROUP_ALARM + "WaterRadiatorFreezingAlarm", new BooleanChannel((byte) 0x10, "flags4"));
                    put(CHANNEL_GROUP_ALARM + "LastErrorNumber", new IntegerChannel((byte) 0x36));

                    // Multiple value channels
                    put("ioPortMultiPurpose1", new MultipleValueChannel((byte) 0x07, Arrays.asList(CHANNEL_GROUP_STATUS + "PostHeatingOn")));

                    put("ioPortMultiPurpose2", new MultipleValueChannel((byte) 0x08, Arrays.asList(CHANNEL_GROUP_STATUS + "DamperMotorPosition",
                                                                                                   CHANNEL_GROUP_ALARM  + "FaultSignalRelayClosed",
                                                                                                   CHANNEL_GROUP_FAN    + "SupplyFanState",
                                                                                                   CHANNEL_GROUP_STATUS + "PreHeatingOn",
                                                                                                   CHANNEL_GROUP_FAN    + "ExhaustFanState",
                                                                                                   CHANNEL_GROUP_STATUS + "FirePlaceBoosterSwitch")));
                    put("flags2", new MultipleValueChannel((byte) 0x6D, Arrays.asList(CHANNEL_GROUP_ALARM + "CO2Alarm",
                                                                                      CHANNEL_GROUP_ALARM + "HrcFreezingAlarm")));

                    put("flags4", new MultipleValueChannel((byte) 0x6F, Arrays.asList(CHANNEL_GROUP_ALARM  + "WaterRadiatorFreezingAlarm",
                                                                                      CHANNEL_GROUP_STATUS + "SlaveMasterIndicator")));

                    put("flags5", new MultipleValueChannel((byte) 0x70, Arrays.asList(CHANNEL_GROUP_SETTINGS + "PreHeatingState")));

                    put("flags6", new MultipleValueChannel((byte) 0x71, Arrays.asList(CHANNEL_GROUP_STATUS   + "RemoteControlOn",
                                                                                      CHANNEL_GROUP_SETTINGS + "ActivateFirePlaceBooster",
                                                                                      CHANNEL_GROUP_STATUS   + "FirePlaceBoosterOn")));

                    put("select", new MultipleValueChannel((byte) 0xA3, Arrays.asList(CHANNEL_GROUP_SETTINGS + "PowerState",
                                                                                      CHANNEL_GROUP_SETTINGS + "CO2AdjustState",
                                                                                      CHANNEL_GROUP_SETTINGS + "HumidityAdjustState",
                                                                                      CHANNEL_GROUP_SETTINGS + "PostHeatingState",
                                                                                      CHANNEL_GROUP_MAINTENANCE + "FilterGuardIndicator",
                                                                                      CHANNEL_GROUP_STATUS      + "PostHeatingIndicator",
                                                                                      CHANNEL_GROUP_ALARM       + "FaultIndicator",
                                                                                      CHANNEL_GROUP_MAINTENANCE + "ServiceReminderIndicator")));

                    put("program1", new MultipleValueChannel((byte) 0xAA, Arrays.asList(CHANNEL_GROUP_SETTINGS + "AdjustmentIntervalMinutes",
                                                                                        CHANNEL_GROUP_SETTINGS + "AutomaticHumidityLevelSeekerState",
                                                                                        CHANNEL_GROUP_SETTINGS + "BoostSwitchMode",
                                                                                        CHANNEL_GROUP_SETTINGS + "RadiatorType",
                                                                                        CHANNEL_GROUP_SETTINGS + "CascadeAdjust")));

                    put("program2", new MultipleValueChannel((byte) 0xB5, Arrays.asList(CHANNEL_GROUP_SETTINGS + "MaxSpeedLimitMode")));
                    // @formatter:on
                }
            });

    /**
     * Get {@link ValloxChannel}
     */
    public static ValloxChannel getValloxChannel(String key) {
        return VALLOXSE.get(key);
    }

    /**
     * Get variable as byte for channel
     */
    public static byte getVariable(String key) {
        ValloxChannel vc = getValloxChannel(key);
        return vc.getVariable();
    }

    /**
     * Get channel for variable
     */
    public static String getChannelForVariable(byte variable) {
        for (String key : VALLOXSE.keySet()) {
            ValloxChannel vc = VALLOXSE.get(key);
            if ((vc.getVariable() == variable) && !(vc instanceof BooleanChannel)) {
                return key;
            }
        }
        return "";
    }
}
