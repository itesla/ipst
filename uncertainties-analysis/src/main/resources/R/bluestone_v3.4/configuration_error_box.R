##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : configuration_error_box.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTestla online module
###   Function      : This file is a configuration file for the export_error_box_clustering script
###   Version       : 3.4
###   Date          : 16/04/2015
##############################################################################################


#Pre-processing parameters
zero_floor=1e-7
equip_type=0 # 0 for all equipments, 1 for load only, 2 for load + unknown
load_floor=0.5
gen_floor=0.5

#Clustering parameters
clustering_type=1 # 0 for measure, 1 for errors

thres_rate_complete_obs=0.1
correlation_floor=0.75

#Box parameter
prct_risk=0.8227
rate_PC=0.1
filtered_box=1 # 0 for calcultation of the box on non filtered data, 1 for filtered data

alpha=qnorm(1-(1-prct_risk)/2)/qnorm(3/4)

#Disaggregation parameter
filtered_disaggregation=1 # 0 for disaggregation on non filtered data, 1 for filtered data

#Input paths
gen_path = "./forecastsDiff_gen.csv"
load_path = "./forecastsDiff_load.csv"

gen_sn_path = "./snapshots_gen.csv"
load_sn_path = "./snapshots_load.csv"

#These files contains only the measures, but for more snapshots
#(typically, one every 15 mn instead of one every hour for the previous files)
gen_measure_path = "./snapshot_generators.csv"
load_measures_path = "./snapshot_loads.csv"

#Script paths
preprocess_script_path="./preprocess_data_error_box.R"
equip_type_script_path="./type_equipment.R"
clustering_script_path="./clustering_correlation.R"
box_script_path="./error_box.R"
disaggregation_script_path="./disaggregation.R"

#Output paths
cluster_description="cluster_description.csv"
export_box_inf="bornes_inf.csv"
export_box_sup="bornes_sup.csv"
export_loadings_PC_clust="loadings_PC_clust.csv"
export_mean_PC_clust="mean_PC_clust.csv"
export_loadings_clust_equip="loadings_clust_equip.csv"
export_mean_clust_equip="mean_clust_equip.csv"
export_loadings_PC_equip="matrice.csv"
export_mean_PC_equip="vecteur.csv"
export_equip_inf="borne_min_equip.csv"
export_equip_sup="borne_max_equip.csv"
rejected_comp="rejected_comp.png"