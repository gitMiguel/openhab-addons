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
package org.openhab.binding.ipupdater.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IpUpdaterBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Miika Jukka - Initial contribution
 */

public class IpUpdaterBindingConstants {

    // Binding ID
    private static final String BINDING_ID = "ipupdater";

    // Bridge
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final String CHANNEL_BRIDGE_FORCEUPDATE = "bridgeForceUpdate";
    public static final String CHANNEL_IPV4ADDRESS = "ipv4address";
    public static final String CHANNEL_IPV6ADDRESS = "ipv6address";

    // IpNumbers
    public static final ThingTypeUID THING_TYPE_IPNUMBERS = new ThingTypeUID(BINDING_ID, "ipnumbers");

    // Dyfi
    public static final ThingTypeUID THING_TYPE_DYFI = new ThingTypeUID(BINDING_ID, "dyfi");
    public static final String CHANNEL_DYFI_FORCEUPDATE = "dyfiForceUpdate";
    public static final String CHANNEL_DYFI_LASTUPDATE = "dyfiLastUpdate";
    public static final Map<String, String> DYFI_RESPONSES;

    // Arkku
    public static final ThingTypeUID THING_TYPE_ARKKU = new ThingTypeUID(BINDING_ID, "arkku");
    public static final String CHANNEL_ARKKU_FORCEUPDATE = "arkkuForceUpdate";
    public static final String CHANNEL_ARKKU_LASTUPDATE = "arkkuLastUpdate";

    // Telewell
    public static final ThingTypeUID THING_TYPE_TELEWELL = new ThingTypeUID(BINDING_ID, "telewell");
    public static final String CHANNEL_TELEWELL_FORCEUPDATE = "telewellForceUpdate";
    public static final String CHANNEL_TELEWELL_LASTUPDATE = "telewellLastUpdate";

    // Dyfi service response map
    static {
        Map<String, String> map = new HashMap<>();
        map.put("abuse", "The service feels YOU are ABUSING it!");
        map.put("badauth", "Authentication failed");
        map.put("nohost", "No hostname given for update, or hostname not yours");
        map.put("notfqdn", "The given hostname is not a valid FQDN");
        map.put("badip", "The client IP address is not valid or permitted");
        map.put("dnserr", "Update failed due to a problem at dy.fi");
        map.put("good", "The update was processed successfully");
        map.put("nochg", "The successful update did not cause a DNS data change");
        DYFI_RESPONSES = Collections.unmodifiableMap(map);
    }
}
