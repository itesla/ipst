/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pclfsim;

import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.LimitViolationType;

import java.util.EnumSet;

/**
 *
 * @author Quinary <itesla@quinary.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
interface PostContLoadFlowSimConstants {

    String PRODUCT_NAME = "PostContingencyLoadFlowSimulator";
    String VERSION = "1.0.0";

    LimitViolationFilter CURRENT_FILTER = new LimitViolationFilter(EnumSet.of(LimitViolationType.CURRENT), 0);

}
