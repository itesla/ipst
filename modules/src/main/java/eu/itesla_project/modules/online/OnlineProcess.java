/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.online;

import java.util.HashMap;

import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
public class OnlineProcess {

    private final String id;
    private final String name;
    private final String owner;
    private final DateTime date;
    private final DateTime creationDate;
    private final String caseType;
    private final Map<String, String> workflowsMap = new HashMap<String, String>();

    public OnlineProcess(@JsonProperty("id")String id, @JsonProperty("name")String name, @JsonProperty("owner")String owner, @JsonProperty("caseType")String caseType, @JsonProperty("date")DateTime date, @JsonProperty("creationDate")DateTime creationDate) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.owner = Objects.requireNonNull(owner);
        this.caseType = Objects.requireNonNull(caseType);
        this.date = Objects.requireNonNull(date);
        this.creationDate = Objects.requireNonNull(creationDate);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public DateTime getDate() {
        return date;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public Map<String, String> getWorkflowsMap() {
        return workflowsMap;
    }

    public void addWorkflow(String bcase, String wid) {
        workflowsMap.put(Objects.requireNonNull(bcase), Objects.requireNonNull(wid));
    }

    public String getCaseType() {
        return caseType;
    }

}
