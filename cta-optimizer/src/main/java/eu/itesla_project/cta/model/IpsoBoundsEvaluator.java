/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class IpsoBoundsEvaluator {

    public static final float EPSILON_VALUE = 0.0001f;
    protected static final float EPSILON_BOUNDS = 0.00001f;

    /**
     * @return true if the value is out of min and max bounds
     */
    public static boolean isOutOfBounds(float value, float min, float max) {
        checkArgument(!Float.isNaN(min), "min value must not ne NaN");
        checkArgument(!Float.isNaN(max), "max value must not ne NaN");
        if(Float.isNaN(value)) {
            return false;
        }
        else {
            return isConfoundedBounds(min, max) ? isOutOfSetpoint(value, min, max) : isOutsideBounds(value, min, max);
        }
    }

    /**
     * @return true if min is very close to max
     */
    public static boolean isConfoundedBounds(float min, float max) {
        checkArgument(!Float.isNaN(min), "min value must not ne NaN");
        checkArgument(!Float.isNaN(max), "max value must not ne NaN");
        checkArgument(max >= min, "max value must be greater then min value");
        return max-min <= EPSILON_BOUNDS;
    }

    private static boolean isOutOfSetpoint(float value, float min, float max) {
        if(Float.isNaN(value)) {
            return false;
        }
        else {
            return value < (min+max )/2 - EPSILON_VALUE || value > (min+max)/2 + EPSILON_VALUE;
        }
    }

    private static boolean isOutsideBounds(float value, float min, float max) {
        checkArgument(!Float.isNaN(value), "value value must not ne NaN");
        checkArgument(!Float.isNaN(min), "min value must not ne NaN");
        checkArgument(!Float.isNaN(max), "max value must not ne NaN");
        checkArgument(max >= min, "max value must be greater then min value");
        return value < min - EPSILON_VALUE || value > max + EPSILON_VALUE;
    }
    public static boolean isJustOnTheBounds(float value, float min, float max) {
        checkArgument(!Float.isNaN(value), "value value must not ne NaN");
        checkArgument(!Float.isNaN(min), "min value must not ne NaN");
        checkArgument(!Float.isNaN(max), "max value must not ne NaN");
        checkArgument(max >= min, "max value must be greater then min value");
        return value < min + EPSILON_VALUE || value > max - EPSILON_VALUE;
    }
}
