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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.telegram.Telegram;

/**
 * This interface defines methods for communication with Vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public interface ValloxConnector {

    /**
     * Connect to vallox.
     *
     * @param config Vallox configuration
     * @throws IOException if connection fails
     **/
    void connect(ValloxConfiguration config) throws IOException;

    /**
     * Return true if connected.
     */
    boolean isConnected();

    /**
     * Closes the connection.
     **/
    void close();

    /**
     * Add listener.
     *
     * @param listener the listener to add
     */
    void addListener(ValloxEventListener listener);

    /**
     * Remove listener.
     *
     * @param listener the listener to remove
     */
    void removeListener(ValloxEventListener listener);

    /**
     * Put telegram to send queue.
     *
     * @param telegram the telegram
     */
    void sendTelegram(Telegram telegram);
}
