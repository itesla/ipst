/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import java.nio.file.Path;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoOptimizationResults {
    private final IpsoOptimizationStatus status;
    private final Path csvFile;
    private final Path outFile;
    private final IpsoSolution solution;

    public IpsoOptimizationResults(IpsoOptimizationStatus status, Path outFile, Path csvFile, IpsoSolution solution) {
        this.status = status;
        this.csvFile = csvFile;
        this.outFile = outFile;
        this.solution = solution;
    }

    public IpsoOptimizationStatus getStatus() {
        return status;
    }

    public IpsoSolution getSolution() {
        return solution;
    }

    public boolean hasSolutionFound() {
        return solution != null;
    }
}
