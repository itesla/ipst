/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class WorkflowInfo {

    private final String id;
    private final String baseCase;
    private final WorkflowResult workflowResult;

    public WorkflowInfo(String workflowId, String baseCase, WorkflowResult workflowResult) {
        this.id = Objects.requireNonNull(workflowId);
        this.baseCase = Objects.requireNonNull(baseCase);
        this.workflowResult = Objects.requireNonNull(workflowResult);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("baseCase")
    public String getBaseCase() {
        return baseCase;
    }

    @JsonProperty("workflowResult")
    public WorkflowResult getWorkflowResult() {
        return workflowResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowInfo workflowInfo = (WorkflowInfo) o;
        return Objects.equals(id, workflowInfo.id) && Objects.equals(baseCase, workflowInfo.baseCase)
                && Objects.equals(workflowResult, workflowInfo.workflowResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, baseCase, workflowResult);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class WorkflowInfo {").append(System.lineSeparator());

        sb.append("    id: ").append(toIndentedString(id)).append(System.lineSeparator());
        sb.append("    baseCase: ").append(toIndentedString(baseCase)).append(System.lineSeparator());
        sb.append("    workflowResult: ").append(toIndentedString(workflowResult)).append(System.lineSeparator());
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
        return o.toString().replace(System.lineSeparator(), System.lineSeparator()+"    ");
    }
}
