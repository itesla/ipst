function [] = Disegna(vettore,ppath,allplot)
% Stand Alone function
% First window: original data  with outliers inside, 
%   - plot of error, snapshot and forecast,
%   - scatterplot of snapshot and forecast.
%   - histogram of the error. 
% Second window:
%   - plot of error without outliers,
%   - scatterplot of snapshot and forecast without outliers,
%   - histogram of the error without outliers.
%
% INPUT:
% vettore = double array with the position of variables to be plotted
% ppath = path where to find the filea Variables.mat and outlier_info.mat
% allplot = 0 if you want both windows,
%           1 if you want only the first,
%           2 if you want only the second.
%

f=fullfile(ppath,'Variables.mat');
load(f,'err_all','ID_in','fo','sn','errore_all','errore')
f=fullfile(ppath,'outlier_info.mat');
load(f,'outlier')


for i=1:length(vettore)
    vari=vettore(i);
    name=ID_in{i};
    kind=name(end);
    if strcmp(kind,'P')==1
        umis='MW';
    else
        umis='Mvar';
    end
    
    if (allplot==0 || allplot==1)
        figure
        subplot(2,2,1)
        hist(err_all{vari},50);
        title(vari)
        xlabel(umis)
        
        subplot(2,2,2)
        scatter(fo(:,vari),sn(:,vari));
        title(umis)
        xlabel('forecast')
        ylabel('snapshot')
        
        subplot(2,2,[3,4])
        plot(errore_all(:,vari))
        hold on
        plot(fo(:,vari))
        plot(sn(:,vari))
        legend('err','fo','sn')
        title(ID_in(vari))
        ylabel(umis)
        
        % individuo i quantili e li piazzo sul grafico
    end
    
    if (allplot==0 || allplot==2)
                outquant=outlier(vari).quantile;
        oq=unique(outquant);
        outcheb=outlier(vari).chebychev;
        oc=unique(outcheb);
        pos_oq=[];
        pos_oc=[];
        for k=1:length(oq)
            findoq=find(err_all{vari}==oq(k));
            pos_oq=[pos_oq;findoq];
        end
        for k=1:length(oc)
            findoc=find(err_all{vari}==oc(k));
            pos_oc=[pos_oc;findoc];
        end

        figure
        %scatterplot senza outliers
        fo_o=fo(:,vari);
        sn_o=sn(:,vari);
        sn_o(pos_oc)=[];
        fo_o(pos_oc)=[];
        
        subplot(2,2,2)
        scatter(fo_o,sn_o)
        title(umis)
        xlabel('forecast')
        ylabel('snapshot')
        
        subplot(2,2,1)
        hist(errore(:,vari),50);
        title('without outliers')
        xlabel(umis)
        
        subplot(2,2,[3,4])
        plot(err_all{vari})
        hold on
        plot(pos_oq,outquant,'o','MarkerFaceColor','c')
        plot(pos_oc,outcheb,'o','MarkerFaceColor','r')
        title(ID_in(vari))
        ylabel(umis)
    end
end