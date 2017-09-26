# - Find Szip (szlib)
#
# SZIP_FOUND             True if Szip  exists, false otherwise
# SZIP_INCLUDE_DIRS      Include path
# SZIP_LIBRARIES         Szip libraries
# SZIP_VERSION_STRING    Library version
#
# =============================================================================
# Copyright (c) 2016, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# =============================================================================

if (NOT SZIP_HOME AND NOT $ENV{SZIP_HOME} STREQUAL "")
    set(SZIP_HOME $ENV{SZIP_HOME})
endif()

if (NOT SZIP_HOME AND NOT $ENV{SZIP_ROOT} STREQUAL "")
    set(SZIP_HOME $ENV{SZIP_ROOT})
endif()

if (NOT SZIP_HOME)
    message(FATAL_ERROR "SZIP libraries not found. The variable SZIP_HOME is NOT set or is NOT a valid directory")
endif()

find_path(SZIP_INCLUDE_DIR NAME szlib.h HINTS ${SZIP_HOME}/include NO_DEFAULT_PATH)
if (USE_STATIC_LIBS)
    find_library(SZIP_LIBRARY libszip.a HINTS ${SZIP_HOME}/lib NO_DEFAULT_PATH)
else()
    find_library(SZIP_LIBRARY SZIP HINTS ${SZIP_HOME}/lib NO_DEFAULT_PATH)
endif()

mark_as_advanced(SZIP_INCLUDE_DIR SZIP_LIBRARY)

if (SZIP_INCLUDE_DIR AND EXISTS "${SZIP_INCLUDE_DIR}/szlib.h")
    set(_SZIP_VERSION_REGEX "^#define[ \t]+SZLIB_VERSION[ \t]+\"(.*)\".*$")
    file(STRINGS "${SZIP_INCLUDE_DIR}/szlib.h" _SZIP_VERSION_STRING LIMIT_COUNT 1 REGEX "${_SZIP_VERSION_REGEX}")
    if (_SZIP_VERSION_STRING)
        string(REGEX REPLACE "${_SZIP_VERSION_REGEX}" "\\1" SZIP_VERSION_STRING "${_SZIP_VERSION_STRING}")
    endif()
    unset(_SZIP_VERSION_REGEX)
    unset(_SZIP_VERSION_STRING)
endif()

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(SZIP DEFAULT_MSG SZIP_LIBRARY SZIP_INCLUDE_DIR)
if (SZIP_FOUND)

    if (DEFINED SZIP_FIND_VERSION)
        if (${SZIP_FIND_VERSION} VERSION_GREATER ${SZIP_VERSION_STRING})
            message(FATAL_ERROR "Szip ${SZIP_VERSION_STRING} found but ${SZIP_FIND_VERSION} is required")
        endif()
    endif()

    set(SZIP_FOUND ${SZIP_FOUND})
    set(SZIP_INCLUDE_DIRS ${SZIP_INCLUDE_DIR})
    set(SZIP_LIBRARIES ${SZIP_LIBRARY})
    
    message(STATUS "Szip version: ${SZIP_VERSION_STRING}")
endif()

