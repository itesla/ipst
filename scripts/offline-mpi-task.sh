#!/bin/bash
#
# Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

installBinDir=$(dirname $(readlink -f $0))
installDir=${installBinDir%/*}

. $installDir/etc/itools.conf

if [[ -n "$OMPI_MCA_rmaps_base_cpus_per_rank" ]]; then
    CORES=$OMPI_MCA_rmaps_base_cpus_per_rank
else
    CORES=1
fi

tmpdir=$HOME/tmp 
mkdir $tmpdir > /dev/null 2>&1

export CLASSPATH=$installDir/share/java/*:$CLASSPATH
rank=$OMPI_COMM_WORLD_RANK
if [ $rank = 0 ]; then
    if [ -z $JAVA_HOME ]; then
        echo "JAVA_HOME is not defined"
        exit -1
    fi
    export LD_LIBRARY_PATH=${installDir}/lib:$LD_LIBRARY_PATH
    [ -n "$itools_cache_dir" ] && options+="-Ditools.cache.dir=$itools_cache_dir"
    [ -n "$itools_config_dir" ] && options+=" -Ditools.config.dir=$itools_config_dir"
    [ -n "$itools_config_name" ] && options+=" -Ditools.config.name=$itools_config_name"
    options+=" -Dcom.sun.management.jmxremote.port=6666"
    options+=" -Dcom.sun.management.jmxremote.authenticate=false"
    options+=" -Dcom.sun.management.jmxremote.ssl=false"
    options+=" -Dlogback.configurationFile="
    [ -f "$itools_config_dir/logback-offline.xml" ] && options+="$itools_config_dir" || options+="$installDir/etc"
    options+="/logback-offline.xml"
    $JAVA_HOME/bin/java \
-Xmx8G \
-verbose:gc -XX:+PrintGCTimeStamps -Xloggc:$tmpdir/gc.log \
$options \
eu.itesla_project.offline.mpi.Master \
--mode=$1 \
--tmp-dir=$tmpdir \
--statistics-factory-class="com.powsybl.computation.mpi.CsvMpiStatisticsFactory" \
--statistics-db-dir=$HOME \
--statistics-db-name="statistics" \
--cores=$CORES \
--stdout-archive=$tmpdir/stdout-archive.zip
else
    # valgrind --show-reachable=yes --track-origins=yes --track-fds=yes --log-file=/tmp/val.log --error-limit=no
    mkdir $HOME/archive > /dev/null 2>&1
    rm -r $tmpdir/itools_common_${rank}* > /dev/null 2>&1
    rm -r $tmpdir/itools_job_${rank}* > /dev/null 2>&1
    rm -r $tmpdir/itools_work_${rank}* > /dev/null 2>&1
    ${installDir}/bin/slave --tmp-dir=$tmpdir --archive-dir=$HOME/archive --log-file=$tmpdir/slave.log --cores=$CORES
fi
