/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.nio.file.Path;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DdExportConfig {

    public static final String MODULE_NAME = "ddImportExport";

    private static final boolean DEFAULT_AUTOMATON_A11 = false;
    private static final boolean DEFAULT_AUTOMATON_A12 = false;
    private static final boolean DEFAULT_AUTOMATON_A14 = false;
    private static final boolean DEFAULT_AUTOMATON_A17 = false;
    private static final boolean DEFAULT_RST = false;
    private static final boolean DEFAULT_ACMC = false;
    private static final boolean DEFAULT_LV_LOAD_MODELING = false;
    private static final String DEFAULT_RST_REGUL_INJECTOR = "RSTN_PCA";
    private static final String DEFAULT_RST_REGUL_GENERATOR = "APRTH1";
    private static final String DEFAULT_RST_REGUL_GENERATOR_DELETE = "CONSIG";
    private static final String DEFAULT_ACMC_REGUL = "ACMC";
    private static final String DEFAULT_RST_PILOT_GENERATORS = "";
    private static final float DEFAULT_LOAD_PATTERN_ALPHA = 1;
    private static final float DEFAULT_LOAD_PATTERN_BETA = 2;
    private static final boolean DEFAULT_GENPQFILTER = false;
    private static final boolean DEFAULT_EXPORT_MAIN_CC_ONLY = false;
    private static final boolean DEFAULT_NOSWITCH = false;
    private static final String DEFAULT_AUTOMATON_A17_REFERENCE_GENERATOR = null;
    private static final double DEFAULT_AUTOMATON_A17_MINIMUM_PHASE_DIFFERENCE_THRESHOLD = -240.0;
    private static final double DEFAULT_AUTOMATON_A17_MAXIMUM_PHASE_DIFFERENCE_THRESHOLD = 240.0;
    private static final double DEFAULT_AUTOMATON_A17_OBSERVATION_DURATION = -1;
    private static final boolean DEFAULT_AUTOMATON_A56 = false;
    private static final Path DEFAULT_AUTOMATON_A56_DETAILS_FILE = null;



    private boolean automatonA11;
    private boolean automatonA12;
    private boolean automatonA14;
    private boolean automatonA17;
    private boolean automatonA56;
    private boolean importExportRST;
    private boolean importExportACMC;
    private boolean LVLoadModeling;
    private String RSTRegulInjector;
    private String RSTRegulGenerator;
    private String RSTRegulGeneratorDelete;
    private String ACMCRegul;
    private String RSTPilotGenerators;
    private float loadPatternAlpha;
    private float loadPatternBeta;
    private boolean gensPQfilter;
    private boolean exportMainCCOnly;
    private boolean noSwitch;
    private String automatonA17AngularReferenceGenerator;
    private double automatonA17MinimumPhaseDifferenceThreshold;
    private double automatonA17MaximumPhaseDifferenceThreshold;
    private double automatonA17ObservationDuration;
    private Path automatonA56DetailsFile;

    public static DdExportConfig load(PlatformConfig platformConfig) {
        boolean automatonA11 = DEFAULT_AUTOMATON_A11;
        boolean automatonA12 = DEFAULT_AUTOMATON_A12;
        boolean automatonA14 = DEFAULT_AUTOMATON_A14;
        boolean automatonA17 = DEFAULT_AUTOMATON_A17;
        boolean automatonA56 = DEFAULT_AUTOMATON_A56;
        boolean importExportRST = DEFAULT_RST;
        boolean importExportACMC = DEFAULT_ACMC;
        boolean lvLoadModeling = DEFAULT_LV_LOAD_MODELING;
        String rstRegulInjector = DEFAULT_RST_REGUL_INJECTOR;
        String rstRegulGenerator = DEFAULT_RST_REGUL_GENERATOR;
        String rstRegulGeneratorDelete = DEFAULT_RST_REGUL_GENERATOR_DELETE;
        String acmcRegul = DEFAULT_ACMC_REGUL;
        String rstPilotGenerators = DEFAULT_RST_PILOT_GENERATORS;
        float loadPatternAlpha = DEFAULT_LOAD_PATTERN_ALPHA;
        float loadPatternBeta = DEFAULT_LOAD_PATTERN_BETA;
        boolean gensPQfilter = DEFAULT_GENPQFILTER;
        boolean exportMainCCOnly = DEFAULT_EXPORT_MAIN_CC_ONLY;
        boolean noSwitch = DEFAULT_NOSWITCH;
        String automatonA17AngularReferenceGenerator = DEFAULT_AUTOMATON_A17_REFERENCE_GENERATOR;
        double automatonA17MinimumPhaseDifferenceThreshold = DEFAULT_AUTOMATON_A17_MINIMUM_PHASE_DIFFERENCE_THRESHOLD;
        double automatonA17MaximumPhaseDifferenceThreshold = DEFAULT_AUTOMATON_A17_MAXIMUM_PHASE_DIFFERENCE_THRESHOLD;
        double automatonA17ObservationDuration = DEFAULT_AUTOMATON_A17_OBSERVATION_DURATION;
        Path automatonA56DetailsFile = DEFAULT_AUTOMATON_A56_DETAILS_FILE;


        if (platformConfig.moduleExists(MODULE_NAME)) {
            ModuleConfig config = platformConfig.getModuleConfig(MODULE_NAME);
            automatonA11 = config.getBooleanProperty("automatonA11", DEFAULT_AUTOMATON_A11);
            automatonA12 = config.getBooleanProperty("automatonA12", DEFAULT_AUTOMATON_A12);
            automatonA14 = config.getBooleanProperty("automatonA14", DEFAULT_AUTOMATON_A14);
            automatonA17 = config.getBooleanProperty("automatonA17", DEFAULT_AUTOMATON_A17);
            automatonA56 = config.getBooleanProperty("automatonA56", DEFAULT_AUTOMATON_A56);
            automatonA17AngularReferenceGenerator = config.getStringProperty("automatonA17AngularReferenceGenerator", DEFAULT_AUTOMATON_A17_REFERENCE_GENERATOR);
            automatonA17MinimumPhaseDifferenceThreshold = config.getDoubleProperty("automatonA17MinimumPhaseDifferenceThreshold", DEFAULT_AUTOMATON_A17_MINIMUM_PHASE_DIFFERENCE_THRESHOLD);
            automatonA17MaximumPhaseDifferenceThreshold = config.getDoubleProperty("automatonA17MaximumPhaseDifferenceThreshold", DEFAULT_AUTOMATON_A17_MAXIMUM_PHASE_DIFFERENCE_THRESHOLD);
            automatonA17ObservationDuration = config.getDoubleProperty("automatonA17ObservationDuration", DEFAULT_AUTOMATON_A17_OBSERVATION_DURATION);
            automatonA56DetailsFile = automatonA56 ? config.getPathProperty("automatonA56DetailsFile") : config.getPathProperty("automatonA56DetailsFile", null);
            importExportRST = config.getBooleanProperty("importExportRST", DEFAULT_RST);
            importExportACMC = config.getBooleanProperty("importExportACMC", DEFAULT_ACMC);
            lvLoadModeling = config.getBooleanProperty("LVLoadModeling", DEFAULT_LV_LOAD_MODELING);
            rstRegulInjector = config.getStringProperty("RSTRegulInjector", DEFAULT_RST_REGUL_INJECTOR);
            rstRegulGenerator = config.getStringProperty("RSTRegulGenerator", DEFAULT_RST_REGUL_GENERATOR);
            rstRegulGeneratorDelete = config.getStringProperty("RSTRegulGeneratorDelete", DEFAULT_RST_REGUL_GENERATOR_DELETE);
            acmcRegul = config.getStringProperty("ACMCRegul", DEFAULT_ACMC_REGUL);
            rstPilotGenerators = config.getStringProperty("RSTPilotGenerators", DEFAULT_RST_PILOT_GENERATORS);
            loadPatternAlpha = config.getFloatProperty("loadPatternAlpha", DEFAULT_LOAD_PATTERN_ALPHA);
            loadPatternBeta = config.getFloatProperty("loadPatternBeta", DEFAULT_LOAD_PATTERN_BETA);
            gensPQfilter = config.getBooleanProperty("gensPQfilter", DEFAULT_GENPQFILTER);
        }

        if (platformConfig.moduleExists("eurostag-ech-export")) {
            ModuleConfig config = platformConfig.getModuleConfig("eurostag-ech-export");
            exportMainCCOnly = config.getBooleanProperty("exportMainCCOnly", DEFAULT_EXPORT_MAIN_CC_ONLY);
            noSwitch = config.getBooleanProperty("noSwitch", DEFAULT_NOSWITCH);
        }

        return new DdExportConfig(automatonA11, automatonA12, automatonA14, automatonA17, automatonA17AngularReferenceGenerator, automatonA17MinimumPhaseDifferenceThreshold, automatonA17MaximumPhaseDifferenceThreshold, automatonA17ObservationDuration,
                automatonA56, automatonA56DetailsFile,
                importExportRST, importExportACMC, lvLoadModeling, rstRegulInjector, rstRegulGenerator, rstRegulGeneratorDelete,
                                  acmcRegul, rstPilotGenerators, loadPatternAlpha, loadPatternBeta, gensPQfilter, exportMainCCOnly, noSwitch);
    }

    public static DdExportConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public DdExportConfig() {
        this(DEFAULT_AUTOMATON_A11, DEFAULT_AUTOMATON_A12, DEFAULT_AUTOMATON_A14, DEFAULT_AUTOMATON_A17, DEFAULT_AUTOMATON_A17_REFERENCE_GENERATOR, DEFAULT_AUTOMATON_A17_MINIMUM_PHASE_DIFFERENCE_THRESHOLD, DEFAULT_AUTOMATON_A17_MAXIMUM_PHASE_DIFFERENCE_THRESHOLD, DEFAULT_AUTOMATON_A17_OBSERVATION_DURATION,
                DEFAULT_AUTOMATON_A56, DEFAULT_AUTOMATON_A56_DETAILS_FILE,
                DEFAULT_RST, DEFAULT_ACMC, DEFAULT_LV_LOAD_MODELING, DEFAULT_RST_REGUL_INJECTOR, DEFAULT_RST_REGUL_GENERATOR, DEFAULT_RST_REGUL_GENERATOR_DELETE,
                DEFAULT_ACMC_REGUL, DEFAULT_RST_PILOT_GENERATORS, DEFAULT_LOAD_PATTERN_ALPHA, DEFAULT_LOAD_PATTERN_BETA, DEFAULT_GENPQFILTER, DEFAULT_EXPORT_MAIN_CC_ONLY, DEFAULT_NOSWITCH);
    }

    public DdExportConfig(boolean automatonA11, boolean automatonA12, boolean automatonA14, boolean automatonA17, String automatonA17AngularReferenceGenerator, double automatonA17MinimumPhaseDifferenceThreshold, double automatonA17MaximumPhaseDifferenceThreshold, double automatonA17ObservationDuration,
                          boolean automatonA56, Path automatonA56DetailsFile,
                          boolean importExportRST, boolean importExportACMC, boolean lvLoadModeling, String rstRegulInjector,
                          String rstRegulGenerator, String rstRegulGeneratorDelete, String acmcRegul,
                          String rstPilotGenerators, float loadPatternAlpha, float loadPatternBeta, boolean gensPQfilter, boolean exportMainCCOnly, boolean noSwitch) {
        this.automatonA11 = automatonA11;
        this.automatonA12 = automatonA12;
        this.automatonA14 = automatonA14;
        this.automatonA17 = automatonA17;
        this.automatonA56 = automatonA56;
        this.importExportRST = importExportRST;
        this.importExportACMC = importExportACMC;
        this.LVLoadModeling = lvLoadModeling;
        this.RSTRegulInjector = rstRegulInjector;
        this.RSTRegulGenerator = rstRegulGenerator;
        this.RSTRegulGeneratorDelete = rstRegulGeneratorDelete;
        this.ACMCRegul = acmcRegul;
        this.RSTPilotGenerators = rstPilotGenerators;
        this.loadPatternAlpha = loadPatternAlpha;
        this.loadPatternBeta = loadPatternBeta;
        this.gensPQfilter = gensPQfilter;
        this.exportMainCCOnly = exportMainCCOnly;
        this.noSwitch = noSwitch;
        this.automatonA17AngularReferenceGenerator = automatonA17AngularReferenceGenerator;
        this.automatonA17MinimumPhaseDifferenceThreshold = automatonA17MinimumPhaseDifferenceThreshold;
        this.automatonA17MaximumPhaseDifferenceThreshold = automatonA17MaximumPhaseDifferenceThreshold;
        this.automatonA17ObservationDuration = automatonA17ObservationDuration;
        this.automatonA56DetailsFile = automatonA56DetailsFile;
    }

    public boolean getAutomatonA11() {
        return automatonA11;
    }

    public boolean getAutomatonA12() {
        return automatonA12;
    }

    public boolean getAutomatonA14() {
        return automatonA14;
    }

    public boolean getAutomatonA17() {
        return automatonA17;
    }

    public boolean getAutomatonA56() {
        return automatonA56;
    }

    public boolean getExportRST() {
        return importExportRST;
    }

    public boolean getExportACMC() {
        return importExportACMC;
    }

    public boolean getLVLoadModeling() {
        return LVLoadModeling;
    }

    public boolean getImportRST() {
        return importExportRST;
    }

    public boolean getImportACMC() {
        return importExportACMC;
    }

    public String getRSTRegulInjector() {
        return RSTRegulInjector;
    }

    public String getRSTRegulGenerator() {
        return RSTRegulGenerator;
    }

    public String getRSTRegulGeneratorDelete() {
        return RSTRegulGeneratorDelete;
    }

    public String getACMCRegul() {
        return ACMCRegul;
    }

    public String getRSTPilotGenerators() {
        return RSTPilotGenerators;
    }

    public float getLoadPatternAlpha() {
        return loadPatternAlpha;
    }

    public float getLoadPatternBeta() {
        return loadPatternBeta;
    }

    public boolean getGensPQfilter() {
        return gensPQfilter;
    }

    public String getAutomatonA17AngularReferenceGenerator() {
        return automatonA17AngularReferenceGenerator;
    }

    public double getAutomatonA17MinimumPhaseDifferenceThreshold() {
        return automatonA17MinimumPhaseDifferenceThreshold;
    }

    public double getAutomatonA17MaximumPhaseDifferenceThreshold() {
        return automatonA17MaximumPhaseDifferenceThreshold;
    }

    public double getAutomatonA17ObservationDuration() {
        return automatonA17ObservationDuration;
    }

    public Path getDefaultAutomatonA56DetailsFile() {
        return automatonA56DetailsFile;
    }

    public void setAutomatonA11(Boolean automatonA11) {
        this.automatonA11 = automatonA11;
    }

    public void setAutomatonA12(Boolean automatonA12) {
        this.automatonA12 = automatonA12;
    }

    public void setAutomatonA14(Boolean automatonA14) {
        this.automatonA14 = automatonA14;
    }

    public void setAutomatonA17(Boolean automatonA17) {
        this.automatonA17 = automatonA17;
    }

    public void setImportExportRST(Boolean importExportRST) {
        this.importExportRST = importExportRST;
    }

    public void setLVLoadModeling(Boolean lvLoadModeling) {
        this.LVLoadModeling = lvLoadModeling;
    }

    public void setImportExportACMC(Boolean importExportACMC) {
        this.importExportACMC = importExportACMC;
    }

    public void setRSTRegulInjector(String rstRegulInjector) {
        this.RSTRegulInjector = rstRegulInjector;
    }

    public void setRSTRegulGenerator(String rstRegulGenerator) {
        this.RSTRegulGenerator = rstRegulGenerator;
    }

    public void setRSTRegulGeneratorDelete(String rstRegulGeneratorDelete) {
        this.RSTRegulGeneratorDelete = rstRegulGeneratorDelete;
    }

    public void setACMCRegul(String acmcRegul) {
        this.ACMCRegul = acmcRegul;
    }

    public void setRSTPilotGenerators(String rstPilotGenerators) {
        this.RSTPilotGenerators = rstPilotGenerators;
    }

    public void setLoadPatternAlpha(float loadPatternAlpha) {
        this.loadPatternAlpha = loadPatternAlpha;
    }

    public void setLoadPatternBeta(float loadPatternBeta) {
        this.loadPatternBeta = loadPatternBeta;
    }

    public void setGensPQfilter(boolean gensPQfilter) {
        this.gensPQfilter = gensPQfilter;
    }

    public void setAutomatonA17AngularReferenceGenerator(String automatonA17AngularReferenceGenerator) {
        this.automatonA17AngularReferenceGenerator = automatonA17AngularReferenceGenerator;
    }

    public void setAutomatonA17MinimumPhaseDifferenceThreshold(double automatonA17MinimumPhaseDifferenceThreshold) {
        this.automatonA17MinimumPhaseDifferenceThreshold = automatonA17MinimumPhaseDifferenceThreshold;
    }

    public void setAutomatonA17MaximumPhaseDifferenceThreshold(double automatonA17MaximumPhaseDifferenceThreshold) {
        this.automatonA17MaximumPhaseDifferenceThreshold = automatonA17MaximumPhaseDifferenceThreshold;
    }

    public void setAutomatonA17ObservationDuration(double automatonA17ObservationDuration) {
        this.automatonA17ObservationDuration = automatonA17ObservationDuration;
    }

    public boolean isExportMainCCOnly() {
        return exportMainCCOnly;
    }

    public boolean isNoSwitch() {
        return noSwitch;
    }

    public boolean isAutomatonA17ObservationDurationSet() {
        return automatonA17ObservationDuration >= 0.0;
    }
}
