/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import java.nio.file.Path;
import java.util.Objects;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;

/**
* @author Giovanni Ferrari <giovanni.ferrari at techrain.eu>
*/
public class CaseProjectorLoadFlowParameters extends AbstractExtension<LoadFlowParameters> {

    private Path amplHomeDir;
    private Path generatorsDomainsFile;
    private boolean debug;

    public CaseProjectorLoadFlowParameters(PlatformConfig config) {
        Objects.requireNonNull(config);

        if (config.moduleExists("caseProjector")) {
            ModuleConfig amplConfig = config.getModuleConfig("caseProjector");
            this.amplHomeDir = amplConfig.getPathProperty("amplHomeDir");
            this.generatorsDomainsFile =  amplConfig.getPathProperty("generatorsDomainsFile");
            this.debug = amplConfig.getBooleanProperty("debug");
        }
    }

    public CaseProjectorLoadFlowParameters(Path amplHomeDir, Path generatorsDomainsFile, boolean debug) {
        this.amplHomeDir = Objects.requireNonNull(amplHomeDir);
        this.generatorsDomainsFile = Objects.requireNonNull(generatorsDomainsFile);
        this.debug = debug;
    }

    @AutoService(LoadFlowParameters.ConfigLoader.class)
    public static class AmplLoadFlowConfigLoader implements LoadFlowParameters.ConfigLoader<CaseProjectorLoadFlowParameters> {

        @Override
        public CaseProjectorLoadFlowParameters load(PlatformConfig config) {
            return new CaseProjectorLoadFlowParameters(config);
        }

        @Override
        public String getCategoryName() {
            return "loadflow-parameters";
        }

        @Override
        public Class<? super CaseProjectorLoadFlowParameters> getExtensionClass() {
            return CaseProjectorLoadFlowParameters.class;
        }

        @Override
        public String getExtensionName() {
            return "ampl";
        }
    }

    @Override
    public String getName() {
        return "ampl";
    }

    public Path getAmplHomeDir() {
        return amplHomeDir;
    }

    public boolean isDebug() {
        return debug;
    }

    public Path getGeneratorsDomainsFile() {
        return generatorsDomainsFile;
    }

}
