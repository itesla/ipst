----------------------
        README
----------------------

I. Version
----------
Version V0 


II. File list
-------------

MAIN_Import_PostCont.m			Main file, which should be executed.
READ_VARIABLES.m				Function to filter the branches
READ_WORKFLOW_STATES_SN.m		Function to read the post-contingency load-flow FO states
READ_WORKFLOW_STATES_FO.m		Function to read the post-contingency load-flow SN states
README							This file

III. Steps to execute the MATLAB script
--------------------------------------

To execute the Matlab script, the following steps must be performed:
1.	Open the MATLAB file “MAIN_Import_PostCont.m”
2.	Inside the code, the following configuration must be specified:
•	Path for the FO folder (in “path_FO =”);
•	Path for the SN folder (in “path_SN =”);
•	Path for the CE folder with the “lienPostesCE.csv” CE file (in “CE_path=”).
•	Path for the output data (in “out_path =”), where the output files are going to be stored.
•	The “transmission area” (in “CE =“) to only include the transmission line records with the specified “transmission area”. If not specified, this filter is not performed and therefore all the line records are considered.
•	The nominal voltage (in “Voltage = “) to only include the transmission line records with the specified nominal voltages. If not specified, no voltage filter is performed.
