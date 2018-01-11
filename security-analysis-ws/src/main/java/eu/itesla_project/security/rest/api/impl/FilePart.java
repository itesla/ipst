/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.impl;

import java.io.InputStream;
import java.util.Objects;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
*/
public class FilePart {

    private final String filename;
    private final InputStream inputStream;

    public FilePart(String filename, InputStream content) {
        this.filename = Objects.requireNonNull(filename);
        this.inputStream = Objects.requireNonNull(content);
    }

    String getFilename() {
        return filename;
    }

    InputStream getInputStream() {
        return inputStream;
    }
}
