#!/bin/bash
#
# Create a core on disk and then run solr in the foreground
# arguments are: corename configdir
# To simply create a core:
#      docker run -P -d solr solr-precreate mycore
# To create a core from mounted config:
#      docker run -P -d -v $PWD/myconfig:/myconfig solr solr-precreate mycore /myconfig
# To create a core in a mounted directory:
#      mkdir mycores; chown 8983:8983 mycores
#      docker run -it --rm -P -v $PWD/mycores:/opt/solr/server/solr/mycores solr solr-precreate mycore
set -e

echo "Executing $0 $@"

if [[ "$VERBOSE" = "yes" ]]; then
    set -x
fi

. /opt/docker-solr/scripts/run-initdb

for core in ${@:1} ; do
  echo "Configuring $core ..."
  if [[ -z $SOLR_HOME ]]; then
      coresdir="/opt/solr/server/solr/mycores"
      mkdir -p $coresdir
  else
      coresdir=$SOLR_HOME
  fi
  coredir="$coresdir/$CORE"
  if [[ ! -d $coredir ]]; then
      cp -r $CONFIG_SOURCE/ $coredir
      touch "$coredir/core.properties"
      echo created "$CORE"
  else
      echo "core $CORE already exists"
  fi
done
