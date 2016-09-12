/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoConstraint3WTransformerFlow extends IpsoConstraint<IpsoTwoWindingsTransformer>  {

     public IpsoConstraint3WTransformerFlow(IpsoTwoWindingsTransformer transformer, float min, float max, int world) {
        super(transformer,min,max,world);
    }

    @Override
    protected float getConstrainedAttributeValueFor(IpsoTwoWindingsTransformer equipment) {
        return 0;
    }

    @Override
    public List<Object> getOrderedValues() {
        return null;
    }

    @Override
    public List<String> getOrderedHeaders() {
        return null;
    }
}
