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
import org.openhab.binding.vallox.internal.telegram.Telegram.TelegramState;

/**
 * The {@link TelegramFactory} creates telegram to send.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class TelegramFactory {

    public static Telegram createPoll(int panelNumber, byte variable) {
        byte[] telegram = new byte[ValloxBindingConstants.TELEGRAM_LENGTH];

        telegram[0] = ValloxBindingConstants.DOMAIN;
        telegram[1] = Converter.panelNumberToByte(panelNumber);
        telegram[2] = ValloxBindingConstants.ADDRESS_MASTER;
        telegram[3] = ValloxBindingConstants.POLL_BYTE;
        telegram[4] = variable;
        telegram[5] = calculateChecksum(telegram);

        return new Telegram(TelegramState.POLL, telegram);
    }

    public static Telegram createCommand(int panelNumber, byte variable, byte value) {
        byte[] telegram = new byte[ValloxBindingConstants.TELEGRAM_LENGTH];

        telegram[0] = ValloxBindingConstants.DOMAIN;
        telegram[1] = Converter.panelNumberToByte(panelNumber);
        telegram[2] = ValloxBindingConstants.ADDRESS_MASTER;
        telegram[3] = variable;
        telegram[4] = value;
        telegram[5] = calculateChecksum(telegram);

        return new Telegram(TelegramState.COMMAND, telegram);
    }

    /**
     * Calculate checksum for telegram
     *
     * @param pTelegram
     * @return calculated checksum
     */
    static byte calculateChecksum(byte[] pTelegram) {
        int checksum = 0;
        for (byte i = 0; i < pTelegram.length - 1; i++) {
            checksum += pTelegram[i];
        }
        return (byte) (checksum % 256);
    }
}
