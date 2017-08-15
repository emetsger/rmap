package info.rmapproject.indexing.solr;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"default", "inmemory-triplestore"})
@ContextConfiguration({"classpath:/rmap-indexing-solr.xml", "classpath:/spring-rmapcore-context.xml"})
public abstract class AbstractSpringIndexingTest {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSpringIndexingTest.class);

    @Before
    public void setUp() throws Exception {
        System.setProperty("spring.profiles.active", "default, inmemory-triplestore, inmemory-idservice");
    }

}
