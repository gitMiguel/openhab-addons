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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.vallox.internal.telegram.SendQueueItem;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.openhab.binding.vallox.internal.telegram.Telegram.TelegramState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface defines methods to receive data from vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public abstract class ValloxBaseConnector implements ValloxConnector {

    private final Logger logger = LoggerFactory.getLogger(ValloxBaseConnector.class);

    private final List<ValloxEventListener> listeners = new ArrayList<>();
    private final LinkedList<SendQueueItem> sendQueue = new LinkedList<>();

    protected @Nullable SerialPortManager portManager;
    protected ScheduledExecutorService scheduler;
    protected @Nullable OutputStream outputStream;
    protected @Nullable InputStream inputStream;
    protected @Nullable ScheduledFuture<?> sendQueueHandler;

    protected byte ackByte;
    protected byte panelNumber;
    protected boolean connected = false;
    protected boolean waitForAckByte = false;
    protected boolean suspendTraffic = false;

    public ValloxBaseConnector(@Nullable SerialPortManager portManager, ScheduledExecutorService scheduler) {
        this.portManager = portManager;
        this.scheduler = scheduler;
    }

    /**
     * Add listener.
     *
     * @param listener the listener to add
     */
    @Override
    public synchronized void addListener(ValloxEventListener listener) {
        if (!listeners.contains(listener)) {
            this.listeners.add(listener);
            logger.debug("Listener registered: {}", listener.toString());
        }
    }

    /**
     * Remove listener.
     *
     * @param listner the listener to remove
     */
    @Override
    public synchronized void removeListener(ValloxEventListener listener) {
        this.listeners.remove(listener);
        logger.debug("Listener removed: {}", listener.toString());
    }

    /**
     * Get connection status.
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Stop queue handler
     */
    @SuppressWarnings("null")
    @Override
    public void close() {
        if (sendQueueHandler != null) {
            sendQueueHandler.cancel(true);
            sendQueueHandler = null;
            logger.debug("Send queue handler stopped");
        }
    }

    /**
     * Send received telegram to registered listeners.
     *
     * @param telegram the telegram to send
     */
    public void sendTelegramToListeners(Telegram telegram) {
        for (ValloxEventListener listener : listeners) {
            try {
                listener.telegramReceived(telegram);
            } catch (Exception e) {
                logger.error("Event listener invoking error", e);
            }
        }
    }

    /**
     * Send received error to registered listeners.
     *
     * @param error the error to send
     */
    public void sendErrorToListeners(String error, @Nullable Exception exception) {
        for (ValloxEventListener listener : listeners) {
            try {
                listener.errorOccurred(error, exception);
            } catch (Exception e) {
                logger.error("Event listener invoking error", e);
            }
        }
    }

    /**
     * Put telegram into send queue and make sure job is scheduled to handle send queue.
     *
     * @param telegram the telegram to put to queue
     */
    @SuppressWarnings("null")
    @Override
    public void sendTelegram(Telegram telegram) {
        sendQueue.add(new SendQueueItem(telegram));
        if (sendQueueHandler == null || sendQueueHandler.isCancelled()) {
            sendQueueHandler = scheduler.scheduleWithFixedDelay(handleSendQueue, 0, 500, TimeUnit.MILLISECONDS);
            logger.debug("Send queue handler started");
        }
    }

    /**
     * Send one command or poll telegram from send queue
     */
    Runnable handleSendQueue = () -> {
        if (suspendTraffic || sendQueue.isEmpty()) {
            return;
        }
        SendQueueItem queueItem = sendQueue.removeFirst();
        Telegram telegram = queueItem.getTelegram();
        switch (telegram.state) {
            case POLL:
                writeToOutputStream(telegram);
                break;
            case COMMAND:
                if (queueItem.retry()) {
                    if (telegram.getCheksum() == ackByte) {
                        waitForAckByte = false;
                        sendTelegramToListeners(new Telegram(TelegramState.ACK, ackByte));
                    } else {
                        waitForAckByte = true;
                        writeToOutputStream(telegram);
                        sendQueue.addFirst(queueItem);
                    }
                } else {
                    sendErrorToListeners("Ack byte not received for telegram: " + telegram.toString(), null);
                }
                break;
            default:
                logger.debug("Unknown telegram in send queue: {}", telegram.state);
                break;
        }
    };

    /**
     * Write telegram bytes to output stream
     *
     * @param telegram the telegram to write
     */
    @SuppressWarnings("null")
    public void writeToOutputStream(Telegram telegram) {
        if (outputStream != null) {
            try {
                outputStream.write(telegram.bytes);
                outputStream.flush();
                logger.debug("Wrote {}", telegram.toString());
            } catch (IOException e) {
                sendErrorToListeners("Write to output stream failed, " + e.getMessage(), e);
            }
        } else {
            logger.debug("Output stream is null");
        }
    }
}
