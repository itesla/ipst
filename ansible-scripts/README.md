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
 - MATLAB Runtime (MCR)

* it clones on the target server(s) the iPST project sources from GitHub (pull if the repository already exists), 
builds the 'platform distribution' and installs it, in place.

## Requirements

- target server(s) and control machine (where these scripts are executed) running a CentOS Linux distribution (tested on v6.5, v7.x), accessible via ssh
- an user with admin rights, to be able to install the above mentioned OS packages, development tools and SW runtimes 
- ansible (ref. http://docs.ansible.com/ansible/intro_installation.html#latest-release-via-yum) 


## Notes

The default target directories for iPST are (these are all configurable, see below in the usage section): 
 - $HOME/ipst_ansible/ipst  (iPST sources) 
 - $HOME/ipst_ansible/thirdparty  (thirdparty libraries: boost, hdf5, etc.)
 - $HOME/itesla (iPST binaries)

SSH is used to communicate with the hosts: apparently connections do not work properly if a connection is not done once to all the servers; to fix it, run once
   ssh-keyscan server_IP or server_NAME >> ~/.ssh/known_hosts, for each target server

The installation process takes 'some time' to complete, at least for the first run when all the required SW packages 
are downloaded (e.g. MATLAB MCR) and (possibly) compiled (e.g. openmpi). 
iPST installation requires 2Gb of disk space;  MCR installation, if enabled, requires further 2Gb in /tmp during the installation and 2Gb in /usr

   
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

## install_prerequisites - (default is True)
install_prerequisites: True


## install_MCR - (Matlab Compiler Runtime, default is False)
install_MCR: False


## sources_path - (default is {{ ansible_env.HOME }}/ipst_ansible/ipst  )
sources_path: "{{ ansible_env.HOME }}/ipst_ansible/ipst"


## thirdparty_path - (default is {{ ansible_env.HOME }}/ipst_ansible/thirdparty )
thirdparty_path: "{{ ansible_env.HOME }}/ipst_ansible/thirdparty"


## install_path - (default is {{ ansible_env.HOME }}/itesla )
install_path: "{{ ansible_env.HOME }}/itesla"


## maven_proxies - (default is not set )
#maven_proxies:
# - {host: "proxyhttps.mydomain.com", port: "443", username: "username", password: "password", protocol: "https"}

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

