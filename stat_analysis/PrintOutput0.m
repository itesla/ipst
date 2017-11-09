function [] = PrintOutput0(filepath,errore)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% print the summary output in a file excel

f=fullfile(filepath,'UnimodMultimod.mat');
load(f);

% numbers of variables
num_totvar=size(errore,2);
num_multimodal_variables=size(multimodal,1);
num_unimodal_variables=size(unimodal,1);
if (num_multimodal_variables+num_unimodal_variables~=num_totvar)
    disp('output error 1');
end


f=fullfile(filepath,'Clusters.mat');
load(f);
% number of clusters with 2 variables
if ~isempty(ClusterFinP)
    num_clusP=size(ClusterFinP,2);
else
    num_clusP=0;
end
if ~isempty(ClusterFinQ)
    num_clusQ=size(ClusterFinQ,2);
else
    num_clusQ=0;
end  
num_clus=num_clusP+num_clusQ;


% num clusters with 3 variables
if ~isempty(BigClusP)
    num_clusP_more=height(BigClusP);
else
    num_clusP_more=0;
end
if ~isempty(BigClusQ)
    num_clusQ_more=height(BigClusQ);
else 
    num_clusQ_more=0;
end
num_clus_more=num_clusP_more+num_clusQ_more;

%num bad clusters 2 var
if ~isempty(BadClusterP)
    num_clusP_Bad=height(BadClusterP);
else
    num_clusP_Bad=0;
end
if ~isempty(BadClusterQ)
    num_clusQ_Bad=height(BadClusterQ);
else
    num_clusQ_Bad=0;
end
num_clus_bad=num_clusP_Bad+num_clusQ_Bad;

%num bad clusters 3 var
if ~isempty(BigBADP)
    num_bigP_Bad=height(BigBADP);
else
    num_bigP_Bad=0;
end
if ~isempty(BigBADQ)
    num_bigQ_Bad=height(BigBADQ);
else
    num_bigQ_Bad=0;
end
num_big_Bad=num_bigP_Bad+num_bigQ_Bad;

% total variables clustered
num_clusterized_variables=2*num_clusP+2*num_clusQ+3*num_clusP_more+3*num_clusQ_more;

% number of replicated
if ~isempty(ReplicatedP)
    num_replicated_variablesP=height(ReplicatedP);
else
    num_replicated_variablesP=0;
end
if ~isempty(ReplicatedQ)
    num_replicated_variablesQ=height(ReplicatedQ);
else
    num_replicated_variablesQ=0;
end
num_replicated_variables=num_replicated_variablesP+num_replicated_variablesQ;

remainder_multimodal_variables=num_multimodal_variables-num_clusterized_variables;

% descriptive statistics
fulll=fullfile(filepath,'Moments_Summary.mat');
load(fulll,'moments_summary');
T=moments_summary;

% moments summary: variance-mean
% 1=low, 2=medium, 3=high
var_mean(1,3)=sum(strcmp(T.Level_Variance,'low')==1 & strcmp(T.Level_Average,'high')==1);
var_mean(1,2)=sum(strcmp(T.Level_Variance,'low')==1 & strcmp(T.Level_Average,'medium')==1);
var_mean(1,1)=sum(strcmp(T.Level_Variance,'low')==1 & strcmp(T.Level_Average,'low')==1);
var_mean(2,3)=sum(strcmp(T.Level_Variance,'medium')==1 & strcmp(T.Level_Average,'high')==1);
var_mean(2,2)=sum(strcmp(T.Level_Variance,'medium')==1 & strcmp(T.Level_Average,'medium')==1);
var_mean(2,1)=sum(strcmp(T.Level_Variance,'medium')==1 & strcmp(T.Level_Average,'low')==1);
var_mean(3,3)=sum(strcmp(T.Level_Variance,'high')==1 & strcmp(T.Level_Average,'high')==1);
var_mean(3,2)=sum(strcmp(T.Level_Variance,'high')==1 & strcmp(T.Level_Average,'medium')==1);
var_mean(3,1)=sum(strcmp(T.Level_Variance,'high')==1 & strcmp(T.Level_Average,'low')==1);

f=fullfile(filepath,'FinalClassification.mat');
load(f);
% print infos
str1={'SENSITIVITY ANALYSIS';'time [min]';'num tot variables';'multimodal variables';'unimodal variables'};
num1=[0;num_totvar;num_multimodal_variables;num_unimodal_variables];
str2={' ','average low','average medium','average high'
      'variance low',' ',' ',' '
      'variance medium',' ',' ',' '
      'variance high',' ',' ',' '};
num2=var_mean;
str3={'varA','varB','varC','varD','varE','varF','varG','varK'};
num3=[size(varA,1),size(varB,1),size(varC,1),size(varD,1),size(varE,1),size(varF,1),size(varG,1),size(varK,1)]; 
str5={ 'CLUSTERS', 'P', 'Q', 'tot';...
       'num clusters with 2 variables',' ',' ',' ';
       'num clusters with 3 variables',' ',' ',' ';...
       'tot num of clusters',' ',' ',' ';...
       'num clusters eliminated',' ',' ',' ';
       'num clusterized variables',' ',' ',' ';...
       'num replicated variables ',' ',' ',' '; 
       'remainder multimodal variables',' ',' ',' '};
num5={num_clusP num_clusQ num_clus;...
      num_clusP_more num_clusQ_more num_clusP_more+num_clusQ_more;...
      num_clusP+num_clusP_more num_clusQ+num_clusQ_more num_clus+num_clus_more;...
      num_clusP_Bad+num_bigP_Bad num_clusQ_Bad+num_bigQ_Bad num_clus_bad+num_big_Bad;...
      0 0 num_clusterized_variables;...
      num_replicated_variablesP num_replicated_variablesQ num_replicated_variables;...
      0 0 remainder_multimodal_variables};

name='Summary_Infos.xls';
f=fullfile(filepath,name);

xlswrite(f,str1,1,'A1');
xlswrite(f,num1,1,'B2');
xlswrite(f,str2,1,'A7');
xlswrite(f,num2,1,'B8');
xlswrite(f,str3,1,'A12');
xlswrite(f,num3,1,'A13');
xlswrite(f,str5,1,'A15');
xlswrite(f,num5,1,'B16');
   