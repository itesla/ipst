/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import eu.itesla_project.histodb.web.handler.DotPathInterceptor;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    public DotPathInterceptor dotInterceptor;

    @Autowired
    public HistoDbConfiguration config;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dotInterceptor).addPathPatterns("/**");
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return container -> {
            if (config.getServer().getHost() != null) {
                try {
                    container.setAddress(InetAddress.getByName(config.getServer().getHost()));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            container.setPort(config.getServer().getPort());
            container.setSsl(config.getSsl());
        };
    }

}
