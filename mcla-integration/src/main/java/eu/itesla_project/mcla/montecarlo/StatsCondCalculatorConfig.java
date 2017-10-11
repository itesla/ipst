/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.mcla.montecarlo;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

/*

#path to the directory containing the compiled matlab binaries (chmod +x all the binaries)
binariesDir=/home/itesla/itesla_wp5/bin
#path to the MCR installation
runtimeHomeDir=/usr/local/MATLAB/MATLAB_Compiler_Runtime/v81
#path to the tmp folder
tmpDir=/home/itesla/itesla_wp5/tmp

rngSeed=1

debug=true


 */
/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class StatsCondCalculatorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsCondCalculatorConfig.class);

    private Path binariesDir;
    private Path runtimeHomeDir;
    private Path tmpDir;
    private final Integer rngSeed;
    private final boolean debug;
    private final Integer isdeterministic;
    private final Integer isuniform;


    public StatsCondCalculatorConfig(
            Path binariesDir,
            Path runtimeHomeDir,
            Path tmpDir,
            Integer isdeterministic,
            Integer isuniform,
            Integer rngSeed,
            boolean debug
    ) {
        Objects.requireNonNull(binariesDir, "sampler compiled binaries directory is null");
        Objects.requireNonNull(runtimeHomeDir, "matlab runtime directory is null");
        Objects.requireNonNull(tmpDir, "tmp directory is null");

        this.binariesDir = binariesDir;
        this.runtimeHomeDir = runtimeHomeDir;
        this.tmpDir = tmpDir;
        this.rngSeed = rngSeed;
        this.debug = debug;
        this.isdeterministic = isdeterministic;
        this.isuniform = isuniform;
    }

    public static StatsCondCalculatorConfig load() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("statsCondCalculator");

        Path binariesDir = config.getPathProperty("binariesDir");
        Path runtimeHomeDir = config.getPathProperty("runtimeHomeDir");
        Path tmpDir = config.getPathProperty("tmpDir");
        Integer rngSeed = config.getOptionalIntegerProperty("rngSeed").orElse(null);
        boolean debug = config.getBooleanProperty("debug", false);
        Integer isdeterministic = config.getOptionalIntegerProperty("isdeterministic").orElse(null);
        Integer isuniform = config.getOptionalIntegerProperty("isuniform").orElse(null);

        return new StatsCondCalculatorConfig(binariesDir, runtimeHomeDir, tmpDir, isdeterministic, isuniform, rngSeed, debug);
    }

    public Path getBinariesDir() {
        return binariesDir;
    }

    public Path getRuntimeHomeDir() {
        return runtimeHomeDir;
    }

    public Path getTmpDir() {
        return tmpDir;
    }


    public Integer getRngSeed() {
        return rngSeed;
    }

    public boolean isDebug() {
        return debug;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public Integer getIsdeterministic() {
        return isdeterministic;
    }

    public Integer getIsuniform() {
        return isuniform;
    }

    @Override
    public String toString() {
        return "StatsCondCalculatorConfig [" + ", binariesDir=" + binariesDir + ", runtimeHomeDir=" + runtimeHomeDir + ", tmpDir=" + tmpDir
                + ", isdeterministic=" + isdeterministic
                + ", isuniform=" + isuniform
                + ", rngSeed=" + rngSeed + ", debug=" + debug + "]";
    }

}
