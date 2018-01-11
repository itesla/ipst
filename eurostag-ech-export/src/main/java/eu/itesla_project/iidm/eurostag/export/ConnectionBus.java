/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Terminal;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ConnectionBus {

    public static ConnectionBus fromTerminal(Terminal t, EurostagEchExportConfig config, EurostagFakeNodes fakeNodes) {
        if (config.isNoSwitch()) {
            Bus bus = t.getBusView().getBus();
            boolean connected;
            if (bus != null) {
                connected = true;
            } else {
                connected = false;
                bus = t.getBusView().getConnectableBus();
                if (bus == null) {
                    // fake node...
                    String fakeNode = (fakeNodes != null) ? fakeNodes.getEsgIdAndIncCounter(t) : null;
                    return new ConnectionBus(false, fakeNode);
                }
            }
            return new ConnectionBus(connected, bus.getId());
        } else {
            Bus bus = t.getBusBreakerView().getBus();
            boolean connected;
            if (bus != null) {
                connected = true;
            } else {
                connected = false;
                bus = t.getBusBreakerView().getConnectableBus();
                if (bus == null) {
                    // fake node...
                    String fakeNode = (fakeNodes != null) ? fakeNodes.getEsgIdAndIncCounter(t) : null;
                    return new ConnectionBus(false, fakeNode);
                }
            }
            return new ConnectionBus(connected, bus.getId());
        }
    }

    private final boolean connected;

    private final String id;

    public ConnectionBus(boolean connected, String id) {
        this.connected = connected;
        this.id = id;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getId() {
        return id;
    }
}
