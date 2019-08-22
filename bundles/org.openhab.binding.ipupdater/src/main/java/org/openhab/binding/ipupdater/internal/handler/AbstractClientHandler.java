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
package org.openhab.binding.ipupdater.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
@NonNullByDefault
public abstract class AbstractClientHandler extends BaseThingHandler implements IpStatusListener {

    private final Logger logger = LoggerFactory.getLogger(AbstractClientHandler.class);

    @Nullable
    private Bridge bridge;

    public AbstractClientHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing handler: {}", this.getClass().getSimpleName());
        this.bridge = getBridge();
        updateConfig();
        if (registerListener()) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing handler: {}", this.getClass().getSimpleName());
        unregisterListener();
        updateStatus(ThingStatus.OFFLINE);
    }

    public abstract void updateConfig();

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        ThingStatus bridgeStatus = bridgeStatusInfo.getStatus();
        if (bridgeStatus == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        } else if (bridgeStatus == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.UNKNOWN);
        }
    }

    public boolean registerListener() {
        if (bridge != null) {
            BridgeListener listener = (BridgeListener) this.bridge.getHandler();
            if (listener != null) {
                listener.registerIpStatusListener(this);
                return true;
            }
        }
        this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                "Handler registration failed. Bridge not initialized.");
        return false;

    }

    public void unregisterListener() {
        BridgeListener listener = getListener(bridge);
        if (listener != null) {
            listener.unregisterIpStatusListener(this);
            return;
        }
        logger.debug("Unregister listener failed");
    }

    private @Nullable BridgeListener getListener(@Nullable Bridge bridge) {
        BridgeListener listener;
        if (bridge != null) {
            listener = (BridgeListener) bridge.getHandler();
            if (listener != null) {
                return listener;
            }
            logger.debug("Bridge listener null!");
            return null;
        }
        logger.debug("Bridge null!");
        return null;
    }

    public String getAddressFromBridge(Integer ipv4or6) {
        BridgeListener listener = getListener(bridge);
        if (listener != null) {
            return listener.getAddressFromBridge(ipv4or6);
        }
        return "";
    }
}
