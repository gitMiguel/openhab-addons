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
package org.openhab.binding.vallox.internal.cache;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Object to put into {@link ChacheMap}.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class CacheObject {
    private long time;
    private byte value;

    public CacheObject(byte value) {
        this.time = System.currentTimeMillis();
        this.value = value;
    }

    /**
     * Check if value has expired
     *
     * @return True if cached value is expired
     */
    public boolean isExpired() {
        return (time + 600000 < System.currentTimeMillis());
    }

    /**
     * Get time when value was cached
     *
     * @return Time in milliseconds when value was cached
     */
    long getTime() {
        return time;
    }

    /**
     * Get value
     *
     * @return value
     */
    public byte getValue() {
        return value;
    }
}
