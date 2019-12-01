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
package org.openhab.binding.vallox.internal.mapper;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Class for channels holding boolean values.
 *
 * @author Miika Jukka - Initial contribution
 */
@NonNullByDefault
public class MultipleValueChannel extends ValloxChannel {

    private Collection<String> subChannels;

    /**
     * Create new instance.
     *
     * @param variable channel as byte
     * @param channelList the list of sub channels
     */
    public MultipleValueChannel(byte variable, Collection<String> subChannels) {
        super(variable);
        this.subChannels = subChannels;
    }

    @Override
    public Collection<String> getSubChannels() {
        return subChannels;
    }
}
