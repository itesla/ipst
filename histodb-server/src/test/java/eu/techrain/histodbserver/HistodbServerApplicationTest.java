/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.techrain.histodbserver;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.junit.Test;
import org.springframework.http.ResponseEntity;

import eu.itesla_project.histodb.config.HistoDbConfiguration;
import eu.itesla_project.histodb.service.mapdb.HistoDataServiceImpl;
import eu.itesla_project.histodb.web.HistoDataResource;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class HistodbServerApplicationTest {

    private final HistoDataResource resource = new HistoDataResource();

    @Test
    public void histodbServerTest() throws IOException {
        HistoDbConfiguration config = new HistoDbConfiguration(new Properties());
        config.getMapDb().setPersistent(false);
        config.getFormatter().setLocale("fr-FR");
        config.getFormatter().setSeparator(';');
        resource.setConfig(config);
        resource.setService(new HistoDataServiceImpl());
        importNetworks();
        setWrongReferenceCIM();
        setReferenceCIM();
        getReferenceCIM();
        getData();
        getFilteredData();
        getDataByIds();
        getForecastsDiff();
        getStats();
    }

    public void setReferenceCIM() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setParameter("dir", (String[]) Collections.singletonList("src/test/resources/network.xiidm").toArray(new String[1]));
        ResponseEntity<String> resp = resource.setReferenceCIM("iteslasim", "test", "2017", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }
    
    public void setWrongReferenceCIM() {
        WebRequestMock wr = new WebRequestMock();
        wr.setParameter("dir", (String[]) Collections.singletonList("src/test/resources/error").toArray(new String[1]));
        ResponseEntity<String> resp = resource.setReferenceCIM("iteslasim", "test", "2017", wr);
        assertEquals(404, resp.getStatusCodeValue());
    }
    
    public void getReferenceCIM() {
        ResponseEntity<String> resp = resource.getReferenceCIM("iteslasim", "test", "2017");
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void importNetworks() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setParameter("dir", Collections.singletonList("src/test/resources/").toArray(new String[1]));
        ResponseEntity <String> resp = (ResponseEntity<String>) resource.importNetworks("iteslasim", "test", "2017", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getData() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "csv", 0);
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]) );
        ResponseEntity <byte[]> resp = resource.getData("iteslasim", "test", "2017", "data", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getFilteredData() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "zip", 0); 
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]));
        wr.setParameter("forecast", (String[]) Collections.singletonList("0").toArray(new String[1]));
        wr.setParameter("time", (String[]) Collections.singletonList("[2017-01-10T01:00:00Z,2017-01-10T12:00:00Z]").toArray(new String[1]));
        wr.setParameter("daytime", (String[]) Collections.singletonList("[00:30:00,23:59:59]").toArray(new String[1]));
        String[] countries = {"FR,BE"};
        wr.setParameter("country", countries);
        String[] equip = {"gen,loads,shunts,stations,2wt,3wt,lines,dangling"};
        wr.setParameter("equip", equip);
        wr.setParameter("colRange", (String[]) Collections.singletonList("2-5").toArray(new String[1]));
        ResponseEntity <byte[]> resp = resource.getData("iteslasim", "test", "2017", "data", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getDataByIds() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "csv", 0); 
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]));
        String[] ids = {"load1,load2,substation1,generator1,LINE1"};
        wr.setParameter("ids", ids);        
        ResponseEntity <byte[]> resp = resource.getData("iteslasim", "test", "2017", "data", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getStats() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "csv", 0);
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]) );
        ResponseEntity <byte[]> resp = resource.getData("iteslasim", "test", "2017", "stats", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getForecastsDiff() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "csv", 0);
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]) );
        wr.setParameter("forecast", (String[]) Collections.singletonList("540").toArray(new String[1]) );
        ResponseEntity <byte[]> resp = resource.forecastDiff("iteslasim", "test", "2017", "forecastsDiff", wr);
        assertEquals(200, resp.getStatusCodeValue());
    }

    public void getForecastsDiffError() throws IOException {
        WebRequestMock wr = new WebRequestMock();
        wr.setAttribute("format", "csv", 0);
        wr.setParameter("headers", (String[]) Collections.singletonList("true").toArray(new String[1]) );
        ResponseEntity <byte[]> resp = resource.forecastDiff("iteslasim", "test", "2017", "forecastsDiff", wr);
        assertEquals(400, resp.getStatusCodeValue());
    }

}
