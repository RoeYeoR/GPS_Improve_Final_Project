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
import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class LosNlosComputer {
    // Search radius in meters - buildings within this distance will be considered for LOS/NLOS
    private static final double SEARCH_RADIUS = 100.0;  // Increased from 3.0 to 100.0 meters
    private static final double HEIGHT = 1.8; // meters - changed to match KML height
    private static Particles particles; // הוספת שדה עבור החלקיקים

    public static void setParticles(Particles p) {
        particles = p;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting LosNlosComputer...");

        // 1. קריאת נקודות מקובץ ה-KML של המסלול
        System.out.println("\nStep 1: Loading route points from KML file...");
        String routeKMLPath = "routeABCDFabricated.kml";
        String buildingsKMLPath = "Esri_v0.4.kml";
        
        // קריאת נקודות המסלול מקובץ ה-KML
        List<Point3D> routePoints = BuildingsFactory.parseKML(routeKMLPath);  // קריאת נקודות המסלול
        System.out.println("Loaded " + routePoints.size() + " route points from KML");
        
        // בדיקת תקינות - יצירת קובץ KML חדש מהנקודות המקוריות
        String validationKMLPath = "route_validation.kml";
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

        // 3. טעינת לווינים מקובץ KML
        System.out.println("\nStep 3: Loading satellites from KML...");
        List<Sat> satellites = loadSatellitesFromKML("satellites.kml");
        int satsize = satellites.size();
        System.out.println("\n " + satsize + " satellites loaded from KML....");

        // Print satellite information
        for (int i = 0; i < Math.min(3, satellites.size()); i++) {
            Sat sat = satellites.get(i);
            System.out.println("Satellite " + i + ": Azimuth=" + sat.getAzimuth() + "°, Elevation=" + sat.getElevetion() + "°");
        }

        // יצירת רשימות לשמירת תוצאות
        List<Point3D> processedPoints = new ArrayList<>();
        List<Point3D> estimatedPoints = new ArrayList<>(); // רשימת הנקודות המשוערות
        List<Boolean> recordedLos = new ArrayList<>(); // רשימת תוצאות LOS/NLOS
        
        System.out.println("\nTotal route points to process: " + routePoints.size());
        for (int i = 0; i < routePoints.size(); i++) {
            System.out.println("\n========================================");
            System.out.println("Processing point " + (i + 1) + "/" + routePoints.size());
            Point3D currentPoint = routePoints.get(i);
            
            // Convert LAT/LON to UTM for LOS calculations
            Point3D currentPointUTM = GeoUtils.convertLATLONtoUTM(currentPoint);
            Point3D nextPointUTM = null;
            Point3D nextPoint = null;
            
            // יצירת חלקיקים
            Point3D p1 = null;
            Point3D p2 = null;
            
            if (i < routePoints.size() - 1) {
                nextPoint = routePoints.get(i + 1);
                nextPointUTM = GeoUtils.convertLATLONtoUTM(nextPoint);
                
                // יצירת חלקיקים בין שתי הנקודות
                p1 = currentPointUTM;
                p2 = nextPointUTM;
            }
            
            // שמירת הנקודה המקורית לפלט
            processedPoints.add(new Point3D(currentPoint.getX(), currentPoint.getY(), currentPoint.getZ()));
            
            // אם זו לא הנקודה האחרונה, נפזר חלקיקים בין הנקודה הנוכחית לבאה
            Particles particles = new Particles();  // יצירת אובייקט חדש
            if (i < routePoints.size() - 1) {
                particles.initParticlesBetweenPoints(currentPointUTM, nextPointUTM);
            } else {
                // עבור הנקודה האחרונה, נפזר חלקיקים סביבה
                p1 = new Point3D(
                    currentPointUTM.getX() - 10,
                    currentPointUTM.getY() - 10,
                    currentPointUTM.getZ()
                );
                p2 = new Point3D(
                    currentPointUTM.getX() + 10,
                    currentPointUTM.getY() + 10,
                    currentPointUTM.getZ()
                );
                particles.initParticles(p1, p2);
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
            particles.MessureSignalFromSats(buildings, satellites);
            
            // חישוב משקלים לפי התאמה לתוצאות המקוריות
            particles.ComputeWeights(losResults.toArray(new Boolean[0]));
            
            // רק אחרי חישוב המשקלים, בדיקה אם החלקיקים בתוך בניינים
            particles.OutFfRegion(buildings, p1, p2);
            
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
        }
        
        // יצירת קבצי KML עם התוצאות
        String originalKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/original_route.kml";
        String estimatedKMLPath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/estimated_route.kml";
        
        // שמירת המסלול המקורי בצהוב
        KML_Generator.Generate_kml_from_List(processedPoints, "original_route.kml", true, "redpin");
        System.out.println("\nCreated original route KML file (yellow) at: " + originalKMLPath);
        
        // שמירת המסלול המשוער באדום
        KML_Generator.Generate_kml_from_List(estimatedPoints, "estimated_route.kml", true, "yellowpin");
        System.out.println("Created estimated route KML file (red) at: " + estimatedKMLPath);
        
        // יצירת קבצי KML נוספים
        // 1. קובץ KML עבור הקרניים - נייצר עבור נקודה אחת לדוגמה
        Point3D samplePoint = processedPoints.get(0);
        Point3D samplePointUTM = GeoUtils.convertLATLONtoUTM(samplePoint);
        generateRaysKML(satellites, samplePointUTM, buildings, "rays.kml");
        System.out.println("Created rays KML file at: rays.kml");
        
        double avgStdDev = calculateAverageDeviation(processedPoints, estimatedPoints);
        System.out.println("\n=== Performance Metrics ===");
        System.out.println("Number of Particles: " + Particles.NumberOfParticles);
        System.out.println("Average Deviation: " + String.format("%.2f", avgStdDev) + " meters");
        System.out.println("========================\n");
        
        // יצירת קובץ KML סופי עם כל הקווים והנקודות
        System.out.println("\nGenerating final KML file with LOS/NLOS lines...");
        String finalKMLPath = "final_output_with_lines.kml";
        generateKMLWithLines(processedPoints, satellites, buildings, finalKMLPath);
        System.out.println("Final KML file created at: " + finalKMLPath);

        System.out.println("\nProcess completed successfully!");
    }

    private static List<Sat> loadSatellitesFromKML(String filename) {
        List<Sat> satellites = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            
            NodeList placemarks = doc.getElementsByTagName("Placemark");
            for (int i = 0; i < placemarks.getLength(); i++) {
                Element placemark = (Element) placemarks.item(i);
                Element coordinates = (Element) placemark.getElementsByTagName("coordinates").item(0);
                String[] coords = coordinates.getTextContent().trim().split(",");
                
                if (coords.length >= 2) {
                    double lon = Double.parseDouble(coords[0]);
                    double lat = Double.parseDouble(coords[1]);
                    
                    // Convert geographic coordinates to azimuth and elevation
                    // Using Tel Aviv as reference point
                    double refLat = 32.0853;  // Tel Aviv latitude
                    double refLon = 34.7818;  // Tel Aviv longitude
                    
                    // Calculate azimuth and elevation
                    double dLon = Math.toRadians(lon - refLon);
                    double lat1 = Math.toRadians(refLat);
                    double lat2 = Math.toRadians(lat);
                    
                    // Calculate azimuth
                    double y = Math.sin(dLon) * Math.cos(lat2);
                    double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
                    double azimuth = Math.toDegrees(Math.atan2(y, x));
                    azimuth = (azimuth + 360) % 360; // Convert to 0-360 range
                    
                    // Calculate elevation
                    double distance = 20200000; // 20,200 km in meters
                    double elevation = Math.toDegrees(Math.atan2(distance, 6371000)); // Assuming Earth radius of 6371 km
                    
                    // Create new satellite using the constructor with azimuth, elevation, and ID
                    satellites.add(new Sat(azimuth, elevation, i));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading satellites from KML: " + e.getMessage());
            e.printStackTrace();
        }
        return satellites;
    }

    private static List<Sat> createSampleSatellites() {
        List<Sat> satellites = new ArrayList<>();
        
        // יצירת רשת של לווינים בזוויות שונות
        for (int elevation = 15; elevation <= 90; elevation += 15) {
            for (int azimuth = 1; azimuth <= 360; azimuth += 45) {
                satellites.add(new Sat(satellites.size() + 1, elevation, azimuth));
            }
        }
        
        return satellites;
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
            Point3D estUTM = GeoUtils.convertUTMtoLATLON(estPoint, 36);
            
            // Calculate Euclidean distance between points in meters
            double dx = origUTM.getX() - estUTM.getX();
            double dy = origUTM.getY() - estUTM.getY();
            double deviation = Math.sqrt(dx*dx + dy*dy);
            
            totalDeviation += deviation;
            points++;
        }

        return points > 0 ? totalDeviation / points : 0;
    }
    
    private static Point3D calculateSatellitePosition(Sat sat, Point3D referencePoint) {
        double distance = 20200000; // 20,200 km in meters
        double elevRad = Math.toRadians(sat.getElevetion());
        double azimRad = Math.toRadians(sat.getAzimuth());
        
        // Convert from geographic azimuth (clockwise from North) to mathematical angle (counterclockwise from East)
        double mathAzimuth = (450 - sat.getAzimuth()) % 360;
        double mathAzimuthRad = Math.toRadians(mathAzimuth);
        
        // Calculate satellite position relative to reference point using mathematical angle
        double x = referencePoint.getX() + distance * Math.cos(elevRad) * Math.cos(mathAzimuthRad);
        double y = referencePoint.getY() + distance * Math.cos(elevRad) * Math.sin(mathAzimuthRad);
        double z = referencePoint.getZ() + distance * Math.sin(elevRad);
        
        return new Point3D(x, y, z);
    }
    
    private static void generateKMLWithLines(List<Point3D> processedPoints, List<Sat> satellites, List<Building> buildings, String filename) {
        try {
            // First read satellite positions from satellites.kml
            Map<String, Point3D> satellitePositions = new HashMap<>();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File("satellites.kml"));
            doc.getDocumentElement().normalize();

            NodeList placemarks = doc.getElementsByTagName("Placemark");
            for (int i = 0; i < placemarks.getLength(); i++) {
                Node placemark = placemarks.item(i);
                if (placemark.getNodeType() == Node.ELEMENT_NODE) {
                    Element placemarkElement = (Element) placemark;
                    String name = placemarkElement.getElementsByTagName("name").item(0).getTextContent();
                    NodeList coordinates = placemarkElement.getElementsByTagName("coordinates");
                    if (coordinates.getLength() > 0) {
                        String[] coords = coordinates.item(0).getTextContent().trim().split(",");
                        
                        if (coords.length == 3) {
                            double lon = Double.parseDouble(coords[0]);
                            double lat = Double.parseDouble(coords[1]);
                            double alt = Double.parseDouble(coords[2]);
                            satellitePositions.put(name, new Point3D(lat, lon, alt));
                        }
                    }
                }
            }

            // Find a point with 4 LOS and 4 NLOS satellites
            Point3D selectedPoint = findPointWith4LOS4NLOS(processedPoints, buildings, satellitePositions);
            if (selectedPoint == null) {
                System.out.println("Could not find a point with exactly 4 LOS and 4 NLOS satellites. Using first point instead.");
                selectedPoint = processedPoints.get(0);
            }

            // Now generate the KML file with lines
            FileWriter writer = new FileWriter(filename);
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
            writer.write("<Document>\n");
            
            // Add styles
            writer.write("<Style id=\"losStyle\">\n");
            writer.write("  <LineStyle>\n");
            writer.write("    <color>ff00ff00</color>\n"); // Green
            writer.write("    <width>2</width>\n");
            writer.write("  </LineStyle>\n");
            writer.write("</Style>\n");
            
            writer.write("<Style id=\"nlosStyle\">\n");
            writer.write("  <LineStyle>\n");
            writer.write("    <color>ff0000ff</color>\n"); // Red
            writer.write("    <width>2</width>\n");
            writer.write("  </LineStyle>\n");
            writer.write("</Style>\n");
            
            writer.write("<Placemark>\n");
            writer.write("  <name>Selected Point</name>\n");
            writer.write("  <Point>\n");
            writer.write("    <coordinates>" + selectedPoint.getY() + "," + selectedPoint.getX() + "," + selectedPoint.getZ() + "</coordinates>\n");
            writer.write("  </Point>\n");
            writer.write("</Placemark>\n");

            // Convert point to UTM for LOS calculations
            Point3D selectedPointUTM = GeoUtils.convertLATLONtoUTM(selectedPoint);

            System.out.println("\nLOS/NLOS Status for Selected Point:");
            System.out.println("Point coordinates: LAT=" + selectedPoint.getX() + ", LON=" + selectedPoint.getY() + ", ALT=" + selectedPoint.getZ());
            System.out.println("UTM coordinates: X=" + selectedPointUTM.getX() + ", Y=" + selectedPointUTM.getY() + ", Z=" + selectedPointUTM.getZ());
            System.out.println("\nSatellite Status:");

            int losCount = 0;
            int nlosCount = 0;

            // Add lines to each satellite
            for (Map.Entry<String, Point3D> entry : satellitePositions.entrySet()) {
                String satName = entry.getKey();
                Point3D satPoint = entry.getValue();
                Point3D satPointUTM = GeoUtils.convertLATLONtoUTM(satPoint);
                
                boolean isLos = LosAlgorithm.ComputeLosWithPosition(selectedPointUTM, buildings, satPointUTM);
                
                if (isLos) losCount++;
                else nlosCount++;
                
                System.out.println(satName + ": " + (isLos ? "LOS" : "NLOS") + 
                    " (Satellite Position: LAT=" + satPoint.getX() + 
                    ", LON=" + satPoint.getY() + 
                    ", ALT=" + satPoint.getZ() + ")");
                
                writer.write("<Placemark>\n");
                writer.write("  <name>Line to " + satName + "</name>\n");
                writer.write("  <styleUrl>#" + (isLos ? "losStyle" : "nlosStyle") + "</styleUrl>\n");
                writer.write("  <LineString>\n");
                writer.write("    <altitudeMode>absolute</altitudeMode>\n");
                writer.write("    <coordinates>\n");
                writer.write("      " + selectedPoint.getY() + "," + selectedPoint.getX() + "," + selectedPoint.getZ() + "\n"); // Selected point
                writer.write("      " + satPoint.getY() + "," + satPoint.getX() + "," + satPoint.getZ() + "\n"); // Satellite point
                writer.write("    </coordinates>\n");
                writer.write("  </LineString>\n");
                writer.write("</Placemark>\n");
            }

            System.out.println("\nSelected Point LOS: " + losCount + " (" + 
                String.format("%.1f", (losCount * 100.0 / satellitePositions.size())) + "%), NLOS: " + 
                nlosCount + " (" + String.format("%.1f", (nlosCount * 100.0 / satellitePositions.size())) + 
                "%), Total Satellites: " + satellitePositions.size());

            writer.write("</Document>\n");
            writer.write("</kml>");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Point3D findPointWith4LOS4NLOS(List<Point3D> routePoints, List<Building> buildings, Map<String, Point3D> satellitePositions) {
        for (Point3D point : routePoints) {
            Point3D pointUTM = GeoUtils.convertLATLONtoUTM(point);
            int losCount = 0;
            int nlosCount = 0;
            
            // Check each satellite
            for (Point3D satPoint : satellitePositions.values()) {
                Point3D satPointUTM = GeoUtils.convertLATLONtoUTM(satPoint);
                boolean isLos = LosAlgorithm.ComputeLosWithPosition(pointUTM, buildings, satPointUTM);
                if (isLos) losCount++;
                else nlosCount++;
            }
            
            // If this point has exactly 4 LOS and 4 NLOS satellites
            if (losCount == 4 && nlosCount == 4) {
                return point;
            }
        }
        return null; // If no point found with exactly 4 LOS and 4 NLOS
    }

    private static void generateRaysKML(List<Sat> satellites, Point3D receiverPos, List<Building> buildings, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
            writer.write("<Document>\n");
            
            // Styles for LOS and NLOS rays
            writer.write("<Style id=\"losStyle\">\n");
            writer.write("  <LineStyle>\n");
            writer.write("    <color>ff00ff00</color>\n"); // Green
            writer.write("    <width>2</width>\n");
            writer.write("  </LineStyle>\n");
            writer.write("</Style>\n");
            
            writer.write("<Style id=\"nlosStyle\">\n");
            writer.write("  <LineStyle>\n");
            writer.write("    <color>ff0000ff</color>\n"); // Red
            writer.write("    <width>2</width>\n");
            writer.write("  </LineStyle>\n");
            writer.write("</Style>\n");

            // Get the particle with highest weight from the first point
            Point3D particlePos = receiverPos;  // Default to receiver position if no particles
            if (particles != null && !particles.getParticleList().isEmpty()) {
                double maxWeight = -1;
                Particle bestParticle = null;
                for (Particle p : particles.getParticleList()) {
                    if (p.getWeight() > maxWeight) {
                        maxWeight = p.getWeight();
                        bestParticle = p;
                    }
                }
                if (bestParticle != null) {
                    particlePos = bestParticle.pos;
                }
            }
            
            // Convert particle position to LAT/LON
            Point3D particleLatLon = GeoUtils.convertUTMtoLATLON(particlePos, 36);
            
            // Add a line for each satellite from the particle position
            for (Sat sat : satellites) {
                // Calculate satellite position relative to particle position
                Point3D satPoint = calculateSatellitePosition(sat, particlePos);
                Point3D satLatLon = GeoUtils.convertUTMtoLATLON(satPoint, 36);
                
                writer.write("<Placemark>\n");
                writer.write("  <n>Ray to Satellite " + sat.getSatID() + "<n>\n");
                writer.write("  <styleUrl>#" + (LosAlgorithm.ComputeLos(particlePos, buildings, sat) ? "losStyle" : "nlosStyle") + "</styleUrl>\n");
                writer.write("  <description>" + (LosAlgorithm.ComputeLos(particlePos, buildings, sat) ? "LOS" : "NLOS") + 
                           " - Elevation: " + sat.getElevetion() + "°, Azimuth: " + sat.getAzimuth() + "°</description>\n");
                writer.write("  <LineString>\n");
                writer.write("    <altitudeMode>absolute</altitudeMode>\n");
                writer.write("    <coordinates>\n");
                writer.write("      " + particleLatLon.getY() + "," + particleLatLon.getX() + "," + particlePos.getZ() + "\n");
                writer.write("      " + satLatLon.getY() + "," + satLatLon.getX() + "," + satPoint.getZ() + "\n");
                writer.write("    </coordinates>\n");
                writer.write("  </LineString>\n");
                writer.write("</Placemark>\n");
            }
            
            writer.write("</Document>\n");
            writer.write("</kml>");
        } catch (IOException e) {
            System.err.println("Error generating rays KML: " + e.getMessage());
        }
    }
}
