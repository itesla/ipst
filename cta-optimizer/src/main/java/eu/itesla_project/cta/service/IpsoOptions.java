/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.service;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoOptions {

    private final boolean transformerRegulateTakenIntoAccount;
    private final boolean transformerSetpointTakenIntoAccount;
    private final boolean generatorSetpointTakenIntoAccount;
    private final boolean voltageLimitsTakenIntoAccount;
    private final boolean currentLimitsTakenIntoAccount;
    private final boolean actionCorrectivesTakenIntoAccount;
    private final boolean continueCorrectiveActionTakenIntoAccount;

    /**
     * Constructor
     */
    private IpsoOptions(boolean transformerRegulateTakenIntoAccount,
                        boolean transformerSetpointTakenIntoAccount,
                        boolean generatorSetpointTakenIntoAccount,
                        boolean voltageLimitsTakenIntoAccount,
                        boolean currentLimitsTakenIntoAccount,
                        boolean actionCorrectivesTakenIntoAccount,
                        boolean continueCorrectiveActionTakenIntoAccount) {
        this.transformerRegulateTakenIntoAccount = transformerRegulateTakenIntoAccount;
        this.transformerSetpointTakenIntoAccount = transformerSetpointTakenIntoAccount;
        this.generatorSetpointTakenIntoAccount = generatorSetpointTakenIntoAccount;
        this.voltageLimitsTakenIntoAccount = voltageLimitsTakenIntoAccount;
        this.currentLimitsTakenIntoAccount = currentLimitsTakenIntoAccount;
        this.actionCorrectivesTakenIntoAccount = actionCorrectivesTakenIntoAccount;
        this.continueCorrectiveActionTakenIntoAccount = continueCorrectiveActionTakenIntoAccount;
    }

    /**
     *
     * @return true if the setpoint of transformer is fixed.
     * If yes, a voltage setpoint constraint is mandatory on the regulated node.
     */
    public boolean isTransformerSetpointTakenIntoAccount() {
        return transformerSetpointTakenIntoAccount;
    }

    /**
     * @return true if the transformer regulates voltage without fixed setpoint.
     * Ipso can then chosen the best setpoint.
     * Remark:
     * return true is the option setpoint transformer is already taken into account
     * otherwize the regulate transformer option is considered.
     */
    public boolean isTransformerRegulateTakenIntoAccount() {
        if (transformerSetpointTakenIntoAccount) {
            return true;
        }
        else {
            return transformerRegulateTakenIntoAccount;
        }
    }

    public boolean isGeneratorSetpointTakenIntoAccount() {
        return generatorSetpointTakenIntoAccount;
    }

    public boolean isVoltageLimitsTakenIntoAccount() {
        return voltageLimitsTakenIntoAccount;
    }

    public boolean isCurrentLimitsTakenIntoAccount() {
        return currentLimitsTakenIntoAccount;
    }

    public boolean isActionCorrectivesTakenIntoAccount() {
        return actionCorrectivesTakenIntoAccount;
    }

    public boolean isContinueCorrectiveActionTakenIntoAccount() {
        return continueCorrectiveActionTakenIntoAccount;
    }

    /**
     * Factory method
     * @return opf options
     */
    public static IpsoOptions createOptionsForOpf() {
        return  new IpsoOptions(
                false, // transformerRegulateTakenIntoAccount;
                false, // transformerSetpointTakenIntoAccount;
                true,  // generatorSetpointTakenIntoAccount;
                false, // voltageLimitsTakenIntoAccount;
                false, // currentLimitsTakenIntoAccount;
                false, // action corrective taken into account
                true   // continue corrective actions taken into acount
        );
    }

    /**
     * Factory method
     * @return scopf options
     */
    public static IpsoOptions createOptionsForScOpf() {
        return  new IpsoOptions(
                true, // transformer Regulate Taken into account;
                true, // transformer Setpoint Taken into account;
                true, // generator Setpoint Taken into account;
                true, // voltage Limits Taken into account;
                true, // current Limits Taken into account;
                true, // action corrective taken into account
                true // continue corrective actions taken into acount
        );
    }
}
