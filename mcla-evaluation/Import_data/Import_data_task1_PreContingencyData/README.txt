----------------------
        README
----------------------

I. Version
----------
Version V1 


II. File list
-------------

MAIN_Import.m					Main file, which should be executed.
READ_VARIABLES.m				Function to filter the branches
READ_WORKFLOW_STATES_SN.m		Function to read the pre-contingency load-flow FO states
READ_WORKFLOW_STATES_FO.m		Function to read the pre-contingency load-flow SN states
README							This file

III. Updates
------------

1. Option to disable the specification of the the region and voltage level. In this case, lines of all regions or/and all voltage level are imported.
2. The variables in which current limit is missing, are removed from each analyzed timestamp.


IV.	Steps to execute the MATLAB script
--------------------------------------

To execute the Matlab script, the following steps must be performed:
1.	Open the MATLAB file “MAIN_Import.m”
2.	Inside the code, the following configuration must be specified:
•	Path for the FO folder (in “path_FO =”);
•	Path for the SN folder (in “path_SN =”);
•	Path for the IIDM SN folder (in “base_case_path =”). Note: In the current version, the month path must be specified, meaning that no more than one month can be included in the input data when running the script.
•	Path for the CE folder with the “lienPostesCE.csv” CE file (in “CE_path=”).
•	Path for the output folder (in “out_path=”), where the output files are going to be stored.
•	The “transmission area” (in “CE =“) to only include the transmission line records with the specified “transmission area”. If not specified, this filter is not performed and therefore all the line records are considered.
•	The nominal voltage (in “Voltage = “) to only include the transmission line records with the specified nominal voltage. If not specified, no voltage filter is performed.
