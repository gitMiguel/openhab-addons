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
package org.openhab.binding.ipupdater.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
@NonNullByDefault
public class BridgeConfig {

    private int updateInterval = 10;
    private String ipv4server = "0::1";
    private String ipv6server = "0::1";
    private boolean ipv6enabled = false;

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public String getIpv4server() {
        return ipv4server;
    }

    public void setIpv4server(String ipv4server) {
        this.ipv4server = ipv4server;
    }

    public String getIpv6server() {
        return ipv6server;
    }

    public void setIpv6server(String ipv6server) {
        this.ipv6server = ipv6server;
    }

    public boolean isIpv6enabled() {
        return ipv6enabled;
    }
}
