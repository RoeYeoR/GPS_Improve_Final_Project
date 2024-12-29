package Algorithm;

import GNSS.Sat;
import Geometry.Building;
import Geometry.Point3D;
import Geometry.Wall;
import Geometry.BuildingsFactory;
import ParticleFilter.Particles;
import ParticleFilter.Particle;
import Utils.KML_Generator;
import Utils.GeoUtils;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LosNlosComputer {
    // Search radius in meters - buildings within this distance will be considered for LOS/NLOS
    private static final double SEARCH_RADIUS = 100.0;  // Increased from 3.0 to 100.0 meters
    private static final double HEIGHT = 1.8; // meters - changed to match KML height

    public static void main(String[] args) throws Exception {
        System.out.println("Starting LosNlosComputer...");
        System.out.println("aaaa..");

        // 1. קריאת נקודות מקובץ ה-KML של המסלול
        System.out.println("\nStep 1: Loading route points from KML file...");
        String routeKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/routeABCDFabricated.kml";
        String buildingsKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/Esri_v0.4.kml";
        
        // קריאת נקודות המסלול מקובץ ה-KML
        List<Point3D> routePoints = BuildingsFactory.parseKML(routeKMLPath);  // קריאת נקודות המסלול
        System.out.println("Loaded " + routePoints.size() + " route points from KML");
        
        // בדיקת תקינות - יצירת קובץ KML חדש מהנקודות המקוריות
        String validationKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/route_validation.kml";
        KML_Generator.Generate_kml_from_List(routePoints, validationKMLPath, true, "redpin");  // true מציין שהנקודות כבר ב-LAT/LON
        System.out.println("Created validation KML file at: " + validationKMLPath);
        
        // 2. יצירת רשימת בניינים מקובץ ה-ESRI
        System.out.println("\nStep 2: Loading buildings from ESRI KML file: " + buildingsKMLPath);
        List<Building> buildings = BuildingsFactory.generateUTMBuildingListfromKMLfile(buildingsKMLPath);
        System.out.println("Successfully loaded " + buildings.size() + " buildings");
        
        // Validate buildings
        if (buildings.isEmpty()) {
            System.err.println("ERROR: No buildings were loaded!");
            return;
        }

       

        // 3. יצירת לווינים לבדיקה
        System.out.println("\nStep 3: Creating satellites for validation...");
        List<Sat> satellites = createSampleSatellites();
        int satsize = satellites.size();
        System.out.println("\n "+satsize+"  satellites created....");

        // Print satellite information
        for (int i = 0; i < Math.min(3, satellites.size()); i++) {
            Sat sat = satellites.get(i);
            
        }

        // יצירת רשימות לשמירת תוצאות
        List<Point3D> processedPoints = new ArrayList<>();
        List<Point3D> estimatedPoints = new ArrayList<>(); // רשימת הנקודות המשוערות
        List<Boolean> recordedLos = new ArrayList<>(); // רשימת תוצאות LOS/NLOS
        Particles particles = new Particles();  // יצירת אובייקט אחד של חלקיקים
        boolean foundFirstPoint = false; // Flag to track if we've found and written the first matching point
        
        System.out.println("\nTotal route points to process: " + routePoints.size());
        for (int i = 0; i < routePoints.size(); i++) {
            System.out.println("\n========================================");
            System.out.println("Processing point " + (i + 1) + "/" + routePoints.size());
            Point3D currentPoint = routePoints.get(i);
            
            // Convert LAT/LON to UTM for LOS calculations
            Point3D currentPointUTM = GeoUtils.convertLATLONtoUTM(currentPoint);
            
            // Calculate elevation and azimuth for each satellite relative to current point
            for (Sat satellite : satellites) {
                double[] elevAzim = calculateElevationAzimuth(currentPointUTM, satellite.getSatPosInECEF());
                satellite.setElevetion(elevAzim[0]);
                satellite.setAzimuth(elevAzim[1]);
            }
            
            // שמירת הנקודה המקורית לפלט - moved outside the if condition
            processedPoints.add(currentPoint);
            
            // אם זו לא הנקודה האחרונה, נפזר חלקיקים בין הנקודה הנוכחית לבאה
            if (i < routePoints.size() - 1) {
                Point3D nextPoint = routePoints.get(i + 1);
                Point3D nextPointUTM = GeoUtils.convertLATLONtoUTM(nextPoint);
                
                // יצירת חלקיקים בין שתי הנקודות
                particles.initParticlesBetweenPoints(currentPointUTM, nextPointUTM);
            } else {
                // עבור הנקודה האחרונה, נפזר חלקיקים סביבה
                particles = new Particles(); // יצירת אובייקט חדש
                Point3D p1 = new Point3D(
                    currentPointUTM.getX() - 10,
                    currentPointUTM.getY() - 10,
                    currentPointUTM.getZ()
                );
                Point3D p2 = new Point3D(
                    currentPointUTM.getX() + 10,
                    currentPointUTM.getY() + 10,
                    currentPointUTM.getZ()
                );
                particles.initParticles(p1, p2);
                
                // חישוב LOS/NLOS לכל החלקיקים מיד אחרי האתחול
                particles.MessureSignalFromSats(buildings, satellites);
            }
            
            // חישוב LOS/NLOS עבור הנקודה הנוכחית - using UTM coordinates
            List<Boolean> losResults = computeLosNlos(satellites, buildings, currentPointUTM);
            
            // ספירת לוויינים LOS/NLOS
            int losCount = 0;
            int nlosCount = 0;
            for (boolean isLos : losResults) {
                if (isLos) losCount++;
                else nlosCount++;
            }
            
            // שמירת תוצאות ה-LOS/NLOS
            for (Boolean los : losResults) {
                recordedLos.add(los);
            }
            
            // חישוב LOS/NLOS לכל החלקיקים - חייב להיות לפני חישוב המשקלים
            if (i < routePoints.size() - 1) {
                particles.MessureSignalFromSats(buildings, satellites);
            }
            
            // חישוב משקלים לפי התאמה לתוצאות המקוריות
            particles.ComputeWeights(losResults.toArray(new Boolean[0]));
            
            // נרמול משקלים
            particles.Normal_Weights();
            
            // ביצוע דגימה מחדש
            particles.Resample();
            
            // בחירת החלקיק עם המשקל הגבוה ביותר והמרה חזרה ל-LAT/LON
            Point3D estimatedPositionUTM = particles.GetParticleWithMaxWeight();
            Point3D estimatedPosition = GeoUtils.convertUTMtoLATLON(estimatedPositionUTM, 36);
            estimatedPoints.add(estimatedPosition);
            
            System.out.println("\nPoint " + (i + 1) + " LOS: " + losCount + " (" + 
                String.format("%.1f", (losCount * 100.0 / satellites.size())) + "%), NLOS: " + 
                nlosCount + " (" + String.format("%.1f", (nlosCount * 100.0 / satellites.size())) + 
                "%), Total Satellites: " + satellites.size());
            
            // Check if this point has exactly 6 LOS and 2 NLOS satellites
            if (losCount == 6 && nlosCount == 2) {
                System.out.println("\nFound point with 6 LOS and 2 NLOS satellites!");
                System.out.println("Point coordinates from routeABCDFabricated.kml: LAT=" + currentPoint.getX() + ", LON=" + currentPoint.getY() + ", Height=" + currentPoint.getZ());
                System.out.println("Point index in route: " + i);
                System.out.println("LOS/NLOS status for each satellite:");
                for (int j = 0; j < losResults.size(); j++) {
                    System.out.println("Satellite " + (j + 1) + ": " + (losResults.get(j) ? "LOS" : "NLOS"));
                }
                
                // Save this point and its LOS results
                //processedPoints.add(currentPoint);
                recordedLos.addAll(losResults);
                
                // Continue processing remaining points
                System.out.println("\nContinuing to process remaining points...\n");
            }
            
            // Rest of the code remains the same
        }
        
        // After processing all points, calculate average deviation
        double avgStdDev = calculateAverageDeviation(processedPoints, estimatedPoints);
        System.out.println("\nProcessed points: " + processedPoints.size() + ", Estimated points: " + estimatedPoints.size());
        System.out.println("Average Deviation: " + String.format("%.2f", avgStdDev) + " meters");
        
        // Generate LOS/NLOS visualization for the last processed point
        System.out.println("\nGenerating LOS/NLOS visualization for the last processed point...");
        String visualizationKML = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/los_nlos_visualization.kml";
        
        if (!processedPoints.isEmpty()) {
            Point3D lastPoint = processedPoints.get(processedPoints.size() - 1);
            Point3D lastPointUTM = GeoUtils.convertLATLONtoUTM(lastPoint);
            List<Boolean> lastPointLosResults = computeLosNlos(satellites, buildings, lastPointUTM);
            
            try (FileWriter writer = new FileWriter(visualizationKML)) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
                writer.write("  <Document>\n");
                writer.write("    <name>LOS/NLOS Analysis</name>\n");
                writer.write("    <description>Visualization of LOS and NLOS satellites</description>\n");
                
                // Style for LOS lines (green)
                writer.write("    <Style id=\"losStyle\">\n");
                writer.write("      <LineStyle>\n");
                writer.write("        <color>ff00ff00</color>\n");
                writer.write("        <width>3</width>\n");
                writer.write("      </LineStyle>\n");
                writer.write("      <IconStyle>\n");
                writer.write("        <scale>1.0</scale>\n");
                writer.write("      </IconStyle>\n");
                writer.write("    </Style>\n");
                
                // Style for NLOS lines (red)
                writer.write("    <Style id=\"nlosStyle\">\n");
                writer.write("      <LineStyle>\n");
                writer.write("        <color>ff0000ff</color>\n");
                writer.write("        <width>3</width>\n");
                writer.write("      </LineStyle>\n");
                writer.write("      <IconStyle>\n");
                writer.write("        <scale>1.0</scale>\n");
                writer.write("      </IconStyle>\n");
                writer.write("    </Style>\n");
                
                // Add the point
                writer.write("    <Placemark>\n");
                writer.write("      <name>Analysis Point</name>\n");
                writer.write("      <description>Last Processed Point</description>\n");
                writer.write("      <Point>\n");
                writer.write("        <altitudeMode>absolute</altitudeMode>\n");
                writer.write("        <coordinates>" + lastPoint.getY() + "," + lastPoint.getX() + "," + lastPoint.getZ() + "</coordinates>\n");
                writer.write("      </Point>\n");
                writer.write("    </Placemark>\n");
                
                // Add lines to satellites
                for (int i = 0; i < satellites.size(); i++) {
                    if (i < lastPointLosResults.size()) {
                        Sat satellite = satellites.get(i);
                        boolean isLos = lastPointLosResults.get(i);
                        
                        // Calculate satellite position with scaling
                        Point3D satPoint = satellite.getSatPosInECEF();
                        // Scale down the satellite position to be visible (about 20km above ground)
                        double scale = 20000.0; // 20km in meters
                        double satAzimuth = Math.toRadians(satellite.getAzimuth());
                        double satElevation = Math.toRadians(satellite.getElevetion());
                        
                        // Convert spherical to Cartesian coordinates
                        double x = scale * Math.cos(satElevation) * Math.sin(satAzimuth);
                        double y = scale * Math.cos(satElevation) * Math.cos(satAzimuth);
                        double z = scale * Math.sin(satElevation);
                        
                        // Add to the receiver position
                        Point3D scaledSatPoint = new Point3D(
                            lastPoint.getX() + x,
                            lastPoint.getY() + y,
                            lastPoint.getZ() + z
                        );
                        
                        // Debug output
                        System.out.println("\nSatellite " + (i + 1) + " visualization data:");
                        System.out.println("Azimuth: " + satellite.getAzimuth() + "°, Elevation: " + satellite.getElevetion() + "°");
                        System.out.println("Receiver: " + lastPoint.getY() + "," + lastPoint.getX() + "," + lastPoint.getZ());
                        System.out.println("Satellite: " + scaledSatPoint.getY() + "," + scaledSatPoint.getX() + "," + scaledSatPoint.getZ());
                        
                        writer.write("    <Placemark>\n");
                        writer.write("      <name>Satellite " + (i + 1) + " - " + (isLos ? "LOS" : "NLOS") + "</name>\n");
                        writer.write("      <description>Elevation: " + String.format("%.1f", satellite.getElevetion()) + 
                                   "°, Azimuth: " + String.format("%.1f", satellite.getAzimuth()) + "°</description>\n");
                        writer.write("      <styleUrl>#" + (isLos ? "losStyle" : "nlosStyle") + "</styleUrl>\n");
                        writer.write("      <LineString>\n");
                        writer.write("        <tessellate>1</tessellate>\n");
                        writer.write("        <altitudeMode>relativeToGround</altitudeMode>\n");
                        writer.write("        <coordinates>\n");
                        writer.write("          " + lastPoint.getY() + "," + lastPoint.getX() + "," + lastPoint.getZ() + "\n");
                        writer.write("          " + scaledSatPoint.getY() + "," + scaledSatPoint.getX() + "," + (scaledSatPoint.getZ() + 1000) + "\n");
                        writer.write("        </coordinates>\n");
                        writer.write("      </LineString>\n");
                        writer.write("    </Placemark>\n");
                        
                        // Add satellite point
                        writer.write("    <Placemark>\n");
                        writer.write("      <name>Satellite " + (i + 1) + " Position</name>\n");
                        writer.write("      <styleUrl>#" + (isLos ? "losStyle" : "nlosStyle") + "</styleUrl>\n");
                        writer.write("      <Point>\n");
                        writer.write("        <altitudeMode>relativeToGround</altitudeMode>\n");
                        writer.write("        <coordinates>" + scaledSatPoint.getY() + "," + scaledSatPoint.getX() + "," + (scaledSatPoint.getZ() + 1000) + "</coordinates>\n");
                        writer.write("      </Point>\n");
                        writer.write("    </Placemark>\n");
                    }
                }
                
                writer.write("  </Document>\n");
                writer.write("</kml>");
                
                System.out.println("Created LOS/NLOS visualization KML file at: " + visualizationKML);
            } catch (IOException e) {
                System.err.println("Error generating visualization KML: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // יצירת קבצי KML עם התוצאות
        String originalKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/original_route.kml";
        String estimatedKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/estimated_route.kml";
        
        // שמירת המסלול המקורי בצהוב
        KML_Generator.Generate_kml_from_List(processedPoints, originalKMLPath, true, "redpin");
        System.out.println("\nCreated original route KML file at: " + originalKMLPath);
        
        // שמירת המסלול המשוער באדום
        KML_Generator.Generate_kml_from_List(estimatedPoints, estimatedKMLPath, true, "yellowpin");
        System.out.println("Created estimated route KML file at: " + estimatedKMLPath);
        
        System.out.println("\n=== Performance Metrics ===");
        System.out.println("Number of Particles: " + Particles.NumberOfParticles);
        System.out.println("========================\n");
        
        System.out.println("\nProgram completed successfully!");
    }

    private static List<Sat> createSampleSatellites() {
        List<Sat> satellites = new ArrayList<>();
        
        // Read satellites from KML file
        String satellitesKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/satellites.kml";
        try {
            // Parse the KML file to get satellite positions
            List<Point3D> satPoints = BuildingsFactory.parseKML(satellitesKMLPath);
            System.out.println("Loaded " + satPoints.size() + " satellites from KML");
            
            // Create satellites with their positions from KML
            for (int i = 0; i < satPoints.size(); i++) {
                Point3D satPoint = satPoints.get(i);
                Point3D satPosECEF = GeoUtils.convertLATLONtoUTM(satPoint); // Convert to UTM coordinates
                Sat satellite = new Sat(satPosECEF, 0, 0, i + 1); // Create satellite with position
                satellites.add(satellite);
            }
        } catch (Exception e) {
            System.err.println("Error reading satellites from KML: " + e.getMessage());
            e.printStackTrace();
        }
        
        return satellites;
    }
    
    // Update satellite angles for a specific point
    private static void updateSatelliteAngles(List<Sat> satellites, Point3D point) {
        for (Sat satellite : satellites) {
            Point3D satPos = satellite.getSatPosInECEF();
            if (satPos != null) {
                double[] elevAzim = calculateElevationAzimuth(point, satPos);
                satellite.setElevetion(elevAzim[0]);
                satellite.setAzimuth(elevAzim[1]);
            }
        }
    }
    
    /**
     * Compute LOS/NLOS for each satellite relative to a receiver position and buildings
     * @param satellites List of satellites
     * @param buildings List of buildings
     * @param receiverPosition Position of the receiver
     * @return List of booleans indicating LOS (true) or NLOS (false) for each satellite
     */
    public static List<Boolean> computeLosNlos(List<Sat> satellites, List<Building> buildings, Point3D receiverPosition) {
        List<Boolean> losResults = new ArrayList<>();

        // For each satellite, check if it has LOS to the receiver
        for (Sat satellite : satellites) {
            // Use LosAlgorithm to determine if there is LOS between the receiver and satellite
            boolean hasLos = LosAlgorithm.ComputeLos(receiverPosition, buildings, satellite);
            losResults.add(hasLos);
        }

        return losResults;
    }
    
    private static double calculateAverageDeviation(List<Point3D> original, List<Point3D> estimated) {
        if (original.size() != estimated.size()) {
            System.err.println("Warning: Lists have different sizes when calculating deviation");
            return -1;
        }

        double totalDeviation = 0;
        int points = 0;

        for (int i = 0; i < original.size(); i++) {
            Point3D origPoint = original.get(i);
            Point3D estPoint = estimated.get(i);
            
            // Convert points to UTM for accurate distance calculation
            Point3D origUTM = GeoUtils.convertLATLONtoUTM(origPoint);
            Point3D estUTM = GeoUtils.convertLATLONtoUTM(estPoint);
            
            // Calculate Euclidean distance between points in meters
            double dx = origUTM.getX() - estUTM.getX();
            double dy = origUTM.getY() - estUTM.getY();
            double deviation = Math.sqrt(dx*dx + dy*dy);
            
            totalDeviation += deviation;
            points++;
        }

        return points > 0 ? totalDeviation / points : 0;
    }
    
    // Calculate elevation and azimuth angles between two points
    private static double[] calculateElevationAzimuth(Point3D from, Point3D to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        
        // Calculate horizontal distance
        double horizontalDistance = Math.sqrt(dx * dx + dy * dy);
        
        // Calculate elevation angle
        double elevation = Math.toDegrees(Math.atan2(dz, horizontalDistance));
        
        // Calculate azimuth angle
        double azimuth = Math.toDegrees(Math.atan2(dy, dx));
        if (azimuth < 0) {
            azimuth += 360;
        }
        
        return new double[]{elevation, azimuth};
    }
}
