/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.atomic.AtomicReference;

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

    private AtomicReference<@Nullable SerialPort> serialPortReference = new AtomicReference<>();
    private final SerialPortManager serialPortManager;
    // private @Nullable SerialPort serialPort;
    private final Object portLock = new Object();

    public ValloxSerialConnector(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
        logger.debug("Serial connector initialized");
    }

    @Override
    public void connect(ValloxSEConfiguration config) throws IOException {
        synchronized (portLock) {
            try {
                logger.debug("Connecting to {}", config.serialPort);
                SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(config.serialPort);
                if (portIdentifier == null) {
                    throw new IOException("No such port " + config.serialPort);
                }
                SerialPort oldSerialPort = serialPortReference.get();
                SerialPort serialPort = portIdentifier.open("vallox", SERIAL_PORT_READ_TIMEOUT);
                serialPort.setSerialPortParams(SERIAL_BAUDRATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                panelNumber = config.getPanelAsByte();
                connected = true;

                serialPort.addEventListener(this);

                serialPort.notifyOnDataAvailable(true);
                serialPort.notifyOnBreakInterrupt(true);
                serialPort.notifyOnFramingError(true);
                serialPort.notifyOnOverrunError(true);
                serialPort.notifyOnParityError(true);

                serialPort.enableReceiveThreshold(SERIAL_RECEIVE_THRESHOLD_BYTES);
                serialPort.enableReceiveTimeout(SERIAL_RECEIVE_TIMEOUT_MILLISECONDS);

                logger.debug("Connected to {}", config.serialPort);

                if (!serialPortReference.compareAndSet(oldSerialPort, serialPort)) {
                    logger.warn("Possible bug because a new serial port value was set during opening new port");
                    throw new IOException("");
                }
                startProcessorJobs();
            } catch (TooManyListenersException e) {
                throw new IOException("Too many listeners", e);
            } catch (PortInUseException e) {
                throw new IOException("Port in use", e);
            } catch (UnsupportedCommOperationException e) {
                throw new IOException("Unsupported comm operation", e);
            }
        }
    }

    /**
     * Closes the serial port.
     */
    @Override
    public void close() {
        synchronized (portLock) {
            super.close();
            SerialPort serialPort = serialPortReference.get();
            if (serialPort != null) {
                serialPort.removeEventListener();

                // Close inputstream
                try {
                    InputStream is = getInputStream();
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    logger.debug("Failed to close serial port inputstream", e);
                }

                // Close outputstream
                try {
                    OutputStream os = getOutputStream();
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    logger.debug("Failed to close serial port outputstream", e);
                }
                serialPort.close();
            }
            serialPort = null;
            logger.debug("Serial connection closed");
        }
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
