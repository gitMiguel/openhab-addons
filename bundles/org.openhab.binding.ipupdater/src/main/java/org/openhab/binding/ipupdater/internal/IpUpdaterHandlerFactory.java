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
package org.openhab.binding.ipupdater.internal;

import static org.openhab.binding.ipupdater.internal.IpUpdaterBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.ipupdater.internal.handler.ArkkuClientHandler;
import org.openhab.binding.ipupdater.internal.handler.DyfiClientHandler;
import org.openhab.binding.ipupdater.internal.handler.IpUpdaterBridgeHandler;
import org.openhab.binding.ipupdater.internal.handler.TelewellClientHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link IpUpdaterHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Miika Jukka - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.ipupdater")
@NonNullByDefault()
public class IpUpdaterHandlerFactory extends BaseThingHandlerFactory {

    private @NonNullByDefault({}) HttpClient httpClient;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ARKKU);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DYFI);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_TELEWELL);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            return new IpUpdaterBridgeHandler((Bridge) thing, httpClient);
        } else if (thingTypeUID.equals(THING_TYPE_ARKKU)) {
            return new ArkkuClientHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_DYFI)) {
            return new DyfiClientHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_TELEWELL)) {
            return new TelewellClientHandler(thing);
        }
        return null;
    }

    @Reference
    protected void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    protected void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = null;
    }
}
