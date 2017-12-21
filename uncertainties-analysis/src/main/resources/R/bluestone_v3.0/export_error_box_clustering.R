##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : export_error_box_clustering.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTestla online module
###   Function      : This file is an example of how to import a dataset, cluster equipments,
###                   compute an error box, disagregate clusters and export results
###   Version       : 3.0
###   Date          : 18/03/2015
##############################################################################################


#Put 0 if you want the "test" mode
if (1) {

#Pre-processing parameters
equip_type=0 # 0 for all equipments, 1 for load only, 2 for load + unknown
load_floor=0.5
gen_floor=0.5

#Clustering parameters
clustering_type=0 # 0 for measure, 1 for errors

thres_nb_complete_obs=250
correlation_floor=0.75
filtered_output=1

#Box parameter
prct_risk=0.8227
nb_PC=150

alpha=qnorm(1-(1-prct_risk)/2)/qnorm(3/4)

#Current directory
setwd("./")

#Input paths
gen_path = "./forecastsDiff_gen.csv"
load_path = "./forecastsDiff_load.csv"

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
rejected_comp="rejected_comp.png"


#Dependencies
source(preprocess_script_path)
source(clustering_script_path)
source(box_script_path)
source(equip_type_script_path)
source(disaggregation_script_path)

# Import and preprocess data_files
data_P=preprocess_data(gen_path,load_path)

data_meas=data_P[data_P$horizon=="SN",!names(data_P)%in%c("datetime","horizon")]

if (equip_type==1){
  all_equip_type=apply(data_meas,2,FUN=typeEquipmentPath,gen_floor=gen_floor,load_floor=load_floor)

  data_P=data_P[,c(names(which(all_equip_type["load",]==1)),"datetime","horizon")]
}else if (equip_type==2){
  all_equip_type=apply(data_meas,2,FUN=typeEquipmentPath,gen_floor=gen_floor,load_floor=load_floor)
  kept_equip=names(which( (all_equip_type["load",]==1) | (all_equip_type["load",]==0 & all_equip_type["gen",]==0)))

  data_P=data_P[,c(kept_equip,"datetime","horizon")]
}


#Cluster equipments
clusters_res=cluster_equipements(data_P,clustering_type,correlation_floor,thres_nb_complete_obs,alpha,filtered_output)
errors_clust=clusters_res[[1]]
errors_equip=clusters_res[[2]]
clust=clusters_res[[3]]
errors_cor=clusters_res[[4]]

# Error boxes
box_res=error_box(errors_clust,prct_risk,errors_cor,nb_PC)

#If some PC cannot move, remove them
#We set the threshold at 1, so that the influence is less than a threshold
indValPC=which((box_res[[2]]-box_res[[1]])>15/nb_PC)

infPC_full = box_res[[1]]
supPC_full = box_res[[2]]

infPC = infPC_full[indValPC]
supPC = supPC_full[indValPC]

matrix_PC_clust=t(box_res$load[,indValPC])
vect_PC_clust=box_res$means

scores=box_res$scores[,indValPC]

# Disaggregation
disaggregation_res=disaggregation(errors_clust,errors_equip,clust)
matrix_clust_equip_fullEquip=as.matrix(disaggregation_res[[1]])
vect_clust_equip_fullEquip=disaggregation_res[[2]]

# Computation of loadings and means from PC to equipments
matrix_PC_equip_fullEquip=matrix_PC_clust%*%matrix_clust_equip_fullEquip
vect_PC_clust_woNaN=vect_PC_clust;
vect_PC_clust_woNaN[is.nan(vect_PC_clust)]=0;
vect_PC_equip_fullEquip=as.vector(vect_clust_equip_fullEquip+vect_PC_clust_woNaN%*%matrix_clust_equip_fullEquip)
names(vect_PC_equip_fullEquip)=names(vect_clust_equip_fullEquip)

#If some equipments cannot move, remove them
#First, compute the maximum move
vMin = vector(length = ncol(matrix_PC_equip_fullEquip))
vMax = vector(length = ncol(matrix_PC_equip_fullEquip))
for (indEquip in 1:ncol(matrix_PC_equip_fullEquip)) {
  vMin[indEquip] = vect_PC_equip_fullEquip[indEquip]
  vMax[indEquip] = vect_PC_equip_fullEquip[indEquip]
  for (indPC in 1:nrow(matrix_PC_equip_fullEquip)) {
    loading = matrix_PC_equip_fullEquip[indPC,indEquip];
    if (!is.na(loading)) {
      if (loading>0){
        vMin[indEquip] = vMin[indEquip] + loading * infPC[[indPC]]
        vMax[indEquip] = vMax[indEquip] + loading * supPC[[indPC]]
      } else {
        vMin[indEquip] = vMin[indEquip] + loading * supPC[[indPC]]
        vMax[indEquip] = vMax[indEquip] + loading * infPC[[indPC]]
      }
    }
  }
}
indValEquip=which(vMax-vMin>1 & !(is.na(vMax-vMin)) )
matrix_PC_equip=matrix_PC_equip_fullEquip[,indValEquip]
#Threshold
matrix_PC_equip[which(abs(matrix_PC_equip)<1E-6)]=0
vect_PC_equip=vect_PC_equip_fullEquip[indValEquip]

matrix_clust_equip=matrix_clust_equip_fullEquip[,indValEquip]
vect_clust_equip=vect_clust_equip_fullEquip[indValEquip]

# Example of how to reconstruct clusters data from PC
# errors_clust_reconstructed=t(apply(box_res$scores%*%matrix_PC_clust,1,function(x) x+vect_PC_clust))

# Example of how to reconstruct equipments data from clusters
# errors_equip_reconstructed1=t(apply(as.matrix(errors_clust_reconstructed)%*%as.matrix(matrix_clust_equip),1,function(x) x+vect_clust_equip))

# Example of how to reconstruct equipments data from PC
# errors_equip3=t(apply(box_res$scores%*%matrix_PC_equip,1,function(x) x+vect_PC_equip))


#Export data

#Clusters
data_temp=data.frame("Equipment"=colnames(errors_equip),"cluster"=rep(0,length(colnames(errors_equip))))
for (i in 1:length(clust)){
  for (j in 1:length(clust[[i]])){
    data_temp[data_temp$Equipment==clust[[i]][j],"cluster"]=i
  }
}
write.table(data_temp, file = cluster_description,  sep = ";", row.names = F, quote = F )

#INF
inf=data.frame(PC=colnames(scores),inf=infPC)
write.table(inf, file = export_box_inf, append = FALSE, quote = FALSE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE, )

#SUP
sup=data.frame(PC=colnames(scores),sup=supPC)
write.table(sup, file = export_box_sup, append = FALSE, quote = FALSE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE, )


#Transition matrices
# PC => clust
loadings=matrix_PC_clust
loadings=cbind(row.names(loadings),loadings)
write.table(loadings, file = export_loadings_PC_clust, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

# clust => equip
loadings=matrix_clust_equip
loadings=cbind(row.names(loadings),loadings)
write.table(loadings, file = export_loadings_clust_equip, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

# PC => equip
loadings=matrix_PC_equip
loadings=cbind(row.names(loadings),loadings)
write.table(loadings, file = export_loadings_PC_equip, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

#Means
# PC => clust
means=t(as.data.frame(vect_PC_clust))
means=cbind(1, means)
write.table(means, file = export_mean_PC_clust, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

# clust => equip
means=t(as.data.frame(vect_clust_equip))
means=cbind(1, means)
write.table(means, file = export_mean_clust_equip, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

# PC => equip
means=t(as.data.frame(vect_PC_equip))
means=cbind(1, means)
write.table(means, file = export_mean_PC_equip, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )


#Min bound by equipment
minEquip=vector(length = ncol(matrix_PC_equip_fullEquip))
minEquip[]=-99999
names(minEquip)=colnames(matrix_PC_equip_fullEquip)
minEquip=t(as.data.frame(minEquip))
minEquip=cbind(1, minEquip)
write.table(minEquip, file = "./borne_min_equip.csv", append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

#Max bound by equipment
maxEquip=vector(length = ncol(matrix_PC_equip_fullEquip))
maxEquip[]=99999
names(maxEquip)=colnames(matrix_PC_equip_fullEquip)
maxEquip=t(as.data.frame(maxEquip))
maxEquip=cbind(1, maxEquip)
write.table(maxEquip, file = "./borne_max_equip.csv", append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )


} else {
	print("Test mode : packaged output files are copied");
}

