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
package org.openhab.binding.ipupdater.internal.ipchecker;

import org.apache.http.conn.util.InetAddressUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
public class Ipv4Checker {

    private Ipv4Address address;
    private Exception exception;

    public Ipv4Checker() {
    }

    public static Ipv4Address getIpv4Address(String server) throws Exception {
        String response = sendRequest(server);
        if (!InetAddressUtils.isIPv4Address(response)) {
            throw new IllegalArgumentException("Address '" + response + "' is not valid");
        }
        return new Ipv4Address(response);
    }

    private static String sendRequest(String url) throws Exception {
        HttpClient client = new HttpClient();
        client.start();
        ContentResponse response = client.GET(url);
        return response.getContentAsString();
    }

    public Ipv4Address getAddress() {
        return address;
    }

    public void setAddress(Ipv4Address address) {
        this.address = address;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
