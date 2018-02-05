/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_export.util.eurostag;

import java.util.Arrays;
import java.util.List;

/**
 * @author Silvia Machado <machados@aia.es>
 */
public final class EurostagFixedData {
    ///Según la API de IIDM dan por supuesto que la potencia nominal es 100
    public static final String        PARAMETER            = "parameter";

    public static final String         OMEGAREF_NAME        = "omegaRef";

    public static final String         ANNOT                = ") annotation (Placement(transformation()));";
    public static final String         ANNOT_CONNECT        = ") annotation (Line());";
    public static final String         CONNECT                = "connect(";

    public static final String        HIN_PIN                = "SN";
    public static final String        SN_PIN                = "HIn";
    public static final String        OMEGA_PIN            = "omega";

    public static final String         GEN_SORTIE_PIN        = "sortie";
    public static final String        GEN_OMEGAREF_PIN    = "omegaRef";

    /**
     * Data about generators with transformer included
     */
    public static final String            TRAFO_INCLUDED        = "T";
    public static final String            TRAFO_NOT_INCLUDED    = "N";
    public static final String            IS_SATURATED        = "S";
    public static final String            IS_UNSATURATED        = "U";
    public static final List<String>    TRAFO_GEN_PARAMS    = Arrays.asList(new String[]{"V1", "V2", "U1N", "U2N", "SNtfo", "RTfoPu", "XTfoPu"});
    public static final List<String>    MACHINE_INIT_PAR    = Arrays.asList(new String[]{"lambdaF0", "lambdaD0", "lambdaAD0", "lambdaAQ0", "lambdaQ10", "lambdaQ20", "iD0", "iQ0", "teta0", "omega_0", "cm0", "efd0", "mDVPu"});
    public static final List<String>    MACHINE_PAR            = Arrays.asList(new String[]{"init_lambdaf", "init_lambdad", "init_lambdaad", "init_lambdaaq", "init_lambdaq1", "init_lambdaq2", "init_id", "init_iq", "init_theta", "init_omega", "init_cm", "init_efd", "WLMDVPu"});
    public static final List<String>    SATURATED_MACHINE    = Arrays.asList(new String[]{"snq", "snd", "mq", "md"});


    /**
     * MODELICA PARAMETER NAMES
     */
    public static final String    UR0            = "ur0";
    public static final String    UI0            = "ui0";
    public static final String    V_0            = "V_0";
    public static final String    ANGLE_0        = "angle_0";
    public static final String    VO_REAL        = "Vo_real";
    public static final String    VO_IMG        = "Vo_img";
    public static final String    P            = "P";
    public static final String    Q            = "Q";
    public static final String    SNOM        = "Snom";
    public static final String    PCU            = "Pcu";
    public static final String    PFE            = "Pfe";
    public static final String    IM            = "IM";
    public static final String    B0            = "B0";
    public static final String    G0            = "G0";
    public static final String    V1            = "V1";
    public static final String    V2            = "V2";
    public static final String    U1N            = "U1N";
    public static final String    U2N            = "U2N";
    public static final String    U1_NOM = "U1nom";
    @SuppressWarnings("checkstyle:constantname")
    @Deprecated
    public static final String    U1nom        = U1_NOM;
    public static final String    U2_NOM       = "U2nom";
    @SuppressWarnings("checkstyle:constantname")
    @Deprecated
    public static final String    U2nom        = U2_NOM;
    public static final String    UCC            = "Ucc";
    public static final String    THETA        = "theta";
    public static final String    ESAT        = "ESAT";
    public static final String    R            = "R";
    public static final String    X            = "X";
    public static final String    G            = "G";
    public static final String    B            = "B";
    @SuppressWarnings("checkstyle:constantname")
    public static final String    r            = "r";
    public static final String    ALPHA        = "alpha";
    public static final String    BETA        = "beta";
    public static final String    TRAFOINCLUDED    = "transformerIncluded";
    public static final String    SATURATED        = "Saturated";
    public static final String    INLMDV            = "IWLMDV";
    public static final String    TX                = "TX";
    public static final String    XD                = "XD";
    public static final String    XPD                = "XPD";
    public static final String    XSD                = "XSD";
    public static final String    TPD0            = "TPD0";
    public static final String    TSD0            = "TSD0";
    public static final String    XQ                = "XQ";
    public static final String    XPQ                = "XPQ";
    public static final String    XSQ                = "XSQ";
    public static final String    TPQ0            = "TPQ0";
    public static final String    TSQ0            = "TSQ0";
    public static final String    IENR            = "IENR";

    public static final String    SNTFO        = "SNtfo";
    public static final String    SN            = "SN";
    public static final String    RTFOPU        = "RTfoPu";
    public static final String    XTFOPU        = "XTfoPu";
    public static final String    SND            = "snd";
    public static final String    SNQ            = "snq";
    public static final String    MD            = "md";
    public static final String    MQ            = "mq";
    public static final String    RSTATIN        = "rStatIn";
    public static final String    LSTATIN     = "lStatIn";
    public static final String    MQ0PU         = "mQ0Pu";
    public static final String    MD0PU        = "mD0Pu";
    public static final String    PN            = "PN";
    public static final String    LDPU        = "lDPu";
    public static final String    RROTIN        = "rRotIn";
    public static final String    LROTIN        = "lRotIn";
    public static final String    RQ1PU        = "rQ1Pu";
    public static final String    LQ1PU        = "lQ1Pu";
    public static final String    RQ2PU        = "rQ2Pu";
    public static final String    LQ2PU        = "lQ2Pu";
    public static final String    MCANPU        = "mCanPu";
    public static final String    PNALT        = "PNALT";


    //M1S & M2S INIT
    public static final String    INIT_SNREF        = "SNREF";
    public static final String    INIT_SN            = "SN";
    public static final String    INIT_PN            = "PN";
    public static final String    INIT_PNALT        = "PNALT";
    public static final String    INIT_SNTFO        = "sNTfo";
    public static final String    INIT_UR0        = "ur0";
    public static final String    INIT_UI0        = "ui0";
    public static final String    INIT_P0            = "p0";
    public static final String    INIT_Q0            = "q0";
    public static final String    INIT_UNRESTFO    = "uNResTfo";
    public static final String    INIT_UNOMNW        = "uNomNw";
    public static final String    INIT_UNMACTFO    = "uNMacTfo";
    public static final String    INIT_UBMAC        = "uBMac";
    public static final String    INIT_RTFOIN        = "rTfoIn";
    public static final String    INIT_XTFOIN        = "xTfoIn";
    public static final String    INIT_NDSAT        = "nDSat";
    public static final String    INIT_NQSAT        = "nQSat";
    public static final String    INIT_MDSATIN    = "mDSatIn";
    public static final String    INIT_MQSATIN    = "mQSatIn";
    public static final String    INIT_RSTATIN    = "rStatIn";
    public static final String    INIT_LSTATIN    = "lStatIn";
    public static final String    INIT_MD0PU        = "mD0Pu";
    public static final String    INIT_PNOM        = "pNom";
    public static final String    INIT_OMEGA0        = "omega_0";
    public static final String    INIT_PPUWLMDV     = "pPuWLMDV";
    public static final String    INIT_IENR        = "IENR";

    //M1S INIT
    public static final String    INIT_MQ0PU        = "mQ0Pu";
    public static final String    INIT_LDPU        = "lDPu";
    public static final String    INIT_RROTIN        = "rRotIn";
    public static final String    INIT_LROTIN        = "lRotIn";
    public static final String    INIT_RQ1PU        = "rQ1Pu";
    public static final String    INIT_LQ1PU        = "lQ1Pu";
    public static final String    INIT_RQ2PU        = "rQ2Pu";
    public static final String    INIT_LQ2PU        = "lQ2Pu";
    public static final String    INIT_MCANPU        = "mCanPu";

    //M2S INIT
    public static final String    INIT_XD         = "XD";
    public static final String    INIT_XSD         = "XSD";
    public static final String    INIT_XPD         = "XPD";
    public static final String    INIT_TPDO         = "TPD0";
    public static final String    INIT_TSDO         = "TSD0";
    public static final String    INIT_XQ         = "XQ";
    public static final String    INIT_XPQ         = "XPQ";
    public static final String    INIT_XSQ         = "XSQ";
    public static final String    INIT_TPQO         = "TPQ0";
    public static final String    INIT_TSQO         = "TSQ0";
    public static final String    INIT_TX         = "TX";

    public static final String NSTEPS            = "nsteps";
    public static final String BO                = "Bo";

    public static final String OPENR            = "OpenR_end";

    private EurostagFixedData() {
    }

}
