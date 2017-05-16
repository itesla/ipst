#!/bin/bash
#

SOURCEDIR=
DESTDIR=
STARTWEEK=
NOWEEKS=

usage() { echo "Usage: $0 -c <PATH> -d <PATH> -s <STARTWEEK> -n <NOWEEKS>" 1>&2; exit 1; }

[ $# -eq 0 ] && usage
while getopts d:c:s:n:h option
do
    case "${option}"
    in
            c) SOURCEDIR=${OPTARG};;
            d) DESTDIR=${OPTARG};;
            s) STARTWEEK=${OPTARG};;
            n) NOWEEKS=${OPTARG};;
            h | *) usage
                   exit 0
                   ;;
    esac
done

[ -z "$SOURCEDIR" ] && usage
[ -z "$DESTDIR" ] && usage
[ -z "$STARTWEEK" ] && usage
[ -z "$NOWEEKS" ] && usage

[ $STARTWEEK -le 0 ] && usage
[ $NOWEEKS -le 0 ] && usage

EXTRACTDIR=$SOURCEDIR
# create the Nordic44 repository (ENTSO-E style)
i=$STARTWEEK
mkdir -p $EXTRACTDIR
while [ $i -le $NOWEEKS ]; do
    # unzip the i week file (only the CIM snapshots)
    unzip -q -d $EXTRACTDIR $SOURCEDIR/"Week"$i".zip" **/CIMSnapshots/**.xml
    # create the repository
    find $EXTRACTDIR -name "*.xml" -exec readlink -f {} \; | grep -P "[\_EQ|\_SV|\_TP]\.xml" | rev | cut -c8- | rev |sort|uniq | python toEntsoeFormat.py $DESTDIR
    # remove the original extracted directories (all the EXTRACTDIR subfolder with the N44_ prefix)
    find $EXTRACTDIR -mindepth 1 -maxdepth 1 -type d -name "N44_*" |xargs  rm -R
    i=$(($i+1))
done

