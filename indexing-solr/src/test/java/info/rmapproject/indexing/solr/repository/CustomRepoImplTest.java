package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.TestResourceManager;
import org.junit.Before;
import org.openrdf.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImplTest extends AbstractSpringIndexingTest {

    @Autowired
    private RDFHandler rdfHandler;

    private CustomRepoImpl underTest = new CustomRepoImpl();

    private TestResourceManager rm;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);
    }


}