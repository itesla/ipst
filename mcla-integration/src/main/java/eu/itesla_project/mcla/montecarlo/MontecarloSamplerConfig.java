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
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class MontecarloSamplerConfig {

    /*
     *  required fea matlab code modules version v1.7.1 (offline sampling)
     *
     *  v1.7.1 : adds 'centering' parameter
     *  v1.7.0 : offline samples
     *
     * example of montecarloSampler.properties file.
     * note that required matlab binaries and MCR are expected to be available at the specified path
     * on each computation node
     *
    #path to the directory containing the compiled matlab binaries (chmod +x all the binaries)
    binariesDir=/shared/allrw/montecarlosampler/bin

    #path to the MCR installation
    runtimeHomeDir=/usr/local/MATLAB/MATLAB_Compiler_Runtime/v81

    #path to the tmp folder
    tmpDir=/home/itesla/itesla_wp5/tmp

    #optionSign  if 1, when value of extracted sample has a different sign wrt forecast the sampled value is put to 0. if 0, the sampled value is assumed as valid sample.
    optionSign=0

    #centering   if centering = 1, conditional samples are centered on DACF (used only if FEA conditional_sampling =1 )
    centering=1

    debug=true

    */

    private static final Logger LOGGER = LoggerFactory.getLogger(MontecarloSamplerConfig.class);

    private Path binariesDir;
    private Path runtimeHomeDir;
    private Path tmpDir;
    private Integer optionSign;
    private Integer centering;
    private final boolean debug;
    private final Integer isdeterministic;
    private final Integer full_dependence;
    private final boolean copyFEFile;

    public MontecarloSamplerConfig(
            Path binariesDir,
            Path runtimeHomeDir,
            Path tmpDir,
            int optionSign,
            int centering,
            int isdeterministic,
            int full_dependence,
            boolean copyFEFile,
            boolean debug
    ) {
        Objects.requireNonNull(binariesDir, "sampler compiled binaries directory is null");
        Objects.requireNonNull(runtimeHomeDir, "matlab runtime directory is null");
        Objects.requireNonNull(tmpDir, "tmp directory is null");

        this.binariesDir = binariesDir;
        this.runtimeHomeDir = runtimeHomeDir;
        this.tmpDir = tmpDir;
        this.optionSign = optionSign;
        this.centering = centering;
        this.isdeterministic = isdeterministic;
        this.full_dependence = full_dependence;
        this.copyFEFile = copyFEFile;
        this.debug = debug;
    }

    public static MontecarloSamplerConfig load() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("montecarloSampler");

        Path binariesDir = config.getPathProperty("binariesDir");
        Path runtimeHomeDir = config.getPathProperty("runtimeHomeDir");
        Path tmpDir = config.getPathProperty("tmpDir");
        int optionSign = config.getIntProperty("optionSign");
        int centering = config.getIntProperty("centering");
        Integer isdeterministic = config.getOptionalIntegerProperty("isdeterministic").orElse(null);
        Integer full_dependence = config.getOptionalIntegerProperty("full_dependence").orElse(null);
        boolean copyFEFile = config.getBooleanProperty("copyFEFile", true);
        boolean debug = config.getBooleanProperty("debug", false);

        return new MontecarloSamplerConfig(binariesDir, runtimeHomeDir, tmpDir, optionSign, centering, isdeterministic, full_dependence, copyFEFile, debug);
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

    public Integer getOptionSign() {
        return optionSign;
    }

    public Integer getCentering() {
        return centering;
    }

    public Integer getIsdeterministic() {
        return isdeterministic;
    }

    public Integer getFull_dependence() {
        return full_dependence;
    }

    public boolean isCopyFEFile() {
        return copyFEFile;
    }

    public boolean isDebug() {
        return debug;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String toString() {
        return "MontecarloSamplerConfig [" + ", binariesDir=" + binariesDir + ", runtimeHomeDir=" + runtimeHomeDir + ", tmpDir=" + tmpDir
                + ", optionSign=" + optionSign + ", centering=" + centering + ", isdeterministic=" + isdeterministic + ", full_dependence=" + full_dependence
                + ", copyFEFile=" + copyFEFile + ", debug=" + debug + "]";
    }

}
