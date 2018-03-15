%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%%% demo: calls FEA and MCLA related functions

%%%%%% FEA ANALYSIS

%% module1

m1_ifile='histoData1.mat';%'fea_input_7busItesla_nat';%'feanalyzerinput.mat';%'histoData.mat';%'feanalyzerinput.mat';%'histoData.mat';%'input_homothetic2'; %'feanalyzerinput.mat';%'input_homothetic'; %'feanalyzerinput.mat';%'input_homothetic'; %'feanalyzerinput.mat';%'input_homothetic'; %'feanalyzerinput.mat';%'feanalyzerinputNEW2.mat';%'fea_input_7busItesla.mat';%'input_EU2.mat';%'feaiteslaFRBE_tutti.mat';%'feanalyzerinput2016';%'feaanalyzeFEB2013';%'feanalyzerinputNEW_NAT';%'feaFRtestMULTIUNI_T2';%'feaiteslaFRBE.mat';%''feaiteslaFRBE_tutti.mat';%'fea_input_7busItesla.mat';%'feaiteslaFRBE_tutti.mat';%'feaiteslaFRBE_tutti.mat';%'feaiteslaFRBE.mat';%'fea_input_7busItesla.mat';%'feanalyzerinput.mat';%'fea_input_7busItesla.mat';%'feanalyzerinput.mat';%'fea_input_7busItesla.mat';%'feaanalyzeinput_400.mat';%'fea_input_7busItesla.mat';%'feaanalyzeinput_400.mat';%'fea_input_7busItesla.mat';%'feanalyzerinput.mat';%'fea_input_7busItesla.mat';%'feanalyzerinput.mat';%% name of the file with forecast data fea_input_7busItesla = 7 bus grid; feanalyzerinput = French grid (latest version from Quinary)
m1_ofile='feam1output_conv_option.mat';   %% module1 output file name
ofile_forFPF = 'fea_stats_for_FPF_conv_option';
opt_GUI='0'; % if 1= compute variables for GUI, 0 = not compute variables for GUI
ofileGUI = 'fea_data_uncertainty_GUI_conv_option'; %% output mat file which stores the data for the GUI representation of forecast errors
IRs='0.8'; %% IR: fraction of explained variance for PCA
Ks='13'; %% number of clusters for PCA --> rule of thumb: sqrt(Nsamples/2)
s_flagPQ='1'; %%  flagPQ for all injections: if 1, P and Q vars are separately sampled, if 0, Q is sampled with the same initial pf starting from P vars.
s_flagPQ_RES='0'; %%  flagPQ for RES: if 1, P and Q injected by RES are separately sampled, if 0, Q is sampled with the same initial pf starting from P injections. up to now it is not used.
percentile_historical = '0.05'; % quantile of the distribution of historical data related to Q vars, to set realistic limits of Q samples in case of using a constant power factor to produce Q samples starting from P samples
percpu_fict_gauss_load = '0.05'; % percentage of current load forecast, to be used for std dev of fictitious gaussians
percpu_fict_gauss_RES = '0.15'; % percentage of current RES forecast, to be used for std dev of fictitious gaussians
correlation_fict_gauss = '0'; % Pearson correlation among RES and load variables: "0" all variables RES and load are uncorrelated; "1" = all variables RES and load are completely correlated; an intermediate value means that RES and LOAD are intermediately correlated, but inside each category (RES and Load) all loads (all RES) are completely correlated.
histo_estremeQ='5'; % multiple of ("percpu_fict_gauss_load" x current_forecast_value) used to estimate the "percentile_historical" and "1-percentile_historical" quantiles of fictitious gaussian distributions in case of lack of historical data
option_sign = '0'; % if 1, when value of extracted sample has a different sign wrt forecast the sampled value is put to 0. if 0, the sampled value is assumed as valid sample.option applied only to RES injections
% options to deal with raw data and fix possible gaps
s_method='4';  %% method for missing data imputation: if 1, method = proposed method by RSE; if 2, method = gaussian conditional sampling; if 3, method = gaussian mixture imputation; if 4, interpolation based method.
imputation_meth='2'; % type of inputation techniques for gaussian mixture (s_method = 3): 2= random draw, 1 = conditional mean of gaussian components
check_module0 = '0'; % evaluates the goodness of the fit by the imputation method (check preservation of correlation among variables)
tolvar = '1e-3';% minimum variance of imput variables, in MW
Nmin_obs_fract = '0.7';% minim fraction of valid samples for each stochastic variables
Nmin_obs_interv = '150';% min nr of samples in common for each pair of variables
Nmin_nz_fract = '0.05';% minim fraction of non zeros for each stochastic variables
outliers = '1';% 0 = outliers are included as valid samples, 1 = outliers are excluded
koutlier='5'; % n for the n-sigma rule to filter out the outliers
Ngaussians='5'; % nr of gaussians of the mixture
Tflags='0'; % truncate gaussian for module 3 backprojection: 0 = no, 1 = yes
%%% options for conditional sampling calculation
conditional_sampling = '1'; % 1 = activated conditional sampling, 0 = not active
iterations = '100'; % nr of iterations for calculation of inverse of correlation matrix with given accuracy
tolerance = '1e-8'; % accuracy in percentage for the calculation of inverse of correlation matrix R
epsilo = '1e-5'; % quantity to be added to diagonal to better approximate the inverse of the correlation matrix (better not exceed 5e-3/iterations)
centering = '0'; % centering the conditioned snapshots onto the specific basecase (DACF) to improve uncertainty model
thresGUI='0.95'; % threshold of vlaue of negative correlation below which the correlations are not reported in the GUI
nations = 'all'; % parameter indicates which countries undergo a FEA using histo data. the others are treated as indipendent areas with Gaussian forecast error models. if string = 'all' all the coutnries with available histo data are treated with a unique normal FEA
%%% new parameters updated Dec 19, 2016
isuniform = '0'; % 1 = uniform distribution for forecast error model for all nations
isdeterministic = '0'; % 0 = stochastic sampling; 1 = deterministic variation from basecase
Pload_deterministic ='0.02'; % pu value of std dev for the load P, 
Qload_deterministic ='0.03'; % pu value of std dev for the load Q 
% general forecast error model with uniform distribution
band_uniform_PL = '0.05'; % half-band for uniform distribution for forecast error model of active power of loads
band_uniform_QL = '0.05'; % half-band for uniform distribution for forecast error model of reactive power of loads
band_uniform_PGEN = '0.05'; % half-band for uniform distribution for forecast error model of active power of generators
correlation_fict_uniform = '0'; % Pearson correlation among RES and load variables: "0" all variables RES and load are uncorrelated; "1" = all variables RES and load are completely correlated; an intermediate value means that RES and LOAD are intermediately correlated, but inside each category (RES and Load) all loads (all RES) are completely correlated.
modo_invs = '2'; %%% inversion mode for forecast correlation matrix: 1 = adding eps to main diagonal, 2= reducing offdiagonal terms
%%%% separazione uni e multimodali, update March-June 2017
unimodal_separate = '0'; % 1 = separate analysis uni e multimodals, 0 = all variables as one set
full_dependence = '1'; % 0 = each injection treated as a pair of vars (SN,FO) and each SN is conditioned only by its own FO, 1 = use of full correlation matrixes (conventional approach)
%%%% additional parameters added Sept-Oct 2017
homothetic = '0'; % option to disaggregate the country sample error on the loads of the corresponding nation: 0 = disactivated, 1 = activated.
opt_FPF='0'; % 1 = compute data for FPF, 0= deactivated computation
model_conv='0'; % 0 = conventional generators are deterministic injections, 1= conv. generator injections are modelled as stochastic variables (outage modeling)
FEA_MODULE1_HELPER(m1_ifile, m1_ofile,nations,ofile_forFPF,ofileGUI,IRs, Ks, s_flagPQ,s_method,tolvar,Nmin_obs_fract,Nmin_nz_fract,Nmin_obs_interv,outliers,koutlier,imputation_meth,Ngaussians,percentile_historical,check_module0,tolerance,iterations,epsilo,conditional_sampling,histo_estremeQ,thresGUI,unimodal_separate,modo_invs,isdeterministic,isuniform,opt_GUI,opt_FPF,homothetic,model_conv);

%% module2
m2_partk_prefix='fea_m2_';  %% module 2 file name prefix for partial cluster results
% partial module2 computation (one per cluster)

for i=1:(str2double(Ks))
    cls = i-1;
    FEA_MODULE2_HELPER(m1_ofile, sprintf('%s%u.mat',m2_partk_prefix, cls), int2str(cls),IRs,Tflags,isdeterministic);
end

% aggregate module1 output and partial module2 results in one file
m2_ofile='fea_output_m2_conv_option.mat';  %% module2 output file name
m2_current_folder='.';
FEA_MODULE2_REDUCE_HELPER(m1_ofile, m2_current_folder, m2_partk_prefix, Ks, m2_ofile,percpu_fict_gauss_load,percpu_fict_gauss_RES,correlation_fict_gauss,isdeterministic,Pload_deterministic,Qload_deterministic,band_uniform_PL,band_uniform_QL,band_uniform_PGEN,correlation_fict_uniform);

% module 3 output
m3_ofile='uncond_sampling_output_EU.mat';  %'feasampleroutput_conv_option.mat';%'uncond_sampling_output_old.mat';  %'mcsamplerinput_forecast_offline_samples_DACF',%'uncond_sampling_output.mat';  %% module3 output file name
m3_current_folder='.';
uncond_nsamples_s='500';
SAMPLING_MODULE3_HELPER(m2_ofile, m3_ofile,uncond_nsamples_s,isdeterministic, '1');

%%%%%% MCLA SAMPLER
mcla_ifile='mcsamplerinput_20170201_0030_FO3_FR0.mat';%'mcsamplerinput_conv_option';%'mcsamplerinput_20170201_0030_FO3_FR0.mat';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20170201_0030_FO3_FR0.mat';%'mcsamplerinput_20130101_0930_FO2_FR0_8205249616537077299_nat';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130101_0030_FO2_FR0_1536860294977284421';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130227_0730_FO3_FR0.mat';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%
mcla_ofile='mcsampleroutput_conv_option.mat';
nsamples_s='30';
MCLA_HELPER(mcla_ifile,m3_ofile, mcla_ofile,nsamples_s,option_sign,centering,full_dependence);

%%%%% FPF HELPER
fpfc_ofile='fea_stats_cond_for_conv_option';
FPF_HELPER(mcla_ifile,m2_ofile, fpfc_ofile,isdeterministic,isuniform,homothetic,opt_FPF,'1');
