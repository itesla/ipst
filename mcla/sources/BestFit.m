%
% Copyright (c) 2017, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [info,BestDist,comp]=BestFit(variab,compon,bestgm,fig)
% This function compares some distributions that fit the data and chooses
% the best one.
% First, it tests some Gaussian mixtures on the data
% then, it looks for the best unimodal distribution that fit the data
% at the end, it compares the negative Loglikelihood and select the
% minimun.
%
% INPUT 
% variab = column to be fitted
% compon = max number of components of gaussian mixture
% bestgm = 'on' if you want to find the best Gaussian mixture with number of
% components lower or equal to "compon", otherwise the code calculates only
% one gaussian mixture with "compon" components
% fig = 'on' if the code have to plot the figure
%
% OUTPUT
% info = absolute value of the differnce of the BIC indeces of the two best
% distributions selected
% BestDist = best distribution selected
% comp = number of components of the best fit

% PHASE 1 : choosing the best number of components in Gaussian Mixture
if strcmp(bestgm,'on')
    bic=zeros(1,compon);
    for i=1:compon
        try
            gmx=fitgmdist(variab,i,'Replicates',3);
        catch
            i=i-1;
            gmx=fitgmdist(variab,i,'Replicates',3);
        end
        result=gmx.NegativeLogLikelihood;
        bic(i) = 2*result+(2*i)*log(size(variab,1));
    end
    compon=find(bic==min(bic));
end

% PHASE 2 : comparison of the best Gaussian Mixture and some unimodal
% distributions
[bicmin,~,bes] = TestDistrib(variab,fig);
[~,bic] = disegnaLaGaussMixcheVuoi(variab,compon,fig);
info=abs(bicmin-bic);

if info
    if min(bicmin,bic)==bicmin
        BestDist=bes;
        comp=1;
        info=bicmin;
    else
        BestDist='GaussMix';
        comp=compon;
        info=gmx.BIC;
    end
    BestDist=cellstr(BestDist);
end