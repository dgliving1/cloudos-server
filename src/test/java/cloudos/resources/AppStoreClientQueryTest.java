package cloudos.resources;

import cloudos.appstore.test.AppStoreSeedData;
import org.junit.Before;

public class AppStoreClientQueryTest extends AppStoreClientTestBase {

    public static final String DOC_TARGET = "App Store API";

    public static final int NUM_ACCOUNTS = 2;
    public static final int NUM_APPS = 8;
    public static final int NUM_VERSIONS = 2;

    protected AppStoreSeedData seedData;

    @Before
    public void populateAppStore () throws Exception {
        seedData = new AppStoreSeedData(appStoreClient, adminToken, NUM_ACCOUNTS, NUM_APPS, NUM_VERSIONS);
    }

//    @Test
    public void testAppStoreDefaultQuery () throws Exception {
// fixme: disabled until we can mock out the app store server
//        apiDocs.startRecording(DOC_TARGET, "default app store query (check "+appStoreApiDocs.getBaseDir()+" for calls)");
//        appStoreApiDocs.startRecording(DOC_TARGET, "default app store query");
//
//        // query app store
//        appStoreApiDocs.addNote("search app store with default query");
//        final SearchResults<AppListing> listings = appStoreClient.searchAppStore(ResultPage.DEFAULT_PAGE);
//        assertEquals(ResultPage.DEFAULT_PAGE.getPageSize(), listings.getResults().size());
    }

}
