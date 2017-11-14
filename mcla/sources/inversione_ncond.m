% 
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
function [invA A2] = inversione_ncond(A1,ncondM,itersmax,b)

A0=A1;
x=0;
while x <=itersmax && cond(A1)>ncondM
    A1 = eye(size(A1)) + (A0 - eye(size(A1))).*exp(-x/b);
    x=x+1    
end
disp(['conditioning matrix in ' num2str(x) ' iterations'])
A2=A1;
invA=pinv(A2);
