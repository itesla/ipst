%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Copyright (c) 2018, RTE and INESC TEC (http://www.rte-france.com and       %%
%% https://www.inesctec.pt)                                                   %%
%% This Source Code Form is subject to the terms of the Mozilla Public        %%
%% License, v. 2.0. If a copy of the MPL was not distributed with this        %%
%% file, You can obtain one at http://mozilla.org/MPL/2.0/.                   %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Authors: José Meirinhos                                                    %%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [vars] = READ_VARIABLES(CE , filename)

fid = fopen( filename ) ; % the original file
new_filename = strcat( 'tmp_', filename );
fidd = fopen( new_filename, 'w' ) ; % the new file
while ~feof( fid ) % reads the original till last line
    tline = fgets( fid );
    if isempty( strfind( tline, 'WARNING' ) )
        fprintf( fidd, tline ) ;
    end
end
fclose all;
data = readtable( new_filename, 'Delimiter', ';' );

if CE ~= false % If a CE was specified
%     idx = find(contains(data{:,2}, CE)); %Find CE
    
    % R2015b version
    idx = find(~cellfun(@isempty,strfind(data{:,2}, CE)));
    vars = (data {idx,1}) ;  % Save variables
else
%     vars = [(data {:,2}) (data {:,3})] ;  % Save variables
%     CE_data = data;
    return;
end

% vars = (data {:,1});  % Save variables

% for i=1:size(vars,1)

delete( new_filename ); % delete new file
end
