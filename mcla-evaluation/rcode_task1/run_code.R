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
#   PARAMETRIZATION
################################################################################
options(warn=-1)

path_tool <-'C:/Users/rcode'

# read configuration file
df.parameters <- read.csv(sprintf('%s/config_file_task1.txt',path_tool),header=T)

#paths
results.path <- as.character(df.parameters[df.parameters$Parameter=='results_path',2])
data.path <- as.character(df.parameters[df.parameters$Parameter=='data_path',2])
# value of p
x <- df.parameters[df.parameters$Parameter=='p-value',2]
p.quantiles <- as.numeric(levels(x)[x])
# value SM
x <- df.parameters[df.parameters$Parameter=='SM-value',2]
SM <- as.numeric(levels(x)[x])
#plot?
trade.off.analysis <- as.logical(df.parameters[df.parameters$Parameter=='TradeOff Analysis',2])
x <- df.parameters[df.parameters$Parameter=='Maximum nr of ensembles',2]
max.ensemble <- c(as.numeric(levels(x)[x]))
time.evolution.plots <- as.logical(df.parameters[df.parameters$Parameter=='Time Evolution Plots',2])
# initial and final dates
date.ini <-as.character(df.parameters[df.parameters$Parameter=='Initial timestamp',2])
date.fin <-as.character(df.parameters[df.parameters$Parameter=='Final timestamp',2])
# p values of fan chart
x <- df.parameters[df.parameters$Parameter=='Fan.chart.p.value1',2]
fan.p.values <- c(as.numeric(levels(x)[x]))
x <- df.parameters[df.parameters$Parameter=='Fan.chart.p.value2',2]
fan.p.values <- c(fan.p.values,as.numeric(levels(x)[x]))
x <- df.parameters[df.parameters$Parameter=='Fan.chart.p.value3',2]
fan.p.values <- c(fan.p.values,as.numeric(levels(x)[x]))
x <- df.parameters[df.parameters$Parameter=='Fan.chart.p.value4',2]
fan.p.values <- c(fan.p.values,as.numeric(levels(x)[x]))
fan.p.values <- sort(fan.p.values)
x <- df.parameters[df.parameters$Parameter=='Minimum timestamps',2]
minimum.timestamp <- as.numeric(levels(x)[x])
rm(x)
bootstrap.random.ind <- as.logical(df.parameters[df.parameters$Parameter=='Bootstrap.random',2])
# re-dispatch option
x <- df.parameters[df.parameters$Parameter=='re-dispatch.costs.options',2]
red.options <- c(as.numeric(levels(x)[x]))
x <- as.character(df.parameters[df.parameters$Parameter=='re-dispatch.costs.SMs',2])
SM.redisp <- as.numeric(unlist(strsplit(x, split=";")))
x <- as.character(df.parameters[df.parameters$Parameter=='re-dispatch.costs.Q(1-p)',2])
Q.redisp <- as.numeric(unlist(strsplit(x, split=";")))

source(sprintf('%s/main_functions.R',path_tool))

# parallel?
do.parallel <- as.logical(df.parameters[df.parameters$Parameter=='do_parallel',2])
if (do.parallel==TRUE){
    x <- df.parameters[df.parameters$Parameter=='Cores',2]
    selected.cores <- as.numeric(levels(x)[x])
    cl.to.use <- min(selected.cores, detectCores(logical=TRUE))
} 

if (nrow(df.parameters)>20){
    vars.to.remove <- as.character(df.parameters[-c(1:20),1])
} else {vars.to.remove <- NULL}

rm(df.parameters)

contingency=basename(data.path)

# Create paths to save results
dir.create(file.path(results.path), showWarnings = FALSE,recursive = TRUE)
dir.create(file.path(sprintf('%s/',results.path)), 
           showWarnings = F,recursive = TRUE)
dir.create(file.path(sprintf('%s/univariate',results.path)), 
           showWarnings = F,recursive = TRUE)
dir.create(file.path(sprintf('%s/multivariate',results.path)), 
           showWarnings = F,recursive = TRUE)
dir.create(file.path(sprintf('%s/univariate/TimeEvolution/',results.path)), 
           showWarnings = F,recursive = TRUE)

################################################################################
# READ AND PROCESS DATA
################################################################################
computation.times <- c() # it will save computation times
# read all data
cat('Reading data...\n')
initial.time <- proc.time()


temp = list.files(path=data.path,pattern="*.RData",full.names=F,recursive=T)


string. <- c(strsplit(gsub("[^[:alnum:] ]", "", sprintf('%s_%s',date.ini,date.fin)), " +")[[1]])
string. <- paste(string.,collapse ="")

if (sum(sprintf('%s.RData',string.)%in%temp)>0){
    load(sprintf('%s/%s.RData',data.path,string.))
}else{
    aux.matrix <- read.data.I(data.path,selected.vars=NULL,date.ini=date.ini,date.fin=date.fin,max.ensemble)
    save(aux.matrix,file=sprintf('%s/%s.RData',data.path,string.))
}

B <- aux.matrix$data.normalized

I.max <- aux.matrix$df.Imax
kV.level <- aux.matrix$df.kV 

nr.ensembles <- ncol(B[[1]])-1
time.read.data <- proc.time()
cat('|-> Total time:', (time.read.data - initial.time)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Read data',(time.read.data - initial.time)[3]))


# remove lines without the minimum timestamps
lines.to.remove <- c()
for (i in 1:length(B)){
    if (sum(!is.na(B[[i]]$SN))<minimum.timestamp) lines.to.remove <- c(lines.to.remove,i)
}

if (length(lines.to.remove)==length(B)){
    cat('There is no observations! Please check the config_file parameters...')
} else{
    remove. <- unique(c(lines.to.remove,which(names(B)%in%sprintf('%s_I',vars.to.remove))))
    
    if (length(remove.)>0) B <- B[names(B)[-remove.]]
    
    df.overload.distances <- Euclidean.distance.overloads(B,p=1,SM=0,cl.to.use=max(round(cl.to.use/2),2))
    
    df.overload.distances.removing.q <- Euclidean.distance.overloads(B,p=p.quantiles,all.situations=TRUE,SM=SM,cl.to.use=max(round(cl.to.use/2),2))
    
    branches.with.data <- names(df.overload.distances$euclidean.distance.SN)[colSums(!is.na(df.overload.distances$euclidean.distance.SN))>0]
    
    if (red.options!=1){
################################################################################
# Compute re-dispatch, if required
################################################################################
        
        cat('Computing re-dispatch (MW)...\n')
        
        overloadsSN <- df.overload.distances$euclidean.distance.SN.overload
        overloadsSN <- (overloadsSN)*subset(I.max,select=names(overloadsSN))
        overloadsSN <- subset(overloadsSN, select=branches.with.data)
        overloadsSN <- colSums(overloadsSN,na.rm=T)
        
        levels.kV <- unique(as.numeric(kV.level[-1]))
        cumulative.overload <- c()
        for (kk in levels.kV){
            cumulative.overload <- cbind(cumulative.overload,round(kk*sum(overloadsSN[kV.level==kk],na.rm=T),3))
        }
        
        cumulative.overload <- data.frame(cumulative.overload)
        names(cumulative.overload) <- levels.kV
        row.names(cumulative.overload) <- 'SN'
        
        for (c0 in SM.redisp){ # for each defined margin
            overloadsDACFc0 <- (1+c0)*df.overload.distances$euclidean.distance.DACF
            overloadsDACFc0[(overloadsDACFc0<=1)&!is.na(overloadsDACFc0)] <- NA
            overloadsDACFc0[(overloadsDACFc0>1)&!is.na(overloadsDACFc0)] <- overloadsDACFc0[(overloadsDACFc0>1)&!is.na(overloadsDACFc0)]-1
            overloadsDACFc0 <- (overloadsDACFc0)*(I.max[names(overloadsDACFc0)])
            overloadsDACFc0 <- subset(overloadsDACFc0, select=branches.with.data)
            overloadsDACFc0 <- colSums(overloadsDACFc0,na.rm=T)
            aux <- c()
            for (kk in levels.kV){
                aux <- cbind(aux,round(kk*sum(overloadsDACFc0[kV.level==kk],na.rm=T),3))
            }
            aux <- data.frame(aux)
            names(aux) <- levels.kV
            row.names(aux) <- sprintf('%sDACF',round(1+c0,2))
            cumulative.overload <- rbind(cumulative.overload,aux)
        }
        
        for (c0 in c(1-Q.redisp)){# for each defined quantile
            df.overload.distances.removing.c0 <- Euclidean.distance.overloads(B,p=c0,all.situations=FALSE,cl.to.use=max(round(cl.to.use/2),2))
            overloadsQc0 <- df.overload.distances.removing.c0$euclidean.distance.Qsup.overload
            overloadsQc0 <- (overloadsQc0)*(I.max[names(overloadsQc0)])
            overloadsQc0 <- subset(overloadsQc0, select=branches.with.data)
            overloadsQc0 <- colSums(overloadsQc0,na.rm=T)
            print(c0)
            aux <- c()
            for (kk in levels.kV){
                aux <- cbind(aux,round(kk*sum(overloadsQc0[kV.level==kk],na.rm=T),3))
            }
            aux <- data.frame(aux)
            names(aux) <- levels.kV
            row.names(aux) <- sprintf('Q(%s)',round(1-c0,2))
            cumulative.overload <- rbind(cumulative.overload,aux)
        }
        
        cumulative.overload <- data.frame(cumulative.overload)
        cumulative.overload$'redispatch (MW)' <- rowSums(cumulative.overload)*(sqrt(3)/1000)
        write.table(round(subset(cumulative.overload,select='redispatch (MW)'),2),file=sprintf('%s/multivariate/redispatch.csv',
                                                                                               results.path),sep=',',dec='.',row.names = T)
        
        time.EE. <- proc.time()
        cat('|-> Total time:', (time.EE.-time.read.data)[3],'seconds \n')
        computation.times <- rbind(computation.times,c('Re-dispatch (in MW) computation',(time.EE.-time.read.data)[3]))
        time.read.data <- time.EE.
    }
    
    if (red.options!=2){
################################################################################
# Compute distances -- Location and Dispersion Metrics
################################################################################
        cat('Computing location and dispersion metrics...\n')
        df1 <- subset(df.overload.distances$distances.summary.load,
                      select=c("Mean SN","Std SN",
                               "min SN","max SN",
                               "Mean DACF","Std DACF",
                               "min DACF","max DACF"))
        df3 <- subset(df.overload.distances$distances.summary.overload,
                      select=c("Mean SN (overload)","Std SN (overload)",
                               "min SN (overload)","max SN (overload)",
                               "Mean DACF (overload)","Std DACF (overload)",
                               "min DACF (overload)","max DACF (overload)"))
        
        df2 <- subset(df.overload.distances.removing.q$distances.summary.load,
                      select=c("Mean Q(1-p)","Std Q(1-p)",
                               "min Q(1-p)","max Q(1-p)"))
        
        df4 <- subset(df.overload.distances.removing.q$distances.summary.overload,
                      select=c("Mean Q(1-p) (overload)","Std Q(1-p) (overload)",
                               "min Q(1-p) (overload)","max Q(1-p) (overload)"))
        df <- cbind(df1,df2,df3,df4)
        
        df$'Nr of real overloads' <- colSums(df.overload.distances$euclidean.distance.SN>1,na.rm=T)
        df$'Nr of DACF overloads' <- colSums(df.overload.distances$euclidean.distance.DACF>1,na.rm=T)
        df$'Nr of Q(1-p) overloads' <- colSums(df.overload.distances.removing.q$euclidean.distance.Qsup>1,na.rm=T)
        df$'Nr total timestamps' <- colSums(!is.na(df.overload.distances$euclidean.distance.SN),na.rm=T)
        data.stat <- df[!is.na(df$`Mean SN`),]
        rm(df1,df2,df3,df4)
        write.table(data.stat,file=sprintf('%s/univariate/LoadMetrics.csv',
                                           results.path),sep=',',dec='.',col.names = NA)
        
        cat('Computing Euclidean distances between SN and the ensemble...\n')
        
        branches.with.data <- names(df.overload.distances$euclidean.distance.SN)[colSums(!is.na(df.overload.distances$euclidean.distance.SN))>0]
        B <- B[branches.with.data]
        
        df.overload.distances$euclidean.distance.SN <- subset(df.overload.distances$euclidean.distance.SN,select=branches.with.data)
        df.overload.distances$euclidean.distance.DACF <- subset(df.overload.distances$euclidean.distance.DACF,select=branches.with.data)
        df.overload.distances$euclidean.distance.Qsup <- subset(df.overload.distances$euclidean.distance.Qsup,select=branches.with.data)
        df.overload.distances.removing.q$euclidean.distance.Qsup <- subset(df.overload.distances.removing.q$euclidean.distance.Qsup,select=branches.with.data)
        
        aux <- colSums(df.overload.distances$euclidean.distance.SN>1,na.rm=T)+
            colSums(df.overload.distances.removing.q$euclidean.distance.DACF>1,na.rm=T)+
            colSums(df.overload.distances.removing.q$euclidean.distance.Qsup>1,na.rm=T)
        
        lines.with.overloads <- names(aux)[aux>0]
        positions.with.overloads <- as.numeric(which(aux>0))
        
        if (length(lines.with.overloads)==0){
            cat('There is no overload situations for the config_file parameters!')
        } else{
            lines.with.overloads. <-substr(lines.with.overloads,1,
                                           nchar(lines.with.overloads)-2)
    ################################################################################
    # Boxplots
    ################################################################################
            boxplots.I <- c()
            for (i in positions.with.overloads){
                timestamps <- row.names(df.overload.distances$euclidean.distance.SN)
                
                ps5 <-  which(df.overload.distances$euclidean.distance.SN[,i]==max(df.overload.distances$euclidean.distance.SN[,i],na.rm=T))[1]
                ifelse(is.na(ps5),
                       ddf <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps5,]))),b='max load SN',c=NA),
                       ddf <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps5,]))),b='max load SN',c=timestamps[ps5])
                )
                
                ps6 <-  which(df.overload.distances$euclidean.distance.DACF[,i]==max(df.overload.distances$euclidean.distance.DACF[,i],na.rm=T))[1]
                ifelse(is.na(ps6),
                       ddf1 <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps6,]))),b='max load DACF',c=NA),
                       ddf1 <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps6,]))),b='max load DACF',c=timestamps[ps6]))
                ddf <- rbind(ddf,ddf1)
                
                ps7 <-  which(df.overload.distances.removing.q$euclidean.distance.Qsup[,i]==max(df.overload.distances.removing.q$euclidean.distance.Qsup[,i],na.rm=T))[1]
                ifelse(is.na(ps7),
                       ddf1 <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps7,]))),b=sprintf('max load Q(%s)',1-p.quantiles),c=NA),
                       ddf1 <- data.frame(a=as.numeric(as.vector((B[[names(df.overload.distances$euclidean.distance.SN)[i]]][ps7,]))),b=sprintf('max load Q(%s)',1-p.quantiles),c=timestamps[ps7]))
                ddf <- rbind(ddf,ddf1)
                
                ddf <- as.data.frame(ddf)
                names(ddf) <- c('values','type','dates.t')
                
                ddf$variable <- names(df.overload.distances$euclidean.distance.SN)[i]
                boxplots.I <- rbind(boxplots.I,ddf)
            }
            
            
            # define color and size of points
            col <- c('red',rep('black',nr.ensembles-1),'green')
            boxplots.I$col=as.character(col)
            boxplots.I$siz[boxplots.I$col=='red'] <- 2
            boxplots.I$siz[boxplots.I$col=='black'] <- 0.3
            boxplots.I$siz[boxplots.I$col=='green'] <- 2
            
            boxplots.I<- boxplots.I[order(boxplots.I$col),]
            scaleFUN <- function(x) sprintf("%.3f", x) # precision to use in plot axis
            pdf(sprintf('%s/univariate/Boxplot.pdf',results.path))
            cutoff <- data.frame( x = c(-Inf, Inf), y =1.00, Overload = factor(1.00) )
            for (im.plot in 1:length(lines.with.overloads)){
                vars.to.consider <- lines.with.overloads[im.plot]
                df.selected <- boxplots.I[boxplots.I$variable%in%vars.to.consider,]
                df.unique <- unique(df.selected[,c("type","variable","dates.t")])
                p <-ggplot(df.selected) +geom_boxplot(aes(x=type,y=values)) +
                    geom_point(aes(x=type,y=values,colour=as.character(col),size=siz)) + 
                    geom_line(aes( x, y, color='red'), cutoff)+
                    scale_color_manual("Legend", values=c(red="red",green="green",black="black"),
                                       labels=c('ensemble member','SN','DACF'))+ 
                    geom_text(data=df.unique,aes(x=type,y=min(df.selected$values,na.rm=T)-0.009,label=dates.t),size=2)+
                    guides(size=FALSE) + scale_size_area(max_size = 2)+
                    facet_wrap(~variable,ncol=1,scale='free_y')+
                    xlab('Date (YYYY-mm-dd HH:MM:00)')+ylab('Electric Current [per unit]')+
                    theme_bw() + theme(strip.background =element_rect(fill="azure2"),
                                       panel.border = element_rect(colour = "black"),
                                       text=element_text(family="serif",size=10),
                                       axis.text.x=element_text(family="serif",size=10))+
                    scale_y_continuous(labels=scaleFUN,limits = c(min(df.selected$values,na.rm=T)-0.009,max(df.selected$values,na.rm=T)+0.009))+
                    labs(caption = sprintf('\t\tContingency: %s',contingency))
                print(p)
                rm(df.selected,df.unique,vars.to.consider)
            }
            dev.off()
            
            rm(boxplots.I)
            
            rm(ddf,ddf1,ddf2,ps5,ps6,ps7)
        
            
            time.EE <- proc.time()
            cat('|-> Total time:', (time.EE-time.read.data)[3],'seconds \n')
            computation.times <- rbind(computation.times,c('Euclidean distances and boxplots',(time.EE-time.read.data)[3]))
            
            #############################
            ## security classification
            #############################
            
            security.class <- security.classification(B,c=1-p.quantiles,SM=SM,cl.to.use=max(round(cl.to.use/2),2))
            
            #  --- BY BRANCH ---
            df.SN.class <- security.class$SN.classification.by.branch
            df.SN.class$timestamps <- row.names(df.SN.class)
            df.SN.class <- melt(df.SN.class,id.vars='timestamps') 
            names(df.SN.class) <- c('timestamps','variable','SN classification')
            
            df.DACF.class <-security.class$DACF.classification.by.branch
            df.DACF.class$timestamps <- row.names(df.DACF.class)
            df.DACF.class <- melt(df.DACF.class,id.vars='timestamps') 
            names(df.DACF.class) <- c('timestamps','variable','DACF classification')
            
            df.FOprob.class <- security.class$FOprob.classification.by.branch
            df.FOprob.class$timestamps <- row.names(df.FOprob.class)
            df.FOprob.class <- melt(df.FOprob.class,id.vars='timestamps') 
            names(df.FOprob.class) <- c('timestamps','variable','FO prob classification')
            
            df.classification <- Reduce(function(x, y) merge(x, y, by=c("timestamps","variable"),all.x=T), 
                                   list(df.SN.class,df.DACF.class,df.FOprob.class))
            
            df.classification$timestamps = as.POSIXct(df.classification$timestamps, 
                                    format = "%Y-%m-%d %H:%M:00",
                                    tz="Europe/Paris")
            df.classification$Frequency=1
            df.classification$variable <- substr(df.classification$variable,1,
                                                 nchar(as.character(df.classification$variable))-2)
            
            df.classification.original <- df.classification
            
            time.sc <- proc.time()
            computation.times <- rbind(computation.times,c('Security classification',(time.sc-time.EE)[3]))
            
            
            my_theme <-function () { # style plot definition
                theme_bw()+
                    theme(axis.text=element_text(colour='black'),
                          axis.title.x = element_blank(), 
                          axis.text.x = element_blank(),
                          axis.text.y=element_blank(),
                          text=element_text(size=10),
                          legend.key.size=unit(0.35, 'cm'),
                          legend.title=element_blank(),
                          plot.title = element_text(size=12))
            }
            
            if (time.evolution.plots){
################################################################################
# Time Evolution plots by variable
################################################################################
                
                cat('Saving Time Evolution Plots...\n')
                scaleFUN <- function(x) sprintf("%.3f", x)
                colfunc <- colorRampPalette(c("slategray1", "slateblue3"))
                cols.fill <-  colfunc(4)
                
                override.linetype <- c(1, 1)
                dd <- c()
                for (var in lines.with.overloads){
                    dd <- rbind(dd,cbind(B[[var]],vars=var))
                }
                
                dd1.pos <- data.frame(timestamps = as.POSIXct(row.names(dd), 
                                                              format = "%Y-%m-%d %H:%M:00",
                                                              tz="Europe/Paris"),
                                      t(apply(dd[,1:nr.ensembles],1,quantile,
                                              probs = c(fan.p.values,rev(1-fan.p.values)),na.rm=T)))
                dd1.pos$DACF = dd$DACF
                dd1.pos$SN = dd$SN
                dd1.pos$SN.overload = dd1.pos$SN-1
                dd1.pos$SN.overload[dd1.pos$SN.overload<0] = NA 
                dd1.pos$DACF.overload = (1+SM)*dd1.pos$DACF-1
                dd1.pos$DACF.overload[dd1.pos$DACF.overload<0] = NA 
                dd1.pos$Qsup.overload <- as.numeric(apply(dd[,1:nr.ensembles],1,quantile,
                                               probs = 1-p.quantiles,na.rm=T))-1
                dd1.pos$Qsup.overload[dd1.pos$Qsup.overload<0] <- NA 
                dd1.pos$variable <- as.factor(substr(as.character(dd$vars),1,nchar(as.character(dd$vars))-2))
                fan.chart.data.withNAN <- dd1.pos
                rm(dd)
                
                df.classification$variable <- as.factor(df.classification$variable)
                
                df.unsecure.members=security.class$FOprob.unsecure.members
                df.unsecure.members$timestamps <- as.POSIXct(row.names(df.unsecure.members), 
                             format = "%Y-%m-%d %H:%M:00",
                             tz="Europe/Paris")
                df.unsecure.members <- melt(df.unsecure.members,id.vars='timestamps')
                df.unsecure.members$variable <- as.factor(substr(as.character(df.unsecure.members$variable),1,nchar(as.character(df.unsecure.members$variable))-2))
                df.unsecure.members$S.members <- 1-df.unsecure.members$value
                names(df.unsecure.members) <- c('timestamps','variable','U.members','S.members')
                df <- Reduce(function(x, y) merge(x, y, by=c("timestamps","variable"),all.x=T), 
                             list(fan.chart.data.withNAN,
                                  df.classification,
                                  df.unsecure.members))
                df <- df[!is.na(df$SN),]
                df$Week <- sprintf('Week %s of %s',strftime(df$timestamps,format="%W"),strftime(df$timestamps,format="%Y")) 
            
                a <-diff(df$timestamps)
                units(a) <- 'hours'
                idx <- c(1, a)
                i2 <- c(1,which(idx >2), nrow(df)+1)
                df$grp.distance <- rep(1:length(diff(i2)), diff(i2))
            
                df.selected <- df
                rm(df)
                names(df.selected) <- c("timestamps","variable","X2.5.","X5.","X10.",
                                        "X15.","X85.","X90.","X95.","X97.5.","DACF",
                                        "SN","overload SN",
                                        "overload DACF","overload Q(1-p)",
                                        "SNclassification","DACFclassification",
                                        "FOprobclassification", "Frequency","Unsecure",
                                        "Secure","Week","grp.distance") 
                
                
                
                plot.uni.vars <- function(var){
                    i = 1
                    plotss <- list()
                    for (Week in sort(unique(df.selected$Week))){
                        fchart <- df.selected[as.character(df.selected$variable)==var,]
                        fchart <- fchart[fchart$Week==Week,]
                        if (nrow(fchart)>0){
                            t.steps='6 hours'
                            lims.date = c(floor_date(fchart$timestamps[1], 
                                                     unit="week",
                                                     week_start = getOption("lubridate.week.start", 1)),
                                          ceiling_date(fchart$timestamps[1], 
                                                       unit="week",
                                                       week_start = getOption("lubridate.week.start", 1)))
                            
                            values.1= as.character(data.frame("orange","red","green","a","b","c"))
                            values.1[1:6]=c(cols.fill,"red","black")
                            names(values.1)=c(sprintf("p=%s",fan.p.values),"DACF","SN")
                            
                            p1 <- ggplot(fchart,aes(x=timestamps))+
                                geom_linerange(aes(x=timestamps, ymin= X2.5., ymax= X97.5.),size=0.3,colour=cols.fill[1])+
                                geom_linerange(aes(x=timestamps, ymin= X5., ymax= X95.),size=0.3,colour=cols.fill[2])+
                                geom_linerange(aes(x=timestamps, ymin= X10., ymax= X90.),size=0.3,colour=cols.fill[3])+
                                geom_linerange(aes(x=timestamps, ymin= X15., ymax= X85.),size=0.3,colour=cols.fill[4])+ 
                                geom_ribbon(aes(ymin= X2.5., ymax= X97.5.,fill=sprintf("p=%s",fan.p.values[1]),group=grp.distance))+
                                geom_ribbon(aes(ymin= X5., ymax= X95., fill=sprintf("p=%s",fan.p.values[2]),group=grp.distance))+
                                geom_ribbon(aes(ymin= X10., ymax= X90., fill=sprintf("p=%s",fan.p.values[3]),group=grp.distance))+
                                geom_ribbon(aes(ymin= X15., ymax= X85., fill=sprintf("p=%s",fan.p.values[4]),group=grp.distance))+
                                geom_line(aes(x=timestamps,y=DACF,colour='DACF',group=grp.distance),size=0.3)+
                                geom_point(aes(x=timestamps,y=DACF,colour='DACF',group=grp.distance),size=0.7) +
                                geom_line(aes(y=SN,colour='SN',group=grp.distance),size=0.3)+
                                geom_point(aes(x=timestamps,y=SN,colour='SN',group=grp.distance),size=0.001,alpha=0.3) +
                                theme_bw() +
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits = lims.date)+
                                theme(axis.text=element_text(colour='black'),
                                      axis.title.x = element_blank(), 
                                      axis.text.x = element_blank(),
                                      text=element_text(size=10))+
                                ylab('Electric Current [p.u.]')+
                                ggtitle(sprintf('%s: %s (positive observed Pik)',Week,var))+
                                scale_colour_manual(name='Legend', values=values.1,
                                                    guide = guide_legend(override.aes = list(
                                                        linetype = override.linetype,
                                                        shape = c(16,NA))))+
                                scale_fill_manual(drop=FALSE,
                                                  name="[Q(p);Q(1-p)]", values=values.1)+
                                scale_y_continuous(labels=scaleFUN)+
                                guides(fill=guide_legend(ncol=2))
                            
                            values.2= as.character(data.frame("orange","red","green"))
                            values.2[1:6]=c("orange","red","green")
                            names(values.2)=c(sprintf("SN<Q(%s)",p.quantiles),
                                              sprintf("SN>Q(%s)",1-p.quantiles),
                                              sprintf("SN in [Q(%s),Q(%s)]",p.quantiles,1-p.quantiles))
                            
                            
                            
                            fchart1 <- fchart[,c("timestamps","variable","overload SN",    
                                                 "overload DACF","overload Q(1-p)","Week","grp.distance")]
                            names(fchart1) <- c("timestamps","variable","SN",    
                                                sprintf("DACF (with %s%% SM)",round(100*SM,2)),sprintf("Q(%s)",1-p.quantiles),"Week","grp.distance")
                            
                            fchart1 <- melt(fchart1,id.vars=c("timestamps","variable","Week","grp.distance"))
                            names(fchart1) <- c("timestamps","variable","Week","grp.distance","overtype","value") 
                            
                            values.3= as.character(data.frame("black","red","#6959CD"))
                            values.3[1:3] <- c("black","red","#6959CD")
                            names(values.3)=c("SN",sprintf("DACF (with %s%% SM)",round(100*SM,2)),sprintf("Q(%s)",1-p.quantiles))
                            
                            p2 <- ggplot(fchart1,aes(x=timestamps,y=value,
                                                     group = interaction(grp.distance, overtype),
                                                     colour = overtype))+ geom_point(size=0.6)+ 
                                geom_line(size=0.3)+ theme_bw() +
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits =lims.date)+
                                theme(axis.text=element_text(colour='black'),
                                      axis.title.x = element_blank(), 
                                      axis.text.x = element_blank(),
                                      text=element_text(size=10),
                                      legend.key.size=unit(0.35, 'cm'),
                                      legend.title=element_blank(),
                                      plot.title = element_text(size=12))+
                                scale_colour_manual(values=values.3)+
                                scale_fill_manual(drop=FALSE,
                                                  name="[Q(p);Q(1-p)]", values=values.3)+
                                xlab('Timestamp (YYYY-mm-dd HH:MM)')+
                                ylab('Overload [p.u.]')+
                                scale_y_continuous(labels=scaleFUN)
                            
                            fchart1 <- unique(fchart[,c("timestamps","variable","Frequency","DACFclassification","FOprobclassification")])
                            p3 <- ggplot(fchart1, aes(x = timestamps, y = Frequency, fill = DACFclassification)) +  
                                geom_bar(stat = "identity") +
                                theme_bw() +
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits =lims.date)+
                                my_theme()+
                                ylab('')+
                                ggtitle(sprintf('Classification Error provided by the deterministic forecast (with %s%% SM)',round(100*SM,2)))+
                                scale_fill_manual(values=c('scs'="#92D050",
                                                           'ucu'="#4F6228",
                                                           'scu [FA]'="#FF9900",
                                                           'ucs [MA]'="#C00000"),
                                                  limits = c("ucu","scs","scu [FA]","ucs [MA]"))+
                                scale_y_continuous(labels=scaleFUN)+guides(fill=guide_legend(ncol=2))
                            
                            p4 <- ggplot(fchart1, aes(x = timestamps, y = Frequency, fill = FOprobclassification)) +  
                                geom_bar(stat = "identity") +
                                theme_bw() +
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits =lims.date)+
                                my_theme()+
                                ylab('')+
                                ggtitle(sprintf('Classification Error provided by the uncertainty forecast (Q(%s))',1-p.quantiles))+
                                scale_fill_manual(values=c('scs'="#92D050",
                                                           'ucu'="#4F6228",
                                                           'scu [FA]'="#FF9900",
                                                           'ucs [MA]'="#C00000"),
                                                  limits = c("ucu","scs","scu [FA]","ucs [MA]"))+
                                scale_y_continuous(labels=scaleFUN)+guides(fill=guide_legend(ncol=2))
                            
                            fchart1 <- unique(fchart[,c("timestamps","variable","Frequency","DACFclassification","FOprobclassification")])
                            p5 <-  ggplot(fchart, aes(x = timestamps, y = Frequency, fill = SNclassification)) +  
                                geom_bar(stat = "identity") +
                                theme_bw() +
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits =lims.date)+
                                theme(axis.text.x = element_text(angle = 90, hjust = 1),
                                      axis.text=element_text(colour='black'),
                                      axis.text.y=element_text(colour="white"),
                                      text=element_text(size=10),
                                      legend.key.size=unit(0.35, 'cm'),
                                      legend.title=element_blank(),
                                      plot.title = element_text(size=12))+
                                guides(fill=guide_legend(ncol=2))+
                                ylab('')+
                                ggtitle('TRUE classification provided by the SN')+
                                scale_fill_manual(values=c('Secure'="#92D050",'Unsecure'="#4F6228"),
                                                  limits = c("Secure","Unsecure"))+
                                scale_y_continuous(labels=scaleFUN)
                            
                            #######CHANGE!
                            fchart1 <- unique(fchart[,c("timestamps","variable","Unsecure","Secure")])
                            fchart1 <- melt(fchart1,id.vars=c("timestamps","variable"))
                            names(fchart1) <- c('timestamps','variable','percentage.ens','value.percentage.ens')
                            fchart1$percentage.ens <- factor(fchart1$percentage.ens, levels = c('Secure','Unsecure'))
                            fchart1$value.percentage.ens <- fchart1$value.percentage.ens*100
                            p6 <- ggplot(fchart1, aes(x = timestamps, y = value.percentage.ens,
                                                      fill = percentage.ens))+ 
                                geom_bar(stat = "identity", position = "stack")+
                                scale_x_datetime(date_breaks = t.steps,
                                                 limits =lims.date)+
                                xlab('Timestamps')+ ylab('')+
                                ggtitle("Relative frequency of secure/unsecure uncertainty forecasting members (%)")+
                                theme_bw()+
                                my_theme()+
                                theme(axis.text.y=element_text(size=7),
                                      legend.title=element_blank(),
                                      plot.title = element_text(size=12))+
                                guides(fill=guide_legend(ncol=2))+
                                scale_fill_manual(name="", 
                                                  values=c('Secure'="#92D050",'Unsecure'="#4F6228"),
                                                  limits = c("Secure","Unsecure"))+
                                scale_y_continuous(limit=c(0,1)*100,breaks=c(0.05,.5,0.95)*100)
                            rm(fchart1)
                            
                            plotss[[i]] <- VAlignPlots(p1,p2,p3,p4,p6,p5,
                                                       hts=c(0.4,0.26,0.13,0.13,0.25,0.45),
                                                       title=sprintf('%s: %s (contingency %s)',
                                                                     Week,var,contingency))
                            
                            
                            
                            rm(p1,p2)
                            i=i+1
                        }
                    }
                    
                    if (length(plotss)>0){
                        rm(fchart,values.3)
                        pdf(sprintf('%s/univariate/TimeEvolution/ %s.pdf',results.path,var),paper='a4')
                        
                        for (i in 1:length(plotss)){
                            grid.arrange(plotss[[i]])
                        }
                        
                        dev.off()
                    }
                }
                
                if (do.parallel==TRUE){
                    # START parallel computation
                    cl <- makeCluster(cl.to.use)
                    registerDoParallel(cl)
                    
                    foreach (var = lines.with.overloads.,
                             .packages=c('lubridate','ggplot2','reshape2','gridExtra','grid')) %dopar% plot.uni.vars(var)
                    stopCluster(cl)
                } else{
                    for (var in sort(as.character(unique(df.selected$variable)))){
                        plot.uni.vars(var)
                    }
                }
                
                # END parallel computation
                time. <- proc.time()
                cat('|-> Total time:', (time.-time.sc)[3],'seconds \n')
                
                computation.times <- rbind(computation.times,c('Time Evolution by line',(time.-time.sc)[3]))
                time.sc=time.
                
                }
            rm(df.selected)
            
            
            #  --- BY SYSTEM ---
            df.SN.class.system <- security.class$SN.classification.by.system
            df.SN.class.system$timestamps <- row.names(df.SN.class.system)
            df.SN.class.system <- melt(df.SN.class.system,id.vars=c('timestamps','SNsystemSTATE')) 
            names(df.SN.class.system) <- c('timestamps','SNsystemSTATE','SNclassification','FreqSN')
            
            df.DACF.class.system <- security.class$DACF.classification.by.system
            rm(security.classDACF)
            df.DACF.class.system$timestamps <- row.names(df.DACF.class.system)
            df.DACF.class.system <- melt(df.DACF.class.system,id.vars=c('timestamps','DACFsystemSTATE')) 
            names(df.DACF.class.system) <- c('timestamps','DACFsystemSTATE','DACFclassification','FreqDACF')
            levels(df.DACF.class.system$DACFclassification)[levels(df.DACF.class.system$DACFclassification)=="ucs"] <- "ucs [MA]"
            levels(df.DACF.class.system$DACFclassification)[levels(df.DACF.class.system$DACFclassification)=="scu"] <- "scu [FA]"
            
            df.FOprob.class.system <- security.class$FOprob.classification.by.system
            df.FOprob.class.system$timestamps <- row.names(df.FOprob.class.system)
            df.FOprob.class.system <- melt(df.FOprob.class.system,id.vars=c('timestamps','FOprobsystemSTATE')) 
            names(df.FOprob.class.system) <- c('timestamps','FOprobsystemSTATE','FOprobclassification','FreqFOprob')
            levels(df.FOprob.class.system$FOprobclassification)[levels(df.FOprob.class.system$FOprobclassification)=="ucs"] <- "ucs [MA]"
            levels(df.FOprob.class.system$FOprobclassification)[levels(df.FOprob.class.system$FOprobclassification)=="scu"] <- "scu [FA]"
            
            
            df.systemSTATE <- Reduce(function(x, y) merge(x, y, by=c("timestamps"),all.x=T), 
                                     list(df.SN.class.system,
                                          df.DACF.class.system,
                                          df.FOprob.class.system))
            
            df.systemSTATE$Week <- sprintf('Week %s of %s',strftime(df.systemSTATE$timestamps,format="%W"),
                                           strftime(df.systemSTATE$timestamps,format="%Y")) 
            
            df.systemSTATE$timestamps = as.POSIXct(df.systemSTATE$timestamps, 
                                                   format = "%Y-%m-%d %H:%M:00",
                                                   tz="Europe/Paris")
            
            
           if (time.evolution.plots){
################################################################################
# Time Evolution for the global system
################################################################################
                cat('Saving Global System Time Evolution Plots...\n')
                scaleFUN <- function(x) sprintf("%.3f", x)
                colfunc <- colorRampPalette(c("slategray1", "slateblue3"))
                cols.fill <-  colfunc(4)
                i = 1
                plotss <- list()
                for (Week in sort(unique(df.systemSTATE$Week))){
                    fchart <- df.systemSTATE[df.systemSTATE$Week==Week,]
                    t.steps='6 hours'
                    lims.date = c(floor_date(fchart$timestamps[1], 
                                             unit="week",
                                             week_start = getOption("lubridate.week.start", 1)),
                                  ceiling_date(fchart$timestamps[1], 
                                               unit="week",
                                               week_start = getOption("lubridate.week.start", 1)))
                    
                    
                    fchart.DACFsystem <- unique(fchart[,c("timestamps","DACFsystemSTATE")])
                    fchart.DACFsystem$Freq <- 1
                    tt <- c(sprintf("Global System Classification Error provided by the\n1) deterministic forecast (with %s%% SM)",round(100*SM,2)))
                    p1 <- ggplot(fchart.DACFsystem, aes(x = timestamps, y = Freq,
                                                      fill = DACFsystemSTATE))+ 
                      geom_bar(stat = "identity", position = "stack")+
                      scale_x_datetime(date_breaks = t.steps,limits =lims.date)+
                      xlab('Timestamps')+ ylab('')+
                      ggtitle(paste(tt, sep='\n'))+
                      my_theme()+
                      scale_fill_manual(drop=FALSE,
                                        name="",
                                        values=c('scs'="#92D050",
                                                 'ucu'="#4F6228",
                                                 'scu [FA]'="#FF9900",
                                                 'ucs [MA]'="#C00000"), 
                                        limits = c("ucu","scs","scu [FA]","ucs [MA]"))+
                     guides(fill=guide_legend(ncol=2))
                    rm(fchart.DACFsystem)
                    
                    
                    fchart.FOprob.system <- unique(subset(fchart,select=c("timestamps","FOprobsystemSTATE")))
                    fchart.FOprob.system$Freq <- 1
                    
                    p2 <- ggplot(fchart.FOprob.system, aes(x = timestamps, y = Freq,
                                                      fill = FOprobsystemSTATE))+ 
                      geom_bar(stat = "identity")+
                      scale_x_datetime(date_breaks = t.steps,
                                       limits =lims.date)+
                        my_theme()+
                        xlab('Timestamps')+ ylab('')+ 
                        ggtitle(sprintf('2) uncertainty forecast (with Q(%s))',1-p.quantiles))+
                      scale_fill_manual(drop=FALSE,
                                        values=c('scs'="#92D050",
                                                 'ucu'="#4F6228",
                                                 'scu [FA]'="#FF9900",
                                                 'ucs [MA]'="#C00000"),
                                        limits = c("ucu","scs","scu [FA]","ucs [MA]"))+
                        guides(fill=guide_legend(ncol=2))
                    rm(fchart.FOprob.system)
                    
                    
                    
                    
                    fchart.SNsystem <- unique(fchart[,c("timestamps","SNsystemSTATE")])
                    fchart.SNsystem$Freq <- 1
                    p3 <- ggplot(fchart.SNsystem, aes(x = timestamps, y = Freq,
                                                fill = SNsystemSTATE))+ 
                      geom_bar(stat = "identity", position = "stack")+
                      scale_x_datetime(date_breaks = t.steps,
                                       limits =lims.date)+
                      xlab('Timestamps')+ ylab('')+ 
                        ggtitle(expression(bold("Number of true secure/unsecure timestamps (from the SN)")))+
                      my_theme()+
                      scale_fill_manual(name="", 
                                        values=c('Secure'="#92D050",'Unsecure'="#4F6228"),
                                        limits = c("Secure","Unsecure"))
                    rm(fchart.SNsystem)
                    
                    fchart.SN <- unique(fchart[,c("timestamps","SNclassification","FreqSN")])
                    fchart.SN$SNclassification <- factor(fchart.SN$SNclassification, levels = c('Secure', 'Unsecure'))
                    
                    p4 <- ggplot(fchart.SN, aes(x = timestamps, y = FreqSN,
                                             fill = SNclassification))+ 
                        geom_bar(stat = "identity", position = "stack")+
                        scale_x_datetime(date_breaks = t.steps,
                                         limits =lims.date)+
                        theme_bw()+ 
                        ylab('')+
                        ggtitle(expression(atop(bold("Number of true secure/unsecure transmission line flows (from the SN)"))))+
                        theme(axis.text.x = element_text(angle = 90, hjust = 1),
                              legend.key.size=unit(0.35, 'cm'),
                              plot.title = element_text(hjust = 0, vjust=0,size=12),
                              text=element_text(size=10),
                              legend.title=element_blank())+
                        guides(fill=guide_legend(ncol=1))+
                        scale_y_continuous(limits=c(0,length(branches.with.data)),
                                           breaks=c(0,length(branches.with.data),ceiling(0.5*length(branches.with.data))))+
                        scale_fill_manual(drop = FALSE,
                                          name="", 
                                          values=c('Secure'="#92D050",'Unsecure'="#4F6228"),
                                          limits = c("Secure","Unsecure"))
                    
                    fchart.DACF <- unique(fchart[,c("timestamps","DACFclassification","FreqDACF")])
                    fchart.DACF <- fchart.DACF[fchart.DACF$DACFclassification!='scs',]
                    fchart.DACF$DACFclassification <- factor(fchart.DACF$DACFclassification, levels = c('scu [FA]', 'ucs [MA]', 'ucu'))
                    
                    tt. <- c(sprintf("Number of transmission line flows with security issues provided by\n1) deterministic forecast (with %s%% SM)",round(100*SM,2)))
                    
                    p5 <- ggplot(fchart.DACF, aes(x = timestamps, y = FreqDACF,
                                                fill = DACFclassification))+ 
                        geom_bar(stat = "identity", position = "stack")+
                        scale_x_datetime(date_breaks = t.steps,
                                         limits =lims.date)+
                        ggtitle(paste(tt., sep='\n'))+
                        scale_fill_manual(drop=FALSE,name="", 
                                          values=c(#'scs'="#92D050",
                                                   'ucu'="#4F6228",
                                                   'scu [FA]'="#FF9900",
                                                   'ucs [MA]'="#C00000"),
                                          limits = c("ucu","scu [FA]","ucs [MA]"))+
                        xlab('Timestamps')+ ylab('')+
                        theme_bw()+
                        theme(axis.text=element_text(colour='black'),
                              axis.title.x = element_blank(), 
                              axis.text.x = element_blank(),
                              text=element_text(size=10),
                              legend.key.size=unit(0.35, 'cm'),
                              legend.title=element_blank(),
                              plot.title = element_text(size=12))
                        guides(fill=guide_legend(ncol=1))
                    
                    fchart.FOprob <- unique(fchart[,c("timestamps","FOprobclassification","FreqFOprob")])
                    fchart.FOprob <- fchart.FOprob[fchart.FOprob$FOprobclassification!='scs',]
                    fchart.FOprob$FOclassification <- factor(fchart.FOprob$FOprobclassification, levels = c('scu [FA]', 'ucs [MA]', 'ucu'))
                    p6 <- ggplot(fchart.FOprob, aes(x = timestamps, y = FreqFOprob,
                                                  fill = FOclassification))+ 
                        geom_bar(stat = "identity", position = "stack")+
                        scale_x_datetime(date_breaks = t.steps,limits =lims.date)+ 
                        xlab('Timestamps')+ ylab('')+
                        theme_bw()+
                        theme(axis.text=element_text(colour='black'),
                              axis.title.x = element_blank(), 
                              axis.text.x = element_blank(),
                              text=element_text(size=10),
                              legend.key.size=unit(0.35, 'cm'),
                              legend.title=element_blank(),
                              plot.title = element_text(size=12))+
                        ggtitle(sprintf('2) uncertainty forecast (with Q(%s))',1-p.quantiles))+
                        guides(fill=guide_legend(ncol=1))+
                        scale_fill_manual(drop=FALSE,name="", 
                                          values=c('ucu'="#4F6228",
                                                   'scu [FA]'="#FF9900",'ucs [MA]'="#C00000"),
                                          limits = c("ucu","scu [FA]","ucs [MA]"))
                    
                    plotss[[i]] <- VAlignPlots(p1,p2,p3,p5,p6,p4,
                                               hts=c(0.092,0.07,0.075,0.28,0.24,0.39),
                                               title=sprintf('%s (contingency %s)',
                                                       Week,contingency),
                                               keepTitles=TRUE,
                                               keepXAxisLegends=TRUE)
                    rm(p1,p2)
                    i=i+1
                }
                
                rm(fchart)
                pdf(sprintf('%s/multivariate/system_time_evolution.pdf',results.path),paper='a4',height=14)
                for (i in 1:length(plotss)){
                    grid.arrange(plotss[[i]])
                }
                dev.off()
                
                time. <- proc.time()
                cat('|-> Total time:', (time.-time.sc)[3],'seconds \n')
                
                computation.times <- rbind(computation.times,c('Time Evolution for global system',(time.-time.sc)[3]))
                time.sc=time.
           }
            
            # Now the DACF
            security.class <- security.classification(B,c=1,SM=0,cl.to.use=max(round(cl.to.use/2),2))
            
            if (trade.off.analysis){
################################################################################
# Trade off analysis
################################################################################
                cat('Performing trade-off analysis...\n')
                
                DACF.errors <- security.class$DACF.classification.by.system
                DACF.errors <- subset(DACF.errors,select='DACFsystemSTATE')
                DACF.errors <- replace(DACF.errors, TRUE, 
                                       lapply(DACF.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                x <- data.frame(data=as.numeric(DACF.errors[,1]))
                timestamps <- as.POSIXct(row.names(DACF.errors), 
                                         format = "%Y-%m-%d %H:%M:00",
                                         tz="Europe/Paris")
                x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                if (bootstrap.random.ind){
                    IC.bootstrap.DACF. <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                }else{
                    IC.bootstrap.DACF. <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                }
                IC.bootstrap.DACF.$variable <- 'System'
                IC.bootstrap.DACF.$c <- 'DACF'
                
                IC.bootstrap <- data.frame(IC.bootstrap.DACF.)
                
                do.trade.off <- function(c0){  
                    security.class <- security.classification(B,c=c0,cl.to.use=max(round(cl.to.use/2),2))
                    # for all system
                    df.SN.system <- security.class$SN.classification.by.system
                    
                    df.FOprob.system <- security.class$FOprob.classification.by.system
                    
                    
                    tab <- as.matrix(table(df.FOprob.system$FOprobsystemSTATE))
                    tab. <- data.frame(matrix(0,1,4))
                    names(tab.) <- c("ucu","scs","scu [FA]","ucs [MA]")
                    tab.[row.names(tab)] <- as.numeric(tab)/sum(as.numeric(tab))
                    
                    FOprob.errors <- security.class$FOprob.classification.by.system
                    FOprob.errors <- subset(FOprob.errors,select='FOprobsystemSTATE')
                    FOprob.errors <- replace(FOprob.errors, TRUE, 
                                             lapply(FOprob.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                    x <- data.frame(data=as.numeric(FOprob.errors[,1]))
                    timestamps <- as.POSIXct(row.names(FOprob.errors), 
                                             format = "%Y-%m-%d %H:%M:00",
                                             tz="Europe/Paris")
                    x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                    if (bootstrap.random.ind){
                        IC.bootstrap.FOprob <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    } else{
                        IC.bootstrap.FOprob <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    }
                    IC.bootstrap.FOprob$variable <- 'System'
                    IC.bootstrap.FOprob$c <- sprintf('Q(%s)',c0)
                    
                    IC.bootstrap. <- data.frame(IC.bootstrap.FOprob)
                    
                    for (var in lines.with.overloads){
                        FOprob.errors <- security.class$FOprob.classification.by.branch
                        FOprob.errors <- subset(FOprob.errors,select=var)
                        FOprob.errors <- replace(FOprob.errors, TRUE, 
                                                 lapply(FOprob.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                        x <- data.frame(data=as.numeric(FOprob.errors[,1]))
                        timestamps <- as.POSIXct(row.names(FOprob.errors), 
                                                 format = "%Y-%m-%d %H:%M:00",
                                                 tz="Europe/Paris")
                        x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                        if (bootstrap.random.ind){
                            IC.bootstrap.FOprob <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                        }else{
                            IC.bootstrap.FOprob <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                        }
                        IC.bootstrap.FOprob$variable <- var
                        IC.bootstrap.FOprob$c <- sprintf('Q(%s)',c0)
                        
                        IC.bootstrap. <- rbind(IC.bootstrap.,data.frame(IC.bootstrap.FOprob))
                        
                    }
                    
                    IC.bootstrap.
                }
                
                IC.bootstrap.Q <- do.trade.off(0.3)
                IC.bootstrap <- rbind(IC.bootstrap,IC.bootstrap.Q)
                qs. <- c(seq(0.3,0.95,0.05),0.96,0.97,0.98,0.99,1.0)
                
                ind = 1
                while ((IC.bootstrap.Q$MA.IC.sup[IC.bootstrap.Q$variable=='System'] > IC.bootstrap.DACF.$MA.IC.inf[IC.bootstrap.DACF.$variable=='System']) & (ind<=19)){
                    ind = ind+1
                    IC.bootstrap.Q <- do.trade.off(qs.[ind])
                    IC.bootstrap <- rbind(IC.bootstrap,IC.bootstrap.Q)
                }
                
                qs. <- qs.[1:ind]
                
             
                do.CI.DACF <- function(var){  
                    DACF.errors <- security.class$DACF.classification.by.branch
                    DACF.errors <- subset(DACF.errors,select=var)
                    DACF.errors <- replace(DACF.errors, TRUE, 
                                           lapply(DACF.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                    x <- data.frame(data=as.numeric(DACF.errors[,1]))
                    timestamps <- as.POSIXct(row.names(DACF.errors), 
                                             format = "%Y-%m-%d %H:%M:00",
                                             tz="Europe/Paris")
                    x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                    if (bootstrap.random.ind){
                        IC.bootstrap.DACF. <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    } else{
                        IC.bootstrap.DACF. <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    }
                    IC.bootstrap.DACF.$variable <- var
                    IC.bootstrap.DACF.$c <- 'DACF'
                    return(data.frame(IC.bootstrap.DACF.))
                }
                
                for (var in lines.with.overloads){
                    IC.bootstrap.D <- do.CI.DACF(var)
                    IC.bootstrap <- rbind(IC.bootstrap,IC.bootstrap.D)
                }
               
                 
                do.trade.off.DACF.margin <- function(c0){  
                    security.class <- security.classification(B,c=0.95,SM=c0,cl.to.use=max(round(cl.to.use/2),2))
                    # for all system
                    DACFmargin.errors <- security.class$DACF.classification.by.system
                    DACFmargin.errors <- subset(DACFmargin.errors,select='DACFsystemSTATE')
                    DACFmargin.errors <- replace(DACFmargin.errors, TRUE, 
                                             lapply(DACFmargin.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                    x <- data.frame(data=as.numeric(DACFmargin.errors[,1]))
                    timestamps <- as.POSIXct(row.names(DACFmargin.errors), 
                                             format = "%Y-%m-%d %H:%M:00",
                                             tz="Europe/Paris")
                    x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                    if (bootstrap.random.ind){
                        IC.bootstrap.DACFmargin <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    } else{
                        IC.bootstrap.DACFmargin <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                    }
                    IC.bootstrap.DACFmargin$variable <- 'System'
                    IC.bootstrap.DACFmargin$c <- sprintf('%sDACF',(1+c0))
                    IC.bootstrap. <- data.frame(IC.bootstrap.DACFmargin)
                    for (var in lines.with.overloads){
                        DACFmargin.errors <- security.class$DACF.classification.by.branch
                        DACFmargin.errors <- subset(DACFmargin.errors,select=var)
                        DACFmargin.errors <- replace(DACFmargin.errors, TRUE, 
                                                 lapply(DACFmargin.errors, factor, levels = c('scu [FA]', 'scs','ucs [MA]', 'ucu')))
                        x <- data.frame(data=as.numeric(DACFmargin.errors[,1]))
                        timestamps <- as.POSIXct(row.names(DACFmargin.errors), 
                                                 format = "%Y-%m-%d %H:%M:00",
                                                 tz="Europe/Paris")
                        x$Week <- sprintf('Day %s of Week %s of %s',strftime(timestamps,format="%d"),strftime(timestamps,format="%W"),strftime(timestamps,format="%Y")) 
                        if (bootstrap.random.ind){
                            IC.bootstrap.DACFmargin <- bootstrap.random(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                        }else{
                            IC.bootstrap.DACFmargin <- bootstrap.week(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=3000)#bootstrap.t.IC(x,alpha.=0.05,interest.levels=c(1,3),n.boot.samples=2000)
                        }
                        IC.bootstrap.DACFmargin$variable <- var
                        IC.bootstrap.DACFmargin$c <- sprintf('%sDACF',(1+c0))
                        IC.bootstrap. <- rbind(IC.bootstrap.,data.frame(IC.bootstrap.DACFmargin))
                    }
                    
                    IC.bootstrap.
                }
                
                
                IC.bootstrap.DACFM. <- do.trade.off.DACF.margin(0.01)
                IC.bootstrap <- rbind(IC.bootstrap,IC.bootstrap.DACFM.)
                DACF.SMs <- seq(0.01,0.2,0.01)
                
                ind = 1
                while ((IC.bootstrap.DACFM.$MA.IC.sup[IC.bootstrap.DACFM.$variable=='System'] > IC.bootstrap.DACF.$MA.IC.inf[IC.bootstrap.DACF.$variable=='System']) & (ind<=20)){
                    ind = ind+1
                    IC.bootstrap.DACFM. <- do.trade.off.DACF.margin(DACF.SMs[ind])
                    IC.bootstrap <- rbind(IC.bootstrap,IC.bootstrap.DACFM.)
                }
                
                DACF.SMs <- DACF.SMs[1:ind]
                
                
                pdf(sprintf('%s/multivariate/TradeOff.pdf',results.path),paper='a4',height=7)
                for (var in c('System',sort(lines.with.overloads))){
                    df. <- IC.bootstrap[IC.bootstrap$variable==var,] 
                    
                    df.1 <- df.[df.$c=='DACF',]
                    st <- sprintf('%sDACF',(1+DACF.SMs))
                    df.2 <- df.[df.$c%in%st,]
                    df. <- df.[!df.$c%in%st,]
                    df. <- df.[!df.$c%in%c('DACF'),]
                    
                    p <- ggplot(df.,aes(x=meanMA,y=meanFA))+
                        geom_point(size=0.8)+
                        geom_point(data=df.1,aes(x=meanMA,y=meanFA,colour='blue'))+
                        geom_point(data=df.2,aes(x=meanMA,y=meanFA,colour='gray60'),size=0.8)+
                        geom_text(aes(label=c),hjust=-0.1, vjust=-0.1,size=1.8)+
                        theme_bw() +
                        geom_text(data=df.1,aes(label=c),hjust=-0.1, vjust=-0.1,size=2)+
                        geom_text(data=df.2,aes(label=c),hjust=-0.1, vjust=-0.1,size=1.8,color='grey60')+
                        xlab('Frequency of Missed Alarms [MA]')+
                        ylab('Frequency of False Alarms [FA]')+
                        theme(legend.position="none")+
                        scale_colour_identity()+
                        ggtitle(sprintf('%s',var))+
                        theme(strip.background =element_rect(fill="azure2"),
                              panel.border = element_rect(colour = "black"),
                              text=element_text(family="serif",size=9),
                              axis.text=element_text(colour='black'))+
                         scale_y_continuous(limit=c(0,1.1*max(df.$FA.IC.sup,df.2$FA.IC.sup)))+
                        scale_x_continuous(limit=c(0,1.1*max(df.$MA.IC.sup,df.2$MA.IC.sup)))+
                        labs(caption = sprintf('\t\tContingency: %s',contingency))
                    print(p)
                    
                    p <- ggplot(df.,aes(x=meanMA,y=meanFA))+
                        geom_point(data=df.1,aes(x=meanMA,y=meanFA,colour='blue'))+
                        geom_point(data=df.2,aes(x=meanMA,y=meanFA,colour='gray'),size=0.8)+
                        geom_errorbarh(data=df.2,aes(xmax = MA.IC.inf, xmin = MA.IC.sup, height = .00,group=c,color='grey60'))+
                        geom_errorbar(data=df.2,aes(ymin=FA.IC.inf, ymax=FA.IC.sup), width=.00,position=position_dodge(0.05),color='grey60') +
                        geom_errorbarh(data=df.1,aes(xmax = MA.IC.inf, xmin = MA.IC.sup, height = .00,group=c,color='blue'))+
                        geom_errorbar(data=df.1,aes(ymin=FA.IC.inf, ymax=FA.IC.sup), width=.00,position=position_dodge(0.05),color='blue')+
                        geom_errorbarh(aes(xmax = MA.IC.inf, xmin = MA.IC.sup, height = .00,group=c,color='#C00000'))+
                        geom_errorbar(aes(ymin=FA.IC.inf, ymax=FA.IC.sup), width=.00,position=position_dodge(0.05),color='#FF9900')+
                        geom_point(size=0.8)+
                        geom_text(aes(label=c),hjust=-0.1, vjust=-0.1,size=2)+
                        geom_text(data=df.1,aes(label=c),hjust=-0.1, vjust=-0.1,size=2)+
                        geom_text(data=df.2,aes(label=c),hjust=-0.1, vjust=-0.1,size=1.8,color='grey30')+
                        theme_bw() +
                        xlab('Frequency of Missed Alarms [MA]')+
                        ylab('Frequency of False Alarms [FA]')+
                        theme(legend.position="none")+
                        scale_colour_identity()+
                        ggtitle(sprintf('%s',var))+
                        theme(strip.background =element_rect(fill="azure2"),
                              panel.border = element_rect(colour = "black"),
                              text=element_text(family="serif",size=9),
                              axis.text=element_text(colour='black'))+
                        scale_y_continuous(limit=c(0,1.1*max(df.$FA.IC.sup,df.2$FA.IC.sup)))+
                        scale_x_continuous(limit=c(0,1.1*max(df.$MA.IC.sup,df.2$MA.IC.sup)))+
                        labs(caption = sprintf('\t\tContingency: %s',contingency))
                    print(p)
                }
                
                dev.off()
                
                time. <- proc.time()
                cat('|-> Total time:', (time.-time.sc)[3],'seconds \n')
                
                computation.times <- rbind(computation.times,c('Trade-off for p value (in security rule)',(time.-time.sc)[3]))
                time.sc=time.
                
                aux <- IC.bootstrap[IC.bootstrap$variable=='System',c("c","variable","meanFA","FA.IC.inf","FA.IC.sup","meanMA","MA.IC.inf","MA.IC.sup")] 
                names(aux) <- c("situation","variable","FA","FA.IC.inf","FA.IC.sup","MA","MA.IC.inf","MA.IC.sup")
                aux[is.na(aux)] <- 0
                aux[,3:8] <- 100*aux[,3:8]
                write.table(aux,file=sprintf('%s/multivariate/TradeOff.csv',
                                                   results.path),sep=',',dec='.',row.names = F)
                rm(aux)
            
            }
            
            
################################################################################
# Classification Errors Summary
################################################################################
            cat('General classification plot...\n')
            i = 1
            plotss <- list()
            df.DACF <- unique(subset(df.systemSTATE,select=c("timestamps","Week","DACFsystemSTATE")))
            df.FOprob <- unique(subset(df.systemSTATE,select=c("timestamps","Week","FOprobsystemSTATE")))
            p <- plot.summary(df.DACF,df.FOprob,title.=sprintf('Global System Classification Errors (contingency %s)',contingency),aux=NULL,p.quantiles=p.quantiles,SM=SM,bootstrap.random.ind)
            plotss[[i]] <- p$p.final
            IC.infos <- p$metrics
            IC.infos$line <- 'Global System'
            for (var in lines.with.overloads.){
                cat(round(i/length(lines.with.overloads.),2)*100,' % \r')
                
                data.selected <- round(data.stat[row.names(data.stat)==sprintf('%s_I',var),],3)
                aux <- data.frame(matrix(NA,3,8))
                aux[1,] <-  as.numeric(data.selected[,names(data.selected)[grepl( "DACF" , names(data.selected) )]])
                aux[2,] <-  as.numeric(data.selected[,names(data.selected)[grepl("Q", names(data.selected) )]])
                aux[3,] <-  as.numeric(data.selected[,names(data.selected)[grepl( "SN" , names(data.selected) )]])
                
                row.names(aux)<-c('Deterministic FO','Uncertainty FO','SN')
                names(aux) <- c('mu','sigma','Min','Max', 'mu (overload)', 'sigma (overload)', 'Min (overload)', 'Max (overload)')
                aux$'Nr.timestamps' <- data.selected$`Nr total timestamps`
                aux$'Nr.over.timestamps' <- c(data.selected$`Nr of DACF overloads`,data.selected$`Nr of Q(1-p) overloads`,data.selected$`Nr of real overloads`)
                
                i <- i+1
                df <- df.classification.original[df.classification.original$variable==var,]
                df.DACF <- df[,c("timestamps", "DACF classification")]
                #df.DACF$Week <- sprintf('Day %s of Week %s of %s',strftime(df.DACF$timestamps,format="%d"),strftime(df.DACF$timestamps,format="%W"),strftime(df.DACF$timestamps,format="%Y")) 
                df.DACF$Week <- sprintf('Week %s of %s',strftime(df.DACF$timestamps,format="%W"),strftime(df.DACF$timestamps,format="%Y")) 
                df.DACF <- df.DACF[,c("timestamps","Week","DACF classification")]
                
                df.FOprob <- df[,c("timestamps", "FO prob classification")]
                df.FOprob$Week <- sprintf('Week %s of %s',strftime(df.FOprob$timestamps,format="%W"),strftime(df.FOprob$timestamps,format="%Y")) 
                df.FOprob <- df.FOprob[,c("timestamps","Week","FO prob classification")]
                p <- plot.summary(df.DACF,df.FOprob,title.=sprintf('Classification Errors for %s (contingency %s)',var,contingency),aux=aux,p.quantiles=p.quantiles,bootstrap.random.ind=bootstrap.random.ind)
                plotss[[i]] <- p$p.final
                pp <- p$metrics
                pp$line <- var
                IC.infos <- rbind(IC.infos,pp)
                rm(df,df.DACF,df.FOprob)
            }
            
            pdf(sprintf('%s/multivariate/ClassificationErrors.pdf',results.path),paper='a4',height=14)
            for (i in 1:length(plotss)){
                grid.arrange(plotss[[i]])
            }
            dev.off()
        
            write.table(IC.infos[,c(25,1:24)],file=sprintf('%s/multivariate/ClassificationErrors.csv',results.path),sep=',',dec='.',row.names = FALSE)
            
            time. <- proc.time()
            cat('|-> Total time:', (time.-time.sc)[3],'seconds \n')
            
            computation.times <- rbind(computation.times,c('General classification plot',(time.-time.sc)[3]))
            
            computation.times <- rbind(computation.times,c('Total time',round((time.-initial.time)[3],2)))
            
            computation.times <- data.frame(computation.times)
            names(computation.times) <- c('Task','Time(s)')
            
            write.table(computation.times,file=sprintf('%s/computation_times.csv',results.path),sep=',',dec='.',row.names = F)
            
            }
    }
}