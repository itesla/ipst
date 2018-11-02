----------------------
        README
----------------------

I. Version
----------
Version: 20180730


II. File list
-------------

MAIN_Import.m					Main file, which should be executed.
READ_VARIABLES.m				Function to filter the branches
READ_WORKFLOW_STATES_SN.m		Function to read the pre-contingency load-flow FO states
READ_WORKFLOW_STATES_FO.m		Function to read the pre-contingency load-flow SN states
README							This file

III. Updates
------------

1. Option to specify multiple voltage levels to filter. 
2. Option to get the load-flow varibles in one or two sides of the transmition line.
3. The variables in which current limit is missing, are removed for the current timestamp.

IV.	Steps to execute the MATLAB script
--------------------------------------

To execute the Matlab script, the following steps must be performed:
1.	Open the MATLAB file “MAIN_Import.m”
2.	Inside the code, the following configuration must be specified:
	- Path for the “FO” folder (in “path_FO =”);
	- Path for the “SN” folder (in “path_SN =”);
	- Path for the IIDM SN folder (in “base_case_path =”).
	  Note: In the current version, the month path must be specified, meaning that no more than one month can be included in the input data when running the script.
	- Path for the main folder with the CE file (in “CE_path=”).
	- Path where the output files are going to be stored (in “out_path=”).
	- Specify the transmission area CE (in “CE =“) to filter the transmission lines.
	  If none is specified, no filter is performed and therefore all the substations are considered. 
	- Specify the nominal voltage (in “Voltage = “) to filter the transmission lines.
	  If none is specified, no voltage filter is performed.
