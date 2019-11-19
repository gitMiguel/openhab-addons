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
package org.openhab.binding.vallox.internal.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.vallox.internal.ValloxBindingConstants;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.telegram.Converter;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.openhab.binding.vallox.internal.telegram.Telegram.TelegramState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ValloxIpConnection} is creates TCP/IP connection to Vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ValloxIpConnection extends ValloxBaseConnection {

    private final Logger logger = LoggerFactory.getLogger(ValloxIpConnection.class);

    protected @Nullable InputStream inputStream;
    protected @Nullable OutputStream outputStream;
    private @Nullable Thread readerThread;
    private Socket socket = new Socket();
    private int panelNumber;

    public ValloxIpConnection() {
        logger.debug("Connection created");
    }

    /**
     * Connect to socket
     */
    @Override
    public void connect(ValloxConfiguration config) throws IOException {
        if (isConnected()) {
            return;
        }
        panelNumber = config.panelNumber;

        logger.debug("Connecting to {}:{}", config.tcpHost, config.tcpPort);
        socket.setSoTimeout(ValloxBindingConstants.SOCKET_READ_TIMEOUT);
        socket.connect(new InetSocketAddress(config.tcpHost, config.tcpPort),
                ValloxBindingConstants.CONNECTION_TIMEOUT);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        logger.debug("Connected");

        readerThread = new TelegramReader();
        readerThread.start();
        connected = true;
    }

    /**
     * Close socket
     */
    @SuppressWarnings("null")
    @Override
    public void close() {
        if (readerThread != null) {
            logger.debug("Interrupt message listener");
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        try {
            socket.close();
        } catch (Exception e) {
            logger.debug("Exception closing connection {}", e);
        }
        readerThread = null;
        connected = false;
        logger.debug("Closed");
    }

    /**
     * Write and flush telegram to output stream
     */
    @SuppressWarnings("null")
    @Override
    public synchronized void sendTelegram(Telegram telegram) {
        if (outputStream != null) {
            try {
                outputStream.write(telegram.bytes);
                outputStream.flush();
                logger.debug("Wrote: {}", telegram.toString());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                sendErrorToListeners(e.getMessage());
            }
        }
    }

    /**
     * {@link Thread} implementation for reading telegrams
     *
     * @author Miika Jukka
     */
    private class TelegramReader extends Thread {
        boolean interrupted = false;

        @Override
        public void interrupt() {
            interrupted = true;
            super.interrupt();
        }

        @SuppressWarnings("null")
        @Override
        public void run() {
            logger.debug("Data listener started");
            while (!interrupted && inputStream != null) {
                Telegram telegram;
                try {
                    if (inputStream.available() == 0) {
                        telegram = new Telegram(TelegramState.EMPTY);
                    }
                    int domain = inputStream.read();
                    if (domain != ValloxBindingConstants.DOMAIN) {
                        if (waitForAckByte) {
                            ackByte = ((byte) domain);
                            waitForAckByte = false;
                            continue;
                        }
                        telegram = new Telegram(TelegramState.NOT_DOMAIN);
                        sendTelegramToListeners(telegram);
                        continue;
                    }
                    int sender = inputStream.read();
                    int receiver = inputStream.read();
                    int command = inputStream.read();
                    int arg = inputStream.read();
                    int checksum = inputStream.read();
                    int computedChecksum = (domain + sender + receiver + command + arg) & 0x00ff;

                    if (checksum != computedChecksum) {
                        telegram = new Telegram(TelegramState.CRC_ERROR);
                        sendTelegramToListeners(telegram);
                        continue;
                    }
                    if (receiver == Converter.panelNumberToByte(panelNumber)
                            || receiver == ValloxBindingConstants.ADDRESS_ALL_PANELS
                            || receiver == ValloxBindingConstants.ADDRESS_PANEL1) {
                        byte[] bytes = new byte[6];
                        bytes[0] = (byte) domain;
                        telegram = new Telegram(TelegramState.OK, (byte) sender, (byte) receiver, (byte) command,
                                (byte) arg, (byte) checksum);
                        sendTelegramToListeners(telegram);
                        continue;
                    }
                    // telegram = new Telegram(TelegramState.NOT_FOR_US);
                    // sendTelegramToListeners(telegram);
                } catch (IOException e) {
                    sendErrorToListeners(e.getMessage());
                    interrupt();
                }
            }
            logger.debug("Telegram listener stopped");
        }
    }

    /**
     * Send command telegram and wait for acknowledge byte
     */
    @Override
    public void sendCommand(Telegram telegram) {
        waitForAckByte = true;
        long timeout = System.currentTimeMillis() + 3000;
        do {
            sendTelegram(telegram);
            if (ackByte != telegram.bytes[5]) {
                waitForAckByte = false;
            }
        } while (waitForAckByte && timeout > System.currentTimeMillis());
        if (!waitForAckByte) {
            sendTelegramToListeners(new Telegram(TelegramState.ACK, ackByte));
        } else {
            sendErrorToListeners("Ack byte not received for telegram: " + telegram.toString());
        }
    }
}
