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
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2016-10-06T14:01:02.692Z")
public class WorkflowInfo {

    private String id = null;
    private String baseCase = null;
    private WorkflowResult workflowResult = null;

    /**
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     **/

    @JsonProperty("baseCase")
    public String getBaseCase() {
        return baseCase;
    }

    public void setBaseCase(String baseCase) {
        this.baseCase = baseCase;
    }

    /**
     **/

    @JsonProperty("workflowResult")
    public WorkflowResult getWorkflowResult() {
        return workflowResult;
    }

    public void setWorkflowResult(WorkflowResult workflowResult) {
        this.workflowResult = workflowResult;
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
        sb.append("class WorkflowInfo {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    baseCase: ").append(toIndentedString(baseCase)).append("\n");
        sb.append("    workflowResult: ").append(toIndentedString(workflowResult)).append("\n");
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
