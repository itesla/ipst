/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.server.message.encoder;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import eu.itesla_project.online.server.message.StatesWithSecurityRulesResultSynthesisMessage;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class StatesWithSecurityRulesResultSynthesisMessageEncoder
        implements Encoder.Text<StatesWithSecurityRulesResultSynthesisMessage> {

    @Override
    public String encode(StatesWithSecurityRulesResultSynthesisMessage message) throws EncodeException {
        return message.toJson();
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

}
