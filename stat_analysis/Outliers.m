function [outlier,err_sz_out_q,err_sz_out_c] = Outliers(err,id_in,filepath)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% OUTPUT:
% outlier
%   is a struct array;
%   every element has 3 fields:
%     *.id = name of the variable
%     *.quantile = values that falls outside the 3*IQR interval AND
%                   that pass the MAD test:
%           "|outlier-median| / mad > 5"
%     *.chebychev = quantile that fall outside the interval
%           [mu - 3sigma, mu + 3sigma]
%
% err_sz_out_q
%   is a cell array that contains, in each variable, the same values of
%   the matrix errore, where nans and outliers calculated with quartile method are eliminated
%
% err_sz_out_c
%   is a cell array that contains, in each variable, the same values of
%   the matrix errore, where nans and outliers calculated with chebyshev method are eliminated
%
% The struct variable all_outlier contains all the info about outliers for each variable:
% the column i refers to the column err{i};
% the row j is the level of n.
% Along one column of all_outliers it is grouped the information of outliers 
% of err{i} when n change (5 cases).
% Every element has 5 fields: 
%     *.id = name of the variable
%     *.n = variation of the amplitude of data not outliers, from 1 to 5;
%     *.quantile = contains the outliers calculated with the quantile formulae
%     *.chebyshev = contains the outliers calculated with the chebyshev formulae
%     *.major = indicates which methods generate the most number of outliers

fig='off';
no_outl=0;
many_outl=0;
diff_meth=0;
one_outl=0;
var_sz_outl=[];
var_many_outl=[];
var_diff_meth=[];
var_one_outl=[];

nc=0;
nq=0;
sizeout=zeros(5,2);

en=size(err,2);
for j=1:en
    i=0;
    for n=1:1:5
        i=i+1;
        
        [o_q,o_c,maj,nc,nq,outl_finali]=outliers_calc(err{j},n,nc,nq);
        
        all_outlier(i,j).ID=char(id_in(j));
        all_outlier(i,j).n=n;
        all_outlier(i,j).quantile=o_q;
        all_outlier(i,j).chebychev=o_c;
        all_outlier(i,j).major=maj;
        
        sizeout(i,:)=[size(o_q,1) size(o_c,1)];
        if n==3
            l=o_c;
            l1=outl_finali;
        end
    end
    % case chebychev = outliers for n=3; case quantile= outliers for n=3
    % AND that pass the MAD test with level=5
    outlier(j).ID=char(id_in(j));
    outlier(j).quantile=l1;
    outlier(j).chebychev=l;
    
    % compare the number of outliers between quantile and chebychev method,
    % to vary from n, with bar plots
    if strcmp(fig,'on')
    if mod(j,25)~=0
        k=mod(j,25);
        subplot(5,5,k)
        bar([1:1:5],sizeout)
        title(j)
    else  k=25;   
        subplot(5,5,k)
        bar([1:1:5],sizeout)
        title(j)
        hL=legend('quantile','chebychev');
        newPosition = [0.4 0.4 0.2 0.2];
        newUnits = 'normalized';
        set(hL,'Position', newPosition,'Units', newUnits);
        figure
    end
    end

 % ********* catalogation of variables with respect to outliers *********
 
 % variables that haven't outliers in one method at least 
 if ismember(0,sizeout(1,:))    
     no_outl=no_outl+1;
     var_sz_outl{1,no_outl}=j;
     var_sz_outl{3,no_outl}=id_in(j);
     var_sz_outl{2,no_outl}=setdiff({'quant' 'cheb'},all_outlier(1,j).major);   
 end
 
 % variables that have outliers only for n=1 in both methods
 if (max(sizeout(1,:)~=0) && max(sizeout(2,:))==0)
     one_outl=one_outl+1;
     var_one_outl{1,one_outl}=j;
     var_one_outl{2,one_outl}=id_in(j);
     var_one_outl{3,one_outl}=all_outlier(1,j).major;
     var_one_outl{4,one_outl}=max(sizeout(1,:));
 end
 
 % variables that have outliers also for n=5 in at least one method
 if sizeout(5,:)~=zeros(1,2)
     many_outl=many_outl+1;
     var_many_outl{1,many_outl}=j;
     var_many_outl{2,many_outl}=id_in(j);
     var_many_outl{3,many_outl}=all_outlier(5,j).major;
     var_many_outl{4,many_outl}=max(sizeout(5,:));
     if(sizeout(5,1)>=0.5*sizeout(1,1) || (sizeout(5,2)>=0.5*sizeout(1,2)))
         var_many_outl{5,many_outl}='slow decrease';
     elseif max(sizeout(5,:))<5
          var_many_outl{5,many_outl}='low outlier for n=5';
     end
 end
 
 % variables that have big difference between the numbers of outliers
 % calculated with the two different methods.
 if (sizeout(1,1)<0.2*sizeout(1,2)||sizeout(1,1)>5*sizeout(1,2))
     diff_meth=diff_meth+1;
     var_diff_meth{1,diff_meth}=j;
     var_diff_meth{2,diff_meth}=id_in(j);
     var_diff_meth{3,diff_meth}=all_outlier(1,j).major;     
     var_diff_meth{4,diff_meth}='n = 1';
     for kk=2:5
         if (min(sizeout(kk,:))~=0 && (sizeout(kk,1)<0.2*sizeout(kk,2)||sizeout(kk,1)>5*sizeout(kk,2)))...
                 || (min(sizeout(kk,:))==0 && max(sizeout(kk,:))>10)
             var_diff_meth{3+kk,diff_meth}=kk;
         end
     end
 end
end

    f=fullfile(filepath,'outlier_info.mat');
    save(f,'all_outlier','outlier','var_sz_outl','var_many_outl',...
        'var_diff_meth','var_one_outl');


    % variables without outliers - quantile
    err_sz_out_q=cell(size(err));
    for j=1:size(err,2)
        ism=ismember(err{j},outlier(j).quantile);
        if max(ism)==1
            p=err{j};
            p(ism==1)=[];
            err_sz_out_q{j}=p;
        else
            err_sz_out_q{j}=err{j};
        end
    end
    
    % variables without outliers - chebychev
    err_sz_out_c=cell(size(err));
    for j=1:size(err,2)
        ism=ismember(err{j},outlier(j).chebychev);
        if max(ism)==1
            p=err{j};
            p(ism==1)=[];
            err_sz_out_c{j}=p;
        else
            err_sz_out_c{j}=err{j};
        end
    end
    
    
end

function [o_q,o_c,major,nc,nq,outl_finali]=...
    outliers_calc(coldata,n,nc,nq)

% this function evaluate outliers with whisker=n;
% the outliers based on the quantiles are the values that fall out of:
% [Q1 - n*(Q3 - Q1) , Q3 + n*(Q3 - Q1 )]
% the outliers based on the Chebyshev's inequality are the elements that fall out of:
% [mu-n*sigma, mu+n*sigma] 
%
% INPUT:  coldata = vector of data
%         n = integer positive
% OUTPUT: o_q = vector of outliers calculated with quantile method
%         o_c = vector of outliers calculated with chebychev method
%         major = method that generates most number of outliers
%         nq = number of outliers with method cheb that aren't outliers
%         with method quantile
%         nc = number of outliers with method quant that aren't outliers
%         with method chebychev
%         outl_finali = outliers that pass the MAD test


%********************* quartile modality  ******************************
% quantile([a b],[q1 q2])=[q1(a) q1(b); q2(a) q2(b)];
q=quantile(coldata,[0.25 0.75]);
lim_low=q(1)-n*(q(2)-q(1));
lim_upp=q(2)+n*(q(2)-q(1));

ol_q=coldata(coldata<lim_low); %outliers lower
ou_q=coldata(coldata>lim_upp); %outliers upper
o_q=[ol_q;ou_q];

rapp=abs(o_q - median(coldata))/mad(coldata);
outl_finali=o_q(rapp>5);

%********************* chebyshev modality ******************************
mu=mean(coldata);
sigma=std(coldata);
lim_low=mu-n*sigma;
lim_upp=mu+n*sigma;

ol_c=coldata(coldata<lim_low);
ou_c=coldata(coldata>lim_upp);
o_c=[ol_c;ou_c];

%************** comparison between the two methods ************************
if (size(ol_c,1)+size(ou_c,1))>(size(ol_q,1)+size(ou_q,1))
    major='cheb';
    nc=nc+1;
elseif (size(ol_c,1)+size(ou_c,1))<(size(ol_q,1)+size(ou_q,1))
    major='quan';
    nq=nq+1;
else
    major=[];
end
end
