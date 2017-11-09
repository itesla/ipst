function [moments_summary]=...
    Moments_analysis(errore,ID_in,filepath,soglia,wrt)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% The function comoputes the first four moments of each variable: average,
% variance, skewness, kurtosis.
%
% INPUT
% errore = error matrix
% err = cell array of the error matris without nans
% ID_in = cell array of the names of the variables
% filepath = where to save the output file
% filepath_all = where if the file with the quantiles of data without
% extracting outliers
% soglia = maximun number of outliers to extract
% wrt = if 'on' it writes the results in a csv file
%
% OUTPUT
% moments_summary = table with the moments of each variable, with the
% classification (high, medium, low)

% inizialization data
levelH={'high'};
levelM={'medium'};
levelL={'low'};

% moments
averages = mean(errore,'omitnan');
variances = var(errore,'omitnan');
skew = skewness(errore,'omitnan');
kurt = kurtosis(errore,'omitnan');

% limits
QA=quantile(averages,[0.25 0.75]);
lim_lowA=QA(1)-3*abs(QA(2)-QA(1));
lim_uppA=QA(2)+3*abs(QA(2)-QA(1));
QV=quantile(variances,[0.80 0.988]);
QS=quantile(skew,[0.10 0.90]);
lim_lowS=QS(1)-3*abs(QS(2)-QS(1));
lim_uppS=QS(2)+3*abs(QS(2)-QS(1));
QK=[3 10];

x=[1:length(ID_in)]';
moments_summary=table(x,ID_in',averages',...
    'VariableNames',{'Var_Position','Var_Name','Averages'});

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% comparison between averages
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% I group the variables that have the averages:
% inside the interval [Q1, Q3]  --> low
% in the interval [lim_low,Q1]U[Q3,lim_upp] --> medium
% outside the previous intervals, i.e. (-inf,Q1)U(Q3,+inf) --> high

level=table(cell(size(ID_in')),'VariableNames',{'Level_Average'});

posL=find(averages<=QA(2) & averages>=QA(1));
level{ismember(moments_summary.Var_Position,posL),1}=levelL;
posM=find((averages<QA(1) & averages>=lim_lowA)|(averages>QA(2) & averages<=lim_uppA));
level{ismember(moments_summary.Var_Position,posM),1}=levelM;
posH=find(averages<lim_lowA | averages>lim_uppA);
level{ismember(moments_summary.Var_Position,posH),1}=levelH;

moments_summary=[moments_summary level];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% comparison between variances
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
moments_summary.Variances=variances';
clear level
level=table(cell(size(ID_in')),'VariableNames',{'Level_Variance'});

posH=find(variances>QV(2));
level{ismember(moments_summary.Var_Position,posH),1}=levelH;
posL=find(variances<=QV(1));
level{ismember(moments_summary.Var_Position,posL),1}=levelL;
posM=find(variances<=QV(2) & (variances>QV(1)));
level{ismember(moments_summary.Var_Position,posM),1}=levelM;


moments_summary=[moments_summary level];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% comparison between skewness
% if skew=0, the distribution is symmetrical (cond. nec. but not suff.)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
moments_summary.Skewness=skew';
clear level
level=table(cell(size(ID_in')),'VariableNames',{'Level_Skewness'});

posL=find(skew<=QS(2) & skew>=QS(1));
level{ismember(moments_summary.Var_Position,posL),1}=levelL;
posM=find((skew<QS(1) & skew>=lim_lowS)|(skew>QS(2) & skew<=lim_uppS));
level{ismember(moments_summary.Var_Position,posM),1}=levelM;
posH=find(skew<lim_lowS | skew>lim_uppS);
level{ismember(moments_summary.Var_Position,posH),1}=levelH;

moments_summary=[moments_summary level];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% comparison between kurtosis
% kurt = 3  for normal distribution
% kurt > 3  distribuzione platicurtica (più appiattita)
% kurt < 3  distribuzione leptocurtica (più allungata)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
moments_summary.Kurtosis=kurt';
clear level
level=table(cell(size(ID_in')),'VariableNames',{'Level_Kurtosis'});

posL=find(kurt<=QK(1));
level{ismember(moments_summary.Var_Position,posL),1}=levelL;
posM=find(kurt<=QK(2) & (kurt>QK(1)));
level{ismember(moments_summary.Var_Position,posM),1}=levelM;
posH=find(kurt>QK(2));
level{ismember(moments_summary.Var_Position,posH),1}=levelH;

moments_summary=[moments_summary level];

% output: write files
if strcmp(wrt,'on')==1
    f=fullfile(filepath,'Moments_Summary.csv');
    writetable(moments_summary,f);
end
f=fullfile(filepath,'Moments_Summary.mat');
save(f,'moments_summary');

if soglia==0
    f=fullfile(filepath,'OriginalQuantile');
else
    f=fullfile(filepath,'Quantile');
end
    save(f,'QA','lim_lowA','lim_uppA','QV','QS','lim_lowS','lim_uppS','QK');
end