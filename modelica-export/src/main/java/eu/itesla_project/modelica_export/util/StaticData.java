/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_export.util;


/**
 * @author Silvia Machado <machados@aia.es>
 */
public final class StaticData {

    //SOURCE ENGINES
    public static final String        EUROSTAG            = "eurostag";
    public static final String        PSSE                = "psse";

    //NAMES PREFIXES
    public static final String        PREF_BUS            = "bus_";
    public static final String        PREF_GEN            = "gen_";
    public static final String        PREF_FIX_INJ        = "fixinj_";
    public static final String        PREF_LINE            = "line_";
    public static final String        PREF_TRAFO            = "trafo_";
    public static final String        PREF_LOAD            = "load_";
    public static final String        PREF_CAP            = "cap_";
    public static final String        PREF_REG            = "reg_";

    public static final String        POWERSYSTEMS        = "PowerSystems";

    ///Según la API de IIDM dan por supuesto que la potencia nominal es 100
    /**
     * Some Modelica parameters
     */
    public static final float        SNREF_VALUE            = 100;
    public static final String        SNREF                = "SNREF";
    public static final String        VO_REAL                = "Vo_real";
    public static final String        VO_IMG                = "Vo_img";
    public static final String        R                    = "R";
    public static final String        X                    = "X";
    public static final String        G                    = "G";
    public static final String        B                    = "B";
    public static final String        V_0                    = "V_0";
    public static final String        ANGLE_0                = "angle_0";



    /**
     * Modelica static and structural data
     */
    public static final String         PARAM_TYPE            = "parameter Real";
    public static final String        PARAMETER            = "parameter";

    public static final String         OMEGAREF_NAME        = "omegaRef";

    public static final String         ANNOT                = ") annotation (Placement(transformation()));";
    public static final String         ANNOT_CONNECT        = ") annotation (Line());";
    public static final String         CONNECT                = "connect(";

    public static final String        PIN                    = "pin_";
    public static final String        INIT_VAR            = "init_";

    public static final String         POSITIVE_PIN        = "p";
    public static final String         NEGATIVE_PIN        = "n";
    public static final String        HIN_PIN                = "SN";
    public static final String        SN_PIN                = "HIn";
    public static final String        OMEGA_PIN            = "omega";

    public static final String         GEN_SORTIE_PIN        = "sortie";
    public static final String        GEN_OMEGAREF_PIN    = "omegaRef";

    public static final String        MO                    = "mo";
    public static final String        MO_INIT                = "init_mo";
    public static final String        MO_EXTENSION        = ".mo";
    public static final String        MO_LIB_EXTENSION    = "_Lib.mo";
    public static final String        MO_INIT_EXTENSION    = "_Init.mo";

    public static final String        NEW_LINE            = System.getProperty("line.separator");
    public static final String        WHITE_SPACE            = " ";
    public static final String        DOT                    = "\\.";

    public static final String        MODEL                = "model ";
    public static final String        WITHIN                = "within ;";
    public static final String        EQUATION            = "equation";
    public static final String        END_MODEL            = "end ";
    public static final String        INITIALIZATION        = "_Initialization";
    public static final String        INIT                = "_init";
    public static final String        SEMICOLON            = ";";
    public static final String        COMMENT                = "//";

    public static final String        MTC_PREFIX_NAME        = "MTC_";


    public static final String        CON_OTHERS            = "// Connecting OTHERS";

    private StaticData() {
    }

}
