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

import static org.openhab.binding.vallox.internal.ValloxBindingConstants.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.vallox.internal.ValloxBindingConstants;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.openhab.binding.vallox.internal.telegram.Telegram.TelegramState;
import org.openhab.binding.vallox.internal.telegram.TelegramFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ValloxSerialConnector} is responsible for creating serial connection to vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ValloxSerialConnector extends ValloxBaseConnector implements SerialPortEventListener {

    private final Logger logger = LoggerFactory.getLogger(ValloxSerialConnector.class);

    private @Nullable SerialPort serialPort;

    public ValloxSerialConnector(SerialPortManager portManager, ScheduledExecutorService scheduler) {
        super(portManager, scheduler);
        logger.debug("Serial connection initialized");
    }

    @SuppressWarnings("null")
    @Override
    public void connect(ValloxConfiguration config) throws IOException {
        if (isConnected()) {
            return;
        }
        try {

            if (portManager == null) {
                throw new IOException("PortManager is null");
            }
            SerialPortIdentifier portIdentifier = portManager.getIdentifier(config.serialPort);

            if (portIdentifier == null) {
                throw new IOException("No such port " + config.serialPort);
            }

            this.serialPort = portIdentifier.open("vallox", SERIAL_PORT_READ_TIMEOUT);
            serialPort.setSerialPortParams(SERIAL_BAUDRATE, SerialPort.DATABITS_8, SerialPort.PARITY_NONE,
                    SerialPort.STOPBITS_1);

            logger.trace("Serial port {} opened", config.serialPort);

            inputStream = new BufferedInputStream(serialPort.getInputStream());
            panelNumber = config.getPanelAsByte();
            connected = true;

            serialPort.addEventListener(this);

            serialPort.notifyOnDataAvailable(true);
            serialPort.notifyOnBreakInterrupt(true);
            serialPort.notifyOnFramingError(true);
            serialPort.notifyOnOverrunError(true);
            serialPort.notifyOnParityError(true);

            serialPort.enableReceiveThreshold(SERIAL_TIMEOUT_MILLISECONDS);
            serialPort.enableReceiveTimeout(SERIAL_TIMEOUT_MILLISECONDS);

            serialPort.setRTS(true);
            logger.debug("Connected to {}", config.serialPort);
        } catch (TooManyListenersException e) {
            throw new IOException("Too many listeners", e);
        } catch (PortInUseException e) {
            throw new IOException("Port in use", e);
        } catch (UnsupportedCommOperationException | IOException e) {
            throw new IOException("Unsupported com operation -> " + e.getMessage(), e);
        }
    }

    /**
     * Closes the serial port.
     */
    @SuppressWarnings("null")
    @Override
    public void close() {
        super.close();
        connected = false;
        serialPort.setRTS(false);
        if (serialPort != null) {
            serialPort.removeEventListener();
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ioe) {
                logger.debug("Failed to close serial port inputstream", ioe);
            }
            serialPort.close();
        }
        serialPort = null;
        connected = false;
        logger.debug("Serial connection closed");
    }

    @Override
    public void serialEvent(@Nullable SerialPortEvent seEvent) {
        if (seEvent == null) {
            return;
        }
        if (logger.isTraceEnabled() && SerialPortEvent.DATA_AVAILABLE != seEvent.getEventType()) {
            logger.trace("Serial event: {}, value:{}", seEvent.getEventType(), seEvent.getNewValue());
        }
        try {
            switch (seEvent.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    handleDataAvailable();
                    break;
                case SerialPortEvent.BI:
                    sendErrorToListeners("Break interrupt " + seEvent.toString(), null);
                    break;
                case SerialPortEvent.FE:
                    sendErrorToListeners("Frame error " + seEvent.toString(), null);
                    break;
                case SerialPortEvent.OE:
                    sendErrorToListeners("Overrun error " + seEvent.toString(), null);
                    break;
                case SerialPortEvent.PE:
                    sendErrorToListeners("Parity error " + seEvent.toString(), null);
                    break;
                default: // do nothing
            }
        } catch (RuntimeException e) {
            logger.warn("RuntimeException during handling serial event: {}", seEvent.getEventType(), e);
        }
    }

    /**
     * Read available data from input stream if its not null
     */
    @SuppressWarnings("null")
    private void handleDataAvailable() {
        try {
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int available = inputStream.available();
                int i = 0;
                while (inputStream.read() != -1) {
                    buffer[i] = (byte) inputStream.read();
                    i++;
                }
                logger.debug("Bytes available: {}, Bytes read: {}", available, i);
                handleBuffer(buffer);
            }
        } catch (IOException e) {
            logger.debug("Exception while handling available data ", e);
        }
    }

    /**
     * Parse byte buffer into telegrams. Separate single acknowledged and false bytes from buffer,
     * and after that pass next 6 bytes to telegram creation.
     *
     * @param buffer byte array to parse into telegrams
     */
    private void handleBuffer(byte[] buffer) {
        List<Byte> skipped = new CopyOnWriteArrayList<>();
        int bytesRead = 0;

        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];

            if (bytesRead > i) {
                continue;
            }
            if (waitForAckByte) {
                ackByte = b;
                waitForAckByte = false;
                continue;
            }
            if (b != ValloxBindingConstants.DOMAIN) {
                skipped.add(b);
                continue;
            }
            byte[] localBuffer = Arrays.copyOfRange(buffer, i, i + 6);
            bytesRead = i + 6;
            createTelegramForListeners(localBuffer);
        }
        if (!skipped.isEmpty()) {
            logger.debug("Skipped bytes: {}", skipped.size());
        }
    }

    /**
     * Form a telegram from bytes and send it to listeners.
     *
     * @param buffer the byte buffer to handle
     */
    private void createTelegramForListeners(byte[] buffer) {
        if (!TelegramFactory.isChecksumValid(buffer, buffer[5])) {
            sendTelegramToListeners(new Telegram(TelegramState.CRC_ERROR, buffer));
            return;
        }
        if (buffer[3] == ValloxBindingConstants.SUSPEND_BYTE) {
            sendTelegramToListeners(new Telegram(TelegramState.SUSPEND));
            suspendTraffic = true;
            return;
        }
        if (buffer[3] == ValloxBindingConstants.RESUME_BYTE) {
            sendTelegramToListeners(new Telegram(TelegramState.RESUME));
            suspendTraffic = false;
            return;
        }
        if (buffer[2] == panelNumber || buffer[2] == ValloxBindingConstants.ADDRESS_ALL_PANELS
                || buffer[2] == ValloxBindingConstants.ADDRESS_PANEL1) {
            sendTelegramToListeners(new Telegram(TelegramState.OK, buffer));
        } else {
            sendTelegramToListeners(new Telegram(TelegramState.NOT_FOR_US, buffer));
        }
    }
}
