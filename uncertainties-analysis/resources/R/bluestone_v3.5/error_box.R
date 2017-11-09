##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : error_box.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Compute error box
###   Parameters    : dataset = the dataset on which the error box is calculated
###                   prct_risk = the level of risk of the error box
###                   errors_cor = robust correlation matrix of the input dataset
###                                the principal components are calculated by diagonalisating this matrix
###                   nb_PC = the number of principal components to be returned
###   Outputs       : inf=      the lower bounds of the box
###                   sup=      the upper bounds of the box
###                   means=    the mean of each equipment
###                   loadings= the transition matrix from clusters to principal components
###                             structure: rows= clusters, columns= principal components
###                   scores=   the dataset in the principal components space
###                   nb_PC=    the number of principal components actually kept. It may be different
###                             from the original parameter if the number of positive components is less
###                             than the number of components asked.
###   Version       : 3.4
###   Date          : 16/04/2015
##############################################################################################
###   Example:
###   box=error_box(errors_clust,prct_risk,errors_cor,nb_PC)
##############################################################################################


#Function which computes the error boxes
error_box<-function(dataset,prct_risk,errors_cor,nb_PC){

  # Compute the mean of each equipment, ignoring NAs
  means=apply(dataset,2,mean,na.rm=T)

  # Remove the mean for each equipment
  dataset=t(apply(dataset,1,function(x) x-means))


  errors_cor=errors_cor[colnames(dataset),colnames(dataset)]
  diag(errors_cor)=rep(1,ncol(errors_cor))
  errors_cor[is.na(errors_cor)]=0
  vp<-eigen(errors_cor)

  nb_PC_pos=sum(vp$values>0)
  if (nb_PC_pos<nb_PC){
    print("Filtering of the negative or null principal components:")
    print(paste('The number of principal components kept has been reduced to ',nb_PC_pos," principal components.",sep=""))
    nb_PC=nb_PC_pos
  }

  load=vp$vectors
  rownames(load)=colnames(errors_cor)
  colnames(load)=paste("comp.",seq(ncol(load)),sep="")

  # Sort PC according to abs(eigen values)
  #load=load[,order(abs(vp$values),decreasing=T)]

  eval_nipals<-function(x,load){
    num=apply(load,2,function(vect_propre)sum(vect_propre*x,na.rm=T))

    den=load^2
    den[is.na(x),]=0
    den=colSums(den)

    return(num/den)
  }


  data_PC= t(apply(dataset,1,eval_nipals,load))
  data_PC[is.na(data_PC)]=0

  probMult=50

  #Flat prct
  prct_inbox_flat=rep(1-(1-prct_risk)^(probMult/nb_PC),nb_PC)

  #Optimized prct
  #!!! some eigenvalues are negative !
  #nb_PC = 500
  weights = vector(length=nb_PC)
  weights[1:nb_PC]=sqrt(pmax(0,vp$values[1:nb_PC]))
  totalWeight = sum(weights[1:nb_PC])
  #weights = weights/totalWeight
  #vp$values[c(1,7,8,160,161)]

  s = pi/sqrt(3) * weights

  #si=seq(0,8,0.0001)
  #tabScore=-log(2*(1-pnorm(si)))/si+log(2*(1-pnorm(1E-6)))/1E-6

  #ORIGINE
  #propKey=0
  #moveSize = 0.1
  propKey=max(s)
  moveSize = 0.05 * propKey
  lastDir=-1

  #ORIGINE
  logOneMinusglobalPrct=0
  logOneMinus_prct_risk_limit=nb_PC*log(0.5)+probMult*log(1-prct_risk)
  pminexp=10/dim(data_PC)[1];
  indIteration = 0;
  while (abs(logOneMinusglobalPrct-logOneMinus_prct_risk_limit)>1E-4 && indIteration<=100) {

  #prct_inbox_obj=0.0001;
  #prct_inbox_opt = 0
  #while (abs(min(prct_inbox_opt)/prct_inbox_obj-1)>1E-4) {
    indIteration = indIteration+1;
    print(indIteration)
    #ORIGINE
    #pcRiskLevel=weights*exp(-propKey)

    #ORIGINE
    #prct_inbox_opt = 1-2*(1-pnorm(pcRiskLevel))
    prct_inbox_opt = pmin(pmax(pminexp,1-s/propKey),0.5)
    print(min(prct_inbox_opt))

    #ORIGINE
    #logOneMinusglobalPrct=sum(log(1-prct_inbox_opt[1:nb_PC]))
    logOneMinusglobalPrct=sum(log(prct_inbox_opt[1:nb_PC]))
    print(propKey)
    print(moveSize)
    #print(lastDir)
    #print(logOneMinusglobalPrct)
    #if (is.na(globalPrct)) {
    #  globalPrct=1
    #}
    #ORIGINE
    if (logOneMinusglobalPrct>logOneMinus_prct_risk_limit) {
    #if (min(prct_inbox_opt)>prct_inbox_obj) {
      if (lastDir==1) {
        moveSize=sqrt(2)*moveSize
      } else {
        if (lastDir==0) {
          moveSize=moveSize/2
        }
      }
      propKey=propKey-moveSize
      lastDir=1;
    } else {
      if (lastDir==0) {
        moveSize=sqrt(2)*moveSize
      } else {
        if (lastDir==1) {
          moveSize=moveSize/2
        }
      }
      propKey=propKey+moveSize
      lastDir=0;
    }
  }
  if (indIteration>=100) {
    print("in error_box.R, dichotomy failed");
  }

  #prct_inbox = prct_inbox_flat;
  prct_inbox = prct_inbox_opt;

  #Compute quantiles for each principal component
  #q_sup=apply(data_PC,2,quantile,1-(1-prct_inbox[1])/2)
  #q_inf=apply(data_PC,2,quantile,(1-prct_inbox[1])/2)
  q_sup = matrix(nrow = 1, ncol=nb_PC)
  q_inf = matrix(nrow = 1, ncol=nb_PC)
  for (indPC in 1:nb_PC) {
    q_sup[indPC]=quantile(data_PC[,indPC],probs = 1-(prct_inbox[indPC]))
    q_inf[indPC]=quantile(data_PC[,indPC],probs =   (prct_inbox[indPC]))
    gap = 0;
    if (q_sup[indPC]<0) {
      gap = q_sup[indPC]
    }
    if (q_inf[indPC]>0) {
      gap = q_inf[indPC]
    }
    q_sup[indPC] = q_sup[indPC]-gap;
    q_inf[indPC] = q_inf[indPC]-gap;
  }
  #0 should be in the interval

  #(q_sup[c(1,7,8,160,161)]-q_inf[c(1,7,8,160,161)])/2

  indsUtiles=which(q_sup>q_inf+1E-6);
  nb_PC = indsUtiles[length(indsUtiles)];

  #Return results
  box_res=list(q_inf[1:nb_PC],q_sup[1:nb_PC],means,load[,1:nb_PC],data_PC[,1:nb_PC],nb_PC)
  names(box_res)=c("inf","sup","means","loadings","scores","nb_PC")

  return(box_res)}