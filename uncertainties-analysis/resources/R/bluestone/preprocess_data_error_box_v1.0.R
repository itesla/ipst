##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : preprocess_data_error_box.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTestla online module
###   Function      : Preprocess input data for error boxes
###   Parameters    : data_gen  = generator equipment dataset
###                   data_load = load equipment dataset
###   Outputs       : errors_P  = data frame of forecasting errors for all equipments
###   Version       : 1.0
###   Date          : 06/10/2014
##############################################################################################
###   Example:
###   errors_P=import_error_box_data(gen_path = "Data/forecastsDiffGen2013.csv",load_path = "Data/forecastsDiffLoads2013.csv")
##############################################################################################


preprocess_error_box_data<-function(data_gen,data_load){

data_gen[,!(names(data_gen) %in% c("horizon"))]=as.data.frame(apply(data_gen[,!(names(data_gen) %in% c("horizon"))],MARGIN=2,FUN=as.numeric))
data_load[,!(names(data_load) %in% c("horizon"))]=as.data.frame(apply(data_load[,!(names(data_load) %in% c("horizon"))],MARGIN=2,FUN=as.numeric))


gen_P=data_gen[, (substr(names(data_gen),nchar(names(data_gen))-1,nchar(names(data_gen)))=="_P" | names(data_gen)=="datetime" | names(data_gen)=="horizon")]
load_P=data_load[,substr(names(data_load),nchar(names(data_load))-1,nchar(names(data_load)))=="_P" | names(data_load)=="datetime" | names(data_load)=="horizon"]


data_P=merge(gen_P,load_P,by=c("horizon", "datetime"))

#remove Nas
data_P[is.na(data_P)]=0


#Remove half observations
datetime=data_P$datetime[seq(1,nrow(data_P),2)]

date=as.POSIXct(datetime, origin="1970-01-01")

minute=substr(date,15,16)


demi_pas_datetime=datetime[minute=="00"]

for (i in 1:length(demi_pas_datetime)){

  time_stamp=demi_pas_datetime[i]

  if ((time_stamp-1800) %in% datetime){

    #Replace forecast
    data_P[data_P$horizon=="DACF" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]=(data_P[data_P$horizon=="DACF" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]+data_P[data_P$horizon=="DACF" & data_P$datetime==time_stamp,!(names(data_P) %in% c("horizon","forecastTime","datetime"))])/2

    #Replace measure
    data_P[data_P$horizon=="SN" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]=(data_P[data_P$horizon=="SN" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]+data_P[data_P$horizon=="SN" & data_P$datetime==time_stamp,!(names(data_P) %in% c("horizon","forecastTime","datetime"))])/2

    #Remove data 00
    data_P=data_P[data_P$datetime!=time_stamp,]

  }


}

#Remove remaining half obs
datetime=data_P$datetime
date=as.POSIXct(datetime, origin="1970-01-01")
minute=substr(date,15,16)
data_P=data_P[minute=="30",]

data_P=data_P[order(data_P$datetime),]

errors_P=data_P[data_P$horizon=="DACF",!names(data_P)%in%c("datetime","horizon")]-data_P[data_P$horizon=="SN",!names(data_P)%in%c("datetime","horizon")]


return(errors_P)
}
