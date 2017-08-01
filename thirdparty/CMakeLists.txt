# Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
# Copyright (c) 2016, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

cmake_minimum_required (VERSION 2.6)

project (thirdparty)

include(ExternalProject)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -Wall -std=c++0x -fPIC")

set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin)
set(LIBRARY_OUTPUT_PATH ${CMAKE_BINARY_DIR}/lib)

set(CMAKE_VERBOSE_MAKEFILE off)

if (NOT download)
    if (NOT DEFINED packs)
        set(packs "$ENV{HOME}/itesla_packs")
    endif()

    if (NOT IS_DIRECTORY "${packs}")
        message(WARNING "${packs} does not exist or is not a directory. External dependencies will be downloaded.")
        set(download true)
    endif()
endif()

if (NOT DEFINED prefix)
    message(FATAL_ERROR "The 'prefix' option is required.")
endif()

set(CMAKE_BINARY_DIR ${prefix}/build)

if (download)
    set(szip_url https://support.hdfgroup.org/ftp/lib-external/szip/2.1/src/szip-2.1.tar.gz)
    set(hdf5_url https://support.hdfgroup.org/ftp/HDF5/releases/hdf5-1.8/hdf5-1.8.13/src/hdf5-1.8.13.tar.gz)
    set(matio_url http://downloads.sourceforge.net/project/matio/matio/1.5.2/matio-1.5.2.tar.gz)
else()
    set(szip_url ${packs}/szip-2.1.tar.gz)
    set(hdf5_url ${packs}/hdf5-1.8.13.tar.gz)
    set(matio_url ${packs}/matio-1.5.2.tar.gz)
endif()

# szip
ExternalProject_Add(szip_project
    INSTALL_DIR ${prefix}/szip
    URL ${szip_url}
    CMAKE_ARGS -DCMAKE_INSTALL_PREFIX=<INSTALL_DIR> -DCMAKE_CXX_FLAGS=-fPIC -DCMAKE_LDFLAGS=-fPIC
)
ExternalProject_Get_Property(szip_project install_dir)
set(szip_install_dir ${install_dir})

# HDF5
ExternalProject_Add(hdf5_project
    DEPENDS szip_project
    INSTALL_DIR ${prefix}/hdf5
    URL ${hdf5_url}
    CONFIGURE_COMMAND <SOURCE_DIR>/configure --prefix=<INSTALL_DIR> --with-szlib=${szip_install_dir} CXXFLAGS=-fPIC LDFLAGS=-fPIC
    BUILD_COMMAND make
    INSTALL_COMMAND make install
)
ExternalProject_Get_Property(hdf5_project install_dir)
set(hdf5_install_dir ${install_dir})

# matio
ExternalProject_Add(matio_project
    DEPENDS hdf5_project
    INSTALL_DIR ${prefix}/matio
    URL ${matio_url}
    CONFIGURE_COMMAND <SOURCE_DIR>/configure --prefix=<INSTALL_DIR> --with-hdf5=${hdf5_install_dir} CXXFLAGS=-fPIC LDFLAGS=-fPIC
    BUILD_COMMAND make
    INSTALL_COMMAND make install
)
ExternalProject_Get_Property(matio_project install_dir)
set(matio_install_dir ${install_dir})
