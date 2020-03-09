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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ipupdater.config.ArkkuConfig;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv4Address;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv6Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
@NonNullByDefault
public class ArkkuClientHandler extends AbstractClientHandler {

    private final Logger logger = LoggerFactory.getLogger(ArkkuClientHandler.class);
    private ArkkuConfig config;

    public ArkkuClientHandler(Thing thing) {
        super(thing);
        this.config = getConfigAs(ArkkuConfig.class);
    }

    @Override
    public void updateConfig() {
        // TODO Auto-generated method stub
    }

    @Override
    public void ipv4Changed(@Nullable Ipv4Address address) {
        logger.debug("Updating ip address to arkku.net...");
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost("https://www.arkku.net/api/whitelist-ip");

            // Request parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>(1);
            params.add(new BasicNameValuePair("key", config.getKey()));
            post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            // Execute and get the response.
            CloseableHttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String content = EntityUtils.toString(entity);
                logger.info("Result: {}", content);
            }
            EntityUtils.consume(entity);
            updateState(CHANNEL_ARKKU_LASTUPDATE, new DateTimeType(ZonedDateTime.now()));

        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error updating arkku.net: " + e.getMessage());
            logger.debug("Error updating arkku.net: ", e);
        }
    }

    @Override
    public void ipv6Changed(@Nullable Ipv6Address address) {
        // Not needed
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            switch (channelUID.getId()) {
                case CHANNEL_ARKKU_FORCEUPDATE:
                    ipv4Changed(null);
                    updateState(CHANNEL_ARKKU_FORCEUPDATE, OnOffType.OFF);
            }
        }
    }
}
