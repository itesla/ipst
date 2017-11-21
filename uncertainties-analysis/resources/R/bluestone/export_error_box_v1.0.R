##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : export_error_box_v0.1.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTestla online module
###   Function      : This file is an example of how to import a dataset, compute error boxes and export results
###   Version       : 1.0
###   Date          : 06/10/2014
##############################################################################################


#Dependencies
source("preprocess_data_error_box_v1.0.R")
source("error_box_v1.1.R")
library("data.table")

#Input paths
gen_path = "forecastsDiff_gen.csv"
load_path = "forecastsDiff_load.csv"

#Output paths
export_inf="bornes_inf.csv"
export_sup="bornes_sup.csv"
export_transfer_matrix="matrice.csv"
export_mean_vector="vecteur.csv"

#Parameters
prct_inbox=0.95
precision_z=0.00001


#Import data
data_gen<-as.data.frame(fread(gen_path,sep=",",header=TRUE,stringsAsFactors=F))
data_load<-as.data.frame(fread(load_path,sep=",",header=TRUE,stringsAsFactors=F))

#Preprocess
errors_P=preprocess_error_box_data(data_gen,data_load)

#Compute error boxes
box=error_box(errors_P,prct_inbox,precision_z)


#Export data

#INF
inf=data.frame(PC=names(box[[2]]),inf=box[[2]])
write.table(inf, file = export_inf, append = FALSE, quote = FALSE, sep = ",",
            eol = "\n", na = "NA", dec = ".", row.names = FALSE,
            col.names = TRUE, )

#SUP
sup=data.frame(PC=names(box[[3]]),sup=box[[3]])
write.table(sup, file = export_sup, append = FALSE, quote = FALSE, sep = ",",
            eol = "\n", na = "NA", dec = ".", row.names = FALSE,
            col.names = TRUE, )

#Transition matrix
loadings=t(as.matrix(box[[4]]))
loadings=cbind(row.names(loadings),loadings)
write.table(loadings, file = export_transfer_matrix, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NA", dec = ".", row.names = FALSE,
            col.names = TRUE )

#Mean
means=t(apply(errors_P,2,mean))
# bidouille GJ pour etre conforme au fichiers de sortie d'EB
means2=cbind(1, means)
write.table(means2, file = export_mean_vector, append = FALSE, quote = TRUE, sep = ",",
            eol = "\n", na = "NA", dec = ".", row.names = FALSE,
            col.names = TRUE )

