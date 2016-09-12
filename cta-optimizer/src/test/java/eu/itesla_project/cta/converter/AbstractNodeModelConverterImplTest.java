/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.converter;

import eu.itesla_project.iidm.network.Bus;
import eu.itesla_project.iidm.network.VoltageLevel;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static java.lang.Float.NaN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class AbstractNodeModelConverterImplTest {

    protected static final float FLOAT_VALUE = 3.3f;
    protected static final float BASE_VOLTAGE_LEVEL = 2f;
    private NodeModelConverter nodeModelConverter;
    private Bus bus;
    private VoltageLevel voltageLevel;

    @Before
    public void setup() {
        ConversionContext conversionContext = new ConversionContext("test");
        nodeModelConverter = new NodeModelConverter(conversionContext);
        voltageLevel = Mockito.mock(VoltageLevel.class);
        bus = Mockito.mock(Bus.class);
    }

    @Test
    public void findBaseVoltageHappyPath() {
        Mockito.when(voltageLevel.getNominalV()).thenReturn(FLOAT_VALUE);
        Mockito.when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        float baseVoltage =  nodeModelConverter.findBaseVoltage(bus);

        assertThat(baseVoltage, is(FLOAT_VALUE));
    }

    @Test
    public void findBaseVoltageReturnsNaNHappyPath() {
        Mockito.when(bus.getVoltageLevel()).thenReturn(null);
        float baseVoltage =  nodeModelConverter.findBaseVoltage(bus);

        assertThat(baseVoltage, is(NaN));
    }

    @Test
    public void findLowVoltageLevelHappyPath() {
        Mockito.when(voltageLevel.getLowVoltageLimit()).thenReturn(FLOAT_VALUE);
        Mockito.when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        float lowVoltageLevel =  nodeModelConverter.findLowVoltageLevel(bus, BASE_VOLTAGE_LEVEL);

        assertThat(lowVoltageLevel, is(FLOAT_VALUE/BASE_VOLTAGE_LEVEL));
    }

    @Test
    public void findLowVoltageLevelReturnsDefaultLowLimit() {
        Mockito.when(voltageLevel.getLowVoltageLimit()).thenReturn(NaN);
        float lowVoltageLevel =  nodeModelConverter.findLowVoltageLevel(bus, BASE_VOLTAGE_LEVEL);

        assertThat(lowVoltageLevel, CoreMatchers.is(AbstractNodeModelConverter.DEFAULT_LOW_VOLTAGE_LEVEL));
    }

    @Test
    public void findHighVoltageLevelReturnsDefaultHighLimit() {
        Mockito.when(voltageLevel.getHighVoltageLimit()).thenReturn(FLOAT_VALUE);
        Mockito.when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        float highVoltageLevel =  nodeModelConverter.findHighVoltageLevel(bus, NaN);

        assertThat(highVoltageLevel, CoreMatchers.is(AbstractNodeModelConverter.DEFAULT_HIGH_VOLTAGE_LEVEL));
    }

}
