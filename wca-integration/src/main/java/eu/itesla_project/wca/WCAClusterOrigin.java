/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum WCAClusterOrigin {
    LF_DIVERGENCE,
    LF_BASIC_VIOLATION,
    DOMAINS_BASIC_VIOLATION,
    NO_PREVENTIVE_ACTION_FOUND,
    LF_SPECIFIC_PREVENTIVE_ACTION_FOUND,
    DOMAINS_SPECIFIC_PREVENTIVE_ACTION_FOUND,
    DOMAINS_NO_PREVENTIVE_ACTION_FOUND,
    LF_RULE_VIOLATION,
    DOMAINS_RULE_VIOLATION,
    LF_POST_CONTINGENCY_DIVERGENCE,
    LF_POST_CONTINGENCY_VIOLATION,
    LF_POST_SPECIFIC_CURATIVE_ACTION_VIOLATION,
    CLUSTERS_ANALYSIS
}
