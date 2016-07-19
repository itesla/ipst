/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.cases;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eu.itesla_project.computation.ComputationManager;
import eu.itesla_project.iidm.datasource.ReadOnlyDataSource;
import eu.itesla_project.iidm.datasource.ReadOnlyDataSourceFactory;
import eu.itesla_project.iidm.datasource.GenericReadOnlyDataSource;
import eu.itesla_project.iidm.import_.Importer;
import eu.itesla_project.iidm.import_.Importers;
import eu.itesla_project.iidm.network.Country;
import eu.itesla_project.iidm.network.Network;
import eu.itesla_project.ucte.util.UcteFileName;
import eu.itesla_project.ucte.util.UcteGeographicalCode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common ENTSOE case repository layout:
 * <pre>
 * CIM/SN/2013/01/15/20130115_0620_SN2_FR0.zip
 *    /FO/...
 * UCT/SN/...
 *    /FO/...
 * </pre>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EntsoeCaseRepository implements CaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntsoeCaseRepository.class);

    static class EntsoeFormat {

        private final Importer importer;

        private final String dirName;

        EntsoeFormat(Importer importer, String dirName) {
            this.importer = Objects.requireNonNull(importer);
            this.dirName = Objects.requireNonNull(dirName);
        }

        Importer getImporter() {
            return importer;
        }

        String getDirName() {
            return dirName;
        }
    }

    private final EntsoeCaseRepositoryConfig config;

    private final List<EntsoeFormat> formats;

    private final ReadOnlyDataSourceFactory dataSourceFactory;

    public static CaseRepository create(ComputationManager computationManager) {
        return new EntsoeCaseRepository(EntsoeCaseRepositoryConfig.load(), computationManager);
    }

    EntsoeCaseRepository(EntsoeCaseRepositoryConfig config, List<EntsoeFormat> formats, ReadOnlyDataSourceFactory dataSourceFactory) {
        this.config = Objects.requireNonNull(config);
        this.formats = Objects.requireNonNull(formats);
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory);
        LOGGER.info(config.toString());
    }

    EntsoeCaseRepository(EntsoeCaseRepositoryConfig config, ComputationManager computationManager) {
        this(config,
                Arrays.asList(new EntsoeFormat(Importers.getImporter("CIM1", computationManager), "CIM"),
                              new EntsoeFormat(Importers.getImporter("UCTE", computationManager), "UCT")), // official ENTSOE formats)
                (directory, baseName) -> new GenericReadOnlyDataSource(directory, baseName));
    }

    public EntsoeCaseRepositoryConfig getConfig() {
        return config;
    }

    private static class ImportContext {
        private final Importer importer;
        private final ReadOnlyDataSource ds;

        private ImportContext(Importer importer, ReadOnlyDataSource ds) {
            this.importer = importer;
            this.ds = ds;
        }
    }

    // because D1 snapshot does not exist and forecast replacement is not yet implemented
    private static Collection<UcteGeographicalCode> forCountryHacked(Country country) {
        return UcteGeographicalCode.forCountry(country).stream()
                .filter(ucteGeographicalCode -> ucteGeographicalCode != UcteGeographicalCode.D1)
                .collect(Collectors.toList());
    }

    public static boolean isIntraday(CaseType ct) {
        return ((ct != null) && (ct.name().startsWith("IDCF")));
    }

    public static String intraForecastDistanceInHoursSx(CaseType ct) {
        return ct.name().substring(4,6);
    }

    private <R> R scanRepository(DateTime date, CaseType type, Country country, Function<List<ImportContext>, R> handler) {
        Collection<UcteGeographicalCode> geographicalCodes = country != null ? forCountryHacked(country)
                                                                             : Arrays.asList(UcteGeographicalCode.UX, UcteGeographicalCode.UC);

        DateTime testDate1=date.minusHours(1);
        String typeDirS=type.name();
        String typeID=type.name();
        if (isIntraday(type)) {
            typeDirS = "IDCF";
            typeID = intraForecastDistanceInHoursSx(type);
        } else if (type.equals(CaseType.D2)) {
            typeDirS="2D"; // because enum names cannot be prefixed with a digit
            typeID="2D";
        }

        for (EntsoeFormat format : formats) {
            Path formatDir = config.getRootDir().resolve(format.getDirName());
            if (Files.exists(formatDir)) {
                Path typeDir = formatDir.resolve(typeDirS);
                if (Files.exists(typeDir)) {
                    Path dayDir = typeDir.resolve(String.format("%04d", date.getYear()))
                            .resolve(String.format("%02d", date.getMonthOfYear()))
                            .resolve(String.format("%02d", date.getDayOfMonth()));
                    if (Files.exists(dayDir)) {
                        List<ImportContext> importContexts = null;
                        for (UcteGeographicalCode geographicalCode : geographicalCodes) {
                            Collection<String> forbiddenFormats = config.getForbiddenFormatsByGeographicalCode().get(geographicalCode);
                            if (!forbiddenFormats.contains(format.getImporter().getFormat())) {
                                for (int i = 9; i >= 0; i--) {
                                    String baseName = String.format("%04d%02d%02d_%02d%02d_" + typeID + "%01d_" + geographicalCode.name() + "%01d",
                                            date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), date.getHourOfDay(), date.getMinuteOfHour(),
                                            date.getDayOfWeek(), i);
                                    if (testDate1.getHourOfDay() == date.getHourOfDay()) {
                                        baseName = baseName.substring(0,9)+'B'+baseName.substring(10);
                                    }
                                    ReadOnlyDataSource ds = dataSourceFactory.create(dayDir, baseName);
                                    if (importContexts == null) {
                                        importContexts = new ArrayList<>();
                                    }
                                    if (format.getImporter().exists(ds)) {
                                        importContexts.add(new ImportContext(format.getImporter(), ds));
                                    }
                                }
                                if (importContexts.size()==0 ) {  // for info purposes, only
                                    String baseName1 = String.format("%04d%02d%02d_%02d%02d_" + typeID + "%01d_" + geographicalCode.name(),
                                            date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), date.getHourOfDay(), date.getMinuteOfHour(),
                                            date.getDayOfWeek());
                                    if (testDate1.getHourOfDay() == date.getHourOfDay()) {
                                        baseName1 = baseName1.substring(0,9)+'B'+baseName1.substring(10);
                                    }
                                    LOGGER.warn("could not find any file {}[0-9] in directory {}", baseName1, dayDir);
                                }
                            }
                        }
                        if (importContexts != null) {
                            R result = handler.apply(importContexts);
                            if (result != null) {
                                return result;
                            }
                        }
                    } else {
                        LOGGER.warn("could not find any (daydir) directory {}", dayDir);
                    }
                } else {
                    LOGGER.warn("could not find any (typedir) directory {}", typeDir);
                }
            } else {
                LOGGER.warn("could not find any (formatdir) directory {}", formatDir);
            }
        }
        return null;
    }

    private static DateTime toCetDate(DateTime date) {
        DateTimeZone CET = DateTimeZone.forID("CET");
        if (!date.getZone().equals(CET)) {
            return date.toDateTime(CET);
        }
        return date;
    }

    @Override
    public List<Network> load(DateTime date, CaseType type, Country country) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(type);
        List<Network> networks2 = scanRepository(toCetDate(date), type, country, importContexts -> {
            List<Network> networks = null;
            if (importContexts.size() > 0) {
                networks = new ArrayList<>();
                for (ImportContext importContext : importContexts) {
                    LOGGER.info("Loading {} in {} format", importContext.ds.getBaseName(), importContext.importer.getFormat());
                    networks.add(importContext.importer.import_(importContext.ds, null));
                }
            }
            return networks;
        });
        return networks2 == null ? Collections.emptyList() : networks2;
    }

	@Override
	public boolean isDataAvailable(DateTime date, CaseType type, Country country) {
		return isNetworkDataAvailable(date, type, country);
	}

	private boolean isNetworkDataAvailable(DateTime date, CaseType type, Country country) {
		Objects.requireNonNull(date);
        Objects.requireNonNull(type);
        return scanRepository(toCetDate(date), type, country, importContexts -> {
            if (importContexts.size() > 0) {
                for (ImportContext importContext : importContexts) {
                    if (importContext.importer.exists(importContext.ds)) {
                        return true;
                    }
                }
                return null;
            }
            return null;
        }) != null;
	}

    private void browse(Path dir, Consumer<Path> handler) {
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted().forEach(child -> {
                if (Files.isDirectory(child)) {
                    browse(child, handler);
                } else {
                    try {
                        if (Files.size(child) > 0) {
                            handler.accept(child);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<DateTime> dataAvailable(CaseType type, Set<Country> countries, Interval interval) {
        Set<UcteGeographicalCode> geographicalCodes = new HashSet<>();
        if (countries == null) {
            geographicalCodes.add(UcteGeographicalCode.UX);
            geographicalCodes.add(UcteGeographicalCode.UC);
        } else {
            for (Country country : countries) {
                geographicalCodes.addAll(forCountryHacked(country));
            }
        }
        Multimap<DateTime, UcteGeographicalCode> dates = HashMultimap.create();

        String typeDirS=type.name();
        if (isIntraday(type)) {
            typeDirS = "IDCF";
        } else if (type.equals(CaseType.D2)) {
            typeDirS="2D"; // because enum names cannot be prefixed with a digit
        }

        for (EntsoeFormat format : formats) {
            Path formatDir = config.getRootDir().resolve(format.getDirName());
            if (Files.exists(formatDir)) {
                Path typeDir = formatDir.resolve(typeDirS);
                if (Files.exists(typeDir)) {
                    browse(typeDir, path -> {
                        UcteFileName ucteFileName = UcteFileName.parse(path.getFileName().toString());
                        UcteGeographicalCode geographicalCode = ucteFileName.getGeographicalCode();
                        if (geographicalCode != null
                                && !config.getForbiddenFormatsByGeographicalCode().get(geographicalCode).contains(format.getImporter().getFormat())
                                && interval.contains(ucteFileName.getDate())) {
                            dates.put(ucteFileName.getDate(), geographicalCode);
                        }
                    });
                }
            }
        }
        return dates.asMap().entrySet().stream()
                .filter(e -> new HashSet<>(e.getValue()).containsAll(geographicalCodes))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
