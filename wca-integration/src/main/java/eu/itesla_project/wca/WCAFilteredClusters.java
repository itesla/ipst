/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.wca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.itesla_project.modules.wca.WCAClusterNum;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.it>
 */
public class WCAFilteredClusters {

    private static final Logger LOGGER = LoggerFactory.getLogger(WCAFilteredClusters.class);

    private final String networkId;
    private Map<String, Collection<WCAClusterNum>> contingencyClusters = new ConcurrentHashMap<String, Collection<WCAClusterNum>>();
    private Map<String, Collection<WCAClusterOrigin>> contingencyFlags = new ConcurrentHashMap<String, Collection<WCAClusterOrigin>>();

    public WCAFilteredClusters(String networkId, Collection<String> contingenciesIds) {
        Objects.requireNonNull(networkId,"network id is null");
        Objects.requireNonNull(contingenciesIds,"contigencies list is null");
        this.networkId = networkId;
        contingenciesIds.forEach(contingencyId -> contingencyClusters.put(contingencyId, 
                new ArrayList<WCAClusterNum>(Arrays.asList(WCAClusterNum.ONE, WCAClusterNum.TWO, WCAClusterNum.THREE, WCAClusterNum.FOUR))));
    }

    public void removeClusters(String contingencyId, WCAClusterNum[] clustersNums, WCAClusterOrigin flag) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        Objects.requireNonNull(clustersNums,"clustersNums is null");
        LOGGER.info("Network {}, contingency {}: removing clusters {} for {}", networkId, contingencyId, Arrays.toString(clustersNums), flag);
        if ( contingencyClusters.containsKey(contingencyId) ) {
            // remove clusters from the list of the contingency
            Collection<WCAClusterNum> clusters = contingencyClusters.get(contingencyId);
            for (int i = 0; i < clustersNums.length; i++) {
                if ( clusters.contains(clustersNums[i]) ) {
                    clusters.remove(clustersNums[i]);
                }
            }
            contingencyClusters.put(contingencyId, clusters);
            if ( flag != null ) {
                // add flag to the list of the contingency
                Collection<WCAClusterOrigin> flags = new ArrayList<WCAClusterOrigin>();
                if ( contingencyFlags.containsKey(contingencyId) ) {
                    flags = contingencyFlags.get(contingencyId);
                }
                if ( !flags.contains(flag) ) {
                    flags.add(flag);
                }
                contingencyFlags.put(contingencyId, flags);
            }
        } else {
            LOGGER.warn("Network {}, contingency {}: no possible clusters", networkId, contingencyId);
        }
    }

    public void addClusters(String contingencyId, WCAClusterNum[] clustersNums, WCAClusterOrigin flag) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        Objects.requireNonNull(clustersNums,"clustersNums is null");
        LOGGER.info("Network {}, contingency {}: adding clusters {} for {}", networkId, contingencyId, Arrays.toString(clustersNums), flag);
        if ( contingencyClusters.containsKey(contingencyId) ) {
            // add clusters to the list of the contingency
            Collection<WCAClusterNum> clusters = contingencyClusters.get(contingencyId);
            for (int i = 0; i < clustersNums.length; i++) {
                if ( !clusters.contains(clustersNums[i]) ) {
                    clusters.add(clustersNums[i]);
                }
            }
            contingencyClusters.put(contingencyId, clusters);
            if ( flag != null ) {
                // add flag to the list of the contingency
                Collection<WCAClusterOrigin> flags = new ArrayList<WCAClusterOrigin>();
                if ( contingencyFlags.containsKey(contingencyId) ) {
                    flags = contingencyFlags.get(contingencyId);
                }
                if ( !flags.contains(flag) ) {
                    flags.add(flag);
                }
                contingencyFlags.put(contingencyId, flags);
            }
        } else {
            LOGGER.warn("Network {}, contingency {}: no possible clusters", networkId, contingencyId);
        }
    }

    public boolean hasCluster(String contingencyId, WCAClusterNum clustersNum) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        Objects.requireNonNull(clustersNum,"clustersNum is null");
        LOGGER.warn("Network {}, contingency {}: checking if {} is included in the possible clusters", networkId, contingencyId, clustersNum);
        return contingencyClusters.containsKey(contingencyId) && contingencyClusters.get(contingencyId).contains(clustersNum);
    }

    public Collection<WCAClusterNum> getClusters(String contingencyId) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        LOGGER.warn("Network {}, contingency {}: getting clusters", networkId, contingencyId);
        if ( contingencyClusters.containsKey(contingencyId) ) {
            return contingencyClusters.get(contingencyId);
        }
        LOGGER.warn("Network {}, contingency {}: no possible clusters", networkId, contingencyId);
        return Collections.emptyList();
    }

    public WCAClusterNum getCluster(String contingencyId) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        LOGGER.warn("Network {}, contingency {}: getting cluster", networkId, contingencyId);
        if ( contingencyClusters.containsKey(contingencyId) && !contingencyClusters.get(contingencyId).isEmpty() ) {
            return contingencyClusters.get(contingencyId).stream().max(Comparator.naturalOrder()).get();
        }
        LOGGER.warn("Network {}, contingency {}: no possible clusters", networkId, contingencyId);
        return WCAClusterNum.UNDEFINED;
    }

    public Collection<WCAClusterOrigin> getFlags(String contingencyId) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        if ( contingencyFlags.containsKey(contingencyId) ) {
            return contingencyFlags.get(contingencyId);
        }
        LOGGER.warn("Network {}, contingency {}: no available flags", networkId, contingencyId);
        return Collections.emptyList();
    }
    
    public void removeAllButCluster(String contingencyId, WCAClusterNum clusterNum, WCAClusterOrigin flag) {
        Objects.requireNonNull(contingencyId,"contingency id is null");
        Objects.requireNonNull(clusterNum,"clustersNums is null");
        LOGGER.info("Network {}, contigency {}: removing all clusters but {} for {}", networkId, contingencyId, clusterNum, flag);
        if ( contingencyClusters.containsKey(contingencyId) ) {
            if ( contingencyClusters.get(contingencyId).contains(clusterNum) ) {
                contingencyClusters.put(contingencyId, new ArrayList<WCAClusterNum>(Collections.singleton(clusterNum)));
                if ( flag != null ) {
                    // add flag to the list of the contingency
                    Collection<WCAClusterOrigin> flags = new ArrayList<WCAClusterOrigin>();
                    if ( contingencyFlags.containsKey(contingencyId) )
                        flags = contingencyFlags.get(contingencyId);
                    if ( !flags.contains(flag) ) {
                        flags.add(flag);
                    }
                    contingencyFlags.put(contingencyId, flags);
                }
            } else {
                LOGGER.warn("Network {}, contingency {}: cluster {} not included in the possible clusters", networkId, contingencyId, clusterNum);
            }
        } else {
            LOGGER.warn("Network {}, contingency {}: no possible clusters", networkId, contingencyId);
        }
    }

}
