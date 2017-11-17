/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import eu.itesla_project.histodb.QueryParams;
import eu.itesla_project.histodb.config.HistoDbConfiguration;
import eu.itesla_project.histodb.domain.DataSet;
import eu.itesla_project.histodb.repository.mapdb.HistoDataSource;
import eu.itesla_project.histodb.repository.mapdb.HistoDataSourceFactory;
import eu.itesla_project.histodb.service.HistoDataService;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@RestController
@RequestMapping("/histodb/rest")
public class HistoDataResource {

    static Logger log = LoggerFactory.getLogger(HistoDataResource.class);

    @Inject
    private HistoDbConfiguration config;

    @Inject
    private HistoDataService service;

    @GetMapping(value = "/{db}/{prefix}/{postfix}/itesla/referenceCIM")
    public ResponseEntity<String> getReferenceCIM(@PathVariable String db, @PathVariable String prefix,
            @PathVariable String postfix) {
        try (HistoDataSource hds = HistoDataSourceFactory.getInstance(config, db, prefix, postfix)) {
            String ref = hds.getReferenceNetwork() != null ? hds.getReferenceNetwork().getId() : "";
            return new ResponseEntity<String>(ref, null, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Get reference CIM error", e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/{db}/{prefix}/{postfix}/itesla/referenceCIM", method = {
            RequestMethod.PUT, RequestMethod.POST })
    public ResponseEntity<String> setReferenceCIM(@PathVariable String db, @PathVariable String prefix,
            @PathVariable String postfix, WebRequest request) {
        try (HistoDataSource hds = HistoDataSourceFactory.getInstance(config, db, prefix, postfix)) {
            String dir = request.getParameter("dir");
            if (dir == null) {
                log.error("Missing parameter 'dir'");
                return new ResponseEntity<String>("Missing parameter 'dir'", HttpStatus.BAD_REQUEST);
            }
            log.info("Set reference CIM from path " + dir);
            Path filePath = Paths.get(dir);
            service.importReferenceNetwork(hds, filePath);
            return new ResponseEntity<String>(
                    "Set ReferenceCIM: " + hds.getReferenceNetwork() != null ? hds.getReferenceNetwork().getId()
                            : "" + " for data source " + prefix + "/" + postfix, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Set reference CIM error", e.getMessage());
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{db}/{prefix}/{postfix}/itesla")
    public ResponseEntity<String> importNetworks(@PathVariable String db, @PathVariable String prefix,
            @PathVariable String postfix, WebRequest request) {
        String dir = request.getParameter("dir");
        if (dir == null) {
            log.error("Missing parameter 'dir'");
            return new ResponseEntity<String>("Missing parameter 'dir'", HttpStatus.BAD_REQUEST);
        }
        log.info("Import data from path " + dir);
        Path importDir = Paths.get(dir);
        String parallelStr = request.getParameter("parallel");
        boolean parallel = parallelStr != null ? Boolean.parseBoolean(parallelStr) : true;

        try (HistoDataSource hds = HistoDataSourceFactory.getInstance(config, db, prefix, postfix)) {
            service.importData(hds, importDir, parallel);
        } catch (Exception e) {
            log.error("Import data error", e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(
                "HistoDB Import from " + dir + " for data source " + prefix + "/" + postfix,
                null, HttpStatus.OK);
    }

    @RequestMapping("/{db}/{prefix}/{postfix}/itesla/{service}")
    public ResponseEntity<byte[]> getData(@PathVariable String db, @PathVariable String prefix,
            @PathVariable String postfix, @PathVariable String service, WebRequest request) {
        log.info("getData " + service);
        String format = (String) request.getAttribute("format", 0);

        try (HistoDataSource hds = HistoDataSourceFactory.getInstance(config, db, prefix, postfix)) {

            QueryParams queryParams = new QueryParams(request);
            DataSet data = null;
            if (service.equals("data")) {
                data = this.service.getData(hds, queryParams);
            } else if (service.equals("stats")) {
                data = this.service.getStats(hds, queryParams);
            } else {
                return new ResponseEntity<byte[]>(service.getBytes(), HttpStatus.NOT_FOUND);
            }
            StringWriter wr = new StringWriter();
            data.writeCsv(wr, new Locale(config.getFormatter().getLocale()), config.getFormatter().getSeparator(),
                    queryParams.isHeaders());
            byte[] bytes = wr.toString().getBytes();
            HttpHeaders header = new HttpHeaders();

            if (format != null && format.equalsIgnoreCase("zip")) {
                bytes = gzip(bytes);
                header.add("Content-Encoding", "gzip");
            }

            header.setContentType(new MediaType("text", "csv"));
            return new ResponseEntity<byte[]>(bytes, header, HttpStatus.OK);
        } catch (Exception e) {
            log.error("getData error ", e);
            return new ResponseEntity<byte[]>(e.getMessage() != null ? e.getMessage().getBytes() : null, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping("/{db}/{prefix}/{postfix}/itesla/data/{service}")
    public ResponseEntity<byte[]> forecastDiff(@PathVariable String db, @PathVariable String prefix,
            @PathVariable String postfix, @PathVariable String service, WebRequest request) {
        String format = (String) request.getAttribute("format", 0);

        if (!service.equals("forecastsDiff")) {
            return new ResponseEntity<byte[]>(service.getBytes(), HttpStatus.NOT_FOUND);
        }

        try (HistoDataSource hds = HistoDataSourceFactory.getInstance(config, db, prefix, postfix)) {

            QueryParams queryParams = new QueryParams(request);

            if (queryParams.getForecastTime() < 0 && (queryParams.getHorizon() == null ||  queryParams.getHorizon().equals("SN"))) {
                return new ResponseEntity<byte[]>("ForecastsDiff operation must be used with either a positive 'forecast' value or a non-snapshot 'horizon'".getBytes(), HttpStatus.BAD_REQUEST);
            }

            DataSet data = this.service.getForecastDiff(hds, queryParams);
            StringWriter wr = new StringWriter();
            data.writeCsv(wr, new Locale(config.getFormatter().getLocale()), config.getFormatter().getSeparator(),
                    queryParams.isHeaders());
            byte[] bytes = wr.toString().getBytes();
            HttpHeaders header = new HttpHeaders();

            if (format != null && format.equalsIgnoreCase("zip")) {
                bytes = gzip(bytes);
                header.add("Content-Encoding", "gzip");
            }

            header.setContentType(new MediaType("text", "csv"));
            return new ResponseEntity<byte[]>(bytes, header, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Forecastdiff error ", e);
            return new ResponseEntity<byte[]>(e.getMessage() != null ? e.getMessage().getBytes() : null, HttpStatus.BAD_REQUEST);
        }
    }

    private byte[] gzip(byte[] in) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        GZIPOutputStream out = new GZIPOutputStream(bao);
        out.write(in);
        out.close();
        return bao.toByteArray();
    }

    public void setConfig(HistoDbConfiguration config) {
        this.config = config;
    }

    public void setService(HistoDataService service) {
        this.service = service;
    }
}
