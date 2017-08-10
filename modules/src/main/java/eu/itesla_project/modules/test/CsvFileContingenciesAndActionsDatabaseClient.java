/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.itesla_project.contingency.BranchContingency;
import eu.itesla_project.contingency.Contingency;
import eu.itesla_project.contingency.ContingencyElement;
import eu.itesla_project.contingency.ContingencyImpl;
import eu.itesla_project.contingency.GeneratorContingency;
import eu.itesla_project.iidm.network.Line;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.iidm.network.TieLine;
import eu.itesla_project.modules.contingencies.Action;
import eu.itesla_project.modules.contingencies.ActionPlan;
import eu.itesla_project.modules.contingencies.ActionsContingenciesAssociation;
import eu.itesla_project.modules.contingencies.ConstraintType;
import eu.itesla_project.modules.contingencies.ContingenciesAndActionsDatabaseClient;
import eu.itesla_project.modules.contingencies.Scenario;
import eu.itesla_project.modules.contingencies.Zone;

/**
 * Contingencies and actions database based on CSV file. Can only contain NmK
 * line contingencies.
 * <p>
 * Example:
 * 
 * <pre>
 *#contingency id;line count;line1 id;line2 id;...
 * ...
 * </pre>
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CsvFileContingenciesAndActionsDatabaseClient implements ContingenciesAndActionsDatabaseClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvFileContingenciesAndActionsDatabaseClient.class);

    private final List<ContingencyData> condingency_data = new ArrayList();

    public CsvFileContingenciesAndActionsDatabaseClient(Path file) throws IOException {
        this(new FileInputStream(file.toFile()));
    }

    public CsvFileContingenciesAndActionsDatabaseClient(InputStream input) {
        try {
            Reader ir = new InputStreamReader(input, Charset.defaultCharset());
            try (BufferedReader r = new BufferedReader(ir)) {

                String txt;
                while ((txt = r.readLine()) != null) {
                    if (txt.startsWith("#")) { // comment
                        continue;
                    }
                    if (txt.trim().isEmpty()) {
                        continue;
                    }
                    String[] tokens = txt.split(";");
                    if (tokens.length < 3) {
                        throw new RuntimeException("Error parsing '" + txt + "'");
                    }
                    String contingencyId = tokens[0];
                    ContingencyData cd = new ContingencyData(contingencyId);
                    int lineCount = Integer.parseInt(tokens[1]);
                    if (tokens.length != lineCount + 2) {
                        throw new RuntimeException("Error parsing '" + txt + "'");
                    }
                    for (int i = 2; i < lineCount + 2; i++) {
                        String id = tokens[i];
                        cd.addElementId(id);
                    }
                    condingency_data.add(cd);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        // pre-index tie lines
        Map<String, String> tieLines = new HashMap<>();
        for (Line l : network.getLines()) {
            if (l.isTieLine()) {
                TieLine tl = (TieLine) l;
                tieLines.put(tl.getHalf1().getId(), tl.getId());
                tieLines.put(tl.getHalf2().getId(), tl.getId());
            }
        }

        List<Contingency> contingencies = new ArrayList<>();
        condingency_data.forEach(cd -> {
            List<ContingencyElement> elements = cd.getElementsIds().stream()
                    .map(id -> getElement(network, tieLines, cd.getId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (elements.size() > 0) {
                contingencies.add(new ContingencyImpl(cd.getId(), elements));
            } else {
                LOGGER.warn("Skip empty contingency " + cd.getId());
            }
        });
        return contingencies;
    }

    private ContingencyElement getElement(Network network, Map<String, String> tieLines, String id) {
        if (network.getLine(id) != null) {
            return new BranchContingency(id);
        } else if (network.getGenerator(id) != null) {
            return new GeneratorContingency(id);
        } else if (tieLines.containsKey(id)) {
            return new BranchContingency(tieLines.get(id));
        } else {
            LOGGER.warn("Contingency element '{}' not found", id);
        }
        return null;

    }

    @Override
    public List<Scenario> getScenarios() {
        return Collections.emptyList();
    }

    @Override
    public Zone getZone(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ActionPlan> getActionPlans() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionPlan getActionPlan(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Zone> getZones() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ActionsContingenciesAssociation> getActionsCtgAssociations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ActionsContingenciesAssociation> getActionsCtgAssociationsByContingency(String contingencyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Contingency getContingency(String name, Network network) {
        for (Contingency c : getContingencies(network)) {
            if (c.getId().equals(name)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Collection<Action> getActions(Network network) {
        throw new UnsupportedOperationException();

    }

    @Override
    public Action getAction(String id, Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Zone> getZones(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ActionPlan> getActionPlans(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ActionsContingenciesAssociation> getActionsCtgAssociations(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ActionsContingenciesAssociation> getActionsCtgAssociationsByConstraint(String equipmentId,
            ConstraintType constraintType) {
        throw new UnsupportedOperationException();
    }

    private class ContingencyData {
        private final String id;
        private final List<String> elementIds;

        ContingencyData(String id) {
            this.id = id;
            elementIds = new ArrayList();
        }

        String getId() {
            return this.id;
        }

        void addElementId(String id) {
            elementIds.add(id);
        }

        List<String> getElementsIds() {
            return elementIds;
        }

    }

}
