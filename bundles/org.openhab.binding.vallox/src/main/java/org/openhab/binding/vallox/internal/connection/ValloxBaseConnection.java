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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This interface defines methods to receive data from vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public abstract class ValloxBaseConnection implements ValloxConnection {

    private final Logger logger = LoggerFactory.getLogger(ValloxBaseConnection.class);

    private final List<ValloxListener> listeners = new ArrayList<>();
    protected byte ackByte;
    protected boolean connected = false;
    protected boolean waitForAckByte = false;
    protected boolean ackByteReceived = false;

    /**
     * Add listener
     */
    @Override
    public synchronized void addListener(ValloxListener listener) {
        if (!listeners.contains(listener)) {
            this.listeners.add(listener);
            logger.debug("Listener registered: {}", listener.toString());
        }
    }

    /**
     * Remove listener
     */
    @Override
    public synchronized void removeListener(ValloxListener listener) {
        this.listeners.remove(listener);
        logger.debug("Listener removed: {}", listener.toString());
    }

    /**
     * Get connection status
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Send received telegram to registered listeners
     */
    public void sendTelegramToListeners(Telegram telegram) {
        for (ValloxListener listener : listeners) {
            try {
                listener.telegramReceived(telegram);
            } catch (Exception e) {
                logger.error("Event listener invoking error", e);
            }
        }
    }

    /**
     * Send error to registered listeners
     */
    public void sendErrorToListeners(String error) {
        for (ValloxListener listener : listeners) {
            try {
                listener.errorOccurred(error);
            } catch (Exception e) {
                logger.error("Event listener invoking error", e);
            }
        }
    }
}
