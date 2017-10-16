/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modelica_export.records;

import eu.itesla_project.iidm.ddb.model.SimulatorInst;
import eu.itesla_project.iidm.ddb.service.DDBManager;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Equipments;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import eu.itesla_project.modelica_export.ModExportContext;
import eu.itesla_project.modelica_export.util.StaticData;
import eu.itesla_project.modelica_export.util.eurostag.EurostagFixedData;
import eu.itesla_project.modelica_export.util.eurostag.EurostagModDefaultTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Create a Modelica Fixed Transformer Record from IIDM Transformer
 * @author Silvia Machado <machados@aia.es>
 */
public class FixedTransformerRecord extends BranchRecord {

    public FixedTransformerRecord(TwoWindingsTransformer transformer,
            float SNREF) {
        super(transformer);
        this.transformer = transformer;
        super.setDEFAULT_BRANCH_TYPE(DEFAULT_FIXED_TRAFO_TYPE);

        super.setDEFAULT_BRANCH_PREFIX(StaticData.PREF_TRAFO);

        this.setParameters(SNREF);
    }

    @Override
    public void createRecord(ModExportContext modContext, DDBManager ddbManager, SimulatorInst simulator) {
        modContext.dictionary.add(this.transformer, this.transformer.getId());

        Equipments.ConnectionInfo info1 = Equipments.getConnectionInfoInBusBreakerView(this.transformer.getTerminal1());
        Bus b1 = info1.getConnectionBus();
        Equipments.ConnectionInfo info2 = Equipments.getConnectionInfoInBusBreakerView(this.transformer.getTerminal2());
        Bus b2 = info2.getConnectionBus();

        if ((!Float.isNaN(b1.getV()) && info1.isConnected()) || (!Float.isNaN(b2.getV()) && info2.isConnected())) {
            if (super.isCorrect()) {
                if (super.getModelicaType() != null) {
                    this.addValue(super.getModelicaType() + StaticData.WHITE_SPACE);
                } else {
                    this.addValue(DEFAULT_FIXED_TRAFO_TYPE + StaticData.WHITE_SPACE);
                }
                this.addValue(super.getModelicaName());
                this.addValue(" (");
                this.addValue(StaticData.NEW_LINE);

                if (!super.iidmbranchParameters.isEmpty()) {
                    for (int i = 0; i < super.iidmbranchParameters.size() - 1; i++) {
                        this.addValue("\t "
                                + super.iidmbranchParameters.get(i).getName()
                                + " = "
                                + super.iidmbranchParameters.get(i).getValue()
                                + ",");
                        this.addValue(StaticData.NEW_LINE);
                    }
                    this.addValue("\t "
                            + super.iidmbranchParameters.get(
                                    super.iidmbranchParameters.size() - 1)
                                    .getName()
                            + " = "
                            + super.iidmbranchParameters.get(
                                    super.iidmbranchParameters.size() - 1)
                                    .getValue());
                    this.addValue(StaticData.NEW_LINE);
                } else if (!super.branchParameters.isEmpty()) {
                    for (int i = 0; i < super.branchParameters.size() - 1; i++) {
                        this.addValue("\t "
                                + super.branchParameters.get(i).getName() + " = "
                                + super.branchParameters.get(i).getValue() + ",");
                        this.addValue(StaticData.NEW_LINE);
                    }
                    this.addValue("\t "
                            + super.branchParameters.get(
                                    super.branchParameters.size() - 1).getName()
                            + " = "
                            + super.branchParameters.get(
                                    super.branchParameters.size() - 1).getValue());
                    this.addValue(StaticData.NEW_LINE);
                }

                this.addValue("\t " + EurostagFixedData.ANNOT);

                //Clear data
                iidmbranchParameters = null;
                branchParameters = null;
            } else {
                LOGGER.error(this.getModelicaName() + " not added to grid model.");
            }
        } else {
            LOGGER.warn("Fixed transformer " + this.getModelicaName() + " disconnected.");
            this.addValue(StaticData.COMMENT + " Fixed transformer " + this.getModelicaName() + " disconnected.");
        }
    }

    /**
     * Add IIDM parameters to Fixed Transformer Modelica Model in p.u
     */
    @Override
    void setParameters(float SNREF) {
        //super.iidmbranchParameters = new ArrayList<IIDMParameter>();

        float t1NomV = this.transformer.getTerminal1().getVoltageLevel().getNominalV();
        float t2NomV = this.transformer.getTerminal2().getVoltageLevel().getNominalV();
        float u1Nom = Float.isNaN(t1NomV) == false ? t1NomV : 0;
        float u2Nom = Float.isNaN(t2NomV) == false ? t2NomV : 0;
        float v1 = Float.isNaN(this.transformer.getRatedU1()) == false ? this.transformer.getRatedU1() : 0; // [kV]
        float v2 = Float.isNaN(this.transformer.getRatedU2()) == false ? this.transformer.getRatedU2() : 0; // [kV]
        float zBase = (float) Math.pow(u2Nom, 2) / SNREF;

        float r = this.transformer.getR() / zBase; // [p.u.]
        super.addParameter(this.iidmbranchParameters, StaticData.R, r); // p.u.

        float x = this.transformer.getX() / zBase; // [p.u.]
        super.addParameter(this.iidmbranchParameters, StaticData.X, x); // p.u.

        float g = this.transformer.getG() * zBase; // [p.u.]
        super.addParameter(this.iidmbranchParameters, StaticData.G, g); // p.u.

        float b = this.transformer.getB() * zBase; // [p.u.]
        super.addParameter(this.iidmbranchParameters, StaticData.B, b); // p.u.

        /*
         * El ratio esta calculado de acuerdo al valor obtenido por HELM FLow
         */
        float vEndPu = v1 / u1Nom;
        float vSourcePu = v2 / u2Nom;
        float ration = vSourcePu / vEndPu; // ...transformation ratio [p.u.]
        super.addParameter(this.iidmbranchParameters, EurostagFixedData.r, ration); // p.u.
    }

    @Override
    public FixedTransformerRecord getClassName() {
        return this;
    }

    private TwoWindingsTransformer transformer;

    private String DEFAULT_FIXED_TRAFO_TYPE = EurostagModDefaultTypes.DEFAULT_FIXED_TRAFO_TYPE;
    private String DEFAULT_FIXED_TRAFO_PREFIX;

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedTransformerRecord.class);
}
