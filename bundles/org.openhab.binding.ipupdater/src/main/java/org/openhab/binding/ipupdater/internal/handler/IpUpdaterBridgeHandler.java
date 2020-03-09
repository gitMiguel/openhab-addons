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
package org.openhab.binding.ipupdater.internal.handler;

import static org.openhab.binding.ipupdater.internal.IpUpdaterBindingConstants.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.ipupdater.config.BridgeConfig;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv4Address;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv4Checker;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv6Address;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv6Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
@NonNullByDefault
public class IpUpdaterBridgeHandler extends BaseBridgeHandler implements BridgeListener {

    private final Logger logger = LoggerFactory.getLogger(IpUpdaterBridgeHandler.class);
    private List<IpStatusListener> listeners = new CopyOnWriteArrayList<>();
    private Ipv4Address currentIpv4Address = new Ipv4Address();
    private Ipv6Address currentIpv6Address = new Ipv6Address();
    private BridgeConfig bridgeConfig;
    @Nullable
    private ScheduledFuture<?> pollingJob;
    private HttpClient httpClient;

    private long lastUpdated = 0;
    private long maxUpdateInterval = TimeUnit.DAYS.toMillis(6);

    public IpUpdaterBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        bridgeConfig = getConfigAs(BridgeConfig.class);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing IpUpdater bridge handler.");
        bridgeConfig = getConfigAs(BridgeConfig.class);
        if (!bridgeConfig.isIpv6enabled()) {
            updateState(CHANNEL_IPV6ADDRESS, UnDefType.UNDEF);
        }
        startPolling();
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing IpUpdater bridge handler");
        stopPolling();
        updateStatus(ThingStatus.OFFLINE);
    }

    @SuppressWarnings("null")
    private void startPolling() {
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 0, bridgeConfig.getUpdateInterval(),
                    TimeUnit.MINUTES);
        }
    }

    Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            Ipv4Address ipv4address = null;
            try {
                ipv4address = Ipv4Checker.getIpv4Address(httpClient, bridgeConfig.getIpv4server());
                if ((ipv4address.compareTo(currentIpv4Address) > 0) || (maxUpdateIntervalReached())) {
                    for (IpStatusListener listener : listeners) {
                        listener.ipv4Changed(ipv4address);
                        lastUpdated = System.currentTimeMillis();
                    }
                    currentIpv4Address = ipv4address;
                    lastUpdated = System.currentTimeMillis();
                }
                updateState(CHANNEL_IPV4ADDRESS, new StringType(ipv4address.toString()));
            } catch (Exception e) {
                logger.debug("Exception caught", e);
            }
            if (bridgeConfig.isIpv6enabled()) {
                Ipv6Address ipv6address;
                try {
                    ipv6address = Ipv6Checker.getIpv6Address(httpClient, bridgeConfig.getIpv6server());
                    if (ipv6address.compareTo(currentIpv6Address) > 0) {
                        for (IpStatusListener listener : listeners) {
                            listener.ipv6Changed(ipv6address);
                        }
                        currentIpv6Address = ipv6address;
                    }
                    updateState(CHANNEL_IPV6ADDRESS, new StringType(ipv6address.toString()));
                } catch (Exception e) {
                    logger.debug("Exception caught", e);
                }
            }
        }

        /**
         * Check if we should do a force update
         *
         * @return true - If 6 days has passed after last update or it's first run
         */
        private boolean maxUpdateIntervalReached() {
            long maxTime = lastUpdated + maxUpdateInterval;
            return System.currentTimeMillis() > maxTime;
        }
    };

    @SuppressWarnings("null")
    private void stopPolling() {
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            switch (channelUID.getId()) {
                case CHANNEL_BRIDGE_FORCEUPDATE:
                    scheduler.execute(pollingRunnable);
                    updateState(CHANNEL_BRIDGE_FORCEUPDATE, OnOffType.OFF);
            }
        }
    }

    @Override
    public boolean registerIpStatusListener(IpStatusListener ipStatusListener) {
        boolean result = listeners.add(ipStatusListener);
        logger.debug("BridgeListener '{}' registered.", ipStatusListener.toString());
        return result;
    }

    @Override
    public boolean unregisterIpStatusListener(IpStatusListener ipStatusListener) {
        boolean result = listeners.remove(ipStatusListener);
        logger.debug("BridgeListener '{}' removed.", ipStatusListener.toString());
        return result;
    }

    @Override
    public String getAddressFromBridge(Integer ipv4OrIpv6) {
        if (ipv4OrIpv6 == 4) {
            return currentIpv4Address.toString();
        } else if (ipv4OrIpv6 == 6) {
            return currentIpv6Address.toString();
        }
        return "";
    }
}
