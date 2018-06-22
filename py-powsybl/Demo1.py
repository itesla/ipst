# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
#   file, You can obtain one at http://mozilla.org/MPL/2.0/.

# @author Christian Biasuzzi <christian.biasuzzi@techrain.it>


from py4j.java_gateway import JavaGateway, GatewayParameters
from py4j.java_gateway import java_import
from py4j.java_gateway import JavaPackage

#connect to the JVM (running iPST/PowSyBl)
gateway = JavaGateway(gateway_parameters = GatewayParameters(auto_field=True))

#other boilerplate imports
java_import(gateway.jvm,'java.nio.file.*')
java_import(gateway.jvm,'java.io.*')

java_import(gateway.jvm,'com.powsybl.commons.config.ComponentDefaultConfig')
java_import(gateway.jvm,'com.powsybl.computation.local.LocalComputationManager')
java_import(gateway.jvm,'com.powsybl.loadflow.LoadFlowFactory')
java_import(gateway.jvm,'com.powsybl.iidm.import_.Importers')
java_import(gateway.jvm,'com.powsybl.iidm.export.Exporters')
java_import(gateway.jvm,'com.powsybl.iidm.network.test.FictitiousSwitchFactory')

#needed below to resolve some class-factories, by name
ReflectionUtil = gateway.jvm.py4j.reflection.ReflectionUtil

#simple dump network flows function
def dumpLinesFlow(network):
    print("\nFlow on lines for network: " + network.getId())
    lines = network.getLines().toList()
    print("line id;terminal1.I;terminal2.I")
    for line in lines:
      print(line.getId()+";" + str(line.getTerminal1().getI()) + ";" + str(line.getTerminal2().getI()))

#load platform config
defaultConfig = gateway.jvm.ComponentDefaultConfig.load()

#instantiate a computation manager and a LF factory
computationManager = gateway.jvm.LocalComputationManager()
loadflowfactory=defaultConfig.newFactoryImpl(ReflectionUtil.classForName("com.powsybl.loadflow.LoadFlowFactory"))

#create a demo network
network = gateway.jvm.com.powsybl.iidm.network.test.FictitiousSwitchFactory.create()

#instantiate a LF
loadFlow = loadflowfactory.create(network, computationManager, 0)

#dump network's lines flow
dumpLinesFlow(network)

#run a LF on the network and dump its results metrics
loadflowResult = loadFlow.run()
print("\nLF result: " + str(loadflowResult.isOk()) + "; metrics: " + str(loadflowResult.getMetrics()))

#dump network's lines flow
dumpLinesFlow(network)


#change the network: open a switch
network.getSwitch("BD").setOpen(True)

#re-run a LF on the network and dump its results metrics
loadflowResult = loadFlow.run()
print("\nLF result: " + str(loadflowResult.isOk()) + "; metrics: " + str(loadflowResult.getMetrics()))

#dump network's lines flow
dumpLinesFlow(network)

#export this network to a file, in xiidm format
gateway.jvm.Exporters.export("XIIDM", network, None,  gateway.jvm.File('/tmp/export1').toPath())