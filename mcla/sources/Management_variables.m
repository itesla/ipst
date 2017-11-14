% 
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% INPUT:
% forec_filt = first two columns contains informations about the data
% snap_filt = idem. variables are in the same position of forec_filt.
% inj_ID = same length and position of variables as forec_filt and
% snap_filt.
% OUTPUT:
% fo = matrix of forecasts that will be analized by the rest of the code.
% sn = matrix of snapshots that will be analized by the rest of the code.
% errore = fo - sn.
% err = cell matrix where the nans are eliminated.
% ID_in = name of the variables selected to the analysis.
function [fo,sn,errore,err,ID_in]=Management_variables(forec_filt,snap_filt,inj_ID)

%************* initialization of data vectors *****************************
fo_in=forec_filt(:,3:end);
sn_in=snap_filt(:,3:end);
fo=fo_in;
sn=sn_in;
ID_in=inj_ID;
nminobs=0.7;%ratio of minimum observation to keep the column in the analysis


%**************** elimination of NaN and constant variables ***************
[rf,cf]=size(fo_in);
col_del_f=[];col_del_s=[];
for j=1:cf
    fo_nan=isnan(fo_in(:,j));
    perc_nan=sum(fo_nan)/rf;
    if perc_nan>=(1-nminobs)    % delete var with too much nans (>30%)
        
        col_del_f=[col_del_f j];
    elseif var(fo_in(~fo_nan,j))<=0.1 % delete variables with constant & nans (variance<=1.4901e-8)
        
       col_del_f=[col_del_f j];
    elseif size(find(fo_in(:,j)==0),1)>(nminobs*rf) % delete element=0 for more than 70%
        
        col_del_f=[col_del_f j];
    end
end
for j=1:cf
    sn_nan=isnan(sn_in(:,j));
    perc_nan=sum(sn_nan)/rf;
    if perc_nan>=(1-nminobs)
        col_del_s=[col_del_s j];
    elseif var(sn_in(~sn_nan,j))<=0.1% delete variables with too many constant & nans (variance<=0.1)
       col_del_s=[col_del_s j];
    elseif size(find(sn_in(:,j)==0),1)>(nminobs*rf) % delete variable=0 for more than 70%
        col_del_s=[col_del_s j];
    end
end
col_del=union(col_del_s,col_del_f); %position of the eliminated variables
fo(:,col_del)=[];
sn(:,col_del)=[];
% id_del=table(ID_in(col_del)');
ID_in(col_del)=[];
% f=fullfile(filepath,'VariablesDeleted.txt');
% writetable(id_del,f);
% tablein=table(ID_in',[1:size(ID_in,2)]');
% f=fullfile(filepath,'NameVariables.txt');
% writetable(tablein,f);

%**************** elimination of NaN elements *************************
    errore=sn-fo;
    %%% verification of 
    i=0;cf = size(errore,2);rf =size(errore,1);
    col_del_e=[];
    for j=1:cf
        errore_nan=isnan(errore(:,j));
        perc_nan=sum(errore_nan)/rf;
        if perc_nan>=(1-nminobs)    % delete var with too much nans (>30%)
            i=i+1;
            col_del_e(i)=j;
        elseif var(errore(~errore_nan,j))<=0.1 % delete variables with constant & nans (variance<=1.4901e-8)
            i=i+1;
            col_del_e(i)=j;
        elseif size(find(errore(:,j)==0),1)>(nminobs*(rf*(1-perc_nan))) % delete element=0 for more than 70%
            i=i+1;
            col_del_e(i)=j;
        end
    end
    
    errore(:,col_del_e)=[];
    ID_in(col_del_e)=[];
    %%%%%%%%%%
    err=cell(1,size(errore,2));
    rem_nan=sum(sum(isnan(errore))); %number of nans scattered in the error matrix
    if rem_nan>0  %I delete the remainder nans scattered inside the error matrix
        for j=1:size(errore,2)
            err{j}=errore(~isnan(errore(:,j)),j); %variable under study
        end
    else
        err = mat2cell(errore,size(errore,1),ones(1,size(errore,2)));
    end

