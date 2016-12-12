/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public class WorkflowResult {

    private String processId = null;
    private String workflowId = null;
    private String basecase = null;
    private List<PreContingencyResult> preContingency = new ArrayList<PreContingencyResult>();
    private List<PostContingencyResult> postContingency = new ArrayList<PostContingencyResult>();

    /**
     **/

    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     **/

    @JsonProperty("workflowId")
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     **/

    @JsonProperty("basecase")
    public String getBasecase() {
        return basecase;
    }

    public void setBasecase(String basecase) {
        this.basecase = basecase;
    }

    /**
     **/

    @JsonProperty("preContingency")
    public List<PreContingencyResult> getPreContingency() {
        return preContingency;
    }

    public void setPreContingency(List<PreContingencyResult> preContingency) {
        this.preContingency = preContingency;
    }

    /**
     **/

    @JsonProperty("postContingency")
    public List<PostContingencyResult> getPostContingency() {
        return postContingency;
    }

    public void setPostContingency(List<PostContingencyResult> postContingency) {
        this.postContingency = postContingency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowResult workflowResult = (WorkflowResult) o;
        return Objects.equals(processId, workflowResult.processId)
                && Objects.equals(workflowId, workflowResult.workflowId)
                && Objects.equals(basecase, workflowResult.basecase)
                && Objects.equals(preContingency, workflowResult.preContingency)
                && Objects.equals(postContingency, workflowResult.postContingency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, workflowId, basecase, preContingency, postContingency);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class WorkflowResult {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
        sb.append("    basecase: ").append(toIndentedString(basecase)).append("\n");
        sb.append("    preContingency: ").append(toIndentedString(preContingency)).append("\n");
        sb.append("    postContingency: ").append(toIndentedString(postContingency)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
