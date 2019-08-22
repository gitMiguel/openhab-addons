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

import static org.openhab.binding.ipupdater.internal.IpUpdaterBindingConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ipupdater.config.DyfiConfig;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv4Address;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv6Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
public class DyfiClientHandler extends AbstractClientHandler {

    private final Logger logger = LoggerFactory.getLogger(DyfiClientHandler.class);
    private DyfiConfig config;

    public DyfiClientHandler(Thing thing) {
        super(thing);
        this.config = getConfigAs(DyfiConfig.class);
    }

    @Override
    public void updateConfig() {
        // TODO Auto-generated method stub

    }

    @Override
    public void ipv4Changed(Ipv4Address address) {
        try {
            logger.info("Updating dy.fi IPv4 DNS records...");

            // Create client with credentials @formatter:off
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(config.getUsername(), config.getPassword());

            provider.setCredentials(AuthScope.ANY, credentials);
            HttpClient client = HttpClientBuilder.create()
                                .setDefaultCredentialsProvider(provider)
                                .build();

            URI uri = buildUri(config.getHostName());
            URI uri2 = buildUri(config.getHostName2());

            parseResponse(client.execute(new HttpGet(uri)), config.getHostName());
            parseResponse(client.execute(new HttpGet(uri2)), config.getHostName2());

            updateState(CHANNEL_DYFI_LASTUPDATE, new DateTimeType(Calendar.getInstance()));

        } catch (Exception e) {
            logger.debug("Error updating dy.fi Ipv4 records", e);
        }
    }

    private URI buildUri(String hostname) throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https")
               .setHost("www.dy.fi")
               .setPath("/nic/update")
               .setParameter("hostname", hostname);
        return builder.build();
        // @formatter:on
    }

    @Override
    public void ipv6Changed(@Nullable Ipv6Address address) {
        try {
            logger.info("Updating dy.fi IPV6 DNS records...");
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost("https://www.dy.fi");

            // Login
            List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
            loginParams.add(new BasicNameValuePair("c", "login"));
            loginParams.add(new BasicNameValuePair("email", config.getUsername()));
            loginParams.add(new BasicNameValuePair("password", config.getPassword()));
            loginParams.add(new BasicNameValuePair("submit", "login"));
            post.setEntity(new UrlEncodedFormEntity(loginParams, "UTF-8"));
            logger.trace("List contents: {}", loginParams);
            HttpResponse loginResponse = client.execute(post);
            parseResponse(loginResponse, null);

            // Update first host
            List<NameValuePair> updateFirst = new ArrayList<NameValuePair>();
            updateFirst.add(new BasicNameValuePair("c", "hopt"));
            updateFirst.add(new BasicNameValuePair("hostid", config.getHostId()));
            updateFirst.add(new BasicNameValuePair("publish", ""));
            updateFirst.add(new BasicNameValuePair("url", ""));
            updateFirst.add(new BasicNameValuePair("framed", ""));
            updateFirst.add(new BasicNameValuePair("title", config.getTitle()));
            updateFirst.add(new BasicNameValuePair("mx_dy", ""));
            updateFirst.add(new BasicNameValuePair("mx", config.getTitle()));
            updateFirst.add(new BasicNameValuePair("a_static", ""));
            updateFirst.add(new BasicNameValuePair("aaaa", address.toString()));
            updateFirst.add(new BasicNameValuePair("submit", "save"));
            post.setEntity(new UrlEncodedFormEntity(updateFirst, "UTF-8"));
            logger.trace("List contents: {}", updateFirst);
            HttpResponse updateFirstResponse = client.execute(post);
            parseResponse(updateFirstResponse, config.getTitle() + " " + config.getHostId());

            // Update second host
            List<NameValuePair> updateSecond = new ArrayList<NameValuePair>();
            updateSecond.add(new BasicNameValuePair("c", "hopt"));
            updateSecond.add(new BasicNameValuePair("hostid", config.getHostId2()));
            updateSecond.add(new BasicNameValuePair("publish", ""));
            updateSecond.add(new BasicNameValuePair("url", ""));
            updateSecond.add(new BasicNameValuePair("framed", ""));
            updateSecond.add(new BasicNameValuePair("title", config.getTitle()));
            updateSecond.add(new BasicNameValuePair("mx_dy", ""));
            updateSecond.add(new BasicNameValuePair("mx", config.getTitle()));
            updateSecond.add(new BasicNameValuePair("a_static", ""));
            updateSecond.add(new BasicNameValuePair("aaaa", address.toString()));
            updateSecond.add(new BasicNameValuePair("submit", "save"));
            post.setEntity(new UrlEncodedFormEntity(updateSecond, "UTF-8"));
            logger.trace("List contents: {}", updateSecond);
            HttpResponse updateSecondResponse = client.execute(post);
            parseResponse(updateSecondResponse, config.getTitle() + " " + config.getHostId2());

            // Logout
            List<NameValuePair> logoutParams = new ArrayList<NameValuePair>();
            logoutParams.add(new BasicNameValuePair("c", "logout"));
            post.setEntity(new UrlEncodedFormEntity(logoutParams, "UTF-8"));
            logger.trace("List contents: {}", logoutParams);
            HttpResponse logoutResponse = client.execute(post);
            parseResponse(logoutResponse, null);
        } catch (Exception e) {
            logger.debug("Error updating dy.fi Ipv6 records", e);
        }
    }

    private void parseResponse(HttpResponse response, String logMessage) throws IOException {
        String result = "Response: ";
        int responseCode = response.getStatusLine().getStatusCode();
        if (Integer.valueOf(responseCode).equals(HttpStatus.SC_OK)) {
            result += response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String content = EntityUtils.toString(entity);
                String parsed = StringUtils.substringBetween(content, "<td class=\"msgtd\">", "</td>");
                if (parsed != null) {
                    result += " " + parsed.replaceAll("<[^>]*>", "");
                } else {
                    content = content.replace("\n", "");
                }
                result = DYFI_RESPONSES.getOrDefault(content, result);
                if (logMessage != null) {
                    String msg = " [ " + logMessage + " ] ";
                    result += msg;
                }
            } else {
                logger.debug("Http reponse entity was null");
            }
        } else {
            throw new IOException("Error while sending request: " + response.getStatusLine());
        }
        logger.debug(result);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            switch (channelUID.getId()) {
                case CHANNEL_DYFI_FORCEUPDATE:
                    ipv4Changed(null);
                    ipv6Changed(new Ipv6Address(getAddressFromBridge(6)));
                    updateState(CHANNEL_DYFI_FORCEUPDATE, OnOffType.OFF);
            }
        }
    }
}
