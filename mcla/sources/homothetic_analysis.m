%%%% analisi dei risultati
clear all
close all
clc
stringa = 'mcsampleroutput_T1_PQ1_uncond_U1';%'mcsampleroutput_T1condPQ1'
load(stringa)
mcla_ifile='mcsamplerinput_20170203_0030_FO5_FR0_878299712139040043.mat';%'mcsamplerinput_20130101_0930_FO2_FR0_8205249616537077299_nat';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130101_0030_FO2_FR0_1536860294977284421';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130227_0730_FO3_FR0.mat';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130227_0230_FO3_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130225_0830_FO1_FR0_DACF_4986881446631756559';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_20130115_1830_FO2_FR0';%'mcsamplerinput_7busItesla';%
load(mcla_ifile)

idx_c = find([carico.conn]>0);
idx_g = find([generatore.conn]>0);

nations = unique({carico.nation });

figure
vect_c = [];
vect_g=[];

for ina = 1:length(nations)
    idx_cn = find(strcmp({carico(idx_c).nation},nations{ina}));
   idx_gn = find(strcmp({generatore(idx_g).nation},nations{ina}));
   idx_c1 = intersect(intersect(idx_cn,find([carico(idx_c).P]~=0)),find([carico(idx_c).Q]~=0));
   if length(idx_c1)>=1
        vect_c = [vect_c (idx_c1(1:min(2,length(idx_c1))))];
    end
    if length(idx_gn)>=1
        vect_g = [vect_g (idx_gn(1:min(2,length(idx_gn))))];
    end
    
  
try   
   subplot(4,6,1+2*(ina-1)),scatter([carico(idx_c(idx_cn)).P],std(PLOAD(:,idx_cn),0,1)),title(['std devs for P loads in nation ' nations{ina}]),xlabel('MW'),ylabel('MW')
%    subplot(8,6,2+3*(ina-1)),scatter([generatore(idx_g(idx_gn)).P],std(PGEN(:,idx_gn),0,1)),title(['std devs for P generators in nation ' nations{ina}])
   subplot(4,6,2+2*(ina-1)),scatter([carico(idx_c(idx_cn)).Q],std(QLOAD(:,idx_cn),0,1)),title(['std devs for Q loads in nation ' nations{ina}]),xlabel('MVAr'),ylabel('MVAr')
catch err
    keyboard
end
end

figure
plotmatrix(PLOAD(:,vect_c)),title('SCATTERPLOT OF P VARIABLES FOR FEW SELECTED LOADS IN EACH COUNTRY'),xlabel('MW'),ylabel('MW')

figure
plotmatrix(QLOAD(:,vect_c)),title('SCATTERPLOT OF Q VARIABLES FOR FEW SELECTED LOADS IN EACH COUNTRY'),xlabel('MVAr'),ylabel('MVAr')


