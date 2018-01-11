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
###   Version       : 3.4
###   Date          : 16/04/2015
##############################################################################################


#Current directory
#setwd("/home/rte/")
#setwd("D:\\Users\\nicoomon\\Documents\\itesla\\bluestone\\Error_box_scripts_v3.4")

#Configuration file path
config_file_path="./configuration_error_box.R"

#Dependencies
source(config_file_path)
source(preprocess_script_path)
source(clustering_script_path)
source(box_script_path)
source(equip_type_script_path)
source(disaggregation_script_path)

debug=0;

# Import and preprocess data_files
print("Importing data...")
zero_time=proc.time()
data_P=preprocess_data(gen_path,load_path,zero_floor)
print("Import completed. Elapsed time :")
print(proc.time()-zero_time)


data_meas=data_P[data_P$horizon=="SN",!names(data_P)%in%c("datetime","horizon")]

if (equip_type==1){
  all_equip_type=apply(data_meas,2,FUN=typeEquipmentPath,gen_floor=gen_floor,load_floor=load_floor)

  data_P=data_P[,c(names(which(all_equip_type["load",]==1)),"datetime","horizon")]
}else if (equip_type==2){
  all_equip_type=apply(data_meas,2,FUN=typeEquipmentPath,gen_floor=gen_floor,load_floor=load_floor)
  kept_equip=names(which( (all_equip_type["load",]==1) | (all_equip_type["load",]==0 & all_equip_type["gen",]==0)))

  data_P=data_P[,c(kept_equip,"datetime","horizon")]
}
if (debug==1) {
  save.image('sauve_A_TEST')
}

#Import snapshots in order to compute historical bounds
data_sn_gen<-as.data.frame(fread(gen_sn_path,sep=",",header=TRUE,stringsAsFactors=F))
data_sn_load<-as.data.frame(fread(load_sn_path,sep=",",header=TRUE,stringsAsFactors=F))

#Compute historical bounds
#Col 1 : date, col 2, 0, col 3 : SN
quantiles_sn_gen =apply(data_sn_gen [,4:dim(data_sn_gen)[2]] ,2,quantile, c(0.01, 1-0.01), names=FALSE, na.rm=TRUE)
quantiles_sn_load=apply(data_sn_load[,4:dim(data_sn_load)[2]],2,quantile, c(0.01, 1-0.01), names=FALSE, na.rm=TRUE)

#Cluster equipments
print("Clustering equipments...")
zero_time=proc.time()
clusters_res=cluster_equipements(data_P,clustering_type,correlation_floor,thres_rate_complete_obs,alpha)
#save(clusters_res,"clusters_res_TEST")
#clusters_res=cluster_equipementsTEST(data_P,clustering_type,correlation_floor,thres_rate_complete_obs,alpha)
print("Clustering completed. Elapsed time :")
print(proc.time()-zero_time)

if (debug==1) {
  save.image('sauve_B_TEST')
}
#load('sauve_B_TEST')

#Filter clusters with only NAs
if (filtered_box+filtered_disaggregation >0){
  ind_kept_clust=colSums(is.na(clusters_res[[3]]))!=nrow(clusters_res[[3]])

  if( sum(ind_kept_clust)!=ncol(clusters_res[[3]])){
    print("Clusters with only NaNs are removed from the analysis.")
    print("The following clusters are filtered out:")
    print(clusters_res[[5]][!ind_kept_clust])
  clusters_res[[3]]=clusters_res[[3]][,ind_kept_clust]
  clusters_res[[5]]=clusters_res[[5]][ind_kept_clust]
  clusters_res[[4]]=clusters_res[[4]][,colnames(clusters_res[[4]]) %in% unlist(clusters_res[[5]])]
  clusters_res[[6]]=clusters_res[[6]][ind_kept_clust,ind_kept_clust]
  }
}
errors_clust=clusters_res[[3]]
errors_equip=clusters_res[[4]]
clust=clusters_res[[5]]
errors_cor=clusters_res[[6]]
nb_PC=floor(rate_PC*ncol(errors_clust))+1


# Error boxes
print("Error box calculation...")
zero_time=proc.time()
box_res=error_box(clusters_res[[1+2*filtered_box]],prct_risk,errors_cor,nb_PC)
print("Error box calculated. Elapsed time :")
print(proc.time()-zero_time)

nb_PC=box_res[[6]]

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
print("Disaggregating...")
zero_time=proc.time()
if (debug==1) {
  save.image('sauve_C_TEST')
}
disaggregation_res=disaggregation(clusters_res[[1+2*filtered_disaggregation]],clusters_res[[2+2*filtered_disaggregation]],clust)
matrix_clust_equip_fullEquip=as.matrix(disaggregation_res[[1]])
vect_clust_equip_fullEquip=disaggregation_res[[2]]
print("Disaggregation completed. Elapsed time :")
print(proc.time()-zero_time)

# Computation of loadings and means from PC to equipments
matrix_PC_equip_fullEquip=matrix_PC_clust%*%matrix_clust_equip_fullEquip
vect_PC_clust_woNaN=vect_PC_clust
vect_PC_clust_woNaN[is.nan(vect_PC_clust)]=0
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
print("Exporting results...")
zero_time=proc.time()

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


#Bound by equipment
equip_bound=rbind(supPC%*%matrix_PC_equip+vect_PC_equip,infPC%*%matrix_PC_equip+vect_PC_equip)

#Min bound by equipment
minEquip = c(quantiles_sn_gen[1,],quantiles_sn_load[1,]);
names(minEquip)=c(colnames(quantiles_sn_gen), colnames(quantiles_sn_load) )
minEquip=t(as.data.frame(minEquip))
minEquip=cbind(1, minEquip)
write.table( minEquip, file = export_equip_inf, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )

#Max bound by equipment
maxEquip = c(quantiles_sn_gen[2,],quantiles_sn_load[2,]);
names(maxEquip)=c(colnames(quantiles_sn_gen), colnames(quantiles_sn_load) )
maxEquip=t(as.data.frame(maxEquip))
maxEquip=cbind(1, maxEquip)
write.table( maxEquip, file = export_equip_sup, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NaN", dec = ".", row.names = FALSE,
            col.names = TRUE )


#Rejected components
scores=box_res[[5]]
q_sup=box_res[[2]]
q_inf=box_res[[1]]
rejected_components=colSums(apply(scores,1,function(x) x>q_sup | x<q_inf))/ncol(scores)*100
names(rejected_components)=data_P[seq(1,nrow(data_P),2),"datetime"]

png(rejected_comp)
plot(rejected_components,cex=0.1,ylab="% rejected components",xlab="",main="Proportion of rejected components\nper observation")
abline(h=quantile(rejected_components,0.95),col="red")
dev.off()

print("Results exported. Elapsed time :")
print(proc.time()-zero_time)