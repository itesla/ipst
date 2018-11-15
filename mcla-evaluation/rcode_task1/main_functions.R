################################################################################
## Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       ##
## https://www.inesctec.pt)                                                   ##
## This Source Code Form is subject to the terms of the Mozilla Public        ##
## License, v. 2.0. If a copy of the MPL was not distributed with this        ##
## file, You can obtain one at http://mozilla.org/MPL/2.0/.                   ##
################################################################################
## Authors: Carla Gonçalves and Helena Vasconcelos                           ##
################################################################################

################################################################################
#-------------------------- THIS IS THE MAIN SCRIPT ---------------------------#
#-------------------------    TO PERFORM TASK 1    ----------------------------#
################################################################################

# ---------------------------------------------------------------------------- #
#                               CODE CONTENTS                                  #
# ---------------------------------------------------------------------------- #
# 1. Libraries required
# 2. Read data
# 3. Mean distances: location and dispersion metrics
# 4. Security classification for SN, DACF and FO
# 5. Bootstrap functions
# 6. Auxiliar functions
################################################################################

# ----------------------------------
#   1. Libraries required 
# ----------------------------------

# Run the next comment line to install the packages:
# install.packages(c( 'xtable','reshape2','ggplot2','latex2exp',
#                     'gridExtra','grid','lubridate',
#                     'DescTools','doParallel','data.table'))
list.of.packages <- c('xtable','reshape2','ggplot2','latex2exp',
                      'gridExtra','grid','lubridate',
                      'DescTools','doParallel','data.table')
new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)) install.packages(new.packages)

suppressMessages(require(fields))
suppressMessages(require(SpecsVerification))
suppressMessages(require(xtable))
suppressMessages(require(fields)) # it is used to compute distances
suppressMessages(require(xtable)) # usefull to construct the pdf tables
suppressMessages(require(reshape2)) # to change dataframe structure
suppressMessages(require(ggplot2)) # to plot the results
suppressMessages(require(latex2exp)) # to plot mathematical formula in axis
suppressMessages(require(gridExtra))
suppressMessages(require(grid))
suppressMessages(require(lubridate))
suppressMessages(require(DescTools))
suppressMessages(require(doParallel))
suppressMessages(require(data.table))

options(datatable.fread.datatable=FALSE)
rm(list.of.packages,new.packages)

# -----------------------------------
#   2. Read data
# -----------------------------------

read.data.I <- function(path_data,selected.vars=NULL,
                               date.ini=NULL,date.fin=NULL,
                               max.ensemble){
    #
    # ---Function to read initial data in order to use its for the analysis---
    #
    # ---------> INPUTS <---------
    # - MANDATORY -
    # paths_data: path for the folder with the .csv data files 
    # max.ensemble: maximum number of ensembles in data files
    # - OPTIONAL -
    # selected.vars: to choose just some variables (default=NULL)
    # date.ini and date.fin to define time window to be analyzed
    # ---------> OUTPUTS <---------
    # List containing:
    # 1) data
    
    # SOME OUTPUT DETAILS
    # (ABOUT 1):
    # data has the following format:
    # data$"variable name" containing a matrix [i,j] where
    # i - timestamp
    # j - ensemble j, the last line is the observed
    
    # Check available .csv files in path_data:
    temp = list.files(path=path_data,pattern="*.csv",full.names=TRUE,recursive=T)
    
    temp. <- basename(temp)
    
    y <- substr(temp.,4,7)#substr(temp,nchar(temp)-16,nchar(temp)-13) # year
    m <- substr(temp.,8,9)#substr(temp,nchar(temp)-12,nchar(temp)-11) # month
    d <- substr(temp.,10,11)#substr(temp,nchar(temp)-10,nchar(temp)-9) #day
    
    H <- substr(temp.,13,14) #substr(temp,nchar(temp)-7,nchar(temp)-6) #hour
    M <- substr(temp.,15,16)#substr(temp,nchar(temp)-5,nchar(temp)-4) #minutes
    
    dates <- sprintf('%s-%s-%s %s:%s:00',y,m,d,H,M)
    dates <- as.POSIXct(dates, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    date.ini <- as.POSIXct(date.ini, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    date.fin <- as.POSIXct(date.fin, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    if (is.null(date.ini)||is.na(date.ini)){date.ini <- min(dates)}
    if (is.null(date.fin)||is.na(date.fin)){date.fin <- max(dates)}
    
    pos.dates.to.consider <- which(dates>=date.ini & dates<=date.fin)
    temp <- temp[pos.dates.to.consider]
    temp. <- temp.[pos.dates.to.consider]
    
    variables.in.files <- c() # array that saves the branch names
    
    if (!is.null(selected.vars)){
      selected.vars<- sprintf('%s_I',selected.vars)
    }
    
    
    y <- substr(temp.,4,7) # year
    m <- substr(temp.,8,9) # month
    d <- substr(temp.,10,11) # day
    
    H <- substr(temp.,13,14) # hour
    M <- substr(temp.,15,16) # minutes
    
    dates <- sprintf('%s-%s-%s %s:%s:00',y,m,d,H,M)
    dates <- as.POSIXct(dates, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    # Once all the variables in the files are known, 
    # start reading all the data in a robust structure, taking into account:
    # -> if for a timestamp t, the variable "VARIABLE" is not available,
    # then B$"VARIABLE"[t,:] = NA
    
    df <- fread(temp[1],check.names=TRUE)
    nr.ensembles <- max.ensemble
    
    df.empty <- data.frame(matrix(NA, length(dates),nr.ensembles+1))
    row.names(df.empty) <- sort(dates)
    names(df.empty) <- c('DACF',sprintf('ensemble %s',1:(nr.ensembles-1)),'SN')

    df.Imax.aux <- data.frame(matrix(NA, length(dates),1))
    row.names(df.Imax.aux) <- sort(dates)
    
    df.Imax <- data.frame(matrix(NA, length(dates),1))
    row.names(df.Imax) <- sort(dates)
    names(df.Imax) <- c('ignore.this')
    
    df.kV <- data.frame(matrix(NA,1,1))
    names(df.kV) <- c('ignore.this')
    
    B <- list()
    variables.in.files <- c()
    for (i in 1:length(temp)){ # for each data file
        cat(round(i/length(temp),2)*100,' % \r')
        
        df <- fread(temp[i],check.names=TRUE)# read the file
        names(df)[1] <- 'state'
        df_ <- subset(df, select=-c(state)) # remove the state column
        # check if a new variable appears in this file:
        pos.new <- which(names(df_)%in%variables.in.files==0) 
        
        kV <- df_[1,]
        
        if (length(pos.new)>0){ # if some new variable appears:
            variables.in.files <- c(variables.in.files,
                                    names(df_)[pos.new]) # save it name
            
            # Check if it is electric current (I) or Active Power (P)
            P.or.I <- substr(names(df_)[pos.new],nchar(names(df_)[pos.new])-1,nchar(names(df_)[pos.new]))
            # Check the columns with electric current
            I.columns <- which(P.or.I=='_I')
            
            for (new.cols in I.columns){
                    B[[names(df_)[pos.new][new.cols]]] <- df.empty
                    aux <- df.Imax.aux
                    names(aux) <- names(df_)[pos.new][new.cols]
                    df.Imax <- cbind(df.Imax,aux)
                    rm(aux)
                    aux <- data.frame(kV[names(df_)[pos.new][new.cols]])
                    names(aux) <- names(df_)[pos.new][new.cols]
                    df.kV <- cbind(df.kV,aux)
            }
            
        }
        
        # Check if it is electric current (I) or Active Power (P)
        P.or.I <- substr(names(df),nchar(names(df))-1,nchar(names(df)))
        # Check the columns with electric current
        I.columns <- which(P.or.I=='_I')
        
        # Select just "I" data
        df <- subset(df,select=names(df)[c(1,I.columns)])
        
        df.<-as.matrix(df) 
        df.<-t(t(df.)/(df.[2,])) 
        df. <- data.frame(df.)
        df. <- df.[-c(1,2),] #remove first line (which have information for the maximum)
        df_ <- subset(df., select=-c(state)) # remove the state column
        
        Imax <- df[2,]
        df <- df[-c(1,2),]
        
        
        if (is.null(selected.vars)){ # and does not select specific variables:
            for (col_ in 1:ncol(df_)){
                name. <- names(df_)[col_]
                new.row <- c(df_[df$state!=-1,col_],df_[df$state==-1,col_])
                
                if (sum(abs(new.row))>(10^(-5))){
                    # NEW PART
                    if (length(new.row)<(nr.ensembles+1)){
                        new.row <- c(new.row[1:length(new.row)],rep(NA,nr.ensembles-length(new.row)),new.row[length(new.row)])
                    }
                    # END NEW PART
                    if (sum(abs(new.row),na.rm=T)>(10^(-5))){
                        B[name.][[1]][sort(dates)==dates[i],] <- new.row
                        df.Imax[sort(dates)==dates[i],name.] <- Imax[names(df)==name.]
                        }
                    
                }
            }
        } else{ # if we select specific variables:
            for (col_ in 1:length(selected.vars)){
                name. <- selected.vars[col_]
                if (sum(names(df_)%in%name.)>0){
                    new.row <- c(df_[df$state!=-1,name.],df_[df$state==-1,name.])
                    if (sum(abs(new.row))>(10^(-5))){
                        # NEW PART
                        if (length(new.row)<(nr.ensembles+1)){
                            new.row <- c(new.row[1:length(new.row)],rep(NA,nr.ensembles-length(new.row)),new.row[length(new.row)])
                        }
                        # END NEW PART
                        B[name.][[1]][sort(dates)==dates[i],] <- new.row
                    }
                }
            }
        }
        
    }
    rm(df,df_,df.emty)
    
    return(list(data.normalized=B,df.Imax=df.Imax,df.kV=df.kV))
}

# -----------------------------------
#  3. Mean distances: location and dispersion metrics
# -----------------------------------
Euclidean.distance.overloads = function(B,p=0.05,all.situations=TRUE,SM=0,cl.to.use=1){
    #
    # --- Function to compute distances between SN and uncertainty ensemble ---
    #
    # all.situations: if TRUE then the metrics are computed for SN,DACF and Prob.Forecast
    # otherwise, just for Prob.Forecast
    # p: quantile 1-p to be considered in ensemble
    # SM: margin to consider in DACF
    # cl.to.use: parallel computation
    
    var.names <- names(B) # variable names
    
    nb.timestamps <- nrow(B[[1]]) # number of timestamps in input data
    nb.variables <- length(B) # number of variables in input data
    n = ncol(B[[1]])-1 # number of ensembles for each timestamp
    
    run.parallel <- function(var){ # for each variable
        # compute the minimum ensemble value for each timestamp
        min.value = as.numeric(apply(B[[var]][,1:n],1,min,na.rm=TRUE))
        # compute the maximum ensemble value for each timestamp
        max.value = as.numeric(apply(B[[var]][,1:n],1,quantile,probs=1-p,na.rm=TRUE))
        
        if(all.situations){
            SN <- as.numeric(B[[var]][,n+1])
            DACF <- as.numeric((1+SM)*B[[var]][,1])
            
            # overload SN distance:
            euclidean.distance.SN <- data.frame(x=SN)
            names(euclidean.distance.SN) <- names(B)[var]
            row.names(euclidean.distance.SN) <- row.names(B[[1]])
            # overload DACF distance:
            euclidean.distance.DACF <- data.frame(x=DACF)
            names(euclidean.distance.DACF) <- names(B)[var]
            row.names(euclidean.distance.DACF) <- row.names(B[[1]])
            
        }
        # overload DACF distance:
        euclidean.distance.Qsup <- data.frame(x=max.value)
        names(euclidean.distance.Qsup) <- names(B)[var]
        row.names(euclidean.distance.Qsup) <- row.names(B[[1]])
        
        if(all.situations){
        return(cbind(euclidean.distance.SN,
                     euclidean.distance.DACF,
                     euclidean.distance.Qsup))
        } else{
            return(euclidean.distance.Qsup)       
        }
        
    }
    
    cl <- makeCluster(cl.to.use) # because it makes more memory by core
    registerDoParallel(cl)
    results <- foreach(i=1:nb.variables) %dopar% run.parallel(i)
    stopCluster(cl)
    
    if(all.situations){
        euclidean.distance.SN <- lapply(results, `[`, 1)
        euclidean.distance.DACF <- lapply(results, `[`, 2)
        euclidean.distance.SN=do.call("cbind",euclidean.distance.SN)
        euclidean.distance.DACF=do.call("cbind",euclidean.distance.DACF)
        euclidean.distance.Qsup <- lapply(results, `[`, 3)
        euclidean.distance.Qsup=do.call("cbind",euclidean.distance.Qsup)
    } else{
        euclidean.distance.Qsup <- lapply(results, `[`, 1)
        euclidean.distance.Qsup=do.call("cbind",euclidean.distance.Qsup)
    }
    
    
    
    if(all.situations){
        euclidean.distance.SN <- as.data.frame(euclidean.distance.SN)
        names(euclidean.distance.SN) <- var.names
        row.names(euclidean.distance.SN) <- row.names(B[[1]])
        euclidean.distance.DACF <- as.data.frame(euclidean.distance.DACF)
        names(euclidean.distance.DACF) <- var.names
        row.names(euclidean.distance.DACF) <- row.names(B[[1]])
        
        euclidean.distance.SN[!is.finite(as.matrix(euclidean.distance.SN))] <- NA
        euclidean.distance.DACF[!is.finite(as.matrix(euclidean.distance.DACF))] <- NA
    }
    euclidean.distance.Qsup <- as.data.frame(euclidean.distance.Qsup)
    names(euclidean.distance.Qsup) <- var.names
    row.names(euclidean.distance.Qsup) <- row.names(B[[1]])
    euclidean.distance.Qsup[!is.finite(as.matrix(euclidean.distance.Qsup))] <- NA
    
    
    ##############################
    # Metrics
    ##############################
     
    if(all.situations){
        df.load = as.data.frame(cbind(colMeans(euclidean.distance.SN,na.rm = T),
                               apply(euclidean.distance.SN, 2, sd,na.rm = T),
                               apply(euclidean.distance.SN, 2, min,na.rm = T),
                               apply(euclidean.distance.SN, 2, max,na.rm = T),
                               colMeans(euclidean.distance.DACF,na.rm = T),
                               apply(euclidean.distance.DACF, 2, sd,na.rm = T),
                               apply(euclidean.distance.DACF, 2, min,na.rm = T),
                               apply(euclidean.distance.DACF, 2, max,na.rm = T),
                               colMeans(euclidean.distance.Qsup,na.rm = T),
                               apply(euclidean.distance.Qsup, 2, sd,na.rm = T),
                               apply(euclidean.distance.Qsup, 2, min,na.rm = T),
                               apply(euclidean.distance.Qsup, 2, max,na.rm = T)))
        names(df.load) = c('Mean SN','Std SN','min SN','max SN',
                      'Mean DACF','Std DACF','min DACF','max DACF',
                      'Mean Q(1-p)','Std Q(1-p)','min Q(1-p)','max Q(1-p)')
        row.names(df.load) = var.names
        df.load[!is.finite(as.matrix(df.load))] <- NA
        
        euclidean.distance.SN.overload <- matrix(NA,nrow(euclidean.distance.SN),ncol(euclidean.distance.SN))
        euclidean.distance.SN.overload[(euclidean.distance.SN>1)&(!is.na(euclidean.distance.SN))] <- euclidean.distance.SN[(euclidean.distance.SN>1)&(!is.na(euclidean.distance.SN))]-1
        euclidean.distance.SN.overload <- data.frame(euclidean.distance.SN.overload)
        names(euclidean.distance.SN.overload) <- names(euclidean.distance.SN)
        
        euclidean.distance.DACF.overload <- matrix(NA,nrow(euclidean.distance.DACF),ncol(euclidean.distance.DACF))
        euclidean.distance.DACF.overload <- data.frame(euclidean.distance.DACF.overload)
        euclidean.distance.DACF.overload[(euclidean.distance.DACF>1)&(!is.na(euclidean.distance.DACF))] <- euclidean.distance.DACF[(euclidean.distance.DACF>1)&(!is.na(euclidean.distance.DACF))]-1
        names(euclidean.distance.DACF.overload) <- names(euclidean.distance.DACF)
        
    } else{
        df.load = as.data.frame(cbind(colMeans(euclidean.distance.Qsup,na.rm = T),
                                      apply(euclidean.distance.Qsup, 2, sd,na.rm = T),
                                      apply(euclidean.distance.Qsup, 2, min,na.rm = T),
                                      apply(euclidean.distance.Qsup, 2, max,na.rm = T)))
        names(df.load) = c('Mean Q(1-p)','Std Q(1-p)','min Q(1-p)','max Q(1-p)')
        row.names(df.load) = var.names
        df.load[!is.finite(as.matrix(df.load))] <- NA
    }
    
    euclidean.distance.Qsup.overload <- matrix(NA,nrow(euclidean.distance.Qsup),ncol(euclidean.distance.Qsup))
    euclidean.distance.Qsup.overload[(euclidean.distance.Qsup>1)&(!is.na(euclidean.distance.Qsup))] <- euclidean.distance.Qsup[(euclidean.distance.Qsup>1)&(!is.na(euclidean.distance.Qsup))]-1
    euclidean.distance.Qsup.overload <- data.frame(euclidean.distance.Qsup.overload)
    names(euclidean.distance.Qsup.overload) <- names(euclidean.distance.Qsup)
    if(all.situations){
        df.overload = as.data.frame(cbind(colMeans(euclidean.distance.SN.overload,na.rm = T),
                                      apply(euclidean.distance.SN.overload, 2, sd,na.rm = T),
                                      apply(euclidean.distance.SN.overload, 2, min,na.rm = T),
                                      apply(euclidean.distance.SN.overload, 2, max,na.rm = T),
                                      colMeans(euclidean.distance.DACF.overload,na.rm = T),
                                      apply(euclidean.distance.DACF.overload, 2, sd,na.rm = T),
                                      apply(euclidean.distance.DACF.overload, 2, min,na.rm = T),
                                      apply(euclidean.distance.DACF.overload, 2, max,na.rm = T),
                                      colMeans(euclidean.distance.Qsup.overload,na.rm = T),
                                      apply(euclidean.distance.Qsup.overload, 2, sd,na.rm = T),
                                      apply(euclidean.distance.Qsup.overload, 2, min,na.rm = T),
                                      apply(euclidean.distance.Qsup.overload, 2, max,na.rm = T)))
        names(df.overload) = c('Mean SN (overload)','Std SN (overload)','min SN (overload)','max SN (overload)',
                           'Mean DACF (overload)','Std DACF (overload)','min DACF (overload)','max DACF (overload)',
                           'Mean Q(1-p) (overload)','Std Q(1-p) (overload)','min Q(1-p) (overload)','max Q(1-p) (overload)')
        row.names(df.overload) = var.names
        df.overload[!is.finite(as.matrix(df.overload))] <- NA
        
        return(list(distances.summary.load=df.load,
                    distances.summary.overload=df.overload,
                    euclidean.distance.SN=euclidean.distance.SN,
                    euclidean.distance.DACF=euclidean.distance.DACF,
                    euclidean.distance.Qsup=euclidean.distance.Qsup,
                    euclidean.distance.SN.overload=euclidean.distance.SN.overload,
                    euclidean.distance.DACF.overload=euclidean.distance.DACF.overload,
                    euclidean.distance.Qsup.overload=euclidean.distance.Qsup.overload
        ))
    } else{
        df.overload = as.data.frame(cbind(colMeans(euclidean.distance.Qsup.overload,na.rm = T),
                                          apply(euclidean.distance.Qsup.overload, 2, sd,na.rm = T),
                                          apply(euclidean.distance.Qsup.overload, 2, min,na.rm = T),
                                          apply(euclidean.distance.Qsup.overload, 2, max,na.rm = T)))
        names(df.overload) = c('Mean Q(1-p) (overload)','Std Q(1-p) (overload)','min Q(1-p) (overload)','max Q(1-p) (overload)')
        row.names(df.overload) = var.names
        df.overload[!is.finite(as.matrix(df.overload))] <- NA
        return(list(distances.summary.load=df.load,
                    distances.summary.overload=df.overload,
                    euclidean.distance.Qsup=euclidean.distance.Qsup,
                    euclidean.distance.Qsup.overload=euclidean.distance.Qsup.overload
        ))
    }
    
}

# -----------------------------------
#  4. Security classification for SN, DACF and FO
# -----------------------------------

# Security classification
security.classification <- function(B,c,SM=0,cl.to.use=1){
    # B - data
    # c - cut-off value for probabilistic rule
    n.vars <- length(B)
    n.timestamps  <- nrow(B[[1]])
    
    
    # For each variable check the SN, DACF and quantile c position, in relation
    # with Imax=1 (because normalization was performed)
    run.parallel <- function(var){
        # SN classification: 0=secure; 1=insecure
        SN.classification <- matrix(NA,n.timestamps,1)
        # SN classification: 0=secure; 1=insecure
        DACF.classification <- matrix(NA,n.timestamps,1)
        # FO probabilistic classification: 0=secure; 1=insecure
        FOprob.classification <- matrix(NA,n.timestamps,1)
        # FO probabilistic percentage members unsecure
        FOprob.unsecure.members <- matrix(NA,n.timestamps,1)
        
        SN.classification[B[[var]]$SN>1,1] = 1
        SN.classification[B[[var]]$SN<=1,1] = 0
        DACF.classification[((1+SM)*B[[var]]$DACF)>1,1] = 1
        DACF.classification[((1+SM)*B[[var]]$DACF)<=1,1] = 0
        cut.values <- as.numeric(apply(B[[var]][,1:(ncol(B[[var]])-1)],1,quantile,probs=c,na.rm=T))
        FOprob.classification[cut.values>1,1] = 1
        FOprob.classification[cut.values<=1,1] = 0
        FOprob.unsecure.members[,1] <- as.numeric(rowSums((B[[var]][,1:(ncol(B[[var]])-1)])>=1,na.rm = T))/(ncol(B[[var]])-1)
        
        # convert in data frame and complete information
        SN.classification <- data.frame(SN.classification)
        names(SN.classification) <- names(B)[var]
        row.names(SN.classification) <- row.names(B[[1]])
        DACF.classification <- data.frame(DACF.classification)
        names(DACF.classification) <- names(B)[var]
        row.names(DACF.classification) <- row.names(B[[1]])
        FOprob.classification <- data.frame(FOprob.classification)
        names(FOprob.classification) <- names(B)[var]
        row.names(FOprob.classification) <- row.names(B[[1]])
        
        FOprob.unsecure.members[is.na(SN.classification)] <- NA
        FOprob.unsecure.members <- data.frame(FOprob.unsecure.members)
        names(FOprob.unsecure.members) <- names(B)[var]
        row.names(FOprob.unsecure.members)<- row.names(B[[1]])
        
        return(cbind(SN.classification,
                    DACF.classification,
                    FOprob.classification,
                    FOprob.unsecure.members
                    ))
    }
    
    cl <- makeCluster(cl.to.use) # because it makes more memory by core
    registerDoParallel(cl)
    
        results <- foreach(i=1:n.vars) %dopar% run.parallel(i)
    stopCluster(cl)
        
    SN.classification <- lapply(results, `[`, 1)
    DACF.classification <- lapply(results, `[`, 2)
    FOprob.classification <- lapply(results, `[`, 3)
    FOprob.unsecure.members <- lapply(results, `[`, 4)
    
    SN.classification=do.call("cbind",SN.classification)
    DACF.classification=do.call("cbind",DACF.classification)
    FOprob.classification=do.call("cbind",FOprob.classification)
    FOprob.unsecure.members=do.call("cbind",FOprob.unsecure.members)
    
    
    ######
    # Summarize BRANCH state
    ######
    
    # Classification errors: 
    # ucu - if the system is unsecure and is classified unsecure
    # scs - if the system is secure and is classified secure
    # ucs [MA] - if the system is unsecure and is classified secure
    # scu [FA] - if the system is secure and is classified unsecure
    
    # Errors in DACF by branch
    DACF.VAR <- DACF.classification+SN.classification
    DACF.VAR[DACF.VAR==2] <- "ucu"
    DACF.VAR[DACF.VAR==0] <- "scs"
    DACF.VAR[(DACF.VAR==1)&(DACF.classification==1)] <- 'scu [FA]'
    DACF.VAR[(DACF.VAR==1)&(DACF.classification==0)] <- 'ucs [MA]'
    
    DACF.VAR <- replace(DACF.VAR, TRUE, lapply(DACF.VAR, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
    
    # Errors in PROBABILISTIC FORECAST by branch
    FOprob.VAR <- FOprob.classification+SN.classification
    FOprob.VAR[FOprob.VAR==2] <- "ucu"
    FOprob.VAR[FOprob.VAR==0] <- "scs"
    FOprob.VAR[(FOprob.VAR==1)&(FOprob.classification==1)] <- 'scu [FA]'
    FOprob.VAR[(FOprob.VAR==1)&(FOprob.classification==0)] <- 'ucs [MA]'
    
    FOprob.VAR <- replace(FOprob.VAR, TRUE, lapply(FOprob.VAR, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
    
    ######
    # Summarize SYSTEM state
    ######
    # Errors by SN for the system (all branches)
    # How much secure and unsecure branches by timestamp?
    SN.classification.system <- data.frame(Secure=rowSums(SN.classification==0,na.rm=T),
                                           Unsecure=rowSums(SN.classification==1,na.rm=T))
    # System state: secure if all branches are secure; 
    # unsecure if at least one branch is unsecure
    SN.classification.system$SNsystemSTATE <- NA
    SN.classification.system$SNsystemSTATE[SN.classification.system$Unsecure>=1] <- 'Unsecure'
    SN.classification.system$SNsystemSTATE[(rowSums(!is.na(SN.classification))>0)&(SN.classification.system$Unsecure==0)] <- 'Secure'
    
    # Errors by DACF for the system (all branches)
    # How much secure and unsecure branches by timestamp?
    DACF.classification.system <- data.frame(ucu=rowSums(DACF.VAR=='ucu',na.rm=T),
                                             scs=rowSums(DACF.VAR=='scs',na.rm=T),
                                             scu=rowSums(DACF.VAR=='scu [FA]',na.rm=T),
                                             ucs=rowSums(DACF.VAR=='ucs [MA]',na.rm=T))
    
    DACF.classification.system$DACFsystemSTATE <- NA
    DACF.classification.system$DACFsystemSTATE[rowSums(DACF.classification.system[,c('ucu','scu')],na.rm=T)>=1] <- 'Unsecure'
    DACF.classification.system$DACFsystemSTATE[(rowSums(!is.na(DACF.classification))>0)&
                                                   (rowSums(DACF.classification.system[,c('ucu','scu')],na.rm=T)==0)] <- 'Secure'
    
    DACF.classification.system$DACFsystemSTATE[(DACF.classification.system$DACFsystemSTATE=='Unsecure')&
                                                   (SN.classification.system$SNsystemSTATE=='Unsecure')] <- 'ucu'
    DACF.classification.system$DACFsystemSTATE[(DACF.classification.system$DACFsystemSTATE=='Secure')&
                                                   (SN.classification.system$SNsystemSTATE=='Secure')] <- 'scs'
    DACF.classification.system$DACFsystemSTATE[(DACF.classification.system$DACFsystemSTATE=='Unsecure')&
                                                   (SN.classification.system$SNsystemSTATE=='Secure')] <- 'scu [FA]'
    DACF.classification.system$DACFsystemSTATE[(DACF.classification.system$DACFsystemSTATE=='Secure')&
                                                   (SN.classification.system$SNsystemSTATE=='Unsecure')] <- 'ucs [MA]'
    
    # Errors by DACF for the system (all branches)
    # How much secure and unsecure branches by timestamp?
    FOprob.classification.system <- data.frame(ucu=rowSums(FOprob.VAR=='ucu',na.rm=T),
                                               scs=rowSums(FOprob.VAR=='scs',na.rm=T),
                                               scu=rowSums(FOprob.VAR=='scu [FA]',na.rm=T),
                                               ucs=rowSums(FOprob.VAR=='ucs [MA]',na.rm=T))
    
    FOprob.classification.system$FOprobsystemSTATE <- NA
    FOprob.classification.system$FOprobsystemSTATE[rowSums(FOprob.classification.system[,c('ucu','scu')],na.rm=T)>=1] <- 'Unsecure'
    FOprob.classification.system$FOprobsystemSTATE[(rowSums(!is.na(FOprob.classification))>0)&
                                                       (rowSums(FOprob.classification.system[,c('ucu','scu')],na.rm=T)==0)] <- 'Secure'
    
    FOprob.classification.system$FOprobsystemSTATE[(FOprob.classification.system$FOprobsystemSTATE=='Unsecure')&
                                                       (SN.classification.system$SNsystemSTATE=='Unsecure')] <- 'ucu'
    FOprob.classification.system$FOprobsystemSTATE[(FOprob.classification.system$FOprobsystemSTATE=='Secure')&
                                                       (SN.classification.system$SNsystemSTATE=='Secure')] <- 'scs'
    FOprob.classification.system$FOprobsystemSTATE[(FOprob.classification.system$FOprobsystemSTATE=='Unsecure')&
                                                       (SN.classification.system$SNsystemSTATE=='Secure')] <- 'scu [FA]'
    FOprob.classification.system$FOprobsystemSTATE[(FOprob.classification.system$FOprobsystemSTATE=='Secure')&
                                                       (SN.classification.system$SNsystemSTATE=='Unsecure')] <- 'ucs [MA]'
    
    
    
    SN.classification[SN.classification==1] <- 'Unsecure'
    SN.classification[SN.classification==0] <- 'Secure'
    SN.classification <- replace(SN.classification, TRUE, lapply(SN.classification, factor, levels = c('Secure','Unsecure')))
    
    return(list(SN.classification.by.branch=SN.classification,
                DACF.classification.by.branch=DACF.VAR,
                FOprob.classification.by.branch=FOprob.VAR,
                FOprob.unsecure.members.by.branch=FOprob.unsecure.members,
                SN.classification.by.system=SN.classification.system,
                DACF.classification.by.system=DACF.classification.system,
                FOprob.classification.by.system=FOprob.classification.system))
}

# -----------------------------------
#  5. Bootstrap functions
# -----------------------------------
# breaking by blocks: each block is a week
bootstrap.week <- function(data,alpha.=0.05,interest.levels,n.boot.samples){
    # number of weeks:
    data <- na.omit(data)
    if (sum(interest.levels%in%unique(data$data))==0){# if there is no MA neither FA
        out <- rep(0,6)
    }    else 
    {
        if ((length(unique(data$data))==1) & (sum(interest.levels%in%unique(data$data))==1)){
            if (which(interest.levels%in%unique(data$data))==interest.levels[1]) out <- c(rep(1,3),rep(0,3))
            if (which(interest.levels%in%unique(data$data))==interest.levels[2]) out <- c(rep(0,3),rep(1,3))
        } else{
            nr.weeks <- length(unique(data$Week))
            R.s <- matrix(NA,n.boot.samples,length(interest.levels))
            for (i in 1:n.boot.samples){
                s <- sample(nr.weeks, nr.weeks, replace = TRUE)
                samples.b <- unlist(sapply(s, function(x) data$data[data$Week==unique(data$Week)[x]],simplify = "array"))
                for (interest.level in interest.levels){
                    mean.0 <- mean(data$data==interest.level,na.rm=T)
                    SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                    SE <- sd(samples.b==interest.level,na.rm=TRUE)/sqrt(sum(!is.na(data$data)))
                    mean. <- mean(samples.b==interest.level,na.rm=T)
                    R. <- (mean.-mean.0)/SE
                    # remove R with inf's
                    R. <- R.[SE!=0]
                    if (length(R.)>0) R.s[i,interest.levels==interest.level] <- R.
                }
            }
            qalpha1 <- apply(R.s,2,quantile,probs=alpha./2.,na.rm=T)
            qalpha2 <- apply(R.s,2,quantile,probs=1-alpha./2,na.rm=T)
            out <- c()
            for (interest.level in interest.levels){
                mean.0 <- mean(data$data==interest.level,na.rm=T)
                SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                out <- c(out,c(mean.0,mean.0-qalpha2[interest.levels==interest.level]*SE.0,
                               mean.0-qalpha1[interest.levels==interest.level]*SE.0))
            }
            out <- data.frame(t(out))
        }
    }
    names(out) <- c('meanFA','FA.IC.inf','FA.IC.sup','meanMA','MA.IC.inf','MA.IC.sup')
    out[out<0] <- 0
    out[out>1] <- 1
    
    return(out)
}

bootstrap.week.all <- function(data,alpha.=0.05,interest.levels,n.boot.samples){
    if (length(unique(na.omit(data$data)))==1){
        out <- matrix(0,length(interest.levels),3)
        out[which(interest.levels%in%unique(data$data)),] <- 1
    } else{
        data$data <- as.numeric(data$state)
        nr.weeks <- length(unique(data$Week))
        R.s <- matrix(NA,n.boot.samples,length(interest.levels))
        for (i in 1:n.boot.samples){
            s <- sample(nr.weeks, nr.weeks, replace = TRUE)
            samples.b <- unlist(sapply(s, function(x) data$data[data$Week==unique(data$Week)[x]],simplify = "array"))
            for (interest.level in interest.levels){
                mean.0 <- mean(data$data==interest.level,na.rm=T)
                SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                SE <- sd(samples.b==interest.level,na.rm=TRUE)/sqrt(sum(!is.na(data$data)))
                mean. <- mean(samples.b==interest.level,na.rm=T)
                R. <- (mean.-mean.0)/SE
                # remove R with inf's
                R. <- R.[SE!=0]
                if (length(R.)>0) R.s[i,interest.levels==interest.level] <- R.
            }
        }
        qalpha1 <- apply(R.s,2,quantile,probs=alpha./2,na.rm=T)
        qalpha2 <- apply(R.s,2,quantile,probs=1-alpha./2,na.rm=T)
        out <- c()
        for (interest.level in interest.levels){
            mean.0 <- mean(data$data==interest.level,na.rm=T)
            SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
            out <- rbind(out,c(mean.0,mean.0-qalpha2[interest.levels==interest.level]*SE.0,
                               mean.0-qalpha1[interest.levels==interest.level]*SE.0))
        }
        out <- data.frame(out)
    }
    names(out) <- c('est','lwr.ci','upr.ci')
    out[is.na(out)]<-0
    out[out<0] <- 0
    out[out>1] <- 1
    
    if (length(unique(data$data))==1){
        out[unique(data$data),] <- 1
    }
    
    return(out)
}


# breaking randomly: each block is a hour
bootstrap.random <- function(data,alpha.=0.05,interest.levels,n.boot.samples){
    # bootstrap to use in trade-off analysis: focusing MA and FA confidencial 
    # intervals
    out <- rep(0,6)
    data <- na.omit(data)
    if (sum(interest.levels%in%unique(data$data))==0){# if there is no MA neither FA
        out <- rep(0,6)
    }    else 
    {
        if ((length(unique(data$data))==1) & (sum(interest.levels%in%unique(data$data))==1)){
            if (which(interest.levels%in%unique(data$data))==interest.levels[1]) out <- c(rep(1,3),rep(0,3))
            if (which(interest.levels%in%unique(data$data))==interest.levels[2]) out <- c(rep(0,3),rep(1,3))
        } else{
            
            nr.weeks <- length(unique(data$Week))
            R.s <- matrix(NA,n.boot.samples,length(interest.levels))
            for (i in 1:n.boot.samples){
                s <- sample(length(data$data), length(data$data), replace = TRUE)
                samples.b <- data$data[s]
                for (interest.level in interest.levels){
                    mean.0 <- mean(data$data==interest.level,na.rm=T)
                    SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                    SE <- sd(samples.b==interest.level,na.rm=TRUE)/sqrt(sum(!is.na(data$data)))
                    mean. <- mean(samples.b==interest.level,na.rm=T)
                    R. <- (mean.-mean.0)/SE
                    # remove R with inf's
                    R. <- R.[SE!=0]
                    if (length(R.)>0) R.s[i,interest.levels==interest.level] <- R.
                }
            }
            qalpha1 <- apply(R.s,2,quantile,probs=alpha./2.,na.rm=T)
            qalpha2 <- apply(R.s,2,quantile,probs=1-alpha./2,na.rm=T)
            out <- c()
            for (interest.level in interest.levels){
                mean.0 <- mean(data$data==interest.level,na.rm=T)
                SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                out <- c(out,c(mean.0,mean.0-qalpha2[interest.levels==interest.level]*SE.0,
                               mean.0-qalpha1[interest.levels==interest.level]*SE.0))
            }
            out <- data.frame(t(out))
        }
    }
    out[out<0] <- 0
    out[out>1] <- 1
    names(out) <- c('meanFA','FA.IC.inf','FA.IC.sup','meanMA','MA.IC.inf','MA.IC.sup')
    
    return(out)
}


bootstrap.random.all <- function(data,alpha.=0.05,interest.levels,n.boot.samples){
    # bootstrap to use in general analysis: focusing ucu, scs, MA and FA 
    # confidencial intervals
    if (length(unique(na.omit(data$data)))==1){
        out <- matrix(0,length(interest.levels),3)
        out[which(interest.levels%in%unique(data$data)),] <- 1
    } else{
        data$data <- as.numeric(data$state)
        nr.weeks <- length(unique(data$Week))
        R.s <- matrix(NA,n.boot.samples,length(interest.levels))
        for (i in 1:n.boot.samples){
            s <- sample(length(data$data), length(data$data), replace = TRUE)
            samples.b <- data$data[s]
            for (interest.level in interest.levels){
                mean.0 <- mean(data$data==interest.level,na.rm=T)
                SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
                SE <- sd(samples.b==interest.level,na.rm=TRUE)/sqrt(sum(!is.na(data$data)))
                mean. <- mean(samples.b==interest.level,na.rm=T)
                R. <- (mean.-mean.0)/SE
                # remove R with inf's
                R. <- R.[SE!=0]
                if (length(R.)>0) R.s[i,interest.levels==interest.level] <- R.
            }
        }
        qalpha1 <- apply(R.s,2,quantile,probs=alpha./2,na.rm=T)
        qalpha2 <- apply(R.s,2,quantile,probs=1-alpha./2,na.rm=T)
        out <- c()
        for (interest.level in interest.levels){
            mean.0 <- mean(data$data==interest.level,na.rm=T)
            SE.0 <- sd(data$data==interest.level,na.rm=T)/sqrt(sum(!is.na(data$data)))
            out <- rbind(out,c(mean.0,mean.0-qalpha2[interest.levels==interest.level]*SE.0,
                               mean.0-qalpha1[interest.levels==interest.level]*SE.0))
        }
        out <- data.frame(out)
    }
    names(out) <- c('est','lwr.ci','upr.ci')
    out[is.na(out)]<-0
    out[out<0] <- 0
    out[out>1] <- 1
    
    if (length(unique(data$data))==1){
        out[unique(data$data),] <- 1
    }
    
    return(out)
}





# -----------------------------------
#  6. Auxiliar functions
# -----------------------------------

# Auxiliar plot function in order to produce side by side plots using ggplot
VAlignPlots <- function(...,
                        globalTitle = "",
                        keepTitles = FALSE,
                        keepXAxisLegends = FALSE,
                        nb.columns = 1,
                        title,
                        hts=c(0.8,0.5,0.5,0.7)) {
    # Retrieve the list of plots to align
    plots.list <- list(...)
    
    # Remove the individual graph titles if requested
    if (!keepTitles) {
        plots.list <- lapply(plots.list, function(x) x <- x)# + ggtitle(""))
        plots.list[[1]] <- plots.list[[1]] + ggtitle(globalTitle)
    }
    
    # Remove the x axis labels on all graphs, except the last one, if requested
    if (!keepXAxisLegends) {
        plots.list[1:(length(plots.list)-1)] <-
            lapply(plots.list[1:(length(plots.list)-1)],
                   function(x) x <- x + theme(axis.title.x = element_blank()))
    }
    
    # Builds the grobs list
    grobs.list <- lapply(plots.list, ggplotGrob)
    
    # Get the max width
    widths.list <- do.call(grid::unit.pmax, lapply(grobs.list, "[[", 'widths'))
    
    # Assign the max width to all grobs
    grobs.list <- lapply(grobs.list, function(x) {
        x[['widths']] = widths.list
        x})
    
    # Create the gtable and display it
    g <- grid.arrange(grobs = grobs.list, ncol = nb.columns, heights=hts, top=textGrob(title, gp=gpar(fontsize=9,font=8)))
    
    return(g)
}


# plot with pie charts - summary of error classification
plot.summary <- function(df.DACF,df.FOprob,title.,aux=NULL,p.quantiles,SM=0,bootstrap.random.ind){
    # df.DACF have the columns timestamps,week and state
    names(df.DACF) <- c('timestamps','Week','state')
    # df.FOprob have the columns timestamps,week and state
    names(df.FOprob) <- c('timestamps','Week','state')
    
    # fix the possible levels
    df.DACF$state <- factor(df.DACF$state, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu'))
    df.FOprob$state <- factor(df.FOprob$state, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu'))
    
    # a ) Pie chart considering all timestamps
    # -----
    # 1 . FO probabilistic
    # -----
    
    FOprob.geral <- unique(subset(df.FOprob,select=c("timestamps","state")))$state
    
    if (bootstrap.random.ind){
        ICs <- round(bootstrap.random.all(df.FOprob,alpha. = 0.05,interest.levels = 1:4,n.boot.samples = 3000),3)*100
    } else{
        df.FOprob.daily <- df.FOprob
        df.FOprob.daily$Week <- sprintf('Day %s Week %s of %s',strftime(df.FOprob.daily$timestamps,format="%d"),strftime(df.FOprob.daily$timestamps,format="%W"),strftime(df.FOprob.daily$timestamps,format="%Y")) 
        ICs <- round(bootstrap.week.all(df.FOprob.daily,alpha. = 0.05,interest.levels = 1:4,n.boot.samples = 3000),3)*100
    }
    
    ICs.prob <- as.vector(t(ICs))
    tt <- as.matrix(table(FOprob.geral))
    count.all <- data.frame(matrix(0,1,4))
    names(count.all) <- c("ucu","scs","scu [FA]","ucs [MA]")
    count.all[,rownames(tt)] <- as.numeric(tt)
    
    counts <- as.numeric(count.all)
    labels.1 <- paste0(round((counts/sum(counts))*100,1),"%","(",counts,") ")
    values.1= as.character(data.frame("#4F6228","#92D050","#FF9900","#C00000"))
    values.1[1:4]=c("#4F6228","#92D050","#FF9900","#C00000")
    names(values.1)=c(sprintf("ucu %s [%s,%s]%%",labels.1[1],as.numeric(ICs[4,2]),as.numeric(ICs[4,3])),
                      sprintf("scs %s [%s,%s]%%",labels.1[2],as.numeric(ICs[2,2]),as.numeric(ICs[2,3])),
                      sprintf("scu [FA] %s [%s,%s]%%",labels.1[3],as.numeric(ICs[1,2]),as.numeric(ICs[1,3])),
                      sprintf("ucs [MA] %s [%s,%s]%%",labels.1[4],as.numeric(ICs[3,2]),as.numeric(ICs[3,3])))
    
    names(count.all) <- names(values.1)
    subjects <- names(count.all)
    subjects <- subjects[count.all>0]
    counts <- as.numeric(count.all[count.all>0])
    
    table_labels <- data.frame(Subject=factor(subjects,levels=subjects[length(subjects):1]),
                               Count=counts,
                               cumulative=cumsum(counts),
                               midpoint=cumsum(counts)-(counts/2),
                               labels= paste0(round((counts/sum(counts))*100,1),"%","(",counts,") "))
    xx2<- 1.55+as.numeric(table_labels$Subject)*0.1
    p.FOprob <- ggplot(table_labels,aes(x='',y=Count,fill=Subject))+
        geom_bar(width=1,stat='identity')+
        coord_polar(theta='y',start=0)+
        theme_bw()+
        scale_colour_manual(name='', values=values.1)+
        scale_fill_manual(name="", values=values.1,
                          limits=names(values.1))+
        labs(x='',y='',title=sprintf('Uncertainty Forecast (using Q(%s))',1-p.quantiles),fill='Legend')+
        annotate("text", x=1.7, y=0, label= sprintf("Time period: from %s \nto %s",df.FOprob$timestamps[1],df.FOprob$timestamps[length(df.FOprob$timestamps)]))+
        theme(plot.title=element_text(hjust=0.5),
              legend.position="bottom",
              legend.title = element_text(hjust=0.5,face="bold",size=10),
              legend.text = element_text(size=10),
              axis.text = element_blank(),
              axis.ticks = element_blank(),
              panel.grid  = element_blank())+
        guides(fill=guide_legend(ncol=1))
    
    # -----
    # 2. DACF
    # -----
    DACF.geral <- unique(subset(df.DACF,select=c("timestamps","state")))$state
    
    tt <- as.matrix(table(DACF.geral))
    count.all <- data.frame(matrix(0,1,4))
    names(count.all) <- c("ucu","scs","scu [FA]","ucs [MA]")
    count.all[,rownames(tt)] <- as.numeric(tt) 
    if (bootstrap.random.ind){
        ICs <- round(bootstrap.random.all(df.DACF,alpha. = 0.05,interest.levels = 1:4,n.boot.samples = 3000),3)*100
    } else{
        df.DACF.daily <- df.DACF
        df.DACF.daily$Week <- sprintf('Day %s Week %s of %s',strftime(df.DACF.daily$timestamps,format="%d"),strftime(df.DACF.daily$timestamps,format="%W"),strftime(df.DACF.daily$timestamps,format="%Y")) 
        ICs <- round(bootstrap.week.all(df.DACF.daily,alpha. = 0.05,interest.levels = 1:4,n.boot.samples = 3000),3)*100
    }
    ICs.prob <- data.frame(t(c(ICs.prob,as.vector(t(ICs)))))
    names(ICs.prob) <- c('FA (Prob.)','CI.inf FA (Prob.)','CI.sup FA (Prob.)',
                         'scs (Prob.)','CI.inf scs (Prob.)','CI.sup scs (Prob.)',
                         'MA (Prob.)','CI.inf MA (Prob.)','CI.sup MA (Prob.)',
                         'ucu (Prob.)','CI.inf ucu (Prob.)','CI.sup ucu (Prob.)',
                         'FA (DACF)','CI.inf FA (DACF)','CI.sup FA (DACF)',
                         'scs (DACF)','CI.inf scs (DACF)','CI.sup scs (DACF)',
                         'MA (DACF)','CI.inf MA (DACF)','CI.sup MA (DACF)',
                         'ucu (DACF)','CI.inf ucu (DACF)','CI.sup ucu (DACF)')
    counts <- as.numeric(count.all)
    labels.2 <- paste0(round((counts/sum(counts))*100,1),"%","(",counts,") ")
    values.2= as.character(data.frame("#4F6228","#92D050","#FF9900","#C00000"))
    values.2[1:4]=c("#4F6228","#92D050","#FF9900","#C00000")
    names(values.2)=c(sprintf("ucu %s [%s,%s]%%",labels.2[1],as.numeric(ICs[4,2]),as.numeric(ICs[4,3])),
                      sprintf("scs %s [%s,%s]%%",labels.2[2],as.numeric(ICs[2,2]),as.numeric(ICs[2,3])),
                      sprintf("scu [FA] %s [%s,%s]%%",labels.2[3],as.numeric(ICs[1,2]),as.numeric(ICs[1,3])),
                      sprintf("ucs [MA] %s [%s,%s]%%",labels.2[4],as.numeric(ICs[3,2]),as.numeric(ICs[3,3])))
    names(count.all) <- names(values.2)
    subjects <- names(count.all)
    subjects <- subjects[count.all>0]
    counts <- as.numeric(count.all[count.all>0])
    
    table_labels <- data.frame(Subject=factor(subjects,levels=subjects[length(subjects):1]),
                               Count=counts,
                               cumulative=cumsum(counts),
                               midpoint=cumsum(counts)-(counts/2),
                               labels= paste0(round((counts/sum(counts))*100,1),"%","(",counts,") "))
    
    xx<- 1.55+as.numeric(table_labels$Subject)*0.1
    p.DACF <- ggplot(table_labels,aes(x='',y=Count,fill=Subject))+
        geom_bar(width=1,stat='identity')+
        coord_polar(theta='y',start=0)+
        theme_bw()+
        scale_colour_manual(name='', values=values.2)+
        scale_fill_manual(name="", values=values.2,
                          limits=names(values.2))+
        labs(x='',y='',title=sprintf('Deterministic Forecast (using %s %% SM)',round(SM*100,2)),fill='Legend')+
        annotate("text", x=1.7, y=0, label= sprintf("Time period: from %s \nto %s",df.FOprob$timestamps[1],df.FOprob$timestamps[length(df.FOprob$timestamps)]))+
        theme(plot.title=element_text(hjust=0.5),
              legend.position="bottom",
              legend.title = element_text(hjust=0.5,face="bold",size=10),
              legend.text = element_text(size=10),
              axis.text = element_blank(),
              axis.ticks = element_blank(),
              panel.grid  = element_blank())+
        guides(fill=guide_legend(ncol=1))
    
    
    
    p1 <- grid.arrange(p.DACF,p.FOprob,ncol=2)
    
    # b ) weekly classification errors
    # DACF
    df.week <- unique(subset(df.DACF,select=c("timestamps","state","Week")))
    df.week.summary.DACF1 <- c()
    df.week.summary.DACF2 <- c()
    
    for (w in sort(unique(df.week$Week))){
        df. <- df.week[df.week$Week==w,"state"]
        tt <- as.matrix(table(df.))
        tt.rel <- as.numeric(tt)/sum(as.numeric(tt))
        df.week.summary.DACF1 <- rbind(df.week.summary.DACF1,
                                       cbind(w,row.names(tt)))
        df.week.summary.DACF2 <- rbind(df.week.summary.DACF2,
                                       cbind(as.numeric(tt),tt.rel*100))
    }
    
    df.week.summary.DACF <- data.frame(Week=df.week.summary.DACF1[,1],error=df.week.summary.DACF1[,2],
                                       abs.value=df.week.summary.DACF2[,1],rel.value=df.week.summary.DACF2[,2])
    df.week.summary.DACF$abs.value <- round(df.week.summary.DACF$abs.value,0)
    data_table.DACF <- ggplot(df.week.summary.DACF, aes(x = Week, y = error,
                                                   label = format(abs.value, nsmall = 0)))+#, colour = error)) +
        geom_text(size = 2.5) + theme_bw() + scale_y_discrete(limits = c("ucu","scs","scu [FA]","ucs [MA]")) +
        theme(panel.grid.major = element_blank(), legend.position = "none",
              panel.border = element_blank(), axis.text.x = element_blank(),
              axis.ticks = element_blank(),plot.margin = unit(c(-0.5,1, 0, 0.5), "lines"),
              text=element_text(size=9)) + xlab(NULL) + ylab(NULL)
    
    
    df.week.summary.DACF <- df.week.summary.DACF[df.week.summary.DACF$abs.value>0,]
    
       
    
    hjust=0.5
    df.week.summary.DACF$error <- factor(df.week.summary.DACF$error, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu'))
    
    p3 <- ggplot(df.week.summary.DACF,aes(x=Week,y=rel.value,fill=error))+
        geom_bar(stat='identity')+
        scale_fill_manual(name='',values=c('scs'="#92D050",
                                                 'ucu'="#4F6228",
                                                 'scu [FA]'="#FF9900",
                                                 'ucs [MA]'="#C00000"),
                          limits = c("ucu","scs","scu [FA]","ucs [MA]"))+ 
        ylab('Relat. Frequency (%)')+
        ggtitle('Weekly classification errors provided by deterministic forecasts')+
        theme_bw()+
        #geom_text(aes(label = abs.value),fontface='bold', size = 3, hjust = hjust, vjust = 0, position ="stack")+
        theme(axis.title.x = element_blank(), 
              axis.text.x = element_blank(),
              #legend.position="bottom",
              legend.position="none",
              legend.key.size=unit(0.35, 'cm'),
              plot.title = element_text(hjust = 0, vjust=0,size=12),
              text=element_text(size=7))+
        guides(fill=guide_legend(ncol=4))
    
        
    # week
    df.week <- unique(subset(df.FOprob,select=c("timestamps","state","Week")))
    df.week.summary.FOprob1 <- c()
    df.week.summary.FOprob2 <- c()
    
    for (w in sort(unique(df.week$Week))){
        df. <- df.week[df.week$Week==w,"state"]
        tt <- as.matrix(table(df.))
        tt.rel <- as.numeric(tt)/sum(as.numeric(tt))
        df.week.summary.FOprob1 <- rbind(df.week.summary.FOprob1,
                                       cbind(w,row.names(tt)))
        df.week.summary.FOprob2 <- rbind(df.week.summary.FOprob2,
                                       cbind(as.numeric(tt),tt.rel*100))
    }
    
    df.week.summary.FOprob <- data.frame(Week=df.week.summary.FOprob1[,1],error=df.week.summary.FOprob1[,2],
                                       abs.value=df.week.summary.FOprob2[,1],rel.value=df.week.summary.FOprob2[,2])
    df.week.summary.FOprob$abs.value <- round(df.week.summary.FOprob$abs.value,0)
    df.week.summary.FOprob$error <- factor(df.week.summary.FOprob$error, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu'))
    
    data_table.FOprob <- ggplot(df.week.summary.FOprob, 
                                aes(x = Week, y = error,
                                    label = format(abs.value, nsmall = 0))) +
        geom_text(size = 2.5) + theme_bw() + 
        scale_y_discrete(limits = c("ucu","scs","scu [FA]","ucs [MA]")) +
        theme(panel.grid.major = element_blank(), legend.position = "none",
              panel.border = element_blank(), 
              axis.text.x = element_text(angle = 90, hjust = 1),
              plot.title = element_text(hjust = 0, vjust=0,size=12),
              axis.ticks.y=element_blank(),
              text=element_text(size=9),plot.margin = unit(c(-0.5,1, 0, 0.5), "lines")) + 
        xlab(NULL) + 
        ylab(NULL)
    hjust=0.5#+0.2*as.numeric(df.week.summary.DACF$error)
    df.week.summary.FOprob <- df.week.summary.FOprob[df.week.summary.FOprob$abs.value>0,]
    p4 <- ggplot(df.week.summary.FOprob,aes(x=Week,y=rel.value,fill=error))+
        geom_bar(stat='identity')+
        scale_fill_manual(name='',values=c('scs'="#92D050",
                                                 'ucu'="#4F6228",
                                                 'scu [FA]'="#FF9900",
                                                 'ucs [MA]'="#C00000"),
                          limits = c("ucu","scs","scu [FA]","ucs [MA]"))+ 
        ylab('Relat. Frequency (%)')+
        ggtitle('Weekly classification errors provided by uncertainty forecasts')+
        theme_bw()+
        theme(axis.text.x = element_blank(),
              axis.title.x = element_blank(),
              plot.title = element_text(hjust = 0, vjust=0,size=12),
              text=element_text(size=7),
              legend.position="none")

    # plot all together fixing positions and spaces
    if (!is.null(aux)){
        tt <- ttheme_default(colhead=list(fg_params = list(parse=TRUE)),
                             core = list(fg_params=list(cex = 0.8)),
                             colhead = list(fg_params=list(cex = 0.9)),
                             rowhead = list(fg_params=list(cex = 0.9)))
        aux.selected <- aux[,c("Nr.timestamps","mu","Min","Max","Nr.over.timestamps","mu (overload)","Min (overload)","Max (overload)")]
        names(aux.selected) <- c("N","Mean","Min","Max","N ","Mean ","Min ","Max ")
        
        tab <- tableGrob(aux.selected[1:3, 1:8], rows=row.names(aux.selected), theme=tt)
        header <- tableGrob(aux.selected[1, 1:2], rows=NULL, cols=c("Load (p.u.)", "Overload (p.u.)")) 
        jn <- combine(header[1,], tab, along=2)
        # change the relevant rows of gtable
        jn$layout[1:4 , c("l", "r")] <- list(c(2,6), c(5,9))
        
        lay <- rbind(c(1,1,2,2),
                    c(NA,3,3,NA),
                     c(4,4,4,4),
                     c(5,5,5,5),
                     c(6,6,6,6),
                     c(7,7,7,7))
        
        p.final <- grid.arrange(p.DACF,p.FOprob,jn,p3,data_table.DACF,p4,data_table.FOprob, 
                           layout_matrix = lay,
                           top=textGrob(title., gp=gpar(fontsize=9,font=8)),
                           heights = c(4, 1.1, 1,0.3,1,0.9))
        
    } 
    else{
        pp <- VAlignPlots(p3,data_table.DACF,p4,data_table.FOprob,
                          hts=c(0.2,0.04,0.2,0.12),
                          title='',
                          keepTitles=TRUE,
                          keepXAxisLegends=TRUE)
        p.final <- grid.arrange(p1,pp,ncol=1,top=textGrob(title., gp=gpar(fontsize=9,font=6)),heights=c(9,9))
    }
    
    return(list(p.final=p.final,metrics=ICs.prob))
    
}







