function [multimodal] =...
    ClassificationVariables(numcomp,IDin,printres,filepath)
% The function classifies the variables with respect to their mean, variance
% and number of peaks. It have to exist the file "Moments_Summary.csv"
%
% INPUT
% numcomp = array of number of peaks detected before
% IDin = cell array with the names of the variables
% printres = if 'on', it prints the classification in a excel file 
% filepath = path where to find the file 'Moments_Summary.csv'
%
% OUTPUT
% a = position of the variables with high average,high variance, many peaks
% b = position of the variables with high average,high variance, one peak
% c = position of the variables with high average,low variance, many peaks
% d = position of the variables with high average,low variance, one peak
% e = position of the variables with low average,high variance, many peaks
% f = position of the variables with low average,high variance, one peak
% g = position of the variables with low average,low variance, many peaks
% k = position of the variables with low average,low variance, one peak
% it prints two files:
%   UnimodMultimod.mat, that contains the division between unimodal and
%           multimodal variables
%   FinalClassification.mat, that contains eight tables with classified
%           variables


% fileinp=fullfile(filepath,'Moments_Summary.csv');
% moments_summary=readtable(fileinp);
fileinp=fullfile(filepath,'Moments_Summary.mat');
load(fileinp,'moments_summary');

condit = (strcmp(moments_summary.Level_Average,'high')|(strcmp(moments_summary.Level_Average,'medium')))...
       & (strcmp(moments_summary.Level_Variance,'high') |(strcmp(moments_summary.Level_Variance,'medium')));
varAB = moments_summary(condit,1);
condit = (strcmp(moments_summary.Level_Average,'high')|(strcmp(moments_summary.Level_Average,'medium')))...
       & (strcmp(moments_summary.Level_Variance,'low'));
varCD = moments_summary(condit,1);
condit = (strcmp(moments_summary.Level_Average,'low'))...
       & (strcmp(moments_summary.Level_Variance,'high') |(strcmp(moments_summary.Level_Variance,'medium')));
varEF = moments_summary(condit,1);
condit = (strcmp(moments_summary.Level_Average,'low'))...
       & (strcmp(moments_summary.Level_Variance,'low'));
varGK = moments_summary(condit,1);

components_one = find(numcomp==1);
components_one = table(components_one,'VariableNames',{'Var_Position'});
components_more = find(numcomp>1);
components_more = table(components_more,'VariableNames',{'Var_Position'});

%%%%%%%%% save a mat file with unimodal and multimodal variables
unimodal=table2array(components_one);
multimodal=table2array(components_more);
ff=fullfile(filepath,'UnimodMultimod.mat');
save(ff,'unimodal','multimodal');
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

varA = intersect(varAB,components_more);
varA.Var_Name=IDin(varA.Var_Position)';
varA.Components=numcomp(varA.Var_Position);
varB = intersect(varAB,components_one);
varB.Var_Name=IDin(varB.Var_Position)';
varB.Components=ones(height(varB),1);


varC = intersect(varCD,components_more);
varC.Var_Name=IDin(varC.Var_Position)';
varC.Components=numcomp(varC.Var_Position);
varD = intersect(varCD,components_one);
varD.Var_Name=IDin(varD.Var_Position)';
varD.Components=ones(height(varD),1);

varE = intersect(varEF,components_more);
varE.Var_Name=IDin(varE.Var_Position)';
varE.Components=numcomp(varE.Var_Position);
varF = intersect(varEF,components_one);
varF.Var_Name=IDin(varF.Var_Position)';
varF.Components=ones(height(varF),1);

varG = intersect(varGK,components_more);
varG.Var_Name=IDin(varG.Var_Position)';
varG.Components=numcomp(varG.Var_Position);
varK = intersect(varGK,components_one);
varK.Var_Name=IDin(varK.Var_Position)';
varK.Components=ones(height(varK),1);

a=varA(:,1);
b=varB(:,1);
c=varC(:,1);
d=varD(:,1);
e=varE(:,1);
f=varF(:,1);
g=varG(:,1);
k=varK(:,1);


%% print results on a file
if strcmp(printres,'on')
fname=fullfile(filepath,'FinalClassification.xls');
xlswrite(fname,{'Classification variables'},1,'A1');
xlswrite(fname,{'var A',' ',' ',' ','var B',' ',' ',' ','var C',' ',' ',' ','var D',' ',' ',' ',},1,'A2');
xlswrite(fname,{'var E',' ',' ',' ','var F',' ',' ',' ','var G',' ',' ',' ','var K',' ',' ',' '},1,'Q2');
writetable(varA,fname,'Range','A3');
writetable(varB,fname,'Range','E3');
writetable(varC,fname,'Range','I3');
writetable(varD,fname,'Range','M3');
writetable(varE,fname,'Range','Q3');
writetable(varF,fname,'Range','U3');
writetable(varG,fname,'Range','Y3');
writetable(varK,fname,'Range','AC3');
xlswrite(fname,{'var A = high and medium mean, high and medium variance, many peaks';...
'var B = high and medium mean mean, high and medium variance, one peak';...
'var C = high and medium mean mean, low variance, many peaks';...
'var D = high and medium mean mean, low variance, one peak';...
'var E = low mean, high and medium variance, many peaks';...
'var F = low mean, high and medium variance, one peak';...
'var G = low mean, low variance, many peaks';...
'var K = low mean, low variance, one peak'},1,'AG1');
end
fname=fullfile(filepath,'FinalClassification.mat');
save(fname,'varA','varB','varC','varD','varE','varF','varG','varK');
