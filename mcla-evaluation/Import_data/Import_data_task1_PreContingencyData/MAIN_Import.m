%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       %%
%% https://www.inesctec.pt)                                                   %%
%% This Source Code Form is subject to the terms of the Mozilla Public        %%
%% License, v. 2.0. If a copy of the MPL was not distributed with this        %%
%% file, You can obtain one at http://mozilla.org/MPL/2.0/.                   %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Author: José Meirinhos jlmm@inesctec.pt                                    %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Script to import the base cases associated the online workflows

% Jose Meirinhos
% jlmm@inesctec.pt
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clc;
close all;
clear all;
warning('off','all')
tStart = tic;

% profile on

global vars voltage

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Configuration
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
path_FO = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\2018_Feb_FO'; % Path of the FO data
path_SN = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\2018_Feb_SN'; % Path of the SN data
base_case_path = 'C:\Users\jlmm\INESC\3. Projects\iPST\Server\caserepo\IIDM\SN\2018\02'; % Path for the base cases
CE_path = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod'; % Path for CE and output files
out_path = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\All_cont\PreCont\2018_Feb_NEW_V_ALL'; % Path for output files

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Comment if not to consider
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Specify a Region to filter the variables.
% CE = 'Lille'; 
% CE = 'Lyon';
% CE = 'Marseille';
% CE = 'Nancy';
% CE = 'Nanterre';
% CE = 'Nantes';
% CE = 'Toulouse';

% Specify "voltage" as follow:
% voltage = {'1'};  % '1' -> 20 kV (and below);
% voltage = {'2'};  % '2' -> 45 kV;
% voltage = {'3'};  % '3' -> 63 kV;
% voltage = {'4'};  % '4' -> 90 kV;
% voltage = {'5'};  % '5' -> 150 kV;
% voltage = {'6'};  % '6' -> 225 kV;
% voltage = {'7'};  % '7' -> 380 kV;

voltage = {'6','7'}; % Speficy multiple voltage levels

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if (exist(out_path, 'dir') == 0)
    mkdir(out_path);
%     fid = fopen( strcat(out_path, '\myTextLog.txt'), 'w' );
%     fclose(fid);
end

diary(strcat(out_path, '\Debug.txt'))

addpath(path_FO, path_SN, base_case_path, CE_path);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Folders FO
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
d_FO = dir(path_FO);
isub_FO = [d_FO(:).isdir]; % returns logical vector
nameFolds_FO = {d_FO(isub_FO).name}';
nameFolds_FO(cellfun('isempty',strfind(nameFolds_FO,'workflow'))) = [];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Folders SN
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
d_SN = dir(path_SN);
isub_SN = [d_SN(:).isdir]; % returns logical vector
nameFolds_SN = {d_SN(isub_SN).name}';
nameFolds_SN(cellfun('isempty',strfind(nameFolds_SN,'workflow'))) = [];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Reading Variables
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if exist('CE', 'var') == 1
    CE_file = 'lienPostesCE.csv';
    [vars] = READ_VARIABLES( CE, CE_file ); % Extracts the variables.
    % n_var = length(vars); % number of variables
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Run for all Workflows
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
for f = 1:numel(nameFolds_FO)
    
    if ~isempty (nameFolds_FO)
        folder_FO = strcat(path_FO,'\',nameFolds_FO{f});
        date = char(nameFolds_FO{f}(10:17));
        day = char(nameFolds_FO{f}(16:17));
        hour =  char(nameFolds_FO{f}(19:22));
    else
        folder_FO = path_FO;
        [upperPath, deepestFolder, ~] = fileparts(path_FO);
        date = char(deepestFolder(10:17));
        day = char(nameFolds_FO{f}(16:17));
        hour =  char(deepestFolder(19:22));
    end
    
    % Specifie a day to get the results
%     if f < 139
        %     if str2double(day) ~= 30
        %     if str2double(day) < 6
%         continue
%     end
    
    % Print the Workflow TimeStamp
    fprintf('WF: %s_%s\n', date, hour );
    
    % Find the SN base case IIDM file of the workflow under analysis
    d_base_case = dir(strcat(base_case_path,'\',day));
    name_base_case = {d_base_case.name}';
%     bc_idx = find(and(startsWith(name_base_case,strcat(date,'_',hour)), endsWith(name_base_case,'.xiidm')));
%     bc_idx_gz = find(and(startsWith(name_base_case,strcat(date,'_',hour)), endsWith(name_base_case,'.xiidm.gz')));
    
    % R2015b version
    % '^' - start of string ; '$' - end of string
    bc_idx = find(and(~cellfun(@isempty,regexp(name_base_case,strcat('^',date,'_',hour))), ~cellfun(@isempty,regexp(name_base_case,'.xiidm$'))));
    bc_idx_gz = find(and(~cellfun(@isempty,regexp(name_base_case,strcat('^',date,'_',hour))), ~cellfun(@isempty,regexp(name_base_case,'.xiidm.gz$'))));
    
    if ~isempty (bc_idx)
%         gunzip(strcat(base_case_path,'\',day,'\',nameFolds_base_case{bc_idx} ), strcat(base_case_path,'\',day,'\'))
        base_case_file = strcat(base_case_path,'\',day,'\',name_base_case{bc_idx});
        
    elseif ~isempty (bc_idx_gz)
        gunzip(strcat(base_case_path,'\',day,'\',name_base_case{bc_idx_gz} ), strcat(base_case_path,'\',day,'\'))
        d_base_case = dir(strcat(base_case_path,'\',day));
        name_base_case = {d_base_case.name}';
%         bc_idx = find(and(startsWith(name_base_case,strcat(date,'_',hour)), endsWith(name_base_case,'.xiidm')));
        % R2015b version
        bc_idx = find(and(~cellfun(@isempty,regexp(name_base_case,strcat('^',date,'_',hour))), ~cellfun(@isempty,regexp(name_base_case,'.xiidm$'))));
        base_case_file = strcat(base_case_path,'\',day,'\',name_base_case{bc_idx});
    else
        fprintf('Base Case not foud for: %s_%s\n', date, hour );
        return
    end
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Check if exist the FO base case correspondent to the SN
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    WF_name_SN = nameFolds_SN(~cellfun('isempty',strfind(nameFolds_SN, strcat(char(nameFolds_FO{f}(1:22))))));
    folder_SN = char(strcat(path_SN,'\',WF_name_SN));
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
   
    if ~isempty (WF_name_SN)
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % Reads SN data with associated variables and returns a vector with the data for each state
        workflow_states_filename = strcat(folder_SN,'\7.workflow_states.csv');
        [header_vars_SN, header_SN, data_vars_SN ] = READ_WORKFLOW_STATES_SN(workflow_states_filename, base_case_file);
%         [~, ~, data_vars_SN ] = READ_WORKFLOW_STATES(workflow_states_filename);        

       %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % Reads FO data with associated variables and returns a vector with the data for each state
        
        workflow_states_filename = strcat(folder_FO,'\7.workflow_states.csv');
        [ header_vars_FO, header_FO, data_vars_FO ] = READ_WORKFLOW_STATES_FO(header_vars_SN, workflow_states_filename);
%         [ header_vars, state_id_FO, data_vars_FO ] = READ_WORKFLOW_STATES( workflow_states_filename);
    else
        fprintf('...WF not found for SN \n');
        continue; %If not exit, continue to other iteration of for loop
%         data_vars_SN = zeros(1, size(data_vars_FO,2));
    end
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Compare SN and FO Headers
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
   
%     header_SN = header_SN';
%     header_FO = header_FO';
    
    header_compare = cellfun(@strcmp, header_SN, header_FO);
    
    if any(header_compare == 0)
         fprintf(' -> Headers of SN and FO do not match!!! \n');
%          continue;
    end
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
   
    state_id_SN = { '-3'; '-2'; '-1'}; % -2 for the limits and -1 for the SN state
    state_id_FO = data_vars_FO(:,1);
%     state_id_FO = (0:size(data_vars_FO,1)-1)';

    [~,ii] = ismember(header_vars_SN(1,:),header_vars_FO(1,:));
    
    data_SN = [state_id_SN data_vars_SN];
    data_FO = [state_id_FO data_vars_FO(:,ii)]; % Order the FO data by the SN data
    
    workflow_header = ['state', header_vars_SN];
    workflow_states = [data_SN ; data_FO];
    
    if ~issorted(str2double(workflow_states(:,1)))
        fprintf(' ---- The states are not ordered ---- \n');
        %     workflow_states = sortrows(workflow_states,1);
    end
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Remove Variables with NaN
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    vars_NaN_idx = any(~cellfun(@isempty,strfind( workflow_states, 'NaN')),1) ; % Find the variables with some NaN value and add to the end
    vars_NaN = workflow_header(vars_NaN_idx);
    
    workflow_states (:,vars_NaN_idx) = []; % Remove the NaN columns
    workflow_header (:,vars_NaN_idx) = [];
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    out_file = strcat(out_path,'\','WF_',date,'_',hour,'.csv');
    textHeader = strjoin( workflow_header, ';');

    % Write header to file
    fid = fopen(out_file,'w');
    fprintf(fid,'%s\n',textHeader);
    
    for i=1:size(workflow_states,1)
        fprintf(fid,'%s;',workflow_states{i,1:end-1});
        fprintf(fid,'%s\n',workflow_states{i,end});
    end
    
    fclose(fid);
    %write data to end of file
%     dlmwrite(out_file,workflow_states,'-append','delimiter',';');
    
%     profile viewer

end


% fclose(fid);


% profile viewer
% profile off
tEnd = toc(tStart);

% fprintf('Duration TIME: %s \n', duration([0, 0, toc]) );
fprintf('Duration Time: %s \n', datestr(tEnd/(24*60*60), 'HH:MM:SS'));
fprintf('End at: %s\n', datestr(now,'HH:MM:SS'))
fprintf('*** THE END ***\n');

diary off