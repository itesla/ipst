function [] = Module00(ppath,input)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% This function do a statistical analysis of the error variables
% error = snapshot - forecast
% Steps (for each variable):
% 1) Management variables (remove if too many nans or 0, or too little variance)
% 2) It compute the outliers
% 3) it removes the outliers
% 4) It computes the first 4 statistical moments
% 5) It computes the linear correlations
% 6) It fits the variables, looking for multimodality
% 7) It classifies the variables
% 8) It cluster the variables with quite same name and unimodal sum 
% 9) It print the resluts
% 
% The output are printed in one file for each section, containing all the resulting
% information. They are collected in the folder 'output', created in the
% path indicated by the user.
% The user can modify the section 'print results' if he vant prina also xls
% and csv files, in addition to the .mat files
%
% INPUT
% input = must be a .mat file containing: forec_filt, snap_filt, inj_ID
% ppath = the path where to find input data and to put output folder
% load input file

f=fullfile(ppath,input);
load(f); 

% create output directory
filepath=cat(2,ppath,'\output');
if exist(filepath)~=7
    mkdir(ppath,'output')
end


%%%%%%%%%%%%%%%%%%%%%%%
% print results
%%%%%%%%%%%%%%%%%%%%%%%
writeCorrel='off'; % if 'off' it print only .mat file, if 'on' it print also .xls file
writemom='off';    % if 'off' it print only .mat file, if 'on' it print also .csv file
writeClass='off';  % if 'off' it print only .mat file, if 'on' it print also .xls file
print_summary = 'on'; % if 'on' it print xls file, if 'off' it doesn't print nothing

thres = 0.07; % percentage of elements to eliminate if they are outliers

%% ************************************************************************
%   MANAGEMENT OF NANS
%**************************************************************************
disp('')
disp(' Management input ')
[fo,sn,errore,err,ID_in]=Management_variables(forec_filt,snap_filt,inj_ID,filepath);

err_all=err;
errore_all=errore;
soglia=floor(size(errore,1)*thres);

%% ************************************************************************
%   OUTLIERS
%*************************************************************************
disp('')
disp(' Calculation of outliers ')
[outlier,err_sz_out_q,err_sz_out_c] = Outliers(err_all,ID_in,filepath);


%% *************************************************************************
% Choose of outliers
%**************************************************************************
disp('')
disp(' Choosing outliers ')
if soglia>0
    if (exist('errore_all')==1 && exist('err_all')==1 && exist('err_sz_out_q')==1 && exist('err_sz_out_c')==1)
        [err,errore]=ChoosingOutliers(errore_all,err_all,soglia,err_sz_out_q,err_sz_out_c,outlier,ID_in,filepath);
    else
        disp(' ')
        disp(' Before extract outliers you must calculate the outliers ');
        return
    end
else
    disp('****')
    disp('      Treshold = 0, any outlier is extracted')
end


%% *************************************************************************
% 4 statistical MOMENTS
%**************************************************************************
disp('')
disp(' Calculation of statistical moments')
Moments_analysis(errore,ID_in,filepath,soglia,writemom);


%% *************************************************************************
%   CORRELATION
%************************************************************************
disp('')
disp(' Calculation of correlations')
Correlation(errore,writeCorrel,filepath);


%% ************************************************************************
%   MULTIMODALITY
%*************************************************************************
disp('')
disp(' Calculation of multimodality')
[num_comp,gaussmix] = Multimodality(err);

% save variables
filename=fullfile(filepath,'Variables.mat');
save(filename,'errore_all','errore','err_all','err','gaussmix','ID_in','fo','sn');


%% ************************************************************************
%   CLASSIFICATION OF VARIABLES
%*************************************************************************
disp('')
disp(' Classification of variables')
multimodal = ClassificationVariables(num_comp,ID_in,writeClass,filepath);


%% ************************************************************************
%   CLUSTERIZATION
%**************************************************************************
disp(' Clusterization ')
% multim = table2array(multimodal);
ClusterBasedOnNames_Collect(ID_in,errore,multimodal,filepath);


%% ************************************************************************
%   PRINT SUMMARY
%**************************************************************************
disp(' Print output')
if strcmp(print_summary,'on')
    PrintOutput0(filepath,errore)
end


end