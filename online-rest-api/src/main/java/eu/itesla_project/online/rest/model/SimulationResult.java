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
public class SimulationResult   {
  
  private Integer state = null;
  private Boolean convergence = Boolean.TRUE;
  private Boolean safe = Boolean.TRUE;
  private List<Violation> violations = new ArrayList<Violation>();

  /**
   **/
  
  @JsonProperty("state")
  public Integer getState() {
    return state;
  }
  public void setState(Integer state) {
    this.state = state;
  }

  /**
   **/
  
  @JsonProperty("convergence")
  public Boolean getConvergence() {
    return convergence;
  }
  public void setConvergence(Boolean convergence) {
    this.convergence = convergence;
  }

  /**
   **/
  
  @JsonProperty("safe")
  public Boolean getSafe() {
    return safe;
  }
  public void setSafe(Boolean safe) {
    this.safe = safe;
  }

  /**
   **/
  
  @JsonProperty("violations")
  public List<Violation> getViolations() {
    return violations;
  }
  public void setViolations(List<Violation> violations) {
    this.violations = violations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimulationResult simulationResult = (SimulationResult) o;
    return Objects.equals(state, simulationResult.state) &&
        Objects.equals(convergence, simulationResult.convergence) &&
        Objects.equals(safe, simulationResult.safe) &&
        Objects.equals(violations, simulationResult.violations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, convergence, safe, violations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SimulationResult {\n");
    
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    convergence: ").append(toIndentedString(convergence)).append("\n");
    sb.append("    safe: ").append(toIndentedString(safe)).append("\n");
    sb.append("    violations: ").append(toIndentedString(violations)).append("\n");
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

