/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public interface AmplConstants {

    String APPLICATION_NAME = "ampl";
    String DAT_FILE_NAME = "ampl.dat";
    String RUN_FILE_NAME = "amplInput.run";
    String TAB_FILE_NAME = "SolverOptions.tab";
    String TEMPLATE_FILE = "template.dat";

    String MODELS_DIR = "amplModels";

    // AmplResults
    String RESULTS_TAG = "results";
    String ID_TAG = "id";
    String PARAMETERS_TAG = "parameters";
    String AMPL_EQUIPMENT_TAG = "equipments";
    String XML_FILE_NAME = "result.xml";
    String SWITCH_TAG = "connection_status";
    String AMPL_STATUS_TAG = "status";
    String AMPL_UNPLUGGED_EQUIPMENT_PREFIX = "disconnected_equipments_";
    String AMPL_UNPLUGGED_EQUIPMENT_SUFFIX = ".dat";
}
