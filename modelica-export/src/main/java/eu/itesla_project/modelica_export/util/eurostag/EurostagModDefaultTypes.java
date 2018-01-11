/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_export.util.eurostag;

/**
 * @author Silvia Machado <machados@aia.es>
 */
public final class EurostagModDefaultTypes {
    //    public final static String PIN_TYPE                            = "iPSL.Connectors.ImPin";
    public static final String PIN_TYPE                            = "PowerSystems.Connectors.ImPin";
    public static final String INPUT_PIN_TYPE                    = "Modelica.Blocks.Interfaces.RealInput";
    public static final String OUTPUT_PIN_TYPE                    = "Modelica.Blocks.Interfaces.RealOutput";

    public static final String DEFAULT_PIN_TYPE                    = "iPSL.Connectors.ImPin"; // iPSL.Connectors.PwPin 20140515

    public static final String DEFAULT_BUS_TYPE                 = "iPSL.Electrical.Buses.Bus";

    public static final String DEFAULT_DETAILED_TRAFO_TYPE        = "iPSL.Electrical.Branches.Eurostag.PwPhaseTransformer";

    public static final String DEFAULT_FIXED_TRAFO_TYPE            = "iPSL.Electrical.Branches.Eurostag.PwTransformer_2";

    public static final String DEFAULT_GEN_TYPE                    = "iPSL.Electrical.Machines.Eurostag.PwGeneratorM2S";

    public static final String DEFAULT_GEN_LOAD_TYPE            = "iPSL.Electrical.Loads.Eurostag.PwLoadPQ";

    public static final String DEFAULT_LINE_TYPE                = "iPSL.Electrical.Branches.PwLine_2";

    public static final String DEFAULT_OPEN_LINE_TYPE            = "iPSL.Electrical.Branches.PwOpenLine";

    public static final String DEF_SEN_OPEN_LINE_TYPE            = "iPSL.Electrical.Branches.PwLinewithOpeningSending";

    public static final String DEF_REC_OPEN_LINE_TYPE            = "iPSL.Electrical.Branches.PwLinewithOpeningReceiving";

    public static final String DEFAULT_LOAD_TYPE                = "iPSL.Electrical.Loads.Eurostag.PwLoadVoltageDependence";

    public static final String LOAD_VOLTAGE_DEP_TYPE            = "iPSL.Electrical.Loads.PwLoadVoltageDependence";

    public static final String LOAD_FREQ_DEP                    = "iPSL.Electrical.Loads.PwLoadFrequencyDependence";

    public static final String DEFAULT_CAPACITOR_TYPE            = "iPSL.Electrical.Banks.PwCapacitorBank";

    public static final String M1S_INIT_MODEL                    = "iPSL.Electrical.Machines.Eurostag.DYNModelM1S_INIT";
    public static final String M2S_INIT_MODEL                    = "iPSL.Electrical.Machines.Eurostag.DYNModelM2S_INIT";

    public static final String M1S_MACHINES                        = "iPSL.Electrical.Machines.Eurostag.PwGeneratorM1S";
    public static final String M2S_MACHINES                        = "iPSL.Electrical.Machines.Eurostag.PwGeneratorM2S";

    private EurostagModDefaultTypes() {
    }
}
