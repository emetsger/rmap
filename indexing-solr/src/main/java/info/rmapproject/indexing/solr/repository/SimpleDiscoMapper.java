package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.core.rdfhandler.RDFType;
import info.rmapproject.indexing.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.stream.Collectors;

import static info.rmapproject.indexing.IndexUtils.notEmpty;
import static info.rmapproject.indexing.IndexUtils.notNull;
import static info.rmapproject.indexing.solr.repository.MappingUtils.tripleToString;
import static info.rmapproject.indexing.solr.repository.MappingUtils.triplesToRDF;

/**
 * Maps the properties of an {@code RMapDiSCO} object to fields in a Solr {@code DiscoSolrDocument}.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
class SimpleDiscoMapper implements DiscoMapper {

    @Autowired
    private RDFHandler rdfHandler;

    @Override
    public DiscoSolrDocument apply(RMapDiSCO disco, DiscoSolrDocument doc) {

        IndexUtils.assertNotNull(disco, "RMapDisco must not be null.");

        if (doc == null) {
            doc = new DiscoSolrDocument();
        }

        if (notNull(disco.getId())) {
            doc.setDiscoUri(disco.getId().getStringValue());
        }

        if (notNull(disco.getAggregatedResources()) && notEmpty(disco.getAggregatedResources())) {
            doc.setDiscoAggregatedResourceUris(
                    disco.getAggregatedResources()
                            .stream()
                            .map(URI::toString)
                            .collect(Collectors.toList())
            );
        }

        if (notNull(disco.getCreator())) {
            doc.setDiscoCreatorUri(disco.getCreator().getStringValue());
        }

        if (notNull(disco.getDescription())) {
            doc.setDiscoDescription(disco.getDescription().getStringValue());
        }

        if (notNull(disco.getProvGeneratedBy())) {
            doc.setDiscoProvenanceUri(disco.getProvGeneratedBy().getStringValue());
        }

        if (notNull(disco.getRelatedStatements())) {
            doc.setDiscoRelatedStatements(triplesToRDF(disco.getRelatedStatements(), rdfHandler, RDFType.NQUADS));
        }

        if (notNull(disco.getProviderId())) {
            doc.setDiscoProviderid(disco.getProviderId());
        }

        return doc;
    }

}
