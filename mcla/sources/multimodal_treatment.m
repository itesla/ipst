% 
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [CTG_table BIV_UU UNIV_DU] = multimodal_treatment(Y_mult,idx_err_mult,idx_fore_mult,tipovar)

tolzeroing=1e-3;
Nfract_nnz_lb = 0.3;
Nfract_fo_lb=0.1;
Nnan_fract = 0.5;

snap_filt0 = Y_mult(:,idx_err_mult);
forec_filt0 = Y_mult(:,idx_fore_mult);

discarded=[];prese=[];X1=[];X2=[];univariate=[];
nb=0;
% quali_multi0 = find(tipovar==2);

% quali_multi = find(ismember(idx_err_mult,quali_multi0));

for j = 1:length(idx_err_mult)
    
    non_nan = setdiff([1:size(snap_filt0,1)],union(find(isnan(forec_filt0(:,j))),find(isnan(snap_filt0(:,j)))));
    snap_filt=snap_filt0(non_nan,:);
    forec_filt=forec_filt0(non_nan,:);
    if length(non_nan) < Nnan_fract*size(snap_filt0,1)
        discarded = [discarded j];
        
    else
        prese = [prese j];
        try
            Q(:,1) = (abs([forec_filt(:,(j)) ]) > tolzeroing );
            Q(:,2) = (abs([ snap_filt(:,(j))]) > tolzeroing );
        catch err
            keyboard
        end
        Y=[forec_filt(:,(j))  snap_filt(:,(j)) ];
        idxUU = find(ismember(Q,[1 1],'rows'));
        idxUD = find(ismember(Q,[1 0],'rows'));
        idxDU = find(ismember(Q,[0 1],'rows'));
        idxDD = find(ismember(Q,[0 0],'rows'));
        CTG_table(j,:,:) = [length(idxUU)/size(snap_filt,1) length(idxUD)/size(snap_filt,1) ; length(idxDU)/size(snap_filt,1) length(idxDD)/size(snap_filt,1)];
        if length(idxUU)>=max(30,Nfract_nnz_lb*size(Y,1)) && min(var(Y(idxUU,:),0,1))>0.01
            % estimate univariate distribution
            try
                if length(idxDU) >= max(30,Nfract_fo_lb*size(Y,1))
                UNIV_DU(j,:) = quantile(Y(idxDU,2),[0.05 0.25 0.5 0.75 0.95]);
                univariate=[univariate j];
                else
                disp('*** too few samples for univariate model with FO=0 ***')
                UNIV_DU(j,:) = quantile(Y(idxDU,2),[0.05 0.25 0.5 0.75 0.95]);    
                end
            catch err
                keyboard
            end
              % estimate bivariate model
            
            k=1;
            
            obj = gmdistribution.fit(Y(idxUU,:),k,'Replicates',3,'Regularize',1);
            
            AIC(k)=obj.AIC;
            param1{k}=obj;
            convergenza =obj.Converged;
            while convergenza == 1
                k=k+1;
                obj=[];
                try
                    obj = gmdistribution.fit(Y(idxUU,:),k,'Replicates',3);
                catch err
                    convergenza =0;
                    %           keyboard
                end
                if isempty(obj)==0
                    convergenza = obj.Converged;
                    AIC(k)=obj.AIC;
                    param1{k}=obj;
                 
                    
                end
                
            end
            idx = find(AIC==min(AIC));
            BIV_UU{j} = param1{idx(1)};
            clear AIC param1 k obj
%           
            X1 = [X1 j];
        else
            UNIV_DU(j,:) = [0 0 0 0  0];
            if length(idxUU) >= 30 && min(var(Y(idxUU,:),0,1))>0.01
                disp('*** ESTIMATE BIVARIATE WITH 30 samples ***')
                 k=1;
            
            obj = gmdistribution.fit(Y(idxUU,:),k,'Replicates',3,'Regularize',1);
            
            AIC(k)=obj.AIC;
            param1{k}=obj;
            convergenza =obj.Converged;
            while convergenza == 1
                k=k+1;
                obj=[];
                try
                    obj = gmdistribution.fit(Y(idxUU,:),k,'Replicates',3);
                catch err
                    convergenza =0;
                    %           keyboard
                end
                if isempty(obj)==0
                    convergenza = obj.Converged;
                    AIC(k)=obj.AIC;
                    param1{k}=obj;
                 
                    
                end
                
            end
            idx = find(AIC==min(AIC));
            BIV_UU{j} = param1{idx(1)};
             clear AIC param1 k obj
            else
                k=1;
            BIV_UU{j} =  gmdistribution.fit(Y(idxUU,:)+0.05*randn(size(Y(idxUU,:))),k,'Replicates',3);   
            end
            X2 = [X2 j];
        end
        clear Q
    end
end
