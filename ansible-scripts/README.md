# iTESLA
http://www.itesla-project.eu/

## License
https://www.mozilla.org/en-US/MPL/2.0/

# IPST ansible scripts

This ansible playbook builds and installs IPST on a target CentOS linux. In particular:

* it installs on the target server all the OS packages, development tools and SW runtimes, needed to build and run the platform (this step can be skipped, if all the packages are already installed on the target server: ref. section Notes, below)
 - C/C++development tools (gcc, g++, make, git, wget, bzip2, zlib )
 - CMake
 - OpenMPI (sources installation + local build with flag --enable-mpi-thread-multiple)
 - Oracle JDK8 
 - Apache Maven
 - WildFly
 - DBMaria
 - MATLAB Runtime (MCR)
 - Hades
 - default data in case of itesla testing

* it clones on the target server(s) the iPST project sources from GitHub (pull if the repository already exists), 
builds the 'platform distribution' and installs it, in place.

* integrate testing data and test the ddb-load-eurostag and security-analysis tools provide by itesla

## Requirements

- target server(s) and control machine (where these scripts are executed) running a CentOS Linux distribution (tested on v6.5, v7.x), accessible via ssh
- an user with admin rights, to be able to install the above mentioned OS packages, development tools and SW runtimes 
- ansible (ref. http://docs.ansible.com/ansible/intro_installation.html#latest-release-via-yum)



## Notes

### iPST

The default target directories for iPST are (these are all configurable, see below in the usage section): 
 - $HOME/ipst_ansible/ipst  (iPST sources) 
 - $HOME/ipst_ansible/ipst-core  (iPST-core sources)
 - $HOME/ipst_ansible/ipst-entsoe (iPST-entsoe sources)
 - $HOME/ipst_ansible/thirdparty  (thirdparty libraries: boost, hdf5, etc.)
 - $HOME/itesla (iPST binaries)

ipst, ipst-core, ipst-entsoe provide a local logging file
 - << playbook_dir >>/install_ipst_<< target_machine >>.log
 - << playbook_dir >>/install_core_<< target_machine >>.log
 - << playbook_dir >>/install_entsoe_<< target_machine >>.log

SSH is used to communicate with the hosts: apparently connections do not work properly if a connection is not done once to all the servers; to fix it, run once
   ssh-keyscan server_IP or server_NAME >> ~/.ssh/known_hosts, for each target server

The installation process takes 'some time' to complete, at least for the first run when all the required SW packages
are downloaded (e.g. MATLAB MCR) and (possibly) compiled (e.g. openmpi).
iPST installation requires 2Gb of disk space;  MCR installation, if enabled, requires further 2Gb in /tmp during the installation and 2Gb in /usr


### security-analysis tool

security-analysis, ...  his usage allow check full itesla installation

the default target directories are
 - $HOME/hades2LF  (Hades binary contains)
 - $HOME/situations/DATA/IIDM/FO/2016/01/01/20160101_0030_FO5_FR0.xiidm.gz (situations datas)
 - $HOME/security-analysis-result.txt' (processing result file)

need additional local files
 - $HOME/tmp/hades/hades2LF.zip (binay files)
 - $HOME/tmp/situations/grovslb1/local/DATA/IIDM/FO/2016/01/01/20160101_0030_FO5_FR0.xiidm.gz (situations)

provide local logging file
 - << playbook_dir >>/install_hades_<< target_machine >>.log

**Remarque:** due to Hades usage, before processing, a disclaimer is prompted


### ddb-load-eurostag tool

ddb-load-eurostag, ... tool usage allow check full itesla installation

the default target directories are
 - $HOME/minimalist_DDB (datas to process)

need additional local file
 - $HOME/minimalist_DDB.zip

provide local logging file
 - << playbook_dir >>/install_ddb_<< target_machine >>.log'




   
## Usage

1. Copy the example inventory file ansible-scripts/ipst-hosts.example to ansible-scripts/ipst-hosts and edit the latter one, adding IPs / specific connection parameters of the target servers to the [ipst_hosts] section.

2. (Optional) To configure the installation procedure to use proxies (downloading OS packages, runtimes and IPST sources), 
  or to customize the installation parameters (e.g. setting iPST target installation directories, enable/disable requirements and MCR packages installations), 
  copy the ipst_hosts example group variables inventory file ansible-scripts/group_vars_ipst_hosts.example to ansible-scripts/group_vars/ipst_hosts and edit it; example:
```
# file: group_vars/ipst_hosts
---
## ipst_environment - (default is not set)
#ipst_environment:
#    http_proxy: http://proxyhttp.mydomain.com:port
#    https_proxy: https://proxyhttps.mydomain.com:port


## ipst_environment - (default is not set)
ipst_environment: {}

## install_prerequisites - (default is True)
install_prerequisites: True

## install_MCR - (Matlab Compiler Runtime, default is False)
install_MCR: False

## True force source build (default value is False)
force_build: False




##--------------------------------------------------------------------------
##-- ispt parameters

## project_home - OS target project root path, will be contain all source and binary files
##   (default value is ansible_env.HOME)
user_project_home: "{{ ansible_env.HOME }}/itesla_project"

## project_temporary - OS target project temporary file path, relative to user_project_home
##   (default value is /tmp)
user_project_temporary: "/tmp"

## log_file_prefix - prefix for all locale log file
##   (default value is {{ playbook_dir }}/install)
user_log_file_prefix: "{{ playbook_dir }}/install"

## log_file_postfix - postfix for all locale log file
##   (default value is {{ inventory_hostname+'.log' }})
user_log_file_postfix: "{{ inventory_hostname+'.log' }}"




## project_bin - OS target binary directory relative to user_project_home
##    (default value is /itesla)
user_project_bin: "/itesla"

## project_branch - github branch project
##    (default value is master)
user_project_branch: "master"

## source_root - OS target source project directory relative to user_project_home
##    (default value is /ipst_ansible)
user_source_root: "/ipst_ansible"

## ipst_source - OS target source ipst module directory relative to user_source_home
##    (default value is /ipst)
user_ipst_source: "/ipst"

## ipst_github - ipst github repository
##    (default value is https://github.com/itesla/ipst.git)
user_ipst_github: "https://github.com/itesla/ipst.git"



## core_source - OS target source core module directory relative to user_source_home
##    (default value is /ipst-core )
user_core_source: "/ipst-core"

## core_github - core github repository
##    (default value is https://github.com/itesla/ipst-core.git)
user_core_github: https://github.com/itesla/ipst-core.git



## entsoe_source - OS target source entsoe module directory relative to user_source_home
##    (default value is /ipst-entsoe)
user_entsoe_source: "/ipst-entsoe"

## entsoe_github - entsoe github repository
##    (default value is https://github.com/iTesla/ipst-entsoe.git)
user_entsoe_github: https://github.com/iTesla/ipst-entsoe.git


## source_thirdparty - OS target source thirdparty module directory relative to user_source_home
##    (default value is /thirdparty)
user_source_thirdparty: "/thirdparty"





##--------------------------------------------------------------------------
##-- define java user parameters

##- jdk8_url - define jdk url
##    (default value is http://download.oracle.com/otn-pub/java/jdk/8u112-b15/jdk-8u112-linux-x64.tar.gz)
user_jdk8_url: http://download.oracle.com/otn-pub/java/jdk/8u112-b15/jdk-8u112-linux-x64.tar.gz

##- jdk8_home - OS target jdk home
##    (default value is /HOME/ansible_user/jdk1.8.0_112)
user_jdk8_home: "{{ ansible_env.HOME }}/java/jdk1.8.0_112"

##- jdk8_log - 0 no screen debug
##    (default value is 0)
user_jdk8_log: 1



##--------------------------------------------------------------------------
##-- define maven user parameters
##    ipst.maven/defaults/main.yml file define default values

## maven_version - maven install version
##    (default value is 3.0.5)
user_maven_version: "3.0.5"

## maven_dest_path - OS target maven install path
##    (default value is /opt)
user_maven_dest_path: "/opt"

## maven_mirror - maven mirror prefix url
##    (default value is http://archive.apache.org/dist/maven/binaries)
user_maven_mirror: "http://archive.apache.org/dist/maven/binaries"

## maven_proxies - maven proxies
##    (default value is none)
#user_maven_proxies:
#    - {host: "proxyhttps.mydomain.com", port: "443", username: "username", password: "password", protocol: "https"}


##--------------------------------------------------------------------------
##-- define wildfly user options
##    ipst.wildfly/defaults/main.yml file define default values

## wildfly_url - wildfly url binary download
##    (default value is http://download.jboss.org/wildfly/8.1.0.Final/wildfly-8.1.0.Final.zip)
user_wildfly_url: http://download.jboss.org/wildfly/8.1.0.Final/wildfly-8.1.0.Final.zip

## wildfly_dest_path - OS target wildfly install path
##    (default value is ansible_env.HOME)
user_wildfly_dest_path: "{{ ansible_env.HOME }}/wildfly"




##--------------------------------------------------------------------------
## ear_name - ear name
##    (default value is iidm-ddb-ear.ear)
user_ear_name: "iidm-ddb-ear.ear"

## ear_path - ear source path, relative to the source install path
##    (default value is /iidm-ddb/iidm-ddb-ear/target)
user_ear_path: "/iidm-ddb/iidm-ddb-ear/target"


##--------------------------------------------------------------------------
## ddb-load-eurostag Part

## ddb_process - (provide database with data and test the integration, default is False)
user_ddb_process: False

## ddb_archive_name - data archive to integrate
##    (default value is minimalist_DDB.zip)
user_ddb_archive_name: "minimalist_DDB.zip"

## ddb_locale_path - locale path of the data archive
##    (default value is ~/)
user_ddb_locale_path: "~/"

## ddb_remote_path - OS target full path for the data
##    (default value is {{ ansible_env.HOME + '/minimalist_DDB' }})
user_ddb_remote_path: "{{ ansible_env.HOME + '/minimalist_DDB' }}"

## ddb_eurostag_version - define the eurostag version
##     (default value is 5.1.1)
user_ddb_eurostag_version: "5.1.1"


##--------------------------------------------------------------------------
##- security-analysis Part

## hades_process - process_hades - (install and process hades test, default is False)
user_hades_process: False

## hades_archive_name - hades binary archive name
##    (default value is hades2LF.zip)
user_hades_archive_name: hades2LF.zip

## hades_src - local hades binary path archive
##    (default value is ~/tmp/hades)
user_hades_src: "~/tmp/hades"

## hades_dst - OS target hades binary prefix destination path
##    (default value is {{ ansible_env.HOME }})
user_hades_dst: "{{ ansible_env.HOME }}/hades"

## hades_create - OS target hades binary destination path relative to user_hades_dst
##    (default value is /hades2LF)
user_hades_create: "/hades2LF"

## hades_situation_test - this is the situation to integrate to hades
##    (default value is 20160101_0030_FO5_FR0.xiidm.gz)
user_hades_situation_test: "20160101_0030_FO5_FR0.xiidm.gz"

## hades_situation_prefix - hades data prefix path, relative to user_hades_situations_prefix_src on local machine, relative to user_hades_situations_home on OS target
##    (default value is /DATA/IIDM/FO/2016/01/01)
user_hades_situation_prefix: "/DATA/IIDM/FO/2016/01/01"

## hades_situations_prefix_src - local hades data path prefix
##    (default value is ~/tmp/situations/grovslb1/local)
user_hades_situations_prefix_src: "~/tmp/situations/grovslb1/local"

## OS target situation path
##    (default value is {{ ansible_env.HOME }})
user_hades_situations_home: "{{ ansible_env.HOME }}"

## OS target hades processing result
##    (default value is {{ ansible_env.HOME+'/security-analysis-result.txt' }})
user_hades_result_file: "{{ ansible_env.HOME+'/security-analysis-result.txt' }}"

## activate a disclaimer for hades usage, if activate, process is stopped meanwhile user press [Enter] key
##    (default value is false)
user_hades_isDisclaimerPrompt: False

## the disclaimer
##    (default value is 'no disclaimer')
user_hades_disclaimer: 'Disclaimer
 Please confirm you want to ...
 Press return to continue.
 Press Ctrl+c and then "a" to abort'

```

Notes: 
- when variable install_prerequisites is set to False, installations of packages requiring admin rights are skipped (to be used when those packages are already installed on the target machines)


3. Run ansible-playbook -i ipst-hosts ./ipst.yml -u USERNAME -k

  ansible connects to the remote servers (listed in ipst-hosts file) as USERNAME (asking interactively for USERNAME's password, when needed - parameter k)
  and starts the build+installation process.

  Additional command line parameters are needed to execute tasks on the target servers that require admin rights, e.g. to install system packages;
  Ansible 
  
  - --become-method=su (or sudo) , to specify what privilege escalation tool to use (default is: sudo) 
  - --become-user=ADMINUSER  , to specify what admin user to use (default is root)
  - --ask-become-pass , interactively asks for ADMINUSER password
  
  These parameters could also be set, per host, in the inventory file ipst-hosts (examples in ipst-hosts.example); note that command line parameter names and inventory file names are different
  (e.g. ansible_become_user instead of become-user)
   
  
  More details on ansible-playbook command and parameters, here:  http://docs.ansible.com/ansible/playbooks_variables.html 
  Privileges escalation related configurations are explained here: http://docs.ansible.com/ansible/become.html

