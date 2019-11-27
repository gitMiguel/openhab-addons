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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;

/**
 * The {@link ConnectorFactory} implements factory class to create connections to vallox.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class ConnectionFactory {

    public static ValloxConnection getConnector(ThingTypeUID type, SerialPortManager portManager) throws IOException {
        if (THING_TYPE_VALLOX_IP.equals(type)) {
            return new ValloxIpConnection();
        } else if (THING_TYPE_VALLOX_SERIAL.equals(type)) {
            return new ValloxSerialConnection(portManager);
        } else {
            throw new IOException(String.format("Unknown connection type for thing %s", type));
        }
    }
}
