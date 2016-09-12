/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import eu.itesla_project.commons.io.ModuleConfig;
import eu.itesla_project.commons.io.PlatformConfig;

import java.nio.file.Path;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoOptimizerConfiguration {

    private String ipsoWorkingDirPrefix = "itesla_ipso_";
    private String amplWorkingDirPrefix = "itesla_ampl_";
    private boolean debug = true;
    private int priority = 1;
    private Path solverPath;
    private Path solverRessourcePath;
    private Path amplPath;
    private Path ipsoPath;

    public IpsoOptimizerConfiguration(Path solverPath, Path ipsoPath, Path amplDir, Path amplResourcePath) {
        this.solverPath = solverPath;
        this.ipsoPath = ipsoPath;
        this.amplPath = amplDir;
        this.solverRessourcePath = amplResourcePath;
    }

    private IpsoOptimizerConfiguration() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("ipso");
        ipsoPath = config.getPathProperty("ipso", null);
        amplPath = config.getPathProperty("ampl", null);
        solverPath = config.getPathProperty("solver", null);
        solverRessourcePath = config.getPathProperty("solverResources", null);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Path getIpsoPath() {
        return ipsoPath;
    }

    public Path getSolverPath() {
        return solverPath;
    }

    public Path getAmplPath() {
        return amplPath;
    }

    public Path getSolverRessourcePath() {
        return solverRessourcePath;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isDebug() {
        return debug;
    }

    public static IpsoOptimizerConfiguration createOptimizationConfiguration() {
        return new IpsoOptimizerConfiguration();
    }

    public String getAmplWorkingDirPrefix() {
        return amplWorkingDirPrefix;
    }
}
