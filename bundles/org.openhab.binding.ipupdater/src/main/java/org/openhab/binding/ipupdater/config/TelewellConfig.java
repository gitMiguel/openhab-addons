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
package org.openhab.binding.ipupdater.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
@NonNullByDefault
public class TelewellConfig {

    private String username = "";
    private String password = "";
    private String address = "";

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }
}
