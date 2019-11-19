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
import org.openhab.binding.vallox.internal.ValloxBindingConstants;

/**
 * The {@link Converter} contains all conversion methods for this binding
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class Converter {

    public static String telegramToString(byte[] telegram) {
        String telegramToString = String.format("01 %s %s %s %s %s", Converter.byteToHex(telegram[1]),
                Converter.byteToHex(telegram[2]), Converter.byteToHex(telegram[3]), Converter.byteToHex(telegram[4]),
                Converter.byteToHex(telegram[5]));
        return telegramToString;
    }

    /**
     * Panel number from integer to byte
     *
     * @param value the value to convert
     * @return byte
     */
    public static byte panelNumberToByte(int value) {
        return ValloxBindingConstants.ADDRESS_PANEL_MAPPING[value - 1];
    }

    /**
     * Temperature from byte to integer
     *
     * @param value the value to convert
     * @return integer
     */
    public static int temperatureToInt(byte value) {
        int index = Byte.toUnsignedInt(value);
        return ValloxBindingConstants.TEMPERATURE_MAPPING[index];
    }

    /**
     * Temperature from integer to byte
     *
     * @param temperature the value to convert
     * @return
     */
    public static byte temperatureToByte(int temperature) {
        byte value = 100;

        for (int i = 0; i < 255; i++) {
            byte valueFromTable = ValloxBindingConstants.TEMPERATURE_MAPPING[i];
            if (valueFromTable >= temperature) {
                value = (byte) i;
                break;
            }
        }

        return value;
    }

    /**
     * Convert a speed number from 1 to 8 to its hex telegram command.
     * 8 --> 0xFF
     *
     * @param value 1-8
     * @return
     */
    public static byte fanSpeedToByte(int value) {
        return ValloxBindingConstants.FAN_SPEED_MAPPING[value - 1];
    }

    /**
     * Convert a hex telegram command value to its speed number from 1 to 8.
     * 0xFF --> 8
     *
     * @param value
     * @return 1-8
     */
    public static int fanSpeedToInt(byte value) {
        int fanSpeed = 0;

        for (byte i = 0; i < 8; i++) {
            if (ValloxBindingConstants.FAN_SPEED_MAPPING[i] == value) {
                fanSpeed = (byte) (i + 1);
                break;
            }
        }
        return fanSpeed;
    }

    /**
     * Humidity calculation formula defined by vallox protocol
     *
     * @param value the value to calculate
     * @return
     */
    public static int humidityToInt(byte value) {
        int index = Byte.toUnsignedInt(value);

        return (int) ((index - 51) / 2.04);
    }

    public static byte humidityToByte(int value) {
        double index = value * 2.04;
        index += 51;

        return (byte) Math.round(index);
    }

    /**
     * Hex array for byte conversion methods
     */

    protected static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Get a human readable string-representation in hex for an input
     * byte.
     *
     * @param byte
     * @return
     */
    public static String byteToHex(byte b) {
        int v = b & 0xFF;
        char c1 = HEX_ARRAY[v >>> 4];
        char c2 = HEX_ARRAY[v & 0x0F];
        return "" + c1 + c2;
    }

    /**
     * Get a human readable string-representation in hex for an input
     * binary array of bytes.
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
