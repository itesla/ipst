/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.histo;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class HistoDbUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoDbUtil.class);

    private static final Range<Double> VOLTAGE_RANGE_DEFAULT_GEN_PU = Range.closed(0.95, 1.05);
    private static final Range<Double> VOLTAGE_RANGE_NUCL_THE_PU = Range.closed(0.95, 1.05);
    private static final Range<Double> VOLTAGE_RANGE_HYD_PU = Range.closed(0.9, 1.1);
    private static final Range<Double> VOLTAGE_RANGE_NO_GEN = Range.closed(0.8, 1.2);

    private HistoDbUtil() {
    }

    public static HistoDbNetworkAttributeId createVoltageAttributeId(VoltageLevel vl) {
        return new HistoDbNetworkAttributeId(vl.getId(), HistoDbAttr.V);
    }

    private static Range<Double> span(Range<Double> r1, Range<Double> r2) {
        return Range.closed(r2.lowerEndpoint() < r1.lowerEndpoint() || Double.isNaN(r1.lowerEndpoint()) ? r2.lowerEndpoint() : r1.lowerEndpoint(),
                            r2.upperEndpoint() > r1.upperEndpoint() || Double.isNaN(r1.upperEndpoint()) ? r2.upperEndpoint() : r1.upperEndpoint());

    }

    public static void fixVoltageLimits(Network network, HistoDbClient histoDbClient, Interval interval) throws IOException, InterruptedException {
        // resize voltage limits with historical data
        Set<HistoDbAttributeId> attributeIds = new LinkedHashSet<>();
        for (VoltageLevel vl : network.getVoltageLevels()) {
            attributeIds.add(createVoltageAttributeId(vl));
        }
        HistoDbStats stats = histoDbClient.queryStats(attributeIds, interval, HistoDbHorizon.SN, true);
        for (VoltageLevel vl : network.getVoltageLevels()) {
            HistoDbNetworkAttributeId attributeId = createVoltageAttributeId(vl);

            Range<Double> histoVoltageRangePu = Range.closed(stats.getValue(HistoDbStatsType.P0_1, attributeId, Float.NaN) / vl.getNominalV(),
                                                            stats.getValue(HistoDbStatsType.P99_9, attributeId, Float.NaN) / vl.getNominalV());

            Set<EnergySource> energySources = EnumSet.noneOf(EnergySource.class);
            for (Generator g : vl.getGenerators()) {
                energySources.add(g.getEnergySource());
            }

            Range<Double> networkVoltageRangePu = Double.isNaN(vl.getLowVoltageLimit()) || Double.isNaN(vl.getHighVoltageLimit())
                    ? Range.closed(Double.NaN, Double.NaN)
                    : Range.closed(vl.getLowVoltageLimit() / vl.getNominalV(), vl.getHighVoltageLimit() / vl.getNominalV());

            LOGGER.trace("Fix voltage range of {}: histo={}, network={}, energySources={}",
                    vl.getId(), histoVoltageRangePu, networkVoltageRangePu, energySources);

            Range<Double> rangeToEnclosePu;
            if (energySources.isEmpty()) {
                rangeToEnclosePu = Range.closed(Double.isNaN(networkVoltageRangePu.lowerEndpoint()) ? VOLTAGE_RANGE_NO_GEN.lowerEndpoint() : networkVoltageRangePu.lowerEndpoint(),
                                                Double.isNaN(networkVoltageRangePu.upperEndpoint()) ? VOLTAGE_RANGE_NO_GEN.upperEndpoint() : networkVoltageRangePu.upperEndpoint());
            } else {
                if (energySources.contains(EnergySource.NUCLEAR)
                        || energySources.contains(EnergySource.THERMAL)) {
                    rangeToEnclosePu = VOLTAGE_RANGE_NUCL_THE_PU;
                } else if (energySources.contains(EnergySource.HYDRO)) {
                    rangeToEnclosePu = VOLTAGE_RANGE_HYD_PU;
                } else if (energySources.contains(EnergySource.SOLAR)
                        || energySources.contains(EnergySource.WIND)
                        || energySources.contains(EnergySource.OTHER)) {
                    rangeToEnclosePu = VOLTAGE_RANGE_DEFAULT_GEN_PU;
                } else {
                    throw new AssertionError();
                }
            }
            Range<Double> rangePu = span(histoVoltageRangePu, rangeToEnclosePu);
            Range<Double> range = Range.closed(rangePu.lowerEndpoint() * vl.getNominalV(), rangePu.upperEndpoint() * vl.getNominalV());

            LOGGER.debug("Voltage range of {}: {} Kv ({} pu)", vl.getId(), range, rangePu);

            // check that we have 0.1pu at least for each of the substation
//            if (rangePu.upperEndpoint() - rangePu.lowerEndpoint() < 0.1) {
//                throw new RuntimeException("Too tight voltage range " + rangePu  + " for voltage level " + vl.getId() + " " + rangeToEnclosePu);
//            }

            vl.setLowVoltageLimit(range.lowerEndpoint())
                    .setHighVoltageLimit(range.upperEndpoint());
        }
    }

    private static boolean hasInconsistenceActiveLimits(Generator g) {
        return g.getMaxP() > 2000;
    }

    public static void fixGeneratorActiveLimits(Network network, HistoDbClient histoDbClient, Interval interval) throws IOException, InterruptedException {
        // replace strange pmin, pmax by historical limits
        Set<HistoDbAttributeId> attributeIds = new LinkedHashSet<>();
        for (Generator g : network.getGenerators()) {
            if (hasInconsistenceActiveLimits(g)) {
                attributeIds.add(new HistoDbNetworkAttributeId(g.getId(), HistoDbAttr.P));
            }
        }
        if (attributeIds.size() > 0) {
            HistoDbStats stats = histoDbClient.queryStats(attributeIds, interval, HistoDbHorizon.SN, true);
            for (Generator g : network.getGenerators()) {
                if (hasInconsistenceActiveLimits(g)) {
                    HistoDbNetworkAttributeId attributeId = new HistoDbNetworkAttributeId(g.getId(), HistoDbAttr.P);
                    double newMinP = -stats.getValue(HistoDbStatsType.MAX, attributeId, Float.NaN);
                    double newMaxP = -stats.getValue(HistoDbStatsType.MIN, attributeId, Float.NaN);
                    if (!Double.isNaN(newMinP) && !Double.isNaN(newMaxP)) {
                        LOGGER.debug("Fix active power limits of generator {}: [{}, {}] -> [{}, {}]",
                                g.getId(), g.getMinP(), g.getMaxP(), newMinP, newMaxP);
                        g.setMinP(newMinP);
                        g.setMaxP(newMaxP);
                    }
                }
            }
        }
    }
}
