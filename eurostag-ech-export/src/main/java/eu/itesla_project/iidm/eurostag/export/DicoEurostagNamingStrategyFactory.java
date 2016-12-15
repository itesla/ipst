package eu.itesla_project.iidm.eurostag.export;

import eu.itesla_project.commons.config.ModuleConfig;
import eu.itesla_project.commons.config.PlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
public class DicoEurostagNamingStrategyFactory implements EurostagNamingStrategyFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DicoEurostagNamingStrategyFactory.class);

    @Override
    public EurostagNamingStrategy create() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfigIfExists("eurostag-naming-strategy-dico");
        if (config != null) {
            Path dicoFile = config.getPathProperty("dicoFile", null);
            LOGGER.info("property dicoFile: {}", dicoFile);
            return new DicoEurostagNamingStrategy(dicoFile);
        } else {
            return new CutEurostagNamingStrategy();
        }
    }
}
