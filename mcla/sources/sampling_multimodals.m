% Copyright (c) 2017, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [X_NEW] = sampling_multimodals(y0,type_X0,dati_condMULTI,conditional_sampling,mod_gauss,mod_unif,scenarios)
X_NEW=[];
inj_ID = dati_condMULTI.inj_ID_mult;
idx_err = dati_condMULTI.idx_err_mult;
idx_fore = dati_condMULTI.idx_fore_mult;
limits_reactive=[];
idx_err0 = idx_err;
idx_fore0 = idx_fore;
Ns = scenarios;
type_X = type_X0(:,find(type_X0(3,:)==1));

n_vars = size(type_X,2);
nout=0;
are_snapshots = find(type_X0(4,:)==1);

idx_RES = (type_X0(2,intersect(find(type_X0(1,:)==1),find(type_X0(4,:)==1))));
idxq_RES = (type_X0(2,intersect(find(type_X0(1,:)==4),find(type_X0(4,:)==1))));
idx_carichi = (type_X0(2,intersect(find(type_X0(1,:)==2),find(type_X0(4,:)==1))));
idx_carichiQ = (type_X0(2,intersect(find(type_X0(1,:)==3),find(type_X0(4,:)==1))));
if conditional_sampling == 1 && mod_gauss == 0 && mod_unif == 0
    i_RES = (find(ismember(idx_err0,intersect(find(type_X0(1,:)==1),find(type_X0(4,:)==1)))));
    iq_RES = (find(ismember(idx_err0,intersect(find(type_X0(1,:)==4),find(type_X0(4,:)==1)))));
    Pi_carichi = (find(ismember(idx_err0,intersect(find(type_X0(1,:)==2),find(type_X0(4,:)==1)))));
    Qi_carichi = (find(ismember(idx_err0,intersect(find(type_X0(1,:)==3),find(type_X0(4,:)==1)))));
else
    i_RES = find(type_X0(1,:)==1);
    iq_RES = find(type_X0(1,:)==4);
    Pi_carichi = find(type_X0(1,:)==2);
    Qi_carichi = find(type_X0(1,:)==3);
end

FO1=y0;
CTG_table = dati_condMULTI.CTG_table;
BIV_UU = dati_condMULTI.BIV_UU;
UNIV_DU = dati_condMULTI.UNIV_DU;
prese = [1:length(FO1)];
% SNAPPY=type_X0(6,dati_condMULTI.idx_err_mult);

    for ii=1:length(prese)
FO=FO1(prese(ii));
X=[];
if abs(FO)<=1e-4
    % apply CTG TABLE
   N_sa_nz = floor(CTG_table(prese(ii),2,1)*Ns/(sum(CTG_table(prese(ii),2,:))));
   N_sa_z = ceil(CTG_table(prese(ii),2,2)*Ns/(sum(CTG_table(prese(ii),2,:))));
%    try
       if isnan(N_sa_z) ||  isnan(N_sa_nz)
           X = [zeros(Ns,1)];
       else
   X = [zeros(N_sa_z,1)];
   quantili = UNIV_DU(prese(ii),:);
   cdfs = rand(N_sa_nz,1);
   inter = [0 0.25 0.5 0.75 1];
   X= [X; interp1(inter,quantili,cdfs,'linear','extrap')];
       end
%    catch err
%        test1(ii)=0;
%        via = [via ii];
%    end
%    figure
%    plot(X),xlabel('sample ID'),ylabel('MW'),title(strrep(inj_ID(prese(ii)),'_','-')),grid on
else
   
   
   N_sa_nz = round(CTG_table(prese(ii),1,1)*Ns/(sum(CTG_table(prese(ii),1,:))));
   N_sa_z = round(CTG_table(prese(ii),1,2)*Ns/(sum(CTG_table(prese(ii),1,:))));
   X = [zeros(N_sa_z,1)];
   obj = BIV_UU{prese(ii)};
   try
   pesi = BIV_UU{prese(ii)}.PComponents;
   catch err
       keyboard
   end
   medie_FO = BIV_UU{prese(ii)}.mu(:,1);
  medie_SN = BIV_UU{prese(ii)}.mu(:,2);
   for k=1:size(obj.mu)
       std_FO_k = sqrt(BIV_UU{prese(ii)}.Sigma(1,1,k));
       p(k) = pesi(k)*normpdf(FO,medie_FO(k),std_FO_k);
   end
   if any(p>0)
   p = p/sum(p);
   else
      p(find(pesi==max(pesi)))=1; 
   end
   N = round(N_sa_nz*p);
   if sum(N) < N_sa_nz
    sampl = N_sa_nz-sum(N);
       ima = find(p==max(p));
       N(ima(1))= N(ima(1))+sampl;
   elseif sum(N)>N_sa_nz
       sampl = -N_sa_nz+sum(N);
       ima = find(p==max(p));
       N(ima(1))= N(ima(1))-sampl;
   end

   for k=1:size(obj.mu)
       if N(k)>0
       muck = medie_SN(k) + BIV_UU{prese(ii)}.Sigma(2,1,k)*(FO-medie_FO(k))/(BIV_UU{prese(ii)}.Sigma(1,1,k));
       SIGck = BIV_UU{prese(ii)}.Sigma(2,2,k) - BIV_UU{prese(ii)}.Sigma(2,1,k)*(BIV_UU{prese(ii)}.Sigma(1,2,k))/(BIV_UU{prese(ii)}.Sigma(1,1,k));
       X = [X; muck + sqrt(SIGck)*randn(N(k),1)];
       end
   end
%    figure
%    plot(X),xlabel('sample ID'),ylabel('MW'),title(strrep(inj_ID(prese(ii)),'_','-')),grid on
end
X = X(randperm(length(X)));
quantili=quantile(X,[0.05 0.95]);
if 0
if SNAPPY(ii) <quantili(1) || SNAPPY(ii) > quantili(2)
    nout = nout+1;
    if rem(nout-1,20) ==0
        figure
    end
    if std(X)<0.1
        stdd = 3;
    else
       stdd = std(X); 
    end
    subplot(2,10,rem(nout-1,20)+1),ezcontour(@(x,y)pdf(obj,[x y]),[FO-5*stdd FO+5*stdd],[SNAPPY(ii)-5*stdd SNAPPY(ii)+5*stdd]),hold on, scatter(FO,SNAPPY(ii),'rx')
    title([strrep(inj_ID(ii),'_','-')]),xlabel('FO'),ylabel('SN')
end
end
try
    X_NEW=[X_NEW X];
catch err
    keyboard
end
% X1{ii}=X;
% figure
% plot(UNIV_DU(univariate,2:4),'o-'),xlabel('variable ID'),ylabel('25, 50 and 75% quantiles'),title('Quantiles for univariate distributions with FO=0')
% grid on
clear X p
    end
% keyboard