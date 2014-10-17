/**
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information.
 *
 * OrbisGIS is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.driver.solene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2gis.h2spatialapi.DriverFunction;
import org.h2gis.h2spatialapi.ProgressVisitor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.h2gis.utilities.SpatialResultSet;
import org.osgi.service.component.annotations.Component;


/**
 * Handle export to solene
 * @author Thomas Leduc
 * @author Erwan Bocher
 * @author Antoine Gourlay
 * @author Nicolas Fortin
 */
@Component
public final class CirDriver implements DriverFunction {

    public static final String DRIVER_NAME = "Solene Cir driver";
    private static final String EXTENSION = "cir";
    private static final String COORD3D_WRITTING_FORMAT = "\t%10.5f\t%10.5f\t%10.5f\r\n";

    @Override
    public IMPORT_DRIVER_TYPE getImportDriverType() {
        return IMPORT_DRIVER_TYPE.COPY;
    }

    @Override
    public String[] getImportFormats() {
        return new String[] {EXTENSION};
    }

    @Override
    public String[] getExportFormats() {
        return new String[] {EXTENSION};
    }

    @Override
    public String getFormatDescription(String format) {
        return DRIVER_NAME;
    }

    @Override
    public void exportTable(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException, IOException {
        int rowCount = 0;
        try(Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM "+tableReference)) {
             if(rs.next()) {
                 rowCount = rs.getInt(1);
             }
        }
        try(Statement st = connection.createStatement();
            SpatialResultSet rs = st.executeQuery("SELECT * FROM "+tableReference).unwrap(SpatialResultSet.class);
            PrintWriter out = new PrintWriter(new FileOutputStream(fileName))) {
            ProgressVisitor writeTask = progress.subProcess(rowCount);
            out.printf("%d %d\r\n", rowCount, rowCount);
            for (int i = 0; i < 5; i++) {
                out.printf("\t\t%d %d\r\n", 99999, 99999);
            }
            long rowIndex = 0;
            while(rs.next()) {
                Geometry g = rs.getGeometry();
                boolean applyZFilter = false;
                // Check that Geometry contain Z value
                for (Coordinate coordinate : g.getCoordinates()) {
                    if (Double.isNaN(coordinate.z)) {
                        applyZFilter = true;
                        break;
                    }
                }
                if (applyZFilter) {
                    // Compute normal will fail if Z is NaN
                    // Then set it to zero
                    g = Geometry3DUtilities.setZeroZ(g);
                }
                if (g instanceof Polygon) {
                    writeAPolygon(out, (Polygon) g, rowIndex);
                } else if (g instanceof MultiPolygon) {
                    writeAMultiPolygon(out, (MultiPolygon) g, rowIndex);
                } else {
                    throw new SQLException("Geomety "+g+" is not a (multi-)polygon !");
                }
                rowIndex++;
                writeTask.endStep();
            }
        }
    }

    private static void writeAMultiPolygon(PrintWriter out,final MultiPolygon multiPolygon,
                                    final long rowIndex) {
        final int nbOfCtrs = multiPolygon.getNumGeometries();
        out.printf("f%d %d\r\n", rowIndex + 1, nbOfCtrs);
        // the normal of the multi-polygon is set to the normal of its 1st
        // component (ie polygon)...
        writeANode(out, Geometry3DUtilities.computeNormal((Polygon) multiPolygon.getGeometryN(0)));
        for (int i = 0; i < nbOfCtrs; i++) {
            writeAContour(out, (Polygon) multiPolygon.getGeometryN(i));
        }
    }

    private static void writeAPolygon(PrintWriter out,final Polygon polygon, final long rowIndex) {
        out.printf("f%d 1\r\n", rowIndex + 1);
        writeANode(out, Geometry3DUtilities.computeNormal(polygon));
        writeAContour(out, polygon);
    }

    private static void writeAContour(PrintWriter out,final Polygon polygon) {
        final LineString shell = polygon.getExteriorRing();
        final int nbOfHoles = polygon.getNumInteriorRing();
        out.printf("c%d\r\n", nbOfHoles);
        writeALinearRing(out, shell);
        for (int i = 0; i < nbOfHoles; i++) {
            out.printf("t\r\n");
            writeALinearRing(out, polygon.getInteriorRingN(i));
        }
    }

    private static void writeALinearRing(PrintWriter out,final LineString shell) {
        final Coordinate[] nodes = shell.getCoordinates();
        out.printf("%d\r\n", nodes.length);
        for (Coordinate node : nodes) {
            writeANode(out, node);
        }
    }

    private static void writeANode(PrintWriter out,final Coordinate node) {
        if (Double.isNaN(node.z)) {
            out.printf(COORD3D_WRITTING_FORMAT, node.x, node.y, 0d);
        } else {
            out.printf(COORD3D_WRITTING_FORMAT, node.x, node.y, node.z);
        }
    }

    @Override
    public void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException, IOException {
        throw new SQLException(new UnsupportedOperationException());
    }
}
