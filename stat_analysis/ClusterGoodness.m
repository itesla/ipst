function [Clok]=ClusterGoodness(cluster_matrix)

% The function check if the sums of the clustered variables are unimodal or not.
%
% INPUT
% cluster_matrix = matrix whos columns are the sum of the clustered
%                   variables, one column for each cluster.
%
% OUTPUT
% Clok(i)= 1 if it's unimodal, 0 elsewhere

warning('off');
Clok=zeros(1,size(cluster_matrix,2));

for i=1:size(cluster_matrix,2)
    % computes the number of peaks
    [picchi,~]=findpeaks2(cluster_matrix(:,i),'no','no');
    if picchi>1
        bic_cl=1e+10*ones(1,picchi);
        clx=cell(1,picchi);
        for k=1:picchi
            % computes the Gaussian mixture
            va=cluster_matrix(~isnan(cluster_matrix(:,i)),i);
            try
                s=k;
                rng(1000);
                clx{k}=fitgmdist(va,k,'Replicates',5);
            catch
                try
                    s=k-1;
                    rng(2000);
                    clx{k}=fitgmdist(va,s,'Replicates',5);
                catch
                    s=k-2;
                    rng(2000);
                    clx{k}=fitgmdist(va,s,'Replicates',5);
                end
            end
            bic_cl(s)=clx{k}.BIC;
        end
        [bicmin_gm,pos]=min(bic_cl);

        if pos==1
            Clok(i)=1;
        else
            %looks for a good unimodal distribution
            [bicmin_uni,~,~] = TestDistrib(cluster_matrix(:,i),'no');
            %comparison between gaussian mixture and unimodal distribution
            if bicmin_gm<bicmin_uni
                % check bimodal test
                [A,D]=BimodalTest(clx,pos);
                if (A<0 || D<2)
                    Clok(i)=1;
                end
            else
                Clok(i)=1;
            end
        end
    else
        Clok(i)=1;
    end
end
