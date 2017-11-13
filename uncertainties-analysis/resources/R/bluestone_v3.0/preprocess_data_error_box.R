##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : preprocess_data_error_box.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Import and preprocess data files
###   Parameters    : gen_path  = generator equipment dataset path
###                   load_path = load equipment dataset path
###   Outputs       : data_P  = dataframe of forecasts and snapshot measures for all equipments
###   Version       : 3.0
###   Date          : 18/03/2015
##############################################################################################
###   Example:
###   data_P=preprocess_error_box_data(gen_path = "Data/forecastsDiffGen2013.csv",load_path = "Data/forecastsDiffLoads2013.csv")
##############################################################################################


preprocess_data<-function(gen_path,load_path){

  # Import library data.table
  library("data.table")

  # Import data
  data_gen<-as.data.frame(fread(gen_path,sep=",",header=TRUE,stringsAsFactors=F))
  data_load<-as.data.frame(fread(load_path,sep=",",header=TRUE,stringsAsFactors=F))

  # Convert to numeric
  data_gen[,!(names(data_gen) %in% c("horizon"))]=as.data.frame(apply(data_gen[,!(names(data_gen) %in% c("horizon"))],MARGIN=2,FUN=as.numeric))
  data_load[,!(names(data_load) %in% c("horizon"))]=as.data.frame(apply(data_load[,!(names(data_load) %in% c("horizon"))],MARGIN=2,FUN=as.numeric))

  # Consider generator convention (take the opposite of load values)
  data_load[,!(names(data_load) %in% c("horizon","datetime","forecastTime"))]= - data_load[,!(names(data_load) %in% c("horizon","datetime","forecastTime"))]

  # Select Active power data
  gen_P=data_gen[, (substr(names(data_gen),nchar(names(data_gen))-1,nchar(names(data_gen)))=="_P" | names(data_gen)=="datetime" | names(data_gen)=="horizon")]
  load_P=data_load[,substr(names(data_load),nchar(names(data_load))-1,nchar(names(data_load)))=="_P" | names(data_load)=="datetime" | names(data_load)=="horizon"]

  # Merge load and gen datasets
  data_P=merge(gen_P,load_P,by=c("horizon", "datetime"))


  # Remove half observations
  datetime=data_P$datetime[seq(1,nrow(data_P),2)]
  date=as.POSIXct(datetime, origin="1970-01-01")
  minute=substr(date,15,16)
  demi_pas_datetime=datetime[minute=="00"]

  for (i in 1:length(demi_pas_datetime)){

    time_stamp=demi_pas_datetime[i]

    if ((time_stamp-1800) %in% datetime){

      # Replace forecast
      data_P[data_P$horizon=="DACF" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]=(data_P[data_P$horizon=="DACF" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]+data_P[data_P$horizon=="DACF" & data_P$datetime==time_stamp,!(names(data_P) %in% c("horizon","forecastTime","datetime"))])/2

      # Replace measure
      data_P[data_P$horizon=="SN" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]=(data_P[data_P$horizon=="SN" & data_P$datetime==(time_stamp-1800),!(names(data_P) %in% c("horizon","forecastTime","datetime"))]+data_P[data_P$horizon=="SN" & data_P$datetime==time_stamp,!(names(data_P) %in% c("horizon","forecastTime","datetime"))])/2

      # Remove data 00
      data_P=data_P[data_P$datetime!=time_stamp,]}}

  # Remove remaining half obs
  datetime=data_P$datetime
  date=as.POSIXct(datetime, origin="1970-01-01")
  minute=substr(date,15,16)
  data_P=data_P[minute=="30",]


  #Put observation below 1e-7 at 0
  temp=data_P[,!(names(data_P) %in% c("horizon","forecastTime","datetime"))]
  temp[abs(temp)<1e-7]=0
  data_P=cbind(data_P[,(names(data_P) %in% c("horizon","forecastTime","datetime"))],temp)

  # Sort data
  data_P=data_P[order(data_P$datetime),]

  return(data_P)
}
