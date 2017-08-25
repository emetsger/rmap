package info.rmapproject.indexing.solr;

import info.rmapproject.core.rdfhandler.RDFHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"default", "inmemory-triplestore", "inmemory-idservice", "http-solr"})
@ContextConfiguration({"classpath:/rmap-indexing-solr.xml", "classpath:/spring-rmapcore-context.xml"})
public abstract class AbstractSpringIndexingTest {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSpringIndexingTest.class);

    @Autowired
    protected RDFHandler rdfHandler;

    private static boolean thisClassSetProfilesProperty = false;

    @Before
    public void setUp() throws Exception {
        if (System.getProperty("spring.profiles.active") == null) {
            System.setProperty("spring.profiles.active", "default, inmemory-triplestore, inmemory-idservice");
            thisClassSetProfilesProperty = true;
        }
    }

    @After
    public void tearDown() throws Exception {
        if (thisClassSetProfilesProperty) {
            System.getProperties().remove("spring.profiles.active");
        }
    }
}
