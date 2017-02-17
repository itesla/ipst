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

    private final int state;
    private final boolean convergence;
    private final boolean safe;
    private final List<Violation> violations = new ArrayList<>();

    public PreContingencyResult(int state, boolean safe, boolean convergence) {
        this.state = state;
        this.safe = safe;
        this.convergence = convergence;
    }

    @JsonProperty("state")
    public int getState() {
        return state;
    }

    @JsonProperty("convergence")
    public boolean getConvergence() {
        return convergence;
    }

    @JsonProperty("safe")
    public boolean getSafe() {
        return safe;
    }

    @JsonProperty("violations")
    public List<Violation> getViolations() {
        return violations;
    }

    public void addViolations(List<Violation> violations) {
        Objects.requireNonNull(violations);
        this.violations.addAll(violations);
    }

    public void addViolation(Violation violation) {
        Objects.requireNonNull(violation);
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
        sb.append("class PreContingencyResult {").append(System.lineSeparator());

        sb.append("    state: ").append(toIndentedString(state)).append(System.lineSeparator());
        sb.append("    convergence: ").append(toIndentedString(convergence)).append(System.lineSeparator());
        sb.append("    safe: ").append(toIndentedString(safe)).append(System.lineSeparator());
        sb.append("    violations: ").append(toIndentedString(violations)).append(System.lineSeparator());
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
