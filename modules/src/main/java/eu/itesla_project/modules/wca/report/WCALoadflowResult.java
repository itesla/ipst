/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca.report;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCALoadflowResult {

    private final boolean loadflowConverged;
    private final String comment;

    public WCALoadflowResult(boolean loadflowConverged, String comment) {
        this.loadflowConverged = loadflowConverged;
        this.comment = comment;
    }

    public boolean loadflowConverged() {
        return loadflowConverged;
    }

    public String getComment() {
        return comment;
    }

}
