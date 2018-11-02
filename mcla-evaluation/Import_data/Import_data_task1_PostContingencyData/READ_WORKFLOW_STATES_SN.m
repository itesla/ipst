%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       %%
%% https://www.inesctec.pt)                                                   %%
%% This Source Code Form is subject to the terms of the Mozilla Public        %%
%% License, v. 2.0. If a copy of the MPL was not distributed with this        %%
%% file, You can obtain one at http://mozilla.org/MPL/2.0/.                   %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Author: José Meirinhos jlmm@inesctec.pt                                    %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [ header, header_vars_SN, data_branches, info_branches ] = READ_WORKFLOW_STATES_SN(filename)
% function [ header_vars, state_id, data_vars ] = READ_WORKFLOW_STATES(vars, filename )
global vars voltage

   % Read data
%     filename = strcat(workflow,file); % Tel que fourni par le workflow
    fid = fopen(filename); 

    header = fgetl(fid);
    header = strsplit(header,';');
    
%     data = fgetl(fid);
%     data = strsplit(data,';');
    
    fclose(fid);
    
    indexColumnsToRead = [];
    
    % Write header to file
%     fid = fopen('header_PostCont.csv','w');
%     fprintf(fid,'%s\n', header{1,:});
%     fclose(fid);


    % Find stateId and contingencyId
    indexCol_state = find(ismember(header,'stateId'));
    indexCol_contingency = find(ismember(header,'contingencyId'));
    indexColumnsToRead = [indexColumnsToRead indexCol_state indexCol_contingency];

%     braches_indexCol = intersect( find(contains(header,'__TO__')), find(or(endsWith(header,'_I'), endsWith(header,'_P'))) );
    % R2015b version
    braches_indexCol = intersect( find(~cellfun(@isempty,strfind(header,'__TO__'))), find(~cellfun(@isempty,regexp(header,'_I$')) | ~cellfun(@isempty,regexp(header,'_P$')) | ~cellfun(@isempty,regexp(header,'_IMAX$'))) );
    braches_indexCol_Imax = intersect( find(~cellfun(@isempty,strfind(header,'__TO__'))), find(~cellfun(@isempty,regexp(header,'_IMAX$'))) );
%     branches_header = header(braches_indexCol);

    for i=1:length(braches_indexCol)
        if ~isempty(voltage)

            voltage_index = ones(1,length(voltage));
            
            for v=1:length(voltage)
                
                %             if ~isequal(header{braches_indexCol(i)}(6:7), strcat('L',voltage))
                if ~isequal(header{braches_indexCol(i)}(6:7), strcat('L',voltage{v}))
%                     fprintf(header{braches_indexCol(i)});
                    voltage_index(v) = 0;
                    %             disp (i);
                end
            end
            
            if all(voltage_index == 0)
                braches_indexCol(i) = 0;
            end
                
        else
            if ~isequal(header{braches_indexCol(i)}(6), 'L')
                braches_indexCol(i) = 0;
%                 disp (i);
            end
        end
        
        % ###### Filter the variable just for one side #######
%         if  braches_indexCol(i) ~= 0
%             
%             % Filter the variable just for one side of the load flow
%             from_bus = header{braches_indexCol(i)}(1:5);
%             
%             %             fprintf('%s \n', header{braches_indexCol(i)} );
%             
%             % R2015b version
%             if isempty(regexp(header{braches_indexCol(i)},strcat('^',from_bus)) & strfind(header{braches_indexCol(i)}, strcat('__TO__',from_bus))) ;
%                 braches_indexCol(i) = 0;
%             end
%         end
        % #####################################################
    end
    
    braches_indexCol = braches_indexCol (braches_indexCol ~= 0);
    
    if ~isempty(vars)
        for n = 1:numel(vars)
            % R2016b version
            indexCol = braches_indexCol( startsWith(header(braches_indexCol),vars{n}) & contains(header(braches_indexCol),strcat('__TO__',vars{n})));
            % R2015b version
%             indexCol = braches_indexCol( ~cellfun(@isempty,regexp(header(braches_indexCol),strcat('^',vars{n}))) & ~cellfun(@isempty,strfind(header(braches_indexCol), strcat('__TO__',vars{n}))) );
            
            if indexCol > 0
                indexColumnsToRead = [indexColumnsToRead indexCol];
            end
        end
        
    else   % In case of CE not specified and all variables are required
        
        indexColumnsToRead = [indexColumnsToRead braches_indexCol];
    end
    
    indexColumnsToRead = sort(indexColumnsToRead);
    indexColumnsToRead = unique(indexColumnsToRead);
    
   
    % 1st aproach - faster
    
    cellFormat = repmat({'%*s '},1,length(header));
    cellFormat(indexColumnsToRead) = {'%s '};
%     cellFormat(2) = {'%s '}; % column of the state ID
    formatString = cell2mat(cellFormat);
    
    fid = fopen(filename);
    headerToRead = textscan(fid,formatString,1,'Delimiter',';');
    dataRead = textscan(fid,formatString,'Delimiter',';','HeaderLines',1);

%     headerToRead = header([2 unique(indexColumnsToRead)]);
%     dataRead = textscan(fid,formatString,'Delimiter',';');
    fclose(fid);
    
%     data = cat(2, cellfun(@str2num,dataRead{1,1}), cellfun(@str2num,dataRead{1,2:end}));
%     data_ordered = sortrows(data,1);
%     
%     state_id = data_ordered(:,1);
%     data_vars = data_ordered(:,2:end);
    
%     stateRaw = dataRead(1);
%     dataRaw = dataRead(2:end);
%     dataRaw = dataRead(1:end);

    nbVariables = length(dataRead);
    nbValues = length(dataRead{1});
%     data_vars=zeros(nbValues,nbVariables);
    data_vars = cell(nbValues,nbVariables);
    %TODO : avoid for*for with cellfun/cell2mat
    for k = 1:nbVariables
        for ind = 1:(nbValues)
%             if ~isempty(str2num(dataRead{1,k}{ind,1}))
            if ~isempty(dataRead{1,k}{ind,1})
                data_vars{ind,k} = dataRead{1,k}{ind,1};
            end
        end
    end
    
    % For the Header
    header_vars = cell(1, length(headerToRead));
    for i = 1:length(headerToRead)
        %         header_vars{1,i} = headerToRead{1,i};
        header_vars{1,i} = headerToRead{1,i}{1,1};
    end
    

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Isolate the _IMAX variables
    
    indexColumnsToRead_Imax = indexColumnsToRead(~cellfun(@isempty,regexp(header_vars,'_IMAX$')));
    
    header_branches_idx = find(~cellfun(@isempty,regexp(header_vars,'_I$')) | ~cellfun(@isempty,regexp(header_vars,'_P$')) );
    header_branches_Imax_idx = find(~cellfun(@isempty,regexp(header_vars,'_IMAX$')));
    
    % IMAX variables
    header_branches_Imax = header_vars(header_branches_Imax_idx);
   
    % Keep just the branches
    header_branches = header_vars ( header_branches_idx );

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % Get the Branche Limits
    
%     header_branches_Imax_ID = regexp(header_branches_Imax,'.*?(?=_IMAX)','match');
    
    % Get the max value of all limits
    header_branches_Imax_Lim = nanmax(cellfun(@(x)str2num(x),data_vars(:,header_branches_Imax_idx)),[],1);

    
    if header_branches_Imax_Lim ~= nanmin(cellfun(@(x)str2num(x),data_vars(:,header_branches_Imax_idx)),[],1)
       fprintf('Value min and max are different!!! \n');
    end
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%     limits_header_branches = strings(1,length(header_branches));
    limits_header_branches = cell(1,length(header_branches));
    voltage_level = cell(1,length(header_branches));
    
    for i =1:length(header_branches)
        
        header_ID = header_branches{i}(1:end-2);
        
        limit_ID_idx = find(~cellfun(@isempty,strfind(header_branches_Imax, header_ID)));
        %
        %         limit_ID_idx = find(strcmp(header_branches_Imax, header_ID));
        
        voltage_code = header_branches{i}(7);
        
        switch voltage_code
            case '1'
                voltage_level{i} = num2str(20);
            case '2'
                voltage_level{i} = num2str(45);
            case '3'
                voltage_level{i} = num2str(63);
            case '4'
                voltage_level{i} = num2str(90);
            case '5'
                voltage_level{i} = num2str(150);
            case '6'
                voltage_level{i} = num2str(225);
            case '7'
                voltage_level{i} = num2str(380);
            otherwise
                disp('ERROR!')
        end


        if strcmp(header_branches{i}(end-1:end), '_I')
            limits_header_branches{i} = num2str( header_branches_Imax_Lim(limit_ID_idx));
            
        elseif strcmp(header_branches{i}(end-1:end),'_P')
            
            if strcmp(header_branches_Imax_Lim(limit_ID_idx),'NaN')
                limits_header_branches{i} = 'NaN';
                continue;
            end
            
            % Smax_total[MVA] = sqrt(3)*Imax[A]*Un_composta[kV]/1000.
            limits_header_branches{i} = num2str(sqrt(3)*header_branches_Imax_Lim(limit_ID_idx)*str2double(voltage_level{i})/1000);
            
        end
        
    end

    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    
    
    info_branches = [ voltage_level; limits_header_branches];
%     info_branches = [header_branches; voltage_level; limits_header_branches];
    
    header_vars_SN = [header_vars(:,[1 2]) header_branches];
    
    data_branches = [ data_vars(:,[1 2]) data_vars(:,header_branches_idx) ];
        
end