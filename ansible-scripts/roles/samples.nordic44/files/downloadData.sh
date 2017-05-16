#!/bin/bash
#

SOURCE_URL_PREFIX="https://zenodo.org/record/162907/files"

DESTDIR=
STARTWEEK=
NOWEEKS=

usage() { echo "Usage: $0 -d <PATH> -s <STARTWEEK> -n <NOWEEKS>" 1>&2; exit 1; }

[ $# -eq 0 ] && usage
while getopts d:s:n:h option
do
    case "${option}"
    in
            d) DESTDIR=${OPTARG};;
            s) STARTWEEK=${OPTARG};;
            n) NOWEEKS=${OPTARG};;
            h | *) usage
                   exit 0
                   ;;

    esac
done

[ -z "$DESTDIR" ] && usage
[ -z "$STARTWEEK" ] && usage
[ -z "$NOWEEKS" ] && usage

[ $STARTWEEK -le 0 ] && usage
[ $NOWEEKS -le 0 ] && usage

mkdir -p $DESTDIR

i=$STARTWEEK
while [ $i -le $NOWEEKS ]; do
    wget -P $DESTDIR --no-check-certificate -qc $SOURCE_URL_PREFIX"/Week"$i".zip" &
    i=$(($i+1))
done
wait
