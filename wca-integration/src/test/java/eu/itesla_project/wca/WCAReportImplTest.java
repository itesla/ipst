/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.io.CharStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.simulation.securityindexes.SecurityIndexId;
import com.powsybl.simulation.securityindexes.SecurityIndexType;

import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.SecurityRule;
import eu.itesla_project.modules.wca.report.WCAActionApplication;
import eu.itesla_project.modules.wca.report.WCALoadflowResult;
import eu.itesla_project.modules.wca.report.WCAPostContingencyStatus;
import eu.itesla_project.modules.wca.report.WCARuleViolationType;
import eu.itesla_project.modules.wca.report.WCASecurityRuleApplication;
import eu.itesla_project.wca.report.WCAReportImpl;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAReportImplTest {

    private FileSystem fileSystem;
    private LimitViolation line1Violation;
    private LimitViolation line2Violation;
    private Network network;
    private String networkId = "network1";
    private String line1Id = "line1";
    private String line2Id = "line2";

    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        line1Violation = new LimitViolation(line1Id, LimitViolationType.CURRENT, "20'", 20*60, 1000f, 1f, 1100f, Branch.Side.ONE);
        line2Violation = new LimitViolation(line2Id, LimitViolationType.CURRENT, "10'", 10*60, 900f, 1f, 950f, Branch.Side.ONE);

        Substation substation = Mockito.mock(Substation.class);
        Mockito.when(substation.getCountry()).thenReturn(Country.FR);

        VoltageLevel voltageLevel = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel.getSubstation()).thenReturn(substation);
        Mockito.when(voltageLevel.getNominalV()).thenReturn(380f);

        Terminal line1Terminal = Mockito.mock(Terminal.class);
        Mockito.when(line1Terminal.getVoltageLevel()).thenReturn(voltageLevel);

        Branch line1 = Mockito.mock(Branch.class);
        Mockito.when(line1.getId()).thenReturn(line1Id);
        Mockito.when(line1.getTerminal(Branch.Side.ONE)).thenReturn(line1Terminal);

        Terminal line2Terminal = Mockito.mock(Terminal.class);
        Mockito.when(line2Terminal.getVoltageLevel()).thenReturn(voltageLevel);

        Branch line2 = Mockito.mock(Branch.class);
        Mockito.when(line2.getId()).thenReturn(line2Id);
        Mockito.when(line2.getTerminal(Branch.Side.ONE)).thenReturn(line2Terminal);

        network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(network.getIdentifiable(line1Id)).thenReturn(line1);
        Mockito.when(network.getIdentifiable(line2Id)).thenReturn(line2);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testExportPreContigencyViolationsWithoutUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(false, "base state loadflow diverged"));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                                           "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";Loadflow;base state loadflow diverged;;;;;;;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }
    
    @Test
    public void testExportPreContigencyViolationsWithoutUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPreContingencyViolationsWithoutUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                                           "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";;;CURRENT;" + line1Id + ";" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE",
                                           networkId + ";;;CURRENT;" + line2Id + ";" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPreContigencyViolationsWithUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.setBaseStateWithUncertaintiesLoadflowResult(new WCALoadflowResult(false, "base state with uncertainties loadflow diverged"));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                                           "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";Loadflow;base state with uncertainties loadflow diverged;;;;;;;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPreContigencyViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPreContingencyViolationsWithUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                                           "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";;;CURRENT;" + line1Id + ";" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE",
                                           networkId + ";;;CURRENT;" + line2Id + ";" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPreventiveActionsApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.addPreventiveActionApplication(new WCAActionApplication("action1", 
                                                                          line1Violation, 
                                                                          new WCALoadflowResult(true, null), 
                                                                          false, 
                                                                          false, 
                                                                          "post action state contains new violations"));
        wcaReport.addPreventiveActionApplication(new WCAActionApplication("action2", 
                                                                          line1Violation, 
                                                                          new WCALoadflowResult(true, null), 
                                                                          true, 
                                                                          true,
                                                                          null));
        wcaReport.addPreventiveActionApplication(new WCAActionApplication("action3", 
                                                                          line2Violation, 
                                                                          new WCALoadflowResult(false, "loadflow on post action state diverged"), 
                                                                          false, 
                                                                          false,
                                                                          null));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_PREVENTIVE_ACTIONS_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_PREVENTIVE_ACTIONS_TITLE,
                                           "Basecase;ActionId;ViolatedEquipment;ViolationType;FailureStep;FailureDescription;ViolationRemoved;ActionApplied;Comment",
                                           networkId + ";action1;" + line1Id + ";CURRENT;;;false;false;post action state contains new violations",
                                           networkId + ";action2;" + line1Id + ";CURRENT;;;true;true;",
                                           networkId + ";action3;" + line2Id + ";CURRENT;Loadflow;loadflow on post action state diverged;false;false;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPostPreventiveActionsViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPostPreventiveActionsViolationsWithUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                                           "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";;;CURRENT;" + line1Id + ";" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f) 
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE",
                                           networkId + ";;;CURRENT;" + line2Id + ";" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportSecurityRulesApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        SecurityRule rule1 = Mockito.mock(SecurityRule.class);
        Mockito.when(rule1.getId()).thenReturn(new RuleId(RuleAttributeSet.WORST_CASE, new SecurityIndexId("fault1", SecurityIndexType.TSO_OVERLOAD)));
        Mockito.when(rule1.getWorkflowId()).thenReturn("workflow-0");
        SecurityRule rule2 = Mockito.mock(SecurityRule.class);
        Mockito.when(rule2.getId()).thenReturn(new RuleId(RuleAttributeSet.WORST_CASE, new SecurityIndexId("fault1", SecurityIndexType.SMALLSIGNAL)));
        Mockito.when(rule2.getWorkflowId()).thenReturn("workflow-0");
        wcaReport.addSecurityRulesApplication(new WCASecurityRuleApplication("fault1", 
                                                                             rule1, 
                                                                             true, 
                                                                             WCARuleViolationType.MISSING_ATTRIBUTE, 
                                                                             "Missing attributes for rule " + rule1.getId()+ ": attribute1"));
        wcaReport.addSecurityRulesApplication(new WCASecurityRuleApplication("fault1", 
                                                                             rule2, 
                                                                             false, 
                                                                             WCARuleViolationType.NO_VIOLATION, 
                                                                             "Rule " + rule2.getId() + " verified"));
        String missingRuleMessage = "Missing rule " + new RuleId(RuleAttributeSet.WORST_CASE, new SecurityIndexId("fault2", SecurityIndexType.TSO_OVERLOAD));
        wcaReport.addSecurityRulesApplication(new WCASecurityRuleApplication("fault2", 
                                                                             null, 
                                                                             true, 
                                                                             WCARuleViolationType.MISSING_RULE, 
                                                                             missingRuleMessage));
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                                           "Basecase;ContingencyId;SecurityRule;WorkflowId;RuleViolated;ViolationType;Cause",
                                           networkId + ";fault1;"+rule1.getId().toString()+";workflow-0;true;MISSING_ATTRIBUTE;Missing attributes for rule " + rule1.getId()+ ": attribute1",
                                           networkId + ";fault1;"+rule2.getId().toString()+";workflow-0;false;NO_VIOLATION;Rule " + rule2.getId() + " verified",
                                           networkId + ";fault2;;;true;MISSING_RULE;"+ missingRuleMessage);
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPostContigencyViolationsWithoutUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        WCAPostContingencyStatus postContingencyStatus = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(false, "post contingency loadflow diverged")); 
        wcaReport.addPostContingencyStatus(postContingencyStatus);
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                                           "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";fault1;Loadflow;post contingency loadflow diverged;;;;;;;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPostContigencyViolationsWithoutUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        WCAPostContingencyStatus postContingencyStatus1 = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyViolationsWithoutUncertainties(Collections.singletonList(line1Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus1);
        WCAPostContingencyStatus postContingencyStatus2 = new WCAPostContingencyStatus("fault2", new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyViolationsWithoutUncertainties(Collections.singletonList(line2Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus2);
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE,
                                           "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";fault1;;;CURRENT;" + line1Id + ";" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE",
                                           networkId + ";fault2;;;CURRENT;" + line2Id + ";" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPostContigencyViolationsWithUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        WCAPostContingencyStatus postContingencyStatus = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(false, "post contingency with uncertainties loadflow diverged"));
        wcaReport.addPostContingencyStatus(postContingencyStatus);
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                                           "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";fault1;Loadflow;post contingency with uncertainties loadflow diverged;;;;;;;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportPostContigencyViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        WCAPostContingencyStatus postContingencyStatus1 = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyViolationsWithUncertainties(Collections.singletonList(line1Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus1);
        WCAPostContingencyStatus postContingencyStatus2 = new WCAPostContingencyStatus("fault2", new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyViolationsWithUncertainties(Collections.singletonList(line2Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus2);
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE,
                                           "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage;Side",
                                           networkId + ";fault1;;;CURRENT;" + line1Id + ";" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE",
                                           networkId + ";fault2;;;CURRENT;" + line2Id + ";" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                                           + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + ";ONE");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportCurativeActionsApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));

        WCAReportImpl wcaReport = new WCAReportImpl(network);
        WCAPostContingencyStatus postContingencyStatus1 = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus1.setCurativeActionsApplication(Arrays.asList(new WCAActionApplication("action1", 
                                                                                                   null, 
                                                                                                   new WCALoadflowResult(true, null), 
                                                                                                   false, 
                                                                                                   false, 
                                                                                                   "violantions found in post action state"),
                                                                           new WCAActionApplication("action2", 
                                                                                                    null, 
                                                                                                    new WCALoadflowResult(true, null), 
                                                                                                    true, 
                                                                                                    true,
                                                                                                    null)));
        wcaReport.addPostContingencyStatus(postContingencyStatus1);
        WCAPostContingencyStatus postContingencyStatus2 = new WCAPostContingencyStatus("fault2", new WCALoadflowResult(true, null));
        postContingencyStatus2.setCurativeActionsApplication(Arrays.asList(new WCAActionApplication("action3", 
                                                                                                    null, 
                                                                                                    new WCALoadflowResult(false, "loadflow on post action state diverged"), 
                                                                                                    false, 
                                                                                                    false,
                                                                                                    null)));
        wcaReport.addPostContingencyStatus(postContingencyStatus2);
        wcaReport.exportCsv(folder);

        Path report = folder.resolve(WCAReportImpl.POST_CURATIVE_ACTIONS_FILE);
        assertTrue(Files.exists(report));
        String reportContent = String.join(System.lineSeparator(),
                                           WCAReportImpl.POST_CURATIVE_ACTIONS_TITLE,
                                           "Basecase;Contingency;ActionId;ViolatedEquipment;ViolationType;FailureStep;FailureDescription;ViolationRemoved;ActionApplied;Comment",
                                           networkId + ";fault1;action1;;;;;false;false;violantions found in post action state",
                                           networkId + ";fault1;action2;;;;;true;true;",
                                           networkId + ";fault2;action3;;;Loadflow;loadflow on post action state diverged;false;false;");
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))).trim());
    }

    @Test
    public void testExportCreateFolder() throws IOException {
        Path folder = fileSystem.getPath("/export-folder");
        WCAReportImpl wcaReport = new WCAReportImpl(network);
        wcaReport.exportCsv(folder);
        assertTrue(Files.exists(folder));
    }

    @Test
    public void testFailExportToFile() throws IOException {
        Path file = Files.createFile(fileSystem.getPath("/file"));
        WCAReportImpl wcaReport = new WCAReportImpl(network);
        assertFalse(wcaReport.exportCsv(file));
    }
}
