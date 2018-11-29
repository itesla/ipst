# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from py4j.java_gateway import JavaGateway, GatewayParameters
from py4j.java_gateway import java_import
from py4j.protocol import Py4JError

gateway = None
string_class = None

importer = None
exporter = None

lf_para = None
defaultConfig = None
default_exporter_properties = None


def connect(nb_port):
    # connect to the JVM (running iPST/PowSyBl)
    global gateway
    try:
        gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_field=True,port=nb_port))

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

        global string_class
        string_class = gateway.jvm.java.lang.String

        global default_exporter_properties
        default_exporter_properties = gateway.jvm.java.util.Properties()

        # needed below to resolve some class-factories, by name
        ReflectionUtil = gateway.jvm.py4j.reflection.ReflectionUtil

        # load platform config
        global defaultConfig
        defaultConfig = gateway.jvm.ComponentDefaultConfig.load()

        # instantiate a computation manager and a LF factory
        global computation_manager
        computation_manager = gateway.jvm.LocalComputationManager()
        global lf_factory
        lf_factory = defaultConfig.newFactoryImpl(ReflectionUtil.classForName("com.powsybl.loadflow.LoadFlowFactory"))
        global lf_para
        lf_para = gateway.jvm.com.powsybl.loadflow.LoadFlowParameters.load()
        global importer
        importer = gateway.jvm.com.powsybl.iidm.import_.Importers
        global exporter
        exporter = gateway.jvm.com.powsybl.iidm.export.Exporters
        return True
    except Py4JError:
        return False


def load(path):
    return Network(importer.loadNetwork(path))


def run_load_flow(network):
    # instantiate a LF
    jnetwork = network.get_java_network()
    load_flow = lf_factory.create(jnetwork, computation_manager, 0)
    lf_result = load_flow.run(jnetwork.getStateManager().getWorkingStateId(), lf_para).join()
    return LoadFlowResult(lf_result)


def save(network, str_path):
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

    exporter.export(export_format, network.get_java_network(), default_exporter_properties, dest_path)


def shundown_pypowsybl():
    gateway.shutdown()


class Network:

    def __init__(self, java_network):
        self.j_instance = java_network

    def get_java_network(self):
        return self.j_instance

    def run_load_flow(self):
        return run_load_flow(self)

    def save(self, path):
        save(self, path)

    def get_line(self, id):
        return self.j_instance.getLine(id)

    def get_lines(self):
        j_lines = self.j_instance.getLines().toList()
        lines = []
        for l in j_lines:
            ll = Line(l)
            lines.append(ll)
        return lines


class LoadFlowResult:

    def __init__(self, j_lf_res):
        self.j_instance = j_lf_res

    def is_ok(self):
        return self.j_instance.isOk()

    def get_metrics(self):
        return self.j_instance.getMetrics()

    def get_logs(self):
        return self.j_instance.getLogs()


class Identifiable:

    def __init__(self, j_identifiable):
        self.j_instance = j_identifiable

    def get_id(self):
        return self.j_instance.getId()


class Branch(Identifiable):

    def __init__(self, j_branch):
        Identifiable.__init__(self, j_branch)
        self.j_instance = j_branch

    def is_overloaded(self):
        return self.j_instance.isOverloaded()

    def get_terminal_1(self):
        return Terminal(self.j_instance.getTerminal1())

    def get_terminal_2(self):
        return Terminal(self.j_instance.getTerminal2())


class Line(Branch):

    def __init__(self, j_line):
        Branch.__init__(self, j_line)
        self.j_instance = j_line


class Terminal:

    def __init__(self, j_terminal):
        self.j_instance = j_terminal

    def set_p(self, p):
        self.j_instance.setP(p)
        return self

    def set_q(self, q):
        self.j_instance.setQ(q)
        return self

    def get_p(self):
        return self.j_instance.getP()

    def get_q(self):
        return self.j_instance.getQ()

    def get_i(self):
        return self.j_instance.getI()

    def connect(self):
        return self.j_instance.connect()

    def disconnect(self):
        return self.j_instance.disconnect()

    def is_connected(self):
        return self.j_instance.isConnected()
