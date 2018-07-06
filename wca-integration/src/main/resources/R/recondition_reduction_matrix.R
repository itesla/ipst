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

library(stringr)
############################################################################# 
## import de la matrice écrite en txt                                      ##
## import de la correspondance numéros de loads nom des loads              ##
## import des sup et inf des composantes principales                       ##
## import de la prévision                                                  ##
#############################################################################


##matrice en fichier texte ##

matrice_path = "./wca_reduction_matrix.txt"

matrice_txt_R<-read.table (matrice_path, header = FALSE , sep="" ) 

##variables de base##

##sup et inf des composantes principales

supetinfCP_path = "./wca_trust_intervals.txt"

supetinfCP_txt_R<-read.table (supetinfCP_path, header = FALSE , sep="" ) 

rcl_sup_PCA<-supetinfCP_txt_R[,-c(2)]

rcl_inf_PCA<-supetinfCP_txt_R[,-c(3)]



##sup et inf des équipements

## generators
supetinf_generators_path = "./wca_histo_generators.txt"

supetinf_generators_txt_R<-read.table (supetinf_generators_path, header = FALSE , sep="" )

vec_G<-rep(c("G"),nrow(supetinf_generators_txt_R))

rcl_sup_equip_generators<-supetinf_generators_txt_R[,-c(2)]

rcl_inf_equip_generators<-supetinf_generators_txt_R[,-c(3)]

rcl_sup_equip_generators<-cbind(vec_G,rcl_sup_equip_generators)

rcl_inf_equip_generators<-cbind(vec_G,rcl_inf_equip_generators)

## loads
supetinf_loads_path = "./wca_histo_loads.txt"

supetinf_loads_txt_R<-read.table (supetinf_loads_path, header = FALSE , sep="" ) 

vec_L<-rep(c("L"),nrow(supetinf_loads_txt_R))

rcl_sup_equip_loads<-supetinf_loads_txt_R[,-c(2)]

rcl_inf_equip_loads<-supetinf_loads_txt_R[,-c(3)]

rcl_sup_equip_loads<-cbind(vec_L,rcl_sup_equip_loads)

rcl_inf_equip_loads<-cbind(vec_L,rcl_inf_equip_loads)


## renommage des variables

colnames(rcl_sup_equip_generators)<-c("V1","V2","V3")

colnames(rcl_inf_equip_generators)<-c("V1","V2","V3")

colnames(rcl_sup_equip_loads)<-c("V1","V2","V3")

colnames(rcl_inf_equip_loads)<-c("V1","V2","V3")




## tous equipements

rcl_sup_equip<-rbind(rcl_sup_equip_loads,rcl_sup_equip_generators)

rcl_inf_equip<-rbind(rcl_inf_equip_loads,rcl_inf_equip_generators)

paste_rcl_sup_equip <- with(rcl_sup_equip, paste(V1,V2, sep = "_"))

paste_rcl_sup_equip<-data.frame(paste_rcl_sup_equip)

rcl_sup_equip<-cbind(paste_rcl_sup_equip,rcl_sup_equip)

rcl_sup_equip<-as.matrix(rcl_sup_equip)

paste_rcl_inf_equip <- with(rcl_inf_equip, paste(V1,V2, sep = "_"))

paste_rcl_inf_equip<-data.frame(paste_rcl_inf_equip)

rcl_inf_equip<-cbind(paste_rcl_inf_equip,rcl_inf_equip)

rcl_inf_equip<-as.matrix(rcl_inf_equip)



############################################################################
## remise en forme de la matrice                                          ##
############################################################################


d.f <- data.frame(matrice_txt_R)


paste_d.f <- with(d.f, paste(V1,V2, sep = "_"))

paste_d.f<-data.frame(paste_d.f)

d.f<-cbind(paste_d.f,d.f)

d.f<-data.frame(d.f)



mat <- matrix( 0, nrow=length( unique( d.f$V3)), ncol=length( unique( d.f$paste_d.f)), dimnames=list( unique( d.f$V3), unique( d.f$paste_d.f)))

colnames(mat)<-as.character(colnames(mat))

rownames(mat)<-as.character(rownames(mat))

d.f[,4]<-as.character(d.f[,4])

d.f[,3]<-as.character(d.f[,3])

d.f<-data.frame(d.f)

mat[as.matrix(d.f[,c(4,1)])] <- d.f$V4

cor_matrice_txt_R<-mat


############################################################################
#remise en forme des bornes-inf et sup des équipements                     #
############################################################################

nrow<-1
ncol<-ncol(cor_matrice_txt_R)
n_data<-nrow*ncol

horizon_rcl_inf_equip<-matrix(rep(0,n_data),nrow,ncol)
horizon_rcl_sup_equip<-matrix(rep(0,n_data),nrow,ncol)

colnames(horizon_rcl_inf_equip)<-colnames(cor_matrice_txt_R)
colnames(horizon_rcl_sup_equip)<-colnames(cor_matrice_txt_R)


for (i in 1:ncol(cor_matrice_txt_R))
  {
  horizon_rcl_inf_equip[1,i]<-as.numeric(rcl_inf_equip[which(rcl_inf_equip[,1]==colnames(horizon_rcl_inf_equip)[i]),4])
  }

for (i in 1:ncol(cor_matrice_txt_R))
{
  horizon_rcl_sup_equip[1,i]<-as.numeric(rcl_sup_equip[which(rcl_sup_equip[,1]==colnames(horizon_rcl_sup_equip)[i]),4])
}


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

prevision_R<-(horizon_rcl_sup_equip+horizon_rcl_inf_equip+2)/2


##############################################################################################
## reconditioning of matrix 
##############################################################################################


  cor_matrix_PC_equip<-cor_matrice_txt_R
  cij_erreur_max_reconstituee<-cor_matrix_PC_equip
  cij_erreur_min_reconstituee<-cor_matrix_PC_equip
  
  ##############################################################################################
  ###  erreurs maximale et minimale reconstituées pour chaque composant de base j
  ##############################################################################################
  
  
  for (i in 1:nrow(cor_matrix_PC_equip))
    {
    for (j in 1:ncol(cor_matrix_PC_equip))
      {
      
      
      if (sign(cor_matrix_PC_equip[i,j])>0){
        
        cij_erreur_max_reconstituee[i,j]<-cor_matrix_PC_equip[i,j]*rcl_sup_PCA[1,i]
        cij_erreur_min_reconstituee[i,j]<-cor_matrix_PC_equip[i,j]*rcl_inf_PCA[1,i]}
      else {
        cij_erreur_max_reconstituee[i,j]<-cor_matrix_PC_equip[i,j]*(-rcl_sup_PCA[1,i])
        cij_erreur_min_reconstituee[i,j]<-cor_matrix_PC_equip[i,j]*(-rcl_inf_PCA[1,i])
        }
      
      }
  }
  
  rcl_PCA<-rcl_sup_PCA - rcl_inf_PCA
  
  erreur_max_reconstituee<-colSums(cij_erreur_max_reconstituee)
  names(erreur_max_reconstituee)<-colnames(cor_matrice_txt_R)
  erreur_max_reconstituee=as.matrix(erreur_max_reconstituee)
  
  erreur_min_reconstituee<-colSums(cij_erreur_min_reconstituee)
  names(erreur_min_reconstituee)<-colnames(cor_matrice_txt_R)
  erreur_min_reconstituee=as.matrix(erreur_min_reconstituee)
  
  ##############################################################################################
  ###  modification des matrix_PC_equip[i,j] en fonction de la valeur prévue pour les variables
  ###  de base
  ##############################################################################################
  
  ####  valeur prévue :  val_prev
  
  diff_inf<-horizon_rcl_inf_equip-prevision_R
  diff_sup<-horizon_rcl_sup_equip-prevision_R
  
  coeff_inf<-diff_inf[1,]/erreur_min_reconstituee[]
  coeff_sup<-diff_sup[1,]/erreur_max_reconstituee[]
  
  
  
  for (i in 1:nrow(cor_matrice_txt_R)){
    for (j in 1:ncol(cor_matrice_txt_R)){
      cor_matrix_PC_equip[i,j]<-cor_matrice_txt_R[i,j]*min(max(0,diff_inf[1,j]/erreur_min_reconstituee[j]),
                                                         max(0,diff_sup[1,j]/erreur_max_reconstituee[j]),1)
    }
  }         
  
  
###############################################################################################
## matrix reconditioning and export
###############################################################################################

nrow<-nrow(cor_matrix_PC_equip)*ncol(cor_matrix_PC_equip)
ncol<-4
n_data<-nrow*ncol

bis_matrice_txt_R<-matrix(rep(0,n_data),nrow,ncol)

for (i in 1:ncol(cor_matrix_PC_equip))
  
{
  for (j in 1:nrow(cor_matrix_PC_equip))
  {
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 1]<-colnames(cor_matrix_PC_equip)[i]
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 1]<-str_sub(bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 1], 1,1) 
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 2]<-colnames(cor_matrix_PC_equip)[i]
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 2]<-str_sub(bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 2],3) 
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 3]<-j
    bis_matrice_txt_R[(i-1)*nrow(cor_matrix_PC_equip)+j, 4]<-cor_matrix_PC_equip[j,i]
  }
}

bis_matrice_txt_R<-bis_matrice_txt_R[bis_matrice_txt_R[,4]!=0,]

colnames(bis_matrice_txt_R)<-c("inj. type","inj. num","var. num","coeff.")


##export of reconditioned matrix

##Output path

export_bis_matrix_PC_equip="./cor_matrice.txt"

write.table(bis_matrice_txt_R, file = export_bis_matrix_PC_equip, append = FALSE, quote = FALSE, sep = " ", eol = "\n", na = "NaN", dec = ".", row.names = FALSE, col.names = TRUE )

export_cor_matrix_PC_equip="./cor_matrice.csv"

write.table(cor_matrix_PC_equip, export_cor_matrix_PC_equip,append = FALSE, quote = FALSE, sep = " ",  na = "NaN", dec = ".", row.names = FALSE, col.names = TRUE,qmethod = "double"  )




