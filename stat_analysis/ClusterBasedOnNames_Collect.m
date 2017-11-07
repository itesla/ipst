function [SameNameP_ok,ClusterFinP,ReplicatedP,BigSumP,BigClusP,BadClusterP,BigBADP,...
    SameNameQ_ok,ClusterFinQ,ReplicatedQ,BigSumQ,BigClusQ,BadClusterQ,BigBADQ]...
     = ClusterBasedOnNames_Collect(ID_in,errore,multimodal,filepath)
 %
 % The function clusters the variables, divided in P and Q, with same 8
 % first characters in the name, and whos sum is unimodal
 %
 % INPUT
 %  ID_in = names of variables
 %  errore = matrix of data to clusterize
 %  multimodal = array with the position of multimodal variables
 %  filepath = path where to put the output
 % OUTPUT (X is P or Q)
 %  SameNameX_ok = table with 2 clustered variables
 %  ClusterFinX = matrix of sums of 2 clustered variable 
 %  ReplicatedX = table with variables equal for at most 97%
 %  BigSumX = table  with 3 clustered variables
 %  BigClusX = matrix of sum of 3 clustered variable
 %  BadClusterX = table with clustered variables whose sum isn't unimodal
 %  BigBADX = matrix with sums not unimodal
 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% P
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% 1 --> P
% 2 --> Q

idin=table([1:length(ID_in)]',ID_in');
Q=regexp(idin.Var2,'Q$','match');
for j=1:length(Q)
    if isempty(Q{j})~=0
        idin.Var3(j)={'P'};
    else
        idin.Var3(j)={'Q'};
    end
end
f=findgroups(idin.Var3);
idin=idin(f==1,:);

[SameNameP_ok,ClusterFinP,ReplicatedP,BigSumP,BigClusP,BadClusterP,BigBADP]...
    = ClusterBasedOnNames_OneByOne(idin,errore,multimodal);

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Q
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

idin=table([1:length(ID_in)]',ID_in');
Q=regexp(idin.Var2,'Q$','match');
for j=1:length(Q)
    if isempty(Q{j})~=0
        idin.Var3(j)={'P'};
    else
        idin.Var3(j)={'Q'};
    end
end
f=findgroups(idin.Var3);
idin=idin(f==2,:);
if isempty(idin)==0
    [SameNameQ_ok,ClusterFinQ,ReplicatedQ,BigSumQ,BigClusQ,BadClusterQ,BigBADQ]...
        = ClusterBasedOnNames_OneByOne(idin,errore,multimodal);
else
    SameNameQ_ok = [];
    ClusterFinQ = [];
    ReplicatedQ = [];
    BigSumQ = [];
    BigClusQ = [];
    BadClusterQ=[];
    BigBADQ=[];
end


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% WRITES RESUTS 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
name='Clusters.mat';
filename=fullfile(filepath,name);
save(filename,...
      'SameNameQ_ok','ClusterFinQ','ReplicatedQ','BigSumQ','BigClusQ','BadClusterQ','BigBADQ',...
      'SameNameP_ok','ClusterFinP','ReplicatedP','BigSumP','BigClusP','BadClusterP','BigBADP')
end

