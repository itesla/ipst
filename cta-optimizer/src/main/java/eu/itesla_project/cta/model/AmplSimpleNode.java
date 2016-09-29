/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AmplSimpleNode extends AmplElement {
    private String id;

    public AmplSimpleNode(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "name must not be null or empty");
        this.id = name;
    }

    public String getId() {
        return id;
    }

    @Override
    public List<Object> getOrderedValues() {
        return Lists.newArrayList(this.id);
    }
}
