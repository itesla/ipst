/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.cta.model.*;
import eu.itesla_project.iidm.network.*;

import java.util.List;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class NetworkModelConverter {

    /**
     * constructor
     */
    public NetworkModelConverter() {
    }

    /**
     * @return Ipso Network State from Iidm Network
     */
    public IpsoNetworkState convert(Network network, ConversionContext context) {

        // create ipso network model
        IpsoNetworkState ipsoNetwork = new IpsoNetworkState(0, context.getCaseName(), context.getMappingBetweenIidmIdAndIpsoEquipment());

        ModelConverter<Bus, IpsoNode> nodeConverter = new NodeModelConverter(context);
        List<IpsoNode> acNodes = nodeConverter.convert(network);

        ModelConverter<DanglingLine, IpsoNode> xNodeConverter = new XnodeModelConverter(context);
        acNodes.addAll(xNodeConverter.convert(network));
        ipsoNetwork.addNodes(acNodes);

        ModelConverter<Line,IpsoLine> lineConverter = new LineModelConverter(context);
        List<IpsoLine> acLines = lineConverter.convert(network);
        ipsoNetwork.addLines(acLines);

        ModelConverter<DanglingLine,IpsoLine> danglingLineConverter = new DanglingLineModelConverter(context);
        ipsoNetwork.addLines(danglingLineConverter.convert(network));

        ModelConverter<Generator, IpsoGenerator> generatorConverter = new GeneratorConverter(context);
        ipsoNetwork.addGenerators(generatorConverter.convert(network));

        ModelConverter<Load, IpsoLoad> loadConverter = new LoadModelConverter(context);
        ipsoNetwork.addLoads(loadConverter.convert(network));

        ModelConverter<DanglingLine, IpsoLoad> xLoadModelConverter = new XloadModelConverter(context);
        ipsoNetwork.addLoads(xLoadModelConverter.convert(network));

        ModelConverter<ShuntCompensator, IpsoBank> bankConverter = new BankModelConverter(context);
        ipsoNetwork.addBanks(bankConverter.convert(network));

        ModelConverter<Switch, IpsoCoupling> couplingConverter = new CouplingDeviceModelConverter(context);
        ipsoNetwork.addCouplings(couplingConverter.convert(network));

        ModelConverter<TwoWindingsTransformer, IpsoTwoWindingsTransformer>
                transformerConverter = new TwoWindingsTransformerModelConverter(context);
        ipsoNetwork.addTwoWindingsTransformers(transformerConverter.convert(network));

        return ipsoNetwork;
    }
}
