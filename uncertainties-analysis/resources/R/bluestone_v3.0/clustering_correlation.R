##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : clustering_correlation.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Cluster equipments based on their correlation
###   Parameters    : data_P  = dataset as preprocessed by preprocess_data function
###                   clustering_type = type of clustering.
###                                     0 to cluster snapshot measures, 1 to cluster forecasting errors
###                   correlation_floor = lowest correlation of the grouped clusters
###                                       The algorithm keeps clustering correlated equipments until the
###                                       correlation floor is reached
###                   thres_nb_complete_obs = minimum percentage of couple of observations without NAs
###                                             in order to compute the correlation of two equipments
###                   alpha = parameter of the filtering of outliers
###                   filtered_output = indicates if the output has outliers filtered
###                                     0 for output with outliers, 1 for output with outliers filtered
###   Outputs       : errors_clust  = forecasting errors for all clusters
###                   errors_equip  = forecasting errors for all equipments
###                   cluster       = list of every equipment in a cluster
###                   errors_cor    = robust correlation matrix of the clusters forecasting errors
###   Version       : 3.0
###   Date          : 18/03/2015
##############################################################################################
###   Example:
###   errors_clust=cluster_equipements(data_P,clustering_type,correlation_floor,thres_nb_complete_obs,alpha,filtered_output)
##############################################################################################


cluster_equipements<-function(data_P,clustering_type,correlation_floor,thres_nb_complete_obs,alpha,filtered_output){

  # Compute forecasting errors
  errors_P=data_P[data_P$horizon=="DACF",!names(data_P)%in%c("datetime","horizon")]-data_P[data_P$horizon=="SN",!names(data_P)%in%c("datetime","horizon")]

  # Put measure values in a list, each element of the list contains data for an equipment
  list_meas=lapply(data_P[data_P$horizon=="SN",! names(data_P) %in% c("horizon","datetime")],function(x) x)


  if (clustering_type==0){ # for measure clustering
    list_data=list_meas
    data=data_P[data_P$horizon=="SN",! names(data_P) %in% c("horizon","datetime")]
    fore_clust=data_P[data_P$horizon=="DACF",names(data)]
  }else if (clustering_type==1){ #for errors clustering
    list_data=lapply(errors_P,function(x) x)
    data=errors_P
  }

  # Calculate the median, filtering NAs and timestamps for which measure is zero
  median_without_zeros<-function(data,data_meas){
    return(median(data[ !is.na(data) & data_meas!=0]))}

  median=mapply(median_without_zeros,list_data,list_meas)


  # Calculate the mad (median average deviation), filtering NAs and timestamps for which measure is zero
  mad_without_zeros<-function(data,data_meas){
    return(mad(data[ !is.na(data) & data_meas!=0]))}

  mad=mapply(mad_without_zeros,list_data,list_meas)


  # Filter out equipments which mad is zero
  filtered_equip=names(mad[is.na(mad)])
  list_data=list_data[!is.na(mad)]
  data=data[,!is.na(mad)]
  if (clustering_type==0){ # for measure clustering
    fore_clust=fore_clust[,!is.na(mad)]}
  median=median[!is.na(mad)]
  mad=mad[!is.na(mad)]


  # Replace outliers by NAs
  filter_outliers<-function(vect,median,mad,alpha){
    vect[abs(vect-median)>alpha*mad]<-NA
    vect[vect==0]<-NA
    return(vect)
  }

  data_cor=mapply(filter_outliers,list_data,median,mad,MoreArgs=list(alpha))


  # Compute correlation matrix, if complete pair of observations, else NA
  cor_matrix=cor(data_cor,use="pairwise.complete.obs")
  diag(cor_matrix)<-NA


  # Compute the rate of complete pair of observations
  temp=!is.na(data_cor)
  rate_complete_obs=t(temp)%*%temp
  rate_complete_obs=rate_complete_obs[colnames(cor_matrix),colnames(cor_matrix)]

  # Do not cluster if the rate of complete observations is below the threshold
  cor_matrix[rate_complete_obs<thres_nb_complete_obs]<-NA

  # Initialisation
  data_cor_save=data_cor
  data_clust=data
  mad_clust=mad
  median_clust=median
  cor_grouped=vector()
  cor_clust=abs(cor_matrix)
  clust=lapply(colnames(cor_matrix),FUN=paste,"",sep="")
  i=0

  # While the max of correlation matrix is above the correlation floor, cluster equipments
  while (max(cor_clust,na.rm=T)>correlation_floor){
    i=i+1
    # Get the most correlated equipments
    id_max=which(cor_clust==max(cor_clust,na.rm=T),arr.ind=T)[1,]
    max_row=min(id_max)
    max_col=max(id_max)
    nom_row=colnames(cor_clust)[max_row]
    nom_col=colnames(cor_clust)[max_col]

    # Save correlation of grouped equipments
    cor_grouped=c(cor_grouped,max(cor_clust,na.rm=T))


    # Update dataframe, replace one by sum and delete the other
    data_clust[,nom_col]=data_clust[,nom_row]+data_clust[,nom_col]
    data_clust=data_clust[,names(data_clust)!=nom_row]

    if (clustering_type==0){
      fore_clust[,nom_col]=fore_clust[,nom_row]+fore_clust[,nom_col]
      fore_clust=fore_clust[,names(fore_clust)!=nom_row]}

    # Update list_meas
    list_meas[[nom_col]]=list_meas[[nom_row]]+list_meas[[nom_col]]
    list_meas=list_meas[names(list_meas)!=nom_row]

    # Update mad
    mad_clust[nom_col]=mad_without_zeros(data_clust[,nom_col],list_meas[[nom_col]])
    mad_clust=mad_clust[names(mad_clust)!=nom_row]

    # Update median
    median_clust[nom_col]=median_without_zeros(data_clust[,nom_col],list_meas[[nom_col]])
    median_clust=median_clust[names(median_clust)!=nom_row]


    # Update data_cor
    data_cor[,nom_col]=filter_outliers(data_clust[,nom_col],median_clust[nom_col],mad_clust[nom_col],alpha)
    data_cor=data_cor[,colnames(data_cor)!=nom_row]


    # Update cor_clust
    cor_vect=abs(apply(data_cor[,colnames(data_cor)!=nom_col],2,cor,data_cor[,nom_col],use="pairwise.complete.obs"))

    # Filter out pair of observations below the threshold
    rate_complete_obs=(t(!is.na(data_cor[,nom_col]))%*%!is.na(data_cor[,colnames(data_cor)!=nom_col]))/nrow(data_cor)
    cor_vect[rate_complete_obs<thres_nb_complete_obs]<-NA

    # Sort cor_vect
    cor_vect=cor_vect[colnames(cor_clust)]


    # Update cor_clust
    cor_clust[max_col,]=cor_vect
    cor_clust[,max_col]=cor_vect

    # Update description of clusters
    clust[[max_col]]=c(clust[[max_col]],clust[[max_row]])

    clust=clust[-max_row]

    cor_clust=cor_clust[-max_row,-max_row]
    print(c(i,cor_grouped[length(cor_grouped)]))
  }

  # Consider data_cor instead of data_clust for filtered output
  if (filtered_output==1){
    data_clust=data_cor
    data=data_cor_save
  }

  # Compute errors matrix
  if (clustering_type==0){# for measure clustering
    errors_clust=fore_clust-data_clust
    errors_equip=data_P[data_P$horizon=="DACF",colnames(data)]-data

    # Compute correlation matrix on errors, if complete pair of observations, else NA
    cor_clust=cor(fore_clust-data_cor,use="pairwise.complete.obs")
    diag(cor_clust)<-NA
    # Compute the rate of complete pair of observations
    temp=!is.na(fore_clust-data_cor)
    rate_complete_obs=t(temp)%*%temp
    rate_complete_obs=rate_complete_obs[colnames(cor_clust),colnames(cor_clust)]
    # Do not cluster if the rate of complete observations is below the threshold
    cor_clust[rate_complete_obs<thres_nb_complete_obs]<-NA

  }else if (clustering_type==1){#for errors clustering
    errors_clust=data_clust
    errors_equip=data
  }

  res=list(errors_clust,errors_equip,clust,cor_clust)
  names(res)=c("errors_clust","errors_equip","cluster","errors_cor")
  return(res)}