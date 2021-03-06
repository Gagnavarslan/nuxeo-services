package org.nuxeo.ecm.platform.query.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

public class TestNXQLQueryBuilder extends SQLRepositoryTestCase {

    String query;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-schemas-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
    }

    public void testBuildIsNullQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "ADVANCED_SEARCH").getWhereClause();
        SortInfo sortInfos = new SortInfo("dc:title", true);
        String[] params = { "foo" };
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

        model.setPropertyValue("search:isPresent", true);
        query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND dc:modified IS NULL AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

        // only boolean available in schema withour default value
        model.setPropertyValue("search:isPresent", false);
        query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND dc:modified IS NOT NULL AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

    }

}
