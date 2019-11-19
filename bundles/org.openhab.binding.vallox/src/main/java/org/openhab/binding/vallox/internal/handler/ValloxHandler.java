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
package org.openhab.binding.vallox.internal.handler;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.vallox.internal.cache.CacheMap;
import org.openhab.binding.vallox.internal.cache.CacheObject;
import org.openhab.binding.vallox.internal.configuration.ValloxConfiguration;
import org.openhab.binding.vallox.internal.connection.ConnectionFactory;
import org.openhab.binding.vallox.internal.connection.ValloxConnection;
import org.openhab.binding.vallox.internal.connection.ValloxListener;
import org.openhab.binding.vallox.internal.mapper.ChannelMapper;
import org.openhab.binding.vallox.internal.mapper.ValloxChannel;
import org.openhab.binding.vallox.internal.telegram.Parser;
import org.openhab.binding.vallox.internal.telegram.Telegram;
import org.openhab.binding.vallox.internal.telegram.TelegramFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ValloxHandler} is responsible for handling commands, which
 * are sent to one of the channels
 *
 * @author Hauke Fuhrmann - Initial contribution
 * @author Miika Jukka - Rewrite
 */
@NonNullByDefault
public class ValloxHandler extends BaseThingHandler implements ValloxListener {

    private final Logger logger = LoggerFactory.getLogger(ValloxHandler.class);

    private final CacheMap cache = new CacheMap();
    private @Nullable ValloxConnection connection;
    private ValloxConfiguration config;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> watchDog;
    private boolean reconnect = false;
    private SerialPortManager portManager;

    public ValloxHandler(Thing thing, SerialPortManager portManager) {
        super(thing);
        config = getConfigAs(ValloxConfiguration.class);
        this.portManager = portManager;
    }

    /**
     * Dispose binding
     */
    @Override
    public void dispose() {
        logger.debug("Disposing vallox");
        if (watchDog != null) {
            watchDog.cancel(true);
            watchDog = null;
        }
        if (pollingJob != null) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        closeConnection();
    }

    /**
     * Initialize binding
     */
    @Override
    public void initialize() {
        logger.debug("Initializing Vallox SE binding");
        updateStatus(ThingStatus.UNKNOWN);
        cache.clear();
        try {
            this.connection = ConnectionFactory.getConnector(thing.getThingTypeUID(), portManager);
            this.config = getConfigAs(ValloxConfiguration.class);
        } catch (Exception ex) {
            String message = "Failed to initialize: ";
            logger.debug(message, ex);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, ex.toString());
            return;
        }
        if (watchDog == null || watchDog.isCancelled()) {
            watchDog = scheduler.scheduleWithFixedDelay(() -> {
                if (reconnect) {
                    reconnect = false;
                    closeConnection();
                }
                connect();
            }, 0, 10, TimeUnit.SECONDS);
        }
        startPollingJob();
    }

    @SuppressWarnings("null")
    private void connect() {
        if (!isConnected()) {
            updateStatus(ThingStatus.UNKNOWN);
            logger.debug("Connecting to Vallox");
            try {
                connection.addListener(this);
                connection.connect(config);
                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                logger.debug("Connection failed: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        } else {
            logger.trace("Connection already open");
        }
    }

    @SuppressWarnings("null")
    private void closeConnection() {
        logger.debug("Closing connection");
        if (isConnected()) {
            connection.removeListener(this);
            connection.close();
        }
    }

    /**
     * Check if connection is initialized and open
     */
    private boolean isConnected() {
        if (connection != null) {
            if (connection.isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle received commands
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (this.thing.getStatus() == ThingStatus.ONLINE && isConnected()) {
            String channelID = channelUID.getId();
            Byte channelVar = ChannelMapper.getVariable(channelID);
            if (command instanceof RefreshType) {
                handleRefreshTypeCommand(command, channelID, channelVar);
            } else if (command instanceof DecimalType) {
                handleDecimalCommand((DecimalType) command, channelID, channelVar);
            } else if (command instanceof OnOffType) {
                handleOnOffCommand(command, channelID, channelVar);
            } else {
                logger.debug("Unsupported command '{}'", command);
            }
        }
    }

    /**
     * Handle refresh type command. CO2 value and setpoint are 16bit values.
     * High and low bytes are handled separately
     */
    private void handleRefreshTypeCommand(Command command, String channelID, Byte channelVar) {

        if (cache.contains(channelVar) && !cache.isExpired(channelVar)) {
            logger.debug("Cache hasn't expired yet. Updating state with cached value for channel: {}", channelID);
            ValloxChannel valloxChannel = ChannelMapper.getValloxChannel(channelID);
            updateState(channelID, valloxChannel.convertToState(cache.getValue(channelVar)));
            return;
        } else if (channelID.equals("Setting#CO2SetPoint")) {
            sendPoll(ChannelMapper.getVariable("Setting#CO2SetPointHigh"));
            sendPoll(ChannelMapper.getVariable("Setting#CO2SetPointLow"));
        } else if (channelID.equals("Status#CO2")) {
            sendPoll(ChannelMapper.getVariable("Status#CO2High"));
            sendPoll(ChannelMapper.getVariable("Status#CO2Low"));
        } else {
            sendPoll(channelVar);
        }
    }

    /**
     * Handle OnOff type commands
     */
    private void handleOnOffCommand(Command command, String channelID, Byte channelVar) {

        String superChannel = ChannelMapper.getChannelForVariable(channelVar);

        if (!cache.contains(channelVar)) {
            logger.debug("Couldn't handle OnOff command because cache doesn't contain any value for channel '{}'",
                    superChannel);
            return;
        }
        byte cachedValue = cache.getValue(channelVar);
        BitSet bits = BitSet.valueOf(new byte[] { cachedValue });
        switch (superChannel) {
            case "select":
                switch (channelID) {
                    // send the first 4 bits of the Select byte; others are read only
                    // 1 1 1 1 1 1 1 1
                    // | | | | | | | |
                    // | | | | | | | +- 0 Power state
                    // | | | | | | +--- 1 CO2 Adjust state
                    // | | | | | +----- 2 %RH adjust state
                    // | | | | +------- 3 Heating state
                    // | | | +--------- 4 Filter guard indicator
                    // | | +----------- 5 Heating indicator
                    // | +------------- 6 Fault indicator
                    // +--------------- 7 service reminder
                    case "Setting#PostHeatingState":
                        bits.set(3, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#HumidityAdjustState":
                        bits.set(2, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#CO2AdjustState":
                        bits.set(1, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#PowerState":
                        bits.set(0, (command == OnOffType.ON) ? true : false);
                        break;
                    default:
                        logger.debug("Unsupported OnOff type channel '{}' with superchannel 'select'", channelID);
                        return;
                }
                break;
            case "program1":
                switch (channelID) {
                    // 1 1 1 1 1 1 1 1
                    // | | | | _______
                    // | | | | |
                    // | | | | +--- 0-3 set adjustment interval of CO2 and %RH in minutes
                    // | | | |
                    // | | | |
                    // | | | |
                    // | | | +--------- 4 automatic RH basic level seeker state
                    // | | +----------- 5 boost switch mode (1=boost, 0( (byte)fireplace)
                    // | +------------- 6 radiator type 0( (byte)electric, 1( (byte)water
                    // +--------------- 7 cascade adjust 0( (byte)off, 1( (byte)on
                    case "Setting#AutomaticHumidityLevelSeekerState":
                        bits.set(4, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#BoostSwitchMode":
                        bits.set(5, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#RadiatorType":
                        bits.set(6, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#CascadeAdjust":
                        bits.set(7, (command == OnOffType.ON) ? true : false);
                        break;
                    case "Setting#AdjustmentIntervalMinutes":
                        byte temp = (byte) (Integer.parseInt(command.toString()) & 0x0F);
                        BitSet aim = BitSet.valueOf(new byte[] { temp });
                        bits.set(0, aim.get(0));
                        bits.set(1, aim.get(1));
                        bits.set(2, aim.get(2));
                        bits.set(3, aim.get(3));
                        break;
                    default:
                        logger.debug("Unsupported OnOff type channel '{}' with superchannel 'program1'", channelID);
                        return;
                }
                break;
            case "flags5":
                switch (channelID) {
                    // 1 1 1 1 1 1 1 1
                    // | | | | | | | |
                    // | | | | | | | +- 0
                    // | | | | | | +--- 1
                    // | | | | | +----- 2
                    // | | | | +------- 3
                    // | | | +--------- 4
                    // | | +----------- 5
                    // | +------------- 6
                    // +--------------- 7 Preheating state
                    case "Setting#PreHeatingState":
                        bits.set(7, (command == OnOffType.ON) ? true : false);
                        break;
                    default:
                        logger.debug("Unsupported OnOff type channel '{}' with superchannel 'flags5'", channelID);
                        return;
                }
                break;
            case "program2":
                switch (channelID) {
                    // 1 1 1 1 1 1 1 1
                    // | | | | | | | |
                    // | | | | | | | +- 0 Maximum speed limit mode
                    // | | | | | | +--- 1
                    // | | | | | +----- 2
                    // | | | | +------- 3
                    // | | | +--------- 4
                    // | | +----------- 5
                    // | +------------- 6
                    // +--------------- 7
                    case "Setting#MaxSpeedLimitMode":
                        bits.set(0, (command == OnOffType.ON) ? true : false);
                        break;
                    default:
                        logger.debug("Unsupported OnOff type channel '{}' with superchannel 'program2'", channelID);
                        return;
                }
                break;
            default:
                logger.debug("Unsupported command '{}' to channel '{}' received", command, channelID);
                break;
        }
        // Ensure that byte array length is always 8 even if all bits are 0.
        byte[] cmd = Arrays.copyOf(bits.toByteArray(), 8);
        if (cmd != null) {
            sendCommand(channelVar, cmd[0]);
        }
    }

    /**
     * Handle decimal type commands
     */
    private void handleDecimalCommand(DecimalType command, String channelID, Byte channelVar) {

        if (channelID.equals("Setting#CO2SetPoint")) {
            int commandValue = command.intValue();
            byte lowByte = (byte) (commandValue & 0xFF);
            byte highByte = (byte) ((commandValue >>> 8) & 0xFF);
            sendCommand(ChannelMapper.getVariable("Setting#CO2SetPointHigh"), highByte);
            sendCommand(ChannelMapper.getVariable("Setting#CO2SetPointLow"), lowByte);
            return;
        }
        if (channelID.equals("Setting#AdjustmentIntervalMinutes")) {
            handleOnOffCommand(command, channelID, channelVar);
            return;
        }
        ValloxChannel valloxChannel = ChannelMapper.getValloxChannel(channelID);
        sendCommand(channelVar, valloxChannel.convertFromState(command.byteValue()));
    }

    /**
     * Get a collection containing all linked channels of a thing
     */
    private Collection<String> linkedChannels() {
        return thing.getChannels().stream().map(Channel::getUID).map(ChannelUID::getId).filter(this::isLinked)
                .collect(Collectors.toList());
    }

    /**
     * Ensure that all linked channels have been polled at least once and has a value.
     * OnOffType or DecimalType commands needs a cached value.
     */
    public void startPollingJob() {
        pollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    try {
                        logger.debug("Vallox heartbeat");
                        for (String channel : linkedChannels()) {
                            Byte channelVar = ChannelMapper.getVariable(channel);
                            if (channelVar != 00 && (!cache.contains(channelVar) || cache.isExpired(channelVar))) {
                                sendPoll(channelVar);
                                logger.debug("Refreshing channel: {}", channel);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Exception sending heartbeat poll: ", ex);
                        Thread.currentThread().interrupt();
                        reconnect = true;
                    }
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Send poll telegram to connection handler.
     */
    @SuppressWarnings("null")
    public void sendPoll(byte channelVar) {
        Telegram telegram = TelegramFactory.createPoll(config.panelNumber, channelVar);
        connection.sendTelegram(telegram);
    }

    /**
     * Send command telegram to connection handler.
     */
    @SuppressWarnings("null")
    public void sendCommand(byte variable, byte value) {
        Telegram telegram = TelegramFactory.createCommand(config.panelNumber, variable, value);
        connection.sendCommand(telegram);
    }

    /**
     * Handle telegram received from connection handler.
     */
    @Override
    public void telegramReceived(Telegram telegram) {
        if (logger.isTraceEnabled()) {
            logger.trace("{} {}", telegram.stateDetails(), telegram.toString());
        }
        switch (telegram.state) {
            case ACK:
                logger.debug("Received ack byte '{}'", telegram.toString());
                break;
            case OK:
                String channelID = ChannelMapper.getChannelForVariable(telegram.bytes[3]);
                cache.put(telegram.bytes[3], new CacheObject(telegram.bytes[4]));
                Map<String, State> channelsToUpdate = Parser.process(channelID, telegram.bytes[4], cache);
                channelsToUpdate.forEach((channel, state) -> {
                    updateState(channel, state);
                });
                break;
            case CRC_ERROR:
            case EMPTY:
            case NOT_DOMAIN:
            case NOT_FOR_US:
            case CORRUPTED:
                logger.debug("{} {}", telegram.stateDetails(), telegram.toString());
                break;
            case RESUME:
                // TODO
                break;
            case SUSPEND:
                // TODO
                break;
            default:
                logger.debug("Unknown telegram received");
                break;
        }
    }

    /**
     * Handle error received from connection handler
     */
    @Override
    public void errorOccurred(String error) {
        logger.debug("Error '{}' occurred, reconnecting", error);
        reconnect = true;
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error);
    }
}
