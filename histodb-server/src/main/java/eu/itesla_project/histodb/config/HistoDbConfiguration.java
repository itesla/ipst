/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.config;

import java.util.Objects;
import java.util.Properties;

import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@Component
@ConfigurationProperties
public class HistoDbConfiguration {

    public final MapDb mapDb = new MapDb();
    public final Formatter formatter = new Formatter();
    public final Server server = new Server();
    public final Ssl ssl = new Ssl();
    public final Security security = new Security();


    public HistoDbConfiguration() {
        ModuleConfig config = PlatformConfig.defaultConfig().getModuleConfig("histodb-server");
        mapDb.setPersistent(config.getBooleanProperty("persistent"));
        mapDb.setBasedir(config.getStringProperty("basedir"));

        formatter.setSeparator(config.getStringProperty("separator", ";").charAt(0));
        formatter.setLocale(config.getStringProperty("locale"));
        server.setHost(config.getStringProperty("host"));
        server.setPort(config.getIntProperty("port", 8080));

        ssl.setKeyStoreType(config.getStringProperty("keyStoreType"));
        ssl.setKeyStore(config.getStringProperty("keyStore"));
        ssl.setKeyStorePassword(config.getStringProperty("keyStorePassword"));
        ssl.setKeyAlias(config.getStringProperty("keyAlias"));

        security.setUsername(config.getStringProperty("username"));
        security.setPassword(config.getStringProperty("password"));
    }

    public HistoDbConfiguration(Properties props) {
        Objects.requireNonNull(props);
        mapDb.setPersistent(Boolean.valueOf(props.getProperty("persistent")));
        mapDb.setBasedir(props.getProperty("basedir"));
        formatter.setSeparator(props.getProperty("separator", ";").charAt(0));
        formatter.setLocale(props.getProperty("locale"));
        server.setHost(props.getProperty("host"));
        server.setPort(Integer.parseInt(props.getProperty("port", "8080")));

        ssl.setKeyStoreType(props.getProperty("keyStoreType"));
        ssl.setKeyStore(props.getProperty("keyStore"));
        ssl.setKeyStorePassword(props.getProperty("keyStorePassword"));
        ssl.setKeyAlias(props.getProperty("keyAlias"));

        security.setUsername(props.getProperty("username"));
        security.setPassword(props.getProperty("password"));
    }

    public MapDb getMapDb() {
        return mapDb;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public static class Formatter {

        public String locale;
        public char separator;

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public char getSeparator() {
            return separator;
        }

        public void setSeparator(char separator) {
            this.separator = separator;
        }
    }

    public static class MapDb {

        public String basedir;

        public boolean persistent;

        public String getBasedir() {
            return basedir;
        }

        public void setBasedir(String basedir) {
            this.basedir = basedir;
        }

        public boolean isPersistent() {
            return this.persistent;
        }

        public void setPersistent(boolean persistent) {
            this.persistent = persistent;
        }
    }

    public static class Server {

        public String host;

        public int port;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }

    public static class Security {

        public String username;

        public String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    }

    public Server getServer() {
        return server;
    }

    public Security getSecurity() {
        return security;
    }

}
