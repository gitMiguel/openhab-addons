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
 * The {@link ValloxConnection} is responsible for creating connection to vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public interface ValloxConnection {

    /**
     * Connect to vallox.
     **/
    public void connect(ValloxConfiguration config) throws IOException;

    /**
     * Return true if connected.
     */
    public boolean isConnected();

    /**
     * Closes the connection.
     **/
    public void close();

    /**
     * Add listener
     */
    public void addListener(ValloxListener listener);

    /**
     * Remove listener
     */
    public void removeListener(ValloxListener listener);

    /**
     * send Telegram
     */
    public void sendTelegram(Telegram telegram);

    /**
     * Send command
     *
     * @throws InterruptedException
     */
    public void sendCommand(Telegram telegram);

}