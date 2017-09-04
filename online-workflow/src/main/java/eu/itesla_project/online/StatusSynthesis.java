/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online;

import java.io.Serializable;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class StatusSynthesis implements Serializable{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String workflowId;
    private final WorkflowStatusEnum status;

    public StatusSynthesis(String workflowId, WorkflowStatusEnum status)
    {
        this.workflowId=workflowId;
        this.status=status;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public WorkflowStatusEnum getStatus() {
        return status;
    }

}
