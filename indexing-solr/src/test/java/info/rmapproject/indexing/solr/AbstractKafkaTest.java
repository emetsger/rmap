package info.rmapproject.indexing.solr;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = {"default", "inmemory-triplestore", "inmemory-idservice", "inmemory-db", "http-solr", "prod-kafka"}, inheritProfiles = false)
public abstract class AbstractKafkaTest extends AbstractSpringIndexingTest {

}
