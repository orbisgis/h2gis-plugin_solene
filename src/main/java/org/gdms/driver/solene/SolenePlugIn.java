package org.gdms.driver.solene;

import org.gdms.data.DataSourceFactory;
import org.gdms.plugins.GdmsPlugIn;

public class SolenePlugIn implements GdmsPlugIn {

       private DataSourceFactory dsf;

        @Override
        public void load(DataSourceFactory dsf) {
                this.dsf = dsf;
                dsf.getSourceManager().getDriverManager().registerDriver(CirDriver.class);
                dsf.getSourceManager().getDriverManager().registerDriver(ValDriver.class);
        }

        @Override
        public void unload() {
                dsf.getSourceManager().getDriverManager().unregisterDriver(CirDriver.DRIVER_NAME);
                dsf.getSourceManager().getDriverManager().unregisterDriver(ValDriver.DRIVER_NAME);
        }

        @Override
        public String getName() {
                return "Solene driver plugin";
        }

        @Override
        public String getVersion() {
                return "1.0";
        }
}
