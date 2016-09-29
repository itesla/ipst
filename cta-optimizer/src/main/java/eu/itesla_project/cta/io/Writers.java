/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.io;

import java.util.ServiceLoader;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public final class Writers {

    private static final ServiceLoader<IpsoWriter> WRITERS_LOADER = ServiceLoader.load(IpsoWriter.class);

    private Writers() {}

    public static IpsoWriter findWriterFor(IpsoOutputFormat format) {
        for (IpsoWriter writer : WRITERS_LOADER) {
            if (writer.getFormat() == format) {
                return writer;
            }
        }
        return null;
    }
}
