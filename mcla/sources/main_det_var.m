%
% Copyright (c) 2016, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% function to run the MC sampling like approach
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Inputs:
% - generatore, nodo: data structure for nodes and generators of the grid
% containing the FORECAST VALUES for LOADS and GENERATION
% % generatore =
% %
% %     estremo_ID = terminal node ID of generator
% %     estremo = terminal node of generator
% %     codice = code of unit
% %     conn (1= available, 0 = out of service)
% %     P (actual active power, MW) from FORECAST <--
% %     Qmax (max reactive power, MVAr)
% %     Qmin (min reactive power,MVAr)
% %     Pmin (min active power, MW)
% %     Pmax (maximum active power, MW)
% %     Anom (MVA of generator)
% %     RES (1 o 2 = RES, = = conventional units)
% %     Tavv (starting time of units in second)
% %     fuel (type of fuel -> 0=RES, 1 = hydro, 2 = gas, 3 = oil, 4 = nuclear, 5 = coal)
% %     dispacc (1 = dispatchable, 0 = not dispatchable): this field is
% useful for redispatching and it is derived from fuel types on the basis
% of realistic assumptions
%
% Fields of interest of struct 'carico' are  =
%
%     node : reference to the node the load is connected to
%     conn: 1 = in service 0= out of service
%     P (actual active power, MW) from FORECAST <--
%     Q (actual reactive power, MVAr) from FORECAST <--
%
% Fields of interest of struct 'nodo' are nodo =
%
%     type (1 = slack, 2 = PQ, 3 = PV)
%     carichi: indexes referring to the loads connected to the node
%     generatori: indexes referring to the generators connected to the node
%
% - scenarios: number of MC samples to be generated , (scalar quantity)
% - dispatch: type of dispatching of conventional dispatchable generators
% (1=max power; 2= actual power; 3= inertia, 0= lumped slack )
% - type_X is the vector (dimension = Nvar) which specifies the nature of the stochastic
% injections (RES or load).
% - module 3 : outputs from previous module 3 containing uncoditioned
% samples
% - module 2: outputs from previous modules containing information about
% estiation of copula families for each pair copula
% - flagPQ: if 1, P and Q are sampled as separate variables, while if
% flagPQ=0 Q samples are derived starting from P samples, by applying a
% constant power factor
% - limits_reactive: the alpha and 1-alpha quantiles of the historical Q
% distributions, to limit Q values in case flagPQ = 0
% - opt_sign: option to avoid sign inversion in samples with respect to
% forecast
% - dati_cond: structure containing the quantities - such as correlation matrixes,
% ...- used for conditional sampling
% - y0: vector of forecasts adsigned by on-line part of MCLA
% - conditional sampling: if 1 conditional sampling is activated. if 0 ->
% uncoditioned sampling of forecast errors
% - mod_gauss -> if 1 gaussian fictitious forecast errors are adopted (used
% for validation purposes)
% - centering -> option to center conditioned samples onto the basecase
% DACF (valid only for conditional_sampling ==1)
function [PGEN PLOAD QLOAD ] = main_det_var(generatore,carico,nodo,module2)

%%%% THIS PARAMETER IS ONLY USED FOR VALIDATION PURPOSES, TO PROVIDE PLOTS
%%%% FOR VALIDATION PHASE
% additional checks: tool generates at most a number of conditional samples
% equal to unconditioned samples

%%%%%

generatore0=generatore;
carico0=carico;

idx_loads = find([carico.conn]==1);
   idx_carichi = idx_loads;
    %%% GAUSSIAN FICTITIOUS FORECAST ERRORS
    
    pres_mc=[];pl_mc=[];ql_mc=[];
    percpu_Pload = module2.allparas.stddev(1);
    percpu_Qload = module2.allparas.stddev(2);
    
    % different correlation cases
    
    for kLOAD = 1:length(idx_loads)
        
        carico(idx_loads(kLOAD)).P = carico0(idx_loads(kLOAD)).P*(1+percpu_Pload);
        carico(idx_loads(kLOAD)).Q = carico0(idx_loads(kLOAD)).Q*(1+percpu_Qload);
    end
    
    
    
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

concentrato = 0;
% save provaW3.mat

idx_conv = intersect(find([generatore.conn]==1),intersect(find([generatore.dispacc]==1),intersect(intersect(find([generatore.P]<0),find([generatore.RES]==0)),find([nodo([generatore.estremo_ID]).type] ~= 2))));%intersect(find([nodo([scenario(1).rete.generatore.nodo]).tipo]==3),intersect([find([scenario(1).rete.generatore.P]<0)],intersect([find([scenario(1).rete.generatore.RES]==0)], [scenario(1).rete.nodi([scenario(1).rete.sezioni([scenario(1).rete.stazioni([scenario(1).rete.area([find([scenario(1).rete.area.nazione]==1)]).stazioni]).sezioni]).nodi]).generatore]))); %#ok<NBRAK>
idx_conv = intersect(intersect(idx_conv,find(-[generatore.P] <= [generatore.Pmax])),find(-[generatore.P] >= [generatore.Pmin]));
gen_attivi = find([generatore.conn]==1);
PGEN=[];
PLOAD=[];
QLOAD=[];
tanfi0 = ([carico0.Q]./[carico0.P]);
idxnaninf = union(find(isnan(tanfi0)),find(isinf(tanfi0)));
tanfi0(idxnaninf)=0;
idx_gen=idx_conv;
%%% FOR ANY STOCHASTIC SCENARIOS, THE REDISPATCHING OF CONVENTIONAL
%%% GENERATION IS PERFORMED TO COMPENSATE THE POWER UNBALANCES DUE TO
%%% UNCERTAINTIES
    disp(['determistic redispatching of generating units'])

    
    
    if concentrato == 0
        
        
            sbilancio_eolico = - (-sum([carico(idx_carichi).P]) + sum([carico0(idx_carichi).P]));
        
        SB(1)=-sbilancio_eolico;

        %
        idx_marg_down=[];idx_marg_up=[];
        sbi=sbilancio_eolico;
        if sbilancio_eolico~=0
            
            
            for g=1:length(idx_gen)
                [generatore(idx_gen(g)).P] = [generatore(idx_gen(g)).P] - sbi*[generatore(idx_gen(g)).participationFactor]./sum([generatore(idx_gen).participationFactor]);
                if -generatore(idx_gen(g)).P >=  generatore(idx_gen(g)).Pmax
                    idx_marg_up = [idx_marg_up idx_gen(g)];
                end
                if -generatore(idx_gen(g)).P <=  generatore(idx_gen(g)).Pmin
                    idx_marg_down = [idx_marg_down idx_gen(g)];
                end
            end
            
            idx_marg=union(idx_marg_up,idx_marg_down);
            idx_lib=idx_gen;
            
            while isempty(idx_marg)==0 && isempty(idx_lib)==0
                
                sbi = sum([generatore(idx_marg_up).P] + [generatore(idx_marg_up).Pmax]) + sum([generatore(idx_marg_down).P]  +  [generatore(idx_marg_down).Pmin]);
                
                for g=1:length(idx_marg_up)
                    generatore(idx_marg_up(g)).P =  -generatore(idx_marg_up(g)).Pmax;
                end
                for g=1:length(idx_marg_down)
                    generatore(idx_marg_down(g)).P =  -generatore(idx_marg_down(g)).Pmin;
                end
                
                idx_lib = setdiff(idx_lib,idx_marg);
                idx_marg_down=[];idx_marg_up=[];
                for g = 1:length(idx_lib)
                    [generatore(idx_lib(g)).P] = [generatore(idx_lib(g)).P] + sbi.*[generatore(idx_lib(g)).participationFactor]./sum([generatore(idx_lib).participationFactor]);
                    
                    if -generatore(idx_lib(g)).P >=  generatore(idx_lib(g)).Pmax
                        idx_marg_up = [idx_marg_up idx_lib(g)];
                    end
                    if -generatore(idx_lib(g)).P <=  generatore(idx_lib(g)).Pmin
                        idx_marg_down = [idx_marg_down idx_lib(g)];
                    end
                    
                end
                
                
                idx_marg=union(idx_marg_up,idx_marg_down);
            end
            sbi = sum([generatore(idx_marg_up).P] + [generatore(idx_marg_up).Pmax]) + sum([generatore(idx_marg_down).P]  +  [generatore(idx_marg_down).Pmin]);
            if sbi~=0
                disp('power imbalance not completely covered by conventional generation - the residual is covered by the slack node!')
            end
        end
        
    end
    PGEN=[PGEN; [generatore(gen_attivi).P]];
    PLOAD=[PLOAD; [carico(idx_loads).P]];
    QLOAD=[QLOAD; [carico(idx_loads).Q]];
    

