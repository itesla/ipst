/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.domain.util;

import java.util.function.Consumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class Statistics implements Consumer<Object> {

    DescriptiveStatistics stats = new DescriptiveStatistics();

    @Override
    public void accept(Object obj) {
        if (obj != null) {
            if (obj instanceof String) {
                return;
            } else if (obj instanceof Double) {
                stats.addValue((Double) obj);
            } else if (obj instanceof Integer) {
                stats.addValue(((Integer) obj).doubleValue());
            } else if (obj instanceof Long) {
                stats.addValue(((Long) obj).doubleValue());
            }
        }
    }

    public void combine(Statistics other) {
        if (other != null) {
            double[] vals = other.getValues();
            for (int i = 0; i < vals.length; i++) {
                stats.addValue(vals[i]);
            }
        }
    }

    public double[] getValues() {
        return stats.getValues();
    }

    public double avg() {
        return stats.getMean();
    }

    public double var() {
        return stats.getVariance();
    }

    public double percentile(double lim) {
        return stats.getPercentile(lim);
    }

    public long count() {
        return stats.getN();
    }

    public double min() {
        return stats.getMin();
    }

    public double max() {
        return stats.getMax();
    }
}
