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
import java.util.Date;
import java.util.List;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class Process {

    private final String id;
    private final String name;
    private final String owner;
    private final Date date;
    private final Date creationDate;
    private final List<WorkflowInfo> workflows = new ArrayList<>();

    public Process(String id, String name, String owner, Date date, Date creationDate) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.owner = owner;
        this.date = Objects.requireNonNull(date);
        this.creationDate = Objects.requireNonNull(creationDate);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonProperty("date")
    public Date getDate() {
        return date;
    }

    @JsonProperty("creationDate")
    public Date getCreationDate() {
        return creationDate;
    }

    @JsonProperty("workflows")
    public List<WorkflowInfo> getWorkflows() {
        return workflows;
    }

    public void addWorkflowInfo(WorkflowInfo workflowInfo) {
        this.workflows.add(workflowInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Process process = (Process) o;
        return Objects.equals(id, process.id) && Objects.equals(name, process.name)
                && Objects.equals(owner, process.owner) && Objects.equals(date, process.date)
                && Objects.equals(creationDate, process.creationDate) && Objects.equals(workflows, process.workflows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, owner, date, creationDate, workflows);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Process {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
        sb.append("    date: ").append(toIndentedString(date)).append("\n");
        sb.append("    creationDate: ").append(toIndentedString(creationDate)).append("\n");
        sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
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
