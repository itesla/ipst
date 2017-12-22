#!/bin/bash
#
# Copyright (c) 2017, RTE (http://www.rte-france.com) This Source Code 
# Form is subject to the terms of the Mozilla Public License, v. 2.0. If a 
# copy of the MPL was not distributed with this file, You can obtain one at 
# http://mozilla.org/MPL/2.0/.

installBinDir=$(dirname $(readlink -f $0))
installDir=${installBinDir%/*}

. $installDir/etc/itools.conf

pidFile=$HOME/.histodb_pid

TIME=`date +%Y-%m-%d_%H_%M_%S`

export CLASSPATH=$installDir/share/java/*:$CLASSPATH

usage() {
   echo "`basename $0` {start|stop|restart|status}"
}

readPid() {
    pid=""
    if [ -f  $pidFile ]; then
        pid=`cat $pidFile`
        # check pid exists anf if not clean pid file
        kill -0 $pid > /dev/null 2>&1
        if [ $? -ne 0 ]; then
            rm -f $pidFile
        fi
    fi
}

start() {
    readPid
    if [ -n "$pid" ]; then
        echo "histodb service already started ($pid)"
    else
        logsDir=$installDir/logs
        mkdir $logsDir >> /dev/null 2>&1
        mv $logsDir/histodb.log $logsDir/histodb.log_${TIME} >> /dev/null 2>&1
        echo "starting histodb server ( execution details and errors will be logged in file: "$logsDir"/histodb.log )"
	nohup $JAVA_HOME/bin/java -jar -Xmx32G -Ditools.config.name=config ../share/java/histodb-server-0.1-SNAPSHOT-exec.jar > $logsDir/histodb.log 2>&1&
        pid=$!
        [ $? -eq 0 ] && echo $pid > $pidFile
        sleep 5
        grep -q " ERROR " $logsDir/histodb.log  && echo "errors starting the online service. Please check the log file for details:  "$logsDir"/histodb.log" || head -5 $logsDir/histodb.log
    fi
}

stop() {
    readPid
    if [ -n "$pid" ]; then
        echo "stopping histodb service"
        kill  $pid
        rm -f $pidFile
    fi
}

status() {
    readPid
    if [ -n "$pid" ]; then
        echo "histodb service is running ($pid)"
    else
        echo "histodb service is not running"
    fi
}



case "$1" in
"start")
    start
    ;;
"stop")
    stop
    ;;
"restart")
    stop
    start
    ;;
"status")
    status
    ;;
*)
    usage
    ;;
esac


