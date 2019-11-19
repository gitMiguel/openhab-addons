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
package org.openhab.binding.vallox.internal.configuration;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration class for Vallox binding.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ValloxConfiguration {
    public String tcpHost = "";
    public int tcpPort;
    public String serialPort = "";
    public int panelNumber;

    @Override
    public String toString() {
        return "Host=" + tcpHost + ", Port=" + tcpPort + ", Serial port=" + serialPort + ", Panel number="
                + panelNumber;
    }
}
