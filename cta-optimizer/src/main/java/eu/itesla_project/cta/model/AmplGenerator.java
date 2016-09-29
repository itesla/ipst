/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplGenerator extends AmplElement  {
    private final String name;

    public AmplGenerator(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<Object> getOrderedValues () {
        return newArrayList(name);
    }

    @Override
    public String toString() {
        return name;
    }

}
