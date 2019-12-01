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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.vallox.internal.ValloxBindingConstants;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.openhab.binding.vallox.internal.telegram.Telegram.TelegramState;
import org.openhab.binding.vallox.internal.telegram.TelegramFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ValloxIpConnector} is creates TCP/IP connection to Vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ValloxIpConnector extends ValloxBaseConnector {

    private final Logger logger = LoggerFactory.getLogger(ValloxIpConnector.class);

    protected @Nullable InputStream inputStream;
    private @Nullable Thread readerThread;
    private Socket socket = new Socket();

    public ValloxIpConnector(ScheduledExecutorService scheduler) {
        super(null, scheduler);
        logger.debug("Tcp Connection initialized");
    }

    /**
     * Connect to socket
     */
    @SuppressWarnings("null")
    @Override
    public void connect(ValloxConfiguration config) throws IOException {
        if (isConnected()) {
            return;
        }
        panelNumber = config.getPanelAsByte();
        socket = new Socket();
        socket.setSoTimeout(ValloxBindingConstants.SOCKET_READ_TIMEOUT);
        socket.connect(new InetSocketAddress(config.tcpHost, config.tcpPort),
                ValloxBindingConstants.CONNECTION_TIMEOUT);
        socket.setSoTimeout(ValloxBindingConstants.SOCKET_READ_TIMEOUT);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        logger.debug("Connected to {}:{}", config.tcpHost, config.tcpPort);

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
        super.close();
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
     * {@link Thread} implementation for reading telegrams
     *
     * @author Miika Jukka
     */
    @SuppressWarnings("null")
    @NonNullByDefault
    private class TelegramReader extends Thread {
        boolean interrupted = false;

        @Override
        public void interrupt() {
            interrupted = true;
            super.interrupt();
        }

        @Override
        public void run() {
            logger.debug("Data listener started");
            while (!interrupted && inputStream != null) {
                try {
                    int domain = inputStream.read();
                    if (domain != ValloxBindingConstants.DOMAIN) {
                        if (waitForAckByte) {
                            ackByte = ((byte) domain);
                            waitForAckByte = false;
                            continue;
                        }
                        sendTelegramToListeners(new Telegram(TelegramState.NOT_DOMAIN, (byte) domain));
                        continue;
                    }
                    int sender = inputStream.read();
                    int receiver = inputStream.read();
                    int variable = inputStream.read();
                    int value = inputStream.read();
                    int checksum = inputStream.read();

                    byte[] bytes = new byte[] { (byte) domain, (byte) sender, (byte) receiver, (byte) variable,
                            (byte) value, (byte) checksum };

                    if (!TelegramFactory.isChecksumValid(bytes, (byte) checksum)) {
                        sendTelegramToListeners(new Telegram(TelegramState.CRC_ERROR, bytes));
                        continue;
                    }
                    if (variable == ValloxBindingConstants.SUSPEND_BYTE) {
                        sendTelegramToListeners(new Telegram(TelegramState.SUSPEND));
                        suspendTraffic = true;
                        continue;
                    }
                    if (variable == ValloxBindingConstants.RESUME_BYTE) {
                        sendTelegramToListeners(new Telegram(TelegramState.RESUME));
                        suspendTraffic = false;
                        continue;
                    }
                    if (receiver == panelNumber || receiver == ValloxBindingConstants.ADDRESS_ALL_PANELS
                            || receiver == ValloxBindingConstants.ADDRESS_PANEL1) {
                        sendTelegramToListeners(new Telegram(TelegramState.OK, bytes));
                        continue;
                    } else {
                        sendTelegramToListeners(new Telegram(TelegramState.NOT_FOR_US, bytes));
                        continue;
                    }
                } catch (IOException e) {
                    sendErrorToListeners(e.getMessage(), e);
                    interrupt();
                }
            }
            logger.debug("Telegram listener stopped");
        }
    }
}
