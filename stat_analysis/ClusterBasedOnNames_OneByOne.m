function [SameName_ok,ClusterFin,Replicated,BigSum,BigClus,BadCluster,BigBAD]...
    = ClusterBasedOnNames_OneByOne(idin,errore,multimodal)
%ClusterBasedOnNames_OneByOne(ID_in,errore,multimodal,type)

% the function generates the clusters composed by variables with the same
% name, and then it sum their values in order to obtain a multimodal
% result.
%
% INPUT:
% ID_in = name of variables
% errore = matrix error under analysis
% multimodal = struct with the position of the multimodal variables
% type = 'P' or 'Q'
%
% OUTPUT
% SameName_ok = table with the cluster of pairs of variables with same
%               name, that generates unimodal sums
% ClusterFin = matrix of the sum of the clustered (paired) variables
% Replicated = pairs of variables that differ less than 1 MW (Mvar)
%              for less than 3% between them
% BigClus = table with the cluster of three variables with same
%               name, that generates unimodal sums
% BigSum = matrix of the sum of each cluster of three variables
% BadCluster = table with clusters of 2 variables, whos sum is multimodal (rejected)
% BigBAD = table with clusters of 3 variables, whos sum is multimodal (rejected)

%%
SameName_ok=[];
ClusterFin=[];
Replicated=[];
BigSum=[];
BigClus=table();
BadCluster=[];
BigBAD=[];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% GROUPING VARIABLES WITH SAME NAME
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
SameName=table(0,{' '},0,{' '},'VariableNames',{'VarPosition1','VarName1','VarPosition2','VarName2'});
a=SameName;
for i=1:(size(idin,1)-1)
    cft=strncmp(idin.Var2(i),idin.Var2(i+1),8);
    if cft==1
        a.VarPosition1=idin.Var1(i);
        a.VarPosition2=idin.Var1(i+1);
        a.VarName1=idin.Var2(i);
        a.VarName2=idin.Var2(i+1);
        SameName=[SameName;a];
    end
end

SameName(1,:)=[];

% Select only multimodal variables.
v1=ismember(SameName.VarPosition1,multimodal);
v2=ismember(SameName.VarPosition2,multimodal);
v=v1+v2;
SameName=SameName(v==2,:);

%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% SEPARATION OF THE REPLICATED VARIABLES
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
lim=ceil(size(SameName.VarPosition1,1)*3/100);
if lim>0
    Repl=errore(:,SameName.VarPosition1)-errore(:,SameName.VarPosition2);
    [~,col_ug]=find(max(Repl(lim:end,:))<1);
    Replicated=SameName(col_ug,:);
    SameName=SameName(setdiff(1:size(Repl,2),col_ug),:);
end

%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% SELECT CLUSTERS COMPOSED BY THREE VARIABLES AND SUM
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % 1) we select the variables that appears in two different clusters
    % 2) we generate a new cluster composed by three variables
    % 3) we sum these three variables in order to obtain a new variable
  
inter=intersect(SameName.VarPosition1,SameName.VarPosition2);
if isempty(inter)==0
    for iii=1:length(inter)
        a=SameName(SameName.VarPosition2==inter(iii),1:4);
        a.VarPosition3=SameName.VarPosition2(SameName.VarPosition1==inter(iii));
        a.VarName3=SameName.VarName2(SameName.VarPosition1==inter(iii));
        BigClus(iii,:)=a;
    end
    BigSum=errore(:,BigClus.VarPosition1)+errore(:,BigClus.VarPosition2)+errore(:,BigClus.VarPosition3);
    SameName(ismember(SameName.VarPosition1,inter),:)=[];
    SameName(ismember(SameName.VarPosition2,inter),:)=[];
else 
    BigSum=[];
    BigClus=[];
end

% we conserve only the clusters that generates unimodal sum (goodness of
% new variables)
Clok=ClusterGoodness(BigSum);

BigSum=BigSum(:,Clok==1);
BigBAD=BigClus(Clok==0,:);
BigClus=BigClus(Clok==1,:);


%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% GENERATE NEW VARIABLES (SUM OF THE TWO ORIGINAL ONES)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% We sum the variables with same name but not replicated
ClusterFin=errore(:,SameName.VarPosition1)+errore(:,SameName.VarPosition2);

% Check of he cluster: it is ok if it is unimodal
[Clok]=ClusterGoodness(ClusterFin);
 SameName.Goodness=Clok';

% select only the unimodal resulting variables
BadCluster=SameName(SameName.Goodness==0,:);
SameName_ok=SameName(SameName.Goodness==1,:);
ClusterFin=ClusterFin(:,Clok==1);
end