# run Chromosome loading pipeline for an assembly with chromosomes
. /etc/profile
APPDIR=/home/rgddata/pipelines/ChromosomePipeline
# set variable HOST to uppercase hostname (f.e. TRAVIS)
HOST=`hostname -s | tr '[a-z]' '[A-Z]'`

$APPDIR/_run.sh --load chrSizes --map_key $1

mailx -s "[$HOST] Chromosome pipeline" mtutaj@mcw.edu < $APPDIR/logs/core.log

