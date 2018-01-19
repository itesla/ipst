/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_events_adder.events;

import java.util.Arrays;
import java.util.List;

/**
 * @author Silvia Machado <machados@aia.es>
 */
public final class EventsStaticData {

    private EventsStaticData() {
    }

    public static final String    LINE_MODEL            = "iPSL.Electrical.Events.PwLineFault";
    public static final String    BUS_MODEL            = "iPSL.Electrical.Events.PwFault";
    public static final String    BREAKER_MODEL        = "iPSL.Electrical.Events.Breaker";
    public static final String    LINE_OPEN_REC_MODEL    = "iPSL.Electrical.Branches.PwLinewithOpeningReceiving";
    public static final String    LINE_2_OPEN_MODEL    = "iPSL.Electrical.Branches.PwLine2Openings";
    public static final String    BANK_MODIF_MODEL    = "iPSL.Electrical.Banks.PwCapacitorBankWithModification";
    public static final String    LOAD_VAR_MODEL        = "iPSL.Electrical.Loads.PwLoadwithVariation";

    public static final String        LINE_FAULT            = "LINE_FAULT";
    public static final String        BUS_FAULT            = "BUS_FAULT";
    public static final String        LINE_OPEN_REC        = "LINE_OPEN_REC";
    public static final String        LINE_2_OPEN            = "LINE_2_OPEN";
    public static final String        BANK_MODIF            = "BANK_MODIF";
    public static final String        LOAD_VAR            = "LOAD_VAR";
    public static final String        BREAKER                = "BREAKER";

    public static final List<String> EVENT_TYPES        =  Arrays.asList(new String[] {BUS_FAULT, LINE_FAULT, LINE_OPEN_REC, BANK_MODIF, LOAD_VAR, BREAKER});

    //Other parameters of the events
    public static String VO_REAL1    = "Vo_real1";
    public static String VO_IMG1    = "Vo_img1";
    public static String VO_REAL2    = "Vo_real2";
    public static String VO_IMG2    = "Vo_img2";
    public static String R1            = "R1";
    public static String X1            = "X1";
    public static String G1            = "G1";
    public static String B1            = "B1";
    public static String P2            = "P2";
    public static String Q2            = "Q2";

}
