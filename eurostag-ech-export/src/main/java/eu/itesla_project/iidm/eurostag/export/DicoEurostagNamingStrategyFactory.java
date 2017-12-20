package eu.itesla_project.iidm.eurostag.export;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class DicoEurostagNamingStrategyFactory implements EurostagNamingStrategyFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DicoEurostagNamingStrategyFactory.class);
    private static final String CONFIG_SECTION_NAME = "eurostag-naming-strategy-dico";
    private static final String CONFIG_PROPERTY_DICO_FILE_NAME = "dicoFile";

    @Override
    public EurostagNamingStrategy create() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfigIfExists(CONFIG_SECTION_NAME);
        if (config != null) {
            Path dicoFile = config.getPathProperty(CONFIG_PROPERTY_DICO_FILE_NAME, null);
            LOGGER.info("Instantiating DicoEurostagNamingStrategy: property {}={} declared in config section '{}'", CONFIG_PROPERTY_DICO_FILE_NAME, dicoFile, CONFIG_SECTION_NAME);
            dicoFile = config.getPathProperty(CONFIG_PROPERTY_DICO_FILE_NAME);
            return new DicoEurostagNamingStrategy(dicoFile);
        } else {
            LOGGER.warn("Cannot instantiate DicoEurostagNamingStrategy: config section '{}' not found  . Using CutEurostagNamingStrategy, instead.", CONFIG_SECTION_NAME);
            return new CutEurostagNamingStrategy();
        }
    }
}
