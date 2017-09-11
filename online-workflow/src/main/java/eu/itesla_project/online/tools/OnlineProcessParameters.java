/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import org.joda.time.DateTime;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class OnlineProcessParameters {

    private String caseType;
    private Integer states;
    private String name;
    private String owner;
    private DateTime creationDate;
    private DateTime date;
    private String basecasesInterval;

    public OnlineProcessParameters() {
    }

    public void setName(String name) {
        this.name = name;

    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;

    }

    public void setStates(Integer states) {
        this.states = states;

    }

    public void setBasecasesInterval(String interval) {
        this.basecasesInterval = interval;

    }

    public void setCreationDate(DateTime date) {
        this.creationDate = date;

    }

    public void setDate(DateTime date) {
        this.date = date;

    }

    public void setOwner(String owner) {
        this.owner = owner;

    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public DateTime getDate() {
        return date;
    }

    public String getBasecasesInterval() {
        return basecasesInterval;
    }

    public String getCaseType() {
        return caseType;
    }

    public Integer getStates() {
        return states;
    }

    @Override
    public String toString() {
        return "OnlineProcessParameters [caseType=" + caseType + ", states=" + states + ", name=" + name + ", owner="
                + owner + ", creationDate=" + creationDate + ", date=" + date + ", basecasesInterval=" + basecasesInterval + "]";
    }


}
