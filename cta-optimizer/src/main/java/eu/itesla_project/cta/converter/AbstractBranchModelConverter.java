/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.IpsoEquipment;
import eu.itesla_project.iidm.network.CurrentLimits;
import eu.itesla_project.iidm.network.Identifiable;
import eu.itesla_project.iidm.network.TwoTerminalsConnectable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */

abstract class AbstractBranchModelConverter<F extends Identifiable, T extends IpsoEquipment> extends AbstractModelConverter<F,T>{

    protected AbstractBranchModelConverter(ConversionContext context) {
        super(context);
    }

    protected float findMaxCurrentPermanentLimitFor(TwoTerminalsConnectable connectable) {
        checkArgument(connectable != null, "connectable must not be null");
        Optional<CurrentLimits> currentLimits1 = Optional.ofNullable(connectable.getCurrentLimits1());
        Optional<CurrentLimits> currentLimits2 = Optional.ofNullable(connectable.getCurrentLimits2());

        if (currentLimits1.isPresent() && currentLimits2.isPresent()) {
            float perm1 = currentLimits1.get().getPermanentLimit();
            float perm2 = currentLimits2.get().getPermanentLimit();

            if (!Float.isNaN(perm1) && !Float.isNaN(perm2)) {
                return Math.max(perm1,perm2);
            }
            else if (!Float.isNaN(perm1)) {
                return perm1;
            }else if(!Float.isNaN(perm2)) {
                return perm2;
            }else {
                return Float.NaN;
            }
        }else if (currentLimits1.isPresent()) {
            return currentLimits1.get().getPermanentLimit();
        } else if (currentLimits2.isPresent()) {
            return currentLimits2.get().getPermanentLimit();
        } else {
            return Float.NaN;
        }
    }
}
