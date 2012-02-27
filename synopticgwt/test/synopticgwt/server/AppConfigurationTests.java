package synopticgwt.server;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;

/**
 * Tests the server-side configuration object.
 */
public class AppConfigurationTests {
    @Test
    public void testGetInstance() throws SQLException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        AppConfiguration conf = AppConfiguration.getInstance(null);
        // NOTE: conf.analyticsTrackerID is allowed to be null.
        assertTrue(conf.modelExportsDir != null);
        assertTrue(conf.modelExportsURLprefix != null);
        assertTrue(conf.uploadedLogFilesDir != null);
        assertTrue(conf.synopticGWTChangesetID != null);
        assertTrue(conf.synopticChangesetID != null);
        assertTrue(conf.derbyDB != null);
    }
}
