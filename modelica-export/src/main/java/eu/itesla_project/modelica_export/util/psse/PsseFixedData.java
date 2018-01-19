/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_export.util.psse;

import java.util.Arrays;
import java.util.List;

/**
 * @author Silvia Machado <machados@aia.es>
 */
@SuppressWarnings("checkstyle:constantname")
public final class PsseFixedData {
    ///Segun la API de IIDM dan por supuesto que la potencia nominal es 100
    public static final float    SNREF_VALUE            = 100;
    public static final String    CONSTANT        = "const(k=0)";
    public static final String    CONST_NAME        = "const";
    public static final String    CONSTANT1        = "const1(k=-9999)";
    public static final String    CONST_NAME1        = "const1";
    public static final String    CONSTANT2        = "const2(k=9999)";
    public static final String    CONST_NAME2        = "const2";

    public static final String    ETERM            = "eterm";
    public static final String    V_0                = "V_0";
    public static final String    V_c0            = "V_c0";
    public static final String    EC0                = "Ec0";
    public static final String    ET0                = "Et0";
    public static final String    Mbase            = "Mbase";
    public static final String    M_b                = "M_b"; //Mbase in machines
    public static final String    ANGLEV0            = "anglev0";
    public static final String    ANGLE_0            = "angle_0";
    public static final String    PELEC            = "pelec";
    public static final String    QELEC            = "qelec";
    public static final String    P_0                = "P_0";
    public static final String    Q_0                = "Q_0";
    public static final String    PMECH            = "pmech";
    public static final String    p0                = "p0";
    public static final String    P0                = "P0";
    public static final String    V0                = "V0";
    public static final String    v0                = "v0";
    public static final String    S_p                = "S_p";
    public static final String    S_i                = "S_i";
    public static final String    S_y                = "S_y";
    public static final String    a                = "a";
    public static final String    b                = "b";
    public static final String    PQBRAK                = "PQBRAK";

    public static final String    VOEL_PIN        = "VOEL";
    public static final String    VUEL_PIN        = "VUEL";
    public static final String    VUEL1_PIN        = "VUEL1";
    public static final String    VUEL2_PIN        = "VUEL2";
    public static final String    VUEL3_PIN        = "VUEL3";
    public static final String    VOTHSG_PIN        = "VOTHSG";
    public static final String    VOTHSG2_PIN        = "VOTHSG2";
    public static final String    VT_PIN            = "VT";
    public static final String    ECOMP_PIN        = "ECOMP";
    public static final String    SIGNAL_PIN        = "Signal";
    public static final String    VCT_PIN            = "VCT";
    public static final String    Y_PIN            = "y";
    public static final String    PMECH_PIN            = "PMECH";
    public static final String    PMECH0_PIN            = "PMECH0";


    public static final List<String> SPECIAL_REGS = Arrays.asList(new String[] {PsseModDefaultTypes.SCRX,
                                                                                PsseModDefaultTypes.SEXS,
                                                                                PsseModDefaultTypes.IEEET2});

    private PsseFixedData() {
    }

}
