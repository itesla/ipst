/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.eurostag.export;

import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 *
 * In charge of creating identifiers to be used in Eurostag, from IIDM identifiers.
 */
public interface EurostagNamingStrategy {

    enum NameType {
        NODE(8),
        GENERATOR(8),
        LOAD(8),
        BANK(8),
        SVC(8),
        VSC(8);

        int length;

        NameType(int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

    /**
     * Fills a dictionary with Eurostag identifiers for the IIDM identifiers passed as argument.
     * @param dictionary the dictionary to be filled
     * @param nameType   the type of equipment
     * @param iidmIds    the list of IIDM ids for which Eurostag identifiers must be created.
     */
    void fillDictionary(EurostagDictionary dictionary, NameType nameType, Set<String> iidmIds);

}
