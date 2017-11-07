function [pos99,pos99neg,pos90,pos90neg,pos70,pos70neg]...
    =Correlation(error,wrt,filepath)
% The function calculate the linear correlation matrix of all the variables, and
% then it writes it on a file. (Pearson's index)
%
% INPUT
% error: error matrix
% wrt: if 'on', the function will write the file Correlation.xls
% filepath: path of the folder where put the file
%
% OUTPUT
% pos99: pairs of variables correlated more than 0.99
% pos99neg: pairs of variables correlated less than -0.99
% pos90: pairs of variables correlated more than 0.90, but less than 0.99
% pos90neg: pairs of variables correlated less than -0.90, but more than
%           -0.99
% pos70: pairs of variables correlated more than 0.70, but less than 0.90
% pos70neg: pairs of variables correlated less than -0.70, but more than
%           -0.90

% cc=corrcoef(error);
cc=corrcoef(error,'rows','pairwise');% more time expansive than the row before.
cc=cc-tril(cc); %only trig sup

[row99,col99]=find(cc>=0.99);
pos99=[row99,col99];
cc(row99,col99)=0;

[row99neg,col99neg]=find(cc<=-0.99);
pos99neg=[row99neg,col99neg];
cc(row99neg,col99neg)=0;

[row90,col90]=find(cc>=0.90);
pos90=[row90,col90];
cc(row90,col90)=0;

[row90neg,col90neg]=find(cc<=-0.90);
pos90neg=[row90neg,col90neg];
cc(row90neg,col90neg)=0;

[row70,col70]=find(cc>=0.70);
pos70=[row70,col70];
cc(row70,col70)=0;

[row70neg,col70neg]=find(cc<=-0.70);
pos70neg=[row70neg,col70neg];

if strcmp(wrt,'on')==1
    warning('off');
    filename=fullfile(filepath,'Correlation.xls');
    xlswrite(filename,{'row >0','col >0','','row <0','col <0'},'|corr|>99','A1')
    if ~isempty(pos99)
        xlswrite(filename,pos99,'|corr|>99','A2')
    end
    if ~isempty(pos99neg)
        xlswrite(filename,pos99neg,'|corr|>99','D2')
    end
    xlswrite(filename,{'row >0','col >0','','row <0','col <0'},'|corr|>90','A1')
    if ~isempty(pos90)
        xlswrite(filename,pos90,'|corr|>90','A2')
    end
    if ~isempty(pos90neg)
        xlswrite(filename,pos90neg,'|corr|>90','D2')
    end
    xlswrite(filename,{'row >0','col >0','','row <0','col <0'},'|corr|>70','A1')
    if ~isempty(pos70)
        xlswrite(filename,pos70,'|corr|>70','A2')
    end
    if ~isempty(pos70neg)
        xlswrite(filename,pos70neg,'|corr|>70','D2')
    end
end
filename=fullfile(filepath,'Correlation.mat');
save(filename,'pos99','pos99neg','pos90','pos90neg','pos70','pos70neg');
end