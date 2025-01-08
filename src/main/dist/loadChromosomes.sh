# load information for assembled chromosomes for a given assembly
# NOTE: any unplaced scaffold information is NOT loaded
#
. /etc/profile
APPDIR="/home/rgddata/pipelines/chromosome-pipeline"

# set variable HOST to uppercase hostname (f.e. TRAVIS)
HOST=`hostname -s | tr '[a-z]' '[A-Z]'`

$APPDIR/_run.sh --load chrSizes --map_key $1

mailx -s "[$HOST] Chromosome pipeline" mtutaj@mcw.edu < $APPDIR/logs/core.log

