/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.service.mapdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.mapdb.BTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.commons.datasource.GenericReadOnlyDataSource;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;

import eu.itesla_project.histodb.QueryParams;
import eu.itesla_project.histodb.domain.Attribute;
import eu.itesla_project.histodb.domain.ComputedAttribute;
import eu.itesla_project.histodb.domain.CurrentPowerRatioAttribute;
import eu.itesla_project.histodb.domain.DataSet;
import eu.itesla_project.histodb.domain.NegativePowerAttribute;
import eu.itesla_project.histodb.domain.NegativeReactivePowerAttribute;
import eu.itesla_project.histodb.domain.PositivePowerAttribute;
import eu.itesla_project.histodb.domain.PositiveReactivePowerAttribute;
import eu.itesla_project.histodb.domain.Record;
import eu.itesla_project.histodb.domain.Value;
import eu.itesla_project.histodb.domain.util.Statistics;
import eu.itesla_project.histodb.repository.mapdb.HistoDataSource;
import eu.itesla_project.histodb.repository.mapdb.HistoKey;
import eu.itesla_project.histodb.service.HistoDataService;
import eu.itesla_project.modules.histo.HistoDbAttributeId;
import eu.itesla_project.modules.histo.HistoDbHorizon;
import eu.itesla_project.modules.histo.HistoDbMetaAttributeType;
import eu.itesla_project.modules.histo.IIDM2DB;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@Service
public class HistoDataServiceImpl implements HistoDataService {

    static Logger log = LoggerFactory.getLogger(HistoDataServiceImpl.class);
    private static final Collection<Importer> IMPORTERS = Importers.list();

    @Override
    public void importReferenceNetwork(HistoDataSource datasource, Path file) throws IOException {
        Objects.requireNonNull(datasource);
        Objects.requireNonNull(file);

        if (!Files.isRegularFile(file)) {
            throw new RuntimeException("Not a regular file");
        }

        Path dir = file.getParent();
        String baseName = DataSourceUtil.getBaseName(file);
        for (Importer importer : IMPORTERS) {
            try {
                Network n = importer.importData(new GenericReadOnlyDataSource(dir, baseName), null);
                if (n != null) {
                    log.debug("read network: " + n.getId());
                    datasource.saveReferenceNetwork(n);
                    break;
                }
            } catch (Throwable e) {
                log.warn("Could not read format " + importer.getFormat());
            }
        }
        try {
            datasource.commit();
        } catch (Throwable t) {
            log.warn("Error during commit " + t.getMessage());
        }
    }

    @Override
    public void importData(HistoDataSource datasource, Path dir, boolean parallel) throws Exception {
        Objects.requireNonNull(datasource);
        Objects.requireNonNull(dir);

        BTreeMap<HistoKey, Map<String, Object>> netMap = datasource.getMap();

        final AtomicReference<Network> lastSnapshot = new AtomicReference<>();

        try {
            for (Importer importer : IMPORTERS) {
                Importers.importAll(dir, importer, parallel, n -> {

                    try {
                        IIDM2DB.CimValuesMap valueMaps = IIDM2DB.extractCimValues(n,
                                new IIDM2DB.Config(n.getId(), true));

                        for (Map.Entry<IIDM2DB.HorizonKey, LinkedHashMap<HistoDbAttributeId, Object>> valueMapEntry : valueMaps
                                .entrySet()) {
                            LinkedHashMap<HistoDbAttributeId, Object> valueMap = valueMapEntry.getValue();
                            TreeMap<String, Object> netAttributes = new TreeMap<String, Object>();
                            List<String> colNames = new ArrayList<String>(valueMap.size());
                            for (HistoDbAttributeId attrId : valueMap.keySet()) {
                                colNames.add(attrId.toString());
                                Object o = valueMap.get(attrId);
                                if (attrId.toString().equals(HistoDbMetaAttributeType.datetime.toString())) {
                                    netAttributes.put(attrId.toString(), ((Date) o).getTime() / 1000);
                                } else if (o instanceof Float) {
                                    netAttributes.put(attrId.toString(), new Double((Float) o));
                                } else if (attrId.toString().endsWith("_TOPO")) {
                                    netAttributes.put(attrId.toString(), escapeTopology((String) o));
                                } else {
                                    netAttributes.put(attrId.toString(), o);
                                }
                            }
                            HistoKey key = new HistoKey(valueMapEntry.getKey().horizon,
                                    n.getCaseDate().toDate().getTime(), valueMapEntry.getKey().forecastDistance);

                            netMap.put(key, netAttributes);
                            if (valueMapEntry.getKey().horizon.equals(HistoDbHorizon.SN.toString())) {
                                lastSnapshot.set(n);
                            }
                        }

                        log.info("Inserted network: " + n.getId() + ", format: " + importer.getFormat());
                    } catch (Exception e) {
                        log.warn("Error reading network attributes", e);
                    }
                });
            }
            if (lastSnapshot.get() != null) {
                datasource.saveReferenceNetwork(lastSnapshot.get());
            }
        } catch (Exception e) {
            log.warn("Failed to insert network from " + dir, e);
        } finally {
            try {
                datasource.commit();
            } catch (Throwable t) {
                log.warn("Error during commit " + t.getMessage());
            }
        }

    }

    @Override
    public DataSet getData(HistoDataSource datasource, QueryParams query) {
        Objects.requireNonNull(datasource);
        Objects.requireNonNull(query);
        log.info("getData " + query);
        DataSet result = new DataSet();
        BTreeMap<HistoKey, Map<String, Object>> netMap = datasource.getMap();

        List<Attribute> columns = query.getCols() != null ? query.getCols()
                : findAttributes(datasource.getReferenceNetwork(), query, true);

        long start = query.getStart() >= 0 ? query.getStart() : 0;
        long maxSize = query.getCount() >= 0 ? query.getCount() : Long.MAX_VALUE;

        result.addHeaders(
                query.getColumnStart() >= 0 && query.getColumnEnd() >= 0
                        ? columns.subList(query.getColumnStart(),
                                query.getColumnEnd() < columns.size() ? query.getColumnEnd() + 1 : columns.size())
                        : columns);

        ConcurrentNavigableMap<HistoKey, Map<String, Object>> filtered = netMap.subMap(
                new HistoKey(query.getHorizon() != null ? query.getHorizon() : "", query.getTimeFrom(),
                        query.getForecastTime() >= 0 ? query.getForecastTime() : Integer.MIN_VALUE),
                new HistoKey(query.getHorizon(), query.getTimeTo(),
                        query.getForecastTime() >= 0 ? query.getForecastTime() : Integer.MAX_VALUE));
        result.addAll(filtered.keySet().stream().filter(k -> matches(k, filtered.get(k), query))
                .skip(start)
                .map(k -> filterColumns(filtered.get(k), query, columns))
                .limit(maxSize)
                .collect(Collectors.toList()));

        return result;
    }

    @Override
    public DataSet getForecastDiff(HistoDataSource datasource, QueryParams query) {
        Objects.requireNonNull(datasource);
        Objects.requireNonNull(query);

        DataSet result = new DataSet();

        BTreeMap<HistoKey, Map<String, Object>> netMap = datasource.getMap();

        List<Attribute> columns = query.getCols() != null ? query.getCols()
                : findAttributes(datasource.getReferenceNetwork(), query, false);

        long start = query.getStart() >= 0 ? query.getStart() : 0;
        long maxSize = query.getCount() >= 0 ? query.getCount() : Long.MAX_VALUE;

        result.addHeaders(
                query.getColumnStart() >= 0 && query.getColumnEnd() >= 0
                        ? columns.subList(query.getColumnStart(),
                                query.getColumnEnd() < columns.size() ? query.getColumnEnd() + 1 : columns.size())
                        : columns);

        ConcurrentNavigableMap<HistoKey, Map<String, Object>> filtered = netMap.subMap(
                new HistoKey(HistoDbHorizon.DACF.toString(), query.getTimeFrom(),
                        query.getForecastTime() >= 0 ? query.getForecastTime() : Integer.MIN_VALUE),
                new HistoKey(HistoDbHorizon.DACF.toString(), query.getTimeTo(),
                        query.getForecastTime() >= 0 ? query.getForecastTime() : Integer.MAX_VALUE));

        filtered.keySet().stream().filter(k -> matches(k, filtered.get(k), query)).skip(start).limit(maxSize)
                .forEach(fk -> {
                    HistoKey sk = new HistoKey(HistoDbHorizon.SN.toString(), fk.getDateTime(), 0);
                    Map sm = netMap.get(sk);
                    if (sm != null) {
                        result.add(filterColumns(filtered.get(fk), query, columns));
                        result.add(filterColumns(sm, query, columns));
                    }
                });
        return result;
    }

    @Override
    public DataSet getStats(HistoDataSource datasource, QueryParams query) {
        DataSet ds = getData(datasource, query);
        DataSet statset = new DataSet();
        List<Attribute> headers = new ArrayList(ds.getHeaders());
        headers.add(0, new Attribute("STATNAME"));
        statset.addHeaders(headers);
        List<Value> means = new ArrayList();
        means.add(new Value("MEAN"));
        List<Value> counters = new ArrayList();
        counters.add(new Value("COUNT"));
        List<Value> mins = new ArrayList();
        mins.add(new Value("MIN"));
        List<Value> maxs = new ArrayList();
        maxs.add(new Value("MAX"));
        List<Value> vars = new ArrayList();
        vars.add(new Value("VAR"));
        List<Value> perc01 = new ArrayList();
        perc01.add(new Value("P0.1"));
        List<Value> perc1 = new ArrayList();
        perc1.add(new Value("P1"));
        List<Value> perc5 = new ArrayList();
        perc5.add(new Value("P5"));
        List<Value> perc50 = new ArrayList();
        perc50.add(new Value("P50"));
        List<Value> perc90 = new ArrayList();
        perc90.add(new Value("P90"));
        List<Value> perc95 = new ArrayList();
        perc95.add(new Value("P95"));
        List<Value> perc99 = new ArrayList();
        perc99.add(new Value("P99"));
        List<Value> perc999 = new ArrayList();
        perc999.add(new Value("P99.9"));
        for (AtomicInteger i = new AtomicInteger(0); i.get() < ds.getHeaders().size(); i.incrementAndGet()) {
            Statistics st = ds.getRecords().stream().map(l -> ds.getHeaders().get(i.get()).getName().equals("datetime") ? null :  l.getValues().get(i.get()).getObject()).collect(Statistics::new,
                    Statistics::accept, Statistics::combine);
            counters.add(new Value((double) st.count()));
            means.add(new Value(st.avg()));
            mins.add(new Value(st.min()));
            maxs.add(new Value(st.max()));
            vars.add(new Value(st.var()));
            perc01.add(new Value(st.percentile(0.1)));
            perc1.add(new Value(st.percentile(1)));
            perc5.add(new Value(st.percentile(5)));
            perc50.add(new Value(st.percentile(50)));
            perc90.add(new Value(st.percentile(90)));
            perc95.add(new Value(st.percentile(95)));
            perc99.add(new Value(st.percentile(99)));
            perc999.add(new Value(st.percentile(99.9)));
        }
        statset.add(new Record(counters));
        statset.add(new Record(means));
        statset.add(new Record(mins));
        statset.add(new Record(maxs));
        statset.add(new Record(vars));
        statset.add(new Record(perc01));
        statset.add(new Record(perc1));
        statset.add(new Record(perc5));
        statset.add(new Record(perc50));
        statset.add(new Record(perc90));
        statset.add(new Record(perc95));
        statset.add(new Record(perc99));
        statset.add(new Record(perc999));
        return statset;
    }

    private boolean matches(HistoKey k, Map<String, Object> map, QueryParams query) {
        if (query.getHorizon() != null && !query.getHorizon().equals(k.getHorizon())) {
            return false;
        }
        if (query.getDayTimeFrom() != null && query.getDayTimeTo() != null) {
            LocalDateTime check = new DateTime(k.getDateTime()).toLocalDateTime();
            LocalDateTime from = new DateTime(query.getDayTimeFrom()).toLocalDateTime();
            LocalDateTime to = new DateTime(query.getDayTimeTo()).toLocalDateTime();

            LocalDateTime start = new DateTime(k.getDateTime()).toLocalDateTime().withHourOfDay(from.getHourOfDay())
                    .withMinuteOfHour(from.getMinuteOfHour()).withSecondOfMinute(from.getSecondOfMinute());
            LocalDateTime end = new DateTime(k.getDateTime()).toLocalDateTime().withHourOfDay(to.getHourOfDay())
                    .withMinuteOfHour(to.getMinuteOfHour()).withSecondOfMinute(to.getSecondOfMinute());
            if (check.isAfter(end) || check.isBefore(start)) {
                return false;
            }
        }
        if (query.getForecastTime() >= 0) {
            if (k.getForecastDistance() != query.getForecastTime()) {
                return false;
            }
        }
        return true;
    }

    private Record filterColumns(Map<String, Object> in, QueryParams query, List<Attribute> columns) {
        List filtered = null;
        if (query.getColumnStart() >= 0 && query.getColumnEnd() >= 0) {
            columns = columns.subList(query.getColumnStart(),
                    query.getColumnEnd() < columns.size() ? query.getColumnEnd() + 1 : columns.size());
        }
        filtered = columns.stream().map(k -> k instanceof ComputedAttribute ? new Value(((ComputedAttribute) k).getValue(in)) : new Value(in.get(k.getName()))).collect(Collectors.toList());
        return new Record(filtered);
    }


    private List<Attribute> findAttributes(Network latestNetwork, QueryParams query, boolean prependDefaults) {
        List<Attribute> attributes = new ArrayList();

        List<String> eqTypes = query.getEquipments();
        List<String> powerTypes = query.getPowers();
        List<String> measureTypes = query.getAttribs();
        List<String> regions = query.getRegions();
        List<String> countries = query.getCountries();
        List<String> equipIds = query.getIds();

        if (eqTypes == null || eqTypes.contains("dangling")) {
            for (DanglingLine dl : latestNetwork.getDanglingLines()) {
                if (equipIds != null && !equipIds.contains(dl.getId())) {
                    continue;
                }
                if (regions != null && Collections.disjoint(regions,
                        dl.getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null && !countries
                        .contains(dl.getTerminal().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }
                if (measureTypes == null || measureTypes.contains("P")) {
                    attributes.add(new Attribute(dl.getId() + "_P"));
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(dl.getId() + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("I")) {
                    attributes.add(new Attribute(dl.getId() + "_I"));
                }
                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(dl.getId() + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("P0")) {
                    attributes.add(new Attribute(dl.getId() + "_P0"));
                }
                if (measureTypes == null || measureTypes.contains("Q0")) {
                    attributes.add(new Attribute(dl.getId() + "_Q0"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(dl.getId() + "_STATUS"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("gen")) {
            for (Generator g : latestNetwork.getGenerators()) {
                if (equipIds != null && !equipIds.contains(g.getId())) {
                    continue;
                }
                if (regions != null && Collections.disjoint(regions,
                        g.getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null && !countries
                        .contains(g.getTerminal().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }
                if (powerTypes != null && !powerTypes.contains(g.getEnergySource().name())) {
                    continue;
                }
                if (measureTypes == null || measureTypes.contains("P")) {
                    attributes.add(new Attribute(g.getId() + "_P"));
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(g.getId() + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(g.getId() + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("I")) {
                    attributes.add(new Attribute(g.getId() + "_I"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(g.getId() + "_STATUS"));
                }
                if (measureTypes == null || measureTypes.contains("PP")) {
                    attributes.add(new PositivePowerAttribute(g.getId() + "_PP"));
                }
                if (measureTypes == null || measureTypes.contains("PN")) {
                    attributes.add(new NegativePowerAttribute(g.getId() + "_PN"));
                }
                if (measureTypes == null || measureTypes.contains("QP")) {
                    attributes.add(new PositiveReactivePowerAttribute(g.getId() + "_QP"));
                }
                if (measureTypes == null || measureTypes.contains("QN")) {
                    attributes.add(new NegativeReactivePowerAttribute(g.getId() + "_QN"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("loads")) {
            for (Load l : latestNetwork.getLoads()) {
                if (equipIds != null && !equipIds.contains(l.getId())) {
                    continue;
                }
                if (regions != null && Collections.disjoint(regions,
                        l.getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null && !countries
                        .contains(l.getTerminal().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }
                if (measureTypes == null || measureTypes.contains("P")) {
                    attributes.add(new Attribute(l.getId() + "_P"));
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(l.getId() + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(l.getId() + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("I")) {
                    attributes.add(new Attribute(l.getId() + "_I"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(l.getId() + "_STATUS"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("shunts")) {
            for (ShuntCompensator sc : latestNetwork.getShunts()) {
                if (equipIds != null && !equipIds.contains(sc.getId())) {
                    continue;
                }
                if (regions != null && Collections.disjoint(regions,
                        sc.getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null && !countries
                        .contains(sc.getTerminal().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(sc.getId() + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(sc.getId() + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(sc.getId() + "_STATUS"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("2wt")) {
            for (TwoWindingsTransformer wt2 : latestNetwork.getTwoWindingsTransformers()) {
                if (equipIds != null && !equipIds.contains(wt2.getId())) {
                    continue;
                }
                if (regions != null
                        && Collections.disjoint(regions,
                                wt2.getTerminal1().getVoltageLevel().getSubstation().getGeographicalTags())
                        && Collections.disjoint(regions,
                                wt2.getTerminal2().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null
                        && !countries
                                .contains(wt2.getTerminal1().getVoltageLevel().getSubstation().getCountry().toString())
                        && !countries
                                .contains(wt2.getTerminal2().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }

                String leg1Id = wt2.getId() + "__TO__" + wt2.getTerminal1().getVoltageLevel().getId();
                String leg2Id = wt2.getId() + "__TO__" + wt2.getTerminal2().getVoltageLevel().getId();

                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(leg1Id + "_V"));
                    attributes.add(new Attribute(leg2Id + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("I")) {
                    attributes.add(new Attribute(leg1Id + "_I"));
                    attributes.add(new Attribute(leg2Id + "_I"));
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(leg1Id + "_Q"));
                    attributes.add(new Attribute(leg2Id + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("P")) {
                    attributes.add(new Attribute(leg1Id + "_P"));
                    attributes.add(new Attribute(leg2Id + "_P"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(leg1Id + "_STATUS"));
                    attributes.add(new Attribute(leg2Id + "_STATUS"));
                }
                if (measureTypes == null || measureTypes.contains("RTC")) {
                    attributes.add(new Attribute(wt2.getId() + "_RTC"));
                }
                if (measureTypes == null || measureTypes.contains("PTC")) {
                    attributes.add(new Attribute(wt2.getId() + "_PTC"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("3wt")) {
            for (ThreeWindingsTransformer wt3 : latestNetwork.getThreeWindingsTransformers()) {
                if (equipIds != null && !equipIds.contains(wt3.getId())) {
                    continue;
                }
                if (regions != null
                        && Collections.disjoint(regions,
                                wt3.getLeg1().getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())
                        && Collections.disjoint(regions,
                                wt3.getLeg2().getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())
                        && Collections.disjoint(regions,
                                wt3.getLeg3().getTerminal().getVoltageLevel().getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null
                        && !countries.contains(
                                wt3.getLeg1().getTerminal().getVoltageLevel().getSubstation().getCountry().toString())
                        && !countries.contains(
                                wt3.getLeg2().getTerminal().getVoltageLevel().getSubstation().getCountry().toString())
                        && !countries.contains(
                                wt3.getLeg3().getTerminal().getVoltageLevel().getSubstation().getCountry().toString())) {
                    continue;
                }

                String leg1Id = wt3.getId() + "__TO__" + wt3.getLeg1().getTerminal().getVoltageLevel().getId();
                String leg2Id = wt3.getId() + "__TO__" + wt3.getLeg2().getTerminal().getVoltageLevel().getId();
                String leg3Id = wt3.getId() + "__TO__" + wt3.getLeg3().getTerminal().getVoltageLevel().getId();

                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(leg1Id + "_V"));
                    attributes.add(new Attribute(leg2Id + "_V"));
                    attributes.add(new Attribute(leg3Id + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("I")) {
                    attributes.add(new Attribute(leg1Id + "_I"));
                    attributes.add(new Attribute(leg2Id + "_I"));
                    attributes.add(new Attribute(leg3Id + "_I"));
                }
                if (measureTypes == null || measureTypes.contains("P")) {
                    attributes.add(new Attribute(leg1Id + "_P"));
                    attributes.add(new Attribute(leg2Id + "_P"));
                    attributes.add(new Attribute(leg3Id + "_P"));
                }
                if (measureTypes == null || measureTypes.contains("Q")) {
                    attributes.add(new Attribute(leg1Id + "_Q"));
                    attributes.add(new Attribute(leg2Id + "_Q"));
                    attributes.add(new Attribute(leg3Id + "_Q"));
                }
                if (measureTypes == null || measureTypes.contains("STATUS")) {
                    attributes.add(new Attribute(leg1Id + "_STATUS"));
                    attributes.add(new Attribute(leg2Id + "_STATUS"));
                    attributes.add(new Attribute(leg3Id + "_STATUS"));
                }
                if (measureTypes == null || measureTypes.contains("RTC")) {
                    attributes.add(new Attribute(leg2Id + "_RTC"));
                    attributes.add(new Attribute(leg3Id + "_RTC"));
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("lines")) {
            for (Branch l : latestNetwork.getBranches()) {
                if (equipIds != null && !equipIds.contains(l.getId())) {
                    continue;
                }
                if (l.getTerminal1().getBusView().getBus() != null) {
                    if ((regions == null || !Collections.disjoint(regions,
                            l.getTerminal1().getVoltageLevel().getSubstation().getGeographicalTags()))
                            && (countries == null || countries.contains(
                                    l.getTerminal1().getVoltageLevel().getSubstation().getCountry().toString()))) {
                        String terminalId = l.getId() + "__TO__" + l.getTerminal1().getVoltageLevel().getId();
                        if (measureTypes == null || measureTypes.contains("P")) {
                            attributes.add(new Attribute(terminalId + "_P"));
                        }
                        if (measureTypes == null || measureTypes.contains("Q")) {
                            attributes.add(new Attribute(terminalId + "_Q"));
                        }
                        if (measureTypes == null || measureTypes.contains("V")) {
                            attributes.add(new Attribute(terminalId + "_V"));
                        }
                        if (measureTypes == null || measureTypes.contains("I")) {
                            attributes.add(new Attribute(terminalId + "_I"));
                        }
                        if (measureTypes == null || measureTypes.contains("STATUS")) {
                            attributes.add(new Attribute(terminalId + "_STATUS"));
                        }
                        if (measureTypes == null || measureTypes.contains("IP")) {
                            attributes.add(new CurrentPowerRatioAttribute(terminalId + "_IP"));
                        }
                    }
                }

                if (l.getTerminal2().getBusView().getBus() != null) {
                    if ((regions == null || !Collections.disjoint(regions,
                            l.getTerminal2().getVoltageLevel().getSubstation().getGeographicalTags()))
                            && (countries == null || countries.contains(
                                    l.getTerminal2().getVoltageLevel().getSubstation().getCountry().toString()))) {
                        String terminalId = l.getId() + "__TO__" + l.getTerminal2().getVoltageLevel().getId();
                        if (measureTypes == null || measureTypes.contains("P")) {
                            attributes.add(new Attribute(terminalId + "_P"));
                        }
                        if (measureTypes == null || measureTypes.contains("Q")) {
                            attributes.add(new Attribute(terminalId + "_Q"));
                        }
                        if (measureTypes == null || measureTypes.contains("V")) {
                            attributes.add(new Attribute(terminalId + "_V"));
                        }
                        if (measureTypes == null || measureTypes.contains("I")) {
                            attributes.add(new Attribute(terminalId + "_I"));
                        }
                        if (measureTypes == null || measureTypes.contains("STATUS")) {
                            attributes.add(new Attribute(terminalId + "_STATUS"));
                        }
                        if (measureTypes == null || measureTypes.contains("IP")) {
                            attributes.add(new CurrentPowerRatioAttribute(terminalId + "_IP"));
                        }
                    }
                }
            }
        }

        if (eqTypes == null || eqTypes.contains("stations")) {
            for (VoltageLevel vl : latestNetwork.getVoltageLevels()) {
                if (equipIds != null && !equipIds.contains(vl.getId())) {
                    continue;
                }
                if (regions != null && Collections.disjoint(regions, vl.getSubstation().getGeographicalTags())) {
                    continue;
                }
                if (countries != null && !countries.contains(vl.getSubstation().getCountry().toString())) {
                    continue;
                }
                if (measureTypes == null || measureTypes.contains("TOPO")) {
                    attributes.add(new Attribute(vl.getId() + "_TOPO"));
                }
                if (measureTypes == null || measureTypes.contains("T")) {
                    attributes.add(new Attribute(vl.getId() + "_TOPOHASH"));
                }
                if (measureTypes == null || measureTypes.contains("V")) {
                    attributes.add(new Attribute(vl.getId() + "_V"));
                }
                if (measureTypes == null || measureTypes.contains("PGEN")) {
                    attributes.add(new Attribute(vl.getId() + "_PGEN"));
                }
                if (measureTypes == null || measureTypes.contains("QGEN")) {
                    attributes.add(new Attribute(vl.getId() + "_QGEN"));
                }
                if (measureTypes == null || measureTypes.contains("PLOAD")) {
                    attributes.add(new Attribute(vl.getId() + "_PLOAD"));
                }
                if (measureTypes == null || measureTypes.contains("QLOAD")) {
                    attributes.add(new Attribute(vl.getId() + "_QLOAD"));
                }
                if (measureTypes == null || measureTypes.contains("QSHUNT")) {
                    attributes.add(new Attribute(vl.getId() + "_QSHUNT"));
                }
            }
        }

        if (!prependDefaults) {
            attributes.add(new Attribute("horizon"));
            attributes.add(new Attribute("forecastTime"));
            attributes.add(new Attribute("datetime"));
        }

        attributes.sort((a1, a2) -> a1.getName().compareTo(a2.getName()));

        if (prependDefaults) {
            attributes.add(0, new Attribute("horizon"));
            attributes.add(0, new Attribute("forecastTime"));
            attributes.add(0, new Attribute("datetime"));
        }
        return attributes;
    }

    private String escapeTopology(String in) {
        return "\"" + in.replaceAll("\"", "\"\"") + "\"";
    }
}
