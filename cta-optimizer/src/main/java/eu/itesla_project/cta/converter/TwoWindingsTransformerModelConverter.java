/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.commons.ITeslaException;
import eu.itesla_project.cta.model.*;
import eu.itesla_project.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
class TwoWindingsTransformerModelConverter  extends AbstractBranchModelConverter<TwoWindingsTransformer, IpsoTwoWindingsTransformer>{

    private static final Logger LOGGER = LoggerFactory.getLogger(TwoWindingsTransformerModelConverter.class);
    protected static final String CANNOT_FIND_REGULATED_BUS = "Cannot find regulated bus for TwoWindingTransformer having the id: ";
    protected static final String PHASE_TRANSFORMER_WITH_UNCONSISTENT_REGULATION_BUS = "Phase transformer %s has a regulated bus which is different of bus1 and bus2";
    protected static final String TRANSFORMER_HAS_TO_MANY_REGULATION_MODE = "Ratio transformer %s with 2 regulation modes is not supported";


    final class Losses {
        float cu;
        float fe;
        float magnetizingCurrent;
        float saturation_exponent;
    }


    final class IpsoStep {
        float rho, alpha;
        TransformerRegulationType mode;
        float un01, un02, ucc, phase;
    }

    /**
     * Constructor
     * @param context
     */
    TwoWindingsTransformerModelConverter(ConversionContext context) {
        super(context);
    }

    @Override
    protected IpsoTwoWindingsTransformer doConvert(TwoWindingsTransformer transformer) {

        Equipments.ConnectionInfo info1 = Equipments.getConnectionInfoInBusBreakerView(transformer.getTerminal1());
        Equipments.ConnectionInfo info2 = Equipments.getConnectionInfoInBusBreakerView(transformer.getTerminal2());
        Bus bus1 = info1.getConnectionBus();
        Bus bus2 = info2.getConnectionBus();
        final IpsoNode ipsoNode1 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus1).get();
        final IpsoNode ipsoNode2 = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(bus2).get();

        String id = createIdFrom(ipsoNode1.getId(), ipsoNode2.getId());

        Losses losses = this.computeLosses(transformer);
        IpsoTap tap = this.computeTapChangerInfo(transformer);
        Map<Integer, IpsoStep> steps = computeIpsoSteps(transformer, tap);

        final TransformerRegulationParameters regulationParameters = findRegulationParametersFor(transformer, bus1, bus2);

        //...writeVariables records
        List<Integer>   indexes = new ArrayList<>();
        List<Float>     voltages_side1 = new ArrayList<>();
        List<Float>     voltages_side2 = new ArrayList<>();
        List<Float>     phases = new ArrayList<>();
        List<Float>     uccs = new ArrayList<>();
        if (steps.size() > 0) {

            for (int index : steps.keySet()) {
                IpsoStep step = steps.get(index);
                indexes.add(index);
                voltages_side1.add(step.un01);
                voltages_side2.add(step.un02);
                phases.add(step.phase);
                uccs.add(step.ucc);
            }
        }

        return new IpsoTwoWindingsTransformer(
                id,
                transformer.getId(),
                ipsoNode1,
                ipsoNode2,
                info1.isConnected(),
                info2.isConnected(),
                losses.cu,
                losses.fe,
                losses.magnetizingCurrent,
                losses.saturation_exponent,
                rate(),
                tap.getNominal(),
                tap.getInitial(),
                tap.getLowstep(),
                tap.getHighstep(),
                steps.size(),
                indexes,
                voltages_side1,
                voltages_side2,
                phases,
                uccs,
                findMaxCurrentPermanentLimitFor(transformer),
                findCurrentFlow(transformer, regulationParameters.getRegulationType()),
                regulationParameters,
                getContext().getWorld());
    }

    private float findCurrentFlow(TwoWindingsTransformer transformer, TransformerRegulationType transformerRegulationType) {
        checkArgument(transformer.getTerminal1() != null, "transformer.getTerminal1() must not be null");
        checkArgument(transformer.getTerminal2() != null, "transformer.getTerminal2() must not be null");
        float flow1 = DataUtil.getSafeValueOf(transformer.getTerminal1().getI(), 0);
        float flow2 = DataUtil.getSafeValueOf(transformer.getTerminal2().getI(), 0);
        return Math.max(flow1, flow2);
    }

    private Function<Terminal, Float> getSafeI() {
        return terminal -> DataUtil.getSafeValueOf(terminal.getI());
    }

    private Losses computeLosses(TwoWindingsTransformer transformer) {
        Losses losses = new Losses();

        float ratedU1 = transformer.getRatedU1();
        float ratedU2 = transformer.getRatedU2();
        float nominalU2 = transformer.getTerminal2().getVoltageLevel().getNominalV();

        //...mTrans.getR() = Get the nominal series resistance specified in Ω at the secondary voltage side.
        float Rpu2 = ( transformer.getR() * snref() ) / nominalU2 / nominalU2;  //...total line resistance  [p.u.](Base snref)
        float Gpu2 = ( transformer.getG() / snref() ) * nominalU2 * nominalU2;  //...semi shunt conductance [p.u.](Base snref)
        float Bpu2 = ( transformer.getB() / snref() ) * nominalU2 * nominalU2;  //...semi shunt susceptance [p.u.](Base snref)

        //...changing base snref -> base RATE to compute losses
        losses.cu = Rpu2 * rate() * 100f / snref();                  //...base RATE (100F -> %)
        losses.fe = 10000f * ( Gpu2 / rate()) * (snref() / 100f) ;   //...base RATE
        float modgb =  (float) Math.sqrt(Math.pow(Gpu2,2.f) + Math.pow(Bpu2, 2.f) );
        losses.magnetizingCurrent = 10000 * ( modgb / rate()) * (snref() / 100f);  //...magnetizing current [% base RATE]
        losses.saturation_exponent = 1.f;

        return losses;
    }

    private IpsoTap computeTapChangerInfo(TwoWindingsTransformer transformer) {
        if (transformer.getRatioTapChanger() != null) {
            return new IpsoTap(transformer.getRatioTapChanger());
        } else if (transformer.getPhaseTapChanger() != null) {
            return new IpsoTap(transformer.getPhaseTapChanger());
        } else {
            return new IpsoTap();
        }
    }

    private TransformerRegulationParameters findRegulationParametersFor(TwoWindingsTransformer transformer, Bus bus1, Bus bus2) {
        checkArgument(transformer != null, "transformer  must not be null");
        checkArgument(bus1 != null, "bus1  must not be null");
        checkArgument(bus2 != null, "bus2  must not be null");
        //...get tap changers
        RatioTapChanger rtc = transformer.getRatioTapChanger();
        PhaseTapChanger ptc = transformer.getPhaseTapChanger();

        TransformerRegulationType regulationType = TransformerRegulationType.NO;
        float setpoint = 0.f;
        Bus regulatedbus = null; // get bus1 as regulated bus by default
        IpsoNode ipsoRegulatedBus = null;
        int currentStepPosition = 0;

        // Ratio tap changer (voltage regulating)
        if (isRegulationDefinedFor(rtc)) {
            regulatedbus = findRegulatedBusFor(rtc).get();

            regulationType = findVolategRegulationType(bus1, bus2, regulatedbus)
                    .orElseThrow(() -> new IllegalStateException(unconsistentRegulationBusFor(transformer)));

            setpoint = rtc.getTargetV() / regulatedbus.getVoltageLevel().getNominalV(); // !!! in PU
            currentStepPosition = rtc.getTapPosition();
        }

        // Phase tap changer (active power regulating)
        if (isRegulationDefinedFor(ptc)) {
            // two regulation types cannot be defined on the same twoWindingTransformer
            if (regulationType.isVoltageRegulationType()) {
                throw new ITeslaException(String.format(TRANSFORMER_HAS_TO_MANY_REGULATION_MODE, transformer.getId()));
            }
            setpoint = ptc.getThresholdI(); // in Ampere
            regulatedbus = findRegulatedBusFor(ptc).get();
            currentStepPosition = ptc.getTapPosition();
            regulationType = findFlowRegulationType(bus1, bus2, regulatedbus)
                    .orElseThrow(() -> new IllegalStateException(unconsistentRegulationBusFor(transformer)));
        }

        if (regulationType != TransformerRegulationType.NO ) {
            ipsoRegulatedBus = getContext().getMappingBetweenIidmIdAndIpsoEquipment().getIpsoNodeFor(regulatedbus).get();
        }
        return new TransformerRegulationParameters(regulationType, setpoint, regulationType.getSideValue(), currentStepPosition, ipsoRegulatedBus);
    }

    private String unconsistentRegulationBusFor(TwoWindingsTransformer transformer) {
        return String.format(PHASE_TRANSFORMER_WITH_UNCONSISTENT_REGULATION_BUS, transformer.getId());
    }

    private Optional<Bus> findRegulatedBusFor(TapChanger tapChanger) {
        checkArgument(tapChanger != null, "tap changer must not be null.");
        return ofNullable(tapChanger.getTerminal())
                .map(Terminal::getBusBreakerView)
                .map(Terminal.BusBreakerView::getBus);
    }

    private Optional<TransformerRegulationType> findVolategRegulationType(Bus bus1, Bus bus2, Bus regulatedbus) {
        checkArgument(bus1 != null, "bus1 must not be null");
        checkArgument(bus2 != null, "bus1 must not be null");
        checkArgument(regulatedbus != null, "regulatedbus must not be null");
        if (bus1.equals(regulatedbus)) {
            return Optional.of(TransformerRegulationType.VOLTAGE_SIDE_1);
        } else if (bus2.equals(regulatedbus)) {
            return Optional.of(TransformerRegulationType.VOLTAGE_SIDE_2);
        }
        else {
            return Optional.empty();
        }
    }

    private Optional<TransformerRegulationType> findFlowRegulationType(Bus bus1, Bus bus2, Bus regulatedbus) {
        checkArgument(bus1 != null, "bus1 must not be null");
        checkArgument(bus2 != null, "bus1 must not be null");
        checkArgument(regulatedbus != null, "regulatedbus must not be null");
        if (bus1.equals(regulatedbus)) {
            return Optional.of(TransformerRegulationType.ACTIVE_FLUX_1);
        } else if (bus2.equals(regulatedbus)) {
            return Optional.of(TransformerRegulationType.ACTIVE_FLUX_2);
        }
        else {
            return Optional.empty();
        }
    }

    private boolean isRegulationDefinedFor(TapChanger tapChanger) {
        return tapChanger != null && tapChanger.isRegulating() && findRegulatedBusFor(tapChanger).isPresent();
    }

    private Map<Integer, IpsoStep> computeIpsoSteps(TwoWindingsTransformer transformer, IpsoTap tap) {

        float ratedU1 = transformer.getRatedU1();
        float ratedU2 = transformer.getRatedU2();
        float nomiU2 = transformer.getTerminal2().getVoltageLevel().getNominalV();

        //...getValue ratio tap changer
        RatioTapChanger rtc = transformer.getRatioTapChanger();
        PhaseTapChanger ptc = transformer.getPhaseTapChanger();

        Map<Integer, IpsoStep> steps = new TreeMap<>();
        for ( int s = tap.getLowstep() ; s <= tap.getHighstep() ; s++ ) {

            IpsoStep step = new IpsoStep();
            step.un01 = ratedU1;
            step.un02 = ratedU2;

            float dr = 0f, dx = 0f, ucc, phase = 0f;
            if (rtc != null ) {
                step.rho = rtc.getStep(s).getRho();
                step.un01 /= rtc.getStep(s).getRho() ;
                step.un02 = ratedU2;
                dr += rtc.getStep(s).getR();
                dx += rtc.getStep(s).getX();
            }

            if (ptc != null ) {
                step.alpha = ptc.getStep(s).getAlpha();
                phase = ptc.getStep(s).getAlpha();
                step.un01 /= ptc.getStep(s).getRho(); //temporary, to be investigated for ptc
                dr += ptc.getStep(s).getR();
                dx += ptc.getStep(s).getX();
            }

            //...transformer.getR() = Get the nominal series resistance specified in Ω at the secondary voltage side.
            float rpu2 = ( transformer.getR() * (1 + dr/100.0f) * snref() ) / nomiU2 / nomiU2;  //...total line resistance  [p.u.](Base snref)
            float xpu2 = ( transformer.getX() * (1 + dx/100.0f) * snref() ) / nomiU2 / nomiU2;  //...total line reactance   [p.u.](Base snref)

            //...leakage impedance [%] (base RATE)
            if ( xpu2 < 0 ) {
                ucc = xpu2 * rate() * 100f / snref();
            }
            else
            {
                float modrx =  (float) Math.sqrt(Math.pow(rpu2,2.f) + Math.pow(xpu2, 2.f) );
                ucc = modrx * rate() * 100f / snref();
            }

            step.ucc = ucc;
            step.phase = phase;
            steps.put(s, step);
        }

        return steps;

    }

    @Override
    public Iterable<TwoWindingsTransformer> gatherDataToConvertFrom(Network network) {
        return network.getTwoWindingsTransformers();
    }

    @Override
    protected ComponentType getComponentType() {
        return ComponentType.TWO_WINDINGS_TRANSFORMER;
    }

}
