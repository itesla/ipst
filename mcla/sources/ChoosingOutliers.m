%
% Copyright (c) 2017, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [err,errore_outl]=ChoosingOutliers(errore,err,soglia,err_sz_out_q,err_sz_out_c,outlier)
% this function eliminates the outliers in those variables that have less then
% "soglia" outliers in both methods
% INPUT
    % errore: error matrix
    % err: error cell array
    % soglia: max number of elements to be removed
    % err_sz_out_q: error cell array without outliers calculated with
    %               quantile method
    % err_sz_out_c: error cell array without outliers calculated with
    %               chebyshev method
    % outlier: structure with the outlier selected to be extracted
    % ID_in: names of the variables
    % filepath: path of the position of the output
% OUTPUT:
    % errore_outl: error matrix where the outliers extracted are
    % substituted by nans elements
    % err: error cell array where the outliers selected are extracted
    
for i=1:length(err)
    num_outl_q(i,1)=size(outlier(i).quantile,1);
    num_outl_c(i,1)=size(outlier(i).chebychev,1);
    if num_outl_q(i,1)>=num_outl_c(i,1)
        num_outl_q(i,2)=1;
        num_outl_c(i,2)=0;
    else
        num_outl_q(i,2)=0;
        num_outl_c(i,2)=1;   
    end
end
num_outl_q(:,1)=num_outl_q(:,1).*num_outl_q(:,2);
num_outl_c(:,1)=num_outl_c(:,1).*num_outl_c(:,2);
elimin_q=intersect(find(num_outl_q(:,1)<=soglia),find(num_outl_q(:,1)>0));%variables whose outliers are eliminated with quantile
elimin_c=intersect(find(num_outl_c(:,1)<=soglia),find(num_outl_c(:,1)>0));%variables whose outliers are eliminated with chebychev
err(elimin_q)=err_sz_out_q(elimin_q);%substitution of variables with outliers with variables without outliers quantile
err(elimin_c)=err_sz_out_c(elimin_c);%substitution of variables with outliers with variables without outliers chebychev

errore_outl=errore;
for i=1:length(err)
    if ismember(i,elimin_c)
        iii=ismember(errore(:,i),outlier(i).chebychev);
        errore_outl(iii,i)=NaN;
    elseif ismember(i,elimin_q)
        iii=ismember(errore(:,i),outlier(i).quantile);
        errore_outl(iii,i)=NaN;
    end
end

% print results
% q=repmat({'quant'},[length(elimin_q),1]);
% c=repmat({'cheb'},[length(elimin_c),1]);
% % tablein_q=table(elimin_q,ID_in(elimin_q)',num_outl_q(elimin_q,1),q,...
%     'VariableNames',{'Var_Position','Var_Name','Outliers_eliminated','Method'});
% tablein_c=table(elimin_c,ID_in(elimin_c)',num_outl_c(elimin_c,1),c,...
%     'VariableNames',{'Var_Position','Var_Name','Outliers_eliminated','Method'});
% outliers_eliminated=[tablein_q;tablein_c];
% f=fullfile(filepath,'OutliersEliminated.csv');
% writetable(tablein,f);
% f=fullfile(filepath,'OutliersEliminated.mat');
% save(f,'outliers_eliminated');