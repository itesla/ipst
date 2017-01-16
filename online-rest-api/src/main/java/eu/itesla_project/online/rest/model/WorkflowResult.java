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
public class WorkflowResult {

    private final String processId;
    private final String workflowId;
    private final String basecase;
    private final List<PreContingencyResult> preContingency = new ArrayList<PreContingencyResult>();
    private final List<PostContingencyResult> postContingency = new ArrayList<PostContingencyResult>();

    public WorkflowResult(String processId, String workflowId, String basecase) {
        this.processId = Objects.requireNonNull(processId);
        this.workflowId = Objects.requireNonNull(workflowId);
        this.basecase = Objects.requireNonNull(basecase);
    }

    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    @JsonProperty("workflowId")
    public String getWorkflowId() {
        return workflowId;
    }

    @JsonProperty("basecase")
    public String getBasecase() {
        return basecase;
    }

    @JsonProperty("preContingency")
    public List<PreContingencyResult> getPreContingency() {
        return preContingency;
    }

    public void addPreContingencies(List<PreContingencyResult> preContingency) {
        this.preContingency.addAll(preContingency);
    }

    public void addPreContingency(PreContingencyResult pcr) {
        this.preContingency.add(pcr);
    }

    @JsonProperty("postContingency")
    public List<PostContingencyResult> getPostContingency() {
        return postContingency;
    }

    public void allPostContingencyList(List<PostContingencyResult> postContingencyList) {
        this.postContingency.addAll(postContingencyList);
    }

    public void addPostContingency(PostContingencyResult pcr) {
        this.postContingency.add(pcr);
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
