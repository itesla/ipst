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

path_tool <-'C:/Users/rcode_task3'

source(sprintf('%s/main_functions.R',path_tool))
# read configuration file
df.parameters <- read.csv(sprintf('%s/config_file.txt',path_tool),header=T)

#paths
results.path <- as.character(df.parameters[df.parameters$Parameter=='results_path',2])
data.path <- as.character(df.parameters[df.parameters$Parameter=='data_path',2])
# value of p
x <- df.parameters[df.parameters$Parameter=='p-value',2]
p.quantiles <- as.numeric(levels(x)[x])
#plot?
time.evolution.plots=TRUE
sensitivity.plot <- as.logical(df.parameters[df.parameters$Parameter=='Sensitivity Analysis',2])
spread.plot <- as.logical(df.parameters[df.parameters$Parameter=='Spread Plot',2])
qq.plot <- as.logical(df.parameters[df.parameters$Parameter=='QQ Plot',2])
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
# boxplot chosen timestamp
boxplot.timestamp <-as.character(df.parameters[df.parameters$Parameter=='Boxplot.timestamp',2])
x <- df.parameters[df.parameters$Parameter=='Minimum timestamps',2]
minimum.timestamp <- as.numeric(levels(x)[x])
rm(x)
if (nrow(df.parameters)>16){
    selected.vars <- as.character(df.parameters[-c(1:16),1])
} else {selected.vars <- NULL}

# parallel?
do.parallel <- as.logical(df.parameters[df.parameters$Parameter=='do_parallel',2])
if (do.parallel==TRUE){
    x <- df.parameters[df.parameters$Parameter=='Cores',2]
    selected.cores <- as.numeric(levels(x)[x])
    cl.to.use <- min(selected.cores, detectCores(logical=TRUE))
} 

rm(df.parameters)

# Create paths to save results
dir.create(file.path(results.path), showWarnings = FALSE)
dir.create(file.path(sprintf('%s/univariate/',results.path)), showWarnings = F)
dir.create(file.path(sprintf('%s/univariate/TimeEvolution',results.path)), 
           showWarnings = F)
dir.create(file.path(sprintf('%s/multivariate/',results.path)), 
           showWarnings = F)



################################################################################
# 1. READ AND PROCESS DATA
################################################################################
computation.times <- c()
# read all data
cat('Reading data...\n')
initial.time <- proc.time()

B <- read.data.function(data.path,selected.vars,date.ini=date.ini,date.fin=date.fin)

# save normalization values
df <- as.data.frame(B$indicator.variables[,-c(1,2)])
names(df) <- c('Imax (A)','Smax (A)','timestamp')
df <- df[,c('Imax (A)','timestamp')]
write.table(df,file=sprintf('%s/univariate/normalization_values.csv',results.path),sep=',',dec='.',col.names = NA)
rm(df)
# separate data according P signal
B.separated <- separate.data(B$data,min.timestamps=minimum.timestamp)

rm(B) # remove B list from memory
time.read.data <- proc.time()
cat('|-> Total time:', (time.read.data - initial.time)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Read data',(time.read.data - initial.time)[3]))
################################################################################
# 2. UNIVARIATE ANALYSIS
################################################################################
nr.ensembles <- ncol(B.separated$data.I[[1]])-1 # number of ensembles

#-----------------------------
## 2.1 UNIVARIATE RANK HISTOGRAM
#-----------------------------

positive.data.indicator <- ifelse(length(B.separated$data.I.pos)>0,1,0)
negative.data.indicator <- ifelse(length(B.separated$data.I.neg)>0,1,0)


if ((positive.data.indicator+negative.data.indicator)==0){#if does not exist data then stop the code
    cat('There is no observations! Please check the config_file parameters...')
} else{ # otherwise the analysis is performed
    
cat('Computing and saving Univariate Rank Histogram...\n')
# POSITIVE cases
if (positive.data.indicator){
    talagrand.diag.I.pos <- as.data.frame(Talagrand.diagram(B.separated$data.I.pos))
    names(talagrand.diag.I.pos) <- sprintf('%s_pos',names(talagrand.diag.I.pos))
    talagrand.diag.I.pos <- subset(talagrand.diag.I.pos,
                                   select=names(talagrand.diag.I.pos))[!is.na(colSums(talagrand.diag.I.pos))]
}

# NEGATIVE cases
if (negative.data.indicator){
    talagrand.diag.I.neg <- as.data.frame(Talagrand.diagram(B.separated$data.I.neg))
    names(talagrand.diag.I.neg) <- sprintf('%s_neg',names(talagrand.diag.I.neg))
    talagrand.diag.I.neg <- subset(talagrand.diag.I.neg,
                                   select=names(talagrand.diag.I.neg))[!is.na(colSums(talagrand.diag.I.neg))]
}

scaleFUN <- function(x) sprintf("%.3f", x) # precision to use in plot axis
# save univariate rank histogram for positive case (if it exists)
for (case in c('pos','neg')){
    if (case=='pos'){
        indicator <- positive.data.indicator
        if (indicator){talagrand.diag <- talagrand.diag.I.pos
        power.id <- '$P_{ik} \\geq 0$'}
    } else{
        indicator <- negative.data.indicator
        if (indicator){talagrand.diag <- talagrand.diag.I.neg
        power.id <- '$P_{ik} < 0$'}
    }
   if (indicator){     
        write.table(talagrand.diag,
                    file=sprintf('%s/univariate/UnivariateRankHistogram_%s.csv',
                    results.path,case),sep=',',dec='.',col.names = NA)
        # plot the information
        # considering 4 branches by page 
        tt <- data.frame(talagrand.diag[-1,])
        names(tt) <- names(talagrand.diag)
        row.names(tt) <- row.names(talagrand.diag)[-1]
        
        
        df <- melt(tt, measure.vars=names(tt)) 
        df <- na.omit(df)
        rm(tt)
        df$termination <- substr(as.character(df$variable),
                                 nchar(as.character(df$variable))-4,
                                 nchar(as.character(df$variable)))
        df$branch <- substr(as.character(df$variable),1,
                            nchar(as.character(df$variable))-6)
        df$bin <- 1:(nr.ensembles+1)
        
        df.aux <- talagrand.diag[1,talagrand.diag[1,]!=0]
        names. <- names(talagrand.diag)[talagrand.diag[1,]!=0]
        df1 <- data.frame(n=as.numeric(df.aux))
        df1$branch <- substr(names.,1,nchar(names.)-6)
        rm(talagrand.diag,df.aux,names.)
        # for each plot 4 branches are considered
        total.plots = trunc(length(unique(df$branch))/4) + ceiling((length(unique(df$branch))%%4)/4)
        
        cutoff <- data.frame( x = c(-Inf, Inf), y = 1/(nr.ensembles+1), cutoff = factor(nr.ensembles) )
        
        pdf(sprintf('%s/univariate/UnivariateRankHistograms_%s.pdf',results.path,case))
        for (im.plot in 1:total.plots){
            vars.to.consider <- na.omit(sort(unique(df$branch))[(4*(im.plot-1)+1):(4*(im.plot))])
            df.selected <- df[df$branch%in%vars.to.consider,] 
            df.selected <- na.omit(df.selected)
            df.selected2 <- df1[df1$branch%in%vars.to.consider,]
            p <- ggplot(df.selected,aes(x=bin,y=value)) + geom_bar(stat = "identity",alpha=.5,colour="blue2")+
                geom_line(aes( x, y, linetype = cutoff ,color='red'), cutoff)+
                facet_wrap(~branch,ncol=1,scales = 'free_y')+
                theme_bw() +
                ggtitle('Electric Current') +
                theme(legend.position="none")+
                xlab('Rank')+ylab('Relative Frequency')+
                ggtitle(TeX(sprintf('Electric Current (observed %s) Rank Histograms (%s of %s)',power.id,im.plot,total.plots)))+
                scale_colour_identity()+
                theme(strip.background =element_rect(fill="azure2"),
                      panel.border = element_rect(colour = "black"))+
                geom_text(data=df.selected2,
                           size=4,aes(x=3, y=1/51+0.05, 
                           label=sprintf('Number of observations: %d',n),
                           family='serif'),
                           vjust = "inward", hjust = "inward")+
                theme(text=element_text(family="serif",size=14),
                      axis.text=element_text(colour='black'))+
                scale_y_continuous(labels=scaleFUN)
            print(p)
            rm(df.selected,vars.to.consider,df.selected2,vars.to.consider)
        }
        rm(case,total.plots,power.id)
        dev.off()
   }
    rm(df)
}
time.urh <- proc.time()
cat('|-> Total time:', (time.urh-time.read.data)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Rank histogram',(time.urh-time.read.data)[3]))
rm(time.read.data)
#-----------------------------
# 2.2 Delta Index
#-----------------------------

cat('Computing delta index...\n')

# POSITIVE case
if (positive.data.indicator){
    delta.index.I.pos <- delta.index(talagrand.diag.I.pos,nr.ensembles)
}

# NEGATIVE case
if (negative.data.indicator){
    delta.index.I.neg <- delta.index(talagrand.diag.I.neg,nr.ensembles)
}

time.delta <- proc.time()
cat('|-> Total time:', (time.delta-time.urh)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Delta index',(time.delta-time.urh)[3]))
rm(time.urh)

#-----------------------------
# 2.3 p-values hypothesis tests
#-----------------------------

cat('Computing p-value associated with chi squared test...\n')

# POSITIVE case
if (positive.data.indicator){
    p.values.I.pos <- round(p.value.rank.hist(talagrand.diag.I.pos,nr.ensembles),3)
}

# NEGATIVE case
if (negative.data.indicator){
    p.values.I.neg <- round(p.value.rank.hist(talagrand.diag.I.neg,nr.ensembles),3)
}


time.p.value <- proc.time()
cat('|-> Total time:', (time.p.value-time.delta)[3],'seconds \n')
computation.times <- rbind(computation.times,c('p-values rank histogram',(time.p.value-time.delta)[3]))
rm(talagrand.diag.I.pos,talagrand.diag.I.neg,time.delta)


#-----------------------------
# 2.4 a) EUCLIDEAN DISTANCE (without remove quantiles)
# and boxplots
#-----------------------------

cat('Computing Euclidean distances between SN and the ensemble...\n')

for (case in c('pos','neg')){
    if (case=='pos'){
        indicator <- positive.data.indicator
        if (indicator) {data. <- B.separated$data.I.pos
                        data.remove.out <- remove_outliers(B.separated$data.I.pos,p=p.quantiles)}
    } else{
        indicator <- negative.data.indicator
        if (indicator) {data. <- B.separated$data.I.neg
                        data.remove.out <- remove_outliers(B.separated$data.I.neg,p=p.quantiles)}
    }
    if (indicator){
        EE <- Euclidean.distance.SN_to_MCLA(data.)
        EE.removing.out <- Euclidean.distance.SN_to_MCLA(data.remove.out)
        distance.I <- EE$distance.by.deviation
        row.names(distance.I) <- sprintf('%s_%s',row.names(distance.I),case)
        count.I <- EE$count.position
        row.names(count.I) <- sprintf('%s_%s',row.names(count.I),case)
        names(count.I) <- c('Nr Observations: SN in ensemble',
                            'Nr Observations: SN<ensemble', 
                            'Nr Observations: SN>ensemble')
        timestamps <- row.names(EE$euclidean.distance)
        
        # boxplots
        # for each variable 4 boxplots:
        # 1.timestamp with < d(SN<->ensemble)
        # 2.timestamp with > d(SN<->ensemble)
        # 3.timestamp with > dunder
        # 4.timestamp with > d(SN<->ens)
        
        boxplot.fun <- function(i){
            # more close
            if (sum(EE$euclidean.distance[,i],na.rm=T)>0){
                timestamps <- row.names(EE$euclidean.distance)
                # timestamp with < d(SN<->ensemble)
                ps1 <- which(EE$euclidean.distance[,i]==min(EE$euclidean.distance[,i],na.rm=T))[1]
                ddf <- data.frame(a=as.numeric(as.vector((data.[[i]][ps1,]))),b=sprintf('<d(SN<->ens.) (%s)', timestamps[ps1]))
                # timestamp with > dunder
                under.SN <- which(EE$position.indicator[,i]==1)
                ps2 <- under.SN[which(EE.removing.out$euclidean.distance[under.SN,i]==max(EE.removing.out$euclidean.distance[under.SN,i],na.rm=T))[1]]
                ddf1 <- data.frame(a=as.numeric(as.vector((data.[[i]][ps2,]))),b=sprintf('>d under. (%s)',timestamps[ps2]))
                ddf <- rbind(ddf,ddf1)
                
                # timestamp with > d(SN<->ens)
                ps3 <- which(EE$euclidean.distance[,i]==max(EE$euclidean.distance[,i],na.rm=T))[1]
                ddf2 <- data.frame(a=as.numeric(as.vector((data.[[i]][ps3,]))),b=sprintf('>d(SN<->ens.) (%s)',timestamps[ps3]))
                ddf <- rbind(ddf,ddf2)
                
                # chosen timestamp 
                ps4 <- which(row.names(EE.removing.out$position.indicator)==boxplot.timestamp)
                if (length(ps4)>0){
                    ddf1 <- data.frame(a=as.numeric(as.vector((data.[[i]][ps4,]))),b=sprintf('chosen t (%s)',timestamps[ps4]))
                } else{
                    ddf1 <- data.frame(a=as.numeric(rep(NA,ncol(data.[[i]]))),b=sprintf('chosen t (%s)',boxplot.timestamp))
                }
                ddf <- rbind(ddf,ddf1)
                ddf <- as.data.frame(ddf)
                names(ddf) <- c('values','type')
                
                ddf$variable <- names(EE.removing.out$euclidean.distance)[i]
                boxplots.I <- data.frame(ddf)
            }
        }
        
        if (do.parallel==TRUE){
            cl <- makeCluster(cl.to.use)
            registerDoParallel(cl)
            boxplots.I <- foreach(i=1:ncol(EE$euclidean.distance),.inorder=FALSE) %dopar% boxplot.fun(i)
            stopCluster(cl)
            boxplots.I <- do.call("rbind",boxplots.I)
        } else{
            boxplots.I <- c()
            for (i in 1:ncol(EE$euclidean.distance)){
                ddf <- boxplot.fun(i)
                boxplots.I <- rbind(boxplots.I,ddf)
            }
        }
        
        # define color and size of points
        col <- c('red',rep('black',nr.ensembles-1),'green')
        boxplots.I$col=as.character(col)
        boxplots.I$siz[boxplots.I$col=='red'] <- 2
        boxplots.I$siz[boxplots.I$col=='black'] <- 0.3
        boxplots.I$siz[boxplots.I$col=='green'] <- 2
        
        boxplots.I$dates.t <- substr(as.character(boxplots.I$type),
                                     nchar(as.character(boxplots.I$type))-20,
                                     nchar(as.character(boxplots.I$type)))
        boxplots.I$type <- substr(as.character(boxplots.I$type),1,
                                  nchar(as.character(boxplots.I$type))-22)
        
        boxplots.I<- boxplots.I[order(boxplots.I$col),]
        
        pdf(sprintf('%s/univariate/Boxplot_%s.pdf',results.path,case))
        cutoff <- data.frame( x = c(-Inf, Inf), y =0, cutoff = factor(nr.ensembles) )
        for (im.plot in 1:length(unique(boxplots.I$variable))){
            vars.to.consider <- na.omit(sort(as.character(unique(boxplots.I$variable)))[im.plot])
            df.selected <- boxplots.I[boxplots.I$variable%in%vars.to.consider,]
            df.unique <- unique(df.selected[,c("type","variable","dates.t")])
            p <-ggplot(df.selected) +geom_boxplot(aes(x=type,y=values)) +
                geom_point(aes(x=type,y=values,colour=as.character(col),size=siz)) + 
                scale_color_manual("Legend", values=c(red="red",green="green",black="black"),
                                   labels=c('ensemble member','SN','DACF'))+ 
                geom_text(data=df.unique,aes(x=type,y=-0.009,label=dates.t),size=2)+
                guides(size=FALSE) + scale_size_area(max_size = 2)+
                facet_wrap(~variable,ncol=1,scale='free_y')+
                xlab('Date (YYYY-mm-dd HH:MM:00)')+ylab('Electric Current [per unit]')+
                theme_bw() + theme(strip.background =element_rect(fill="azure2"),
                                   panel.border = element_rect(colour = "black"),
                                   text=element_text(family="serif",size=16),
                                   axis.text.x=element_text(family="serif",size=10))+
                scale_y_continuous(labels=scaleFUN)
            print(p)
            rm(df.selected,df.unique,vars.to.consider)
        }
        dev.off()
        
        rm(boxplots.I)
        
        if (case=='pos'){
            distance.I.pos <- distance.I
            count.I.pos <- count.I
        } else{
            distance.I.neg <- distance.I
            count.I.neg <- count.I
        }
        
        rm(EE,ddf,ddf1,ddf2,ps1,ps2,ps3,ps4,distance.I,count.I,case,data.)
    }
}


#-----------------------------
# 2.4 b) EUCLIDEAN DISTANCE (removing quantiles)
#-----------------------------

for (case in c('pos','neg')){
    if (case=='pos'){
        indicator <- positive.data.indicator
        if (indicator) {
            EE <- Euclidean.distance.SN_to_MCLA(remove_outliers(B.separated$data.I.pos,p=p.quantiles))}
    } else{
        indicator <- negative.data.indicator
        if (indicator) {
            EE <- Euclidean.distance.SN_to_MCLA(remove_outliers(B.separated$data.I.neg,p=p.quantiles))}
    }
    if (indicator){
        distance.I.removing.q <- EE$distance.by.deviation # distances by type of SN position
        row.names(distance.I.removing.q) <- sprintf('%s_%s',row.names(distance.I.removing.q),case)
        distance.I.removing.q$variable <- as.factor(row.names(distance.I.removing.q))
        
        count.I.removing.q <- EE$count.position # number of situations SN<Q(p), SN>Q(1-p), SN in [Q(p),Q(1-p)]
        row.names(count.I.removing.q) <- sprintf('%s_%s',row.names(count.I.removing.q),case)
        names(count.I.removing.q) <- c(sprintf('Frequency(SN in [Q(%s),Q(%s)])',p.quantiles,1-p.quantiles),
                                sprintf('Frequency(SN<Q(%s))',p.quantiles),
                                sprintf('Frequency(SN>Q(%s))',1-p.quantiles))
        count.I.removing.q$n <- rowSums(count.I.removing.q[,names(count.I.removing.q)],na.rm=T)
        count.I.removing.q[c(sprintf('Frequency(SN in [Q(%s),Q(%s)])',p.quantiles,1-p.quantiles),
                                sprintf('Frequency(SN<Q(%s))',p.quantiles),
                                sprintf('Frequency(SN>Q(%s))',1-p.quantiles))] <- 
                                count.I.removing.q[c(sprintf('Frequency(SN in [Q(%s),Q(%s)])',p.quantiles,1-p.quantiles),
                                sprintf('Frequency(SN<Q(%s))', p.quantiles),
                                sprintf('Frequency(SN>Q(%s))',1-p.quantiles))]/count.I.removing.q$n
        count.I.removing.q[count.I.removing.q$n==0,]=0
        
        d.under.I <- EE$euclidean.distance
        d.under.I[EE$position.indicator!=1] <- NA
        names(d.under.I) <- sprintf('%s_%s',names(d.under.I),case)
        
        position.indicator.I <- EE$position.indicator
        position.indicator.I$termination <- sprintf('I_%s',case)
        position.indicator.I[position.indicator.I==-2] = NA
        
        if (case=='pos'){
            distance.I.pos.removing.q <- distance.I.removing.q
            count.I.pos.removing.q <- count.I.removing.q
            d.under.I.pos <- d.under.I
            position.indicator.I.pos <- position.indicator.I
        } else{
            distance.I.neg.removing.q <- distance.I.removing.q
            count.I.neg.removing.q <- count.I.removing.q
            d.under.I.neg <- d.under.I
            position.indicator.I.neg <- position.indicator.I
        }
        
        rm(distance.I.removing.q,count.I.removing.q,d.under.I,position.indicator.I,EE)
    }
}



time.EE <- proc.time()
cat('|-> Total time:', (time.EE-time.p.value)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Euclidean distances and boxplots',(time.EE-time.p.value)[3]))

rm(time.p.value)
#-----------------------------
# 2.5 spread measure
#-----------------------------
cat('Computing spread measure...\n')

# POSITIVE case
if (positive.data.indicator){
    rmse.sharp.I.pos <- sharpness.root.mean(B.separated$data.I.pos,p=p.quantiles)
    row.names(rmse.sharp.I.pos) <- sprintf('%s_pos',row.names(rmse.sharp.I.pos))
    names(rmse.sharp.I.pos) <- c('Std ens.','d(Q(1-p);Q(p))')
} 

# NEGATIVE case
if (negative.data.indicator){
    rmse.sharp.I.neg <- sharpness.root.mean(B.separated$data.I.neg,p=p.quantiles)
    row.names(rmse.sharp.I.neg) <- sprintf('%s_neg',row.names(rmse.sharp.I.neg))
    names(rmse.sharp.I.neg) <- c('Std ens.','d(Q(1-p);Q(p))')
} 

time.rmse.sharp <- proc.time()
cat('|-> Total time:', (time.rmse.sharp-time.EE)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Spread measures',(time.rmse.sharp-time.EE)[3]))

rm(time.EE)
#-----------------------------
# 2.6 a) CRPS
#-----------------------------

cat('Computing CRPS...\n')

# POSITIVE case
if (positive.data.indicator){
    crps.pos <- CRPS(B.separated$data.I.pos)
    CRPS.I.pos <- crps.pos$crps.mean
    row.names(CRPS.I.pos) <- sprintf('%s_pos',row.names(CRPS.I.pos))
    names(CRPS.I.pos) <- 'crps'
}

# NEGATIVE case
if (negative.data.indicator){
    crps.neg <- CRPS(B.separated$data.I.neg)
    CRPS.I.neg <- crps.neg$crps.mean
    row.names(CRPS.I.neg) <- sprintf('%s_neg',row.names(CRPS.I.neg))
    names(CRPS.I.neg) <- 'crps'
}

# Time Evolution of CRPS
if (time.evolution.plots==TRUE){
    # POSITIVE case
    if (positive.data.indicator){
        CRPS.ts.I.pos <- crps.pos$crps.time.evolution
        CRPS.ts.I.pos$termination <- 'I_pos'
        CRPS.ts.I.pos$timestamps <- timestamps
        CRPS.ts.I.pos <- melt(CRPS.ts.I.pos,id.vars = c('termination',
                                                        'timestamps','metric'))
        rm(crps.pos)
    }
    
    # NEGATIVE case
    if (negative.data.indicator){
        CRPS.ts.I.neg <- crps.neg$crps.time.evolution
        CRPS.ts.I.neg$termination <- 'I_neg'
        CRPS.ts.I.neg$timestamps <- timestamps
        CRPS.ts.I.neg <- melt(CRPS.ts.I.neg,id.vars = c('termination',
                                                        'timestamps','metric'))
        rm(crps.neg)
    }
}

time. <- proc.time()
cat('|-> Total time:', (time.-time.rmse.sharp)[3],'seconds \n')
computation.times <- rbind(computation.times,c('CRPS',(time.-time.rmse.sharp)[3]))

rm(time.rmse.sharp)

#-----------------------------
# 2.6 b) Spread plot
#-----------------------------

if (spread.plot){
  cat('Computing spread plot...\n')
  
  # POSITIVE case
  if (positive.data.indicator){
      if (do.parallel==TRUE) {
          aux <- sharp.computation(B.separated$data.I.pos,names(B.separated$data.I.pos),parallel = TRUE,cl)
      } else { aux <- sharp.computation(B.separated$data.I.pos,names(B.separated$data.I.pos))}
      
      sharp.I.pos <- aux$sharpness
      sharp.I.pos <- melt(sharp.I.pos,id.vars = 'coverages')
      
      sharp.I.pos <- na.omit(sharp.I.pos)
      total.plots = trunc(length(unique(sharp.I.pos$variable))/4) + ceiling((length(unique(sharp.I.pos$variable))%%4)/4)
      
      pdf(sprintf('%s/univariate/Spread Plot_pos.pdf',results.path))
      for (im.plot in 1:total.plots){
          vars.to.consider <- na.omit(unique(sort(as.character(unique(sharp.I.pos$variable))))[(4*(im.plot-1)+1):(4*(im.plot))])
          cutoff <- data.frame( x = c(-Inf, Inf), y =0, cutoff = factor(nr.ensembles) )
          df.to.use <- sharp.I.pos[sharp.I.pos$variable%in%vars.to.consider,]
          df.to.use$variable <- as.factor(df.to.use$variable)
          p <-ggplot(df.to.use) + geom_line(aes(x=coverages,y = value))+
              facet_wrap(~variable,ncol=1)+
              theme_bw() +
              ggtitle('Electric Current') +
              theme(legend.position="none")+
              xlab(TeX('$\\beta$'))+ylab(TeX('Mean distance between $Q_{\\beta/2} and Q_{1-\\beta/2}$'))+
              ggtitle(TeX(sprintf('Electric Current (observed $P_{ik} \\geq 0$) Spread plot (%s of %s)',im.plot,total.plots)))+
              scale_colour_identity()+
              theme(strip.background =element_rect(fill="azure2"),
                    panel.border = element_rect(colour = "black"))+
              theme(text=element_text(family="serif",size=14),axis.text=element_text(colour='black'))#+
          print(p)
      }
      dev.off()
      
      rm(aux,sharp.I.pos,df.to.use)
  }
  
  
  
  
  # NEGATIVE case
  if (negative.data.indicator){
      if (do.parallel==TRUE) {
        aux <- sharp.computation(B.separated$data.I.neg,names(B.separated$data.I.neg),parallel = TRUE,cl)
    } else {aux <- sharp.computation(B.separated$data.I.neg,names(B.separated$data.I.neg))}
      sharp.I.neg <- aux$sharpness
      sharp.I.neg <- melt(sharp.I.neg,id.vars = 'coverages')
      
      sharp.I.neg <- na.omit(sharp.I.neg)
      total.plots = trunc(length(unique(sharp.I.neg$variable))/4) + ceiling((length(unique(sharp.I.neg$variable))%%4)/4)
      
      pdf(sprintf('%s/univariate/Spread Plot_neg.pdf',results.path))
      for (im.plot in 1:total.plots){
          vars.to.consider <- na.omit(unique(sort(as.character(unique(sharp.I.neg$variable))))[(4*(im.plot-1)+1):(4*(im.plot))])
          cutoff <- data.frame( x = c(-Inf, Inf), y =0, cutoff = factor(nr.ensembles) )
          df.to.use <- sharp.I.neg[sharp.I.neg$variable%in%vars.to.consider,]
          df.to.use$variable <- as.factor(df.to.use$variable)
          p <-ggplot(df.to.use) + geom_line(aes(x=coverages,y = value))+
              facet_wrap(~variable,ncol=1)+
              theme_bw() +
              ggtitle('Electric Current') +
              theme(legend.position="none")+
              xlab(TeX('$\\beta$'))+ylab(TeX('Mean distance between $Q_{\\beta/2} and Q_{1-\\beta/2}$'))+
              ggtitle(TeX(sprintf('Electric Current (observed $P_{ik} < 0$) Spread plot (%s of %s)',im.plot,total.plots)))+
              scale_colour_identity()+
              theme(strip.background =element_rect(fill="azure2"),
                    panel.border = element_rect(colour = "black"))+
              theme(text=element_text(family="serif",size=14),axis.text=element_text(colour='black'))#+
          print(p)
      }
      dev.off()
      rm(aux,sharp.I.neg,df.to.use)
  }
  
  time.spread <- proc.time()
  cat('|-> Total time:', (time.spread-time.)[3],'seconds \n')
  computation.times <- rbind(computation.times,c('Spread plots', (time.spread-time.)[3]))
  
  time. <- time.spread
}
#-----------------------------
# 2.6 c) QQ plot
#-----------------------------

if(qq.plot){
    cat('Computing QQ plot...\n')
    
    # POSITIVE case
    if (positive.data.indicator){
        qq.I.pos <- qq.computation(B.separated$data.I.pos)
        names(qq.I.pos)[-ncol(qq.I.pos)] <- sprintf('%s_pos',names(qq.I.pos)[-ncol(qq.I.pos)])
        qq.I.pos <- melt(qq.I.pos,id.vars = 'x')
        qq.I.pos$terminations <- substr(as.character(qq.I.pos$variable),nchar(as.character(qq.I.pos$variable))-4,nchar(as.character(qq.I.pos$variable)))
        qq.I.pos$variable <- substr(as.character(qq.I.pos$variable),1,nchar(as.character(qq.I.pos$variable))-6)
        qq.I.pos <- na.omit(qq.I.pos)
        # for each page we will plot 4 variables
        total.plots = trunc(length(unique(qq.I.pos$variable))/4) + ceiling((length(unique(qq.I.pos$variable))%%4)/4)
        
        pdf(sprintf('%s/univariate/QQ-Plot_pos.pdf',results.path))
        for (im.plot in 1:total.plots){
            vars.to.consider <- na.omit(sort(as.character(unique(qq.I.pos$variable)))[(4*(im.plot-1)+1):(4*(im.plot))])
            cutoff <- data.frame( x = c(-Inf, Inf), y =0, cutoff = factor(nr.ensembles) )
            df.selected <- qq.I.pos[qq.I.pos$variable%in%vars.to.consider,]
            p <-ggplot(df.selected) + geom_point(aes(x=x,y = x+value))+
                facet_wrap(~variable,ncol=1)+
                theme_bw() +
                geom_abline(slope=1, intercept=0, color='red')+
                ggtitle('Electric Current') +
                theme(legend.position="none")+
                xlab(TeX('Theorical Quantiles N(0,1)'))+ylab(TeX('Mean normalized ensemble quantiles'))+
                ggtitle(TeX(sprintf('Electric Current (observed $P_{ik} \\geq 0$) Q-Q plot (%s of %s)',im.plot,total.plots)))+
                scale_colour_identity()+
                theme(strip.background =element_rect(fill="azure2"),
                      panel.border = element_rect(colour = "black"))+
                theme(text=element_text(family="serif",size=14),axis.text=element_text(colour='black'))
            print(p)
        }
        dev.off()
    }
    
    rm(qq.I.pos,df.selected,vars.to.consider,total.plots)
    
    # NEGATIVE case
    if (negative.data.indicator){
        qq.I.neg <- qq.computation(B.separated$data.I.neg)
        names(qq.I.neg)[-ncol(qq.I.neg)] <- sprintf('%s_neg',names(qq.I.neg)[-ncol(qq.I.neg)])
        qq.I.neg <- melt(qq.I.neg,id.vars = 'x')
        qq.I.neg$terminations <- substr(as.character(qq.I.neg$variable),nchar(as.character(qq.I.neg$variable))-4,nchar(as.character(qq.I.neg$variable)))
        qq.I.neg$variable <- substr(as.character(qq.I.neg$variable),1,nchar(as.character(qq.I.neg$variable))-6)
        qq.I.neg <- na.omit(qq.I.neg)
        # for each page we will plot 4 variables
        total.plots = trunc(length(unique(qq.I.neg$variable))/4) + ceiling((length(unique(qq.I.neg$variable))%%4)/4)
        
        pdf(sprintf('%s/univariate/QQ-Plot_neg.pdf',results.path))
        for (im.plot in 1:total.plots){
            vars.to.consider <- na.omit(sort(as.character(unique(qq.I.neg$variable)))[(4*(im.plot-1)+1):(4*(im.plot))])
            cutoff <- data.frame( x = c(-Inf, Inf), y =0, cutoff = factor(nr.ensembles) )
            df.selected <- qq.I.neg[qq.I.neg$variable%in%vars.to.consider,]
            p <-ggplot(df.selected) + geom_point(aes(x=x,y = x+value))+
                facet_wrap(~variable,ncol=1)+
                theme_bw() +
                geom_abline(slope=1, intercept=0, color='red')+
                ggtitle('Electric Current') +
                theme(legend.position="none")+
                xlab(TeX('Theorical Quantiles N(0,1)'))+ylab(TeX('Mean normalized ensemble quantiles'))+
                ggtitle(TeX(sprintf('Electric Current (observed $P_{ik} < 0$) Q-Q plot (%s of %s)',im.plot,total.plots)))+
                scale_colour_identity()+
                theme(strip.background =element_rect(fill="azure2"),
                      panel.border = element_rect(colour = "black"))+
                theme(text=element_text(family="serif",size=14),axis.text=element_text(colour='black'))
            print(p)
        }
        dev.off()
    }
    
    time.qq <- proc.time()
    cat('|-> Total time:', (time.qq-time.)[3],'seconds \n')
    computation.times <- rbind(computation.times,c('QQ plots', (time.qq-time.)[3]))
    time. <- time.qq
    rm(qq.I.neg,df.selected,vars.to.consider,total.plots)
}
#-----------------------------
# save the measures to .csv
#-----------------------------

cat('Saving measures...\n')

for (case in c('pos','neg')){
  if (case=='pos'){
    indicator <- positive.data.indicator
    if (indicator){
      p.values.I <- p.values.I.pos 
      delta.index.I <- delta.index.I.pos
      count.I.removing.q <- count.I.pos.removing.q
      rmse.sharp.I <- rmse.sharp.I.pos
      CRPS.I <- CRPS.I.pos
      distance.I <- distance.I.pos
      distance.I.removing.q <- distance.I.pos.removing.q
      rm(distance.I.pos,distance.I.pos.removing.q)
    }
  } else{
    indicator <- negative.data.indicator
    if (indicator){
      p.values.I <- p.values.I.neg
      delta.index.I <- delta.index.I.neg
      count.I.removing.q <- count.I.neg.removing.q
      rmse.sharp.I <- rmse.sharp.I.neg
      CRPS.I <- CRPS.I.neg
      distance.I <- distance.I.neg
      distance.I.removing.q <- distance.I.neg.removing.q
      rm(distance.I.neg,distance.I.neg.removing.q)
      }
    }
  if (indicator){
      p.values.I$variable <- row.names(p.values.I)
      delta.index.I$variable <- row.names(delta.index.I)
      count.I.removing.q$variable <-row.names(count.I.removing.q)
      rmse.sharp.I$variable <- row.names(rmse.sharp.I)
      CRPS.I$variable <- row.names(CRPS.I)
      df1 <- subset(distance.I, select="d(SN<->ensemble)")
      df1$variable <- row.names(df1)
      df2 <- subset(distance.I.removing.q, select=c("d(SN<Q(p))","d(SN>Q(1-p))",
                                                        "d over","Mean SN","Std SN"))
      df2$variable <- row.names(df2)
      all.metrics = Reduce(function(x, y) merge(x, y,by='variable'), 
                           list(p.values.I,delta.index.I,count.I.removing.q,
                                df1,df2,rmse.sharp.I,CRPS.I))
      
      all.metrics$out <- rowSums(all.metrics[,c(sprintf('Frequency(SN<Q(%s))',p.quantiles),
                                                        sprintf('Frequency(SN>Q(%s))',1-p.quantiles))],na.rm=T)/
          rowSums(all.metrics[,c(sprintf('Frequency(SN in [Q(%s),Q(%s)])',p.quantiles,1-p.quantiles),
                                     sprintf('Frequency(SN<Q(%s))',p.quantiles),
                                     sprintf('Frequency(SN>Q(%s))',1-p.quantiles))],na.rm=T)
      all.metrics$SNoutQ <- all.metrics[,sprintf('Frequency(SN>Q(%s))',1-p.quantiles)]/
          rowSums(all.metrics[,c(sprintf('Frequency(SN in [Q(%s),Q(%s)])',p.quantiles,1-p.quantiles),
                                     sprintf('Frequency(SN<Q(%s))',p.quantiles),
                                     sprintf('Frequency(SN>Q(%s))',1-p.quantiles))],na.rm=T)
      
      all.metrics <- all.metrics[!is.na(all.metrics$crps),]
      
      all.metrics$'rank d(Q(1-p);Q(p)) vs outside' <- NA
      x <- t(all.metrics[,c('out','d(Q(1-p);Q(p))')])
      rank.trade1. <- rep(NA,ncol(x))
      pos <- which(!is.na(x[1,]))
      if (length(pos)>0) rank.trade1.[pos] <- mv.rank.quality(as.matrix(x[,pos]))
      all.metrics$'rank d(Q(1-p);Q(p)) vs outside' <- rank.trade1.
      rm(rank.trade1.)
      
      all.metrics$'rank d(Q(1-p);Q_(p)) vs relative # SN>Q(1-p)' <- NA
      x <- t(all.metrics[,c('SNoutQ','d(Q(1-p);Q(p))')])
      rank.trade4. <- rep(NA,ncol(x))
      pos <- which(!is.na(x[1,]))
      if (length(pos)>0) rank.trade4.[pos] <- mv.rank.quality(as.matrix(x[,pos]))
      all.metrics$'rank d(Q(1-p);Q_(p)) vs relative # SN>Q(1-p)' <- rank.trade4.
      rm(rank.trade4.)
      
      
      all.metrics$'rank Std ens. vs d(SN<->ensemble)' <- NA
      x <- t(all.metrics[,c('Std ens.',"d(SN<->ensemble)")])
      rank.trade2. <- rep(NA,ncol(x))
      pos <- which(!is.na(x[1,]))
      if (length(pos)>0) rank.trade2.[pos] <- mv.rank.quality(as.matrix(x[,pos]))
      all.metrics$'rank Std ens. vs d(SN<->ensemble)' <- rank.trade2.
      rm(rank.trade2.)
      
      all.metrics$'rank Std ens. vs d under.' <- NA
      x <- t(all.metrics[,c('Std ens.',"d(SN>Q(1-p))")])
      rank.trade3. <- rep(NA,ncol(x))
      pos <- which(!is.na(x[1,]))
      if (length(pos)>0) rank.trade3.[pos] <- mv.rank.quality(as.matrix(x[,pos]))
      all.metrics$'rank Std ens. vs d under.' <- rank.trade3.
      rm(rank.trade3.)
      
      all.metrics.to.save <- all.metrics
      names(all.metrics.to.save)[names(all.metrics.to.save)=='p.values.cvm'] <- 'p.values.Watson'
      names(all.metrics.to.save)[names(all.metrics.to.save)=='delta.ind'] <- 'delta.index'
      names(all.metrics.to.save)[names(all.metrics.to.save)=='d(SN<Q(p))'] <- sprintf('d(SN<Q(%s))',p.quantiles)
      names(all.metrics.to.save)[names(all.metrics.to.save)=='d(SN>Q(1-p))'] <- sprintf('d(SN>Q(%s))',1-p.quantiles)
      names(all.metrics.to.save)[names(all.metrics.to.save)=='d(Q(1-p);Q(p))'] <- sprintf('d(Q(%s);Q(%s))',p.quantiles,1-p.quantiles)
      names(all.metrics.to.save)[names(all.metrics.to.save)=='d over'] <- 'd overestimate'
      names(all.metrics.to.save)[names(all.metrics.to.save)=='out'] <- sprintf('Frequency(SN not in [(Q(%s);Q(%s)])',p.quantiles,1-p.quantiles)
      names(all.metrics.to.save)[names(all.metrics.to.save)=='rank d(Q(1-p);Q(p)) vs outside'] <- sprintf('rank d(Q(%s);Q(%s)) vs outside',p.quantiles,1-p.quantiles)
      names(all.metrics.to.save)[names(all.metrics.to.save)=='rank d(Q(1-p);Q_(p)) vs relative # SN>Q(1-p)'] <- sprintf('rank d(Q(%s);Q_(%s)) vs relative # SN>Q(%s)',p.quantiles,1-p.quantiles,1-p.quantiles)
      all.metrics.to.save <- all.metrics.to.save[,names(all.metrics.to.save)[names(all.metrics.to.save)!='SNoutQ']]
      
      write.table(all.metrics.to.save,file=sprintf('%s/univariate/general_results_%s.csv',results.path,case),sep=',',dec='.',row.names=F)
      
      rm(p.values.I, delta.index.I,distance.I.removing.q,count.I.removing.q,df1,df2,
         rmse.sharp.I,CRPS.I,all.metrics.to.save)
      
      if (case=='pos'){
          rm(p.values.I.pos, delta.index.I.pos,
             rmse.sharp.I.pos,CRPS.I.pos)
      } else{
          rm(p.values.I.neg, delta.index.I.neg,
             rmse.sharp.I.neg,CRPS.I.neg)
      }
      
      if (case=='pos'){
          all.metrics.pos <- all.metrics
      } else{
          all.metrics.neg <- all.metrics
      }
      rm(all.metrics,distance.I)
  }
}

time.save <- proc.time()
cat('|-> Total time:', (time.save-time.)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Save measures', (time.save-time.)[3]))


#-----------------------------
# 2.7 Trade-offs
#-----------------------------

cat('Saving Trade-off plots...\n')


for (case in c('pos','neg')){
    if (case=='pos'){
        indicator <- positive.data.indicator
        if (indicator){
          all.metrics <- all.metrics.pos
          power.id <- '$P_{ik} \\geq 0$'}
    } else{
        indicator <- negative.data.indicator
        if (indicator){
          all.metrics <- all.metrics.neg
          power.id <- '$P_{ik} < 0$'
          }
    }
    if (indicator){  
        pdf(sprintf(sprintf('%s/univariate/TradeOffs_%s.pdf',results.path,case)))
        df <- all.metrics
        variable.names <- substr(as.character(df$variable),1,nchar(as.character(df$variable))-6)
        df$variable.names <- as.factor(variable.names)
        max.values <- apply(df[,c("Std ens.","d(SN<->ensemble)","d(SN>Q(1-p))","d(Q(1-p);Q(p))","out","SNoutQ")],2,max,na.rm=T)
        df <- df[,c("Std ens.","d(SN<->ensemble)","variable.names" )]
        names(df) <- c('x1','y1','factor2')
        breaks.inc.x <- ifelse(round(1.25*max.values['Std ens.']/10,5)>0,
                               round(1.25*max.values['Std ens.']/10,5),
                               1.25*max.values['Std ens.'])
        breaks.x <- seq(0,1.25*max.values['Std ens.'],breaks.inc.x)
        breaks.inc.y <- ifelse(round(max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))'])/10,5)>0,
                               round(max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))'])/10,5),
                               max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))']))
        breaks.y <- seq(0,max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))']),breaks.inc.y)
        
        p <- ggplot(df,aes(x=x1,y=y1))+ geom_point(size=1,colour='blue') +
            geom_text(aes(label=factor2),hjust=0, vjust=0,size=2)+
            scale_x_continuous(limit=c(0,1.25*max.values['Std ens.']),
                               breaks=breaks.x)+ 
            scale_y_continuous(limit=c(0,max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))'])),
                               breaks=breaks.y)+
            xlab('Mean ensemble standard deviation')+ylab(TeX('$\\bar{d}(SN <-> ensemble)$'))+
            theme_bw() +
            theme(text=element_text(family="serif",size=14),
                  axis.text.x = element_text(family="serif",size=12),
                  strip.background =element_rect(fill="azure2"),
                  panel.border = element_rect(colour = "black"))+
            ggtitle(TeX(sprintf('Electric Current (observed %s)',power.id)))
        print(p)
        
        df <- all.metrics
        variable.names <- substr(as.character(df$variable),1,nchar(as.character(df$variable))-6)
        df$variable.names <- as.factor(variable.names)
        df <- df[,c("Std ens.","d(SN>Q(1-p))","variable.names" )]
        names(df) <- c('x1','y1','factor2')
        p <- ggplot(df,aes(x=x1,y=y1))+
            geom_point(size=1,colour='blue') +geom_text(aes(label=factor2),hjust=0, vjust=0,size=2)+
            scale_x_continuous(limit=c(0,1.25*max.values['Std ens.']),
                               breaks=breaks.x)+ 
            scale_y_continuous(limit=c(0,max(max.values['d(SN<->ensemble)'],max.values['d(SN>Q(1-p))'])),
                               breaks=breaks.y)+
            xlab('Mean ensemble standard deviation')+ylab(TeX(sprintf('$\\bar{d}(SN > Q(%s))$',1-p.quantiles)))+
            theme_bw() +
            theme(text=element_text(family="serif",size=14),
                  axis.text.x = element_text(family="serif",size=12),
                  strip.background =element_rect(fill="azure2"),
                  panel.border = element_rect(colour = "black"))+
            ggtitle(TeX(sprintf('Electric Current (observed %s)',power.id)))
        
        rm(breaks.inc.x,breaks.x,breaks.inc.y,breaks.y)
        print(p)
        
        breaks.inc.x <- ifelse(round(1.25*max.values['d(Q(1-p);Q(p))']/10,5)>0,
                               round(1.25*max.values['d(Q(1-p);Q(p))']/10,5),
                               1.25*max.values['d(Q(1-p);Q(p))'])
        breaks.x <- seq(0,1.25*max.values['d(Q(1-p);Q(p))'],breaks.inc.x)
        
        breaks.inc.y <- ifelse(round(max(max.values['out'],max.values['SNoutQ'])/10,5)>0,
                               round(max(max.values['out'],max.values['SNoutQ'])/10,5),
                               max(max.values['out'],max.values['SNoutQ']))
        breaks.y <- seq(0,max(max.values['out'],max.values['SNoutQ']),breaks.inc.y)
        
        df <- all.metrics
        variable.names <- substr(as.character(df$variable),1,nchar(as.character(df$variable))-6)
        df$variable.names <- as.factor(variable.names)
        df <- df[,c("variable","variable.names","d(Q(1-p);Q(p))","out")]
        names(df) <- c("variable","variable.names",'x1',"y1")
        
         
        p<-ggplot(df,aes(x1,y1)) + geom_point(size=1,colour='blue') + 
            geom_text(aes(label=variable.names),hjust=0, vjust=0,size=2)+
            xlab(TeX(sprintf("$\\bar{d}_{Q(%s) <-> Q(%s)}$",p.quantiles,1-p.quantiles)))+
            ylab(TeX(sprintf('Relative frequency of SN outside $\\[Q(%s),Q(%s)\\]$',p.quantiles,1-p.quantiles)))+
            theme_bw() + 
            scale_x_continuous(limit=c(0,1.25*max.values['d(Q(1-p);Q(p))']),
                               breaks=breaks.x)+ 
            scale_y_continuous(limit=c(0,max(max.values['out'],max.values['SNoutQ'])),
                               breaks=breaks.y)+
            theme(text=element_text(family="serif",size=14),
                  axis.text.x = element_text(family="serif",size=12),
                  strip.background =element_rect(fill="azure2"),
                  panel.border = element_rect(colour = "black"))+
            ggtitle(TeX(sprintf('Electric Current (observed %s)',power.id)))
        print(p)
        
        
        df <- all.metrics
        variable.names <- substr(as.character(df$variable),1,nchar(as.character(df$variable))-6)
        df$variable.names <- as.factor(variable.names)
        
        df <- df[,c("variable","variable.names","d(Q(1-p);Q(p))","SNoutQ")]
        names(df) <- c("variable","variable.names",'x1',"y1")
        
        
        p <- ggplot(df,aes(x1,y1)) + geom_point(size=1,colour='blue') + 
            geom_text(aes(label=variable.names),hjust=0, vjust=0,size=2)+
            xlab(TeX(sprintf("$\\bar{d}_{Q(%s) <-> Q(%s)}$",p.quantiles,1-p.quantiles)))+ylab(TeX(sprintf('Relative frequency of SN > $Q(%s)$',1-p.quantiles)))+
            theme_bw() + 
            scale_x_continuous(limit=c(0,1.25*max.values['d(Q(1-p);Q(p))']),
                               breaks=breaks.x)+ 
            scale_y_continuous(limit=c(0,max(max.values['out'],max.values['SNoutQ'])),
                               breaks=breaks.y)+
            theme(text=element_text(family="serif",size=14),
                  axis.text.x = element_text(family="serif",size=12),
                  strip.background =element_rect(fill="azure2"),
                  panel.border = element_rect(colour = "black"))+
            ggtitle(TeX(sprintf('Electric Current (observed %s)',power.id)))
        print(p)
        
        rm(breaks.inc.x,breaks.x,breaks.inc.y,breaks.y)
        
        dev.off()

    }
}

time.to <- proc.time()
cat('|-> Total time:', (time.to-time.save)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Trade-off plots', (time.to-time.save)[3]))

time. <- time.to
rm(df,all.metrics,all.metrics.pos,all.metrics.neg)

#-----------------------------
# 2.8 Sensitivity Analysis of value of p
#-----------------------------

if(sensitivity.plot){
    cat('Computing Sensitivity Analysis...\n')
    for (case in c('pos','neg')){
        if (case=='pos'){
            indicator <- positive.data.indicator
            power.id <- 'Pik positive'
            if (indicator) data. <- B.separated$data.I.pos
        } else{
            indicator <- negative.data.indicator
            if (indicator) data. <- B.separated$data.I.neg
            power.id <- 'Pik negative'
        }
        if (indicator){  
            main_df_under <- c()
            p.s <-  c(0,0.01,seq(0.025,0.1,0.025),seq(0.15,0.5,0.05))
            
            #if (positive.data.indicator){
            #    df.tradeoff <- c()
            
            trade.off.fun <- function(p0){
                cat(round(which(p.s==p0)/length(p.s),2)*100,' % for ', case,'case \r')
                EE <- Euclidean.distance.SN_to_MCLA(remove_outliers(data.,p=p0)) # compute distances
                freq.under.I <- apply(EE$position.indicator,2,function(x) length(which(x==1))/length(which(x!=-2))) 
                distance.out.I <- apply(EE$position.indicator,2,function(x) length(which(x==1|x==-1))/length(which(x!=-2)))
                interquantile <- sharpness.root.mean(data.,p = p0)[,'interquantile.amplitude']
                df_aux <- data.frame(variable=names(EE$euclidean.distance),
                                     p=sprintf('p=%s',p0),
                                     IQ=interquantile,
                                     freq.under=freq.under.I,
                                     freq.out=distance.out.I)
                data.frame(df_aux)
                #rm(EE,freq.under.I,interquantile,df_aux,df_aux1,df_aux2)
                #rm(EE)
            }
            
            if (do.parallel==TRUE){
                cl <- makeCluster(cl.to.use)
                registerDoParallel(cl)
                
                #for (p0 in p.s){
                df.tradeoff <- foreach (p0 = p.s,.packages=c('reshape2'),.inorder=FALSE) %dopar% trade.off.fun(p0)
                
                stopCluster(cl)
                
                df.tradeoff <- do.call("rbind", df.tradeoff) 
            } else {
                df.tradeoff <- c()
                for (p0 in p.s){
                    df_aux <- trade.off.fun(p0)
                    df.tradeoff <- rbind(df.tradeoff,df_aux)
                }
            }
            
            
            # Plot and save in pdf
            df.tradeoff <- na.omit(df.tradeoff)
            names(df.tradeoff)[names(df.tradeoff)=='freq.under'] = 'SN>Q(1-p)'
            names(df.tradeoff)[names(df.tradeoff)=='freq.out'] = 'SN not in [Q(p);Q(1-p)]'
            df.tradeoff <- melt(df.tradeoff,id.vars=c("variable","p" ,"IQ"))
            names(df.tradeoff)=c("variable","p","IQ","variable1","value")
            
            
            df.tradeoff.to.save <- df.tradeoff
            df.tradeoff.to.save <- df.tradeoff.to.save[,c("variable","variable1","p","IQ","value")]
            names(df.tradeoff.to.save) <- c("variable","metric","p","d(Q(p);Q(1-p))","metric.value")
            df.tradeoff.to.save$p <- as.numeric(substr(as.character(df.tradeoff.to.save$p),3,nchar(as.character(df.tradeoff.to.save$p))))
            write.table(df.tradeoff.to.save,file=sprintf('%s/univariate/sensitivity_analysis_%s.csv',results.path,case),sep=',',dec='.',row.names=F)
            rm(df.tradeoff.to.save)
            
            total.plots = length(unique(df.tradeoff$variable))
            
            pdf(sprintf('%s/univariate/SensitivityAnalysis_%s.pdf',results.path,case))
            for (im.plot in 1:total.plots){
                vars.to.consider <- na.omit(sort(unique(df.tradeoff$variable))[im.plot])
                cutoff <- data.frame( x = c(-Inf, Inf), y = 1/nr.ensembles, cutoff = factor(nr.ensembles) )
                df.selected <- df.tradeoff[df.tradeoff$variable%in%vars.to.consider,]
                p <-ggplot(df.selected,aes(x=IQ,y=value))+ 
                    geom_point(size=0.6)+
                    facet_wrap(~variable1,ncol=2)+ 
                    xlab(TeX('$\\bar{d}_{Q_{1-p} <-> Q_p}$'))+ ylab(TeX('Relative Frequency'))+
                    geom_text(aes(label=p),hjust=-0.1, vjust=-0.1,size=2.5)+
                    theme_bw() + ggtitle(vars.to.consider) +
                    theme(legend.position="none")+
                    ggtitle(sprintf('%s (observed %s)',vars.to.consider,power.id))+
                    scale_colour_identity()+
                    theme(strip.background =element_rect(fill="azure2"),
                          panel.border = element_rect(colour = "black"))+
                    scale_y_continuous(limit=c(0,1),breaks = seq(0, 1, by = 0.05))+
                    scale_x_continuous(limit=c(0,1.1*max(df.selected$IQ)),
                                       breaks=round(seq(0,1.1*max(df.selected$IQ),
                                                        1.1*max(df.selected$IQ)/6),3))+
                    theme(text=element_text(family="serif",size=12),
                          axis.text=element_text(colour='black'))#+
                print(p)
            }
            dev.off() 
        #}
        rm(df.tradeoff)
        }
        rm(data.)
    }
    
    time.sensitivity <- proc.time()
    cat('|-> Total time:', (time.sensitivity-time.)[3],'seconds \n')
    computation.times <- rbind(computation.times,c('Sensitivity analysis', (time.sensitivity-time.)[3]))
    time. <- time.sensitivity
}

#-----------------------------
# 2.9 Time Evolution
#-----------------------------

if (time.evolution.plots){
    cat('Saving Time Evolution Plots...\n')
    scaleFUN <- function(x) sprintf("%.3f", x)
    colfunc <- colorRampPalette(c("slategray1", "slateblue3"))
    cols.fill <-  colfunc(4)
    
    override.linetype <- c(1, 1)
    for (case in c('pos','neg')){
        if (case=='pos'){
            indicator <- positive.data.indicator
            if (indicator){
                # fan chart
                dd <- c()
                for (var in 1:length(B.separated$data.I.pos)){
                    dd <- rbind(dd,cbind(B.separated$data.I.pos[[var]],
                                         vars=names(B.separated$data.I.pos)[var]))
                }
                position.chart <- position.indicator.I.pos
                df <- d.under.I.pos
                df.crps <- CRPS.ts.I.pos
                rm(position.indicator.I.pos,d.under.I.pos,CRPS.ts.I.pos)
                
            }
            power.id <- 'Pik positive'
        } else{
            indicator <- negative.data.indicator
            if (indicator){
                # fan chart
                dd <- c()
                for (var in 1:length(B.separated$data.I.neg)){
                    dd <- rbind(dd,cbind(B.separated$data.I.neg[[var]],
                                         vars=names(B.separated$data.I.neg)[var]))
                }
                position.chart <- position.indicator.I.neg
                df <- d.under.I.neg
                df.crps <- CRPS.ts.I.neg
                rm(position.indicator.I.neg,d.under.I.neg,CRPS.ts.I.neg)
                power.id <- 'Pik negative'
            }
        }
        if (indicator){  
            # fan chart
            dd1.pos <- data.frame(timestamps = as.POSIXct(row.names(dd), 
                                                          format = "%Y-%m-%d %H:%M:00",
                                                          tz="Europe/Paris"),
                                  t(apply(dd[,1:nr.ensembles],1,quantile,
                                          probs = c(fan.p.values,rev(1-fan.p.values)),na.rm=T)))
            dd1.pos$DACF = dd$DACF
            dd1.pos$SN = dd$SN
            dd1.pos$variable <- as.factor(substr(as.character(dd$vars),1,nchar(as.character(dd$vars))-2))
            fan.chart.data.withNAN <- dd1.pos
            rm(dd)
            # position indicator
            position.chart$timestamps = as.POSIXct(row.names(position.chart), 
                                                   format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
            position.chart <- melt(position.chart,id.vars = c('termination','timestamps'))
            position.chart$Frequency <- 1
            position.chart$value[position.chart$value==-1] <- sprintf('SN<Q(%s)',p.quantiles)
            position.chart$value[position.chart$value==1] <- sprintf('SN>Q(%s)',1-p.quantiles)
            position.chart$value[position.chart$value==0] <- sprintf('SN in [Q(%s),Q(%s)]',p.quantiles,1-p.quantiles)
            position.chart$value <- as.factor(position.chart$value)
            position.chart$variable <-  as.factor(substr(as.character(position.chart$variable),1,nchar(as.character(position.chart$variable))-2))
            position.chart$timestamps <- as.POSIXct(position.chart$timestamps, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
            positionchart.data.withNAN <- position.chart
            
            # underestimate distance
            df$timestamps <- as.POSIXct(row.names(df), format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
            df1 <- na.omit(melt(df,id.vars='timestamps'))
            df1$variable <-  as.factor(substr(as.character(df1$variable),1,
                                              nchar(as.character(df1$variable))-4))
            dunder.data.withNAN <- df1
            
            # CRPS
            df.crps$timestamps <- as.POSIXct(df.crps$timestamps, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
            df.crps <- df.crps[order(df.crps$timestamps),]
            df.crps$variable <-  as.factor(substr(as.character(df.crps$variable),1,nchar(as.character(df.crps$variable))-2))
            crps.data.withNAN <- df.crps
            
            names(dunder.data.withNAN) <- c("timestamps","variable","dunderestimate")
            dunder.data.withNAN$variable <- substr(as.character(dunder.data.withNAN$variable),1,
                                                   nchar(as.character(dunder.data.withNAN$variable))-2)
            names(positionchart.data.withNAN)<- c( "termination","timestamps","variable","value","Frequency")
            names(crps.data.withNAN) <- c("termination","timestamps","metric","variable","value.metric")
            
            rm(dd1.pos,position.chart,df,df1)
            
            df <- Reduce(function(x, y) merge(x, y, by=c("timestamps","variable"),all.x=T), 
                         list(fan.chart.data.withNAN,dunder.data.withNAN,positionchart.data.withNAN,crps.data.withNAN))
            df <- df[!is.na(df$SN),]
            df$Week <- sprintf('Week %s of %s',strftime(df$timestamps,format="%W"),strftime(df$timestamps,format="%Y")) 
            
            rm(fan.chart.data.withNAN,dunder.data.withNAN,positionchart.data.withNAN,crps.data.withNAN,df.crps)
            
            a <-diff(df$timestamps)
            units(a) <- 'hours'
            idx <- c(1, a)
            i2 <- c(1,which(idx >2), nrow(df)+1)
            df$grp.distance <- rep(1:length(diff(i2)), diff(i2))
    
            df.selected <- df
            rm(df)
            names(df.selected) <- c("timestamps","variable","X2.5.","X5.","X10.",
                                    "X15.","X85.","X90.","X95.","X97.5.","DACF",
                                    "SN","dunderestimate","termination.x","value",
                                    "Frequency","termination.y","metric","value.metric","Week","grp.distance" )
            
            plot.uni.vars <- function(var){
                #for (var in sort(as.character(unique(df.selected$variable)))){
                fchart <- df.selected[df.selected$variable==var,]
                plots <- list()
                i = 1
                #cat(sprintf('%s %% of %stive case \r',round((i/length(unique(df.selected$variable)))*100,2),case))
                plotss <- list()
                for (Week in sort(unique(fchart$Week))){
                    fchart <- df.selected[df.selected$variable==var,]
                    fchart <- fchart[fchart$Week==Week,]
                    
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
                        scale_fill_manual(name="[Q(p);Q(1-p)]", values=values.1)+
                        scale_y_continuous(labels=scaleFUN)
                    
                    values.2= as.character(data.frame("orange","red","green"))
                    values.2[1:6]=c("orange","red","green")
                    names(values.2)=c(sprintf("SN<Q(%s)",p.quantiles),
                                      sprintf("SN>Q(%s)",1-p.quantiles),
                                      sprintf("SN in [Q(%s),Q(%s)]",p.quantiles,1-p.quantiles))
                    
                    p2 <-  ggplot(fchart, aes(x = timestamps, y = Frequency, fill = value)) +  
                        geom_bar(stat = "identity") +
                        theme_bw() +
                        scale_x_datetime(date_breaks = t.steps,
                                         limits =lims.date)+
                        theme(axis.text.x = element_text(angle = 90, hjust = 1),
                              axis.text=element_text(colour='black'),
                              axis.text.y=element_text(colour="white"),
                              text=element_text(size=10))+
                        ylab('SN position')+
                        scale_fill_manual(name="", values=values.2)+
                        scale_y_continuous(labels=scaleFUN)
                    
                    if (sum(fchart$dunderestimate,na.rm=T)==0){# if there not exist 
                        # underestimated situations then let's craeate an empty plot
                        p3 <- ggplot(fchart,aes(x=timestamps,y=Frequency))+ 
                            theme_bw() +
                            scale_x_datetime(date_breaks = t.steps,
                                             limits =lims.date)+
                            theme(axis.text=element_text(colour='black'),
                                  axis.title.x = element_blank(), 
                                  axis.text.x = element_blank(),
                                  text=element_text(size=10))+
                            xlab('Timestamp (YYYY-mm-dd HH:MM)')+
                            ylab('d(under.) [p.u.]')+
                            scale_y_continuous(labels=scaleFUN)
                    } else{
                        p3 <- ggplot(fchart,aes(x=timestamps,y=dunderestimate))+ 
                            geom_point(size=0.6)+ 
                            geom_line(size=0.3)+ 
                            theme_bw() +
                            scale_x_datetime(date_breaks = t.steps,
                                             limits =lims.date)+
                            theme(axis.text=element_text(colour='black'),
                                  axis.title.x = element_blank(), 
                                  axis.text.x = element_blank(),
                                  text=element_text(size=10))+
                            xlab('Timestamp (YYYY-mm-dd HH:MM)')+
                            ylab('d(under.) [p.u.]')+
                            scale_y_continuous(labels=scaleFUN)
                    }
                    
                    p4 <- ggplot(fchart,aes(x=timestamps,y=value.metric,
                                            group = interaction(grp.distance, metric),
                                            colour = metric))+ geom_point(size=0.6)+ 
                        geom_line(size=0.3)+ theme_bw() +
                        scale_x_datetime(date_breaks = t.steps,
                                         limits =lims.date)+
                        theme(axis.text=element_text(colour='black'),
                              axis.title.x = element_blank(), 
                              axis.text.x = element_blank(),
                              text=element_text(size=10))+
                        scale_color_discrete(name = "",
                                             breaks=c('CRPS','d(SN<->ens.)','d(ens.i<->ens.j)'),
                                             labels=list(TeX('ES'),TeX('$d_{SN <-> ens.}$'),TeX('$d_{ens._i <-> ens._j}$')))+
                        xlab('Timestamp (YYYY-mm-dd HH:MM)')+
                        ylab('Value [p.u.]')+
                        scale_y_continuous(labels=scaleFUN)
                    
                    
                    plotss[[i]] <- VAlignPlots(p1,p4,p3,p2,
                                               title=sprintf('%s: %s (observed %s)',
                                                             Week,var,power.id))
                    rm(p1,p2,p3,p4)
                    i=i+1
                }
                
                rm(fchart)
                pdf(sprintf('%s/univariate/TimeEvolution/ %s_%s.pdf',results.path,var,case),paper='a4')
                
                for (i in 1:length(plotss)){
                    grid.arrange(plotss[[i]])
                }
                
                dev.off()
            }
            
            if (do.parallel==TRUE){
                # START parallel computation
                cl <- makeCluster(cl.to.use)
                registerDoParallel(cl)
                
                foreach (var = sort(as.character(unique(df.selected$variable))),
                         .packages=c('lubridate','ggplot2','reshape2','gridExtra','latex2exp'),
                         .inorder=FALSE) %dopar% plot.uni.vars(var)
            stopCluster(cl)
            } else{
                for (var in sort(as.character(unique(df.selected$variable)))){
                    plot.uni.vars(var)
                }
            }
        }
        rm(df.selected)
    }
    time.time.evolution <- proc.time()
    cat('|-> Total time:', (time.time.evolution-time.)[3],'seconds \n')
    computation.times <- rbind(computation.times,c('Time evolution plots',(time.time.evolution-time.)[3]))
    time. <- time.time.evolution
}


#-----------------------------
# 2.10 Barplots
#-----------------------------

cat('Saving barplots...\n')

for (case in c('pos','neg')){
    if (case=='pos'){
        indicator <- positive.data.indicator
        if (indicator) {count. <- count.I.pos
        count.removing.q <- count.I.pos.removing.q
        power.id <- '$P_{ik} \\geq 0$'
        rm(count.I.pos,count.I.pos.removing.q)}
    } else{
        indicator <- negative.data.indicator
        if (indicator) {count. <- count.I.neg
        count.removing.q <- count.I.neg.removing.q
        power.id <- '$P_{ik} < 0$'
        rm(count.I.neg,count.I.neg.removing.q)}
    }
        if (indicator){  
          count. <- na.omit(count.)
          count.removing.q <- na.omit(count.removing.q)
          count. <- data.frame(as.matrix(t(count.[,c(2,1,3)])))
          count.removing.q <- data.frame(t(as.matrix(count.removing.q[,c(2,1,3)]*colSums(count.))))
          
          count. <- data.matrix(subset(count.,select=names(count.)[colSums(count.)>=minimum.timestamp]))
          count.removing.q <- data.matrix(subset(count.removing.q,select=names(count.removing.q)[colSums(count.removing.q)>=minimum.timestamp]))
          total.plots = trunc(ncol(count.)/4) + ceiling((ncol(count.)%%4)/4)
          
          x.max = max(max(count.)+0.6*max(count.),
                      max(count.removing.q)+0.6*max(count.removing.q))
          
          pdf(sprintf('%s/univariate/Absolute and relative frequencies_%s.pdf',results.path,case))
          for (im.plot in 1:total.plots){
            par(mfrow=c(2,1),mar=c(5,18,5,8),family='serif',cex=0.7)
            info.to.plot <- subset(count.,select=colnames(count.)[order(colnames(count.), decreasing=FALSE)][(4*(im.plot-1)+1):min((4*(im.plot)),ncol(count.))])
            
            barplots.number.inoutside(info.to.plot,'All ensemble',x.max=x.max)
            
            mtext(sprintf('Absolute and relative frequencies (%s of %s)',im.plot,total.plots), side=3,line=3)
            
            info.to.plot <- subset(count.removing.q,select=colnames(count.removing.q)
                                   [order(colnames(count.removing.q), 
                                          decreasing=FALSE)][(4*(im.plot-1)+1):min((4*(im.plot)),
                                                                                   ncol(count.removing.q))])
            
            barplots.number.inoutside(info.to.plot,sprintf('Removing the %s percent most extreme values (p=%s)',2*p.quantiles*100,p.quantiles),type.a='a',x.max=x.max)
            
          }
          
          dev.off()
          
          rm(info.to.plot,count.,count.removing.q)
        }
}

time.boxplots <- proc.time()
cat('|-> Total time:', (time.boxplots-time.)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Barplots',(time.boxplots-time.)[3]))



################################################################################
# 3. MULTIVARIATE ANALYSIS
################################################################################
#-----------------------------
# 3.1 MULTIVARIATE RANK HISTOGRAM
#-----------------------------

cat('Computing Multivariate Rank Histogram...\n')
multi.rank.hist <- mrh.rhist(B.separated$data.I,nr.ensembles)

write.table(multi.rank.hist,
            file=sprintf('%s/multivariate/MultivariateRankHistogram.csv',
                         results.path,case),sep=',',dec='.',col.names = NA)
# plot the information
# considering 4 branches by page 
df <- data.frame(value=multi.rank.hist[-1,])
row.names(df) <- row.names(multi.rank.hist)[-1]

df$bin <- 1:(nr.ensembles+1)

pdf(sprintf('%s/multivariate/MultivariateRankHistogram.pdf',results.path))
    cutoff <- data.frame( x = c(-Inf, Inf), y = 1/(nr.ensembles+1), cutoff = factor(nr.ensembles) )
    p <- ggplot(df,aes(x=bin,y=value)) + geom_bar(stat = "identity",alpha=.5,colour="blue2")+
        geom_line(aes( x, y, linetype = cutoff ,color='red'), cutoff)+
        theme_bw() +
        ggtitle('Electric Current Multivariate Rank Histogram') +
        theme(legend.position="none")+
        xlab('Rank')+ylab('Relative Frequency')+
        scale_colour_identity()+
        theme(strip.background =element_rect(fill="azure2"),
              panel.border = element_rect(colour = "black"))+
        theme(text=element_text(family="serif",size=14),
              axis.text=element_text(colour='black'))+
        scale_y_continuous(labels=scaleFUN)
    print(p)
dev.off()

time.multirank <- proc.time()
cat('|-> Total time:', (time.multirank-time.boxplots)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Multivariate Rank Histogram',(time.multirank-time.boxplots)[3]))


#-----------------------------
# 3.2 Delta Index
#-----------------------------

cat('Computing delta index...\n')

delta.index <- delta.index(multi.rank.hist,nr.ensembles)

time.deltamultirank <- proc.time()
cat('|-> Total time:', (time.deltamultirank-time.multirank)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Multivariate delta index',(time.deltamultirank-time.multirank)[3]))

#-----------------------------
# 3.3 p-values hypothesis tests
#-----------------------------
cat('Computing p-value associated with chi squared test...\n')

p.values <- round(p.value.rank.hist(multi.rank.hist,nr.ensembles),3)
rm(multi.rank.hist)

time.pmultirank <- proc.time()
cat('|-> Total time:', (time.pmultirank-time.deltamultirank)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Multivariate p-value',(time.pmultirank-time.deltamultirank)[3]))


#-----------------------------
# 3.4 Energy score
#-----------------------------

cat('Computing energy score...\n')

ES.I <- energy.score(B.separated$data.I,timestamps)
ES <- ES.I$ES.mean

ES.timeseries <- ES.I$ES.by.timestamp
names(ES.timeseries) <- c('timestamps','ES','E1','E2')
rm(ES.I)

time.esmultirank <- proc.time()
cat('|-> Total time:', (time.esmultirank-time.pmultirank)[3],'seconds \n')
computation.times <- rbind(computation.times,c('Energy Score',(time.pmultirank-time.deltamultirank)[3]))

df.measures <- data.frame(delta.index=delta.index,p.value.Watson=p.values$p.values.cvm,ES)
names(df.measures) <- c("delta.index","p.value.Watson","ES","d(SN<->ens.)","d(ens.i<->ens.j)")
write.table(df.measures,file=sprintf('%s/multivariate/general_results.csv',results.path),sep=',',dec='.',col.names = NA)
rm(df.measures,p.values,ES,delta.index)
#-----------------------------
# 3.5 Time Evolution
#-----------------------------

# ES timeseries
cat('Saving Time Evolution for the multivariate case...\n')

ES.timeseries$timestamps <- as.POSIXct(ES.timeseries$timestamps, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
ES.timeseries$Week <- sprintf('Week %s of %s',strftime(ES.timeseries$timestamps,format="%W"),strftime(ES.timeseries$timestamps,format="%Y")) 
ES.timeseries <- ES.timeseries[order(ES.timeseries$timestamps),]

ES.timeseries <- melt(ES.timeseries,id.vars=c('timestamps','Week'))

# underestimated distances timeseries
EE <- Euclidean.distance.SN_to_MCLA(remove_outliers(B.separated$data.I,p=p.quantiles))
d.under.I <- EE$euclidean.distance
d.under.I[EE$position.indicator!=1] <- NA
names(d.under.I) <- sprintf('%s_%s',names(d.under.I),case)
d.under.I$timestamps <- row.names(d.under.I)
dd <- data.frame(d.under.I[,-ncol(d.under.I)])
names(dd) <- names(d.under.I)[-ncol(d.under.I)]
d.under.I$mean <- rowMeans(dd,na.rm = T)
rm(dd)
d.under.I <- melt(d.under.I,id.var=c('timestamps','mean'))
d.under.I$timestamps <- as.POSIXct(d.under.I$timestamps, format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
d.under.I$Week <- sprintf('Week %s of %s',strftime(d.under.I$timestamps,format="%W"),strftime(d.under.I$timestamps,format="%Y")) 

# Nr out information
position.indicator <- EE$position.indicator
position.indicator$termination <- sprintf('I_%s',case)
position.indicator[position.indicator==-2] = NA

rm(EE)

position.indicator1 <- subset(position.indicator,select=names(position.indicator)[names(position.indicator)!='termination'])
total.t <- rowSums(!is.na(position.indicator1))
position.indicator$"SN>Q(1-p)" <- rowSums(position.indicator1==1,na.rm=T)
position.indicator$"SN<Q(p)" <- rowSums(position.indicator1==-1,na.rm=T)
rm(position.indicator1)
position.indicator$"SN in [Q(p);Q(1-p)]" = total.t - (position.indicator$"SN>Q(1-p)"+position.indicator$"SN<Q(p)")

position.indicator$timestamps <- as.POSIXct(row.names(position.indicator), format = "%Y-%m-%d %H:%M:00",tz="Europe/Paris")
position.indicator$Week <- sprintf('Week %s of %s',strftime(position.indicator$timestamps,format="%W"),
                                   strftime(position.indicator$timestamps,format="%Y")) 

position.indicator <- position.indicator[,c("SN>Q(1-p)","SN<Q(p)","SN in [Q(p);Q(1-p)]","timestamps","Week")]
names(position.indicator) <- c(sprintf("SN>Q(%s)",1-p.quantiles),sprintf("SN<Q(%s)",p.quantiles),sprintf("SN in [Q(%s),Q(%s)]",
                                                                                                         p.quantiles,
                                                                                                         1-p.quantiles),"timestamps","Week")
position.indicator <- melt(position.indicator,id.vars=c('timestamps','Week'))

position.indicator$variable <- as.factor(position.indicator$variable)

names(position.indicator) <- c("timestamps","Week","position","freq.position")
names(ES.timeseries) <- c("timestamps","Week","energyscore","energyscore.value")
names(d.under.I) <- c("timestamps","mean","underestimated","underestimated.value","Week")
all.metrics = Reduce(function(x, y) merge(x, y,by=c('timestamps','Week')), 
                     list(ES.timeseries,d.under.I,position.indicator))

rm(ES.timeseries,d.under.I,position.indicator)
a <-diff(all.metrics$timestamps)
units(a) <- 'hours'
idx <- c(1, a)
i2 <- c(1,which(idx >2), nrow(all.metrics)+1)
all.metrics$grp.data <- rep(1:length(diff(i2)), diff(i2))
t.steps='6 hours'

plotss <- list()
i=1
for (Week in sort(unique(all.metrics$Week))){
    df.selected2 <- all.metrics[all.metrics$Week==Week,]
    
    lims.date = c(floor_date(df.selected2$timestamps[1], unit="week",week_start = getOption("lubridate.week.start", 1)),
                  ceiling_date(df.selected2$timestamps[1], unit="week",week_start = getOption("lubridate.week.start", 1)))[1:2]
    
    p1 <- ggplot(df.selected2,aes(x=timestamps,y=energyscore.value,colour=energyscore,
                                  group = interaction(grp.data, energyscore)))+ 
        geom_point(size=0.4)+ geom_line(size=0.4)+ 
        facet_wrap(~Week,ncol=1)+ xlab('Timestamps')+ ylab('Value [per unit]')+
        theme_bw()+
        scale_x_datetime(date_breaks = '6 hours',
                         limits =lims.date)+
        theme(axis.title.x = element_blank(), 
              axis.text.x = element_blank())+
        scale_color_manual(name = "Energy Score",
                          values=c("#F8766D","#619CFF","#00BA38"),
                         breaks=c("ES", "E1", "E2"),
                         labels=list(TeX('ES'),TeX('$d_{SN <-> ens.}$'),TeX('$d_{ens._i <-> ens._j}$'))) +
    theme(strip.background =element_rect(fill="azure2"),
              panel.border = element_rect(colour = "black"))+
        scale_y_continuous(labels=scaleFUN) 
    
    p2 <- ggplot(df.selected2)+ geom_point(aes(x=timestamps,y=underestimated.value,colour='Specific Branch'),size=0.4)+ 
        geom_point(aes(x=timestamps,y=mean,colour='Mean'),size=0.4)+
        ylab('d(under.) [p.u.]')+
        theme_bw()+
        scale_x_datetime(date_breaks = t.steps,
                         limits =lims.date)+
        theme(
            axis.title.x = element_blank(), 
            axis.text.x = element_blank())+ 
        scale_y_continuous(labels=scaleFUN)+
        scale_colour_manual(name='', values=c('Specific Branch'='grey60',
                                              'Mean'='black'))
    
    values.= as.character(data.frame("orange","red","green"))
    values.[1:3]=c("orange","red","green")
    names(values.)=c(sprintf("SN<Q(%s)",p.quantiles), 
                     sprintf("SN>Q(%s)",1-p.quantiles),
                     sprintf("SN in [Q(%s),Q(%s)]",p.quantiles,1-p.quantiles))
    
    df.selected2 <- unique(df.selected2[,c('timestamps','freq.position','position')])
    p3 <- ggplot(df.selected2, aes(x = timestamps, y = freq.position,
                                  fill = position))+ 
        geom_bar(stat = "identity", position = "stack")+
        scale_x_datetime(date_breaks = t.steps,
                         limits =lims.date)+
        #facet_wrap(~Week)+
        scale_fill_manual(name="Legend", values=values.)+
        xlab('Timestamps')+ ylab('Number of branches')+
        theme_bw()+ 
        theme(axis.text.x = element_text(angle = 90, hjust = 1))+
        scale_y_continuous(labels=scaleFUN,
                           breaks=seq(0,length(B.separated$data.I),ceiling(0.25*length(B.separated$data.I)))) +
        theme(strip.background =element_rect(fill="azure2"),
              panel.border = element_rect(colour = "black"))
    plotss[[i]] <- VAlignPlots(p1,p2,p3,title='Multivariate Time Evolution',hts=c(0.6,0.4,0.8))
    rm(p1,p2,p3)
    rm(p,df.selected2)
    i=i+1
}

rm(all.metrics)


for (i in 1:length(plotss)){ # save in png first to reduce size
    png(sprintf('%s/multivariate/TimeEvolution_%s.png',results.path,i),width = 780, height = 880)
    grid.arrange(plotss[[i]])
    dev.off()
}


merge.png.pdf <- function(pdfFile, pngFiles, deletePngFiles=FALSE) {
    # convert all png into a unique pdf
    #### Package Install ####
    pngPackageExists <- require ("png")
    
    if ( !pngPackageExists ) {
        install.packages ("png")
        library ("png")
        
    }
    #########################
    
    pdf(pdfFile)
    n <- length(pngFiles)
    for( i in 1:n) {
        pngFile <- pngFiles[i]
        pngRaster <- readPNG(pngFile)
        grid.raster(pngRaster, width=unit(0.7, "npc"), height= unit(0.72, "npc"))
        if (i < n) plot.new()
    }
    dev.off()
    if (deletePngFiles) {
        unlink(pngFiles)
    }
    
}

merge.png.pdf(sprintf('%s/multivariate/TimeEvolution.pdf',results.path),
              sprintf('%s/multivariate/TimeEvolution_%s.png',results.path,1:length(plotss)))

file.remove(sprintf('%s/multivariate/TimeEvolution_%s.png',results.path,1:length(plotss)))

if (length(B.separated$data.I)<=50){
    pdf(sprintf('%s/multivariate/TimeEvolution_highresolution.pdf',results.path),paper='a4')
    for (i in 1:length(plotss)){ # high resolution pdf 
        grid.arrange(plotss[[i]])
    }
    dev.off()
}

rm(B.separated,plotss)

time.temultirank <- proc.time()
cat('|-> Total time:', (time.temultirank-time.esmultirank)[3],'seconds \n')

cat('###########################\n Done in a total of',round((time.temultirank-initial.time)[3]/60,2),' mins \n###########################')

computation.times <- rbind(computation.times,c('Multivariate time evolution plots',(time.temultirank-time.esmultirank)[3]))
computation.times <- rbind(computation.times,c('Total time',round((time.temultirank-initial.time)[3],2)))

computation.times <- data.frame(computation.times)
names(computation.times) <- c('Task','Time(s)')

write.table(computation.times,file=sprintf('%s/computation_times.csv',results.path),sep=',',dec='.',col.names = NA)

}



