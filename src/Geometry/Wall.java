package Geometry;

/**
 * Created by Roi on 1/7/2015.
 * This class represents a surface (wall).
 * The main restriction is that the entire wall "sits" in a signle plane.
 * Since the wall is not restricted to 2.5D, we represent ALL of the wall's vertices.
 *
 * wall Type :
 * 1- square;
 * 2 - Polygon
 *
 */

public class Wall {
    public enum WallType {SQUARE, POLYGON};//
    public WallType typeOfwall;

    double maxHeight;
    Line3D wallAsLine; //this Line3D represent a wall relative to ground.
    Point3D[] point3dArray;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Wall {");
        sb.append("typeOfwall=").append(typeOfwall);
        sb.append(", maxHeight=").append(maxHeight);
        sb.append(", wallAsLine=").append(wallAsLine != null ? wallAsLine.toString() : "null");

        sb.append(", point3dArray=[");
        if (point3dArray != null) {
            for (int i = 0; i < point3dArray.length; i++) {
                sb.append(point3dArray[i] != null ? point3dArray[i].toString() : "null");
                if (i < point3dArray.length - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append("]");
        sb.append(" }");
        return sb.toString();
    }
    public Line3D getWallAsLine() {
        return wallAsLine;
    }

    public void setWallAsLine(Line3D wallAsLine) {
        this.wallAsLine = wallAsLine;
    }

    public Wall(Point3D a, Point3D b)
    {
        wallAsLine = new Line3D(a, b);
        typeOfwall= WallType.SQUARE;
        this.maxHeight = Math.max(a.getZ(), b.getZ());
        point3dArray = new Point3D[2];
        point3dArray[0] = a;
        point3dArray[1] = b;


    }

    public static int hasIntersectionCount = 0;
    public static int has2DIntersectionCount = 0;
    public static int methodIntersectionCount = 0;

    //This function calculates the intersection point between a 3D line (represented by Line3D object) and a wall.
    public Point3D intersectionPoint3D(Line3D line){
        methodIntersectionCount++;
        Point2D point2D = getWallAsLine().intersectionPoint(line);
        if (point2D == null){
            return null;
        }
        has2DIntersectionCount++;
        double zOfRay = interpolate(line, point2D);
        double zOfWall = interpolate(getWallAsLine(), point2D);
        
        // Debug logging
        
        
        // בדיקה אם נקודת החיתוך היא בתוך גבולות הקיר
        // Check if the intersection point is within the wall's height bounds
        if (zOfRay >= 0 && zOfRay <= zOfWall && zOfWall <= getMaxHeight()) {
            hasIntersectionCount++;
            return new Point3D(point2D.getX(), point2D.getY(), zOfRay);
        }
        
        return null;
    }
    //This function calculates the interpolated z-coordinate of a point on a line segment.
    private double interpolate(Line3D line, Point2D point2D) {
        double length = line.getP1().distance2D(line.getP2());
        double percentageInXYFromLineP1 = line.getP1().distance(point2D) / length;
        double ans = line.getP1().getZ() + (percentageInXYFromLineP1 * (line.getP2().getZ() - line.getP1().getZ()));
        return ans;
    }


    public double getMaxHeight() {
        return maxHeight;
    }

    public WallType getWallType() {
        return typeOfwall;
    }


    public Point3D[]    getPoint3dArray() {
        return point3dArray;
    }

    //This function checks if a given 3D line (ray) intersects with the wall.
    public boolean isIntersecting(Line3D ray)
    {
        //ray is given in UTM
        return intersectionPoint3D(ray) != null;
    }

    //calculate the distance between a point on the wall and the top of the wall
    public double distanceToTop(Point3D pointOnWall) {
        double distance = maxHeight - pointOnWall.getZ();
        return distance >= 0 ? distance : -1;
    }

    public boolean doesIntersect(Point3D p1, Point3D p2) {
        // Create a line segment between the two points
        Line3D lineSegment = new Line3D(p1, p2);
        
        // For a square wall
        if (typeOfwall == WallType.SQUARE) {
            // Check if the line segment intersects with the wall plane
            // First, check if the line intersects with the wall's base line
            if (wallAsLine != null && wallAsLine.intersects(lineSegment)) {
                // Then check if the intersection point is within the wall's height
                Point3D intersection = wallAsLine.getIntersectionPoint(lineSegment);
                if (intersection != null) {
                    return intersection.getZ() <= maxHeight;
                }
            }
            return false;
        }
        // For a polygon wall
        else if (typeOfwall == WallType.POLYGON && point3dArray != null) {
            // Check if the line segment intersects with any of the wall's faces
            for (int i = 0; i < point3dArray.length - 1; i++) {
                Line3D edge = new Line3D(point3dArray[i], point3dArray[i + 1]);
                if (edge.intersects(lineSegment)) {
                    return true;
                }
            }
            // Check the last edge (connecting last point to first point)
            if (point3dArray.length > 0) {
                Line3D lastEdge = new Line3D(point3dArray[point3dArray.length - 1], point3dArray[0]);
                return lastEdge.intersects(lineSegment);
            }
        }
        return false;
    }
}