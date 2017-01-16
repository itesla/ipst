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
public class PreContingencyResult {

    private final Integer state;
    private final Boolean convergence;
    private final Boolean safe;
    private final List<Violation> violations = new ArrayList<>();

    public PreContingencyResult(Integer state, boolean safe, boolean convergence) {
        this.state = Objects.requireNonNull(state);
        this.safe = Objects.requireNonNull(safe);
        this.convergence = Objects.requireNonNull(convergence);
    }

    @JsonProperty("state")
    public Integer getState() {
        return state;
    }

    @JsonProperty("convergence")
    public Boolean getConvergence() {
        return convergence;
    }

    @JsonProperty("safe")
    public Boolean getSafe() {
        return safe;
    }

    @JsonProperty("violations")
    public List<Violation> getViolations() {
        return violations;
    }

    public void addViolations(List<Violation> violations) {
        this.violations.addAll(violations);
    }

    public void addViolation(Violation violation) {
        this.violations.add(violation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PreContingencyResult preContingencyResult = (PreContingencyResult) o;
        return Objects.equals(state, preContingencyResult.state)
                && Objects.equals(convergence, preContingencyResult.convergence)
                && Objects.equals(safe, preContingencyResult.safe)
                && Objects.equals(violations, preContingencyResult.violations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, convergence, safe, violations);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PreContingencyResult {\n");

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
