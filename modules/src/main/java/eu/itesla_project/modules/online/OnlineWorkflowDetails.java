/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.online;

import org.joda.time.DateTime;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class OnlineWorkflowDetails {

    final String workflowId;
    DateTime workflowDate;

    public OnlineWorkflowDetails(String workflowId) {
        this.workflowId = workflowId;
    }

    public DateTime getWorkflowDate() {
        return workflowDate;
    }

    public void setWorkflowDate(DateTime workflowDate) {
        this.workflowDate = workflowDate;
    }

    public String getWorkflowId() {
        return workflowId;
    }

}
