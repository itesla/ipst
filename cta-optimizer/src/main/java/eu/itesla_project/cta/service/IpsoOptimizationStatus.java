/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public enum IpsoOptimizationStatus {
    ABORTED("IPSO ABORTED"),
    EXECUTION_FAILED("IPSO EXECUTION FAILED"),
    IPSO_OUT_FILE_MISSING("IPSO OUT FILE MISSING"),
    IPSO_CSV_FILE_MISSING("IPSO CSV FILE MISSING"),
    ERROR("IPSO OPTIMIZATION HAS ENCOUNTERED ERROR(S)"),
    SUCCEDED("IPSO OPTIMISATION SUCCEEDED"),
    UNDEFINED("NO STATUS AVAILABLE");

    private String message;

    IpsoOptimizationStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
