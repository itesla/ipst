# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import sys

from py4j.java_gateway import JavaGateway, GatewayParameters
from py4j.java_gateway import java_import

# connect to the JVM (running iPST/PowSyBl)
gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_field=True))

# other boilerplate imports
java_import(gateway.jvm, 'java.nio.file.Paths')
java_import(gateway.jvm, 'java.io.*')
java_import(gateway.jvm, 'java.util.Properties')
java_import(gateway.jvm, 'java.lang.String')

java_import(gateway.jvm, 'com.powsybl.commons.config.ComponentDefaultConfig')
java_import(gateway.jvm, 'com.powsybl.computation.local.LocalComputationManager')
java_import(gateway.jvm, 'com.powsybl.loadflow.LoadFlowFactory')
java_import(gateway.jvm, 'com.powsybl.iidm.import_.Importers')
java_import(gateway.jvm, 'com.powsybl.iidm.export.Exporters')
java_import(gateway.jvm, 'com.powsybl.iidm.network.test.FictitiousSwitchFactory')

# java_import(gateway.jvm, 'py4j.GatewayServer')
# server = gateway.jvm.py4j.GatewayServer()

string_class = gateway.jvm.java.lang.String

default_exporter_properties = gateway.jvm.java.util.Properties()

# needed below to resolve some class-factories, by name
ReflectionUtil = gateway.jvm.py4j.reflection.ReflectionUtil

# load platform config
defaultConfig = gateway.jvm.ComponentDefaultConfig.load()

# instantiate a computation manager and a LF factory
computation_manager = gateway.jvm.LocalComputationManager()
lf_factory = defaultConfig.newFactoryImpl(ReflectionUtil.classForName("com.powsybl.loadflow.LoadFlowFactory"))

lf_para = gateway.jvm.com.powsybl.loadflow.LoadFlowParameters.load()
importer = gateway.jvm.com.powsybl.iidm.import_.Importers
exporter = gateway.jvm.com.powsybl.iidm.export.Exporters
network = None


def load(path):
    global network
    network = importer.loadNetwork(path)


def run_load_flow():
    if network is None:
        sys.exit('network is null!')

    # instantiate a LF
    load_flow = lf_factory.create(network, computation_manager, 0)
    lf_result = load_flow.run(network.getStateManager().getWorkingStateId(), lf_para).join()
    return lf_result


def get_network():
    return network


def save(str_path):
    # convert string path to jvm string array
    split = str_path.split("/")
    length = len(split)
    str_array = gateway.new_array(string_class, length)
    for i in range(length):
        str_array[i] = split[i]

    # find format
    foo = str_array[length-1].split(".")
    ext = foo[len(foo)-1]
    export_format = ext.upper()

    dest_path = gateway.jvm.java.nio.file.Paths.get("/", str_array)

    exporter.export(export_format, network, default_exporter_properties, dest_path)


def shundown_pypowsybl():
    gateway.shutdown()
