function [bicmin,aicmin,bes] = TestDistrib(variab,fig)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% the function tests the distributions weibull, normal, logistic, gamma, lognormal
% to fit the data
%
% INPUT
% variab = column(variable) to be fitted
% fig = if 'on', the function produces a histogram of the data in input, with
%       all the distributions tested
%
% OUTPUT
% bicmin = minimum bic of the distribution that best fits the data
% aicmin = minimum aic of the distribution that best fits the data
% bes = name of the distribution that best fits the data

rng(10);
[WeiErr] = fitdist(variab-min(variab)+0.1,'weibull');
[NormErr] = fitdist(variab,'normal');
[LogErr] = fitdist(variab,'logistic');
[GamErr] = fitdist(variab-min(variab)+0.1,'gamma');
[LoNErr] = fitdist(variab-min(variab)+0.1,'lognormal');
[GevErr] = fitdist(variab,'GeneralizedExtremeValue');
[TLSErr] = fitdist(variab,'tLocationScale');

WeiNll = WeiErr.NLogL;
WeiAIC = 2*WeiNll+2*2;
WeiBIC = 2*WeiNll+2*log(size(variab,1));
NormNll = NormErr.NLogL;
NormAIC = 2*NormNll+2*2;
NormBIC = 2*NormNll+2*log(size(variab,1));
LogNll = LogErr.NLogL;
LogAIC = 2*LogNll+2*2;
LogBIC = 2*LogNll+2*log(size(variab,1));
GamNll = GamErr.NLogL;
GamAIC = 2*GamNll+2*2;
GamBIC = 2*GamNll+2*log(size(variab,1));
LoNNll = LoNErr.NLogL;
LoNAIC = 2*LoNNll+2*2;
LoNBIC = 2*LoNNll+2*log(size(variab,1));
GevNll = GevErr.NLogL;
GevAIC = 2*GevNll+2*3;
GevBIC = 2*GevNll+3*log(size(variab,1));
TLSNll = TLSErr.NLogL;
TLSAIC = 2*TLSNll+2*3;
TLSBIC = 2*TLSNll+3*log(size(variab,1));

aic=[WeiAIC;NormAIC;LogAIC;GamAIC;LoNAIC;GevAIC;TLSAIC];
bic=[WeiBIC;NormBIC;LogBIC;GamBIC;LoNBIC;GevBIC;TLSBIC];

[bicmin,posbes] = min(bic);
aicmin=min(aic);

if strcmp(fig,'on')
    h=(max(variab)-min(variab))/100;
    x=min(variab):h:max(variab);

    % Create a histogram of the sample data
    figure;
    [n,y] = hist(variab,50);
    b = bar(y,n,'hist');
    set(b,'FaceColor',[0.5,0.5,0]);
    
    % Scale the density by the histogram area for easier display
    area = sum(n)*(y(2)-y(1));

    pdfWei = pdf(WeiErr,x-min(variab)+0.1);
    pdfNorm = pdf(NormErr,x);
    pdfLog = pdf(LogErr,x);
    pdfGam = pdf(GamErr,x-min(variab)+0.1);
    pdfLoN = pdf(LoNErr,x-min(variab)+0.1);
    pdfGev = pdf(GevErr,x);
    pdfTLS = pdf(TLSErr,x);

    % Plot the pdf of each fitted distribution
    line(x,pdfWei*area,'LineStyle','-','Color','c','LineWidth',1.5);
    hold on;
    line(x,pdfNorm*area,'LineStyle','-.','Color','b','LineWidth',1.5);
    line(x,pdfLog*area,'LineStyle','-','Color','g','LineWidth',1.5);
    line(x,pdfGam*area,'LineStyle','--','Color','r','LineWidth',1.5);
    line(x,pdfLoN*area,'LineStyle','--','Color','k','LineWidth',1.5);
    line(x,pdfGev*area,'LineStyle','--','Color','y','LineWidth',1.5);
    line(x,pdfTLS*area,'LineStyle',':','Color','r','LineWidth',1.5);
    legend('Data','Weibull','Normal','Logistic','Gamma','LogNormal','Gev','TLS');
    title(bicmin);
    xlabel('MW');
    hold off;
end
switch posbes
    case 1
        bes = 'Weibull';
    case 2
        bes = 'Normal';
    case 3
        bes = 'Logistic';
    case 4
        bes = 'Gamma';
    case 5
        bes = 'LogNormal';
    case 6
        bes = 'Gev';
    case 7
        bes = 'TLS';
    otherwise
        bes = [];
        disp ('no best fit')
end
end
