package info.rmapproject.indexing.solr;

import org.junit.Before;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = {"default", "inmemory-triplestore", "inmemory-idservice", "inmemory-db", "http-solr", "prod-kafka"}, inheritProfiles = false)
public abstract class AbstractKafkaTest extends AbstractSpringIndexingTest {

    @Before
    @Override
    public void setUp() throws Exception {
        if (System.getProperty("spring.profiles.active") == null) {
            System.setProperty("spring.profiles.active", "default, integration-db, inmemory-triplestore, inmemory-idservice, http-solr, prod-kafka");
            thisClassSetProfilesProperty = true;
        }
    }

}
