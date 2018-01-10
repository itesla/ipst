function [A,D]=BimodalTest(gaussmix,num)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% The Gaussian mixture is subjected to two tests, Shilling and Ashman:
% if A<0 or D<=2 the distribution is unimodal, otherwise multimodal
% 
% INPUT
% gaussmix = mixture that fit the data
% num = position of the variable (which mixture we have to select?)
%
% OUTPUT
% A = index of Shilling test
% D = index of Ashman test


% in fitgmdist, GMm.Sigma is the variance
mixt=gaussmix{1,num};
sigma=mixt.Sigma;
mu=mixt.mu;

% Schilling test
if size(mu,1)==1
    A=-1;
elseif size(mu,1)==2
    r=(sigma(1))/(sigma(2));
    S=sqrt(-2+3*r+3*r^2-2*r^3+2*(1-r+r^2)^1.5)/(sqrt(r)*(1+sqrt(r)));
    A=abs(mu(1)-mu(2))-S*(sqrt(sigma(1))-sqrt(sigma(2)));
elseif size(mu,1)>2
    combin=combnk(1:size(mu,1),2);
    for k=1:size(combin,1)
        r=(sigma(combin(k,1)))/(sigma(combin(k,2)));
        S=sqrt(-2+3*r+3*r^2-2*r^3+2*(1-r+r^2)^1.5)/(sqrt(r)*(1+sqrt(r)));
        A=abs(mu(combin(k,1))-mu(combin(k,2)))-S*(sqrt(sigma(combin(k,1)))-sqrt(sigma(combin(k,2))));
        if A>=0 
            break;
        end
    end
end


% Ashman's D test
if size(mu,1)==1
    D=-3;
elseif size(mu,1)==2
    D=sqrt(2)*abs(mu(1)-mu(2))/sqrt((sigma(1)+sigma(2)));
elseif size(mu,1)>2
    combin=combnk(1:size(mu,1),2);
    for k=1:size(combin,1)
        D=sqrt(2)*abs(mu(combin(k,1))-mu(combin(k,2)))/sqrt((sigma(combin(k,1))+sigma(combin(k,2))));
        if D>2 
            break;
        end
    end
end
