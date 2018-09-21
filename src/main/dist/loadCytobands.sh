#!/usr/bin/env bash

# run Cytoband loading pipeline
# note: please ensure that properties/AppConfigure.xml file contains the path of source file for loading
#
. /etc/profile
APPDIR=/home/rgddata/pipelines/ChromosomePipeline
cd $APPDIR
# set variable HOST to uppercase hostname (f.e. KIRWAN, KYLE)
HOST=`hostname -s | tr '[a-z]' '[A-Z]'`

$APPDIR/_run.sh --load cytoband --map_key $1

mailx -s "[$HOST] Cytoband pipeline" mtutaj@mcw.edu < $APPDIR/logs/core.log

