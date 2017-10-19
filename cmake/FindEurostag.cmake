# - Find Eurostag
#
# Eurostag_FOUND             True if Eurostag exists, false otherwise
# Eurostag_INCLUDE_DIRS      Include path
# Eurostag_LIBRARIES         Eurostag libraries
#
# =============================================================================
# Copyright (c) 2016, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# =============================================================================

if (NOT EUROSTAG_SDK_HOME AND NOT $ENV{EUROSTAG_SDK_HOME} STREQUAL "")
    set(EUROSTAG_SDK_HOME $ENV{EUROSTAG_SDK_HOME})
endif()

if (NOT EUROSTAG_SDK_HOME AND NOT $ENV{EUROSTAG_SDK_ROOT} STREQUAL "")
    set(EUROSTAG_SDK_HOME $ENV{EUROSTAG_SDK_ROOT})
endif()

if (NOT EUROSTAG_SDK_HOME)
    message(FATAL_ERROR "Eurostag SDK not found. The variable EUROSTAG_SDK_HOME is NOT set or is NOT a valid directory")
endif()

find_path(Eurostag_INCLUDE_DIR NAME api_eurostag.h HINTS ${EUROSTAG_SDK_HOME}/include NO_DEFAULT_PATH)
mark_as_advanced(Eurostag_INCLUDE_DIR)

# Note that EUROSTAG SDK v5.1 provides only static libraries, whereas v5.2 provides only dynamic libraries.
if (EXISTS "${EUROSTAG_SDK_HOME}/lib/libeustag_esg.so")
    set(EUROSTAG_DYNAMIC_LIBS true)
    set(components
       eustag_esg
    )
else()
    set(EUROSTAG_DYNAMIC_LIBS false)
    set(components
       eustag_esg
       eustag_cpt
       eustag_a
       light_lib_a_t
       eustag_i
       eustag_s
       light_lib_s
       eustag_lf
       light_lib_t_s
       light_lib_t
       eustag_bld
       util
       klu
       amd
    )
endif()

include(FindPackageHandleStandardArgs)
foreach(component ${components})
    string(TOUPPER ${component} COMPONENT)
    set(Eurostag_${component}_FIND_QUIETLY true)

    if (EUROSTAG_DYNAMIC_LIBS)
        find_library(Eurostag_${component}_LIBRARY ${component} HINTS ${EUROSTAG_SDK_HOME}/lib NO_DEFAULT_PATH)
    else()
        find_library(Eurostag_${component}_LIBRARY lib${component}.a HINTS ${EUROSTAG_SDK_HOME}/lib NO_DEFAULT_PATH)
    endif()

    mark_as_advanced(Eurostag_${component}_LIBRARY)
    find_package_handle_standard_args(Eurostag_${component} DEFAULT_MSG Eurostag_${component}_LIBRARY)

    if (EUROSTAG_${COMPONENT}_FOUND)
        set(Eurostag_LIBRARIES ${Eurostag_LIBRARIES} ${Eurostag_${component}_LIBRARY})
    else()
        message(FATAL_ERROR "Eurostag library not found: ${component}")
    endif()

    unset(EUROSTAG_${COMPONENT}_FOUND)
    unset(COMPONENT)
    unset(Eurostag_${component}_FIND_QUIETLY)
    unset(Eurostag_${component}_LIBRARY)
endforeach()

set(Eurostag_FOUND true)
set(Eurostag_INCLUDE_DIRS ${Eurostag_INCLUDE_DIR})
message(STATUS "Eurostag SDK found: ${EUROSTAG_SDK_HOME}")
if (EUROSTAG_DYNAMIC_LIBS)
    message(STATUS "Eurostag SDK: linking to dynamic libraries")
else()
    message(STATUS "Eurostag SDK: linking to static libraries")
endif()

