/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.NetworkTest1Factory;
import eu.itesla_project.modules.histo.HistoDbAttr;
import eu.itesla_project.modules.histo.HistoDbNetworkAttributeId;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.SecurityRuleExpression;
import eu.itesla_project.modules.rules.SecurityRuleStatus;
import eu.itesla_project.modules.rules.expr.AndOperator;
import eu.itesla_project.modules.rules.expr.Attribute;
import eu.itesla_project.modules.rules.expr.ComparisonOperator;
import eu.itesla_project.modules.rules.expr.ExpressionNode;
import eu.itesla_project.modules.rules.expr.Litteral;
import com.powsybl.simulation.securityindexes.SecurityIndexId;
import com.powsybl.simulation.securityindexes.SecurityIndexType;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCASecurityRulesWriterTest {

    @Test
    public void testWrite() {
        ContingencyElement contingencyElement = new BranchContingency("line");
        Contingency contingency = new Contingency("contigency_1", contingencyElement);

        Network network = NetworkTest1Factory.create();

        List<SecurityRuleExpression> rules = new ArrayList<>();
        SecurityIndexType securityIndexType = SecurityIndexType.TSO_OVERLOAD;
        SecurityIndexId securityIndexId = new SecurityIndexId(contingency.getId(), securityIndexType);
        RuleAttributeSet ruleAttributeSet = RuleAttributeSet.WORST_CASE;
        RuleId ruleId = new RuleId(ruleAttributeSet, securityIndexId);
        ComparisonOperator condition1 = new ComparisonOperator(
                new Attribute(new HistoDbNetworkAttributeId(network.getLoads().iterator().next().getId(), HistoDbAttr.P)),
                new Litteral(10d),
                ComparisonOperator.Type.GREATER_EQUAL);
        ComparisonOperator condition2 = new ComparisonOperator(
                new Attribute(new HistoDbNetworkAttributeId(network.getGenerators().iterator().next().getId(), HistoDbAttr.Q)),
                new Litteral(5d),
                ComparisonOperator.Type.LESS);
        ExpressionNode condition = new AndOperator(condition1, condition2);
        rules.add(new SecurityRuleExpression(ruleId, SecurityRuleStatus.SECURE_IF, condition));

        MemDataSource dataSource = new MemDataSource();

        StringToIntMapper<AmplSubset> mapper = new StringToIntMapper<>(AmplSubset.class);
        AmplUtil.fillMapper(mapper, network);
        mapper.newInt(AmplSubset.FAULT, contingency.getId());

        new WCASecurityRulesWriter(network, rules, dataSource, mapper, false, false).write();

        String fileContent = String.join(System.lineSeparator(),
                                         "#(("+ network.getLoads().iterator().next().getId() + "_P >= 10.0) AND ("+ network.getGenerators().iterator().next().getId() + "_Q < 5.0))",
                                         "#Security rules",
                                         "#\"inequality num\" \"convex num\" \"var type (1: P, 2: Q, 3: V)\" \"entity type (1: branch, 2: load, 3: generator, 4: compensator shunt, 5: substation)\" \"entity num\" \"branch side (1 or 2, 0 if NA)\" \"inequality coeff.\" \"constant value\" \"contingency num\" \"security index type\" \"attribute set (0: active only, 1: active/reactive)\"",
                                         "1 1 1 2 1 0 -1.00000 -10.0000 1 4 0",
                                         "2 1 2 3 1 0 1.00000 5.00000 1 4 0");
        assertEquals(fileContent, new String(dataSource.getData(WCAConstants.SECURITY_RULES_FILE_SUFFIX, WCAConstants.TXT_EXT), StandardCharsets.UTF_8).trim());
    }

}
