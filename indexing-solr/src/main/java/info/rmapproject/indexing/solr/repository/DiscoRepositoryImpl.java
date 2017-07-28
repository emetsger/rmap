package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTransactionSynchronizationAdapterBuilder;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.repository.query.SolrEntityInformation;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DiscoRepositoryImpl extends SimpleSolrRepository<DiscoSolrDocument, Long> implements DiscoRepository {

    private final SolrOperations solrOperations;

    public DiscoRepositoryImpl(SolrOperations solrOperations) {
        super(solrOperations);
        this.solrOperations = solrOperations;
    }

    @Override
    public long count(Query query) {
        Query countQuery = SimpleQuery.fromQuery(query);
        return getSolrOperations().count("discos", countQuery);
    }

    @Override
    public void deleteAll() {
        registerTransactionSynchronisationIfSynchronisationActive();
        this.solrOperations.delete("discos", new SimpleFilterQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
        commitIfTransactionSynchronisationIsInactive("discos");
    }

    private void registerTransactionSynchronisationIfSynchronisationActive() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            registerTransactionSynchronisationAdapter();
        }
    }

    private void registerTransactionSynchronisationAdapter() {
        TransactionSynchronizationManager.registerSynchronization(SolrTransactionSynchronizationAdapterBuilder
                .forOperations(this.solrOperations).withDefaultBehaviour());
    }

    private void commitIfTransactionSynchronisationIsInactive() {
        commitIfTransactionSynchronisationIsInactive(null);
    }

    private void commitIfTransactionSynchronisationIsInactive(String coreName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (StringUtils.hasText(coreName)) {
                this.solrOperations.commit(coreName);
            } else {
                this.solrOperations.commit();
            }
        }
    }

}
