package Algorithm;

import GNSS.Sat;
import Geometry.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Roi on 1/7/2015.
 * Those set of functions return true for a LOS sattelite and false for NLOS satelite.
 */
public class LosAlgorithm {


    public static void main(String[] args) {
//        ourtest1();
//        ourtest2();

    }

    private static void ourtest1() {
        Point3D p1 = new Point3D(0, 0, 0);
        Point3D p2 = new Point3D(10, 10, 10);
        Wall wall = new Wall(p1, p2);

        Sat sat = new Sat(45, 45, 0);

        Point3D pos = new Point3D(1, 1, 1);
        double losDistance = ourComputeLos(pos, wall, sat);

        System.out.println("Distance from intersection point to top of wall: " + losDistance);

    }
    private static void ourtest2() {
        // Create a sample building with walls
        Point3D p1 = new Point3D(0, 0, 0);
        Point3D p2 = new Point3D(10, 10, 10);
        Point3D p3 = new Point3D(20, 20, 20);
        Point3D p4 = new Point3D(30, 30, 30);
        List<Point3D> temp = new ArrayList<>();
        temp.add(p1);
        temp.add(p2);
        temp.add(p3);
        temp.add(p4);
        Building building = new Building(temp);

        // Create a sample satellite
        Sat sat = new Sat(45, 45, 0); // Sample azimuth and elevation angles
        // Create a sample observer position
        Point3D pos = new Point3D(0, 0, 35); // Sample observer position
        // Test the ourComputeLos function
        double losDistance = ourComputeLos(pos, building, sat);
        System.out.println("Line-of-sight distance to top of building: " + losDistance);



    }

    //Computes LOS between a point, a wall, and a satellite.
    public static boolean ComputeLos(Point3D pos, Wall wall, Sat sat)
    {
        Line3D ray = new Line3D(pos, sat.getAzimuth(), sat.getElevation(), 300);
        Point3D intersectionPoint = wall.intersectionPoint3D(ray);
        return intersectionPoint == null; // אם אין נקודת חיתוך - יש LOS
    }
    /**
     * receives a wall and satellite point,
     * You find the line between the satellite and the point and calculate whether there is a point of intersection between this line and the wall.
     * If none, returns -1
     * If there is, returns the distance between the intersection point and the height of the wall.
     */
    public static double ourComputeLos(Point3D pos, Wall wall, Sat sat) {
        // Create line of sight ray
        Line3D ray = new Line3D(pos, sat.getAzimuth(), sat.getElevation(), 300);
        
        // Get the 2D intersection point
        Point2D point2D = wall.getWallAsLine().intersectionPoint(ray);
        if (point2D == null) {
            return -1; // No intersection in 2D
        }
        
        // Calculate horizontal distance to intersection
        double dx = point2D.getX() - ray.getP1().getX();
        double dy = point2D.getY() - ray.getP1().getY();
        double horizontalDistance = Math.sqrt(dx*dx + dy*dy);
        
        // Calculate height at intersection point
        double heightGain = horizontalDistance * Math.tan(Math.toRadians(sat.getElevation()));
        double zAtIntersection = pos.getZ() + heightGain;

        // Check if ray passes below wall top
        double wallTotalHeight = wall.getWallAsLine().getP1().getZ() + wall.getMaxHeight();
        if (zAtIntersection < wallTotalHeight) {
            double heightDiff = wallTotalHeight - zAtIntersection;

            System.out.println("\nLOS/NLOS Analysis Results:");
            System.out.println("==========================");
            System.out.println("Wall Information:");
            System.out.println("- Base Height: " + String.format("%.1f", wall.getWallAsLine().getP1().getZ()) + " meters");
            System.out.println("- Total Height: " + String.format("%.1f", wall.getMaxHeight()) + " meters");
            
            System.out.println("\nObserver Information:");
            System.out.println("- Position: " + pos.toString());
            System.out.println("- Height: " + String.format("%.1f", pos.getZ()) + " meters");
            
            System.out.println("\nSatellite Information:");
            System.out.println("- Elevation: " + String.format("%.1f", sat.getElevation()) + " degrees");
            System.out.println("- Azimuth: " + String.format("%.1f", sat.getAzimuth()) + " degrees");
            System.out.println("- Height at Intersection: " + String.format("%.1f", zAtIntersection) + " meters");
            
            System.out.println("\nNLOS Details:");
            System.out.println("- Height Difference: " + String.format("%.3f", heightDiff) + " meters");
            
            System.out.println("\nFinal Result: NLOS");
            System.out.println("Distance needed to clear wall (vertical): " + String.format("%.3f", heightDiff) + " meters");

            return heightDiff;
        }
        
        // Ray passes above wall
        System.out.println("\nLOS/NLOS Analysis Results:");
        System.out.println("==========================");
        System.out.println("Wall Information:");
        System.out.println("- Base Height: " + String.format("%.1f", wall.getWallAsLine().getP1().getZ()) + " meters");
        System.out.println("- Total Height: " + String.format("%.1f", wall.getMaxHeight()) + " meters");
        
        System.out.println("\nObserver Information:");
        System.out.println("- Position: " + pos.toString());
        System.out.println("- Height: " + String.format("%.1f", pos.getZ()) + " meters");
        
        System.out.println("\nSatellite Information:");
        System.out.println("- Elevation: " + String.format("%.1f", sat.getElevation()) + " degrees");
        System.out.println("- Azimuth: " + String.format("%.1f", sat.getAzimuth()) + " degrees");
        System.out.println("- Height at Intersection: " + String.format("%.1f", zAtIntersection) + " meters");
        
        System.out.println("\nFinal Result: LOS");
        return -1;
    }

    //Computes LOS between a point, a building, and a satellite by iterating over the walls of the building.
    public static boolean ComputeLos(Point3D pos, Building building, Sat sat)
    {
        for(Wall wall : building.getWalls())
        {
            if(ComputeLos(pos, wall, sat)==false)
                return false;
        }
        return true;
    }

    /**
     * Computes the maximum Line of Sight (LOS) distance from a given point to the top of a building,
     *
     * @param pos The position of the observer (Point3D object).
     * @param building The building object representing the structure.
     * @param sat The satellite object used for LOS calculations.
     * @return The maximum LOS distance to the top of the building.
     */
    public static double ourComputeLos(Point3D pos, Building building, Sat sat)
    {
        double max_distanceToTop = Integer.MIN_VALUE;
        for(Wall wall : building.getWalls())
        {
            // Compute the LOS distance from the observer's position to the current wall.
            double distance = ourComputeLos(pos, wall, sat);
            if((distance != -1) && (distance > max_distanceToTop)){
                max_distanceToTop = distance;

            }
        }

        return max_distanceToTop == Integer.MIN_VALUE ? -1: max_distanceToTop;
    }

    //Computes LOS between a point, a list of buildings, and a satellite by iterating over the buildings and calling the previous function.
    public static boolean ComputeLos(Point3D pos,List<Building> buildings, Sat sat)
    {
        for(Building building : buildings)
        {
            if(ComputeLos(pos, building, sat)==false)
                return false;
        }
        return true;
    }

    /**
     * Computes the maximum Line of Sight (LOS) distance from a given point to the top of multiple buildings,
     *
     * @param pos The position of the observer (Point3D object).
     * @param buildings A list of Building objects representing the structures.
     * @param sat The satellite object used for LOS calculations.
     * @return The maximum LOS distance to the top of any building in the list.
     */
    public static double ourComputeLos(Point3D pos, List<Building> buildings, Sat sat)
    {
        double max_distanceToTop = Integer.MIN_VALUE;
        for(Building building : buildings)
        {
            // Compute the LOS distance from the observer's position to the top of the current building.
            double distance  = ourComputeLos(pos, building, sat);
            if((distance != -1) && (distance > max_distanceToTop))
                max_distanceToTop = distance;
        }
        return max_distanceToTop == Integer.MIN_VALUE ? -1: max_distanceToTop;
    }

    public static boolean ComputeLosWithPosition(Point3D pos, List<Building> buildings, Point3D satPos) {
        // Calculate direction vector from position to satellite
        double dx = satPos.getX() - pos.getX();
        double dy = satPos.getY() - pos.getY();
        double dz = satPos.getZ() - pos.getZ();
        
        // Create ray from position in direction of satellite
        Line3D ray = new Line3D(pos, new Point3D(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
        
        // Check each building for intersection
        for (Building building : buildings) {
            for (Wall wall : building.getWalls()) {
                Point3D intersection = wall.intersectionPoint3D(ray);
                if (intersection != null) {
                    return false; // Found intersection, so it's NLOS
                }
            }
        }
        return true; // No intersections found, so it's LOS
    }

    public static Set<Building> findBuildings(Point2D base, double az, List<Building> allBuildings, int azimutResolution) {

        double minAz = az-azimutResolution/2;
        double maxAz = az + azimutResolution/2;
        Set<Building> resultSet = new HashSet<>();
        boolean added;
        for (Building building : allBuildings) {
            added = false;
            List<Wall> walls = building.getWalls();
            for (Wall wall : walls) {
                Point3D[] point3dArray = wall.getPoint3dArray();
                for (Point3D point3D : point3dArray) {
                    double angRandians = Math.atan2(point3D.getY() - base.getY(), point3D.getX() - base.getX());
                    double angDegrees = Math.toDegrees(angRandians);
                    if (angDegrees < 0){
                        angDegrees += 360;
                    }
                    if (angDegrees < 0 || angDegrees >= 360){
                        assert false;
                    }
                    double angDegNorthHead = 450 - angDegrees;
                    if (angDegNorthHead >= 360){
                        angDegNorthHead -= 360;
                    }
                    if (angDegNorthHead < 0 || angDegNorthHead >= 360){
                        assert false;
                    }
                    if (angDegNorthHead <= maxAz && angDegNorthHead >= minAz){
                        resultSet.add(building);
                        added = true;
                    }
                    if (added){
                        break;
                    }
                }
                if (added){
                    break;
                }
            }
        }
        return resultSet;
    }
}