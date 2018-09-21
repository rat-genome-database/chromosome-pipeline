# run Chromosome loading pipeline
. /etc/profile
APPDIR=/home/rgddata/pipelines/ChromosomePipeline
# set variable HOST to uppercase hostname (f.e. KIRWAN, KYLE)
HOST=`hostname -s | tr '[a-z]' '[A-Z]'`

$APPDIR/_run.sh --map_key $1

mailx -s "[$HOST] Chromosome pipeline" mtutaj@mcw.edu < $APPDIR/logs/core.log

