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
import java.util.TooManyListenersException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ValloxSerialConnection} is responsible for creating serial connection to vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ValloxSerialConnection extends ValloxBaseConnection implements SerialPortEventListener {

    private final Logger logger = LoggerFactory.getLogger(ValloxSerialConnection.class);

    private @Nullable BufferedInputStream inputStream;
    private @Nullable SerialPort serialPort;
    private SerialPortManager portManager;
    private final byte[] buffer = new byte[1024]; // 1K

    public ValloxSerialConnection(SerialPortManager portManager) {
        this.portManager = portManager;
        logger.debug("Vallox serial connection initialized");
    }

    @Override
    public void connect(ValloxConfiguration config) throws IOException {
        if (isConnected()) {
            return;
        }
        logger.debug("Connecting to serial port: {}", config.serialPort);
        try {
            SerialPortIdentifier portIdentifier = portManager.getIdentifier(config.serialPort);
            if (portIdentifier == null) {
                throw new IOException("No such port" + config.serialPort);
            }
            SerialPort serialPort = portIdentifier.open("vallox", SERIAL_PORT_READ_TIMEOUT);
            serialPort.setSerialPortParams(SERIAL_BAUDRATE, SerialPort.DATABITS_8, SerialPort.PARITY_NONE,
                    SerialPort.STOPBITS_1);

            logger.trace("Serial port {} opened", config.serialPort);

            inputStream = new BufferedInputStream(serialPort.getInputStream());

            serialPort.addEventListener(this);

            serialPort.notifyOnDataAvailable(true);
            serialPort.notifyOnBreakInterrupt(true);
            serialPort.notifyOnFramingError(true);
            serialPort.notifyOnOverrunError(true);
            serialPort.notifyOnParityError(true);

            serialPort.enableReceiveThreshold(SERIAL_TIMEOUT_MILLISECONDS);
            serialPort.enableReceiveTimeout(SERIAL_TIMEOUT_MILLISECONDS);
            connected = true;
            logger.debug("Connected to {}", config.serialPort);
        } catch (TooManyListenersException e) {
            throw new IOException("Too many listeners", e);
        } catch (PortInUseException e) {
            throw new IOException("Port in use", e);
        } catch (UnsupportedCommOperationException | IOException e) {
            throw new IOException("Unsupported com operation: {}" + e.getMessage(), e);
        }
    }

    /**
     * Closes the serial port and release OS resources.
     */
    @SuppressWarnings("null")
    @Override
    public void close() {
        connected = false;
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
            logger.trace("Serial event: {}, new value:{}", seEvent.getEventType(), seEvent.getNewValue());
        }
        try {
            switch (seEvent.getEventType()) {
                case SerialPortEvent.DATA_AVAILABLE:
                    handleDataAvailable();
                    break;
                case SerialPortEvent.BI:
                    sendErrorToListeners("Break interrupt " + seEvent.toString());
                    break;
                case SerialPortEvent.FE:
                    sendErrorToListeners("Frame error " + seEvent.toString());
                    break;
                case SerialPortEvent.OE:
                    sendErrorToListeners("Overrun error " + seEvent.toString());
                    break;
                case SerialPortEvent.PE:
                    sendErrorToListeners("Parity error " + seEvent.toString());
                    break;
                default: // do nothing
            }
        } catch (RuntimeException e) {
            logger.warn("RuntimeException during handling serial event: {}", seEvent.getEventType(), e);
        }
    }

    /**
     * Handles available data
     */
    protected void handleDataAvailable() {
        try {
            BufferedInputStream localInputStream = inputStream;

            if (localInputStream != null) {
                int bytesAvailable = localInputStream.available();
                while (bytesAvailable > 0) {
                    int bytesAvailableRead = localInputStream.read(buffer, 0, Math.min(bytesAvailable, buffer.length));

                    if (connected && bytesAvailableRead > 0) {
                        // dsmrConnectorListener.handleData(buffer, bytesAvailableRead);
                        // sendTelegramToListeners(buffer);
                        logger.debug("Expected bytes {}, read bytes {}", bytesAvailable, bytesAvailableRead);
                    } else {
                        logger.debug("Expected bytes {} to read, but {} bytes were read", bytesAvailable,
                                bytesAvailableRead);
                    }
                    bytesAvailable = localInputStream.available();
                }
            }

        } catch (IOException e) {
            logger.debug("Exception while handling available data ", e);
        }
    }

    @Override
    public void sendTelegram(Telegram telegram) {
        // TODO Auto-generated method stub

    }

    @Override
    public void sendCommand(Telegram telegram) {
        // TODO Auto-generated method stub

    }

}
