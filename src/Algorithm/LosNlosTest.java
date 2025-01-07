package Algorithm;

import GNSS.Sat;
import Geometry.Line3D;
import Geometry.Point3D;
import Geometry.Wall;
import Utils.GeoUtils;
import Utils.KML_Generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LosNlosTest {
    private static final double WALL_HEIGHT = 100.0; // Height of the wall in meters
    private static final double OBSERVER_HEIGHT = 1.8; // Height of the observer in meters
    private static final double SATELLITE_HEIGHT = 1000.0; // Height of the satellite in meters
    private static final double SATELLITE_ELEVATION = 45.0; // Elevation angle in degrees
    private static final double SATELLITE_AZIMUTH = 45.0; // Azimuth angle in degrees (north-east)

    public static void main(String[] args) {
        try {
            // Create test point (observer) in LAT/LON coordinates (Tel Aviv area)
            Point3D testPoint = new Point3D(32.0853, 34.7818, OBSERVER_HEIGHT);
            
            // Convert to UTM for calculations
            Point3D testPointUTM = GeoUtils.convertLATLONtoUTM(testPoint);
            
            // Calculate wall position based on satellite azimuth
            double wallDistance = 30.0; // Distance from observer to wall in meters
            double wallLength = 20.0;   // Length of the wall in meters
            
            // Calculate wall position perpendicular to the line of sight
            double wallAngle = SATELLITE_AZIMUTH; // Wall should be perpendicular to line of sight
            double dx = wallDistance * Math.sin(Math.toRadians(wallAngle));
            double dy = wallDistance * Math.cos(Math.toRadians(wallAngle));
            
            // Calculate wall endpoints perpendicular to the line of sight
            double wallHalfLength = wallLength / 2.0;
            double wallDx = wallHalfLength * Math.cos(Math.toRadians(wallAngle)); // Perpendicular to line of sight
            double wallDy = -wallHalfLength * Math.sin(Math.toRadians(wallAngle)); // Perpendicular to line of sight
            
            Point3D wallCenter = new Point3D(
                testPointUTM.getX() + dx,
                testPointUTM.getY() + dy,
                0
            );
            
            Point3D wallStart = new Point3D(
                wallCenter.getX() + wallDx,
                wallCenter.getY() + wallDy,
                0  // Base of the wall
            );
            
            Point3D wallEnd = new Point3D(
                wallCenter.getX() - wallDx,
                wallCenter.getY() - wallDy,
                0  // Base of the wall
            );
            
            // Create the wall
            Wall wall = new Wall(wallStart, wallEnd);
            wall.setMaxHeight(WALL_HEIGHT);  // Set the wall height explicitly
            
            // Calculate satellite position based on elevation and azimuth
            double horizontalDistance = SATELLITE_HEIGHT * Math.tan(Math.toRadians(90 - SATELLITE_ELEVATION));
            dx = horizontalDistance * Math.sin(Math.toRadians(SATELLITE_AZIMUTH));
            dy = horizontalDistance * Math.cos(Math.toRadians(SATELLITE_AZIMUTH));
            
            Point3D satPosUTM = new Point3D(
                testPointUTM.getX() + dx,
                testPointUTM.getY() + dy,
                SATELLITE_HEIGHT
            );
            
            // Convert back to LATLON for KML
            Point3D satPosLATLON = GeoUtils.convertUTMtoLATLON(satPosUTM, 36);
            Point3D wallStartLATLON = GeoUtils.convertUTMtoLATLON(wallStart, 36);
            Point3D wallEndLATLON = GeoUtils.convertUTMtoLATLON(wallEnd, 36);

            // Create satellite object for LOS calculation
            Sat satellite = new Sat(SATELLITE_AZIMUTH, SATELLITE_ELEVATION, 0);
            
            // Calculate LOS/NLOS using ourComputeLos
            double losDistance = LosAlgorithm.ourComputeLos(testPointUTM, wall, satellite);
            
            // Calculate actual intersection point
            Line3D sightLine = new Line3D(testPointUTM, SATELLITE_AZIMUTH, SATELLITE_ELEVATION, 300);
            Point3D intersectionPoint = wall.intersectionPoint3D(sightLine);
            
            if (intersectionPoint != null) {
                // Calculate actual height difference considering elevation angle
                double heightDiff = WALL_HEIGHT - intersectionPoint.getZ();
                double actualDistance = heightDiff / Math.sin(Math.toRadians(SATELLITE_ELEVATION));
                System.out.println("Actual height difference: " + heightDiff + " meters");
                System.out.println("Required distance along line of sight: " + actualDistance + " meters");
            }
            
            // Print the result from ourComputeLos
            boolean isLos = (losDistance == -1); // -1 means no intersection (LOS)
            

            // Generate KML file
            String srcDir = new File("").getAbsolutePath();
            // Go up one level to the project root
            String projectDir = new File(srcDir).getParent();
            File kmlOutputDir = new File(projectDir, "KMLoutput");
            if (!kmlOutputDir.exists()) {
                kmlOutputDir.mkdir();
            }
            String kmlPath = new File(kmlOutputDir, "los_nlos_test.kml").getAbsolutePath();
            try (FileWriter writer = new FileWriter(kmlPath)) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
                writer.write("<Document>\n");
                
                // Style for wall
                writer.write("<Style id=\"wallStyle\">\n");
                writer.write("  <LineStyle>\n");
                writer.write("    <color>ff0000ff</color>\n");
                writer.write("    <width>2</width>\n");
                writer.write("  </LineStyle>\n");
                writer.write("  <PolyStyle>\n");
                writer.write("    <color>4d0000ff</color>\n"); // Changed opacity to make wall more visible
                writer.write("    <fill>1</fill>\n");
                writer.write("    <outline>1</outline>\n");
                writer.write("  </PolyStyle>\n");
                writer.write("</Style>\n");
                
                // Style for line of sight
                writer.write("<Style id=\"" + (isLos ? "green" : "red") + "LineStyle\">\n");
                writer.write("  <LineStyle>\n");
                writer.write("    <color>" + (isLos ? "ff00ff00" : "ff0000ff") + "</color>\n");
                writer.write("    <width>2</width>\n");
                writer.write("  </LineStyle>\n");
                writer.write("</Style>\n");

                // Add observer point
                writer.write("<Placemark>\n");
                writer.write("  <Name>Observer</Name>\n");
                writer.write("  <Point>\n");
                writer.write("    <altitudeMode>relativeToGround</altitudeMode>\n");
                writer.write("    <coordinates>" + testPoint.getY() + "," + testPoint.getX() + "," + OBSERVER_HEIGHT + "</coordinates>\n");
                writer.write("  </Point>\n");
                writer.write("</Placemark>\n");

                // Add wall as a polygon
                writer.write("<Placemark>\n");
                writer.write("  <Name>Wall</Name>\n");
                writer.write("  <styleUrl>#wallStyle</styleUrl>\n");
                writer.write("  <Polygon>\n");
                writer.write("    <extrude>1</extrude>\n");
                writer.write("    <tessellate>1</tessellate>\n");
                writer.write("    <altitudeMode>relativeToGround</altitudeMode>\n");
                writer.write("    <outerBoundaryIs>\n");
                writer.write("      <LinearRing>\n");
                writer.write("        <coordinates>\n");
                writer.write("          " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + ",0\n");
                writer.write("          " + wallEndLATLON.getY() + "," + wallEndLATLON.getX() + ",0\n");
                writer.write("          " + wallEndLATLON.getY() + "," + wallEndLATLON.getX() + "," + WALL_HEIGHT + "\n");
                writer.write("          " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + "," + WALL_HEIGHT + "\n");
                writer.write("          " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + ",0\n");
                writer.write("        </coordinates>\n");
                writer.write("      </LinearRing>\n");
                writer.write("    </outerBoundaryIs>\n");
                writer.write("  </Polygon>\n");
                writer.write("</Placemark>\n");

                // Add wall outline
                writer.write("<Placemark>\n");
                writer.write("  <Name>Wall Outline</Name>\n");
                writer.write("  <LineString>\n");
                writer.write("    <altitudeMode>relativeToGround</altitudeMode>\n");
                writer.write("    <coordinates>\n");
                writer.write("      " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + ",0\n");
                writer.write("      " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + "," + WALL_HEIGHT + "\n");
                writer.write("      " + wallEndLATLON.getY() + "," + wallEndLATLON.getX() + "," + WALL_HEIGHT + "\n");
                writer.write("      " + wallEndLATLON.getY() + "," + wallEndLATLON.getX() + ",0\n");
                writer.write("      " + wallStartLATLON.getY() + "," + wallStartLATLON.getX() + ",0\n");
                writer.write("    </coordinates>\n");
                writer.write("  </LineString>\n");
                writer.write("</Placemark>\n");

                // Add satellite point
                writer.write("<Placemark>\n");
                writer.write("  <Name>Satellite</Name>\n");
                writer.write("  <Point>\n");
                writer.write("    <altitudeMode>relativeToGround</altitudeMode>\n");
                writer.write("    <coordinates>" + satPosLATLON.getY() + "," + satPosLATLON.getX() + "," + SATELLITE_HEIGHT + "</coordinates>\n");
                writer.write("  </Point>\n");
                writer.write("</Placemark>\n");

                // Add line of sight
                writer.write("<Placemark>\n");
                writer.write("  <Name>Line of Sight</Name>\n");
                writer.write("  <styleUrl>#" + (isLos ? "green" : "red") + "LineStyle</styleUrl>\n");
                writer.write("  <LineString>\n");
                writer.write("    <altitudeMode>relativeToGround</altitudeMode>\n");
                writer.write("    <coordinates>\n");
                writer.write("      " + testPoint.getY() + "," + testPoint.getX() + "," + OBSERVER_HEIGHT + "\n");
                // Calculate the end point using elevation angle
                double endDistance = 300; // meters
                double endHeight = OBSERVER_HEIGHT + endDistance * Math.tan(Math.toRadians(SATELLITE_ELEVATION));
                Point3D endPointUTM = new Point3D(
                    testPointUTM.getX() + endDistance * Math.sin(Math.toRadians(SATELLITE_AZIMUTH)),
                    testPointUTM.getY() + endDistance * Math.cos(Math.toRadians(SATELLITE_AZIMUTH)),
                    endHeight
                );
                Point3D endPointLATLON = GeoUtils.convertUTMtoLATLON(endPointUTM, 36);
                writer.write("      " + endPointLATLON.getY() + "," + endPointLATLON.getX() + "," + endHeight + "\n");
                writer.write("    </coordinates>\n");
                writer.write("  </LineString>\n");
                writer.write("</Placemark>\n");

                writer.write("</Document>\n");
                writer.write("</kml>");
            }

            System.out.println("KML file has been generated at: " + kmlPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
