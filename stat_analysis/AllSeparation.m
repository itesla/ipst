function [A,B,D,C,TF,tabClus] = AllSeparation(caso,newsum,fig,path)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

% Stand Alone function
% This function separates the sum of one cluster into two or three clustered variables
% INPUT
%   caso = one of {'P2', 'P3', 'Q2', 'Q3'} considering active or reactive power, 2 or 3
%          clustered variables
%   newsum = data to be sepatated, one column for each cluster
%   fig = 'on' if you want to plot scatterplot of each cluster, original
%           and new data
%   path = where to find the output folder ('\output' must to be specified)
% 

%look for the original data
ff1=fullfile(path,'Clusters.mat');
ff2=fullfile(path,'Variables.mat');
load(ff2,'errore');

if strcmp(caso,'P2')==1
    load(ff1,'SameNameP_ok','ClusterFinP');
    tabClus=SameNameP_ok;
    dataClus=ClusterFinP;
    fun=2;
elseif strcmp(caso,'Q2')==1
    load(ff1,'SameNameQ_ok','ClusterFinQ');
    tabClus=SameNameQ_ok;
    dataClus=ClusterFinQ;
    fun=2;
elseif strcmp(caso,'P3')==1
    load(ff1,'BigClusP','BigSumP');
    tabClus=BigClusP;
    dataClus=BigSumP;
    fun=3;
elseif strcmp(caso,'Q3')==1
    load(ff1,'BigClusQ','BigSumQ');
    tabClus=BigClusQ;
    dataClus=BigSumQ;   
    fun=3;
end
if size(newsum,2)~=size(dataClus,2)
    fprintf('newsum has %i columns \n',size(newsum,2))
    fprintf('Cluster Table has %i clusters \n',size(dataClus,2))
    disp('They must be of same number!!')
    A=[];B=[];C=[];D=[];TF=[];
else
    if fun==2
        [A,B,C,TF]=ComeBack2(tabClus,dataClus,errore,newsum,fig,caso,path);
        D=[];
    else
        [A,B,D,C,TF]=ComeBack3(tabClus,dataClus,errore,newsum,fig,caso,path);
    end
end

end
function [A,B,Cmat,TF]=ComeBack2(tabClus,dataClus,errore,newsum,fig,caso,path)

TF=[];
A=zeros(size(newsum));
B=A;
Cmat=A;
numcl=size(newsum,2);
numel=size(newsum,1);
for cl=1:numcl
    a(1)=tabClus.VarPosition1(cl);
    a(2)=tabClus.VarPosition2(cl);
    vs=dataClus(:,cl);
    v1=errore(:,a(1));
    v2=errore(:,a(2));
    A3mat=zeros(numel,100);
    B3mat=zeros(numel,100);
    
    % 1) genero la nuova somma
    C=newsum(:,cl);
    
    % 2) conditioned mean and variance of A
    covac=cov(v1,vs,'omitrows');
    ccv1v2=corrcoef(v1,v2,'rows','pairwise');
    
    % se abs(corr(A,B))>=0.9 allora A=alfaC, B=(1-alfa)C
    if abs(ccv1v2(1,2))<=0.9
        mu_cc=mean(C-mean(C,'omitnan'),'omitnan');
        mu_ac=mean(v1,'omitnan')+covac(1,2)*mu_cc/covac(2,2);
        sigma_ac=covac(1,1)-covac(1,2)*covac(1,2)/covac(2,2);
        n1=gmdistribution(mu_ac,sigma_ac);
        % 3) compute A from randomly from normal distribution, B = C-A
        i=1;
        while i<101
            A3mat(:,i)= random(n1,numel);
            B3mat(:,i)=C-A3mat(:,i);
            i=i+1;
        end
        % select B3 with lowest std
        stdb3=std(B3mat,'omitnan');
        col=find(stdb3==min(stdb3));
        B3=B3mat(:,col);
        A3=A3mat(:,col);
    else
        rapp=abs(v1)./(abs(v1)+abs(v2));
        lrapp=length(rapp);
        ff=floor(numel/lrapp);
        mf=mod(numel,lrapp);
        rapp=[repmat(rapp,ff,1);rapp(1:mf)];
        A3=sign(ccv1v2(1,2))*rapp.*C;
        B3=C-A3;
    end
    % 4) checks
    ccab=corrcoef(A3,B3,'rows','pairwise');
    cov12=cov(v1,v2,'omitrows');
    cccc=cov(A3,B3,'omitrows');
    
    T1=table(...
        [mean(v1,'omitnan'); mean(A3,'omitnan')],...
        [mean(v2,'omitnan'); mean(B3,'omitnan')],...
        [cov12(1,1); cccc(1,1)], ...
        [cov12(2,2); cccc(2,2)], ...
        [cov12(1,2); cccc(1,2)], ...
        [sqrt(cov12(1,1)); sqrt(cccc(1,1))],...
        [sqrt(cov12(2,2)); sqrt(cccc(2,2))],...
        [ccv1v2(1,2); ccab(1,2)],...
        'VariableNames',...
        {'mean_A','meana_B','variance_A','variance_B','covar_AB','devstd_A','devstd_B','correl_AB'});
    
    T1.sumcov=[sum(sum(cov12));sum(sum(cccc))];
    T1.sumvarAB=(T1.variance_A)+(T1.variance_B);
    T1.sumstdAB=(T1.devstd_A)+(T1.devstd_B); 
    TF=[TF;T1];
    
    A(:,cl)=A3;
    B(:,cl)=B3;
    Cmat(:,cl)=C;
    
    if strcmp(fig,'on')
        figure
        scatterhist(A3,B3)
        str = sprintf('NEW %s and NEW %s', tabClus.VarName1{cl},tabClus.VarName2{cl});
        title(str)
        figure
        scatterhist(v1,v2)
        str = sprintf('ORIGINAL %s and ORIGINAL %s', tabClus.VarName1{cl},tabClus.VarName2{cl});
        title(str)
    end
end

str1=sprintf('fintab_NEW_%s.xls',caso);
f1=fullfile(path,str1);
writetable(TF,f1)
str2=sprintf('finalSep_NEW_%s.mat',caso);
f2=fullfile(path,str2);
save(f2,'TF','A','B','C');
clear A3mat B3mat A3 B3 a aa c3 c4 cl col rapp stdb3
end

function [A,B,D,Cmat,TF]=ComeBack3(tabClus,dataClus,errore,newsum,fig,caso,path)

TF=[];
A=zeros(size(newsum));
B=A;
D=A;
Cmat=A;
numcl=size(newsum,2);
numel=size(newsum,1);
for cl=1:numcl
    a(1)=tabClus.VarPosition1(cl);
    a(2)=tabClus.VarPosition2(cl);
    a(3)=tabClus.VarPosition3(cl);
    vs=dataClus(:,cl);
    v1=errore(:,a(1));
    v2=errore(:,a(2));
    v3=errore(:,a(3));

    % 1) select the sum to be divided
    C=newsum(:,cl);
    
    % 2) corelation
    ccv1v2v3=corrcoef([v1,v2,v3],'rows','pairwise');
    
    % 3) compute A and B, D=A-B
    r1=abs(v1)./(abs(v1)+abs(v2)+abs(v3));
    lr1=length(r1);
    ff=floor(numel/lr1);
    mf=mod(numel,lr1);
   
    r1=[repmat(r1,ff,1);r1(1:mf)];
    A3=sign(ccv1v2v3(1,3))*r1.*C;
    
    r2=abs(v2)./(abs(v1)+abs(v2)+abs(v3));
    r2=[repmat(r2,ff,1);r2(1:mf)];
    B3=sign(ccv1v2v3(2,3))*r2.*C;
 
    D3=C-A3-B3;
    
    % 4) checks
    ccabd=corrcoef([A3,B3,D3],'rows','pairwise');
    cov123=cov([v1,v2,v3],'omitrows');
    cccc=cov([A3,B3,D3],'omitrows');
    
    T1=table(...
        [mean(v1,'omitnan'); mean(A3,'omitnan')],...
        [mean(v2,'omitnan'); mean(B3,'omitnan')],...
        [mean(v3,'omitnan'); mean(D3,'omitnan')],...
        [cov123(1,1); cccc(1,1)], ...
        [cov123(2,2); cccc(2,2)], ...
        [cov123(3,3); cccc(3,3)], ...
        [cov123(1,2); cccc(1,2)], ...
        [cov123(1,3); cccc(1,3)], ...
        [cov123(2,3); cccc(2,3)], ...
        [sqrt(cov123(1,1)); sqrt(cccc(1,1))],...
        [sqrt(cov123(2,2)); sqrt(cccc(2,2))],...
        [sqrt(cov123(3,3)); sqrt(cccc(3,3))],...
        [ccv1v2v3(1,2); ccabd(1,2)],...
        [ccv1v2v3(1,3); ccabd(1,3)],...
        [ccv1v2v3(2,3); ccabd(2,3)],...
        'VariableNames',...
        {'mean_A','mean_B','mean_D',...
        'variance_A','variance_B','variance_D',...
        'covar_AB','covar_AD','covar_BD',...
        'devstd_A','devstd_B','devstd_D',...
        'correl_AB','correl_AD','correl_BD'});
    
    T1.sumcov=[sum(sum(cov123));sum(sum(cccc))];
    T1.sumvarABD=(T1.variance_A)+(T1.variance_B)+(T1.variance_D);
    T1.sumstdAB=(T1.devstd_A)+(T1.devstd_B)+(T1.devstd_D);
    TF=[TF;T1];
    
    A(:,cl)=A3;
    B(:,cl)=B3;
    D(:,cl)=D3;
    Cmat(:,cl)=C;
    
    if strcmp(fig,'on')
        figure
        scatter3(A3,B3,D3)
        title('new variables')
        str = sprintf('NEW %s and NEW %s and NEW %s', tabClus.VarName1{cl},tabClus.VarName2{cl},tabClus.VarName3{cl});
        title(str)
        figure
        scatter3(v1,v2,v3)
        str = sprintf('ORIGINAL %s and ORIGINAL %s and ORIGINAL %s', tabClus.VarName1{cl},tabClus.VarName2{cl},tabClus.VarName3{cl});
        title(str)
    end
end

str1=sprintf('fintab_NEW_%s.xls',caso);
f1=fullfile(path,str1);
if isempty(TF)~=1
    writetable(TF,f1)
    str2=sprintf('finalSep_NEW_%s.mat',caso);
    f2=fullfile(path,str2);
    save(f2,'TF','A','B','C');
else
    A=[]; B=[]; D=[];Cmat=[];
    disp('no clustered variables')
end
end
