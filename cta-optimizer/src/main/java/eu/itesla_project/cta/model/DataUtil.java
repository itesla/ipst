/** Copyright (c) 2016, Tractebel (http://www.tractebel-engie.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 **/
package eu.itesla_project.cta.model;

/**
 * @author Yannick Pihan <yannick.pihan at tractebel.engie.com>
 */
public class DataUtil {

    /**
     * Give value without NaN
     * @return the value or 0.f if the value is NaN
     */
    public static float getSafeValueOf(float value) {
        return Float.isNaN(value) ? 0f : value;
    }

    public static Object replaceNanByEmpty(float value) {
        return Float.isNaN(value) ? "" : value;
    }

    /**
     * Give value without NaN
     * @return the value or the default value iff the value is NaN
     */
    public static float getSafeValueOf(float value, float defaultValue) {
        return Float.isNaN(value) ? defaultValue : value;
    }

    /**
     * Give value without NaN
     * @return the value or 0.f if the value is NaN
     */
    public static double getSafeValueOf(double value) {
        return Double.isNaN(value) ? 0f : value;
    }

    /**
     * Give value without NaN
     * @return the value or the default value iff the value is NaN
     */
    public static double getSafeValueOf(double value, double defaultValue) {
        return Double.isNaN(value) ? 0f : defaultValue;
    }
}
