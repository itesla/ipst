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

global contingency vars voltage vars_NaN

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Configuration
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
path_FO = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\2018_Mar_FO'; % Path of the FO data
path_SN = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\2018_Mar_SN'; % Path of the SN data
CE_path = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod'; % Path for CE
out_path = 'D:\INESC\iPST\Server\FEA 2018jan_feb_nS_50_UniMod\All_cont\PostCont\2018_Mar_NEW_V_ALL'; % Path for output files

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

% Specify Contingency as follow:
% contingency = 'N-2_Tavel-Realtor';
% contingency = 'N-2_Launa_Taute';

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

if (exist(out_path, 'dir') == 0)
    mkdir(out_path);
%     fid = fopen( strcat(out_path, '\myTextLog.txt'), 'w' );
%     fclose(fid);
end

diary(strcat(out_path, '\Debug_PostCont.txt'))


addpath(path_FO, path_SN, CE_path);

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
if ~isempty(contingency)
    vars_file = 'variables_contingency.csv';
    [vars] = READ_VARIABLES( vars_file ); % Extracts the variables and limits.
    n_var = length(vars); % number of variables

elseif exist('CE', 'var') == 1
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
%     if f > 300
%     if str2double(day) ~= 30
%     if str2double(day) < 6
%         continue
%     end
    
    % Print the Workflow TimeStamp
    fprintf('WF: %s_%s\n', date, hour );
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Check if exist the FO base case correspondent to the SN
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    WF_name_SN = nameFolds_SN(~cellfun('isempty',strfind(nameFolds_SN, strcat(char(nameFolds_FO{f}(1:22))))));
    folder_SN = char(strcat(path_SN,'\',WF_name_SN));
    
    states_filename_SN = strcat(folder_SN,'\9.workflow_postcontingency_states.csv');
    states_filename_FO = strcat(folder_FO,'\9.workflow_postcontingency_states.csv');
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    %     if ~isempty (WF_name_SN)
    if   exist(states_filename_SN, 'file') % == 2;
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % Reads SN data with associated variables and returns a vector with the data for each state
        [ header_SN, header_vars_SN, data_vars_SN, info_branches ] = READ_WORKFLOW_STATES_SN(states_filename_SN);
        %         [~, ~, data_vars_SN ] = READ_WORKFLOW_STATES(workflow_states_filename);
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % Reads FO data with associated variables and returns a vector with the data for each state
    
        [ header_vars_FO, header_FO, data_vars_FO ] = READ_WORKFLOW_STATES_FO(header_vars_SN, states_filename_FO);
        %         [ header_vars, state_id_FO, data_vars_FO ] = READ_WORKFLOW_STATES( workflow_states_filename);
    else
        fprintf('... PostContingency_states not found for SN \n');
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

        % Write header to file
%         fid = fopen('header_SN.csv','w');
%         fprintf(fid,'%s\n', header_SN{1,:});
%         fclose(fid);
%         fid = fopen('header_FO.csv','w');
%         fprintf(fid,'%s\n', header_FO{1,:});
%         fclose(fid);
    end

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Split results by contingency
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    contingencies = unique( data_vars_SN(:,2));
    
    for i = 1:length(contingencies)
        
        clear workflow_header workflow_states
        
        data_vars_SN_cont = data_vars_SN ( ~cellfun(@isempty,strfind( data_vars_SN(:,2), contingencies{i})), :);
        data_vars_FO_cont = data_vars_FO ( ~cellfun(@isempty,strfind( data_vars_FO(:,2), contingencies{i})), :);
        header_vars_SN_cont = header_vars_SN;
        header_vars_FO_cont = header_vars_FO;

        data_vars_SN_cont(1) = {'-1'}; % -1 for the SN state
        
        
        data_vars_SN_cont (:,2) = []; % Remove column contingency
        data_vars_FO_cont (:,2) = []; % Remove column contingency
        header_vars_SN_cont (:,2) = []; % Remove column contingency
        header_vars_FO_cont (:,2) = []; % Remove column contingency
        
        state_id_Imax = { '-3'; '-2' }; % -2 for the limits
        
%         state_id_FO = data_vars_FO_cont(:,1);
        
        [~,ii] = ismember(header_vars_SN_cont(1,:),header_vars_FO_cont(1,:));
        
        data_SN = [state_id_Imax info_branches; data_vars_SN_cont];
        data_FO = data_vars_FO_cont(:,ii); % Order the FO data by the SN data
        
        workflow_header = header_vars_SN_cont;
        workflow_states = [data_SN ; data_FO];
        if ~issorted(str2double(workflow_states(:,1)))
            fprintf(' ---- The states are not ordered ---- \n');
            %     workflow_states = sortrows(workflow_states,1);
        end
        
        n_states_FO = length(data_vars_FO_cont(:,2));
        if n_states_FO ~= 50
            fprintf('# %i states for the contingency %s\n', n_states_FO, contingencies{i}); % Print the number of states
        end
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % Remove Variables with NaN 
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        vars_NaN_idx = any(~cellfun(@isempty,strfind( workflow_states, 'NaN')),1) ; % Find the variables with some NaN value and add to the end
        vars_NaN = workflow_header(vars_NaN_idx);
        
%         vars_NaN = unique(vars_NaN);
        
        workflow_states (:,vars_NaN_idx) = [];
        workflow_header (:,vars_NaN_idx) = [];
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       
        out_file = strcat(out_path,'\', contingencies{i},'\' ,'WF_',date,'_',hour,'_PostCont', '.csv');
        
        if ~exist(strcat(out_path,'\', contingencies{i}),'dir')
            mkdir (strcat(out_path,'\', contingencies{i}))
        end

%         out_file = strcat(out_path,'\','WF_',date,'_',hour, '_', contingencies{i} ,'.csv');
        
%         header_vars_SN = ['state', header_vars_SN];
        textHeader = strjoin( workflow_header, ';');
        
        % Write header to file
        fid = fopen(out_file,'w');
        fprintf(fid,'%s\n',textHeader);
        
        for j=1:size(workflow_states,1)
            fprintf(fid,'%s;',workflow_states{j,1:end-1});
            fprintf(fid,'%s\n',workflow_states{j,end});
        end
        
        fclose(fid);
        %write data to end of file
        %     dlmwrite(out_file,workflow_states,'-append','delimiter',';');
        
        %     profile viewer
    end

end

% fclose(fid);

tEnd = toc(tStart);
% fprintf('Duration TIME: %s \n', duration([0, 0, toc]) );
fprintf('Duration Time: %s \n', datestr(tEnd/(24*60*60), 'HH:MM:SS'));
fprintf('End at: %s\n', datestr(now,'HH:MM:SS'))
fprintf('*** THE END ***\n');

diary off
% profile viewer
% profile off
