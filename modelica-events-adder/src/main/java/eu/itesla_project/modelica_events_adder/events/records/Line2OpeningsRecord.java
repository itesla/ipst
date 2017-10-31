/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_events_adder.events.records;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.itesla_project.modelica_events_adder.events.EventsStaticData;
import eu.itesla_project.modelica_events_adder.events.utils.IIDMParameter;
import eu.itesla_project.modelica_events_adder.events.utils.StaticData;

/**
 * @author Silvia Machado <machados@aia.es>
 */
public class Line2OpeningsRecord extends EventRecord {


    public Line2OpeningsRecord(Record lineRecord, Event event, Map<String, Record> recordsMap) {
        super(event.getType(), event.getCIMDevice(), event.getParams());
        this.event = event;
        this.lineRecord = lineRecord;
        this.recordsMap = recordsMap;
        super.setModelicaType(EventsStaticData.LINE_2_OPEN_MODEL);

        for (String par : event.getParams()) {
            String name = par.split("=")[0];
            String value = par.split("=")[1];
            addParameter(name, value);
        }
    }

    @Override
    public void createModelicaName() {
//        String modelicaName = this.event.getCIMDevice();
        String modelicaName = this.lineRecord.getModelicaName();
        super.setModelicaName(modelicaName);

        getLineParameters();
    }

    @Override
    public void createRecord() {
        this.addValue(super.getModelicaType() + StaticData.WHITE_SPACE);
        this.addValue(super.getModelicaName());
        this.addValue(" (");
        this.addValue(StaticData.NEW_LINE);

        if (!iidmParameters.isEmpty()) {
            for (int i = 0; i < iidmParameters.size() - 1; i++) {
                this.addValue("\t " + iidmParameters.get(i).getName() + " = " + iidmParameters.get(i).getValue() + ",");
                this.addValue(StaticData.NEW_LINE);
            }
            this.addValue("\t " + iidmParameters.get(iidmParameters.size() - 1).getName() + " = " + iidmParameters.get(iidmParameters.size() - 1).getValue());
            this.addValue(StaticData.NEW_LINE);
        }

        this.addValue("\t " + StaticData.ANNOT);

        //Clear data
        iidmParameters = null;

    }

    @Override
    public String parseName(String name) {
        return null;
    }

    @Override
    public Line2OpeningsRecord getClassName() {
        return this;
    }


    /**
     * Gets R, X, G, B from the original line to set them in the line opening receiving.
     */
    private void getLineParameters() {
        String r = this.lineRecord.getParamsMap().get(StaticData.R);
        String x = this.lineRecord.getParamsMap().get(StaticData.X);
        String g = this.lineRecord.getParamsMap().get(StaticData.G);
        String b = this.lineRecord.getParamsMap().get(StaticData.B);

        addParameter(StaticData.R, r);
        addParameter(StaticData.X, x);
        addParameter(StaticData.G, g);
        addParameter(StaticData.B, b);
    }

    private void addParameter(String name, Object value) {
        this.iidmParameters.add(new IIDMParameter(name, value));
    }

    private Record lineRecord;
    private Event event;
    private List<IIDMParameter>    iidmParameters    = new ArrayList<IIDMParameter>();

    private Map<String, Record> recordsMap = new HashMap<String, Record>();
}
