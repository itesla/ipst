% 
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%% m1file - sampling network data, to be read
%% m2file - FEA output file (output from module1 and module2), to be read
%% ofile  - module3 output file, to be written
%% s_scenarios - number of samples to generate (in ofile)
%% homoths - homothethic disaggrgation option (1=active, 0=deactivated)
%% s_rng_seed - int seed (optional, default is 'shuffle' on current date)
function exitcode=FPF_HELPER(m1file, m2file, ofile,isdeterministics,isuniforms,homoths,opf_FPFs, s_rng_seed)
close all;
mversion='1.8.2';
disp(sprintf('wp5 - FEA STATS FOR FPF - version: %s', mversion));
disp(sprintf(' m1file:  %s',m1file));
disp(sprintf(' m2file:  %s',m2file));
disp(sprintf(' ofile:  %s', ofile));

isdeterministic=str2double(isdeterministics);
isuniform=str2double(isuniforms);
homoth = str2num(homoths);
opf_FPF = str2num(opf_FPFs);

moutput.errmsg='Ok';
try
if opf_FPF==1
    
        
        % module1: struct, output from module1
        load(m1file);
        % module2:  module2 output
        load(m2file);
        % s_scenarios: number of samples to generate
        if isdeterministic == 0
            %if seed is not specified, 'shuffle'  on current platform time
            if nargin < 8
                rng('shuffle','twister');
                disp(sprintf(' rng seed:  not specified, default is shuffle on current platform time'));
            else
                rng(str2double(s_rng_seed),'twister');
                disp(sprintf(' rng seed:  %s',s_rng_seed));
            end
            
            disp(sprintf('flagPQ:  %u', out(1).flagPQ));
            disp(['preprocessing: type_x, etc.'])
            tic;
            
            % type_X is the vector which specifies the nature of the stochastic
            % injections (RES or load). here is an example with 3 RES and one stochastic load. the vector must be
            % completed taking information from IIDM.
            
            % flagPQ: =0 se ci sono solo P stocastiche, =1 se anche le Q sono
            % stocastiche
            %%%%%
            inj_print = {};
            m_print=[];
            std_print=[];
            
            
            
            
            for iout = 1:length(out)
                dati_FPF = out(iout).dati_FPF;
                module1 = out(iout).module1;
                flagPQ = out(iout).flagPQ;
                type_X=[];y0=[];
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                idx_errA = dati_FPF.idx_errA;
                idx_foreA = dati_FPF.idx_foreA;
                inj_ID0 = dati_FPF.inj_ID;
                nat_ID0 = dati_FPF.nat_ID;
                
                if homoth == 1
                    nations = unique(nat_ID0);
                    for ina = 1:length(nations)
                        idxg = find(ismember({generatore.nation},nations{ina}));
                        idxc = find(ismember({carico.nation},nations{ina}));
                        inj_ids = find(ismember(nat_ID0,nations{ina}));
                        carico1(ina).P = sum([generatore(idxg).P]) - sum([carico(idxc).P]);
                        carico1(ina).Q = sum([generatore(idxg).Q]) - sum([carico(idxc).Q]);
                        carico1(ina).codice = inj_ID0{inj_ids(1)}(1:end-2);
                        carico1(ina).conn=1;
                    end
                    generatore1=generatore;
                else
                    carico1 = carico;
                    generatore1 = generatore;
                    nations = [];
                end
                
                
                
                type_X=zeros(2,length(inj_ID0));
                if flagPQ == 0
                    for jcol = 1:size(inj_ID0,2)
                        idxgen = find(ismember({generatore1.codice},inj_ID0{jcol}(1:end-2)));
                        idxload = find(ismember({carico1.codice},inj_ID0{jcol}(1:end-2)));
                        if isempty(idxgen)==0
                            if generatore1(idxgen(1)).conn == 1
                                if strcmp(inj_ID0{jcol}(end),'P')
                                    type_X(:,jcol) = [1;idxgen(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = generatore1(idxgen(1)).P;
                                    end
                                else
                                    type_X(:,jcol) = [4;idxgen(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = generatore1(idxgen(1)).Q;
                                    end
                                end
                                
                            end
                        end
                        if isempty(idxload)==0
                            if carico1(idxload(1)).conn == 1 %&& carico1(idxload(1)).P ~= 0 && carico1(idxload(1)).Q ~= 0
                                type_X(:,jcol) = [2;idxload(1)];
                                if ismember(jcol,idx_foreA)
                                    y0(find(idx_foreA==jcol)) = carico1(idxload(1)).P;
                                end
                                
                            end
                        end
                    end
                else
                    for jcol = 1:size(inj_ID0,2)
                        idxgen = find(ismember({generatore1.codice},inj_ID0{jcol}(1:end-2)));
                        idxload = find(ismember({carico1.codice},inj_ID0{jcol}(1:end-2)));
                        if isempty(idxgen)==0
                            if generatore1(idxgen(1)).conn == 1
                                if strcmp(inj_ID0{jcol}(end),'P')
                                    type_X(:,jcol) = [1;idxgen(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = generatore1(idxgen(1)).P;
                                    end
                                else
                                    type_X(:,jcol) = [4;idxgen(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = generatore1(idxgen(1)).Q;
                                    end
                                end
                            end
                        end
                        if isempty(idxload)==0
                            if carico1(idxload(1)).conn == 1
                                if strcmp(inj_ID0{jcol}(end),'P')
                                    type_X(:,jcol) = [2;idxload(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = carico1(idxload(1)).P;
                                    end
                                else
                                    type_X(:,jcol) = [3;idxload(1)];
                                    if ismember(jcol,idx_foreA)
                                        y0(find(idx_foreA==jcol)) = carico1(idxload(1)).Q;
                                    end
                                end
                            end
                        end
                    end
                end
                
                idx_miss = find(~any(type_X,1));
                idx_available = setdiff([1:size(type_X,2)],idx_miss);
                type_X(4,idx_foreA(ismember(idx_foreA,idx_available)))=-1;
                type_X(4,idx_errA(ismember(idx_errA,idx_available)))=1;
                type_X(3,:) = zeros(1,size(type_X,2));
                type_X(3,idx_available) = [1];
                
                are_forerrors = intersect(find(type_X(4,:)==1),find(type_X(3,:)==1));
                inj_ID0C = inj_ID0(idx_errA(find(ismember(idx_errA,are_forerrors))));
                
                %%%%%%
                % save fpf_test.mat
                
                if isempty(module1) == 0
                    
                    %%%%% conditional sampling for forecast errors accounting for the data
                    %%%%% stored in dati_FPF
                    YFPF = dati_FPF.YFPF;
                    matrice = dati_FPF.matrice_yy;
                    y1 = (matrice*y0')';
                    
                    invcorr_yy2x = dati_FPF.invmatr_corryy;
                    
                    YFPF_ER= YFPF(:,are_forerrors);
                    YFPF_FO0= YFPF(:,idx_foreA);
                    
                    idx_err1 = dati_FPF.idx_err;
                    idx_err=idx_err1(find(ismember(idx_errA,are_forerrors)));
                    idx_fore = dati_FPF.idx_fore;
                    
                    YFPF_FO= YFPF_FO0*matrice';
                    
                    YFPF1(:,idx_err1(find(ismember(idx_errA,are_forerrors)))) = YFPF_ER;
                    YFPF1(:,idx_fore) = YFPF_FO;
                    
                    %%% CONDITIONAL MEANS AND STD DEVS
                    m_e = nanmean(YFPF_ER,1);
                    m_y = nanmean(YFPF_FO,1);
                    std_e = std(YFPF_ER,0,1);
                    std_y = std(YFPF_FO,0,1);
                    
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    for iy = 1:size(YFPF_FO,2)
                        FO(:,iy) = (YFPF_FO(:,iy) - m_y(iy))/std_y(iy);
                    end
                    for ie = 1:size(YFPF_ER,2)
                        ERN(:,ie) = (YFPF_ER(:,ie) - m_e(ie))/std_e(ie);
                    end
                    outoflimit = [];tol=10/size(YFPF_ER,1);
                    % NATAF TRANSFORMATION FOR THE FORECASTS
                    for iy = 1:size(YFPF_FO,2)
                        %     yp = y0(iy);
                        [CDF_Y{iy} xfo{iy}] = ecdf(FO(:,iy));
                        A = [xfo{iy},CDF_Y{iy}];
                        
                        [dummy1, uniq,dummy3]=unique(A(:,1));
                        
                        CDF_Y_NEW1(:,iy) = max(tol,min(1-tol,interp1(A(uniq,1),A(uniq,2),FO(:,iy),'linear','extrap')));
                        INVG_Y_NEW1(:,iy) = norminv(CDF_Y_NEW1(:,iy),0,1);
                        cdf_yp = max(tol,min(1-tol,interp1(A(uniq,1),A(uniq,2),(y1(iy) - m_y(iy))/std_y(iy),'linear','extrap')));
                        if cdf_yp == tol || cdf_yp == 1-tol
                            outoflimit = [outoflimit iy];
                        end
                        yp = norminv(cdf_yp,0,1);
                        yyo(iy)=yp;
                        dy(:,iy) = yp - INVG_Y_NEW1(:,iy);
                    end
                    
                    % NATAF TRANSFORMATION FOR THE ERRORS
                    for ie = 1:length(idx_err)
                        
                        [CDF_ER{ie} xsn{ie}] = ecdf(ERN(:,ie));
                        
                        A = [xsn{ie},CDF_ER{ie}];
                        
                        [dummy1, uniq,dummy3]=unique(A(:,1));
                        
                        CDF_E_NEW1(:,ie) =  max(tol,min(1-tol,interp1(A(uniq,1),A(uniq,2),ERN(:,ie),'linear','extrap')));
                        INVG_E_NEW1(:,ie) = norminv(CDF_E_NEW1(:,ie),0,1);
                    end
                    
                    X_NEW2S(:,idx_err)=INVG_E_NEW1;
                    X_NEW2S(:,idx_fore)=INVG_Y_NEW1;
                    
                    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                    cov_tot = cov(X_NEW2S);
                    
                    cov_yy2 = cov_tot(idx_fore,idx_fore);
                    cov_ee2 = cov_tot(idx_err,idx_err);
                    cov_ey2 = cov_tot(idx_err,idx_fore);
                    
                    corr_yy2 = (( diag(diag(cov_yy2)))^(-0.5))*cov_yy2*(( diag(diag(cov_yy2)))^(-0.5));
                    corr_ey2 = (( diag(diag(cov_ee2)))^(-0.5))*cov_ey2*(( diag(diag(cov_yy2)))^(-0.5));
                    corr_ee2 = (( diag(diag(cov_ee2)))^(-0.5))*cov_ee2*(( diag(diag(cov_ee2)))^(-0.5));
                    %
                    B1 = inv(( diag(diag(cov_yy2)))^(0.5))*invcorr_yy2x*inv((( diag(diag(cov_yy2)))^(0.5)));
                    
                    mm_y = mean(X_NEW2S(:,idx_fore),1);
                    mm_e = mean(X_NEW2S(:,idx_err),1);
                    
                    mm_ec = mm_e' + cov_ey2*B1*(yyo - mm_y)';
                    SIG = cov_ee2 - cov_ey2*B1*cov_ey2';
                    
                    SIG = closest_corr(SIG);
                    SIG = real(SIG);
                    
                    % % add a small eps to main diagonal in case det=0
                    % %
                    % ntempt = 0;
                    % while det(SIG)==0 & ntempt < 100
                    % SIG_FIL = SI+1e-4*eye(size(SIG_FIL,1));
                    % ntempt = ntempt+1;
                    % end
                    if det(SIG) > 0
                        snap_new1G = mvnrnd(mm_ec,(SIG+SIG')/2,1000);
                    else
                        snap_new1G = mvnrnd(mm_ec,diag(diag((SIG+SIG')/2)),1000);
                    end
                    for ie = 1:length(idx_err)
                        A = [CDF_ER{ie},xsn{ie}];
                        [dummy1, uniq,dummy3]=unique(A(:,1));
                        
                        CDF_EG_NEW1(:,ie) =  max(tol,min(1-tol,normcdf(snap_new1G(:,ie),0,1)));
                        snap_new1(:,ie) = m_e(ie) + std_e(ie)*interp1(A(uniq,1),A(uniq,2),CDF_EG_NEW1(:,ie),'linear','extrap');
                    end
                    
                    m_ec = mean(snap_new1,1)';
                    std_ec = std(snap_new1,0,1)';
                    
                else
                    
                    if isuniform==0
                        %%%% MODALITY GAUSSIAN ACTIVATED
                        module2 = out(iout).module2;
                        m_ec = zeros(length(are_forerrors),1);
                        idx_loads = find(ismember(type_X(1,:),[2 3]));
                        idx_RES = find(ismember(type_X(1,:),[1 4]));
                        if  isfield(module2.allparas,'stddev')==0 % working with multimodals
                            stddevs(intersect(idx_loads,are_forerrors))=0.05;
                            stddevs(intersect(idx_RES,are_forerrors))=0.15;
                        else % working with gaussian forecast models
                            stddevs(intersect(idx_loads,are_forerrors))=module2.allparas.stddev(1);
                            stddevs(intersect(idx_RES,are_forerrors))=module2.allparas.stddev(2);
                        end
                        std_ec = stddevs(find(ismember(idx_errA,are_forerrors)))'.*max(1e-6,abs(y0(find(ismember(idx_errA,are_forerrors))))');
                    else
                        %%%% MODALITY UNIFORM ACTIVATED
                        module2 = out(iout).module2;
                        m_ec = zeros(length(are_forerrors),1);
                        idx_loadsP = find(ismember(type_X(1,:),[2]));
                        idx_loadsQ = find(ismember(type_X(1,:),[3]));
                        
                        idx_RES = find(ismember(type_X(1,:),[1 4]));
                        stddevs(intersect(idx_loadsP,are_forerrors))=(1/12)*(2*module2.allparas.band_unif(1))^2;%module2.allparas.stddev(1);
                        stddevs(intersect(idx_loadsQ,are_forerrors))=(1/12)*(2*module2.allparas.band_unif(2))^2;%module2.allparas.stddev(1);
                        stddevs(intersect(idx_RES,are_forerrors))=(1/12)*(2*module2.allparas.band_unif(3))^2;%module2.allparas.stddev(2);
                        std_ec = stddevs(find(ismember(idx_errA,are_forerrors)))'.*max(1e-6,abs(y0(find(ismember(idx_errA,are_forerrors))))');
                    end
                    
                end
                
                if homoth == 1
                    inj_ID1C={};
                    nations = unique({carico.nation});
                    for ina=1:length(nations)
                        quali_car = intersect(find([carico.conn]==1),find(ismember({carico.nation},nations{ina})));
                        
                        if isempty(quali_car)==0
                            mP(quali_car) = [carico(quali_car).P] + m_ec(ismember(idx_errA,intersect(find(type_X(1,:)==2),find(ismember(out(iout).dati_FPF.nat_ID,nations{ina}))))).*abs([carico(quali_car).P])./sum(abs([carico(quali_car).P]));
                            sP(quali_car) = {carico(quali_car).codice};
                            std_P(quali_car) = std_ec(ismember(idx_errA,intersect(find(type_X(1,:)==2),find(ismember(out(iout).dati_FPF.nat_ID,nations{ina}))))).*abs([carico(quali_car).P])./sum(abs([carico(quali_car).P]));
                            if flagPQ==1
                                std_Q(quali_car) = std_ec(ismember(idx_errA,intersect(find(type_X(1,:)==3),find(ismember(out(iout).dati_FPF.nat_ID,nations{ina}))))).*abs([carico(quali_car).P])./sum(abs([carico(quali_car).P]));
                                mQ(quali_car) = [carico(quali_car).P] + m_ec(ismember(idx_errA,intersect(find(type_X(1,:)==3),find(ismember(out(iout).dati_FPF.nat_ID,nations{ina}))))).*abs([carico(quali_car).P])./sum(abs([carico(quali_car).P]));
                                
                                sQ(quali_car) = {carico(quali_car).codice};
                            else
                                tanfi0 = [carico(quali_car).Q]./[carico(quali_car).P];
                                idxnaninf = union(find(isnan(tanfi0)),find(isinf(tanfi0)));
                                tanfi0(idxnaninf)=0;
                                std_Q(quali_car) = std_P(quali_car).*abs(tanfi0);
                                mQ(quali_car) = mP(quali_car).*tanfi0;
                                sQ(quali_car) = {carico(quali_car).codice};
                            end
                        end
                    end
                    m_ec = zeros(1,length(mP)*2);
                    std_ec= zeros(1,length(mP)*2);
                    inj_ID0C=cell(1,length(sP)*2);
                    m_ec(1:2:end)=mP;
                    m_ec(2:2:end)=mQ;
                    std_ec(1:2:end)=std_P;
                    std_ec(2:2:end)=std_Q;
                    inj_ID0C(1:2:end)=sP;
                    inj_ID0C(2:2:end)=sQ;
                    
                end
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                % here ends the  RSE CODE, extracted from TEST_MCLA.m
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                
                disp(['FPF COMPUTATION COMPLETED.'])
                toc;
                
                
                moutput(iout).errmsg='Ok';
                moutput(iout).m_ec=m_ec;
                moutput(iout).std_ec=std_ec;
                moutput(iout).inj_ID=inj_ID0C;%(:,idx_errA);
                moutput(iout).rng_data=out(iout).rng_data;
                moutput(iout).mversion=out(iout).mversion;
                inj_print = [inj_print inj_ID0C];
                m_print = [m_print m_ec'];
                std_print = [std_print std_ec'];
                clear m_ec std_ec inj_ID0C y0 type_X module1 module2
                
                %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                totmoutput.outcomeFPF(iout) = moutput(iout);
                
            end
            
            %save output in .mat
            csvFileName=sprintf('%s.csv',[ofile]);
            fid = fopen(csvFileName,'w');
            fmtString = [repmat('%s,',1,size(inj_print,2)-1),'%s\n'];
            fprintf(fid,fmtString,inj_print{:});
            fclose(fid);
            dlmwrite(csvFileName,m_print,'delimiter',',','-append');
            dlmwrite(csvFileName,std_print,'delimiter',',','-append');
        else
            moutput(1).errmsg='det';
            moutput(1).m_ec=[];
            moutput(1).std_ec=[];
            moutput(1).inj_ID=[];%(:,idx_errA);
            moutput(1).rng_data=out(1).rng_data;
            moutput(1).mversion=out(1).mversion;
            totmoutput.outcomeFPF(1) = moutput(1);
        end
        else
            moutput(1).errmsg='null';
            moutput(1).m_ec=[];
            moutput(1).std_ec=[];
            moutput(1).inj_ID=[];%(:,idx_errA);
            moutput(1).rng_data=[];
            moutput(1).mversion=[];
            totmoutput.outcomeFPF(1) = moutput(1);
    end
    exitcode=0;
    catch err
        moutput(1).errmsg=err.message;
        disp(getReport(err,'extended'));
        exitcode=-1;
end
save(ofile, '-struct', 'totmoutput');
% dump inj_ID header, means and std deviations values to a csv file

end
