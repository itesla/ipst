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

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EurostagEchExportConfig {

    protected static final String EUROSTAG_ECH_EXPORT_CONFIG = "eurostag-ech-export";

    protected final static String DEFAULTEUROSTAGFORBIDDENCHARACTERS = "/%()^$,;?";
    protected final static Character DEFAULTEUROSTAGFORBIDDENCHARACTERSREPLACEMENT = '#';
    protected final static boolean DEFAULTNOGENERATORMINMAXQ = false;
    protected final static boolean DEFAULTNOSWITCH = false;
    protected final static Class DEFAULTNAMINGFACTORY = DicoEurostagNamingStrategyFactory.class;

    private boolean noGeneratorMinMaxQ;

    private boolean noSwitch;

    private final String forbiddenCharacters;

    private final Character forbiddenCharactersReplacement;

    public EurostagEchExportConfig() {
        this(false, false, DEFAULTEUROSTAGFORBIDDENCHARACTERS, DEFAULTEUROSTAGFORBIDDENCHARACTERSREPLACEMENT);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ) {
        this(noGeneratorMinMaxQ, false, DEFAULTEUROSTAGFORBIDDENCHARACTERS, DEFAULTEUROSTAGFORBIDDENCHARACTERSREPLACEMENT);
    }

    public EurostagEchExportConfig(boolean noGeneratorMinMaxQ, boolean noSwitch, String forbiddenCharacters, Character forbiddenCharactersReplacement) {
        Objects.requireNonNull(forbiddenCharacters, "forbiddenCharacters string must be not null");
        Objects.requireNonNull(forbiddenCharactersReplacement, "forbiddenCharactersReplacement (single char) string must not be null");
        this.noGeneratorMinMaxQ = noGeneratorMinMaxQ;
        this.noSwitch = noSwitch;
        this.forbiddenCharacters = forbiddenCharacters;
        if (forbiddenCharacters.contains("" + forbiddenCharactersReplacement)) {
            throw new IllegalArgumentException("forbiddenCharactersReplacement " + forbiddenCharactersReplacement + " must not appear also in the forbiddenCharacters string: " + forbiddenCharacters);
        }
        this.forbiddenCharactersReplacement = forbiddenCharactersReplacement;
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

    public Character getDefaultEurostagForbiddenCharactersReplacement() {
        return forbiddenCharactersReplacement;
    }

    public static EurostagEchExportConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static EurostagEchExportConfig load(PlatformConfig platformConfig) {
        if (platformConfig.moduleExists(EUROSTAG_ECH_EXPORT_CONFIG)) {
            ModuleConfig config = platformConfig.getModuleConfig(EUROSTAG_ECH_EXPORT_CONFIG);
            boolean noGeneratorMinMaxQ = config.getBooleanProperty("noGeneratorMinMaxQ", DEFAULTNOGENERATORMINMAXQ);
            boolean noSwitch = config.getBooleanProperty("noSwitch", DEFAULTNOSWITCH);
            String forbiddenCharacters = config.getStringProperty("forbiddenCharacters", DEFAULTEUROSTAGFORBIDDENCHARACTERS);
            String replacementCharString = config.getStringProperty("forbiddenCharactersReplacement", "" + DEFAULTEUROSTAGFORBIDDENCHARACTERSREPLACEMENT);
            if (replacementCharString.length() > 1) {
                throw new IllegalArgumentException("forbiddenCharactersReplacement must be a single character: " + replacementCharString);
            }
            Character forbiddenCharactersReplacement = replacementCharString.charAt(0);
            return new EurostagEchExportConfig(noGeneratorMinMaxQ, noSwitch, forbiddenCharacters, forbiddenCharactersReplacement);
        } else {
            System.out.println("no eurostag-ech-export config found: Using defaults.");
            return new EurostagEchExportConfig(DEFAULTNOGENERATORMINMAXQ, DEFAULTNOSWITCH, DEFAULTEUROSTAGFORBIDDENCHARACTERS, DEFAULTEUROSTAGFORBIDDENCHARACTERSREPLACEMENT);
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
