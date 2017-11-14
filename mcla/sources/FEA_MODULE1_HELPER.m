%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% UPDATES June-July 2017
% 1) inserted the options for a separate sampling of unimodal and
% multimodal variables (option "unimodd")
% 2) added the option "opt_GUI" to decide if data for GUI must be stored
% for subsequent visualization or not
% 3) added the function "find_unimodals" to separate unimodal from
% multimodal injections
% 4) inserted an option to choose the mode for inverting the forecast
% correlation matrix ("modo_inv" option)
% 5) added new data structure to store data for multimodal variables
% 6) added the switch -v7.3 to save large data file .mat 
% 7) added "uniform distribution" and "deterministic" options for forecast error modeling
% UPDATE Sept-Oct 2017:
% 1) added aggregation of Pen Pload Qgen and Qload variables into nation
% cross-border powerflows for homothetic disaggregation option
% 2) assure consistency in FPF and GUI output files
% s_rng_seed - int seed (optional, default is 'shuffle' on current date)
function exitcode=FEA_MODULE1_HELPER(ifile, ofile,natS,ofile_forFPF,ofileGUI, IRs, Ks, s_flagPQ,s_method,tolvar,Nmin_obs_fract,Nnz,Nmin_obs_interv,outliers,koutlier,imputation_meth,Ngaussians,percentile_historical,check_module0,toleranceS,iterationsS,epsiloS,conditional_samplingS,histo_estremeQs,thresGUIs,unimod,modo_invs,isdeterministics,isuniforms,opt_GUIs,opt_FPFs,homoths,s_rng_seed)

close all;
mversion='1.8.2';
disp(sprintf('wp5 - module1 - version: %s', mversion));
disp(sprintf(' ifile:  %s',ifile));
disp(sprintf(' ofile:  %s',ofile));
disp(sprintf(' nat:  %s',natS));
disp(sprintf(' IR:  %s',IRs));
disp(sprintf(' K:  %s',Ks));
disp(sprintf(' flagPQ:  %s',s_flagPQ));
disp(sprintf(' method:  %s',s_method));
disp(sprintf(' tolvar:  %s',tolvar));
disp(sprintf(' Nmin_obs_fract:  %s',Nmin_obs_fract));
disp(sprintf(' Nmin_non_zeros_fract:  %s',Nnz));
disp(sprintf(' Nmin_obs_interv:  %s',Nmin_obs_interv));
disp(sprintf(' outliers:  %s',outliers));
disp(sprintf(' n for n-sigma rule to filter out outliers:  %s',koutlier));
disp(sprintf(' imputation_meth:  %s',imputation_meth));
disp(sprintf(' Ngaussians:  %s',Ngaussians));
disp(sprintf(' percentile_historical:  %s',percentile_historical));
disp(sprintf(' check_module0:  %s',check_module0));
disp(sprintf(' tolerance:  %s',toleranceS));
disp(sprintf(' iterations:  %s', iterationsS));
disp(sprintf(' epsilo:  %s', epsiloS));
disp(sprintf(' conditional_sampling:  %s', conditional_samplingS));
disp(sprintf(' histo_estremeQ:  %s', histo_estremeQs));
disp(sprintf(' thresGUI:  %s', thresGUIs));
disp(sprintf(' unimod:  %s', unimod));
disp(sprintf(' mode of inversion:  %s', modo_invs));
disp(sprintf(' isdeterministics:  %s', isdeterministics));
disp(sprintf(' isuniforms:  %s', isuniforms));
disp(sprintf(' opt_GUIs:  %s', opt_GUIs));
disp(sprintf(' opt_FPFs:  %s', opt_FPFs));
disp(sprintf(' homoths:  %s', homoths));


modo_inv=str2double(modo_invs);
par_tolvar = str2double(tolvar);% minimum variance of imput variables, in MW
par_Nmin_obs_fract = str2double(Nmin_obs_fract);% minim fraction of valid samples for each stochastic variables
par_nnz = str2double(Nnz);%
par_Nmin_obs_interv = str2double(Nmin_obs_interv);% min nr of samples in common for each pair of variables
par_imputation_meth = str2double(imputation_meth); % type of inputation techniques for gaussian mixture: 2= maximum likelihood, 1 = highest prob. gaussian
unimodd = str2double(unimod);
moutput.errmsg='Ok';
par_outliers = str2double(outliers);% 0 = outliers are included as valid samples, 1 = outliers are excluded
par_imputation_meth = str2double(imputation_meth); % type of inputation techniques for gaussian mixture: 2= maximum likelihood, 1 = highest prob. gaussian
par_Ngaussians = str2double(Ngaussians); % nr of gaussians of the mixture
percentil = str2double(percentile_historical); % percentage of quantile to identify min and max Q values for Q samples in case flagPQ = 0
check_mod0 = str2double(check_module0); % if 1, calculate some performance metrics
Koutliers = str2double(koutlier); % multiplier of std dev in n sigma rule
tolerance = str2double(toleranceS); % accuracy in correlation matrix inversion
iterations = str2double(iterationsS); % max nr of iterations for matrix inversion
epsilo = str2double(epsiloS); % increment at each iteration in matrix inversion procedure
conditional_sampling = str2double(conditional_samplingS); % activation of conditional sampling
histo_estremeQ=str2double(histo_estremeQs);
thresGUI=str2double(thresGUIs);
isdeterministic = str2double(isdeterministics);
isuniform=str2double(isuniforms);
opt_GUI = str2num(opt_GUIs);
opt_FPF = str2num(opt_FPFs);
homoth = str2num(homoths);
IR=str2double(IRs);
K=str2double(Ks);
% if seed is not specified, 'shuffle'  on current platform time
if nargin < 33
    rng('shuffle','twister');
    disp(sprintf(' rng seed:  not specified, default is shuffle on current platform time'));
else
    rng(str2double(s_rng_seed),'twister');
    disp(sprintf(' rng seed:  %s',s_rng_seed));
end
rng_data=rng;
flagPQ=str2double(s_flagPQ);

try
    if isdeterministic == 0
    
    load(ifile);
    
    %%%% 
   if homoth == 1
    nationss = unique(nat_ID);
    forec= [forec_filt(:,1:2)];
    snap=[snap_filt(:,1:2)];
    for iinat = 1:length(nationss)
        idx_ = find(strcmp(nat_ID,nationss{iinat}));
        idx_pl = intersect(idx_,intersect(find(strcmp(type_ID,'L')),[1:2:length(inj_ID)]));
        idx_ql = intersect(idx_,intersect(find(strcmp(type_ID,'L')),[2:2:length(inj_ID)]));
        idx_pg = intersect(idx_,intersect(find(strcmp(type_ID,'G')),[1:2:length(inj_ID)]));
        idx_qg = intersect(idx_,intersect(find(strcmp(type_ID,'G')),[2:2:length(inj_ID)]));
        forec_filt(isnan(forec_filt))=0; 
        snap_filt(isnan(snap_filt))=0;
        
        fo_p = - sum(forec_filt(:,2+idx_pg),2) - sum(forec_filt(:,2+idx_pl),2);
        fo_q = - sum(forec_filt(:,2+idx_qg),2) - sum(forec_filt(:,2+idx_ql),2);
        sn_p = - sum(snap_filt(:,2+idx_pg),2) - sum(snap_filt(:,2+idx_pl),2);
        sn_q = - sum(snap_filt(:,2+idx_qg),2) - sum(snap_filt(:,2+idx_ql),2);
        forec = [forec,  fo_p fo_q];
        snap = [snap,  sn_p sn_q];
        inj1{2*(iinat-1)+1}=['error_' nat_ID{idx_(1)} '_P'];
        inj1{2*(iinat-1)+2}=['error_' nat_ID{idx_(1)} '_Q'];
        nat1{2*(iinat-1)+1} = nat_ID{idx_(1)};
        nat1{2*(iinat-1)+2} = nat_ID{idx_(1)};
    end
       forec_filt = forec;
       snap_filt = snap;
       inj_ID=inj1;
       nat_ID=nat1;
   end
%     clear snap_filt
    tipovar = ones(1,length(inj_ID));
    if unimodd
        
        [uni multi] = find_unimodals(forec_filt,snap_filt,inj_ID);
        
       quali_uni = find(ismember(inj_ID,uni));
       quali_multi = find(ismember(inj_ID,multi));
        tipovar(quali_multi)=2;
    
    end
    flagesistenza = exist('snap_filt');
  
    %%%%% NAT_ID
    quali_nats = unique(nat_ID);
    for inat = 1:length(quali_nats)
        inj_nat{inat} = find(strcmp(nat_ID,quali_nats{inat}));
        FOnat{inat} = forec_filt(:,2+inj_nat{inat});
        SNnat{inat} = snap_filt(:,2+inj_nat{inat});
        quanti_fovalid(inat) = length(find(sum(not(isnan(FOnat{inat})),1)>par_Nmin_obs_fract*size(FOnat{inat},1)));
        quanti_snvalid(inat) = length(find(sum(not(isnan(SNnat{inat})),1)>par_Nmin_obs_fract*size(SNnat{inat},1)));
        if isuniform == 0
        if quanti_fovalid(inat) >= 0.5*size(FOnat{inat},2)
            modality_gaussians(inat) = 0;
            modality_uniform(inat) = 0;
        else
            modality_gaussians(inat) = 1;
            modality_uniform(inat) = 0;
        end
        else
            modality_uniform(inat) = 1;
             modality_gaussians(inat) = 0;
        end
        nations{inat,1}=    [FOnat{inat}];
        nations{inat,2} =     [SNnat{inat}];
        nations{inat,3} =   inj_ID(inj_nat{inat});
        nations{inat,4} =   tipovar(inj_nat{inat});
        nations{inat,5} =   nat_ID(inj_nat{inat});
        if strcmp(lower(natS),'all') || strcmp(lower(natS),'ALL')
            selezionate(inat) =1;
        else
            if isempty(strfind(natS,quali_nats{inat}))
                selezionate(inat) =0;
            else
                selezionate(inat) =1;
            end
        end
    end
    
    qua_selezionate = find(selezionate == 1);
    
    quali_normal = intersect(intersect(find(modality_gaussians == 0),find(modality_uniform == 0)),qua_selezionate);
    if isuniform == 0
    quali_gauss = setdiff([1:length(quali_nats)],quali_normal);
    quali_uniform=[];
    else
    quali_uniform = setdiff([1:length(quali_nats)],quali_normal);    
    quali_gauss=[];
    end
    
    if isempty(quali_normal) == 0
        nations_normal.forec_filt = [forec_filt(:,1:2) nations{quali_normal,1}];
        nations_normal.snap_filt = [snap_filt(:,1:2) nations{quali_normal,2}];
        nations_normal.inj_ID = [nations{quali_normal,3}];
        nations_normal.tipovar = [nations{quali_normal,4}];
         nations_normal.nat_ID = [nations{quali_normal,5}];
        k_norm = 1;
    else
        k_norm = 0;
        nations_normal.forec_filt=[];
        nations_normal.snap_filt=[];
    end
    nations_gauss.forec_filt = [];
    nations_gauss.snap_filt = [];
    nations_gauss.inj_ID = {};
    nations_unif.forec_filt = [];
    nations_unif.snap_filt = [];
    nations_unif.inj_ID = {};
    nations_unif.nat_ID = {};
    for ino = 1:length(quali_gauss)
        nations_gauss(ino).forec_filt = [];
        nations_gauss(ino).snap_filt = [];
        nations_gauss(ino).inj_ID = nations{quali_gauss(ino),3};
        nations_gauss(ino).nat_ID = nations{quali_gauss(ino),5};
    end
     for ino = 1:length(quali_uniform)
        nations_unif(ino).forec_filt = [];
        nations_unif(ino).snap_filt = [];
        nations_unif(ino).inj_ID = nations{quali_uniform(ino),3};
        nations_unif(ino).nat_ID = nations{quali_uniform(ino),5};
    end
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NORMAL TREATMENT
    
    %
    
    if ~isempty(nations_normal.forec_filt)
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        % here starts RSE CODE, extracted from TEST_MCLA,m
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        forec_filt = nations_normal.forec_filt;
        snap_filt =  nations_normal.snap_filt;
        flagesistenza = not(isempty(snap_filt));
        inj_ID = nations_normal.inj_ID;
        nat_ID=nations_normal.nat_ID;
        tipovar = nations_normal.tipovar;
        method = str2double(s_method);
        
        
        if opt_FPF == 1 || opt_GUI == 1
            
            if opt_GUI == 1
        disp(['STARTED ALGORITHM TO RETRIEVE DATA FOR UNCERTAINTY GUI'])
        else
         disp(['DATA FOR UNCERTAINTY GUI NOT GENERATED'])   
        end
            if opt_FPF == 1
        disp(['STARTED ALGORITHM TO RETRIEVE DATA FOR FPF CONDITIONED ERROR FORECASTS'])
            else
          disp(['FPF COMPUTATIONS NOT PERFORMED'])      
            end
        [dati_FPF] = calcolo_inv_mat_perFPF(snap_filt-forec_filt,forec_filt,inj_ID,nat_ID,flagPQ,method,ofile_forFPF,ofileGUI,par_tolvar,par_Nmin_obs_fract,par_nnz,par_Nmin_obs_interv,par_outliers,Koutliers,par_imputation_meth,par_Ngaussians,check_mod0,percentil,tolerance,iterations,epsilo,thresGUI,modo_inv,tipovar,opt_GUI,opt_FPF);
%         else
%             dati_FPF = [];
        else
            %%%% null contents for both GUI and FPF output files
            statisticals.means = [];
            statisticals.stddevs = [];
            dati_FPF=[];
            inj_IDFPF1{1} = 'null';
            
            save(ofile_forFPF,'-struct','statisticals')
            
            csvFileName=sprintf('%s.csv',ofile_forFPF);
            fid = fopen(csvFileName,'w');
            fmtString = [repmat('%s,',1,size(inj_IDFPF1,2)-1),'%s\n'];
            fprintf(fid,fmtString,inj_IDFPF1{:});
            fclose(fid);
            dlmwrite(csvFileName,statisticals.means,'delimiter',',','-append');
            dlmwrite(csvFileName,statisticals.stddevs,'delimiter',',','-append');
            uncertaintyGUI.Ev =[];
            uncertaintyGUI.inj_IDGUI = [];
            uncertaintyGUI.correlatio = [];
            uncertaintyGUI.eigenvalues = [];
            uncertaintyGUI.loadings = [];
            save(ofileGUI, '-struct', 'uncertaintyGUI','-v7.3');

        end
        
        disp(['STARTED ALGORITHM TO FIX NaN VALUES IN FORECAST ERROR MATRIX'])
        tic;
        
        %%% CONDITIONAL SAMPLING ACTIVATED
        if conditional_sampling == 1
                        
              [Y inj_ID nat_ID idx_err idx_fore snapQ inj_IDQ dummy1 dummy2 dummy3 dummy4 dummy5 tipovar] = MODULE0(snap_filt,forec_filt,inj_ID,nat_ID,flagPQ,method,par_tolvar,par_Nmin_obs_fract,par_nnz,par_Nmin_obs_interv,par_outliers,Koutliers,par_imputation_meth,par_Ngaussians,check_mod0,conditional_sampling,tipovar);
                
        t_module0= toc;
        disp(['GAP FILLING MODULE COMPLETED IN ' num2str(t_module0) ' SECONDS'])
       
        disp(['EVALUATING THE MAXIMUM VALUES OF VARIABLES (PERCENTILES)'])
        inj_ID0 = inj_ID;
        nat_ID0 = nat_ID;
        idx_err0 = idx_err;
        idx_fore0 = idx_fore;
                
        idx_err_uni = find(ismember(find(tipovar==1),idx_err));
       idx_err0_uni = find(ismember(find(tipovar==1),idx_err0));
        idx_fore_uni = find(ismember(find(tipovar==1),idx_fore));
        idx_fore0_uni = find(ismember(find(tipovar==1),idx_fore0));
        Y_uni = Y(:,find(tipovar==1));
        Y_mult= Y(:,find(tipovar==2));
        inj_ID_uni = inj_ID(find(tipovar==1));
        nat_ID_uni = nat_ID(find(tipovar==1));
        inj_ID0_uni = inj_ID0(find(tipovar==1));
        nat_ID0_uni = nat_ID0(find(tipovar==1));
        
        inj_ID_mult = inj_ID(find(tipovar==2));
        
        nat_ID_mult = nat_ID(find(tipovar==2));
        
        idx_err_mult = find(ismember(find(tipovar==2),idx_err));
        idx_fore_mult = find(ismember(find(tipovar==2),idx_fore));
        tipovar_uni = tipovar(find(tipovar==1));
        %%% multimodal vars treatment
        
        dati_condMULTI=[];
        dati_condUNI=[];
        module1 = [];
        
        for ncol = 1:size(snapQ,2)
            validi = find(~isnan(snapQ(:,ncol)));
            if length(validi) < 100
                maxvalue(ncol,:)=[0 0]; 
            else
            [x1,x2] = ecdf(snapQ(validi,ncol));
            idquantileL = find(abs(x1-percentil)==min(abs(x1 - percentil)));
            idquantileH = find(abs(x1-(1-percentil))==min(abs(x1 - (1-percentil))));
            maxvalue(ncol,1) = x2(idquantileL(1));
            maxvalue(ncol,2) = x2(idquantileH(1));
            end
        end
        %%%%%%%%
                if isempty(find(tipovar==2))==0
                [CTG_table BIV_UU UNIV_DU] = multimodal_treatment(Y_mult,idx_err_mult,idx_fore_mult,tipovar);
        
                dati_condMULTI.CTG_table = CTG_table;
                dati_condMULTI.BIV_UU = BIV_UU;
                dati_condMULTI.nat_ID = nat_ID;
                dati_condMULTI.UNIV_DU = UNIV_DU;
                dati_condMULTI.idx_err_mult = idx_err_mult;
                dati_condMULTI.idx_fore_mult = idx_fore_mult;
                dati_condMULTI.inj_ID_mult = inj_ID_mult;
                dati_condMULTI.nat_ID_mult = nat_ID_mult;
        
        
                end
        
        if isempty(find(tipovar==1))==0
            
        % find matching between idx err0 and idx fore 0
        for jfor = 1:length(idx_fore0_uni)
            dummys = find(ismember(inj_ID0_uni(idx_err0_uni),inj_ID0_uni(idx_fore0_uni(jfor))));
            if isempty(dummys)==0
            match_snap_to_fore(jfor)=dummys;
            else
            match_snap_to_fore(jfor)=NaN;    
            end
        end
        
        FORE0=Y_uni(:,idx_fore0_uni);
       
        m_e = mean(Y_uni(:,idx_err_uni),1);
        m_y = mean(Y_uni(:,idx_fore_uni),1);
        std_e = std(Y_uni(:,idx_err_uni),0,1);
        std_y = std(Y_uni(:,idx_fore_uni),0,1);
        
        disp(['FILTERING OUT HIGHLY CORRELATED FORECAST VARIABLES'])
        tic
        [m_y std_y Y_uni inj_ID_uni nat_ID_uni tipovar_uni idx_err_uni idx_fore_uni matrice idx_fore0_uni tipovar0_uni] = FILTERFOREC(m_y, std_y, Y_uni, inj_ID_uni,nat_ID_uni, tipovar_uni, idx_err_uni, idx_fore_uni);

        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        tempo(1)=toc;
        %%%%% put into a function
                       
                       
        disp(['STARTED ALGORITHM TO ESTIMATE COPULAS AND CDFS'])
        tic;
        X = Y_uni;
        [ module1 ] = MODULE1( X,K);
        
        toc;
        
        tic
        media(idx_err_uni) = m_e;
        media(idx_fore_uni) = m_y;
        stddev(idx_err_uni)=std_e;
        stddev(idx_fore_uni)=std_y;
        disp(['NATAF TRANSFORMATION OF FORECASTS AND SNAPSHOTS'])
        
        for ij = 1:size(Y_uni,2)
            X_NEW1S(:,ij) = (Y_uni(:,ij) - media(ij))./(stddev(ij));
        end
        
        for ie = 1:length(idx_err_uni)
%             tic
            CDF_E_NEW1(:,ie) = estract_cdf(X_NEW1S(:,idx_err_uni(ie)));
            
            INVG_E_NEW1(:,ie) = norminv(CDF_E_NEW1(:,ie),0,1);
        end
        for iy = 1:length(idx_fore_uni)
             CDF_Y_NEW1(:,iy) = estract_cdf(X_NEW1S(:,idx_fore_uni(iy)));
            INVG_Y_NEW1(:,iy) = norminv(CDF_Y_NEW1(:,iy),0,1);
        end
        
        X_NEW2S(:,idx_err_uni)=INVG_E_NEW1;
        X_NEW2S(:,idx_fore_uni)=INVG_Y_NEW1;
        
        cov_tot = cov(X_NEW2S);
        
        cov_yy2 = cov_tot(idx_fore_uni,idx_fore_uni);
        cov_ey2 = cov_tot(idx_err_uni,idx_fore_uni);
        cov_ee2 = cov_tot(idx_err_uni,idx_err_uni);
        
        corr_yy2 = (( diag(diag(cov_yy2)))^(-0.5))*cov_yy2*(( diag(diag(cov_yy2)))^(-0.5));
        corr_ey2 = (( diag(diag(cov_ee2)))^(-0.5))*cov_ey2*(( diag(diag(cov_yy2)))^(-0.5));
        corr_ee2 = (( diag(diag(cov_ee2)))^(-0.5))*cov_ee2*(( diag(diag(cov_ee2)))^(-0.5));
        disp(['INVERSION OF FORECAST CORREALTION MATRIX WITH GIVEN ACCURACY'])
        % 
        
        
        switch modo_inv
            case 1
        [invcorr_yy2x corr_yy2] = inversion_with_verify2(corr_yy2,tolerance,iterations,epsilo);
            case 2
        [invcorr_yy2x corr_yy2] = inversione_ncond(corr_yy2,1/eps,100,10);
        end
        
        disp('check frobenius norm of inverse * cov matrix from Id matrix ...')
        norm((( diag(diag(cov_yy2)))^(-0.5))*invcorr_yy2x*(( diag(diag(cov_yy2)))^(-0.5))*cov_yy2 - eye(size(cov_yy2)),'fro')
        
        %%%%%%%%%%%%%%%%%%%%%%%%%%
        %%% DATA STRUCTURE CONTAINING VARIABLES USED FOR CONDITIONAL
        %%% SAMPLING ALGORITHM
%        
        dati_condUNI.invmatr_corryy = invcorr_yy2x;
        dati_condUNI.matrice_yy = matrice;
        dati_condUNI.cov_ey = cov_ey2;
        dati_condUNI.corr_ee = corr_ee2;
        dati_condUNI.corr_ey = corr_ey2;
        dati_condUNI.idx_err = idx_err_uni;
        dati_condUNI.idx_fore = idx_fore_uni;
        dati_condUNI.idx_err0 = idx_err0_uni;
        dati_condUNI.idx_fore0 = idx_fore0_uni;
        dati_condUNI.match_snap_to_fore = match_snap_to_fore;
        dati_condUNI.inj_ID0 = inj_ID0_uni;
        dati_condUNI.nat_ID0 = nat_ID0_uni;
        dati_condUNI.nat_ID = nat_ID_uni;
        dati_condUNI.tolerance = tolerance;
        dati_condUNI.iterations = iterations;
        dati_condUNI.epsilo = epsilo;
        dati_condUNI.Y=Y_uni;
        dati_condUNI.FORE0=FORE0;
        end
        %%%%%% 	ARRAY OF STRINGS TO IDENTIFY REACTIVE INJECTIONS IN THE
        %%%%%% 	MAXVALUE MATRIX
        dati_Q.inj_IDQ = inj_IDQ;
            
        else
            %%%% CALCOLO QUANTIT� PER FPF
            
            
            %%% CONDITIONAL SAMPLING DEACTIVATED -> UNCODITIONED SAMPLING
            %%% OF FORECAST ERRORS
            %%% CONDITIONAL SAMPLING DEACTIVATED -> UNCODITIONED SAMPLING
            %%% OF FORECAST ERRORS
            [Y inj_ID nat_ID idx_err idx_fore snapQ  inj_IDQ dummy1 dummy2 dummy3 dummy4] = MODULE0(snap_filt,forec_filt,inj_ID,nat_ID,flagPQ,method,par_tolvar,par_Nmin_obs_fract,par_nnz,par_Nmin_obs_interv,par_outliers,Koutliers,par_imputation_meth,par_Ngaussians,check_mod0,conditional_sampling,tipovar);
                
        t_module0= toc;
        disp(['GAP FILLING MODULE COMPLETED IN ' num2str(t_module0) ' SECONDS'])
       
        disp(['EVALUATING THE MAXIMUM VALUES OF VARIABLES (PERCENTILES)'])
        
        
        for ncol = 1:size(snapQ,2)
            validi = find(~isnan(snapQ(:,ncol)));
            if length(validi) < 100
                maxvalue(ncol,:)=[0 0]; 
            else
            [x1,x2] = ecdf(snapQ(validi,ncol));
            idquantileL = find(abs(x1-percentil)==min(abs(x1 - percentil)));
            idquantileH = find(abs(x1-(1-percentil))==min(abs(x1 - (1-percentil))));
            maxvalue(ncol,1) = x2(idquantileL(1));
            maxvalue(ncol,2) = x2(idquantileH(1));
            end
        end
      
        tic;
        X = Y;
        [ module1 ] = MODULE1( X,K);
        toc;
        
        dati_Q.inj_IDQ = inj_IDQ;
       dati_condUNI=[];
       dati_condMULTI=[];
       inj_ID_uni = inj_ID;
       nat_ID_uni = nat_ID;
        end
                
        
        moutput(1).errmsg='Ok';
        moutput(1).rng_data=rng_data;
        moutput(1).inj_ID=inj_ID_uni;
        moutput(1).flagPQ=flagPQ;
        moutput(1).nat_ID=nat_ID_uni;
        moutput(1).method=method;
        moutput(1).module1=module1;
        moutput(1).modality_gaussian=0;
            moutput(1).modality_uniform=0;
        moutput(1).dati_condUNI = dati_condUNI;
        moutput(1).dati_condMULTI = dati_condMULTI;
        moutput(1).dati_Q = dati_Q;
        moutput(1).dati_FPF = dati_FPF;
        moutput(1).mversion=mversion;
        moutput(1).flagesistenza=flagesistenza;
        % added field
        moutput(1).maxvalue=maxvalue;
        moutput(1).conditional_sampling=conditional_sampling;
        clear maxvalue  inj_ID
    end
    
    if  ~isempty(nations_gauss(1).inj_ID)
        %         keyboard
        
        for ino = 1:length(nations_gauss)
            snap_filt = nations_gauss(ino).snap_filt;
            forec_filt = nations_gauss(ino).forec_filt;
            inj_ID = nations_gauss(ino).inj_ID;
            nat_ID = nations_gauss(ino).nat_ID;
            flagesistenza = not(isempty(snap_filt));
            %%% CALCULATION OF HISTORICAL LIMIT QUANTILES ALSO IN CASE OF
            %%% GAUSSIAN FICTITIOUS FORECAST ERRORS
            if isempty(snap_filt)==0
                snap_new1 = snap_filt(:,3:end);
                snapQ = snap_new1(:,2:2:end);
                inj_IDQ1 = inj_ID(2:2:end);
                for ncol = 1:size(snapQ,2)
                    validi = find(~isnan(snapQ(:,ncol)));
                    if length(validi) < 100
                        maxvalue(ncol,:)=[0 0];
                    else
                        [x1,x2] = ecdf(snapQ(validi,ncol));
                        idquantileL = find(abs(x1-percentil)==min(abs(x1 - percentil)));
                    idquantileH = find(abs(x1-(1-percentil))==min(abs(x1 - (1-percentil))));
                     maxvalue(ncol,1) = x2(idquantileL(1));
                     maxvalue(ncol,2) = x2(idquantileH(1));
                    end
                    inj_IDQ{ncol}=inj_IDQ1{ncol}(1:end-2);
                end
            else
                inj_IDQ1 = inj_ID(2:2:end);
                for ncol = 1:length(inj_IDQ1)
                    
                    maxvalue(ncol,1) = -histo_estremeQ;
                    maxvalue(ncol,2) = histo_estremeQ;
                    
                    inj_IDQ{ncol}=inj_IDQ1{ncol}(1:end-2);
                end
            end
            if flagPQ == 0
                inj_ID(:,2:2:end)=[];  %odd columns of reactive injections are discarded
                nat_ID(:,2:2:end)=[]; 
            end
            method = 0;
            module1 = [];
            %         maxvalue=[];
            dati_Q.inj_IDQ = inj_IDQ;
            dati_FPF.idx_errA = [1:length(inj_ID)];
            dati_FPF.idx_foreA = [1:length(inj_ID)];
            dati_FPF.inj_ID = inj_ID;
            dati_condUNI=[];
            dati_condMULTI=[];
            moutput(k_norm+ino).errmsg='Ok';
            moutput(k_norm+ino).rng_data=rng_data;
            moutput(k_norm+ino).inj_ID=inj_ID;
             moutput(k_norm+ino).nat_ID=nat_ID;
            moutput(k_norm+ino).modality_gaussian=1;
            moutput(k_norm+ino).modality_uniform=0;
            moutput(k_norm+ino).flagPQ=flagPQ;
            moutput(k_norm+ino).method=method;
            moutput(k_norm+ino).module1=module1;
            moutput(k_norm+ino).dati_condUNI = dati_condUNI;
            moutput(k_norm+ino).dati_condMULTI = dati_condMULTI;
            moutput(k_norm+ino).dati_Q = dati_Q;
            moutput(k_norm+ino).dati_FPF = dati_FPF;
            moutput(k_norm+ino).mversion=mversion;
            moutput(k_norm+ino).flagesistenza=flagesistenza;
            % added field
            moutput(k_norm+ino).maxvalue=maxvalue;
            moutput(k_norm+ino).conditional_sampling=conditional_sampling;
            clear maxvalue inj_IDQ inj_IDQ1 snap_new1 snapQ inj_ID
        end
    end
    
    %%%% UNIFORM DISTRIBU
      if  ~isempty(nations_unif(1).inj_ID)
        %         keyboard
        
        for ino = 1:length(nations_unif)
            snap_filt = nations_unif(ino).snap_filt;
            forec_filt = nations_unif(ino).forec_filt;
            inj_ID = nations_unif(ino).inj_ID;
            nat_ID = nations_unif(ino).nat_ID;
            flagesistenza = not(isempty(snap_filt));
            %%% CALCULATION OF HISTORICAL LIMIT QUANTILES ALSO IN CASE OF
            %%% GAUSSIAN FICTITIOUS FORECAST ERRORS
            if isempty(snap_filt)==0
                snap_new1 = snap_filt(:,3:end);
                snapQ = snap_new1(:,2:2:end);
                inj_IDQ1 = inj_ID(2:2:end);
                for ncol = 1:size(snapQ,2)
                    validi = find(~isnan(snapQ(:,ncol)));
                    if length(validi) < 100
                        maxvalue(ncol,:)=[0 0];
                    else
                        [x1,x2] = ecdf(snapQ(validi,ncol));
                        idquantileL = find(abs(x1-percentil)==min(abs(x1 - percentil)));
            idquantileH = find(abs(x1-(1-percentil))==min(abs(x1 - (1-percentil))));
            maxvalue(ncol,1) = x2(idquantileL(1));
            maxvalue(ncol,2) = x2(idquantileH(1));
                    end
                    inj_IDQ{ncol}=inj_IDQ1{ncol}(1:end-2);
                end
            else
                inj_IDQ1 = inj_ID(2:2:end);
                for ncol = 1:length(inj_IDQ1)
                    
                    maxvalue(ncol,1) = -histo_estremeQ;
                    maxvalue(ncol,2) = histo_estremeQ;
                    
                    inj_IDQ{ncol}=inj_IDQ1{ncol}(1:end-2);
                end
            end
            if flagPQ == 0
                inj_ID(:,2:2:end)=[];  %odd columns of reactive injections are discarded
                nat_ID(:,2:2:end)=[]; 
            end
            method = 0;
            module1 = [];
            %         maxvalue=[];
            dati_Q.inj_IDQ = inj_IDQ;
            dati_FPF.idx_errA = [1:length(inj_ID)];
            dati_FPF.idx_foreA = [1:length(inj_ID)];
            dati_FPF.inj_ID = inj_ID;
           
            dati_condUNI=[];
        dati_condMULTI=[];
            moutput(k_norm+ino).errmsg='Ok';
            moutput(k_norm+ino).rng_data=rng_data;
            moutput(k_norm+ino).inj_ID=inj_ID;
             moutput(k_norm+ino).nat_ID=nat_ID;
            moutput(k_norm+ino).modality_gaussian=0;
            moutput(k_norm+ino).modality_uniform=1;
            moutput(k_norm+ino).flagPQ=flagPQ;
            moutput(k_norm+ino).method=method;
            moutput(k_norm+ino).module1=module1;
            moutput(k_norm+ino).dati_condUNI = dati_condUNI;
             moutput(k_norm+ino).dati_condMULTI = dati_condMULTI;
            moutput(k_norm+ino).dati_Q = dati_Q;
            moutput(k_norm+ino).dati_FPF = dati_FPF;
            moutput(k_norm+ino).mversion=mversion;
            moutput(k_norm+ino).flagesistenza=flagesistenza;
            % added field
            moutput(k_norm+ino).maxvalue=maxvalue;
            moutput(k_norm+ino).conditional_sampling=conditional_sampling;
            clear maxvalue inj_IDQ inj_IDQ1 snap_new1 snapQ inj_ID
        end
    end
    %%%%%%%%%%%%%%%%%%%%%%%%%%
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % here ends RSE CODE, extracted from TEST_MCLA,m
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    else
       
            method = 0;
            module1 = [];
            %         maxvalue=[];
            dati_condUNI=[];
            dati_condMULTI=[];
            moutput(1).errmsg='Ok';
            moutput(1).rng_data=rng_data;
            moutput(1).inj_ID=[];
            moutput(1).nat_ID=[];
            moutput(1).modality_gaussian=0;
            moutput(1).modality_uniform=0;
            moutput(1).flagPQ=0;
            moutput(1).method=method;
            moutput(1).module1=module1;
            moutput(1).dati_condUNI = [];
            moutput(1).dati_condMULTI = [];
            moutput(1).dati_Q = [];
            moutput(1).dati_FPF = [];
            moutput(1).mversion=mversion;
            moutput(1).flagesistenza=0;
            % added field
            moutput(1).maxvalue=0;
            moutput(1).conditional_sampling=0;
           
        
    end
    %save output in .mat
    
    exitcode=0;
catch err
    moutput(1).errmsg=err.message;
    disp(getReport(err,'extended'));
    exitcode=-1;
end
totmoutput.out1 = moutput;

save(ofile, '-struct', 'totmoutput','-v7.3');

% exit(exitcode);
end