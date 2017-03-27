/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import com.google.common.base.Strings;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CutEurostagNamingStrategy implements EurostagNamingStrategy {
    @Override
    public void fillDictionary(EurostagDictionary dictionary, NameType nameType, Set<String> iidmIds) {
        EurostagEchExportConfig config = dictionary.getConfig();
        String forbiddenChars = config.getForbiddenCharactersString();
        Character replacementChar = config.getForbiddenCharactersReplacement();
        String regex = (forbiddenChars.length() > 0) ? "[" + Pattern.quote(forbiddenChars) + "]" : null;
        String repl = Matcher.quoteReplacement(replacementChar.toString());

        iidmIds.forEach(iidmId -> {
            if (!dictionary.iidmIdExists(iidmId)) {
                String esgId = getEsgId(dictionary, nameType, iidmId, regex, repl);
                dictionary.add(iidmId, esgId);
            }
        });
    }

    private String getEsgId(EurostagDictionary dictionary, NameType nameType, String iidmId, String regex, String repl) {
        String esgId = iidmId.length() > nameType.getLength() ? iidmId.substring(0, nameType.getLength())
                : Strings.padEnd(iidmId, nameType.getLength(), ' ');
        if (regex != null) {
            esgId = esgId.replaceAll(regex, repl);
        }
        int counter = 0;
        while (dictionary.esgIdExists(esgId)) {
            String counterStr = Integer.toString(counter++);
            if (counterStr.length() > nameType.getLength()) {
                throw new RuntimeException("Renaming fatal error " + iidmId + " -> " + esgId);
            }
            esgId = esgId.substring(0, nameType.getLength() - counterStr.length()) + counterStr;
        }
        return esgId;
    }
}

