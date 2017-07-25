FROM solr:6.6.0-alpine


COPY ./solr-precreate-nostart.sh /opt/docker-solr/scripts
RUN chmod 755 /opt/docker-solr/scripts/solr-precreate-nostart.sh && \
    chown solr:solr /opt/docker-solr/scripts/solr-precreate-nostart.sh
