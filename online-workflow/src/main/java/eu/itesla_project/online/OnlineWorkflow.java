/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public interface OnlineWorkflow {
	/*
	String getProcessId();
	
	void setProcessId(String processId);
	*/
	String getId();

	void start(OnlineWorkflowContext oCtx) throws Exception;

	void addOnlineApplicationListener(OnlineApplicationListener listener);

	void removeOnlineApplicationListener(OnlineApplicationListener listener);

	

}