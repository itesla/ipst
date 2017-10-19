/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.iidm.ddb.eurostag_imp_exp;

import com.powsybl.iidm.network.Network;
import com.powsybl.commons.Versionable;

import java.nio.file.Path;
import java.util.Map;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface DynamicDatabaseClient extends Versionable {

    void dumpDtaFile(Path workingDir, String fileName, Network network, Map<String, Character> parallelIndexes,
                     String eurostagVersion, Map<String, String> iidm2eurostagId);

}
