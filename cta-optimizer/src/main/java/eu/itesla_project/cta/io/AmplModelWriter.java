/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.io;

import eu.itesla_project.cta.model.AmplModel;
import eu.itesla_project.cta.service.AmplConstants;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplModelWriter {

    public void write(AmplModel model, String toPath, String templatePath) throws IOException {
        VelocityEngine templateEngine = new VelocityEngine();
        templateEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
        templateEngine.init();
        Template template = templateEngine.getTemplate(AmplConstants.TEMPLATE_FILE);

        VelocityContext context = new VelocityContext(formatParameters(model));
        FileWriter writer = new FileWriter(new File(toPath));
        template.merge(context, writer);

        writer.close();
    }

    private Map<String, List<String>> formatParameters(AmplModel model) {
        return stream(AmplParameters.values())
                .collect(toMap(AmplParameters::getName, parameter -> parameter.format(model)));
    }
}
