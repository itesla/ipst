/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class TestServiceUtility {

    private static TestServiceUtility singleton = null;

    public static TestServiceUtility getInstance() {
        if (singleton == null) {
            singleton = new TestServiceUtility();
        }
        return singleton;
    }

    private TestServiceUtility() {}

    public Path createDirectoryInTempWithNameOf(String name) throws IOException {
        Path wdir = Paths.get(System.getProperty("java.io.tmpdir"), name);
        Files.createDirectories(wdir);
        return wdir;
    }

    public Properties loadConfigurationFor(String filename) throws IOException {
        String configurationFilename = filename + ".properties";
        Path configurationFile = Paths.get(this.getClass().getResource(configurationFilename).getPath());
        Properties configuration = new Properties();
        configuration.load(Files.newInputStream(configurationFile));
        return configuration;
    }
}
