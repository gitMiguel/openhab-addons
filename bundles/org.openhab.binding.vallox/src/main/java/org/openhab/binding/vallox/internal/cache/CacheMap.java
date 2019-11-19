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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for caching channel values.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class CacheMap {

    private final Logger logger = LoggerFactory.getLogger(CacheMap.class);

    private Map<Byte, CacheObject> cacheMap;

    public CacheMap() {
        cacheMap = Collections.synchronizedMap(new HashMap<Byte, CacheObject>());
    }

    public void clear() {
        cacheMap.clear();
    }

    public void put(Byte channelVar, CacheObject cacheObject) {
        logger.trace("Variable '{}' cached", channelVar);
        cacheMap.put(channelVar, cacheObject);
    }

    public boolean contains(Byte channelVar) {
        return cacheMap.containsKey(channelVar);
    }

    public boolean containsTemperatures() {
        if (cacheMap.containsKey((byte) 0x34) && cacheMap.containsKey((byte) 0x32) && cacheMap.containsKey((byte) 0x33)
                && cacheMap.containsKey((byte) 0x35)) {
            return true;
        }
        return false;
    }

    public boolean containsCO2() {
        if (cacheMap.containsKey((byte) 0x2B) && cacheMap.containsKey((byte) 0x2C)) {
            return true;
        }
        return false;
    }

    public boolean containsCO2SetPoint() {
        if (cacheMap.containsKey((byte) 0xB3) && cacheMap.containsKey((byte) 0xB4)) {
            return true;
        }
        return false;
    }

    public boolean isExpired(Byte channelVar) {
        return cacheMap.get(channelVar).isExpired();
    }

    public Byte getValue(Byte channelVar) {
        return cacheMap.get(channelVar).getValue();
    }

    public long getTime(Byte channelVar) {
        return cacheMap.get(channelVar).getTime();
    }
}
