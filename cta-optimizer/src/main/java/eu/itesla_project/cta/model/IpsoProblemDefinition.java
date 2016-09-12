/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import eu.itesla_project.cta.service.CorrectiveActions;
import eu.itesla_project.cta.service.IpsoInvalidComponent;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoProblemDefinition  implements IpsoOptimizationDataInput {

    private final List<IpsoInvalidComponent> invalidComponents;
    private List<IpsoControlVariableProductionP> variableGeneratorPs;
    private List<IpsoControlVariableProductionQ> variableGeneratorQs;
    private List<IpsoControlVariableGeneratorStatism> variableGeneratorStatisms;
    private List<IpsoControlVariableBankStep> variableBankSteps;
    private List<IpsoControlVariable2WTransformerTap> variable2WTransformerTaps;

    private List<AbstractIpsoConstraintLineFlow> constraintLineFlow;
    private List<IpsoConstraint2WTransformerFlow> constraint2WTransformerFlows;
    private List<IpsoConstraintNodeAcVoltageBounds> constraintNodeAcVoltageBounds;
    private List<IpsoConstraintNodeAcAngleBounds> constraintNodeAcAngleBounds;
    private List<IpsoConstraintGeneratorPBounds> constraintGeneratorPBounds;
    private List<IpsoConstraintGeneratorQBounds> constraintGeneratorQBounds;
    private List<IpsoConstraint2WTransformerTapBounds> constraint2WTransformerTaps;
    private List<IpsoConstraintBankStepBounds> constraintBankStepBounds;

    private int world;
    private final String caseName;
    private CorrectiveActions correctiveActions;

    /**
     * Constructor
     */
    public IpsoProblemDefinition(String caseName, int world,
                                 List<IpsoControlVariableProductionP> variableGeneratorPs,
                                 List<IpsoControlVariableProductionQ> variableGeneratorQs,
                                 List<IpsoControlVariableGeneratorStatism> variableGeneratorStatisms,
                                 List<IpsoControlVariableBankStep> variableBankSteps,
                                 List<IpsoControlVariable2WTransformerTap> variable2WTransformerTaps,
                                 List<AbstractIpsoConstraintLineFlow> constraintLineFlow,
                                 List<IpsoConstraint2WTransformerFlow> constraint2WTransformerFlows,
                                 List<IpsoConstraintNodeAcVoltageBounds> constraintNodeAcVoltageBoundses,
                                 List<IpsoConstraintNodeAcAngleBounds> constraintNodeAcAngleBounds,
                                 List<IpsoConstraintGeneratorPBounds> constraintGeneratorPBounds,
                                 List<IpsoConstraintGeneratorQBounds> constraintGeneratorQBounds,
                                 List<IpsoConstraint2WTransformerTapBounds> constraint2WTransformerTaps,
                                 List<IpsoConstraintBankStepBounds> constraintBankStepBoundses,
                                 List<IpsoInvalidComponent> invalidComponents) {
        checkArgument(variableGeneratorPs != null, "variableGeneratorPs must not be null");
        checkArgument(variableGeneratorQs != null, "variableGeneratorQs must not be null");
        checkArgument(variableGeneratorStatisms != null, "variableGeneratorStatisms must not be null");
        checkArgument(variableBankSteps != null, "variableBankSteps must not be null");
        checkArgument(variable2WTransformerTaps != null, "variable2WTransformerTaps must not be null");
        checkArgument(constraintLineFlow != null, "variable2WTransformerTaps must not be null");
        checkArgument(constraint2WTransformerFlows != null, "constraint2WTransformerFlows must not be null");
        checkArgument(constraintNodeAcVoltageBoundses != null, "constraintNodeAcVoltageBounds must not be null");
        checkArgument(constraintNodeAcAngleBounds != null, "constraintNodeAcAngleBounds must not be null");
        checkArgument(constraintGeneratorPBounds != null, "constraintGeneratorPBounds must not be null");
        checkArgument(constraintGeneratorQBounds != null, "constraintGeneratorPBounds must not be null");
        checkArgument(constraint2WTransformerTaps != null, "constraint2WTransformerTaps must not be null");
        checkArgument(constraintBankStepBoundses != null, "constraintBankStepBounds must not be null");
        checkArgument(invalidComponents != null, "invalidComponents must not be null");
        this.caseName = caseName;
        this.world = world;
        this.variableGeneratorPs = variableGeneratorPs;
        this.variableGeneratorQs = variableGeneratorQs;
        this.variableGeneratorStatisms = variableGeneratorStatisms;
        this.variableBankSteps = variableBankSteps;
        this.variable2WTransformerTaps = variable2WTransformerTaps;
        this.constraintLineFlow = constraintLineFlow;
        this.constraint2WTransformerFlows = constraint2WTransformerFlows;
        this.constraintNodeAcVoltageBounds = constraintNodeAcVoltageBoundses;
        this.constraintNodeAcAngleBounds = constraintNodeAcAngleBounds;
        this.constraintGeneratorPBounds = constraintGeneratorPBounds;
        this.constraintGeneratorQBounds = constraintGeneratorQBounds;
        this.constraint2WTransformerTaps = constraint2WTransformerTaps;
        this.constraintBankStepBounds = constraintBankStepBoundses;
        this.invalidComponents = invalidComponents;

    }

    public List<IpsoControlVariableProductionP> getVariableGeneratorPs() {
        return variableGeneratorPs;
    }

    public List<IpsoControlVariableProductionQ> getVariableGeneratorQs() {
        return variableGeneratorQs;
    }

    public List<IpsoControlVariableBankStep> getVariableBankSteps() {
        return variableBankSteps;
    }

    public List<IpsoControlVariable2WTransformerTap> getVariable2WTransformerTaps() {
        return variable2WTransformerTaps;
    }

    public List<IpsoConstraintGeneratorPBounds> getConstraintGeneratorPBounds() {
        return constraintGeneratorPBounds;
    }

    public List<IpsoConstraintGeneratorQBounds> getConstraintGeneratorQBounds() {
        return constraintGeneratorQBounds;
    }

    public List<AbstractIpsoConstraintLineFlow> getConstraintLineFlows() {
        return constraintLineFlow;
    }

    public List<IpsoConstraint2WTransformerFlow> getConstraint2WTransformerFlows() {return constraint2WTransformerFlows;}

    public List<IpsoConstraintNodeAcVoltageBounds> getConstraintNodeAcVoltageBounds() {return constraintNodeAcVoltageBounds;}

    public List<IpsoControlVariableGeneratorStatism> getVariableGeneratorStatisms() {return variableGeneratorStatisms;}

    public List<IpsoConstraintNodeAcAngleBounds> getConstraintNodeAcAngleBounds() {return constraintNodeAcAngleBounds;}

    public List<IpsoConstraint2WTransformerTapBounds> getConstraint2WTransformerTaps() {return constraint2WTransformerTaps;}

    public List<IpsoConstraintBankStepBounds> getConstraintBankStepBounds() {
        return constraintBankStepBounds;
    }

    public List<IpsoInvalidComponent> getInvalidComponents() {
        return invalidComponents;
    }

    @Override
    public int getWorld() {
        return world;
    }

    @Override
    public String getCaseName() {
        return caseName;
    }

    public void mergeControlVariables(List<IpsoControlVariable> controlVariables) {

        final List<IpsoControlVariableProductionP> newVariableProductionP = controlVariables.stream()
                .filter(controlVariable -> controlVariable instanceof IpsoControlVariableProductionP)
                .map(IpsoControlVariableProductionP.class::cast)
                .collect(toList());

        final List<IpsoControlVariableProductionQ> newVariableProductionQ = controlVariables.stream()
                .filter(controlVariable -> controlVariable instanceof IpsoControlVariableProductionQ)
                .map(IpsoControlVariableProductionQ.class::cast)
                .collect(toList());

        final List<IpsoControlVariableGeneratorStatism> newVariableGeneratorStatism = controlVariables.stream()
                .filter(controlVariable -> controlVariable instanceof IpsoControlVariableGeneratorStatism)
                .map(IpsoControlVariableGeneratorStatism.class::cast)
                .collect(toList());

        final List<IpsoControlVariableBankStep> newVariableBankStep = controlVariables.stream()
                .filter(controlVariable -> controlVariable instanceof IpsoControlVariableBankStep)
                .map(IpsoControlVariableBankStep.class::cast)
                .collect(toList());

        final List<IpsoControlVariable2WTransformerTap> newVariable2WTransformerTap = controlVariables.stream()
                .filter(controlVariable -> controlVariable instanceof IpsoControlVariable2WTransformerTap)
                .map(IpsoControlVariable2WTransformerTap.class::cast)
                .collect(toList());

        mergeControlVariables(variableGeneratorPs, newVariableProductionP);
        mergeControlVariables(variableGeneratorQs, newVariableProductionQ);
        mergeControlVariables(variableGeneratorStatisms, newVariableGeneratorStatism);
        mergeControlVariables(variableBankSteps, newVariableBankStep);
        mergeControlVariables(variable2WTransformerTaps, newVariable2WTransformerTap);
    }


    public void mergeConstraints(List<IpsoConstraint> constraints) {

        final List<IpsoConstraintGeneratorPBounds> newConstraintGeneratorPBounds = findSubTypeOf(IpsoConstraintGeneratorPBounds.class, constraints);

        final List<IpsoConstraintGeneratorQBounds> newConstraintGeneratorQBounds = findSubTypeOf(IpsoConstraintGeneratorQBounds.class, constraints);

        final List<IpsoConstraintBankStepBounds> newConstraintBankStep = findSubTypeOf(IpsoConstraintBankStepBounds.class, constraints);

        final List<IpsoConstraint2WTransformerTapBounds> newConstraint2WTransformerTap = findSubTypeOf(IpsoConstraint2WTransformerTapBounds.class, constraints);

        final List<AbstractIpsoConstraintLineFlow> newConstraintLineFlow = constraints.stream()
                .filter(constraint -> constraint instanceof AbstractIpsoConstraintLineFlow)
                .map(AbstractIpsoConstraintLineFlow.class::cast)
                .collect(toList());

        final List<IpsoConstraint2WTransformerFlow> newConstraintTransformerFlow = constraints.stream()
                .filter(constraint -> constraint instanceof IpsoConstraint2WTransformerFlow)
                .map(IpsoConstraint2WTransformerFlow.class::cast)
                .collect(toList());

        final List<IpsoConstraintNodeAcVoltageBounds> newConstraintVoltageBounds = constraints.stream()
                .filter(constraint -> constraint instanceof IpsoConstraintNodeAcVoltageBounds)
                .map(IpsoConstraintNodeAcVoltageBounds.class::cast)
                .collect(toList());

        final List<IpsoConstraintNodeAcAngleBounds> newConstraintAngleBounds = constraints.stream()
                .filter(constraint -> constraint instanceof IpsoConstraintNodeAcAngleBounds)
                .map(IpsoConstraintNodeAcAngleBounds.class::cast)
                .collect(toList());

        mergeConstraints(this.constraintLineFlow,               newConstraintLineFlow);
        mergeConstraints(this.constraint2WTransformerFlows,     newConstraintTransformerFlow);
        mergeConstraints(this.constraintNodeAcVoltageBounds,    newConstraintVoltageBounds);
        mergeConstraints(this.constraintNodeAcAngleBounds,      newConstraintAngleBounds);
        mergeConstraints(this.constraintGeneratorPBounds,       newConstraintGeneratorPBounds);
        mergeConstraints(this.constraintGeneratorQBounds,       newConstraintGeneratorQBounds);
        mergeConstraints(this.constraint2WTransformerTaps,      newConstraint2WTransformerTap);
        mergeConstraints(this.constraintBankStepBounds,         newConstraintBankStep);
    }

    <T extends IpsoConstraint> List<T> findSubTypeOf(Class<T> constraintClass, List<IpsoConstraint> constraints) {
        return constraints.stream()
                .filter(constraint -> constraint.getClass().equals(constraintClass))
                .map(constraintClass::cast)
                .collect(toList());
    }

    private <T extends IpsoControlVariable> void mergeControlVariables(List<T> existingVariables,
                                                                          List<T> newVariables) {
        final List<String>  equipmentIds = newVariables.stream()
                .map(variable -> variable.getRelatedIpsoEquipment().getId())
                .collect(toList());

        List<T> variables = existingVariables.stream()
                .filter(existingVariable -> thatIsNotRelatedToAnEquipmentIn(equipmentIds, existingVariable))
                .collect(toList());

        variables.addAll(newVariables);
        existingVariables.clear();
        existingVariables.addAll(variables);
    }

    private <T extends IpsoConstraint> void mergeConstraints(List<T> existingConstraints,
                                                                List<T> newConstraints) {
        // get equipment id's which are handled by the new constraints
        final List<String>  equipmentIds = newConstraints.stream()
                .map(variable -> variable.getRelatedIpsoEquipment().getId())
                .collect(toList());

        // keep existing constraints related to an equipment which is not in the list of equipment ids
        List<T> constraints = existingConstraints.stream()
                .filter(existingConstraint -> thatIsNotRelatedToAnEquipmentIn(equipmentIds, existingConstraint))
                .collect(toList());

        constraints.addAll(newConstraints);
        existingConstraints.clear();
        existingConstraints.addAll(constraints);
    }

    /**
     * @return true if the existing variable is related to an equipment which is not in the list of equipement ids
     */
    private boolean thatIsNotRelatedToAnEquipmentIn(List<String> equipmentIds, IpsoControlVariable existingVariable) {
        return !equipmentIds.contains(existingVariable.getRelatedIpsoEquipment().getId());
    }

    /**
     * @return true if the existing variable is related to an equipment which is not in the list of equipement ids
     */
    private boolean thatIsNotRelatedToAnEquipmentIn(List<String> equipmentIds, IpsoConstraint existingConstraint) {
        return !equipmentIds.contains(existingConstraint.getRelatedIpsoEquipment().getId());
    }

}
