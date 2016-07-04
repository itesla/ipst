# iTESLA
http://www.itesla-project.eu/

## License
https://www.mozilla.org/en-US/MPL/2.0/

# IPST ansible scripts

This ansible playbook builds and installs IPST on a target CentOS linux. In particular:

* it installs on the target server all the OS packages and SW runtimes, needed to build and run the platform

 - C/C++development tools (gcc, g++, make, git, etc)
 - CMake
 - OpenMPI (sources installation + local build with flag --enable-mpi-thread-multiple)
 - Oracle JDK8 
 - Apache Maven
 - MATLAB Runtime (MCR)

* it clones on the target server(s) the IPST project sources from GitHub (pull if the repository already exists), builds the 'platform distribution' and installs it, in place.

## Requirements

- target server(s) and control machine (where these scripts are executed) running a CentOS Linux distribution (tested on v6.5, v7.x)
- access to those servers via ssh
- ansible (ref. http://docs.ansible.com/ansible/intro_installation.html#latest-release-via-yum) 
- user 'itesla' must exist on the target machines ( with sudo rights, to be able to install the above mentioned packages )


## Notes

- The target directories for IPST are defined in ipst.yml file: default is 
 - /home/itesla/itesla_sources  (ipst sources)
 - /home/itesla/itesla_thirdparty (thirdparty libraries: boost, hdf5, etc.)
 - /home/itesla/itesla (ipst binaries)

- The paths where the packages are installed are declared in roles/`*`/defaults/main.yml

- The installation process takes 'some time' to complete, at least for the first run when all the required SW packages are downloaded (e.g. MATLAB MCR) and (possibly) compiled (e.g. openmpi).
 

## Usage

1. Copy file 'init-hosts.example' to 'init-hosts' and edit the latter one, adding the IPs/names of the target servers to the [ipst_hosts] section.

2. Run ansible-playbook -i ipst-hosts ./ipst.yml -u itesla -k

  ansible will connect to the remote servers using the 'itesla' user (asking for 'itesla' password, when needed)
  and start the build+installation process