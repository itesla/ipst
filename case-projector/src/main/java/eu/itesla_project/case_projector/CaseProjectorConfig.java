/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.case_projector;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class CaseProjectorConfig {

    private final Path amplHomeDir;

    private final boolean debug;

    public static CaseProjectorConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static CaseProjectorConfig load(PlatformConfig platformConfig) {
        ModuleConfig config = platformConfig.getModuleConfig("caseProjector");
        Path amplHomeDir = config.getPathProperty("amplHomeDir");
        boolean debug = config.getBooleanProperty("debug", false);
        return new CaseProjectorConfig(amplHomeDir, debug);
    }

    public CaseProjectorConfig(Path amplHomeDir, boolean debug) {
        this.amplHomeDir = Objects.requireNonNull(amplHomeDir);
        this.debug = debug;
    }

    public Path getAmplHomeDir() {
        return amplHomeDir;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [amplHomeDir=" + amplHomeDir +
                ", debug=" + debug +
                "]";
    }

}
