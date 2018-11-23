%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       %%
%% https://www.inesctec.pt)                                                   %%
%% This Source Code Form is subject to the terms of the Mozilla Public        %%
%% License, v. 2.0. If a copy of the MPL was not distributed with this        %%
%% file, You can obtain one at http://mozilla.org/MPL/2.0/.                   %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Authors: José Meirinhos                                                    %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [ header_vars, header, data_vars ] = READ_WORKFLOW_STATES_SN(filename, base_case_file)
% function [ header_vars, state_id, data_vars ] = READ_WORKFLOW_STATES(vars, filename )
global vars voltage flow

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
%     fid = fopen('header.csv','w');
%     fprintf(fid,'%s\n', header{1,:});
%     fclose(fid);

%     braches_indexCol = intersect( find(contains(header,'__TO__')), find(or(endsWith(header,'_I'), endsWith(header,'_P'))) );
    % R2015b version
    braches_indexCol = intersect( find(~cellfun(@isempty,strfind(header,'__TO__'))), find(or(~cellfun(@isempty,regexp(header,'_I$')), ~cellfun(@isempty,regexp(header,'_P$')))) );
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
        
        if flow == 1
            % ###### Filter the variable just for one side #######
            if  braches_indexCol(i) ~= 0
                
                % Filter the variable just for one side of the load flow
                from_bus = header{braches_indexCol(i)}(1:5);
                
                %             fprintf('%s \n', header{braches_indexCol(i)} );
                
                % R2015b version
                if isempty(regexp(header{braches_indexCol(i)},strcat('^',from_bus)) & strfind(header{braches_indexCol(i)}, strcat('__TO__',from_bus))) ;
                    braches_indexCol(i) = 0;
                end
            end
            % #####################################################
        end
    end
    
    braches_indexCol = braches_indexCol (braches_indexCol ~= 0);
    
    if ~isempty(vars)
        for n = 1:numel(vars)
            
%             %         indexCol = braches_indexCol( startsWith(header(braches_indexCol),vars{n}) & contains(header(braches_indexCol),strcat('__TO__',vars{n})));
%             % R2015b version
%             indexCol = braches_indexCol( ~cellfun(@isempty,regexp(header(braches_indexCol),strcat('^',vars{n}))) & ~cellfun(@isempty,strfind(header(braches_indexCol), strcat('__TO__',vars{n}))) );
%             
%             if indexCol > 0
%                 indexColumnsToRead = [indexColumnsToRead indexCol];
%             end
            
            % Update 20180719 for just fister the header variables that contain the the vars
            indexCol = braches_indexCol( ~cellfun(@isempty,strfind(header(braches_indexCol), vars{n})) );
            
            if indexCol > 0
                indexColumnsToRead = [indexColumnsToRead indexCol];
            end          
        end    
    else   % In case of CE not specified and all variables are required
        
        indexColumnsToRead = braches_indexCol;
    end
    
    indexColumnsToRead = sort(indexColumnsToRead);
    indexColumnsToRead = unique(indexColumnsToRead);
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    header_branches = header(indexColumnsToRead);
    
%     header_branches = endsWith(header(indexColumnsToRead),'_I');

%     header_ID = cellfun(@(s)extractBefore(s,'__TO__'),header_branches,'UniformOutput',false);
%     header_ID = unique([header_ID{1,:}]);
    
    % R2015b version
    header_ID = regexp(header_branches,'.*?(?=__TO__)','match');
    header_ID = unique(string(header_ID));
    
    base_case=fileread(base_case_file);
    % A = splitlines(A);
    
%     limits_ID = strings(1,length(header_ID));
    limits_ID = cell(1,length(header_ID));
    
%     tic
    
    for i=1:length(header_ID)
        
        line_idx = strfind(base_case,strcat('line id="', header_ID{i},'"'));
        
        if ~isempty(line_idx)
%             line_arg = extractBetween(base_case,line_idx,'line>');
            
            % R2015b version
%             expression = strcat('<line id=','"', header_ID{i},'"','.*\</line>');
%             [tokens,matches] = regexp(base_case,expression,'tokens','match');

            % Info: https://www.mathworks.com/matlabcentral/answers/289160-extract-strings-between-one-repeated-tag-in-a-xml-file
%                                  (?<=names id="\d+" name="Name1").*?(?=</names>)
%             expression = strcat('(?<=line id="', header_ID{i},'"', ').*?(?=</line>)');
            expression = strcat('(?=line id="', header_ID{i},'"', ').*?(?=line>)');
            matches = regexp(base_case,expression,'match');
            line_arg = string(matches(1));
            
%             expression = strcat('(?<=<line)\s+id="', header_ID{i},'"', '(.*?)>(.*?)(?=</line>)');
%             [tokens,matches] = regexp(base_case,expression,'tokens','match');
        
        else
            line_idx = strfind(base_case,strcat('twoWindingsTransformer id="', header_ID{i},'"'));
            
            if ~isempty(line_idx)
%                 line_arg = extractBetween(base_case,line_idx,'twoWindingsTransformer>');
                % R2015b version
                expression = strcat('(?=twoWindingsTransformer id="', header_ID{i},'"', ').*?(?=twoWindingsTransformer>)');
                matches = regexp(base_case,expression,'match');
                line_arg = string(matches(1));
                
                fprintf('- line ID: %s was modeled as twoWindingsTransformer \n', header_ID{i});
            else
                fprintf(' ### line ID: %s not found ### \n', header_ID{i});
            end
        end
        
%         disp(i);
        
        if ~isempty(line_arg)
            
%             line_arg = extractBetween(base_case,line_idx,'</line>');
            %         line_id = extractBetween(line_arg,'<line id="','"');
%             line_currentlimit = extractBetween(line_arg,'currentLimits1 permanentLimit="','"');
            % R2015b version
            expression = '(?<=currentLimits1 permanentLimit=").*?(?=")';
            line_currentlimit = char(regexp(line_arg,expression,'match'));
            
            if isempty(line_currentlimit)
                %                 line_currentlimit = extractBetween(line_arg,'currentLimits2 permanentLimit="','"');
                % R2015b version
                expression = '(?<=currentLimits2 permanentLimit=").*?(?=")';
                line_currentlimit = regexp(line_arg,expression,'match');
                
                if ~isempty(line_currentlimit)
                    fprintf('- line ID: %s only has currentLimits2 \n', header_ID{i});
                end
                
            end
            if isempty(line_currentlimit)
                
                fprintf(' ### Current Limit for the ID: %s not found ### \n', header_ID{i});
                limits_ID{i} = 'NaN';
                continue; %If not exit, continue to other iteration of for loop
            end
            
            if ~isempty(line_currentlimit)
%                 line_currentlimit = double(line_currentlimit);
                limits_ID{i} = line_currentlimit;
            end
        end
    end
    
    %         if ~isempty(line_currentlimit)

%     limits_header_branches = strings(1,length(header_branches));
    limits_header_branches = cell(1,length(header_branches));
    
    for i =1:length(header_branches)
        
%         limit_ID_idx = find(strcmp(header_ID, extractBefore(header_branches{i},'__TO__')));
        % R2015b version
        expression = '.*?(?=__TO__)';
        matches = string(regexp(header_branches{i},expression,'match'));
        limit_ID_idx = find(strcmp(header_ID, matches));
        
        if strcmp(header_branches{i}(end-1:end), '_I')
            limits_header_branches{i} = limits_ID{limit_ID_idx};
            
        elseif strcmp(header_branches{i}(end-1:end),'_P')
            
            if strcmp(limits_ID{limit_ID_idx},'NaN')
                limits_header_branches{i} = 'NaN';
                continue;
            end
            
            
            voltage_level = header_branches{i}(7);
            
            switch voltage_level
                case '1'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*20/1000);  % Smax_total[MVA] = sqrt(3)*Imax[A]*Un_composta[kV]/1000.
                case '2'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*45/1000);
                case '3'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*63/1000);
                case '4'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*90/1000);
                case '5'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*150/1000);
                case '6'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*225/1000);
                case '7'
                    limits_header_branches{i} = num2str(sqrt(3)*str2double(limits_ID{limit_ID_idx})*380/1000);
                otherwise
                    disp('ERROR!')
            end
        end
        
    end
    
%     toc
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
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
    dataRaw = dataRead(1:end);

    nbVariables = length(dataRaw);
    nbValues = length(dataRaw{1});
%     data_vars=zeros(nbValues,nbVariables);
    data_vars = cell(nbValues,nbVariables);
    %TODO : avoid for*for with cellfun/cell2mat
    for k = 1:nbVariables
        for ind = 1:(nbValues)
            if ~isempty(str2num(dataRaw{1,k}{ind,1}))
                data_vars{ind,k} = dataRaw{1,k}{ind,1};
            end
        end
    end
    
    data_vars = [limits_header_branches; data_vars];
        
%     state_id=zeros(nbValues,1);
%     %TODO : avoid for*for with cellfun/cell2mat
%     for ind = 1:(nbValues)
%         if ~isempty(str2num(stateRaw{1,1}{ind,1}))
%             state_id(ind,1) = str2num(stateRaw{1,1}{ind,1});
%         end
%     end
        
    % For the Header
    header_vars = cell(1, length(headerToRead));
    for i = 1:length(headerToRead)
%         header_vars{1,i} = headerToRead{1,i};
        header_vars{1,i} = headerToRead{1,i}{1,1};
    end
    
    % 2nd aproach - slower
    
%     tmp = readtable( filename, 'Delimiter', ';' );
%     state_id = tmp{ :, 2 };
%     data_vars = tmp{ :, indexColumnsToRead };
        
end