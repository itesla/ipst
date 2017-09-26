#EUROSTAG integration module


**eurostag-integration**, together with other modules prefixed by 'eurostag-' provides an integration between the platform and the EUROSTAG simulation software. 

 
##Required software:

A licensed version of the EUROSTAG software for CentOS v7 is required to compile some of the integration modules (e.g. eurostag-indexes) and actually needed to run the EUROSTAG simulator from the iPST platform. 

EUROSTAG for CentOS linux is usually released as a rpm file, to be installed by means of the OS software packages tool 'yum' (root privileges required).
Please refer to the EUROSTAG documentation and release notes to activate the software license.

Depending on EUROSTAG version (see specific requirements, below), the [Intel Fortran Compiler libraries](https://software.intel.com/en-us/articles/redistributable-libraries-for-the-intel-c-and-fortran-composer-xe-for-linux) might also be required at compile time for some integration modules (e.g. eurostag-indexes).
If needed, set the environment INTEL_HOME to the root of the Intel Fortran Compiler libraries installation path, before running the iPST platform installer scripts.

Note that the platform modules that actually require EUROSTAG are optional.
To compile iPST with the EUROSTAG integration module enabled, the platform installer needs to know the EUROSTAG SDK location (include + libraries),
either by setting the environment variable EUROSTAG_SDK_HOME or using the install.sh _--with-eurostag_  parameter.

##Assumptions:
EUROSTAG SDK includes an api_eurostag.h plus a set of static and/or dynamic libraries 

 - the api_eurostag.h file is expected to be found in the `$EUROSTAG_SDK_HOME/include` directory
 - the SDK libraries are expected to be found in the `$EUROSTAG_SDK_HOME/lib` directory

##EUROSTAG v5.1 specific installation instructions and fixes:
EUROSTAG v5.1 installation is shipped as an rpm whilst its SDK is distributed as a separate archive, to be unzipped in `$HOME/esg-devel`.
The archive might contain a single ./src (with nested subdirectories); in this case the .a/.h files must be manually moved or copied to fit the expected structure (`$EUROSTAG_SDK_HOME/include` and `$EUROSTAG_SDK_HOME/lib`)
Note: EUROSTAG v5.1 requires the installation of the **Intel Fortran Compiler** redistributable libraries.

##EUROSTAG v5.2 specific installation instructions and fixes:
The current rpm distribution for v5.2 provides an include .h file plus a dynamic library
Since the include file is in `/usr/local/include` directory and the .so file is in `usr/local/eurostag/v52_rte/x64` directory
these symbolic links must be defined 

`sudo ln -s /usr/local/eurostag/v52_rte/x64 /usr/local/eurostag/v52_rte/lib`

`sudo ln -s /usr/local/include /usr/local/eurostag/v52_rte/include`

and make EUROSTAG_SDK_HOME point to the `/usr/local/eurostag/v52_rte` directory.

Directory `/usr/local/eurostag/v52_rte/espion` is not created by the EUROSTAG installation but it must exist for some EUROSTAG commands (eustag_cpt) to work.
To create the missing directory

`sudo mkdir /usr/local/eurostag/v52_rte/espion && sudo chmod a+w /usr/local/eurostag/v52_rte/espion`

Note: EUROSTAG v5.2 does not require the installation of the **Intel Fortran Compiler** redistributable libraries.



##Platform configuration (config.xml):

###v5.1
Parameter `eurostag/eurostagHomeDir` must point to the EUROSTAG installation directory, 
e.g. `/home/itesla/eurostag_Linux_v51_iTesla`
Parameter `eurostag/eurostagCptCommandName` to be set to `eustag_cpt.e`  (default value, not required) 


###v5.2
Parameter `eurostag/eurostagHomeDir` in config.xml must point to `/usr/local/eurostag/v52_rte` (the default EUROSTAG v5.2 location)
Parameter `eurostag/eurostagCptCommandName` to be set to `eustag_cpt_noGUI.e`   

**Note: There is a "core dump" problem with the current v5.2 version of the sofware. Temporary workaround, set the `eurostag/eurostagCptCommandName`
to `ulimit -s unlimited &amp;&amp; eustag_cpt_noGUI.e`** 

 



EUROSTAG software is (C) Tractebel - RTE