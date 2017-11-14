%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [num_comp,gaussmix,bic,negloglik] = Multimodality(err)

% The function finds the Gaussian components that fit each variable
%
% INPUT:
% err = cell array  of the variable error (there aren't nans)
%
% OUTPUT:
% num_comp = array with the number of modes of each variables 
% gaussmix = cell array whos elements are the gmdistribution of the
% variables
% bic = bic index of the best fit
% negloglik = neative loglikelihood index of the best fit

warning('off','stats:gmdistribution:FailedToConverge')
numvar=size(err,2);
num_peak=zeros(1,numvar);
num_comp=zeros(numvar,1);
bic=zeros(numvar,1);
negloglik=zeros(numvar,1);
gaussmix=cell(1,numvar);

% number of peaks in each variable

for i=1:numvar   
    [num_peak(i),~]=findpeaks(err{i},i,'no');
end

% for each variable:
for i=1:numvar
    % GAUSSIAN MIXTURES
    BIC=1e+5;
    ng=1e+5;
    for j=1:(num_peak(i))+1
        try
            rng(100);
            gm=gmdistribution.fit(err{i},j,'Replicates',3);
        catch
            gm=gm1;
        end
        if gm.Converged~=0
            if (gm.BIC<BIC && gm.NlogL<ng)
                BIC=gm.BIC;
                ng=gm.NlogL;
                gaussmix{i} = gm;
            end            
        end
        gm1=gm;
    end
    try
    num_comp(i)=gaussmix{i}.NComponents;
    bic(i)=gaussmix{i}.BIC;
    negloglik(i)=gaussmix{i}.NlogL;
    catch errx
        keyboard
    end
    
    % UNIMODAL DISTRIBUTION
    if num_comp(i)>1
        [bic_unimod,aic_unimod,distr_unimod] = TestDistrib(err{i},'no');
        
        % TEST COMPARISON
        minbic=min(bic(i),bic_unimod);
        
        if minbic==bic_unimod
            gaussmix{i}=[];
            gaussmix{i}.NumVariables=1;
            gaussmix{i}.DistributionName=distr_unimod;
            gaussmix{i}.NComponents=1;
            gaussmix{i}.ComponentProportion=1;
            gaussmix{i}.AIC=aic_unimod;
            gaussmix{i}.BIC=bic_unimod;
            num_comp(i)=1;
        else
            % Bimodal Test
            [A,D]=BimodalTest(gaussmix,i);
            if (A<0 || D<2)
                gaussmix{i}=[];
                gaussmix{i}.NumVariables=1;
                gaussmix{i}.DistributionName=distr_unimod;
                gaussmix{i}.NComponents=1;
                gaussmix{i}.ComponentProportion=1;
                gaussmix{i}.AIC=aic_unimod;
                gaussmix{i}.BIC=bic_unimod;
                num_comp(i)=1;
            end
        end
    end
end

end


