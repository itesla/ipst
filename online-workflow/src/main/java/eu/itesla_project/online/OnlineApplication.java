/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import org.joda.time.DateTime;

import eu.itesla_project.modules.online.OnlineWorkflowParameters;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public interface OnlineApplication extends AutoCloseable {

    int getAvailableCores();

    String startWorkflow(OnlineWorkflowStartParameters start, OnlineWorkflowParameters params);

    void stopWorkflow();

    void addListener(OnlineApplicationListener l);

    void removeListener(OnlineApplicationListener l);
    
    void notifyListeners();
    
    void startProcess(String name, String owner, DateTime date, DateTime creationDate,
			OnlineWorkflowStartParameters start, OnlineWorkflowParameters params, DateTime[] basecases)
					throws Exception;
}
	
