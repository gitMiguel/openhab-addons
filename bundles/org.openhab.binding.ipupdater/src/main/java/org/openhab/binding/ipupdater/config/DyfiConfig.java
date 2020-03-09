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
public class DyfiConfig {

    private String dyfiUsername = "";
    private String dyfiPassword = "";
    private String dyfiTitle = "";
    private String dyfiHostName = "";
    private String dyfiHostId = "";
    private String dyfiHostName2 = "";
    private String dyfiHostId2 = "";

    public String getUsername() {
        return dyfiUsername;
    }

    public String getPassword() {
        return dyfiPassword;
    }

    public String getTitle() {
        return dyfiTitle;
    }

    public String getHostName() {
        return dyfiHostName;
    }

    public String getHostId() {
        return dyfiHostId;
    }

    public String getHostName2() {
        return dyfiHostName2;
    }

    public String getHostId2() {
        return dyfiHostId2;
    }
}
