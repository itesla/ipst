%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       %%
%% https://www.inesctec.pt)                                                   %%
%% This Source Code Form is subject to the terms of the Mozilla Public        %%
%% License, v. 2.0. If a copy of the MPL was not distributed with this        %%
%% file, You can obtain one at http://mozilla.org/MPL/2.0/.                   %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Author: José Meirinhos jlmm@inesctec.pt                                    %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [ header_vars, header, data_vars ] = READ_WORKFLOW_STATES_FO(vars, filename )
% function [ header_vars, state_id, data_vars ] = READ_WORKFLOW_STATES(vars, filename )

   % Read data
%     filename = strcat(workflow,file); % Tel que fourni par le workflow
    fid = fopen(filename); 

    header = fgetl(fid);
    header = strsplit(header,';');
    fclose(fid);
    
    indexColumnsToRead = [];
    
    % Find state column
    indexCol_state = find(ismember(header,'state'));
    indexColumnsToRead = [indexColumnsToRead indexCol_state];
    
    % Write header to file
%     fid = fopen('header.csv','w');
%     fprintf(fid,'%s\n', header{1,:});
%     fclose(fid);
    
    for n = 1:numel(vars)
        
        indexCol = find(ismember(header,vars{n}));
        

        if indexCol > 0
            indexColumnsToRead = [indexColumnsToRead indexCol];
        else
            disp(['Variable not found: ' vars{n}]);
        end
    end
    indexColumnsToRead = sort(indexColumnsToRead);
    
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

    data_vars=cell(nbValues,nbVariables);
    %TODO : avoid for*for with cellfun/cell2mat
    for k = 1:nbVariables
        for ind = 1:(nbValues)
            if ~isempty(dataRead{1,k}{ind,1})
                data_vars{ind,k} = dataRead{1,k}{ind,1};
            end
        end
    end
        
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

%     if nbValues ~= (length(unique(data_vars(:,2))) * 50)
%         fprintf('Number of states: %i\n',nbValues); % Print the number of states
%     end
        
end