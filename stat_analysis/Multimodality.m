function [num_comp,gaussmix] = Multimodality(err)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% The function finds the Gaussian Mixture that fits each variable
%
% INPUT:
% err = cell array  of the variable error (there aren't nans)
%
% OUTPUT:
% num_comp = array with the number of modes of each variables 
% gaussmix = cell array whos elements are the gmdistribution of the
%            variables

warning('off')
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

% for each variable, looks for the GAUSSIAN MIXTURES
for i=1:numvar
    BIC=1e+5;
    ng=1e+5;
    for j=1:(num_peak(i))+1
        try
            rng(100);
            gm=fitgmdist(err{i},j,'Replicates',3);
        catch
            gm=gm1;
        end
        if gm.Converged~=0
            if (gm.BIC<BIC && gm.NegativeLogLikelihood<ng)
                BIC=gm.BIC;
                ng=gm.NegativeLogLikelihood;
                gaussmix{i} = gm;
            end            
        end
        gm1=gm;
    end
    num_comp(i)=gaussmix{i}.NumComponents;
    bic(i)=gaussmix{i}.BIC;
    negloglik(i)=gaussmix{i}.NegativeLogLikelihood;
    
    % UNIMODAL DISTRIBUTION
    if num_comp(i)>1
        [bic_unimod,aic_unimod,distr_unimod] = TestDistrib(err{i},'no');
        
        % TEST COMPARISON
        minbic=min(bic(i),bic_unimod);
        
        if minbic==bic_unimod
            gaussmix{i}=[];
            gaussmix{i}.NumVariables=1;
            gaussmix{i}.DistributionName=distr_unimod;
            gaussmix{i}.NumComponents=1;
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
                gaussmix{i}.NumComponents=1;
                gaussmix{i}.ComponentProportion=1;
                gaussmix{i}.AIC=aic_unimod;
                gaussmix{i}.BIC=bic_unimod;
                num_comp(i)=1;
            end
        end
    end
end

end

%%
function [numpicchi,massi]=findpeaks(variab,tit,fig)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% the function finds the peaks in a distribution.
%
% INPUT
% variab =  column to analyse
% tit = title to put on the figure, when it exist
% fig = if 'on', the function produce a figure of the distribution with the
% peack detectd
%
% OUTPUT
% numpicchi = vector with the number of peaks detected in each variable
% massi = vector with the positions and the values of the peaks detected

[y,x]=hist(variab,50);

% run peakdet twice left-to-right and right-to-left
delta = 0.5;
[ymaxtab, ymintab] = peakdet(y, delta, x);
[ymaxtab2, ymintab2] = peakdet(y(end:-1:1), delta, x(end:-1:1));
ymaxtab = unique([ymaxtab; ymaxtab2],'rows');
ymintab = unique([ymintab; ymintab2],'rows');

% plot the curve and show extreme points based on number of peaks
if strcmp(fig,'on')
    figure
    plot(x,y)
    hold on
    if size(ymaxtab,1) == 2 && size(ymintab,1) == 1 % if double peak
        plot(ymintab(:,1),ymintab(:,2),'c.','markersize',30)
    elseif size(ymaxtab,1) == 1 && size(ymintab,1) == 0 % if single peak
        plot(ymaxtab(:,1),ymaxtab(:,2),'r.','markersize',30)
    else % if more (or less)
        plot(ymintab(:,1),ymintab(:,2),'c.','markersize',30)
        plot(ymaxtab(:,1),ymaxtab(:,2),'r.','markersize',30)
    end
end

massi=[];
pm=find(ymaxtab(:,2)==max(ymaxtab(:,2)));
j=1;
for i=pm:-1:2
    if (ymaxtab(i,2)<ymaxtab(i-1,2) && i>2)
        if ymaxtab(i-2,2)<=ymaxtab(i-1,2)
            massi(j,:)=ymaxtab(i-1,:);
            j=j+1;
        end
    elseif (ymaxtab(i,2)<=ymaxtab(i-1,2) && i==2)
            massi(j,:)=ymaxtab(i-1,:);
            j=j+1;        
    end
end
flipud(massi);
massi=[massi;ymaxtab(pm,:)];
j=j+1;
for i=pm:length(ymaxtab)-1
    if (ymaxtab(i,2)<ymaxtab(i+1,2) && i+2<=length(ymaxtab))
        if ymaxtab(i+1,2)>=ymaxtab(i+2,2)
        massi(j,:)=ymaxtab(i+1,:);
        j=j+1;
        end
    elseif (ymaxtab(i,2)<=ymaxtab(i+1,2) && (i+2==length(ymaxtab) || i+1==length(ymaxtab)))
        massi(j,:)=ymaxtab(i+1,:);
        j=j+1;        
    end
end

% delete peaks lower than 10% of highest peak 
ok=find(massi(:,2)/max(ymaxtab(:,2))<0.1);
if isempty(ok)==0
    massi(ok,:)=[];
end
if strcmp(fig,'si')
    hold on
    plot(massi(:,1),massi(:,2),'xk','lineWidth',2)
    title(tit)
    hold off
end

numpicchi=size(massi,1);

end

%%
function [maxtab, mintab]=peakdet(v, delta, x)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%PEAKDET Detect peaks in a vector
%[MAXTAB, MINTAB] = PEAKDET(v, DELTA) finds the local maxima and minima
%("peaks") in the vector v.
% otuput: 
%  MAXTAB indices in x,
%  MINTAB the found values.
%  A point is considered a maximum peak if it has the maximal value,
%  and was preceded (to the left) by a value lower by DELTA.


maxtab = [];
mintab = [];

mn = Inf;
mx = -Inf;
mnpos = NaN;
mxpos = NaN;

lookformax = 1;

for i=1:length(v)
  this = v(i);
  if this > mx
      mx = this;
      mxpos = x(i);
  end
  if this < mn
      mn = this;
      mnpos = x(i);
  end
  
  if lookformax
    if this < mx-delta
      maxtab = [maxtab ; mxpos mx];
      mn = this; mnpos = x(i);
      lookformax = 0;
    end  
  else
    if this > mn+delta
      mintab = [mintab ; mnpos mn];
      mx = this; mxpos = x(i);
      lookformax = 1;
    end
  end
end
end

