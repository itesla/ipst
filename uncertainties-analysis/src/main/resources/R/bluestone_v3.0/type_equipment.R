##############################################################################################
###   Copyright (c) 2017, RTE (http://www.rte-france.com)
###   This Source Code Form is subject to the terms of the Mozilla Public
###   License, v. 2.0. If a copy of the MPL was not distributed with this
###   file, You can obtain one at http://mozilla.org/MPL/2.0/.
###
###   Program name  : type_equipment.R
###   Author        : Cedric Faucheux (Bluestone)
###   Project       : RTE prototype for the iTesla online module
###   Function      : Find the type of an equipment (load, gen or undetermined) based on the sign of the measures
###   Parameters    : values_equip = vector of equipment measures
###                   gen_floor    = minimum rate of positive values for an equipment to be considered a generator
###                   load_floor   = minimum rate of negative values for an equipment to be considered a load
###   Outputs       : gen = if gen is equal to 1, the equipment is a generator equipment
###                   load = if load is equal to 1, the equipment is a load equipment
###                   if both load and gen are equal to zero, the type of equipment cannot be determined
###   Version       : 3.0
###   Date          : 18/03/2015
##############################################################################################
###   Example:
###   all_equip_type=apply(data_meas,2,FUN=typeEquipmentPath,gen_floor=gen_floor,load_floor=load_floor)
##############################################################################################







ratesPositiveNegativeNul<-function(values_equip) {
  n=sum(!is.na(values_equip))
  if (n>0){
    ratesPositive=sum(values_equip>0,na.rm=TRUE)/n
    ratesNegative=sum(values_equip<0,na.rm=TRUE)/n
    ratesNul=sum(values_equip==0,na.rm=TRUE)/n
  }else{
    ratesPositive=NA
    ratesNegative=NA
    ratesNul=NA
  }
  result=c("ratesPositive"=ratesPositive,
           "ratesNegative"=ratesNegative,
           "ratesNul"=ratesNul)
  return(result)
}


typeEquipment<-function(values_equip,x,y) {
  #values_equip is a column representing an equipment
  n=sum(!is.na(values_equip))
  if(n>0){
    gen=as.numeric((sum(values_equip>=0,na.rm=TRUE)/n)>x)
    load=as.numeric((sum(values_equip<=0,na.rm=TRUE)/n)>y)
  }
  else{
    gen=NA
    load=NA
  }
  result=c("gen"=gen,"load"=load)
  return(result)
}


typeEquipmentPath<-function(values_equip,gen_floor,load_floor) {
  x=gen_floor
  y=load_floor

  #values_equip is a column representing an equipment
  ind=typeEquipment(values_equip,x,y)
  if(sum(!is.na(ind))==2){
    if((ind[1]+ind[2])==2){
      gen=as.numeric((sum(values_equip>0)/(sum(values_equip>0)+sum(values_equip<0)))>x)
      load=as.numeric((sum(values_equip<0)/(sum(values_equip>0)+sum(values_equip<0)))>y)
      if (is.na(gen)|is.na(load)){
        gen=0
        load=0
      }
      result=c("gen"=gen,"load"=load)
    }
    else
    {
      result=ind
    }
  }
  else
  {
    result=ind
  }
  return(result)
}
