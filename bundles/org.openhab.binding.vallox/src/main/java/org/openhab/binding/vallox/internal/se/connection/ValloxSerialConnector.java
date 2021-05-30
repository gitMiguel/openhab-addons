/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.vallox.internal.se.connection;

import static org.openhab.binding.vallox.internal.se.ValloxSEConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.TooManyListenersException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.vallox.internal.se.configuration.ValloxSEConfiguration;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEvent;
import org.openhab.core.io.transport.serial.SerialPortEventListener;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
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

    private final SerialPortManager serialPortManager;
    private @Nullable SerialPort serialPort;

    public ValloxSerialConnector(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
        logger.debug("Serial connector initialized");
    }

    @Override
    public void connect(ValloxSEConfiguration config) throws IOException {
        if (isConnected()) {
            return;
        }
        try {
            SerialPort localSerialPort = serialPort;
            logger.debug("Connecting to {}", config.serialPort);
            SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(config.serialPort);
            if (portIdentifier == null) {
                throw new IOException("No such port " + config.serialPort);
            }
            localSerialPort = portIdentifier.open("vallox", SERIAL_PORT_READ_TIMEOUT);
            localSerialPort.setSerialPortParams(SERIAL_BAUDRATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            inputStream = localSerialPort.getInputStream();
            outputStream = localSerialPort.getOutputStream();
            panelNumber = config.getPanelAsByte();
            connected = true;

            localSerialPort.addEventListener(this);

            localSerialPort.notifyOnDataAvailable(true);
            localSerialPort.notifyOnBreakInterrupt(true);
            localSerialPort.notifyOnFramingError(true);
            localSerialPort.notifyOnOverrunError(true);
            localSerialPort.notifyOnParityError(true);

            localSerialPort.enableReceiveThreshold(SERIAL_RECEIVE_THRESHOLD_BYTES);
            localSerialPort.enableReceiveTimeout(SERIAL_RECEIVE_TIMEOUT_MILLISECONDS);

            logger.debug("Connected to {}", config.serialPort);
            serialPort = localSerialPort;
            startProcessorJobs();
        } catch (TooManyListenersException e) {
            throw new IOException("Too many listeners", e);
        } catch (PortInUseException e) {
            throw new IOException("Port in use", e);
        } catch (UnsupportedCommOperationException e) {
            throw new IOException("Unsupported comm operation", e);
        }
    }

    /**
     * Closes the serial port.
     */
    @Override
    public void close() {
        super.close();
        SerialPort localSerialPort = serialPort;
        if (localSerialPort != null) {
            localSerialPort.removeEventListener();
            IOUtils.closeQuietly(getInputStream());
            IOUtils.closeQuietly(getOutputStream());
            localSerialPort.close();
            serialPort = null;
        }
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
    private void handleDataAvailable() {
        InputStream inputStream = getInputStream();
        try {
            while (inputStream != null && inputStream.available() > 0) {
                buffer.add((byte) inputStream.read());
            }
        } catch (IOException e) {
            logger.debug("Exception while handling available data ", e);
        } catch (IllegalStateException e) {
            logger.warn("Read buffer full. Cleaning.");
            buffer.clear();
        }
    }
}
