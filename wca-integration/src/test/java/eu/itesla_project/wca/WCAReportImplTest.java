/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
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

import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.modules.rules.RuleAttributeSet;
import eu.itesla_project.modules.rules.RuleId;
import eu.itesla_project.modules.rules.SecurityRule;
import eu.itesla_project.modules.wca.report.WCAActionApplication;
import eu.itesla_project.modules.wca.report.WCALoadflowResult;
import eu.itesla_project.modules.wca.report.WCAPostContingencyStatus;
import eu.itesla_project.modules.wca.report.WCARuleViolationType;
import eu.itesla_project.modules.wca.report.WCASecurityRuleApplication;
import eu.itesla_project.security.LimitViolation;
import eu.itesla_project.security.LimitViolationType;
import eu.itesla_project.simulation.securityindexes.SecurityIndexId;
import eu.itesla_project.simulation.securityindexes.SecurityIndexType;
import eu.itesla_project.wca.report.WCAReportImpl;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAReportImplTest {
    
    private FileSystem fileSystem;
    private LimitViolation line1Violation;
    private LimitViolation line2Violation;
    
    @Before
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        Identifiable line1 = Mockito.mock(Identifiable.class);
        Mockito.when(line1.getId()).thenReturn("line1");
        line1Violation = new LimitViolation(line1, LimitViolationType.CURRENT, 1000f, "20'", 1, 1100f, Country.FR, 380f);
        Identifiable line2 = Mockito.mock(Identifiable.class);
        Mockito.when(line2.getId()).thenReturn("line2");
        line2Violation = new LimitViolation(line2, LimitViolationType.CURRENT, 900f, "10'", 1, 950f, Country.FR, 380f);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testExportPreContigencyViolationsWithoutUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(false, "base state loadflow diverged"));
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;Loadflow;base state loadflow diverged;;;;;;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPreContigencyViolationsWithoutUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPreContingencyViolationsWithoutUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;;;CURRENT;line1;" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator() +
                               "network1;;;CURRENT;line2;" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPreContigencyViolationsWithUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.setBaseStateWithUncertaintiesLoadflowResult(new WCALoadflowResult(false, "base state with uncertainties loadflow diverged"));
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;Loadflow;base state with uncertainties loadflow diverged;;;;;;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPreContigencyViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPreContingencyViolationsWithUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.PRE_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;;;CURRENT;line1;" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator() +
                               "network1;;;CURRENT;line2;" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPreventiveActionsApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
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
        String reportContent = WCAReportImpl.POST_PREVENTIVE_ACTIONS_TITLE + System.lineSeparator() + 
                               "Basecase;ActionId;ViolatedEquipment;ViolationType;FailureStep;FailureDescription;ViolationRemoved;ActionApplied;Comment" + System.lineSeparator() +
                               "network1;action1;line1;CURRENT;;;false;false;post action state contains new violations" + System.lineSeparator() +
                               "network1;action2;line1;CURRENT;;;true;true;" + System.lineSeparator() +
                               "network1;action3;line2;CURRENT;Loadflow;loadflow on post action state diverged;false;false;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPostPreventiveActionsViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.setBaseStateLoadflowResult(new WCALoadflowResult(true, null));
        wcaReport.setPostPreventiveActionsViolationsWithUncertainties(Arrays.asList(line1Violation, line2Violation));
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.POST_PREVENTIVE_ACTIONS_VIOLATIONS_WITH_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;;;CURRENT;line1;" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f) 
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator() +
                               "network1;;;CURRENT;line2;" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }

    @Test
    public void testExportSecurityRulesApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
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
        String reportContent = WCAReportImpl.SECURITY_RULES_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;ContingencyId;SecurityRule;WorkflowId;RuleViolated;ViolationType;Cause" + System.lineSeparator() +
                               "network1;fault1;"+rule1.getId().toString()+";workflow-0;true;MISSING_ATTRIBUTE;Missing attributes for rule " + rule1.getId()+ ": attribute1" + System.lineSeparator() +
                               "network1;fault1;"+rule2.getId().toString()+";workflow-0;false;NO_VIOLATION;Rule " + rule2.getId() + " verified" + System.lineSeparator() +
                               "network1;fault2;;;true;MISSING_RULE;"+ missingRuleMessage + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPostContigencyViolationsWithoutUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        WCAPostContingencyStatus postContingencyStatus = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(false, "post contingency loadflow diverged")); 
        wcaReport.addPostContingencyStatus(postContingencyStatus);
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;fault1;Loadflow;post contingency loadflow diverged;;;;;;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPostContigencyViolationsWithoutUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        WCAPostContingencyStatus postContingencyStatus1 = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyViolationsWithoutUncertainties(Collections.singleton(line1Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus1);
        WCAPostContingencyStatus postContingencyStatus2 = new WCAPostContingencyStatus("fault2", new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyViolationsWithoutUncertainties(Collections.singleton(line2Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus2);
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITHOUT_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;fault1;;;CURRENT;line1;" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator() +
                               "network1;fault2;;;CURRENT;line2;" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPostContigencyViolationsWithUncertaintiesLoadflowDivergence() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        WCAPostContingencyStatus postContingencyStatus = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(false, "post contingency with uncertainties loadflow diverged"));
        wcaReport.addPostContingencyStatus(postContingencyStatus);
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;fault1;Loadflow;post contingency with uncertainties loadflow diverged;;;;;;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportPostContigencyViolationsWithUncertainties() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        WCAPostContingencyStatus postContingencyStatus1 = new WCAPostContingencyStatus("fault1", new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(true, null));
        postContingencyStatus1.setPostContingencyViolationsWithUncertainties(Collections.singleton(line1Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus1);
        WCAPostContingencyStatus postContingencyStatus2 = new WCAPostContingencyStatus("fault2", new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyWithUncertaintiesLoadflowResult(new WCALoadflowResult(true, null));
        postContingencyStatus2.setPostContingencyViolationsWithUncertainties(Collections.singleton(line2Violation));
        wcaReport.addPostContingencyStatus(postContingencyStatus2);
        wcaReport.exportCsv(folder);
        
        Path report = folder.resolve(WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_FILE);
        assertTrue(Files.exists(report));
        String reportContent = WCAReportImpl.POST_CONTINGENCY_VIOLATIONS_WITH_UNCERTAINTIES_TITLE + System.lineSeparator() + 
                               "Basecase;Contingency;FailureStep;FailureDescription;ViolationType;Equipment;Value;Limit;Country;BaseVoltage" + System.lineSeparator() +
                               "network1;fault1;;;CURRENT;line1;" + String.format(Locale.getDefault(),"%g",1100f) + ";" + String.format(Locale.getDefault(),"%g",1000f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator() +
                               "network1;fault2;;;CURRENT;line2;" + String.format(Locale.getDefault(),"%g",950f) + ";" + String.format(Locale.getDefault(),"%g",900f)
                               + ";FR" + ";" + String.format(Locale.getDefault(),"%g",380f) + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportCurativeActionsApplication() throws IOException {
        Path folder = Files.createDirectory(fileSystem.getPath("/export-folder"));
        
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
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
        String reportContent = WCAReportImpl.POST_CURATIVE_ACTIONS_TITLE + System.lineSeparator() + 
                               "Basecase;Contingency;ActionId;ViolatedEquipment;ViolationType;FailureStep;FailureDescription;ViolationRemoved;ActionApplied;Comment" + System.lineSeparator() +
                               "network1;fault1;action1;;;;;false;false;violantions found in post action state" + System.lineSeparator() +
                               "network1;fault1;action2;;;;;true;true;" + System.lineSeparator() +
                               "network1;fault2;action3;;;Loadflow;loadflow on post action state diverged;false;false;" + System.lineSeparator();
        assertEquals(reportContent, CharStreams.toString(new InputStreamReader(Files.newInputStream(report))));
    }
    
    @Test
    public void testExportCreateFolder() throws IOException {
        Path folder = fileSystem.getPath("/export-folder");
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        wcaReport.exportCsv(folder);
        assertTrue(Files.exists(folder));
    }
    
    @Test
    public void testFailExportToFile() throws IOException {
        Path file = Files.createFile(fileSystem.getPath("/file"));
        WCAReportImpl wcaReport = new WCAReportImpl("network1");
        assertFalse(wcaReport.exportCsv(file));
    }
}
