/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.db.debug;

import java.util.List;

/**
 * @author Quinary <itesla@quinary.com>
 */
public class BusData implements EquipmentData {

    private final String busId;
    private final double ratedVoltage;
    private final double voltageMagnitude;
    private final double voltageAngle;
    private final boolean connectedToGenerator;
    private final List<String> generators;
    private final double activeInjection;
    private final double reactiveInjection;
    private final boolean connectedToLoad;
    private final List<String> loads;
    private final double activeAbsorption;
    private final double reactiveAbsorption;
    private final double activePower;
    private final double reactivePower;
    private boolean slack;


    public BusData(String busId, double ratedVoltage, double voltageMagnitude, double voltageAngle, boolean connectedToGenerator, List<String> generators,
                   double activeInjection, double reactiveInjection, boolean connectedToLoad, List<String> loads, double activeAbsorption, double reactiveAbsorption,
                   double activePower, double reactivePower, boolean slack) {
        this.busId = busId;
        this.ratedVoltage = ratedVoltage;
        this.voltageMagnitude = voltageMagnitude;
        this.voltageAngle = voltageAngle;
        this.connectedToGenerator = connectedToGenerator;
        this.generators = generators;
        this.activeInjection = activeInjection;
        this.reactiveInjection = reactiveInjection;
        this.connectedToLoad = connectedToLoad;
        this.loads = loads;
        this.activeAbsorption = activeAbsorption;
        this.reactiveAbsorption = reactiveAbsorption;
        this.activePower = activePower;
        this.reactivePower = reactivePower;
        this.slack = slack;
    }

    public void setSlack(boolean slack) {
        this.slack = slack;
    }

    public static String[] getFields() {
        return new String[]{
            "id",
            "rated voltage",
            "voltage magnitude",
            "voltage angle",
            "connected to generator",
            "generators",
            "Pg",
            "Qg",
            "connected to load",
            "loads",
            "Pc",
            "Qc",
            "active power",
            "reactive power",
            "slack"
        };
    }

    @Override
    public String getFieldValue(String fieldName) {
        switch (fieldName) {
            case "id":
                return busId;
            case "rated voltage":
                return Double.toString(ratedVoltage);
            case "voltage magnitude":
                return Double.toString(voltageMagnitude);
            case "voltage angle":
                return Double.toString(voltageAngle);
            case "connected to generator":
                return Boolean.toString(connectedToGenerator);
            case "generators":
                return listToString(generators);
            case "Pg":
                return Double.toString(activeInjection);
            case "Qg":
                return Double.toString(reactiveInjection);
            case "connected to load":
                return Boolean.toString(connectedToLoad);
            case "loads":
                return listToString(loads);
            case "Pc":
                return Double.toString(activeAbsorption);
            case "Qc":
                return Double.toString(reactiveAbsorption);
            case "active power":
                return Double.toString(activePower);
            case "reactive power":
                return Double.toString(reactivePower);
            case "slack":
                return Boolean.toString(slack);
            default:
                throw new RuntimeException("no " + fieldName + " available in bus data");
        }
    }

    private String listToString(List<String> list) {
        String listString = "";
        for (String string : list) {
            listString += string + ";";
        }
        if (!listString.isEmpty()) {
            listString = listString.substring(0, listString.length() - 1);
        }
        return listString;
    }

}
