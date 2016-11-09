/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.online;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class OnlineProcess {
	
	private String id;
	private String name;
	private String owner;
	private DateTime date;
	private DateTime creationDate;
	private String caseType;
	private Map<String,String> workflowsMap = new HashMap<String,String>();
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public DateTime getDate() {
		return date;
	}
	public void setDate(DateTime date) {
		this.date = date;
	}
	public DateTime getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(DateTime creationDate) {
		this.creationDate = creationDate;
	}
	public Map<String, String> getWorkflowsMap() {
		return workflowsMap;
	}
	public void addWorkflow(String bcase, String wid) {
		workflowsMap.put(bcase, wid);
		
	}
	public String getCaseType() {
		return caseType;
	}
	public void setCaseType(String caseType) {
		this.caseType = caseType;
	}
	
		
	

}
