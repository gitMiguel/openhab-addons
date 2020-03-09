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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.ZonedDateTime;

import org.apache.commons.net.telnet.TelnetClient;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ipupdater.config.TelewellConfig;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv4Address;
import org.openhab.binding.ipupdater.internal.ipchecker.Ipv6Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Miika Jukka - Initial contribution
 *
 */
public class TelewellClientHandler extends AbstractClientHandler {

    private final Logger logger = LoggerFactory.getLogger(TelewellClientHandler.class);
    private TelewellConfig config;

    private TelnetClient telnet = new TelnetClient();
    private InputStream in;
    private PrintStream out;
    private String prompt = "#";

    public TelewellClientHandler(Thing thing) {
        super(thing);
        this.config = getConfigAs(TelewellConfig.class);
    }

    @Override
    public void updateConfig() {
        this.config = getConfigAs(TelewellConfig.class);
    }

    @Override
    public void ipv4Changed(@Nullable Ipv4Address address) {
        try {
            // Connect to the specified server
            telnet.connect(config.getAddress(), 23);

            // Get input and output stream references
            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());
            logger.debug("Connected to {}", config.getAddress());

            // Log the user in
            readUntil("login: ");
            write(config.getUsername());
            readUntil("Password: ");
            write(config.getPassword());

            // Advance to command line
            readUntil("Enter the option(0-10):");
            logger.debug("Logged in");

            write("0");
            readUntil(prompt);

            // Send commands
            sendCommands();

            updateState(CHANNEL_TELEWELL_LASTUPDATE, new DateTimeType(ZonedDateTime.now()));
        } catch (Exception e) {
            logger.debug("Exception while communicating: ", e);
        } finally {
            if (telnet.isConnected()) {
                disconnect();
            }
        }
    }

    private void sendCommands() throws IOException {
        // Forward http(s) traffic between usb0 and br0
        if (!sendCommand("ip6tables -S").contains("-A FORWARD -p tcp -m tcp --dport 443 -j ACCEPT")) {
            sendCommand("ip6tables -I FORWARD 1 -p tcp --dport 443 -j ACCEPT");
        }
        if (!sendCommand("ip6tables -S").contains("-A FORWARD -p tcp -m tcp --dport 80 -j ACCEPT")) {
            sendCommand("ip6tables -I FORWARD 1 -p tcp --dport 80 -j ACCEPT");
        }

        // Get current ebtables rules
        String ebtables = sendCommand("ebtables -L");

        // Drop http(s) traffic in bridge br0. Send these commands first so order of rules in ebtables goes right
        if (!ebtables.contains("-p 0x86dd -o eth0.+ --ip6-proto 6 --ip6-dport 443 -j DROP")) {
            sendCommand("ebtables -I OUTPUT 1 -o eth0.+ --proto 86DD --ip6-proto 6 --ip6-dport 443 -j DROP");
        }
        if (!ebtables.contains("-p 0x86dd -o eth0.+ --ip6-proto 6 --ip6-dport 80 -j DROP")) {
            sendCommand("ebtables -I OUTPUT 1 -o eth0.+ --proto 86DD --ip6-proto 6 --ip6-dport 80 -j DROP");
        }

        // Allow http(s) traffic only to single physical port eth0.2.
        if (!ebtables.contains("-p 0x86dd -o eth0.2 --ip6-proto 6 --ip6-dport 443 -j ACCEPT")) {
            sendCommand("ebtables -I OUTPUT 1 -o eth0.2 --proto 86DD --ip6-proto 6 --ip6-dport 443 -j ACCEPT");
        }
        if (!ebtables.contains("-p 0x86dd -o eth0.2 --ip6-proto 6 --ip6-dport 80 -j ACCEPT")) {
            sendCommand("ebtables -I OUTPUT 1 -o eth0.2 --proto 86DD --ip6-proto 6 --ip6-dport 80 -j ACCEPT");
        }

        // Add route for local ipv6 addresses.
        if (!sendCommand("ip -6 route show").contains("fc00::/7")) {
            sendCommand("ip -6 route add fc00::/7 dev br0");
        }
    }

    /**
     * Read InputStream until supplied pattern is found
     *
     * @param pattern - Stop when found
     * @throws IOException
     */
    private String readUntil(String pattern) throws IOException {
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuffer sb = new StringBuffer();
        char ch = (char) in.read();
        long timeout = System.currentTimeMillis() + 2000;
        while (true) {
            sb.append(ch);
            if (ch == lastChar) {
                if (sb.toString().endsWith(pattern)) {
                    logger.trace("Received:\n{}", sb.toString());
                    return sb.toString();
                } else if (timeout > System.currentTimeMillis()) {
                    String msg = sb.toString();
                    msg = msg.replace("\n", "").replace("\r", "");
                    throw new IOException("Read timeout. Pattern didn't match. Received last: " + msg);
                }
            }
            ch = (char) in.read();
        }
    }

    /**
     * Write to {@link PrintStream} from current telnet connection
     *
     * @param value - string to write
     * @throws IOException
     */
    private void write(String value) throws IOException {
        try {
            out.println(value);
            out.flush();
            logger.trace("Wrote: {}", value);
        } catch (Exception e) {
            throw new IOException("Error while writing command: ", e);
        }
    }

    /**
     * Send command to server
     *
     * @param command - String to send
     * @return null - If error was cached, else the reply from server
     * @throws IOException
     */
    private String sendCommand(String command) throws IOException {
        try {
            write(command);
            return readUntil(prompt);
        } catch (Exception e) {
            throw new IOException("Error while sending command: ", e);
        }
    }

    /**
     * Disconnect
     */
    private void disconnect() {
        try {
            telnet.disconnect();
            logger.debug("Disconnected");
        } catch (Exception e) {
            logger.error("Error while disconnecting: ", e);
        }
    }

    @Override
    public void ipv6Changed(@Nullable Ipv6Address address) {
        // TODO Auto-generated method stub
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof OnOffType) {
            switch (channelUID.getId()) {
                case CHANNEL_TELEWELL_FORCEUPDATE:
                    ipv4Changed(null);
                    updateState(CHANNEL_TELEWELL_FORCEUPDATE, OnOffType.OFF);
            }
        }
    }
}
