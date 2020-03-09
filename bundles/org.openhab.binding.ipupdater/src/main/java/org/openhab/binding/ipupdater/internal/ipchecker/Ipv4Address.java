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
package org.openhab.binding.ipupdater.internal.ipchecker;

/**
 * Comparable class that holds one ipv4 address
 *
 * @author Miika Jukka - Initial contribution
 */
public class Ipv4Address implements Comparable<Ipv4Address> {

    public String ipv4address = "";

    public Ipv4Address(String ipv4address) {
        this.ipv4address = ipv4address;
    }

    public Ipv4Address() {
        this.ipv4address = "";
    }

    @Override
    public String toString() {
        return ipv4address;
    }

    @Override
    public int compareTo(Ipv4Address other) {
        return ipv4address.equals(other.ipv4address) ? 0 : 1;
    }
}
