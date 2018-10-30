################################################################################
## Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       ##
## https://www.inesctec.pt)                                                   ##
## This Source Code Form is subject to the terms of the Mozilla Public        ##
## License, v. 2.0. If a copy of the MPL was not distributed with this        ##
## file, You can obtain one at http://mozilla.org/MPL/2.0/.                   ##
################################################################################
## Authors: Carla Gonçalves, Helena Vasconcelos                              ##
################################################################################

################################################################################
#-------------------------- THIS IS THE MAIN SCRIPT ---------------------------#
#-------------------------    TO PERFORM TASK 3    ----------------------------#
################################################################################

# ---------------------------------------------------------------------------- #
#                               CODE CONTENTS                                  #
# ---------------------------------------------------------------------------- #
# 1. Libraries required
#
# 2. Read data
#   2.1 Read all data to define the ensemble and timestamp dimensions
#   2.2 Separate data by the active power flow signal
#
# 3. Univariate Analysis
#   3.1 Univariate Rank Histogram
#   3.2 Chi square and Watson p-values for Rank histogram
#   3.3 Delta Index
#   3.4 Mean distances and number of SN inside/outside ensemble
#   3.5 Spread
#   3.6 CRPS
#
# 4. Multivariate Analysis
#   4.1 Multivariate Rank Histogram
#   4.2 Energy Score
################################################################################

# ----------------------------------
#   1. Libraries required 
# ----------------------------------

# Run the next comment line to install the packages:
# install.packages(c('fields','SpecsVerification','xtable',
#                     'reshape2','ggplot2','latex2exp',
#                     'goftest','gridExtra','lubridate','png','doParallel'))

list.of.packages <- c('fields','SpecsVerification','xtable',
                      'reshape2','ggplot2','latex2exp',
                      'goftest','gridExtra','lubridate',
                      'png','doParallel')
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
suppressMessages(require(goftest))
suppressMessages(require(gridExtra))
suppressMessages(require(lubridate))
suppressMessages(require(doParallel))
rm(list.of.packages,new.packages)

# -----------------------------------
#   2. Read data
# -----------------------------------
# -----------------------------------
# 2.1 Read all data 
# -----------------------------------
read.data.function <- function(path_data,selected.vars=NULL,
                                date.ini=NULL,date.fin=NULL){
    #
    # ---Function to read initial data in order to use its for the analysis---
    #
    # ---------> INPUTS <---------
    # - MANDATORY -
    # paths_data: path for the folder with the .csv data files 
    # - OPTIONAL -
    # selected.vars: to choose just some variables (default=NULL)
    #
    # ---------> OUTPUTS <---------
    # List containing:
    # 1) data
    # 2) indicator.variables
    
    # SOME OUTPUT DETAILS
    # (ABOUT 1):
    # data has the following format:
    # data$"variable name" containing a matrix [i,j] where
    # i - timestamp
    # j - ensemble j, the last line is the observed
    #
    # (ABOUT 2):
    # indicator.variables has the following information:
    # each row represents a different branch
    # the columns have the information about:
    # _I: 0 if the branch does not appear in the files for the electric current, 1 otherwise;
    # _P: 0 if the branch does not appear in the files for the active power flow, 1 otherwise;
    # Imax: value to use in normalization  
    # Smax: value to use in normalization  
    # datestamps: the used timestamp to obtain the last two values
    
    # Check available .csv files in path_data:
    temp = list.files(path=path_data,pattern="*.csv",full.names=TRUE,recursive=T)
    
    y <- substr(temp,nchar(temp)-16,nchar(temp)-13) # year
    m <- substr(temp,nchar(temp)-12,nchar(temp)-11) # month
    d <- substr(temp,nchar(temp)-10,nchar(temp)-9) #day
    
    H <- substr(temp,nchar(temp)-7,nchar(temp)-6) #hour
    M <- substr(temp,nchar(temp)-5,nchar(temp)-4) #minutes
    
    dates <- sprintf('%s-%s-%s %s:%s:00',y,m,d,H,M)
    dates <- as.POSIXct(dates, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    date.ini <- as.POSIXct(date.ini, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    date.fin <- as.POSIXct(date.fin, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    if (is.null(date.ini)||is.na(date.ini)){date.ini <- min(dates)}
    if (is.null(date.fin)||is.na(date.fin)){date.fin <- max(dates)}
    
    pos.dates.to.consider <- which(dates>=date.ini & dates<=date.fin)
    temp <- temp[pos.dates.to.consider]
    
    
    y <- substr(temp,nchar(temp)-16,nchar(temp)-13) # year
    m <- substr(temp,nchar(temp)-12,nchar(temp)-11) # month
    d <- substr(temp,nchar(temp)-10,nchar(temp)-9) #day
    
    H <- substr(temp,nchar(temp)-7,nchar(temp)-6) #hour
    M <- substr(temp,nchar(temp)-5,nchar(temp)-4) #minutes
    
    dates <- sprintf('%s-%s-%s %s:%s:00',y,m,d,H,M)
    dates <- as.POSIXct(dates, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
    
    
    df <- read.csv(temp[1],sep=';')
    nr.ensembles <- nrow(df)-2
    
    df.empty <- data.frame(matrix(as.numeric(NA), length(dates),nr.ensembles+1))
    row.names(df.empty) <- sort(dates)
    names(df.empty) <- c('DACF',sprintf('ensemble %s',1:(nr.ensembles-1)),'SN')
    
    
    if (!is.null(selected.vars)){
        selected.vars<- c(sprintf('%s_P',selected.vars),sprintf('%s_I',selected.vars))
    }
    if (is.null(selected.vars)){
        B <- list()
    } else{
        B <- rep(list(df),length(selected.vars))
        names(B) <- sort(selected.vars)
    }
    
    rm(df)
    
    variables.in.files <- c() # array that saves the branch names
    normalization.value <- c() # array that saves the Imax and Smax values 
    timestamp.of.normalization.value <- c() # array that saves the time where 
    # the Imax/Smax was registered
    
    
    for (i in 1:length(temp)){ # for each data file
        cat(round(i/length(temp),2)*100,' % \r')
        
        df <- read.csv(temp[i],sep=';') # read the file
        df_ <- subset(df, select=-c(state)) # remove the state column
        # check if a new variable appears in this file:
        pos.new <- which(names(df_)%in%variables.in.files==0) 
        
        if (length(pos.new)>0){ # if some new variable appears:
            variables.in.files <- c(variables.in.files,
                                    names(df_)[pos.new]) # save it name
            normalization.value <- c(normalization.value,
                                     as.matrix(df_[1,pos.new])) # save the Imax and Smax values
            timestamp.of.normalization.value <- c(timestamp.of.normalization.value,
                                                  rep(i,length(pos.new))) # save the first timestamp where it appears
            if (is.null(selected.vars)){
                for (new.var in names(df_)[pos.new]){
                    B[[new.var]] = df.empty
                }
            } else{
                for (new.var in selected.vars[selected.vars%in%names(df_)[pos.new]]){
                    if (sum(selected.vars==new.var)==1) B[[new.var]] = df.empty
                }
            }
        }
        
        rm(df_)
        
        df <- df[-1,] #remove first line (which have information for the maximum)
        df_ <- subset(df, select=-c(state)) # remove the state column
        
        if (is.null(selected.vars)){ # and does not select specific variables:
            for (col_ in 1:ncol(df_)){
                name. <- names(df_)[col_]
                I.or.P <- substr(name.,nchar(name.)-1,nchar(name.))
                new.row <- c(df_[df$state!=-1,col_],df_[df$state==-1,col_])
                if (I.or.P=='_P'){ # if it is active power save
                    B[name.][[1]][sort(dates)==dates[i],] <- new.row/normalization.value[variables.in.files==name.]
                } else{ # if it is current save just if it is greater than 10^-5
                    if (sum(abs(new.row))>(10^(-5))){
                    B[name.][[1]][sort(dates)==dates[i],] <- new.row/normalization.value[variables.in.files==name.]
                    }
                }
            }
        } else{ # if we select specific variables:
            for (col_ in 1:length(selected.vars)){
                name. <- selected.vars[col_]
                I.or.P <- substr(name.,nchar(name.)-1,nchar(name.))
                if (sum(names(df_)%in%name.)>0){
                    new.row <- c(df_[df$state!=-1,name.],df_[df$state==-1,name.])
                    if (I.or.P=='_P'){ # if it is active power save
                        B[name.][[1]][sort(dates)==dates[i],] <- new.row/normalization.value[variables.in.files==name.]
                    } else{ # if it is current save just if it is greater than 10^-5
                        if (sum(abs(new.row))>(10^(-5))){
                        B[name.][[1]][sort(dates)==dates[i],] <- new.row/normalization.value[variables.in.files==name.]
                        }
                    }
                }
            }
        }
        
    }
    
    df.normalization <- data.frame(normalization.value)
    row.names(df.normalization) <- variables.in.files
    
    # Check variable names
    variable.names <- substr(variables.in.files,1,nchar(variables.in.files)-2)
    # Check if it is electric current (I) or Active Power (P)
    P.or.I <- substr(variables.in.files,nchar(variables.in.files)-1,nchar(variables.in.files))
    # Check the columns with Active Power
    P.columns <- which(P.or.I=='_P')
    # Check the columns with electric current
    I.columns <- which(P.or.I=='_I')
    
    # Create a indicator matrix where the columns are
    # each variable
    # and the rows indicate 
    # 1st line: variable registered for P
    # 2nd line: variable registered for I
    indicator.variables <- data.frame(matrix(0,5,max(length(unique(variable.names)))))
    names(indicator.variables) <- unique(variable.names)
    
    tstamps =  substr(temp,nchar(temp)-16,nchar(temp)-4)
    indicator.variables[2,variable.names[P.columns]]=1
    indicator.variables[1,variable.names[I.columns]]=1
    indicator.variables[4,variable.names[P.columns]]= normalization.value[P.columns]
    indicator.variables[3,variable.names[I.columns]]=normalization.value[I.columns]
    row.names(indicator.variables) <- c('_I','_P','Imax','Smax','timestamps')
    
    indicator.variables[5,variable.names[I.columns]]=as.character(dates[timestamp.of.normalization.value[I.columns]])
    indicator.variables[5,variable.names[P.columns]]=as.character(dates[timestamp.of.normalization.value[P.columns]])
    
    indicator.variables=data.frame(t(indicator.variables))
    indicator.variables$timestamp=dates[timestamp.of.normalization.value[I.columns]]
    
    return(list(data=B,
                indicator.variables=indicator.variables))
}

# -----------------------------------
# 2.2 Process data by power flow sign
# -----------------------------------

separate.data <- function(B,min.timestamps=100){
    # variables considered:
    variables.in.files <- names(B)
    # remove the terminal "_P" and "_I" of variable names:
    variable.names <- substr(variables.in.files,1,nchar(variables.in.files)-2)
    # find columns with active power(P) and columns with electri current (I)
    P.columns <- which(substr(names(B),nchar(names(B))-1,nchar(names(B)))=='_P')
    P.vars <- variable.names[P.columns]
    I.columns <- which(substr(names(B),nchar(names(B))-1,nchar(names(B)))=='_I')
    I.vars <- variable.names[I.columns]
    
    B.I <- B[variables.in.files[I.columns]] # information about electric current
    # list to save electric current associated with positive power flow
    B.I.pos <- B.I 
    # list to save electric current associated with negative power flow  
    B.I.neg <- B.I
    
    B.P <- B[variables.in.files[P.columns]] # information about active power
    
    
    for (var in 1:length(B.P)){ 
        name. <- names(B.P)[var]
        var.n <- substr(name.,1,nchar(name.)-2)
        I.var <- sprintf('%s_I',var.n)
        negative.P <- which((B.P[name.][[1]][,'SN']<0)&(!is.na(B.I[I.var][[1]][,'SN']))) # timestamps with P<0
        positive.P <- which((B.P[name.][[1]][,'SN']>=0)&(!is.na(B.I[I.var][[1]][,'SN']))) # timestamps with P>=0
        B.I.pos[I.var][[1]][negative.P,] <- as.numeric(NA)
        B.I.neg[I.var][[1]][positive.P,] <- as.numeric(NA)
        if (length(negative.P)<min.timestamps){ 
            B.I.neg[I.var][[1]][negative.P,] <- as.numeric(NA)
        }
        if (length(positive.P)<min.timestamps){
            B.I.pos[I.var][[1]][positive.P,] <- as.numeric(NA)
        }
    }
    
    # Eliminate variables without data:
    B.I.pos=B.I.pos[lapply(B.I.pos, sum,na.rm=T)!=0]
    
    B.I.neg=B.I.neg[lapply(B.I.neg, sum,na.rm=T)!=0]
    
    #B.I=B.I.neg[lapply(B.I, sum,na.rm=T)!=0]
    
    return(list(#data.P.pos=B.P.pos,data.P.neg=B.P.neg,
        data.I.pos=B.I.pos,data.I.neg=B.I.neg,
        data.I=B.I))
}


################################################################################
#                               UNIVARIATE ANALYSIS                            #
################################################################################
# -----------------------------------
# 3.1 Univariate Rank Histogram bins computation
# -----------------------------------

Talagrand.diagram = function(B){
    #
    # --- Function to compute the Univariate Rank Histogram bins ---
    #
    # Create the output matrix: 
    # for each timestamp (line of output)
    # and
    # for each variable (column of output)
    # we check the position of the SN 
    # in the ensemble
    rank.matrix <- matrix(0,nrow=nrow(B[[1]]),ncol=length(B))
    for (var in 1:length(B)){ # for each variable check the position
        # of the SN in relation to ensemble (in each timestamp):
        rank.matrix[,var] <- unname(round(apply(B[[var]],1,rank,na.last='keep')[ncol(B[[1]]),],0))
    }
    rank.matrix<- data.frame(rank.matrix)
    names(rank.matrix) <- names(B)
    
    # summary the rank histogram information (check the number of ocorrences of 
    # each SN position, for each variable)
    talagrand.csv = matrix(0,ncol(B[[1]]),length(names(rank.matrix))) 
    for(i in 1:length(names(rank.matrix))){
        a <- table(rank.matrix[,names(rank.matrix)[i]])
        talagrand.csv[as.numeric(names(a)),i] = as.matrix(a)
    }
    talagrand.csv=as.data.frame(talagrand.csv)
    names(talagrand.csv) <- names(rank.matrix)
    talagrand.csv <- rbind(colSums(talagrand.csv),talagrand.csv)
    talagrand.csv <- as.matrix(talagrand.csv)
    talagrand.csv[-1,] <- t(t(talagrand.csv[-1,])/talagrand.csv[1,])
    row.names(talagrand.csv) <- c('n.timestamps',sprintf('bin %s',1:(nrow(talagrand.csv)-1)))
    
    return(talagrand.csv)
}

# -----------------------------------
# 3.2 Chi square and Watson p-values for Rank histogram
# -----------------------------------

p.value.rank.hist <- function(talagrand.diagram,nr.ensembles){
    #
    # --- Function to compute the p-value associated with Univariate RH ---
    #
    
    matrix.of.ranks <- matrix(0,nrow=nr.ensembles+1, ncol=ncol(talagrand.diagram))
    
    for (i in 1:ncol(talagrand.diagram)){
        matrix.of.ranks[,i] <- round(talagrand.diagram[-1,i]*talagrand.diagram[1,i])
    }
    
    hip.test.chi <- apply(matrix.of.ranks,2,TestRankhist)
    
    hip.test.cvm <- apply(talagrand.diagram,2,function(x){
        goftest::cvm.test(x[!is.na(x)], 
                          null = "punif",min=1,
                          max=nr.ensembles+1)})
    
    p.values.chi <- rep(0,ncol(talagrand.diagram))
    p.values.cvm <- rep(0,ncol(talagrand.diagram))
    for (i in 1:ncol(talagrand.diagram)){
        p.values.chi[i] <- hip.test.chi[[i]]$pearson.chi2[2]
        p.values.cvm[i] <- hip.test.cvm[[i]]$p.value
    }
    p.values.chi <- as.data.frame(p.values.chi)
    row.names(p.values.chi) <- names(talagrand.diagram)
    p.values.cvm <- as.data.frame(p.values.cvm)
    row.names(p.values.cvm) <- names(talagrand.diagram)
    out <- cbind(p.values.chi,p.values.cvm)
    return(out)
} 

# -----------------------------------
# 3.3 Delta Index
# -----------------------------------

delta.index = function(talagrand.diagram,number.of.ensembles){
    #
    # --- Function to compute the Delta index associated with Univariate RH ---
    #
    delta.ind <- rep(0,ncol(talagrand.diagram))
    for (col_ in 1:ncol(talagrand.diagram)){ # for each variable
        empirical <- talagrand.diagram[-1,col_] 
        delta.ind[col_] <- sum(abs(empirical-rep(1/(number.of.ensembles+1),number.of.ensembles+1)))
    }
    delta.ind <- as.data.frame(delta.ind)/((2*number.of.ensembles)/(number.of.ensembles+1))
    row.names(delta.ind) <- names(talagrand.diagram)
    
    return(delta.ind)
}

# -----------------------------------
# 3.4 Mean distances and number of SN inside/outside ensemble
# and barplots
# -----------------------------------
#AUXILIAR
auxiliar.distance <- function(B.column){
    m <- as.matrix(dist(matrix(B.column,ncol=1)))
    return(mean(m[nrow(m),-nrow(m)]))
}

Euclidean.distance.SN_to_MCLA = function(B){
    #
    # --- Function to compute distances between SN and uncertainty ensemble ---
    #
    
    var.names <- names(B) # variable names
    # Matrix to save the indicator of position:
    outside.indicator <- matrix(-2, nrow=nrow(B[[1]]),ncol=length(B)) 
    # indicates if a SN is inside or outside MCLA ensemble
    # the sinal indicates the signal of (SN-MCLA)
    # -1: SN<FO
    #  1: SN>FO
    
    # Matrix to save the euclidean distances according SN and ensemble position:
    euclidean.distance <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    # Matrix to save the total euclidean distances:
    euclidean.distance.total <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    # Matrix to save the euclidean distances in overestimated situations:
    euclidean.distance.overestimated <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    
    # Matrix to save the SN:
    SN.hist <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    
    nb.timestamps <- nrow(B[[1]]) # number of timestamps in input data
    nb.variables <- length(B) # number of variables in input data
    n = ncol(B[[1]])-1 # number of ensembles for each timestamp
    
    for (var in 1:nb.variables){ # for each variable
        # compute the minimum ensemble value for each timestamp
        min.value = as.numeric(apply(B[[var]][,1:n],1,min,na.rm=TRUE))
        # compute the maximum ensemble value for each timestamp
        max.value = as.numeric(apply(B[[var]][,1:n],1,max,na.rm=TRUE))
        
        SN <- as.numeric(B[[var]][,n+1])
        SN.hist[,var] <- as.numeric(B[[var]][,n+1]) # save SN
        
        outside.indicator[which(SN>=min.value&SN<=max.value),var] <- 0
        outside.indicator[which(SN<min.value),var] <- -1
        outside.indicator[which(SN>max.value),var] <- 1
        
        euclidean.distance[which(SN<min.value),var] <- (min.value-SN)[which(SN<min.value)]
        euclidean.distance[which(SN>max.value),var] <- (SN-max.value)[which(SN>max.value)]
        
        # overestimated distance:
        euclidean.distance.overestimated[which(SN<max.value),var] <- (max.value-SN)[which(SN<max.value)]
        
        # Euclidean distance:
        distance.matrix <-  apply(B[[var]],1,auxiliar.distance)
        euclidean.distance.total[,var] <- distance.matrix
    }
    outside.indicator <- as.data.frame(outside.indicator)
    names(outside.indicator) <- var.names
    row.names(outside.indicator) <- row.names(B[[1]])
    euclidean.distance = as.data.frame(euclidean.distance)
    names(euclidean.distance) <- var.names
    row.names(euclidean.distance) <- row.names(B[[1]])
    
    ##############################
    # Mean of Euclidean distance
    ##############################
    # 1. in general
    # 2. just for the cases where SN<ensemble
    # 3. just for the cases where SN>ensemble
    # 4. overestimated distance when SN<major ensemble
    
    SN.major.ensemble <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    SN.major.ensemble[outside.indicator==1] = euclidean.distance[outside.indicator==1]
    
    SN.minor.ensemble <- matrix(NA, nrow=nrow(B[[1]]),ncol=length(B)) 
    SN.minor.ensemble[outside.indicator==-1] = euclidean.distance[outside.indicator==-1]
    
    
    df = as.data.frame(cbind(colMeans(euclidean.distance.total,na.rm=T),
                             cbind(colMeans(SN.minor.ensemble,na.rm = T),
                                   colMeans(SN.major.ensemble,na.rm = T),
                                   colMeans(euclidean.distance.overestimated,na.rm = T),
                                   colMeans(SN.hist,na.rm = T),
                                   apply(SN.hist, 2, sd,na.rm = T))))
    names(df) = c('d(SN<->ensemble)', "d(SN<Q(p))","d(SN>Q(1-p))",'d over', 'Mean SN', 'Std SN')
    row.names(df) = var.names#sprintf('\texttt{%s}',var.names)
    #table.distance <- xtable(df,family='serif',digits=3)
    
    df_long = melt(outside.indicator, measure.vars=names(outside.indicator))
    df_table <- as.data.frame.matrix(table(df_long))
    pos <- which(names(df_table)%in%c("-2", "-1", "0","1"))
    count_table <- as.data.frame(matrix(0,nrow(df_table),4))
    names(count_table) <- c("-2", "-1", "0","1")
    row.names(count_table) <- row.names(df_table)
    count_table[,pos] <- df_table
    count.table <- count_table[,c("0", "-1", "1")]
    
    return(list(position.indicator=outside.indicator,
                euclidean.distance=euclidean.distance,
                distance.by.deviation=df,
                count.position=count.table))
}


remove_outliers <- function(B, na.rm = TRUE,p=0.05) {
    #
    # --- Function to remove the 2p most extreme values ---
    #
    B1 <- B
    n.vars <- length(B)
    n.ems  <- ncol(B[[1]])-1
    for (var.index in 1:n.vars){
        x <- data.matrix(B[[var.index]][,1:n.ems])
        q.inf <- as.numeric(apply(x,1,quantile,probs=p,na.rm=T))
        q.sup <- as.numeric(apply(x,1,quantile,probs=1-p,na.rm=T))
        indicator.inf <- apply(x, 2, function(i) i < q.inf)
        indicator.sup <- apply(x, 2, function(i) i > q.sup)
        y <- x
        y[indicator.inf] <- NA
        y[indicator.sup] <- NA
        if (p==0.5){y[,1] <- q.inf}
        B1[[var.index]][,1:n.ems] <- y
    }
    return(B1)
}

# barplots
barplots.number.inoutside <- function(table.with.absolute.freqs,main,
                                      type.a='all',x.max=NULL){
    #
    # --- Function to plot frequencies ---
    #
    table.with.absolute.freqs <- subset(table.with.absolute.freqs,
                                        select=colnames(table.with.absolute.freqs)[order(colnames(table.with.absolute.freqs), decreasing=TRUE)])
    table.with.relative.freqs <- table.with.absolute.freqs
    table.with.relative.freqs[,colSums(table.with.absolute.freqs)>0]  <- round(t(t(table.with.absolute.freqs[,colSums(table.with.absolute.freqs)>0])/
                                                                                     colSums(as.matrix(table.with.absolute.freqs[,colSums(table.with.absolute.freqs)>0]),na.rm=T))*100,2)
    
    xx <- barplot(table.with.absolute.freqs,
                  las=2,
                  beside = TRUE,
                  legend=F,
                  horiz=T,
                  main = main,
                  xlab = 'Number of occurrences',
                  col =  c("goldenrod", "green",'red'),
                  xlim = c(0,max(table.with.absolute.freqs)+
                               0.7*max(table.with.absolute.freqs)))
    
    text(x = table.with.absolute.freqs+0.001*max(table.with.absolute.freqs), 
         y = xx-0.1,
         label = sprintf('%s%%',as.character(table.with.relative.freqs)), 
         pos = 4, cex = 0.6, col = "black")
    
    if (type.a=='all'){
        legend(x=max(table.with.absolute.freqs)+
                   0.1*max(table.with.absolute.freqs), y = max(xx), 
               legend = c("SN>ensemble", "SN in ensemble","SN<ensemble"),
               fill = c("red", "green","goldenrod"),
               bty = "n")
    } else{
        legend(x=max(table.with.absolute.freqs)+
                   0.1*max(table.with.absolute.freqs), y = max(xx), 
               legend = c(TeX("SN>$Q_{1-p}$"), 
                          TeX("SN $\\in \\[Q_{p},Q_{1-p}\\]$"),TeX("SN<$Q_p$")), 
               fill = c("red", "green","goldenrod"),
               bty = "n")
    }
    
}

# -----------------------------------
# 3.5 Spread
# -----------------------------------

# measures
iqt <- function(X,p)
{
    qt <- quantile(X,c(p,1-p),na.rm = T)
    return(qt[2]-qt[1])
}

sharpness.root.mean = function(B,p=0.05){
    # function to compute  the distance between the quantile p and 1-p of ensembles
    # and the standard deviation of ensemble members
    rmse.spread <- matrix(0,nrow=nrow(B[[1]]),ncol=length(B))
    interquantile.amplitude <- matrix(0,nrow=nrow(B[[1]]),ncol=length(B))
    n.ens <- ncol(B[[1]])-1
    for (var in 1:length(B)){
        rmse.spread[,var] <- apply(B[[var]][,1:n.ens],1,sd,na.rm=T)
        interquantile.amplitude[,var] <- apply(B[[var]][,1:n.ens],1,iqt,p=p)
    }
    rmse.spread <- as.data.frame(rmse.spread)
    names(rmse.spread) <- names(B)
    rmse.spread <- apply(rmse.spread,2,mean,na.rm=TRUE)
    interquantile.amplitude <- as.data.frame(interquantile.amplitude)
    names(interquantile.amplitude) <- names(B)
    interquantile.amplitude <- apply(interquantile.amplitude,2,mean,na.rm=TRUE)
    spread.measures = as.data.frame(t(rbind(rmse.spread,interquantile.amplitude)))
    return(spread.measures)
}

# Spread plot
fun_sharpness <- function(Q){ 
    # auxiliar funtion to compute the spread plot coordinates
    quantiles <- seq(0,1,0.05)
    
    distances <- rep(0,length(quantiles))
    
    for (q_ind in 1:length(quantiles)){
        q <- quantiles[q_ind]
        q_q <- apply(Q,1,quantile,probs=q/2,na.rm=T)
        q_1q <- apply(Q,1,quantile,probs=1-q/2,na.rm=T)
        distances[q_ind] <- mean(q_1q-q_q,na.rm=T)
    }
    
    n.timestamps <- sum(!is.na(Q[,1]))
    
    # sharpness dataframe
    sharpness <- data.frame(coverages=quantiles,values=distances,n.timestamps=n.timestamps)
    return(sharpness)
}


sharp.computation.without.parallel <- function(ensembles.by.variable,var.names){
    var <- length(ensembles.by.variable)
    ens <- ncol(ensembles.by.variable[[1]])-1
    sharpness <- c()
    timestamps <- c()
    for (i in 1:var){
        ss <- fun_sharpness(Q=ensembles.by.variable[[i]][,1:ens])
        sharpness = cbind(sharpness,ss$values)
        timestamps <- c(timestamps,unique(ss$n.timestamps))
    }
    sharpness = as.data.frame(cbind(sharpness,fun_sharpness(Q=ensembles.by.variable[[i]])$coverages))
    names(sharpness) <- c(var.names,'coverages')
    return(list(sharpness=sharpness,n.timestamps=timestamps))
}

sharp.computation <- function(ensembles.by.variable,var.names,parallel=FALSE,cl=NULL){
    var <- length(ensembles.by.variable)
    ens <- ncol(ensembles.by.variable[[1]])-1
    
    if (parallel==FALSE){
        return(sharp.computation.without.parallel(ensembles.by.variable,var.names))
    }
    else{
        cl <- makeCluster(detectCores(logical=TRUE))
        registerDoParallel(cl)
        
        results <- foreach(i=1:var) %dopar% {
            
            # Spread plot function
            fun_sharpness <- function(Q){ 
                # auxiliar funtion to compute the spread plot coordinates
                quantiles <- seq(0,1,0.05)
                
                distances <- rep(0,length(quantiles))
                
                for (q_ind in 1:length(quantiles)){
                    q <- quantiles[q_ind]
                    q_q <- apply(Q,1,quantile,probs=q/2,na.rm=T)
                    q_1q <- apply(Q,1,quantile,probs=1-q/2,na.rm=T)
                    distances[q_ind] <- mean(q_1q-q_q,na.rm=T)
                }
                
                n.timestamps <- sum(!is.na(Q[,1]))
                
                # sharpness dataframe
                sharpness <- data.frame(coverages=quantiles,values=distances,n.timestamps=n.timestamps)
                return(sharpness)
            }
            
            ss <- fun_sharpness(Q=ensembles.by.variable[[i]][,1:ens])
            sharpness = data.frame(ss$values)
            names(sharpness) <- var.names[i]
            timestamps <- data.frame(unique(ss$n.timestamps))
            names(timestamps) <- var.names[i]
            return(cbind(sharpness,timestamps))
        }
        
        stopCluster(cl)
        
        sharpness <- lapply(results, `[`, 1)
        sharpness <- do.call("cbind",sharpness)
        
        timestamps <- lapply(results, `[`, 2)
        timestamps <- do.call("cbind",timestamps)[1,]
        
        sharpness = as.data.frame(cbind(sharpness,fun_sharpness(Q=ensembles.by.variable[[1]])$coverages))
        names(sharpness) <- c(var.names,'coverages')
        return(list(sharpness=sharpness,n.timestamps=timestamps))
    }
}

# QQplot
qq.deviation <- function(sample){
    qq.dev <- c()
    for (i in 1:nrow(sample)){
        if ((sum(is.na(sample[i,]))>0 || (sd(sample[i,])==0))){
            qq.dev <- rbind(qq.dev,rep(NA,ncol(sample)))
        }else{
            aux <- qqnorm((sample[i,]-mean(sample[i,]))/sd(sample[i,]),plot.it=F)
            dev <- sort(aux$y)-sort(aux$x)
            qq.dev <- rbind(qq.dev,dev)
        }
    }
    return(colMeans(qq.dev,na.rm=T))
}

qq.computation <- function(ensembles.by.variable){
    t = nrow(ensembles.by.variable[[1]])
    ens <- ncol(ensembles.by.variable[[1]])-1
    var <- length(ensembles.by.variable)
    ensembles.by.variable1 = rep( list(matrix(NA,t,ens)), var ) 
    
    for (var.ind in 1:var){
        for (j in 1:nrow(ensembles.by.variable[[var.ind]])){
            ensembles.by.variable1[[var.ind]][j,1:ens] <- sort(data.matrix(ensembles.by.variable[[var.ind]][j,1:ens]),na.last=T)
        }
    }
    
    qq.norm <- c()
    for (i in 1:var){
        qq.norm = cbind(qq.norm,qq.deviation(ensembles.by.variable1[[i]]))
    }
    
    qq.norm = as.data.frame(qq.norm)
    names(qq.norm) = names(ensembles.by.variable)
    qq.norm$x <- sort(qqnorm(rnorm(ens),plot.it=F)$x)
    return(qq.norm)
}

# -----------------------------------
# 3.6 CRPS
# -----------------------------------

CRPS <- function(B){
    crps <- matrix(0,nrow(B[[1]]),length(B))
    s1 <- matrix(0,nrow(B[[1]]),length(B))
    s2 <- matrix(0,nrow(B[[1]]),length(B))
    
    for (i in 1:nrow(B[[1]])){
        for (var in 1:length(B)){
            x <- B[[var]][i,ncol(B[[var]])] # SN value
            ens <-  B[[var]][i,-ncol(B[[var]])]
            distance.matrix <- as.matrix(dist(as.numeric(ens),method = 'euclidean')) #spread
            sum1 <- mean(rdist(as.numeric(ens), x))
            sum2 <- mean(distance.matrix,na.rm = TRUE)
            crps[i,var] <- sum1-0.5*sum2
            s1[i,var] <- sum1
            s2[i,var] <- sum2
        }
    }
    
    crps <- as.data.frame(crps)
    names(crps) <- names(B)
    crps$metric='CRPS'
    s1 <- as.data.frame(s1)
    names(s1) <- names(B)
    s1$metric = 'd(SN<->ens.)'
    s2 <- as.data.frame(s2)
    names(s2) <- names(B)
    s2$metric = 'd(ens.i<->ens.j)'
    time.evolution <- rbind(crps,s1,s2)
    time.evolution$timestamps <- row.names(B[[1]])
    crps. <- subset(crps,select=names(crps)[-ncol(crps)])
    cmeans = colMeans(crps.,na.rm=TRUE)
    return(list(crps.time.evolution=time.evolution,
                crps.mean=melt(cmeans, measure.vars=names(cmeans))))
}
################################################################################
#                            MULTIVARIATE ANALYSIS                             #
################################################################################

# -----------------------------------
# 4.1 Multivariate rank histograms
# -----------------------------------

## Multivariate ranks 
mv.rank <- function(x)
{
    x <- na.omit(x)
    d <- dim(x)
    x.prerank <- numeric(d[2])
    if (dim(x)[1]>0){
        for(i in 1:d[2]) {
            x.prerank[i] <- sum(apply(x<=x[,i],2,all,na.rm=T,na.last='keep'))
        }
    }  else{
        x.prerank <- NA
    }
    x.rank <- rank(x.prerank,ties="random",na.last='keep')
    return(x.rank)
}


## Multivariate rank histograms
mrh.rhist <- function(B,nr.ensembles)
{   
    reps=nrow(B[[1]])
    x <- rep(0,reps) 
    for(i in 1:reps){
        B. <- matrix(0,ncol(B[[1]]),length(B))
        for(j in 1:length(B)){
            B.[,j] <- as.numeric(B[[j]][i,])
        }
        x[i] <- mv.rank(t(B.))[ncol(t(B.))]
    }
    
    x <- as.data.frame(x)
    x <- na.omit(x)
    # summary the rank histogram information (check the number of ocorrences of 
    # each SN position, for each variable)
    a <- table(x)
    talagrand.csv = rep(0,nr.ensembles+1)
    talagrand.csv[as.numeric(row.names(a))] <- as.matrix(a)
    
    talagrand.csv=as.data.frame(talagrand.csv)
    talagrand.csv <- rbind(colSums(talagrand.csv),talagrand.csv)
    talagrand.csv <- as.matrix(talagrand.csv)
    talagrand.csv[-1,] <- t(t(talagrand.csv[-1,])/talagrand.csv[1,])
    row.names(talagrand.csv) <- c('n.timestamps',sprintf('bin %s',1:(nr.ensembles+1)))
    
    return(talagrand.csv)
}



# -----------------------------------
# 4.2 Energy Score
# -----------------------------------

energy.score <- function(B,timestamps){
    ES <- rep(0,nrow(B[[1]]))
    s1 <- c()
    s2 <- c()
    
    for (i in 1:nrow(B[[1]])){
        mm <- matrix(0,ncol(B[[1]]),length(B))
        for (j in 1:length(B)){
            mm[,j] <- as.numeric(B[[j]][i,])
        }
        m <- as.matrix(dist(mm,method = 'euclidean'))
        sum1 <- mean(m[nrow(m),-nrow(m)],na.rm=TRUE)
        s1 <- c(s1,sum1)
        sum2 <- mean(m[-nrow(m),-nrow(m)],na.rm = TRUE)
        s2 <- c(s2,sum2)
        ES[i] <- sum1-0.5*sum2
        
    }
    ES. <- data.frame(ES=mean(ES,na.rm=TRUE),E1=mean(s1,na.rm=T), E2=mean(s2,na.rm=T))
    EScore.by.timestamp = data.frame(timestamp=row.names(B[[1]]),ES=ES,E1=s1,E2=s2)
    return(list(ES.by.timestamp=EScore.by.timestamp,ES.mean=ES.))
}


###############################################################################
#           AUXILIAR FUNCTION TO PLOT INFORMATION
###############################################################################

mv.rank.quality <- function(x)
{
    d <- dim(x)
    x.prerank <- numeric(d[2])
    for(i in 1:d[2]) {
        x.prerank[i] <- sum(apply(x<=x[,i],2,all,na.rm=T))
    }
    x.rank <- x.prerank#rank(x.prerank)
    return(trunc(x.rank))
}


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
        plots.list <- lapply(plots.list, function(x) x <- x + ggtitle(""))
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
    g <- grid.arrange(grobs = grobs.list, ncol = nb.columns, heights=hts,top=title)
    
    return(g)
}