package org.nuxeo.ecm.platform.usermanager.providers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestGroupsPageProvider extends NXRuntimeTestCase {

    protected static final String PROVIDER_NAME = "groups_listing";

    protected PageProviderService ppService;

    protected UserManager userManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.DATABASE.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/directory-config.xml");
        deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
                "test-usermanagerimpl/userservice-config.xml");

        ppService = Framework.getService(PageProviderService.class);
        assertNotNull(ppService);

        userManager = Framework.getService(UserManager.class);
        assertNotNull(userManager);

        initGroups();
    }

    @Override
    public void tearDown() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
        super.tearDown();
    }

    protected void initGroups() throws ClientException {
        userManager.createGroup(createGroup("group1"));
        userManager.createGroup(createGroup("group2"));
    }

    protected DocumentModel createGroup(String groupName) throws ClientException {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupName);
        return newGroup;
    }

    public void testGroupsPageProviderAllMode() throws ClientException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(GroupsPageProvider.GROUPS_LISTING_MODE_PROPERTY,
                GroupsPageProvider.ALL_MODE);
        PageProvider<DocumentModel> groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "");
        List<DocumentModel> groups = groupsProvider.getCurrentPage();
        assertNotNull(groups);
        assertEquals(4, groups.size());

        DocumentModel group = groups.get(0);
        assertEquals("administrators", group.getId());
        group = groups.get(1);
        assertEquals("group1", group.getId());
        group = groups.get(2);
        assertEquals("group2", group.getId());
        group = groups.get(3);
        assertEquals("members", group.getId());
    }

    public void testGroupsPageProviderSearchMode() throws ClientException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(GroupsPageProvider.GROUPS_LISTING_MODE_PROPERTY,
                GroupsPageProvider.SEARCH_ONLY_MODE);
        PageProvider<DocumentModel> groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "gr");
        List<DocumentModel> groups = groupsProvider.getCurrentPage();
        assertNotNull(groups);
        assertEquals(2, groups.size());
        DocumentModel group = groups.get(0);
        assertEquals("group1", group.getId());
        group = groups.get(1);
        assertEquals("group2", group.getId());
    }

}
