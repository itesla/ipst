/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.AtomicLongMap;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.util.Identifiables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class EurostagFakeNodes {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagFakeNodes.class);

    private static int FAKENODELENGTH = 6;

    private static String PREFIX = "FK";

    private final BiMap<String, String> fakeNodesMap;
    final AtomicLongMap<String> countUsesMap;
    private final Network network;

    private static String newEsgId(BiMap<String, String> fakeNodesMap, String iidmId) {
        String esgId = PREFIX + (iidmId.length() > FAKENODELENGTH ? iidmId.substring(0, FAKENODELENGTH)
                : Strings.padEnd(iidmId, FAKENODELENGTH, ' '));
        int counter = 0;
        while (fakeNodesMap.inverse().containsKey(esgId)) {
            String counterStr = Integer.toString(counter++);
            if (counterStr.length() > FAKENODELENGTH) {
                throw new RuntimeException("Renaming fatal error " + iidmId + " -> " + esgId);
            }
            esgId = PREFIX + esgId.substring(PREFIX.length(), PREFIX.length() + FAKENODELENGTH - counterStr.length()) + counterStr;
        }
        return esgId;
    }

    public static EurostagFakeNodes build(Network network, EurostagEchExportConfig config) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(config);

        BiMap<String, String> fakeNodesMap = HashBiMap.create(new HashMap<>());
        AtomicLongMap<String> countUsesMap = AtomicLongMap.create();

        //adds 2 default fake nodes
        fakeNodesMap.put(EchUtil.FAKE_NODE_NAME1, EchUtil.FAKE_NODE_NAME1);
        countUsesMap.getAndIncrement(EchUtil.FAKE_NODE_NAME1);
        fakeNodesMap.put(EchUtil.FAKE_NODE_NAME2, EchUtil.FAKE_NODE_NAME2);
        countUsesMap.getAndIncrement(EchUtil.FAKE_NODE_NAME2);

        Identifiables.sort(network.getVoltageLevels()).stream().map(VoltageLevel::getId).forEach(vlId ->
                fakeNodesMap.put(vlId, newEsgId(fakeNodesMap, vlId)));

        return new EurostagFakeNodes(fakeNodesMap, countUsesMap, network);
    }

    private EurostagFakeNodes(Map<String, String> fakeNodesMap, AtomicLongMap<String> countUsesMap, Network network) {
        this.network = network;
        this.fakeNodesMap = HashBiMap.create(fakeNodesMap);
        this.countUsesMap = countUsesMap;
    }

    public Map<String, String> toMap() {
        return fakeNodesMap;
    }

    public long countUses(String id) {
        return countUsesMap.get(id);
    }

    //the esg nodes ids that are referenced at least once
    public Stream<String> referencedEsgIdsAsStream() {
        return fakeNodesMap.values().stream().filter(esgId -> countUses(esgId) > 0);
    }

    public Stream<String> esgIdsAsStream() {
        return fakeNodesMap.values().stream();
    }

    public String getEsgIdAndIncCounter(Terminal t) {
        return getEsgIdAndIncCounter(t.getVoltageLevel());
    }

    public VoltageLevel getVoltageLevelByEsgId(String esgId) {
        String voltageLevelId = fakeNodesMap.inverse().get(esgId);
        return voltageLevelId != null ? network.getVoltageLevel(voltageLevelId) : null;
    }

    private String getEsgIdAndIncCounter(VoltageLevel vl) {
        String ret = fakeNodesMap.get(vl.getId());
        countUsesMap.getAndIncrement(ret);
        return ret;
    }
}

