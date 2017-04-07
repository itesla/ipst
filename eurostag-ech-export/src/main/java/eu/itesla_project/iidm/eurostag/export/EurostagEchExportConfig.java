/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;
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

    private boolean noGeneratorMinMaxQ;

    private boolean noSwitch;

    private final String forbiddenCharacters;

    private final Character forbiddenCharactersReplacement;

    public EurostagEchExportConfig() {
        this(false, false, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ) {
        this(noGeneratorMinMaxQ, false, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ, boolean noSwitch) {
        this(noGeneratorMinMaxQ, noSwitch, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ, boolean noSwitch, String forbiddenCharacters, Character forbiddenCharactersReplacement) {
        this.forbiddenCharacters = Objects.requireNonNull(forbiddenCharacters, "forbiddenCharacters string must be not null");
        this.forbiddenCharactersReplacement = Objects.requireNonNull(forbiddenCharactersReplacement, "forbiddenCharactersReplacement (single char) string must not be null");
        this.noGeneratorMinMaxQ = noGeneratorMinMaxQ;
        this.noSwitch = noSwitch;
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
        if (platformConfig.moduleExists(EUROSTAG_ECH_EXPORT_CONFIG)) {
            ModuleConfig config = platformConfig.getModuleConfig(EUROSTAG_ECH_EXPORT_CONFIG);
            boolean noGeneratorMinMaxQ = config.getBooleanProperty("noGeneratorMinMaxQ", DEFAULT_NOGENERATORMINMAXQ);
            boolean noSwitch = config.getBooleanProperty("noSwitch", DEFAULT_NOSWITCH);
            String forbiddenCharacters = config.getStringProperty("forbiddenCharacters", DEFAULT_FORBIDDEN_CHARACTERS);
            String replacementCharString = config.getStringProperty("forbiddenCharactersReplacement", DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT.toString());
            if (replacementCharString.length() != 1) {
                throw new IllegalArgumentException("forbiddenCharactersReplacement must be a single character: " + replacementCharString);
            }
            Character forbiddenCharactersReplacement = replacementCharString.charAt(0);
            return new EurostagEchExportConfig(noGeneratorMinMaxQ, noSwitch, forbiddenCharacters, forbiddenCharactersReplacement);
        } else {
            LOGGER.warn("no eurostag-ech-export config found: Using defaults.");
            return new EurostagEchExportConfig(DEFAULT_NOGENERATORMINMAXQ, DEFAULT_NOSWITCH, DEFAULT_FORBIDDEN_CHARACTERS, DEFAULT_FORBIDDEN_CHARACTERS_REPLACEMENT);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [noGeneratorMinMaxQ=" + noGeneratorMinMaxQ +
                ", noSwitch=" + noSwitch +
                ", forbiddenCharacters=" + forbiddenCharacters +
                ", forbiddenCharactersReplacement=" + forbiddenCharactersReplacement +
                "]";
    }

}
