##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : error_box.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Compute error box with the error_box function
###   Parameters    : dataset=  the dataset on which the error box is calculated
###                             the dataset is in the equipment base
###                   prct=     the percentage of observations in the error box
###                   epsilon=  the precision for z, the box main parameter
###   Outputs       : z=        the box parameter
###                   inf=      the lower bounds of the box
###                   sup=      the upper bounds of the box
###                   loadings= the transition matrix from equipments to principal components
###                             structure: rows= equipments, columns= principal components
###   Version       : 1.1
###   Modification  : "comp.XX" names changed to "DimXX"
###   Date          : 14/10/2014
###   Comments      : This file also contains four other functions:
###                   plot_error_box function plots the error box, given two principal components
###                   nb_inbounds function calculates which principal components have observations out of the box
###                   find_z function calculates the box parameter, called by error_box.
###                   nb_inbox function calculates the number of observations in the box, called by error_box.
##############################################################################################
###   Example:
###   box=error_box(errors_P,0.95,0.00001)
###   data_PC=as.matrix(errors_P)%*%box[[4]]
###   PC_inbound=nb_inbounds(data_PC,error_box = box)
###   plot_error_box(data_PC,error_box = box,comp1 = PC_inbound[1,1],comp2 = PC_inbound[2,1])
##############################################################################################


#Function which computes the error boxes
error_box<-function(dataset,prct,epsilon){
  # dataset: the dataset on which the error box is calculated, the dataset is in the equipment base
  # prct: the percentage of observations in the error box
  # epsilon: the precision on z

  #PCA: error boxes are calculated on PCA axes
  acp=princomp(dataset,cor=F)
  data_PC=acp$scores
  #Compute mu and sigma for each column
  mu=apply(data_PC,2,mean)
  sigma=apply(data_PC,2,sd)
  #Find z
  z=find_z(data_PC,prct,epsilon,mu,sigma)
  #Lower bound
  inf=mu-z*sigma
  #Upper bound
  sup=mu+z*sigma
  res=list(z,inf,sup,acp$loadings)
  names(res)=c("z","inf","sup","loadings")
  names(res$inf)=paste("Dim",substr(names(res$inf),6,nchar(names(res$inf))),sep="")
  names(res$sup)=paste("Dim",substr(names(res$sup),6,nchar(names(res$sup))),sep="")
  colnames(res$loadings)=paste("Dim",substr(colnames(res$loadings),6,nchar(colnames(res$loadings))),sep="")

  # z: the box parameter
  # inf: the lower bound of the box
  # sup: the upper bound of the box
  # loadings: the matrix to go from equipments to principal components
  # loadings structure: rows= equipments, columns= principal components

  return(res)
}

#Function which find z
find_z<-function(data_PC,prct,epsilon,mu,sigma){

  #find extrema of z
  val=matrix(0,nrow=nrow(data_PC),ncol=ncol(data_PC))
  for(k in 1:ncol(data_PC)){
    if(sigma[k]!=0){
      val[,k]=data_PC[,k]/sigma[k]
    }else{
      val[,k]=0
    }
  }
  zmax<-max(na.omit(val))
  zmin=0

  #initialisation
  zd=(zmax+zmin)/2


  #Required number of observations in the box
  ndata=prct*nrow(data_PC)

  #### Find zd by dichotomy
  #Stop iterations when zd change less than Epsilon from one iteration to the other
  nbox=nb_inbox(zd,data_PC,mu,sigma)
  nb_iter=0
  while(nb_iter!=-1){
    nb_iter=nb_iter+1
    old_zd=zd
    if(nbox<ndata){
      zmin=zd
      zd=(zmax+zmin)/2
    }else{
      zmax=zd
      zd=(zmax+zmin)/2
    }
    if(abs(zd-old_zd)<epsilon){
      z=c(zd,old_zd)
      zd=z[which.min(c(abs(nb_inbox(zd,data_PC,mu,sigma)-ndata),abs(
        nb_inbox(old_zd,data_PC,mu,sigma)-ndata)))]
      nb_iter=-1
    }
    nbox=nb_inbox(zd,data_PC,mu,sigma)
    print(c(zmin,zmax,zd,nbox,nbox/ndata*prct*100,nb_iter))
  }
  return(zd)
}


##Function which calculates the number of observation in the box
nb_inbox<-function(z,data_PC,mu,sigma){
  if (z!=0){
    inf=mu-z*sigma
    sup=mu+z*sigma
    data_inf<-sweep(data_PC,2,inf)
    data_sup<-sweep(data_PC,2,sup)
    nobs_line=length(which(apply((data_inf>=0)&(data_sup<=0),1,sum)==ncol(data_PC)))
  }else {nobs_line=0}
  return(nobs_line)
}


##Function which calculates the number of observation in the box for each principal component
nb_inbounds<-function(data_PC,error_box){
  inf=error_box[[2]]
  sup=error_box[[3]]
  nobs_pc<-as.data.frame(matrix(0,ncol(data_PC),0))

  for (col in 1:ncol(data_PC)){
    nobs_pc[col,1]<-colnames(data_PC)[col]
    nobs_pc[col,2]<-length(data_PC[,col][which((data_PC[,col]>=inf[col])&(data_PC[,col]<=sup[col]))])
  }
  nobs_pc[,3]=nobs_pc[,2]/nrow(data_PC)*100

  #Sort according to the number of observations in the box
  nobs_pc=nobs_pc[order(nobs_pc[,2]),]
  colnames(nobs_pc)<-c("Principal Component","Observations_in_box","%Observations_in_box")
  return(nobs_pc)
}


##Function to plot error box
plot_error_box<-function(data_PC,error_box,comp1,comp2){

  data=data_PC[,c(comp1,comp2)]

  inf=error_box[[2]]
  sup=error_box[[3]]

  abs1<-as.numeric(inf[comp1])
  abs2<-as.numeric(sup[comp1])
  ord1<-as.numeric(inf[comp2])
  ord2<-as.numeric(sup[comp2])

  a1=min(abs1,min(data[,1]))
  a2=max(abs2,max(data[,1]))
  o1=min(ord1,min(data[,2]))
  o2=max(ord2,max(data[,2]))

  smoothScatter(data[,1],data[,2],main=paste("Error box for ",colnames(data)[1]," and ",colnames(data)[2],sep=""),xlab=colnames(data)[1],ylab=colnames(data)[2],xlim=c(a1-1,a2+1),ylim= c(o1-1,o2+1))
  rect(abs1,ord1,abs2,ord2,border='red',xpd=T)

}






