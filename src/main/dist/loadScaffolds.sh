# load information for assembled chromosomes AND unplaced scaffolds for given assembly
#
. /etc/profile
APPDIR="/home/rgddata/pipelines/chromosome-pipeline"
# set variable HOST to uppercase hostname (f.e. TRAVIS)
HOST=`hostname -s | tr '[a-z]' '[A-Z]'`

$APPDIR/_run.sh --load chrSizes --load_scaffolds --map_key $1

mailx -s "[$HOST] Chromosome pipeline" mtutaj@mcw.edu < $APPDIR/logs/core.log

