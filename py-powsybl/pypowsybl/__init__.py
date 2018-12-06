# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
import subprocess
import time
from enum import Enum
from threading import Thread

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

retried = 0


def launch_task(config_name, nb_port):
    if config_name is None:
        cmd = 'itools py-powsybl -port=' + str(nb_port)
    else:
        cmd = 'itools --config-name ' + config_name + ' py-powsybl -port=' + str(nb_port)
    subprocess.call([cmd], shell=True)


def launch(nb_port):
    launch(None, nb_port)


def launch(config_name, nb_port):
    t = Thread(target=launch_task, args=(config_name, nb_port))
    t.start()


def connect(nb_port):
    # connect to the JVM (running iPST/PowSyBl)
    global gateway
    try:
        time.sleep(1)
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

        global string_class
        string_class = gateway.jvm.java.lang.String

        global default_exporter_properties
        default_exporter_properties = gateway.jvm.java.util.Properties()

        # needed below to resolve some class-factories, by name
        reflection_util = gateway.jvm.py4j.reflection.ReflectionUtil

        # load platform config
        global defaultConfig
        defaultConfig = gateway.jvm.ComponentDefaultConfig.load()

        # instantiate a computation manager and a LF factory
        global computation_manager
        computation_manager = gateway.jvm.LocalComputationManager()
        global lf_factory
        lf_factory = defaultConfig.newFactoryImpl(reflection_util.classForName("com.powsybl.loadflow.LoadFlowFactory"))
        global lf_para
        lf_para = gateway.jvm.com.powsybl.loadflow.LoadFlowParameters.load()
        global importer
        importer = gateway.jvm.com.powsybl.iidm.import_.Importers
        global exporter
        exporter = gateway.jvm.com.powsybl.iidm.export.Exporters
        return True
    except Py4JError:
        global retried
        while retried < 5:
            retried = retried + 1
            print("retring..." + str(retried))
            connect(nb_port)
        return False


def load_network(path):
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


def shutdown_pypowsybl():
    gateway.shutdown()


class Identifiable:

    def __init__(self, j_identifiable):
        self.j_instance = j_identifiable

    def get_id(self):
        return self.j_instance.getId()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Network
class Network(Identifiable):

    def __init__(self, j_network):
        Identifiable.__init__(self, j_network)
        self.j_instance = j_network

    def add_listener(self, var0):
        self.j_instance.addListener(var0)

    def get_branch(self, var0):
        return Branch(self.j_instance.getBranch(var0))

    def get_branch_count(self):
        return self.j_instance.getBranchCount()

    def get_branches(self):
        l_branch = []
        for j_e in self.j_instance.getBranches().toList():
            l_branch.append(Branch(j_e))
        return l_branch

    def get_busbar_section(self, var0):
        return BusbarSection(self.j_instance.getBusbarSection(var0))

    def get_busbar_section_count(self):
        return self.j_instance.getBusbarSectionCount()

    def get_busbar_sections(self):
        l_busbarsection = []
        for j_e in self.j_instance.getBusbarSections().toArray():
            l_busbarsection.append(BusbarSection(j_e))
        return l_busbarsection

    def get_country_count(self):
        return self.j_instance.getCountryCount()

    def get_dangling_line(self, var0):
        return DanglingLine(self.j_instance.getDanglingLine(var0))

    def get_dangling_line_count(self):
        return self.j_instance.getDanglingLineCount()

    def get_dangling_lines(self):
        l_danglingline = []
        for j_e in self.j_instance.getDanglingLines().toArray():
            l_danglingline.append(DanglingLine(j_e))
        return l_danglingline

    def get_forecast_distance(self):
        return self.j_instance.getForecastDistance()

    def get_generator(self, var0):
        return Generator(self.j_instance.getGenerator(var0))

    def get_generator_count(self):
        return self.j_instance.getGeneratorCount()

    def get_generators(self):
        l_generator = []
        for j_e in self.j_instance.getGenerators().toArray():
            l_generator.append(Generator(j_e))
        return l_generator

    def get_hvdc_converter_station(self, var0):
        return HvdcConverterStation(self.j_instance.getHvdcConverterStation(var0))

    def get_hvdc_converter_station_count(self):
        return self.j_instance.getHvdcConverterStationCount()

    def get_hvdc_converter_stations(self):
        l_hvdcconverterstation = []
        for j_e in self.j_instance.getHvdcConverterStations().toList():
            l_hvdcconverterstation.append(HvdcConverterStation(j_e))
        return l_hvdcconverterstation

    def get_hvdc_line(self, var0):
        return HvdcLine(self.j_instance.getHvdcLine(var0))

    def get_hvdc_line_count(self):
        return self.j_instance.getHvdcLineCount()

    def get_hvdc_lines(self):
        l_hvdcline = []
        for j_e in self.j_instance.getHvdcLines().toArray():
            l_hvdcline.append(HvdcLine(j_e))
        return l_hvdcline

    def get_identifiable(self, var0):
        return Identifiable(self.j_instance.getIdentifiable(var0))

    def get_lcc_converter_station(self, var0):
        return LccConverterStation(self.j_instance.getLccConverterStation(var0))

    def get_lcc_converter_station_count(self):
        return self.j_instance.getLccConverterStationCount()

    def get_lcc_converter_stations(self):
        l_lccconverterstation = []
        for j_e in self.j_instance.getLccConverterStations().toArray():
            l_lccconverterstation.append(LccConverterStation(j_e))
        return l_lccconverterstation

    def get_line(self, var0):
        return Line(self.j_instance.getLine(var0))

    def get_line_count(self):
        return self.j_instance.getLineCount()

    def get_lines(self):
        l_line = []
        for j_e in self.j_instance.getLines().toList():
            l_line.append(Line(j_e))
        return l_line

    def get_load(self, var0):
        return Load(self.j_instance.getLoad(var0))

    def get_load_count(self):
        return self.j_instance.getLoadCount()

    def get_loads(self):
        l_load = []
        for j_e in self.j_instance.getLoads().toArray():
            l_load.append(Load(j_e))
        return l_load

    def get_shunt(self, var0):
        return ShuntCompensator(self.j_instance.getShunt(var0))

    def get_shunt_count(self):
        return self.j_instance.getShuntCount()

    def get_shunts(self):
        l_shuntcompensator = []
        for j_e in self.j_instance.getShunts().toArray():
            l_shuntcompensator.append(ShuntCompensator(j_e))
        return l_shuntcompensator

    def get_source_format(self):
        return self.j_instance.getSourceFormat()

    def get_static_var_compensator(self, var0):
        return StaticVarCompensator(self.j_instance.getStaticVarCompensator(var0))

    def get_static_var_compensator_count(self):
        return self.j_instance.getStaticVarCompensatorCount()

    def get_static_var_compensators(self):
        l_staticvarcompensator = []
        for j_e in self.j_instance.getStaticVarCompensators().toArray():
            l_staticvarcompensator.append(StaticVarCompensator(j_e))
        return l_staticvarcompensator

    def get_substation(self, var0):
        return Substation(self.j_instance.getSubstation(var0))

    def get_substation_count(self):
        return self.j_instance.getSubstationCount()

    def get_substations(self):
        l_substation = []
        for j_e in self.j_instance.getSubstations().toArray():
            l_substation.append(Substation(j_e))
        return l_substation

    def get_switch(self, var0):
        return Switch(self.j_instance.getSwitch(var0))

    def get_switch_count(self):
        return self.j_instance.getSwitchCount()

    def get_switches(self):
        l_switch = []
        for j_e in self.j_instance.getSwitches().toArray():
            l_switch.append(Switch(j_e))
        return l_switch

    def get_three_windings_transformer(self, var0):
        return ThreeWindingsTransformer(self.j_instance.getThreeWindingsTransformer(var0))

    def get_three_windings_transformer_count(self):
        return self.j_instance.getThreeWindingsTransformerCount()

    def get_three_windings_transformers(self):
        l_threewindingstransformer = []
        for j_e in self.j_instance.getThreeWindingsTransformers().toArray():
            l_threewindingstransformer.append(ThreeWindingsTransformer(j_e))
        return l_threewindingstransformer

    def get_two_windings_transformer(self, var0):
        return TwoWindingsTransformer(self.j_instance.getTwoWindingsTransformer(var0))

    def get_two_windings_transformer_count(self):
        return self.j_instance.getTwoWindingsTransformerCount()

    def get_two_windings_transformers(self):
        l_twowindingstransformer = []
        for j_e in self.j_instance.getTwoWindingsTransformers().toArray():
            l_twowindingstransformer.append(TwoWindingsTransformer(j_e))
        return l_twowindingstransformer

    def get_voltage_level(self, var0):
        return VoltageLevel(self.j_instance.getVoltageLevel(var0))

    def get_voltage_level_count(self):
        return self.j_instance.getVoltageLevelCount()

    def get_voltage_levels(self):
        l_voltagelevel = []
        for j_e in self.j_instance.getVoltageLevels().toList():
            l_voltagelevel.append(VoltageLevel(j_e))
        return l_voltagelevel

    def get_vsc_converter_station(self, var0):
        return VscConverterStation(self.j_instance.getVscConverterStation(var0))

    def get_vsc_converter_station_count(self):
        return self.j_instance.getVscConverterStationCount()

    def get_vsc_converter_stations(self):
        l_vscconverterstation = []
        for j_e in self.j_instance.getVscConverterStations().toArray():
            l_vscconverterstation.append(VscConverterStation(j_e))
        return l_vscconverterstation

    def get_java_network(self):
        return self.j_instance

    def run_load_flow(self):
        return run_load_flow(self)

    def save(self, path):
        save(self, path)


class LoadFlowResult:

    def __init__(self, j_lf_res):
        self.j_instance = j_lf_res

    def is_ok(self):
        return self.j_instance.isOk()

    def get_metrics(self):
        return self.j_instance.getMetrics()

    def get_logs(self):
        return self.j_instance.getLogs()


# TODO container
class Substation(Identifiable):

    def __init__(self, j_substation):
        Identifiable.__init__(self, j_substation)

    def get_tso(self):
        return self.j_instance.getTso()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Connectable
class Connectable(Identifiable):

    def __init__(self, j_connectable):
        Identifiable.__init__(self, j_connectable)
        self.j_instance = j_connectable

    def get_terminals(self):
        l_terminal = []
        for j_e in self.j_instance.getTerminals():
            l_terminal.append(Terminal(j_e))
        return l_terminal

    def get_type(self):
        return ConnectableType(str(self.j_instance.getType()))


class Injection(Connectable):

    def __init__(self, j_injection):
        Connectable.__init__(self, j_injection)
        self.j_instance = j_injection

    def get_terminal(self):
        return Terminal(self.j_instance.getTerminal())


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Branch
class Branch(Connectable):

    def __init__(self, j_branch):
        Connectable.__init__(self, j_branch)
        self.j_instance = j_branch

    def check_permanent_limit_1(self, var0=None):
        if var0 is None:
            return self.j_instance.checkPermanentLimit1()
        else:
            return self.j_instance.checkPermanentLimit1(var0)

    def check_permanent_limit_2(self, var0=None):
        if var0 is None:
            return self.j_instance.checkPermanentLimit2()
        else:
            return self.j_instance.checkPermanentLimit2(var0)

    def get_current_limits_1(self):
        return CurrentLimits(self.j_instance.getCurrentLimits1())

    def get_current_limits_2(self):
        return CurrentLimits(self.j_instance.getCurrentLimits2())

    def get_overload_duration(self):
        return self.j_instance.getOverloadDuration()

    def get_terminal(self, var0):
        return Terminal(self.j_instance.getTerminal(var0))

    def get_terminal_1(self):
        return Terminal(self.j_instance.getTerminal1())

    def get_terminal_2(self):
        return Terminal(self.j_instance.getTerminal2())

    def is_overloaded(self, var0=None):
        if var0 is None:
            return self.j_instance.isOverloaded()
        else:
            return self.j_instance.isOverloaded(var0)


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Bus
class Bus(Identifiable):

    def __init__(self, j_bus):
        Identifiable.__init__(self, j_bus)
        self.j_instance = j_bus

    def get_angle(self):
        return self.j_instance.getAngle()

    def get_connected_component(self):
        return Component(self.j_instance.getConnectedComponent())

    def get_connected_terminal_count(self):
        return self.j_instance.getConnectedTerminalCount()

    def get_dangling_lines(self):
        l_danglingline = []
        for j_e in self.j_instance.getDanglingLines().toArray():
            l_danglingline.append(DanglingLine(j_e))
        return l_danglingline

    def get_generators(self):
        l_generator = []
        for j_e in self.j_instance.getGenerators().toArray():
            l_generator.append(Generator(j_e))
        return l_generator

    def get_lcc_converter_stations(self):
        l_lccconverterstation = []
        for j_e in self.j_instance.getLccConverterStations().toArray():
            l_lccconverterstation.append(LccConverterStation(j_e))
        return l_lccconverterstation

    def get_lines(self):
        l_line = []
        for j_e in self.j_instance.getLines().toArray():
            l_line.append(Line(j_e))
        return l_line

    def get_loads(self):
        l_load = []
        for j_e in self.j_instance.getLoads().toArray():
            l_load.append(Load(j_e))
        return l_load

    def get_p(self):
        return self.j_instance.getP()

    def get_q(self):
        return self.j_instance.getQ()

    def get_shunt_compensators(self):
        l_shuntcompensator = []
        for j_e in self.j_instance.getShuntCompensators().toArray():
            l_shuntcompensator.append(ShuntCompensator(j_e))
        return l_shuntcompensator

    def get_static_var_compensators(self):
        l_staticvarcompensator = []
        for j_e in self.j_instance.getStaticVarCompensators().toArray():
            l_staticvarcompensator.append(StaticVarCompensator(j_e))
        return l_staticvarcompensator

    def get_synchronous_component(self):
        return Component(self.j_instance.getSynchronousComponent())

    def get_three_windings_transformers(self):
        l_threewindingtransformer = []
        for j_e in self.j_instance.getThreeWindingsTransformers().toArray():
            l_threewindingtransformer.append(ThreeWindingsTransformer(j_e))
        return l_threewindingtransformer

    def get_two_windings_transformers(self):
        l_twowindingtransformer = []
        for j_e in self.j_instance.getTwoWindingsTransformers().toArray():
            l_twowindingtransformer.append(TwoWindingsTransformer(j_e))
        return l_twowindingtransformer

    def get_v(self):
        return self.j_instance.getV()

    def get_voltage_level(self):
        return VoltageLevel(self.j_instance.getVoltageLevel())

    def get_vsc_converter_stations(self):
        l_vscconverterstation = []
        for j_e in self.j_instance.getVscConverterStations().toArray():
            l_vscconverterstation.append(VscConverterStation(j_e))
        return l_vscconverterstation

    def is_in_main_connected_component(self):
        return self.j_instance.isInMainConnectedComponent()

    def is_in_main_synchronous_component(self):
        return self.j_instance.isInMainSynchronousComponent()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Component
class Component:

    def __init__(self, j_component):
        self.j_instance = j_component

    def get_num(self):
        return self.j_instance.getNum()

    def get_buses(self):
        l_bus = []
        for j_e in self.j_instance.getBuses().toArray():
            l_bus.append(Bus(j_e))
        return l_bus

    def get_size(self):
        return self.j_instance.getSize()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.CurrentLimits
class CurrentLimits:

    def __init__(self, j_currentlimits):
        self.j_instance = j_currentlimits

    def get_permanent_limit(self):
        return self.j_instance.getPermanentLimit()

    def get_temporary_limit_value(self, var0):
        return self.j_instance.getTemporaryLimitValue(var0)


# Auto generated python wrapper for java class: com.powsybl.iidm.network.DanglingLine
class DanglingLine(Injection):

    def __init__(self, j_danglingline):
        Injection.__init__(self, j_danglingline)
        self.j_instance = j_danglingline

    def get_b(self):
        return self.j_instance.getB()

    def get_current_limits(self):
        return CurrentLimits(self.j_instance.getCurrentLimits())

    def get_g(self):
        return self.j_instance.getG()

    def get_p0(self):
        return self.j_instance.getP0()

    def get_q0(self):
        return self.j_instance.getQ0()

    def get_r(self):
        return self.j_instance.getR()

    def get_ucte_xnode_code(self):
        return self.j_instance.getUcteXnodeCode()

    def get_x(self):
        return self.j_instance.getX()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.HvdcConverterStation
class HvdcConverterStation(Injection):

    def __init__(self, j_hvdcconverterstation):
        Injection.__init__(self, j_hvdcconverterstation)
        self.j_instance = j_hvdcconverterstation

    def get_loss_factor(self):
        return self.j_instance.getLossFactor()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.HvdcLine
class HvdcLine(Identifiable):

    def __init__(self, j_hvdcline):
        Identifiable.__init__(self, j_hvdcline)
        self.j_instance = j_hvdcline

    def get_active_power_setpoint(self):
        return self.j_instance.getActivePowerSetpoint()

    def get_converter_station1(self):
        return HvdcConverterStation(self.j_instance.getConverterStation1())

    def get_converter_station2(self):
        return HvdcConverterStation(self.j_instance.getConverterStation2())

    def get_max_p(self):
        return self.j_instance.getMaxP()

    def get_network(self):
        return Network(self.j_instance.getNetwork())

    def get_nominal_v(self):
        return self.j_instance.getNominalV()

    def get_r(self):
        return self.j_instance.getR()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.LccConverterStation
class LccConverterStation(HvdcConverterStation):

    def __init__(self, j_lccconverterstation):
        HvdcConverterStation.__init__(self, j_lccconverterstation)
        self.j_instance = j_lccconverterstation

    def get_power_factor(self):
        return self.j_instance.getPowerFactor()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.VscConverterStation
class VscConverterStation(HvdcConverterStation):

    def __init__(self, j_vscconverterstation):
        HvdcConverterStation.__init__(self, j_vscconverterstation)
        self.j_instance = j_vscconverterstation

    def get_reactive_limits(self):
        return ReactiveLimits(self.j_instance.getReactiveLimits())

    def get_reactive_power_setpoint(self):
        return self.j_instance.getReactivePowerSetpoint()

    def get_voltage_setpoint(self):
        return self.j_instance.getVoltageSetpoint()

    def is_voltage_regulator_on(self):
        return self.j_instance.isVoltageRegulatorOn()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Line
class Line(Branch):

    def __init__(self, j_line):
        Branch.__init__(self, j_line)
        self.j_instance = j_line

    def get_b1(self):
        return self.j_instance.getB1()

    def get_b2(self):
        return self.j_instance.getB2()

    def get_g1(self):
        return self.j_instance.getG1()

    def get_g2(self):
        return self.j_instance.getG2()

    def get_r(self):
        return self.j_instance.getR()

    def get_x(self):
        return self.j_instance.getX()

    def is_tie_line(self):
        return self.j_instance.isTieLine()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Load
class Load(Injection):

    def __init__(self, j_load):
        Injection.__init__(self, j_load)
        self.j_instance = j_load

    def get_p0(self):
        return self.j_instance.getP0()

    def get_q0(self):
        return self.j_instance.getQ0()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Generator
class Generator(Injection):

    def __init__(self, j_generator):
        Injection.__init__(self, j_generator)
        self.j_instance = j_generator

    def get_target_v(self):
        return self.j_instance.getTargetV()

    def get_target_p(self):
        return self.j_instance.getTargetP()

    def get_target_q(self):
        return self.j_instance.getTargetQ()

    def get_rated_s(self):
        return self.j_instance.getRatedS()

    def get_regulating_terminal(self):
        return Terminal(self.j_instance.getRegulatingTerminal())

    def is_voltage_regulator_on(self):
        return self.j_instance.isVoltageRegulatorOn()

    def get_max_p(self):
        return self.j_instance.getMaxP()

    def get_min_p(self):
        return self.j_instance.getMinP()

    def get_reactive_limits(self):
        return ReactiveLimits(self.j_instance.getReactiveLimits())


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Terminal
class Terminal:

    def __init__(self, j_terminal):
        self.j_instance = j_terminal

    def connect(self):
        return self.j_instance.connect()

    def disconnect(self):
        return self.j_instance.disconnect()

    def get_connectable(self):
        return Connectable(self.j_instance.getConnectable())

    def get_i(self):
        return self.j_instance.getI()

    def get_p(self):
        return self.j_instance.getP()

    def get_q(self):
        return self.j_instance.getQ()

    def get_voltage_level(self):
        return VoltageLevel(self.j_instance.getVoltageLevel())

    def is_connected(self):
        return self.j_instance.isConnected()

    def traverse(self, var0):
        self.j_instance.traverse(var0)


# Auto generated python wrapper for java class: com.powsybl.iidm.network.ReactiveLimits
class ReactiveLimits:

    def __init__(self, j_reactivelimits):
        self.j_instance = j_reactivelimits

    def get_min_q(self):
        return self.j_instance.getMinQ()

    def get_max_q(self):
        return self.j_instance.getMaxQ()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.ThreeWindingsTransformer
class ThreeWindingsTransformer(Connectable):

    def __init__(self, j_threewindingstransformer):
        Connectable.__init__(self, j_threewindingstransformer)
        self.j_instance = j_threewindingstransformer

    def get_terminal(self):
        return Terminal(self.j_instance.getTerminal())

    def get_substation(self):
        return Substation(self.j_instance.getSubstation())


# Auto generated python wrapper for java class: com.powsybl.iidm.network.TwoWindingsTransformer
class TwoWindingsTransformer(Branch):

    def __init__(self, j_twowindingstransformer):
        Branch.__init__(self, j_twowindingstransformer)
        self.j_instance = j_twowindingstransformer

    def get_r(self):
        return self.j_instance.getR()

    def get_x(self):
        return self.j_instance.getX()

    def get_g(self):
        return self.j_instance.getG()

    def get_b(self):
        return self.j_instance.getB()

    def get_rated_u1(self):
        return self.j_instance.getRatedU1()

    def get_rated_u2(self):
        return self.j_instance.getRatedU2()

    def get_substation(self):
        return Substation(self.j_instance.getSubstation())


# Auto generated python wrapper for java class: com.powsybl.iidm.network.BusbarSection
class BusbarSection(Injection):

    def __init__(self, j_busbarsection):
        Injection.__init__(self, j_busbarsection)
        self.j_instance = j_busbarsection

    def get_angle(self):
        return self.j_instance.getAngle()

    def get_v(self):
        return self.j_instance.getV()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.VoltageLevel
# TODO container
class VoltageLevel(Identifiable):

    def __init__(self, j_voltagelevel):
        Identifiable.__init__(self, j_voltagelevel)
        self.j_instance = j_voltagelevel

    def get_high_voltage_limit(self):
        return self.j_instance.getHighVoltageLimit()

    def get_connectables(self):
        l_connectable = []
        for j_e in self.j_instance.getConnectables().toArray():
            l_connectable.append(Connectable(j_e))
        return l_connectable

    def get_lcc_converter_station_count(self):
        return self.j_instance.getLccConverterStationCount()

    def get_connectable_count(self):
        return self.j_instance.getConnectableCount()

    def get_generators(self):
        l_generator = []
        for j_e in self.j_instance.getGenerators().toArray():
            l_generator.append(Generator(j_e))
        return l_generator

    def get_generator_count(self):
        return self.j_instance.getGeneratorCount()

    def get_loads(self):
        l_load = []
        for j_e in self.j_instance.getLoads().toArray():
            l_load.append(Load(j_e))
        return l_load

    def get_switches(self):
        l_switch = []
        for j_e in self.j_instance.getSwitches().toArray():
            l_switch.append(Switch(j_e))
        return l_switch

    def get_switch_count(self):
        return self.j_instance.getSwitchCount()

    def get_load_count(self):
        return self.j_instance.getLoadCount()

    def get_shunt_count(self):
        return self.j_instance.getShuntCount()

    def get_shunt_compensators(self):
        l_shuntcompensator = []
        for j_e in self.j_instance.getShuntCompensators().toArray():
            l_shuntcompensator.append(ShuntCompensator(j_e))
        return l_shuntcompensator

    def get_shunt_compensator_count(self):
        return self.j_instance.getShuntCompensatorCount()

    def get_dangling_lines(self):
        l_danglingline = []
        for j_e in self.j_instance.getDanglingLines().toArray():
            l_danglingline.append(DanglingLine(j_e))
        return l_danglingline

    def get_dangling_line_count(self):
        return self.j_instance.getDanglingLineCount()

    def get_static_var_compensators(self):
        l_staticvarcompensator = []
        for j_e in self.j_instance.getStaticVarCompensators().toArray():
            l_staticvarcompensator.append(StaticVarCompensator(j_e))
        return l_staticvarcompensator

    def get_static_var_compensator_count(self):
        return self.j_instance.getStaticVarCompensatorCount()

    def get_vsc_converter_stations(self):
        l_vscconverterstation = []
        for j_e in self.j_instance.getVscConverterStations().toArray():
            l_vscconverterstation.append(VscConverterStation(j_e))
        return l_vscconverterstation

    def get_vsc_converter_station_count(self):
        return self.j_instance.getVscConverterStationCount()

    def get_lcc_converter_stations(self):
        l_lccconverterstation = []
        for j_e in self.j_instance.getLccConverterStations().toArray():
            l_lccconverterstation.append(LccConverterStation(j_e))
        return l_lccconverterstation

    def get_substation(self):
        return Substation(self.j_instance.getSubstation())

    def get_nominal_v(self):
        return self.j_instance.getNominalV()

    def get_low_voltage_limit(self):
        return self.j_instance.getLowVoltageLimit()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.ShuntCompensator
class ShuntCompensator(Injection):

    def __init__(self, j_shuntcompensator):
        Injection.__init__(self, j_shuntcompensator)
        self.j_instance = j_shuntcompensator

    def get_current_b(self):
        return self.j_instance.getCurrentB()

    def get_current_section_count(self):
        return self.j_instance.getCurrentSectionCount()

    def get_maximum_b(self):
        return self.j_instance.getMaximumB()

    def get_maximum_section_count(self):
        return self.j_instance.getMaximumSectionCount()

    def get_b_per_section(self):
        return self.j_instance.getbPerSection()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.StaticVarCompensator
class StaticVarCompensator(Injection):

    def __init__(self, j_staticvarcompensator):
        Injection.__init__(self, j_staticvarcompensator)
        self.j_instance = j_staticvarcompensator

    def get_bmax(self):
        return self.j_instance.getBmax()

    def get_bmin(self):
        return self.j_instance.getBmin()

    def get_reactive_power_set_point(self):
        return self.j_instance.getReactivePowerSetPoint()

    def get_voltage_set_point(self):
        return self.j_instance.getVoltageSetPoint()


# Auto generated python wrapper for java class: com.powsybl.iidm.network.Switch
class Switch(Identifiable):

    def __init__(self, j_switch):
        Identifiable.__init__(self, j_switch)
        self.j_instance = j_switch

    def get_voltage_level(self):
        return VoltageLevel(self.j_instance.getVoltageLevel())

    def is_fictitious(self):
        return self.j_instance.isFictitious()

    def is_open(self):
        return self.j_instance.isOpen()

    def is_retained(self):
        return self.j_instance.isRetained()

    def set_fictitious(self, var0):
        self.j_instance.setFictitious(var0)

    def set_open(self, var0):
        self.j_instance.setOpen(var0)

    def set_retained(self, var0):
        self.j_instance.setRetained(var0)


############ ENUM ##############

class ConnectableType(Enum):
    BUSBAR_SECTION = 'BUSBAR_SECTION'
    LINE = 'LINE'
    TWO_WINDINGS_TRANSFORMER = 'TWO_WINDINGS_TRANSFORMER'
    THREE_WINDINGS_TRANSFORMER = 'THREE_WINDINGS_TRANSFORMER'
    GENERATOR = 'GENERATOR'
    LOAD = 'LOAD'
    SHUNT_COMPENSATOR = 'SHUNT_COMPENSATOR'
    DANGLING_LINE = 'DANGLING_LINE'
    STATIC_VAR_COMPENSATOR = 'STATIC_VAR_COMPENSATOR'
    HVDC_CONVERTER_STATION = 'HVDC_CONVERTER_STATION'
