/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
enum AmplStatus {
    SUCCEEDED("SUCCEEDED"),
    UNFEASIBLE("UNFEASIBLE"),
    ERROR("ERROR"),
    EXECUTION_ERROR("EXECUTION_ERROR");

    private final String textDescription;

    AmplStatus(String textDescription) {
        this.textDescription = textDescription;
    }

    public String getTextDescription() {
        return textDescription;
    }
}
