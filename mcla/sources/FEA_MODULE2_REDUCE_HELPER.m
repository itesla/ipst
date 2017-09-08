%
% Copyright (c) 2017, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

% m1file: output of module1
% mspartspath: directory where module2 partial clusters files results are stored
% m2fileprefix: string prefix of module2 partial clusters files (e.g. m2_ )
% s_k : number of cluster to process  (s_k will be appended to the m2fileprefix, to build the complete module2 partial cluster filename
%   e.g. m2_0.m, m2_1.m,    ....  , m2_k.m
% ofile: output file for FEA, will contain structs module1, module2, inj_ID, flagPQ
%isdeterministics,Pload_deterministics,Qload_deterministics: condition(0/1) and parameters to perform "deterministic" variations of P and Q's from basecase
% band_uniformPLs,band_uniformQLs,band_uniformPGENs,correlation_fict_uniforms:
% bands for the uniform distributions of P, Q of loads and P from RES
% injectors, and the correlation among the two categories (load and RES)
function exitcode=FEA_MODULE2_REDUCE_HELPER(m1file, m2partspath, m2fileprefix, s_k, ofile,percpu_fict_gauss_load,percpu_fict_gauss_RES,correlationS,isdeterministics,Pload_deterministics,Qload_deterministics,band_uniformPLs,band_uniformQLs,band_uniformPGENs,correlation_fict_uniforms)
close all;
mversion='1.8.1';
disp(sprintf('wp5 - module2 reduce - version: %s', mversion));
disp(sprintf(' m1 file: %s', m1file));
disp(sprintf(' m2 parts path: %s', m2partspath));
disp(sprintf(' m2 file prefix: %s', m2fileprefix));
disp(sprintf(' k (total number of clusters): %s', s_k));
disp(sprintf(' ofile: %s', ofile));
disp(sprintf(' percpu_fict_gauss_load: %s', percpu_fict_gauss_load));
disp(sprintf(' percpu_fict_gauss_RES: %s', percpu_fict_gauss_RES));
disp(sprintf(' isdeterministic: %s', isdeterministics));
disp(sprintf(' Pload_deterministic: %s', Pload_deterministics));
disp(sprintf(' Qload_deterministic: %s', Qload_deterministics));
disp(sprintf(' band_uniformPGEN: %s', band_uniformPGENs));
disp(sprintf(' band_uniformPL: %s', band_uniformPLs));
disp(sprintf(' band_uniformQL: %s', band_uniformQLs));
disp(sprintf(' correlation_fict_uniform: %s', correlation_fict_uniforms));


moutput.errmsg='Ok';
percpu_gau_load=str2double(percpu_fict_gauss_load);
percpu_gau_RES=str2double(percpu_fict_gauss_RES);
correlation = str2double(correlationS);
isdeterministic = str2double(isdeterministics);
Pload_deterministic = str2double(Pload_deterministics);
Qload_deterministic = str2double(Qload_deterministics);
band_uniformPGEN = str2double(band_uniformPGENs);
band_uniformPL = str2double(band_uniformPLs);
band_uniformQL = str2double(band_uniformQLs);

correlation_fict_uniform = str2double(correlation_fict_uniforms);

k=str2double(s_k);
try
    load(m1file);
    for cls=0:k-1
        filename = sprintf('%s%d.mat', m2fileprefix, cls );
        filename =fullfile(m2partspath,filename);
        disp(filename);
        load(filename);
        for iout = 1:length(out1)
            mod_gaussian = out1(iout).modality_gaussian;
            mod_unif = out1(iout).modality_uniform;
            if mod_gaussian == 0 && mod_unif == 0
                if isdeterministic
                    moutput(iout).module2.allparas.stddev(1)=Pload_deterministic;
                    moutput(iout).module2.allparas.stddev(2)=Qload_deterministic;
                    moutput(iout).module2.allparas.corre = [];
                else
                    para = out2(iout).para;
                    [allparas{cls+1,1}] = para;
                    moutput(iout).module2.allparas=allparas;
                end
            else
                if mod_gaussian == 1
                    moutput(iout).module2.allparas.stddev(1)=percpu_gau_load;
                    moutput(iout).module2.allparas.stddev(2)=percpu_gau_RES;
                    moutput(iout).module2.allparas.corre = correlation;
                else
                    moutput(iout).module2.allparas.band_unif(1)= band_uniformPL;
                    moutput(iout).module2.allparas.band_unif(2)= band_uniformQL;
                    moutput(iout).module2.allparas.band_unif(3)= band_uniformPGEN;
                    moutput(iout).module2.allparas.corre = correlation_fict_uniform;
                end
            end
            moutput(iout).errmsg=out1(iout).errmsg;
            moutput(iout).module1=out1(iout).module1;
            moutput(iout).dati_condUNI = out1(iout).dati_condUNI;
            moutput(iout).dati_condMULTI = out1(iout).dati_condMULTI;
            moutput(iout).dati_Q = out1(iout).dati_Q;
            moutput(iout).dati_FPF = out1(iout).dati_FPF;
            moutput(iout).flagesistenza=out1(iout).flagesistenza;
            moutput(iout).rng_data=out1(iout).rng_data;
            moutput(iout).inj_ID=out1(iout).inj_ID;
            moutput(iout).flagPQ=out1(iout).flagPQ;
            moutput(iout).maxvalue=out1(iout).maxvalue;
            moutput(iout).mversion=out1(iout).mversion;
            moutput(iout).mod_gaussian=out1(iout).modality_gaussian;
            moutput(iout).mod_unif=out1(iout).modality_uniform;
            moutput(iout).conditional_sampling=out1(iout).conditional_sampling;
            totmoutput.out(iout) = moutput(iout);
        end
    end

    exitcode=0;
    
catch err
    moutput(1).errmsg=err.message;
    disp(getReport(err,'extended'));
    exitcode=-1;
end

save(ofile, '-struct', 'totmoutput');
% exit(exitcode);
end
