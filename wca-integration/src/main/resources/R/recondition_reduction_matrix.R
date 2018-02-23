##############################################################################################
###   Copyright (c) 2017, 2018, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###

##############################################################################################
###   Program name  : recondition_reduction_matrix.R                                        ##
###   Author        : Eric Bourgade                                                         ##
###   Project       : RTE prototype for the iTesla online module                            ##
###   Function      : reconditioning of reconstruction matrix after errors calculation      ##
###   Inputs        : txt files produced by the error calculation                           ##
###      ./wca_reduction_matrix.txt                                                         ##
###      ./wca_trust_intervals.txt                                                          ##
###      ./wca_histo_generators.txt                                                         ##
###      ./wca_histo_loads.txt                                                              ##
###      ./wca_0_network_loads.txt                                                          ##
###   Outputs       :                                                                       ##
###      ./wca_reduction_matrix.txt                                                         ##
###        (input file renamed to "./wca_reduction_matrix_not_reconditioned.txt")           ##
###   Version       : 3                                                                     ##
###   Date          : 18/10/2017                                                            ##
##############################################################################################


## folder 
## setwd("V:/iTesla/Dossiers personnels/Eric/itesla_wca_clusters_3965401218891112628")

library("data.table")


############################################################################# 
## import de la matrice écrite en txt                                      ##
## import de la correspondance numéros de loads nom des loads              ##
## import des sup et inf des composantes principales                       ##
## import de la prévision                                                  ##
#############################################################################


##matrice en fichier texte ##

matrice_path = "./wca_reduction_matrix.txt"

matrice_txt_R<-read.table (matrice_path, header = FALSE , sep="" ) 

##correspondance numéros de loads type de load##

int_cor<-matrice_txt_R[,-c(3,4)]

doublonsmatrice_txt_R<-which(duplicated(int_cor$V2))

matrice_txt_R_2<-int_cor[-doublonsmatrice_txt_R,]

##sup et inf des composantes principales

supetinfCP_path = "./wca_trust_intervals.txt"

supetinfCP_txt_R<-read.table (supetinfCP_path, header = FALSE , sep="" ) 

rcl_sup_PCA<-supetinfCP_txt_R[,-c(2)]

rcl_inf_PCA<-supetinfCP_txt_R[,-c(3)]

##sup et inf des équipements

## generators
supetinf_generators_path = "./wca_histo_generators.txt"

supetinf_generators_txt_R<-read.table (supetinf_generators_path, header = FALSE , sep="" )

rcl_sup_equip_generators<-supetinf_generators_txt_R[,-c(2)]

rcl_inf_equip_generators<-supetinf_generators_txt_R[,-c(3)]


## loads
supetinf_loads_path = "./wca_histo_loads.txt"

supetinf_loads_txt_R<-read.table (supetinf_loads_path, header = FALSE , sep="" ) 

rcl_sup_equip_loads<-supetinf_loads_txt_R[,-c(2)]

rcl_inf_equip_loads<-supetinf_loads_txt_R[,-c(3)]


############################################################################
## remise en forme de la matrice                                          ##
############################################################################


d.f <- data.frame(matrice_txt_R)

d.f<-d.f[,-1]

d.f<-d.f[,c(2,1,3)]

mat <- matrix( 0, nrow=length( unique( d.f$V3)), ncol=length( unique( d.f$V2)), dimnames=list( unique( d.f$V3), unique( d.f$V2)))

colnames(mat)<-as.character(colnames(mat))

rownames(mat)<-as.character(rownames(mat))

d.f[,1]<-as.character(d.f[,1])

d.f[,2]<-as.character(d.f[,2])

mat[as.matrix(d.f[,c(1,2)])] <- d.f$V4

cor_matrice_txt_R<-mat


############################################################################
##remise en forme des bornes-inf et sup des équipements                    #
############################################################################

nrow<-1
ncol<-ncol(cor_matrice_txt_R)
n_data<-nrow*ncol

horizon_rcl_inf_equip<-matrix(rep(0,n_data),nrow,ncol)
horizon_rcl_sup_equip<-matrix(rep(0,n_data),nrow,ncol)

colnames(horizon_rcl_inf_equip)<-colnames(cor_matrice_txt_R)
colnames(horizon_rcl_sup_equip)<-colnames(cor_matrice_txt_R)

names(horizon_rcl_inf_equip)<-colnames(cor_matrice_txt_R)
names(horizon_rcl_sup_equip)<-colnames(cor_matrice_txt_R)

for (i in 1:ncol(cor_matrice_txt_R)){
  horizon_rcl_inf_equip[1,i]<-rcl_inf_equip_loads[as.numeric(names(horizon_rcl_inf_equip[1,i])),2]}

for (i in 1:ncol(cor_matrice_txt_R)){
  horizon_rcl_sup_equip[1,i]<-rcl_inf_equip_loads[as.numeric(names(horizon_rcl_sup_equip[1,i])),2]}

############################################################################
## remise en forme des bornes-inf et sup des variables réduites           ##
############################################################################

rcl_inf_PCA<-rcl_inf_PCA[,-c(1)]
rcl_sup_PCA<-rcl_sup_PCA[,-c(1)]

rcl_inf_PCA<-t(rcl_inf_PCA)
rcl_sup_PCA<-t(rcl_sup_PCA)

############################################################################
## prévision : on prend en test un snapshot quelconque                    ##
############################################################################

snapshot_path="./wca_0_network_loads.txt"

snapshot_R<-read.table (snapshot_path, header = FALSE , sep="" ) 

prevision_R<-horizon_rcl_sup_equip

for (i in 1:ncol(horizon_rcl_sup_equip)){
  prevision_R[1,i]<-snapshot_R[as.numeric(names(horizon_rcl_sup_equip[1,i])),4]
}

##############################################################################################
## function  :  reconditioning of matrix 
##############################################################################################



modification_matrice<-function(matrix_PC_equip,sup_PCA,inf_PCA,sup_equip,inf_equip,val_prev){
  
  
  
  cor_matrix_PC_equip<-matrix_PC_equip
  cij_erreur_max_reconstituee<-matrix_PC_equip
  cij_erreur_min_reconstituee<-matrix_PC_equip
  
  ##############################################################################################
  ###  erreurs maximale et minimale reconstituées pour chaque composant de base j
  ##############################################################################################
  
  
  for (i in 1:nrow(matrix_PC_equip)){
    for (j in 1:ncol(matrix_PC_equip)){
      
      
      if (sign(matrix_PC_equip[i,j])>0){
        
        cij_erreur_max_reconstituee[i,j]<-matrix_PC_equip[i,j]*sup_PCA[,i]
        cij_erreur_min_reconstituee[i,j]<-matrix_PC_equip[i,j]*(-inf_PCA[,i])}
      else {
        cij_erreur_max_reconstituee[i,j]<-matrix_PC_equip[i,j]*inf_PCA[,i]
        cij_erreur_min_reconstituee[i,j]<-matrix_PC_equip[i,j]*(-sup_PCA[,i])}
      
      
    }
  }
  
  erreur_max_reconstituee<-colSums(cij_erreur_max_reconstituee)
  
  
  erreur_min_reconstituee<-colSums(cij_erreur_min_reconstituee)
  
  
  ##############################################################################################
  ###  modification des matrix_PC_equip[i,j] en fonction de la valeur prévue pour les variables
  ###  de base
  ##############################################################################################
  
  ####  valeur prévue :  val_prev
  
  diff_inf<-inf_equip-val_prev
  diff_sup<-sup_equip-val_prev
  
  for (i in 1:nrow(matrix_PC_equip)){
    for (j in 1:ncol(matrix_PC_equip)){
      cor_matrix_PC_equip[i,j]<-matrix_PC_equip[i,j]*min(max(0,diff_inf[1,j]/erreur_min_reconstituee[j]),
                                                         min(0,diff_sup[1,j]/erreur_max_reconstituee[j]),1)
    }
  }         
  
  
  
  return(cor_matrix_PC_equip)
}




###############################################################################################
## matrix reconditioning and export
###############################################################################################

cor_matrix_PC_equip=modification_matrice(cor_matrice_txt_R,rcl_sup_PCA,rcl_inf_PCA,horizon_rcl_sup_equip,horizon_rcl_inf_equip,prevision_R)

cor_matrix_PC_equip<-as.matrix(cor_matrix_PC_equip)

colnames(cor_matrix_PC_equip)<-colnames(cor_matrice_txt_R)

var_names<-as.vector(colnames(cor_matrice_txt_R))

nrow<-nrow(cor_matrix_PC_equip)*ncol(cor_matrix_PC_equip)
ncol<-4
n_data<-nrow*ncol

bis_matrice_txt_R<-matrix(rep(0,n_data),nrow,ncol)


for (i in 1:ncol(cor_matrix_PC_equip))
  
{
  for (j in 1:nrow(cor_matrix_PC_equip))
  {
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 2]<-var_names[i]
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 3]<-j
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 4]<-cor_matrix_PC_equip[j,i]
  }
  
}

bis_matrice_txt_R[,1]<-as.character(bis_matrice_txt_R[,1])

bis_matrice_txt_R <- merge(bis_matrice_txt_R,matrice_txt_R_2,by="V2",sort=FALSE)

bis_matrice_txt_R<-bis_matrice_txt_R[,-2]

bis_matrice_txt_R<-bis_matrice_txt_R[,c(4,1,2,3)]

colnames(bis_matrice_txt_R)<-c("inj. type","inj. num","var. num","coeff.")


##export of reconditioned matrix
##Output path
##export_cor_matrix_PC_equip="./cor_matrice.txt"

## backup the input matrice_path file to ./wca_reduction_matrix_not_reconditioned.txt
matrice_not_reconditioned_path = "./wca_reduction_matrix_not_reconditioned.txt"
file.rename(matrice_path, matrice_not_reconditioned_path)

## dump the reconditioned matrix, overwriting matrice_path file
cat("#Reconditioned reduction matrix\n#\"inj. type\" \"inj. num\" \"var. num\" \"coeff.\"\n",file=matrice_path)
write.table(bis_matrice_txt_R, file = matrice_path, append = TRUE, quote = FALSE, sep = " ", eol = "\n", na = "NaN", dec = ".", row.names = FALSE, col.names = FALSE )
