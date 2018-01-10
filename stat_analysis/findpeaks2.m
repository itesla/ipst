function [numpicchi,massi]=findpeaks2(var,tit,fig)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
% The function finds the peaks in a distribution. 
% The function throw away the variables with discrete values.
%
% INPUT
% variab =  column to analyse
% tit = title to put on the figure, when it exist
% fig = if 'on', the function produce a figure of the distribution with the
% peack detectd
%
% OUTPUT
% numpicchi = vector with the number of peaks detected in each variable
% massi = vector with the positions and the values of the peaks detected

[y,x]=hist(var,50);

% run peakdet twice left-to-right and right-to-left
delta = 0.5;
[ymaxtab, ymintab] = peakdet(y, delta, x);
[ymaxtab2, ymintab2] = peakdet(y(end:-1:1), delta, x(end:-1:1));
ymaxtab = unique([ymaxtab; ymaxtab2],'rows');
ymintab = unique([ymintab; ymintab2],'rows');

% plot the curve and show extreme points based on number of peaks
if strcmp(fig,'si')
    figure
    plot(x,y)
    hold on
    if size(ymaxtab,1) == 2 && size(ymintab,1) == 1 % if double peak
        plot(ymintab(:,1),ymintab(:,2),'c.','markersize',30)
    elseif size(ymaxtab,1) == 1 && size(ymintab,1) == 0 % if single peak
        plot(ymaxtab(:,1),ymaxtab(:,2),'r.','markersize',30)
    else % if more (or less)
        plot(ymintab(:,1),ymintab(:,2),'c.','markersize',30)
        plot(ymaxtab(:,1),ymaxtab(:,2),'r.','markersize',30)
    end
end
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%MODIFICA PER I PICCHI
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%SEPARATI
a=1:length(ymintab);
b=length(ymaxtab);
a1=[];a2=[];
for i=1:length(a)
    if mod(i,2)==1
    a1=[a1 a(i)];
    else
    a2=[a2 a(i)];
    end
end
if (sum(ymintab(a1,2)==0) + sum(ymintab(a2,2)==0)>=b)
    massi=[];
    for i=1:length(ymaxtab)
        s=find(ymintab(:,1)<=ymaxtab(i,1));
        if ~isempty(s)
            ss=ymintab(s(end),:);
            if (ss(2)<=1 && s(end)<length(ymintab))
                if ymintab(s(end)+1,2)<=1
                    massi=[massi;ymaxtab(i,:)];
                elseif (size(ymintab,1)>=(s(end)+2) && ymintab(s(end)+2,2)<=1)
                    massi=[massi;ymaxtab(i+1,:)];
                end
            elseif ss(2)==0
                massi=[massi;ymaxtab(end,:)];
            end
        else
            massi=[ymaxtab(1,:);massi];
        end
    end
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
else
    massi=[];
    pm=find(ymaxtab(:,2)==max(ymaxtab(:,2)));
    j=1;
    for i=pm:-1:2
        if (ymaxtab(i,2)<ymaxtab(i-1,2) && i>2)
            if ymaxtab(i-2,2)<=ymaxtab(i-1,2)
                massi(j,:)=ymaxtab(i-1,:);
                j=j+1;
            end
        elseif (ymaxtab(i,2)<=ymaxtab(i-1,2) && i==2)
            massi(j,:)=ymaxtab(i-1,:);
            j=j+1;
        end
    end
    flipud(massi);
    massi=[massi;ymaxtab(pm,:)];
    j=j+1;
    for i=pm:length(ymaxtab)-1
        if (ymaxtab(i,2)<ymaxtab(i+1,2) && i+2<=length(ymaxtab))
            if ymaxtab(i+1,2)>=ymaxtab(i+2,2)
                massi(j,:)=ymaxtab(i+1,:);
                j=j+1;
            end
        elseif (ymaxtab(i,2)<=ymaxtab(i+1,2) && (i+2==length(ymaxtab) || i+1==length(ymaxtab)))
            massi(j,:)=ymaxtab(i+1,:);
            j=j+1;
        end
    end
    
end    
    % delete peaks lower than 50% of highest peak
    ok=find(massi(:,2)/max(ymaxtab(:,2))<0.05);
    if isempty(ok)==0
        massi(ok,:)=[];
    end

if strcmp(fig,'si')
    hold on
    plot(massi(:,1),massi(:,2),'xk','lineWidth',2)
    title(tit)
    hold off
end

numpicchi=size(massi,1);

end

%%
function [maxtab, mintab]=peakdet(v, delta, x)
%
% Copyright (c) 2017, RTE (http://www.rte-france.com) and RSE (http://www.rse-web.it) 
% This Source Code Form is subject to the terms of the Mozilla Public
% License, v. 2.0. If a copy of the MPL was not distributed with this
% file, You can obtain one at http://mozilla.org/MPL/2.0/.
%
%[MAXTAB, MINTAB] = PEAKDET(v, DELTA) finds the localb maxima and minima
%("peaks") in the vector v.
% otuput: 
%  MAXTAB indices in x,
%  MINTAB the found values.
%  A point is considered a maximum peak if it has the maximal value,
%  and was preceded (to the left) by a value lower by DELTA.

maxtab = [];
mintab = [];

v = v(:); % Just in case this wasn't a proper vector

if nargin < 3
  x = (1:length(v))';
else 
  x = x(:);
  if length(v)~= length(x)
    error('Input vectors v and x must have same length');
  end
end
  
if (length(delta(:)))>1
  error('Input argument DELTA must be a scalar');
end

if delta <= 0
  error('Input argument DELTA must be positive');
end

mn = Inf; mx = -Inf;
mnpos = NaN; mxpos = NaN;

lookformax = 1;

for i=1:length(v)
  this = v(i);
  if this > mx, mx = this; mxpos = x(i); end
  if this < mn, mn = this; mnpos = x(i); end
  
  if lookformax
    if this < mx-delta
      maxtab = [maxtab ; mxpos mx];
      mn = this; mnpos = x(i);
      lookformax = 0;
    end  
  else
    if this > mn+delta
      mintab = [mintab ; mnpos mn];
      mx = this; mxpos = x(i);
      lookformax = 1;
    end
  end
end
end

