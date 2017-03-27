/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.modules.wca;

import java.util.concurrent.CompletableFuture;

import eu.itesla_project.modules.wca.report.WCAReport;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface WCA {

    WCAResult run(WCAParameters parameters) throws Exception;

    CompletableFuture<WCAAsyncResult> runAsync(String baseStateId, WCAParameters parameters) throws Exception;

    WCAReport getReport();

}
