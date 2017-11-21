#########################################################################
## Copyright (c) 2017, RTE (http://www.rte-france.com)                  #
## This Source Code Form is subject to the terms of the Mozilla Public  #
## License, v. 2.0. If a copy of the MPL was not distributed with this  #
## file, You can obtain one at http://mozilla.org/MPL/2.0/.             #
#########################################################################

library("MASS");
library("FactoMineR");

################################################################
## definition du fichier csv � traiter                         #
################################################################

############################################################
## essai2 : reseau France                                 ##
############################################################


fichier="forecastsDiff_load.csv"

################################################################
## lecture du fichier csv d'une matrice comprenant             #
## en alterne dacfs et snapshots                               #
################################################################

lecture<-function(Nom_Requette) {
  Decimale = "."
  matrice <- read.csv2(Nom_Requette,header=T,dec=Decimale,sep=",")
  initial<-matrice
  return(matrice)
}
################################################################
## calcul des composantes principales et des quantiles � 5 et  #
## 95 % de la diffence entre dacfs et snapshots � partir de la #
## matrice des dacfs et des snapshots extraite de la base      #
## de data mining                                              #
################################################################


################################################################
## obtention d'une matrice dont le terme courant est la        #
## difference entre dacf et snapshot � partir de la matrice    #
## d'entree                                                    #
################################################################

diff_dacfs_snapshots<-function(d_s,ncp) {

  mat<-mat1

  date_horizon = cbind(mat[,"datetime"],mat[,"horizon"])


  mat <- mat[,colnames(mat)!="horizon"]

  mat<- mat[,colnames(mat)!="forecastTime"]

  mat<-cbind(date_horizon,mat)

  names(mat)[names(mat)=="2"]<-"horizon"

  names(mat)[names(mat)=="1"]<-"datetime"

  print(data.frame("debut extraction dacfs",date()))
  dacfs<-mat [mat [,"horizon"]%in%(1),]

  date<-dacfs[,"datetime"]

  dacfs<-dacfs[,colnames(dacfs)!="datetime"]

  dacfs<-dacfs[,colnames(dacfs)!="horizon"]

  dacfs<-dacfs[,colnames(dacfs)!="date"]


  snapshots<-mat [mat [,"horizon"]%in%(2),]

  snapshots<-snapshots[,colnames(snapshots)!="datetime"]

  snapshots<-snapshots[,colnames(snapshots)!="horizon"]

  snapshots<-snapshots[,colnames(snapshots)!="date"]

  print(data.frame("debut difference dacfs-snapshots",date()))
  diff<-dacfs-snapshots

  diff<-diff[,colnames(diff)!="row.names"]
  print(data.frame("fin difference dacfs-snapshots",date()))




  #diff <- matrix (rep(0, (nrow(mat)/2)*ncol(mat)),( nrow(mat)/2),ncol(mat))

  #cat(dim(diff))
  #cat(dim(mat))

  #colnames(diff)<-colnames(mat)

  #for( k in 1:nrow(diff)) {

  #    for (m in 1:ncol(diff) ){

  #     diff[k,m]=mat[(2*k-1),m]-mat[(2*k),m]}
  #}

  #for( k in 1:nrow(mat)/2) { diff[k,1]=mat[2*k-1,1]}

 # clair <- matrix (rep(0, (nrow(diff))*5), nrow(diff),5)

 # diff<-cbind(clair,diff)


  #for(i in 1:nrow(diff)){
  #  val=diff[i,6]
   # diff[i,1] = val
    #diff[i,2] = as.numeric(format(as.POSIXct(val, origin = "1970-01-01"), format="%Y"))
  #  diff[i,3] = as.numeric(format(as.POSIXct(val, origin = "1970-01-01"), format="%m"))
  #  diff[i,4] = as.numeric(format(as.POSIXct(val, origin = "1970-01-01"), format="%d"))
  #  diff[i,5] = as.numeric(format(as.POSIXct(val, origin = "1970-01-01"), format="%H"))
  #  diff[i,6] = as.numeric(format(as.POSIXct(val, origin = "1970-01-01"), format="%M"))
  #}
  #dimnames(diff)[[2]][1]<-"datetime"
  #dimnames(diff)[[2]][2]<-"year"
  #dimnames(diff)[[2]][3]<-"month"
  #dimnames(diff)[[2]][4]<-"day"
  #dimnames(diff)[[2]][5]<-"hour"
  #dimnames(diff)[[2]][6]<-"minute"

  #princ<-diff[,colnames(diff)!=("datetime")]
  #princ<-princ[,colnames(princ)!=("year")]
  #princ<-princ[,colnames(princ)!=("month")]
  #princ<-princ[,colnames(princ)!=("day")]
  #princ<-princ[,colnames(princ)!=("hour")]
  #princ<-princ[,colnames(princ)!=("minute")]

 princ<-diff

  return (princ)
}


###########################################################
## PCA with PCA                                         ##
## generation of results and explanation                ##
###########################################################


ACPPCA<-function(Xcr){
  resPCA<-PCA(Xcr,ncp=ncol(Xcr),scale.unit=TRUE,graph = FALSE)
}


############################################################
## calcul des bornes des composantes principales, de la    #
## matrice et du vecteur necessaires au retour vers les    #
## variables originelles                                   #
############################################################

bornes_CPs<-function(resPCA){

  coordcp<-resPCA$ind$coord


  distrib1<- matrix (rep(0, nrow(coordcp)*1),  nrow(coordcp),1)

  distribinf<-matrix(rep(0,1*ncol(coordcp)),1,ncol(coordcp))

  distribsup<-matrix(rep(0,1*ncol(coordcp)),1,ncol(coordcp))

  for (i in 1:ncol(coordcp)){

    distrib1=coordcp[,i]

    fit1<-fitdistr(distrib1,"normal")

    c=fit1$estimate[1][1]

    c<-as.numeric(c)

    d=fit1$estimate[2][1]

    d<-as.numeric(d)

    inf<-qnorm(0.05, mean = c, sd = d)

    sup<-qnorm(0.95,mean=c,sd=d)

    distribinf[i]=inf

    distribsup[i]=sup
  }

  colnames(distribinf)=colnames(coordcp)

  colnames(distribsup)=colnames(coordcp)

  res = list("bornes_inf"=distribinf,"bornes_sup" =distribsup,"resultats_CP"=resPCA)

  nombreCPs<-matrix(data=rep(0,200),ncol=2)

  nombreCPs[,1]=c(1:100)

  for (seuil in 1:100) {

    for (i in 1:nrow(resPCA$eig)){

      if ((resPCA$eig[i,3]-seuil)<0){
        ncp=i+1
        nombreCPs[seuil,2]=i+1
      }
    }
  }

  ncp=nombreCPs[80,2]

  distribinf<-as.matrix(distribinf[,1:ncp])

  distribsup<-as.matrix(distribsup[,1:ncp])


  coord.var = as.matrix(res$resultats_CP$var$coord)[, 1:ncp, drop = F]

  coord.ind = as.matrix(res$resultats_CP$ind$coord)[, 1:ncp, drop = F]

  A<-t(sweep(coord.var, 2, sqrt(res$resultats_CP$eig[1:ncp,1]), FUN = "/"))

  B <- diag(res$resultats_CP$call$ecart.type)

  C<-res$resultats_CP$call$centre

  C<-t(as.matrix(C))

  TR<-A%*%B

  colnames(TR)<-colnames(unimodaux)


  sortie = list("matrice_unimodaux"=unimodaux,"bornes_inf"=distribinf,"bornes_sup" =distribsup,"matrice"=TR,"vecteur"=C)


  return (sortie)
}
#####################################################################################
#tri des variables dans dacfs ou snapshots ou diff en fonction des zeros, des modes #
#####################################################################################


tri<-function(L_a)
{
  mn_a <- apply(as.matrix(L_a),2,mean,na.rm=TRUE)
  st_a <- apply(as.matrix(L_a),2,sd,na.rm=TRUE)

  # Delete outliers > S_out * sigma
  S_out=12
  # Delete all zero values if non consistent with mean and std
  pzero=5/100
  # Delete Loads with too small variations ( sigma  <= 1 MW )
  sigm=1

  L_ac <- L_a
  w <- which(st_a > sigm)
#  cat(w)

  N <- length(L_a[,1])
  M <- length(L_a[1,])
  L_mode <- array(0,M)
  L_zero <- array(0,M)

  Np <- N*pzero

  for (l in w)
  {
  # cat(l)
    wout <- which(abs(L_a[,l]-mn_a[l]) > S_out*st_a[l])
    if (length(wout) > 0 )
    {
   #   cat(">> ", names(L_a)[l], " outlier : ", length(wout),"\n")
      L_ac[wout,l] <- NA
    }

    wzero <- which(L_ac[,l] == 0)
    Nzero = length(wzero)

    m_lac=mean(L_ac[,l],na.rm=TRUE)
    s_lac=sd(L_ac[,l],na.rm=TRUE)

    # Number of realisations around zero [-sigma/10, sigma/10]

    Neps_e <- ( pnorm(s_lac/10,mean=m_lac,sd=s_lac) - pnorm(-s_lac/10,mean=m_lac,sd=s_lac) )*N
    Neps_a <- length(which(abs(L_ac[,l]) < s_lac/10));

    if (Nzero > Np | Neps_a > 2*Neps_e)
    {
      L_ac[wzero,l] <- NA
#      cat("    !!", names(L_a)[l], " zeros : ", Nzero, " # ", Neps_e, Neps_a, "\n")
      L_zero[l] <- 1;
    }

    wnan <- which(!is.na(L_ac[,l]))
    if (length(wnan) > Np) {

      L_mode[l] <- 1

      cl    <- kmeans(L_ac[wnan,l],2)
      st_cl <- sqrt(cl$withinss/cl$size)
      mn_cl <- cl$centers

      dnp <- density(L_ac[wnan,l])
      t <- dnp$x

      dg1 <- dnorm(t,mean=m_lac,sd=s_lac)
      D1 <- sum((dnp$y-dg1)**2)
      Dm1 <- max(abs(dnp$y-dg1))

      dg2 <- (cl$size[1]*dnorm(t,mean=mn_cl[1],sd=st_cl[1])+cl$size[2]*dnorm(t,mean=mn_cl[2],sd=st_cl[2]))/(cl$size[1]+cl$size[2])
      D2 <- sum((dnp$y-dg2)**2)
      Dm2 <- max(abs(dnp$y-dg2))

      if ( D2 < 0.8*D1)
      {
 #       cat("             ## ", names(L_a)[l], " Not unimodal: ", D1,D2," # ",Dm1,Dm2, "\n")
        L_mode[l] <- -1;
      }
    }
  }
  sortie=list("L_mode"=L_mode ,"L_ac"=L_ac ,"L_a"=L_a)

  return(sortie)
}
##################################################################
#### production des resultats unimodaux en supposant             #
#### que les variables de d�part sont les composantes principales#
##################################################################

bornes_VDs<-function(M){

  TR<-diag(ncol(M))

  C<- rep(0,ncol(M))

  C<-t(as.matrix(C))

  colnames(TR)<-colnames(M)

  colnames(C)<-colnames(M)

  distrib1<- matrix (rep(0, nrow(M)*1),  nrow(M),1)

  distribinf<-matrix(rep(0,1*ncol(M)),1,ncol(M))

  distribsup<-matrix(rep(0,1*ncol(M)),1,ncol(M))

  for (i in 1:ncol(M)){

    distrib1=M[,i]

    fit1<-fitdistr(distrib1,"normal")

    c=fit1$estimate[1][1]

    c<-as.numeric(c)

    d=fit1$estimate[2][1]

    d<-as.numeric(d)

    inf<-qnorm(0.25, mean = c, sd = d)

    sup<-qnorm(0.75,mean=c,sd=d)

    distribinf[i]=inf

    distribsup[i]=sup
  }
  colnames(distribinf)<-colnames(sansna)
  colnames(distribsup)<-colnames(sansna)
  rownames(TR)<-colnames(sansna)

  for (j in 1:ncol(distribinf)){
    dimension<-"Dim"
    nombre<-j
    nomcol<-paste(dimension,nombre, sep="")
    colnames(distribinf)[j]<-nomcol
    }

  for (j in 1:ncol(distribsup)){
    dimension<-"Dim"
    nombre<-j
    nomcol<-paste(dimension,nombre, sep="")
    colnames(distribsup)[j]<-nomcol
    rownames(TR)[j]<-nomcol
  }
  distribinf<-as.matrix(distribinf)
  distribsup<-as.matrix(distribsup)

  distribinf<-t(distribinf)
  distribsup<-t(distribsup)



  sortie = list("matrice_unimodaux"=unimodaux,"bornes_inf"=distribinf,"bornes_sup" =distribsup,"matrice"=TR,"vecteur"=C)


  return (sortie)
}

##################################################################
##  programme principal de fourniture des resultats             ##
##################################################################

#lecture des donnees au format csv#
mat1<-lecture(fichier)

#calcul des la matrice "prevu"-"realise"#
princ<-diff_dacfs_snapshots(mat1)

#tri des variables en unimodales, bimodales et constantes
sortie_tri<-tri(princ)

L_mode_princ<-sortie_tri$L_mode

L_ac_princ<-sortie_tri$L_ac

L_a_princ<-sortie_tri$L_a

wuni<-which(L_mode_princ==1)

wbimodal<-which(L_mode_princ==-1)

wzero<-which(L_mode_princ==0)

#traitement des unimodaux
unimodaux<-princ[,wuni]
sansna<-unimodaux
sansna[is.na(sansna)] <- 0

#resPCA<-ACPPCA(unimodaux)

#sortie_unimodaux<- bornes_CPs(resPCA)

sortie_unimodaux<- bornes_VDs(sansna)


#traitement des constants : moyenne des valeurs#

constants<-princ[,wzero]

moyenne<- apply(as.matrix(constants),2,mean,na.rm=TRUE)

sortie_constants<-list("matrice_constants"=constants, "valeurs_constants"=moyenne)

#traitement des bimodaux : recherche directe des bornes

bimodaux<-as.matrix(princ[,wbimodal])

distrib1<- matrix (rep(0, nrow(bimodaux)*1),  nrow(bimodaux),1)

distribinf<-matrix(rep(0,1*ncol(bimodaux)),1,ncol(bimodaux))

distribsup<-matrix(rep(0,1*ncol(bimodaux)),1,ncol(bimodaux))

for (i in 1:ncol(bimodaux)){
  distrib1=bimodaux[,i]
  borne1<-quantile(distrib1,probs=0.05,na.rm=TRUE)
  distribinf[i]=borne1
  borne2<-quantile(distrib1,probs=0.95,na.rm=TRUE)
  distribsup[i]=borne2
}

colnames(distribinf)=colnames(bimodaux)

colnames(distribsup)=colnames(bimodaux)

sortie_bimodaux = list("matrice_bimodaux"=bimodaux,"bornes_inf_bimodaux"=distribinf,"bornes_sup_bimodaux" =distribsup)

resultats=list("resultats_unimodaux"=sortie_unimodaux,"resultats_bimodaux"=sortie_bimodaux,"resultats_constants"=sortie_constants,"mat1"=mat1)



write.csv(resultats$resultats_unimodaux$bornes_inf, file = "bornes_inf.csv")

write.csv(resultats$resultats_unimodaux$bornes_sup, file = "bornes_sup.csv")

write.csv(resultats$resultats_unimodaux$matrice_unimodaux, file = "matrice_unimodaux.csv")

write.csv(resultats$resultats_unimodaux$matrice, file = "matrice.csv")

write.csv(resultats$resultats_unimodaux$vecteur, file = "vecteur.csv")
