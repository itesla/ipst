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
public class PostContingencyResult {

    private final String contingency;
    private final List<SimulationResult> results = new ArrayList<>();

    public PostContingencyResult(String cont, List<SimulationResult> results) {
        this.contingency = Objects.requireNonNull(cont);
        Objects.requireNonNull(results);
        this.results.addAll(results);
    }

    @JsonProperty("contingency")
    public String getContingency() {
        return contingency;
    }

    @JsonProperty("results")
    public List<SimulationResult> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PostContingencyResult postContingencyResult = (PostContingencyResult) o;
        return Objects.equals(contingency, postContingencyResult.contingency)
                && Objects.equals(results, postContingencyResult.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contingency, results);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PostContingencyResult {\n");

        sb.append("    contingency: ").append(toIndentedString(contingency)).append("\n");
        sb.append("    results: ").append(toIndentedString(results)).append("\n");
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
