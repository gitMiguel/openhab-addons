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
 * Comparable class that hold Ipv4 and Ipv6 addresses
 *
 * @author Miika Jukka - Initial contribution
 */
public class Ipv6Address implements Comparable<Ipv6Address> {
    public String ipv6address = "";

    public Ipv6Address(String ipv6address) {
        this.ipv6address = ipv6address;
    }

    public Ipv6Address() {
        this.ipv6address = "";
    }

    @Override
    public String toString() {
        return ipv6address;
    }

    @Override
    public int compareTo(Ipv6Address other) {
        return ipv6address.equals(other.ipv6address) ? 0 : 1;
    }
}
