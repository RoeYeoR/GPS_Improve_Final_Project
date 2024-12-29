package Geometry;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import Utils.GeoUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Created by Roi on 1/7/2015.
 */
public class BuildingsFactory {

    //This function accepts the name of a kml file of buildings and creates a list of buildings in utm units
    public static List<Building> generateUTMBuildingListfromKMLfile(String file) throws Exception {
        List<Building> buildingList = new ArrayList<Building>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            int lineCount = 0;
            int buildingCount = 0;
            
            line = reader.readLine();
            while(line != null) {
                lineCount++;
                if(line.contains("<coordinates")) {
                    //Reading a line of coordinates
                    line = reader.readLine();
                    lineCount++;
                    
                    if (line != null && !line.trim().isEmpty()) {
                        //Creating a building from coordinate line
                        Building tmpBuilding = generateUTMBuildingFromCordString(line);
                        if (tmpBuilding != null && !tmpBuilding.getWalls().isEmpty()) {
                            buildingList.add(tmpBuilding);
                            buildingCount++;
                            
                           
                            Wall firstWall = tmpBuilding.getWalls().get(0);
                            Point3D[] points = firstWall.getPoint3dArray();
                            
                        } else {
                            System.out.println("Warning: Failed to create valid building from coordinates");
                        }
                    }
                }
                line = reader.readLine();
            }
            
            
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + file);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading file: " + file);
            e.printStackTrace();
        }

        return buildingList;

    }
    //This function that receives a string of coordinates representing a building in units of latlong and returns a building in units of utm
    private static Building generateUTMBuildingFromCordString(String cordFromKMLString) throws Exception {
        //Separates the list by comma or space
        String[] cords = cordFromKMLString.split(",| ");
        //A list of points that will hold the vertices of the building
        List<Point3D> buildingVertices = new ArrayList<Point3D>();
        int size = cords.length;
        int idx=0;
        while(idx<size-2)
        {
            // בקובץ KML הסדר הוא: longitude,latitude,altitude
            double lon = Double.parseDouble(cords[idx]);
            double lat = Double.parseDouble(cords[idx+1]);
            double alt = Double.parseDouble(cords[idx+2]);
            
            // המרה לנקודה בפורמט UTM
            Point3D latLonPoint = new Point3D(lon, lat, alt);
            Point3D utmPoint = GeoUtils.convertLATLONtoUTM(latLonPoint);
            buildingVertices.add(utmPoint);
            
            idx+=3;
        }
        int Size = buildingVertices.size();
        //System.out.println("size: "+Size);
        if(buildingVertices.get(Size-1).equals(buildingVertices.get(0))){
            buildingVertices.remove(Size-1); //remove the last point since it similar to the first point
        }
        else{
            throw new Exception("Bad KML File");
        }
        //Returning the building by creating it from the list of vertices from which it is built
        return new Building(buildingVertices);
    }
    public static List<Point3D> parseKML(String filePath) {
        List<Point3D> points = new ArrayList<>();
        System.out.println("Starting to parse KML file: " + filePath);

        try {
            File file = new File(filePath);
            System.out.println("File exists: " + file.exists());
            System.out.println("File absolute path: " + file.getAbsolutePath());
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            System.out.println("Parsing XML file...");
            Document doc = dBuilder.parse(new FileInputStream(file));

            doc.getDocumentElement().normalize();
            System.out.println("XML parsing complete. Root element: " + doc.getDocumentElement().getNodeName());

            // Get all Point elements
            NodeList pointList = doc.getElementsByTagName("Point");
            System.out.println("Found " + pointList.getLength() + " Point nodes");

            for (int i = 0; i < pointList.getLength(); i++) {
                Node pointNode = pointList.item(i);
                if (pointNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element pointElement = (Element) pointNode;
                    NodeList coordinatesList = pointElement.getElementsByTagName("coordinates");
                    if (coordinatesList.getLength() > 0) {
                        String coordinatesStr = coordinatesList.item(0).getTextContent().trim();
                        String[] coordinates = coordinatesStr.split(",");
                        
                        // בקובץ KML הסדר הוא longitude,latitude,altitude
                        double lon = Double.parseDouble(coordinates[0]);
                        double lat = Double.parseDouble(coordinates[1]);
                        double alt = coordinates.length > 2 ? Double.parseDouble(coordinates[2]) : 1.8;
                        
                        // יצירת נקודה חדשה - שימו לב שהסדר ב-Point3D הוא x=lon, y=lat, z=alt
                        points.add(new Point3D(lon, lat, alt));
                        
                    }
                }
            }
            System.out.println("Successfully parsed " + points.size() + " points");

        } catch (Exception e) {
            System.err.println("Error parsing KML file: " + e.getMessage());
            e.printStackTrace();
        }

        return points;
    }
}