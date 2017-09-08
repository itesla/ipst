% Copyright (c) 2017, Ricerca sul Sistema Energetico – RSE S.p.A. <itesla@rse-web.it>
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [unimodal multimodal] = find_unimodals(forec_filt,snap_filt,inj_ID)

%%%%%%%%%%%%%%%%%%%%%%%
% parameters
%%%%%%%%%%%%%%%%%%%%%%%
thres = 0.7; %percentage of elements to eliminate if they are outliers, choose between 0, 0.03, 0.07


% ************************************************************************
%   MANAGEMENT OF NANS
%**************************************************************************
disp('')
disp(' Management input ')
[fo,sn,errore,err,ID_in]=Management_variables(forec_filt,snap_filt,inj_ID);

err_all=err;
errore_all=errore;
soglia=floor(size(errore,1)*thres);



% ************************************************************************
%   OUTLIERS
%*************************************************************************
disp('')
disp(' Calculation of outliers ')
[outlier,err_sz_out_q,err_sz_out_c] = Outliers(err_all,ID_in);


% *************************************************************************
% Choose of outliers
%**************************************************************************
disp('')
disp(' Choosing outliers ')
if soglia>0
    if (exist('errore_all')==1 && exist('err_all')==1 && exist('err_sz_out_q')==1 && exist('err_sz_out_c')==1)
        [err,errore]=ChoosingOutliers(errore_all,err_all,soglia,err_sz_out_q,err_sz_out_c,outlier);
    else
        disp(' ')
        disp(' Before extract outliers you must calculate the outliers - put OUTLIERS_CALC=ON and keep results in current workspace');
        return
    end
else
    disp('****')
    disp('      Treshold = 0, any outlier is extracted')
end


% ************************************************************************
%   MULTIMODALITY
%*************************************************************************
disp('')
disp(' Calculation of multimodality')
[num_comp,~,~,~] = Multimodality(err);

unimodal=ID_in(num_comp==1);
multimodal=ID_in(num_comp>1);