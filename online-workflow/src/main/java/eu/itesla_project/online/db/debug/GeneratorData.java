/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.db.debug;

/**
 *
 * @author Quinary <itesla@quinary.com>
 */
public class GeneratorData implements EquipmentData {

    private final String generatorId;
    private final String busId;
    private final boolean isConnected;
    private final double apparentPower;
    private final double activePower;
    private final double reactivePower;
    private final double nominalPower;
    private final double maxReactivePower;
    private final double minReactivePower;


    public GeneratorData(String generatorId, String busId, boolean isConnected, double apparentPower, double activePower,
                         double reactivePower, double nominalPower, double maxReactivePower, double minReactivePower) {
        this.generatorId = generatorId;
        this.busId = busId;
        this.isConnected = isConnected;
        this.apparentPower = apparentPower;
        this.activePower = activePower;
        this.reactivePower = reactivePower;
        this.nominalPower = nominalPower;
        this.maxReactivePower = maxReactivePower;
        this.minReactivePower = minReactivePower;
    }

    public static String[] getFields() {
        return new String[]{
            "id",
            "bus id",
            "connected",
            "apparent power",
            "active power",
            "reactive power",
            "nominal power",
            "max q",
            "min q"
        };
    }

    @Override
    public String getFieldValue(String fieldName) {
        switch (fieldName) {
            case "id":
                return generatorId;
            case "bus id":
                return busId;
            case "connected":
                return Boolean.toString(isConnected);
            case "apparent power":
                return Double.toString(apparentPower);
            case "active power":
                return Double.toString(activePower);
            case "reactive power":
                return Double.toString(reactivePower);
            case "nominal power":
                return Double.toString(nominalPower);
            case "max q":
                return Double.toString(maxReactivePower);
            case "min q":
                return Double.toString(minReactivePower);
            default:
                throw new RuntimeException("no " + fieldName + " available in generator data");
        }
    }


}
