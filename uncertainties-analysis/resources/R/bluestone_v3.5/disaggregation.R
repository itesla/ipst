##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : disaggregation.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Compute the transition matrix and vector to disaggregate clusters back to the equipment level
###   Parameters    : errors_clust = forecasting errors for all clusters
###                   errors_equip = forecasting errors for all equipments
###                   clust = list of every equipment in a cluster
###   Outputs       : matrix_clust_equip = the transition matrix to go from clusters to equipements
###                   vect_clust_equip = the vector to go from clusters to equipements
###   Version       : 3.4
###   Date          : 16/04/2015
##############################################################################################
###   Example:
###   disaggregation_res=disaggregation(errors_clust,errors_equip,clust)
##############################################################################################




compute_clust_to_equip<-function(list_clust,dfm_equip){

    list_cluster=list_clust[[1]]
    list_vector=list_clust[[2]]
    list_Mean_Equip=dfm_equip$mean
    names(list_Mean_Equip)=dfm_equip$equipment
    list_Mad_Equip=dfm_equip$mad
    names(list_Mad_Equip)=dfm_equip$equipment
    list_Var_Equip=dfm_equip$var
    names(list_Var_Equip)=dfm_equip$equipment
    nb_equip=length(dfm_equip$equipment)
    nbObs=length(list_vector[[1]])
    nb_clusters=length(list_vector)
    ## Mad au carré
    list_Mad_Equip2=list_Mad_Equip^2

    ##Moyenne des clusters
    MeanAgreg=sapply(list_vector,mean,na.rm=T)

    ## Calcul de mu
    ## Pour les clusters à un elt muEq=0
    muEq=numeric(nb_equip)
    names(muEq)=dfm_equip$equipment
    for(i in 1:length(list_cluster)){
      nom=names(list_cluster[i])
      col=unlist(list_cluster[i])
      Mad=list_Mad_Equip2[col]
      Mean=list_Mean_Equip[col]
      if (length(col)==1){
        muEq[col]=0
      }else{
        if (sum(Mad,na.rm=T)!=0){
          muEq[col]=Mean[col]-MeanAgreg[nom]*Mad[col]/sum(Mad)
        }
        else{
          Var=list_Var_Equip[col]
          if (sum(Var,na.rm=T)!=0){
            muEq[col]=Mean[col]-MeanAgreg[nom]*Var[col]/sum(Var)
          }
          else{
            muEq[col]=Mean[col]-MeanAgreg[nom]/length(col)
          }
        }
      }
    }

    ## Calcul de sigma
    sigma=data.frame(matrix(nrow=nb_clusters,ncol=nb_equip,0))
    colnames(sigma)=dfm_equip$equipment
    rownames(sigma)=names(list_cluster)
    for(i in 1:length(list_cluster)){
      nom=names(list_cluster)[i]
      col=unlist(list_cluster[i])
      Mad=list_Mad_Equip2[col]
      if (sum(Mad,na.rm=T)!=0){
        sigma[which(rownames(sigma)==nom),col]=Mad[col]/sum(Mad)
      }
      else{
        Var=list_Var_Equip[col]
        if (sum(Var,na.rm=T)!=0){
          sigma[which(rownames(sigma)==nom),col]=Var[col]/sum(Var)
        }
        else{
          sigma[which(rownames(sigma)==nom),col]=1/length(col)
        }
      }
    }
    res=list(sigma,muEq)
    names(res)=c("matrix_clust_equip","vect_clust_equip")
    return(res)
  }


disaggregation<-function(errors_clust,errors_equip,clust){


  names_cluster=colnames(errors_clust)
  tempCluster=list()
  tempErrorsCluster=list()
  date=data_P[seq(1,nrow(data_P),2),"datetime"]
  for(i in 1:length(names_cluster)){
    tempCluster[i]=clust[i]
    names(tempCluster)[i]=names_cluster[i]
    tempErrorsCluster[[i]]=errors_clust[,names_cluster[i]]
    #names(tempErrorsCluster[[i]])=date
    names(tempErrorsCluster)[i]=names_cluster[i]
  }

  list_clust=list(tempCluster,tempErrorsCluster)
  names(list_clust)=c("clusters","errors")

  #dfm equipememnt
  dfm_equip=data.frame(matrix(nrow=dim(errors_equip)[2],ncol=4))
  colnames(dfm_equip)=c("equipment","mean","mad","var")
  dfm_equip['equipment']=colnames(errors_equip)
  dfm_equip['mean']=apply(errors_equip,2,mean,na.rm=T)
  dfm_equip['mad']=apply(errors_equip,2,mad,na.rm=T)
  dfm_equip['var']=apply(errors_equip,2,var,na.rm=T)

  return(compute_clust_to_equip(list_clust,dfm_equip))
}