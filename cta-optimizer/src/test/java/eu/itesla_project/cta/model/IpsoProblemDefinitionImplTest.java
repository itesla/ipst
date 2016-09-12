/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoProblemDefinitionImplTest {

    private IpsoProblemDefinition ipsoProblemDefinition;

    @Before
    public void setup() {

        ipsoProblemDefinition = new IpsoProblemDefinition(
                "test", 0,
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList());
    }

    @Test
    public void findSubTypeOfHappyPath() {

        IpsoGenerator generator = Mockito.mock(IpsoGenerator.class);
        IpsoTwoWindingsTransformer transformer = Mockito.mock(IpsoTwoWindingsTransformer.class);

        IpsoConstraintGeneratorQBounds constraintGeneratorQBounds1 = new IpsoConstraintGeneratorQBounds(generator, 0, 10, 0);
        IpsoConstraintGeneratorQBounds constraintGeneratorQBounds2 = new IpsoConstraintGeneratorQBounds(generator, 0, 11, 0);
        IpsoConstraintGeneratorPBounds constraintGeneratorPBounds1 = new IpsoConstraintGeneratorPBounds(generator, 0, 12, 0);
        IpsoConstraintGeneratorPBounds constraintGeneratorPBounds2 = new IpsoConstraintGeneratorPBounds(generator, 0, 13, 0);
        IpsoConstraint2WTransformerTapBounds constraint2WTransformerTapBounds1 = new IpsoConstraint2WTransformerTapBounds(transformer, 0,13,0);
        IpsoConstraint2WTransformerTapBounds constraint2WTransformerTapBounds2 = new IpsoConstraint2WTransformerTapBounds(transformer, 0,13,0);
        IpsoConstraint2WTransformerTapBounds constraint2WTransformerTapBounds3 = new IpsoConstraint2WTransformerTapBounds(transformer, 0,13,0);

        List<IpsoConstraint> constraints = Lists.newArrayList(
                constraintGeneratorQBounds1,
                constraintGeneratorQBounds2,
                constraintGeneratorPBounds1,
                constraintGeneratorPBounds2,
                constraint2WTransformerTapBounds1,
                constraint2WTransformerTapBounds2,
                constraint2WTransformerTapBounds3);

        List<IpsoConstraint2WTransformerTapBounds> expectedConstraints = ipsoProblemDefinition.findSubTypeOf(IpsoConstraint2WTransformerTapBounds.class, constraints);
        assertThat(expectedConstraints.size(), is(3));
    }

}
