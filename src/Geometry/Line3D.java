package Geometry;

import Utils.GeomUtils;

/**
 * Created by Roi on 1/7/2015.
 */
public class Line3D {

    private Point3D p1,p2;

    public Line3D(Point3D p1, Point3D p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Line3D(Point2D p1, Point2D p2, double z1, double z2) {
        this.p1 = new Point3D(p1.getX(), p1.getY(), z1);
        this.p2 = new Point3D(p2.getX(), p2.getY(), z2);
    }

    public double length(){
        return p1.distance(p2);
    }

    public Line3D(Line3D l) {
        this.p1 = l.getP1();
        this.p2 = l.getP2();
    }

    public  Line3D(Point3D pos, double azimuth, double elevetion, int dist) {

        //This section converts Sat azimut (where 0 degrees is North ) to unit circle azimut where north is 90 degrees
        //note that elevetion is messures from xy plane , hence the diffrence between those equations and wikipedia equations
        //http://en.wikipedia.org/wiki/Spherical_coordinate_system
        double newAzimut = GeomUtils.ConvertSatAzimutInDegreesTOfitUnitCircle(azimuth);
        //newAzimut = azimuth;
        // הגדלת מרחק הבדיקה ל-1000 מטר
        dist = 1000;
        double newX = pos.getX() + dist*Math.cos(Math.toRadians(elevetion))*Math.cos(Math.toRadians(newAzimut));
        double newY = pos.getY() + dist*Math.cos(Math.toRadians(elevetion))*Math.sin(Math.toRadians(newAzimut));
        double newZ = pos.getZ() + dist*Math.sin(Math.toRadians(elevetion));
        Point3D newPoint = new Point3D(newX, newY, newZ);
        this.p1 = pos;
        this.p2 = newPoint;
    }

    public Point3D getP1() {
        return p1;
    }

    public Point3D getP2() {
        return p2;

    }

    public double dx()
    {
        return p1.getX()-p2.getX();
    }

    public double dy()
    {
        return p1.getY()-p2.getY();
    }
    public double dz()
    {
        return p1.getZ()-p2.getZ();
    }

    @Override
    public String toString() {
        return "Line3D{" +
                "p1=" + p1 +
                ", p2=" + p2 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Line3D)) return false;

        Line3D line3D = (Line3D) o;

        if (p1 != null ? !p1.equals(line3D.p1) : line3D.p1 != null) return false;
        if (p2 != null ? !p2.equals(line3D.p2) : line3D.p2 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = p1 != null ? p1.hashCode() : 0;
        result = 31 * result + (p2 != null ? p2.hashCode() : 0);
        return result;
    }

    public Point3D getCenterPoint()
    {
        return new Point3D((this.p1.getX()+this.p2.getX())/2, (this.p1.getY()+this.p2.getY())/2, (this.p1.getZ()+this.p2.getZ())/2);
    }

    /** this method checks if the Point3D p falls on the this (Line3D) */
    public boolean line3DContains(Point3D p) {return false;}



    public Point2D intersectionPoint(Line3D l) {
        return lineIntersect(l.getP1().getX(), l.getP1().getY(),l.getP2().getX(), l.getP2().getY(), this.p1.getX(),this.p1.getY(), this.p2.getX(), this.p2.getY());
    }

    public static  Point2D lineIntersect(double x1, double y1, double x2, double y2, double  x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new Point2D( x1 + ua*(x2 - x1), y1 + ua*(y2 - y1));
        }

        return null;
    }

    public boolean intersects(Line3D other) {
        // Get the direction vectors of both lines
        Point3D dir1 = new Point3D(p2.getX() - p1.getX(), p2.getY() - p1.getY(), p2.getZ() - p1.getZ());
        Point3D dir2 = new Point3D(other.p2.getX() - other.p1.getX(), 
                                  other.p2.getY() - other.p1.getY(), 
                                  other.p2.getZ() - other.p1.getZ());
        
        // Calculate the cross product of the direction vectors
        Point3D cross = new Point3D(
            dir1.getY() * dir2.getZ() - dir1.getZ() * dir2.getY(),
            dir1.getZ() * dir2.getX() - dir1.getX() * dir2.getZ(),
            dir1.getX() * dir2.getY() - dir1.getY() * dir2.getX()
        );
        
        // If the cross product is zero, lines are parallel
        if (cross.getX() == 0 && cross.getY() == 0 && cross.getZ() == 0) {
            return false;
        }
        
        // Calculate the intersection point
        Point3D intersection = getIntersectionPoint(other);
        if (intersection == null) {
            return false;
        }
        
        // Check if intersection point lies on both line segments
        return isPointOnSegment(intersection) && other.isPointOnSegment(intersection);
    }
    
    public Point3D getIntersectionPoint(Line3D other) {
        // Get the direction vectors of both lines
        Point3D dir1 = new Point3D(p2.getX() - p1.getX(), p2.getY() - p1.getY(), p2.getZ() - p1.getZ());
        Point3D dir2 = new Point3D(other.p2.getX() - other.p1.getX(), 
                                  other.p2.getY() - other.p1.getY(), 
                                  other.p2.getZ() - other.p1.getZ());
        
        // Calculate the cross product of the direction vectors
        Point3D cross = new Point3D(
            dir1.getY() * dir2.getZ() - dir1.getZ() * dir2.getY(),
            dir1.getZ() * dir2.getX() - dir1.getX() * dir2.getZ(),
            dir1.getX() * dir2.getY() - dir1.getY() * dir2.getX()
        );
        
        // If the cross product is zero, lines are parallel
        if (cross.getX() == 0 && cross.getY() == 0 && cross.getZ() == 0) {
            return null;
        }
        
        // Calculate parameters for the intersection point
        Point3D delta = new Point3D(other.p1.getX() - p1.getX(), 
                                   other.p1.getY() - p1.getY(), 
                                   other.p1.getZ() - p1.getZ());
        
        double t = (delta.getX() * dir2.getY() * dir1.getZ() + 
                   dir2.getX() * dir1.getY() * delta.getZ() + 
                   delta.getY() * dir1.getX() * dir2.getZ() - 
                   delta.getZ() * dir1.getY() * dir2.getX() - 
                   delta.getY() * dir2.getZ() * dir1.getX() - 
                   dir2.getY() * dir1.getZ() * delta.getX()) / 
                  (dir1.getX() * dir2.getY() * dir2.getZ() + 
                   dir2.getX() * dir2.getZ() * dir1.getY() + 
                   dir2.getY() * dir1.getZ() * dir2.getX() - 
                   dir2.getZ() * dir1.getY() * dir2.getX() - 
                   dir2.getY() * dir2.getZ() * dir1.getX() - 
                   dir2.getX() * dir1.getZ() * dir2.getY());
        
        // Calculate the intersection point
        return new Point3D(
            p1.getX() + t * dir1.getX(),
            p1.getY() + t * dir1.getY(),
            p1.getZ() + t * dir1.getZ()
        );
    }
    
    private boolean isPointOnSegment(Point3D point) {
        // Check if point lies on the line segment
        double d1 = point.distance(p1);
        double d2 = point.distance(p2);
        double lineLength = p1.distance(p2);
        
        // Allow for small floating-point errors
        double epsilon = 0.000001;
        return Math.abs(d1 + d2 - lineLength) < epsilon;
    }

    public double distanceFromPoint(Point3D p)
  {return 0;}

}