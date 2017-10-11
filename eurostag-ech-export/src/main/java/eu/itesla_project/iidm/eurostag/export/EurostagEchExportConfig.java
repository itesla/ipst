/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EurostagEchExportConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(EurostagEchExportConfig.class);

    private static final String EUROSTAG_ECH_EXPORT_CONFIG = "eurostag-ech-export";

    private final static String DEFAULT_FORBIDDEN_CHARACTERS = "/%()^$,;?";
    private final static Character DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT = '#';
    private final static boolean DEFAULT_NOGENERATORMINMAXQ = false;
    private final static boolean DEFAULT_NOSWITCH = false;
    private final static boolean DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF = false;
    private final static boolean DEFAULT_SPECIFIC_COMPATIBILITY = false;

    private boolean noGeneratorMinMaxQ;

    private boolean noSwitch;

    private boolean svcAsFixedInjectionInLF;

    private boolean specificCompatibility;

    private final String forbiddenCharacters;

    private final Character forbiddenCharactersReplacement;

    public EurostagEchExportConfig() {
        this(false, false, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT, DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF, DEFAULT_SPECIFIC_COMPATIBILITY);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ) {
        this(noGeneratorMinMaxQ, false, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT, DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF, DEFAULT_SPECIFIC_COMPATIBILITY);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ, boolean noSwitch) {
        this(noGeneratorMinMaxQ, noSwitch, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT, DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF, DEFAULT_SPECIFIC_COMPATIBILITY);
    }


    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ, boolean noSwitch, String forbiddenCharacters, Character forbiddenCharactersReplacement, boolean svcAsFixedInjectionInLF, boolean specificCompatibility) {
        this.forbiddenCharacters = Objects.requireNonNull(forbiddenCharacters, "forbiddenCharacters string must be not null");
        this.forbiddenCharactersReplacement = Objects.requireNonNull(forbiddenCharactersReplacement, "forbiddenCharactersReplacement (single char) string must not be null");
        this.noGeneratorMinMaxQ = noGeneratorMinMaxQ;
        this.noSwitch = noSwitch;
        this.specificCompatibility = specificCompatibility;
        if (specificCompatibility) {
            this.svcAsFixedInjectionInLF = true;
            LOGGER.info("force svcAsFixedInjectionInLF to true (specificCompatibility is true)");
        } else {
            this.svcAsFixedInjectionInLF = svcAsFixedInjectionInLF;
        }
        if (forbiddenCharacters.contains(forbiddenCharactersReplacement.toString())) {
            throw new IllegalArgumentException("forbiddenCharactersReplacement " + forbiddenCharactersReplacement + " must not appear also in the forbiddenCharacters string: " + forbiddenCharacters);
        }
    }

    public boolean isNoGeneratorMinMaxQ() {
        return noGeneratorMinMaxQ;
    }

    public boolean isNoSwitch() {
        return noSwitch;
    }

    public boolean isSvcAsFixedInjectionInLF() {
        return svcAsFixedInjectionInLF;
    }

    public boolean isSpecificCompatibility() {
        return specificCompatibility;
    }

    public String getForbiddenCharactersString() {
        return forbiddenCharacters;
    }

    public Character getForbiddenCharactersReplacement() {
        return forbiddenCharactersReplacement;
    }

    public static EurostagEchExportConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static EurostagEchExportConfig load(PlatformConfig platformConfig) {

        // specificCompatibility parameter = true forces  svcAsFixedInjectionInLF to true
        ModuleConfig loadFlowModuleConfig = platformConfig.getModuleConfigIfExists("load-flow-default-parameters");
        boolean specificCompatibility = (loadFlowModuleConfig != null) ? loadFlowModuleConfig.getBooleanProperty("specificCompatibility", DEFAULT_SPECIFIC_COMPATIBILITY) : DEFAULT_SPECIFIC_COMPATIBILITY;

        if (platformConfig.moduleExists(EUROSTAG_ECH_EXPORT_CONFIG)) {
            ModuleConfig config = platformConfig.getModuleConfig(EUROSTAG_ECH_EXPORT_CONFIG);
            boolean noGeneratorMinMaxQ = config.getBooleanProperty("noGeneratorMinMaxQ", DEFAULT_NOGENERATORMINMAXQ);
            boolean noSwitch = config.getBooleanProperty("noSwitch", DEFAULT_NOSWITCH);
            boolean svcAsFixedInjectionInLF = config.getBooleanProperty("svcAsFixedInjectionInLF", DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF);
            String forbiddenCharacters = config.getStringProperty("forbiddenCharacters", DEFAULT_FORBIDDEN_CHARACTERS);
            String replacementCharString = config.getStringProperty("forbiddenCharactersReplacement", DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT.toString());
            if (replacementCharString.length() != 1) {
                throw new IllegalArgumentException("forbiddenCharactersReplacement must be a single character: " + replacementCharString);
            }
            Character forbiddenCharactersReplacement = replacementCharString.charAt(0);
            return new EurostagEchExportConfig(noGeneratorMinMaxQ, noSwitch, forbiddenCharacters, forbiddenCharactersReplacement, svcAsFixedInjectionInLF, specificCompatibility);
        } else {
            LOGGER.warn("no eurostag-ech-export config found: Using defaults.");
            return new EurostagEchExportConfig(DEFAULT_NOGENERATORMINMAXQ, DEFAULT_NOSWITCH, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT, DEFAULT_SVC_AS_FIXED_INJECTION_IN_LF, specificCompatibility);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [noGeneratorMinMaxQ=" + noGeneratorMinMaxQ +
                ", noSwitch=" + noSwitch +
                ", forbiddenCharacters=" + forbiddenCharacters +
                ", forbiddenCharactersReplacement=" + forbiddenCharactersReplacement +
                ", svcAsFixedInjectionInLF=" + svcAsFixedInjectionInLF +
                ", specificCompatibility=" + specificCompatibility +
                "]";
    }

}
