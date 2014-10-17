package org.orbisgis.driver.solene;

import org.h2gis.h2spatial.ut.SpatialH2UT;
import org.h2gis.h2spatialapi.EmptyProgressVisitor;
import org.h2gis.utilities.SFSUtilities;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Fortin
 */
public class CirDriverTest {

    @Test
    public void exportTest() throws Exception {
        try(Connection connection = SFSUtilities.wrapConnection(SpatialH2UT.createSpatialDataBase(CirDriverTest.class.getSimpleName()));
            Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS TEST");
            st.execute("CREATE TABLE TEST(gid serial, the_geom POLYGON);");
            st.execute("INSERT INTO TEST(the_geom) VALUES('POLYGON((1 1,5 1,5 5,1 5,1 1))')");
            st.execute("INSERT INTO TEST(the_geom) VALUES('POLYGON((1 6,5 6,5 10,1 10,1 6))')");
            CirDriver cirDriver = new CirDriver();
            cirDriver.exportTable(connection, "TEST", new File("target/test.cir"), new EmptyProgressVisitor());
            assertTrue(new File("target/test.cir").exists());
        }
    }
}
