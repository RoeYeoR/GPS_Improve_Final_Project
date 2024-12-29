package ParticleFilter;

import GNSS.Sat;
import Geometry.Building;
import Geometry.Point3D;

import java.util.List;

public class Particle {
    public Point3D pos;
    public double Weight;
    public boolean OutOfRegion;
    public Boolean[] LOS;

    public Particle() {
        this.pos = new Point3D(0, 0, 0);
        this.Weight = 0;
        this.LOS = null;
        this.OutOfRegion = false;
    }

    public Particle(Point3D pos) {
        this.pos = pos;
        this.Weight = 1;
        this.OutOfRegion = false;
    }

    public Particle(double x, double y, double z) {
        this.pos = new Point3D(x, y, z);
        this.Weight = 1;
        this.OutOfRegion = false;
    }

    public double getWeight() {
        return Weight;
    }

    public void setWeight(double weight) {
        Weight = weight;
    }

    public Boolean[] getLOS() {
        return LOS;
    }

    public void setLOS(Boolean[] LOS) {
        this.LOS = LOS;
    }

    public void MessureSesnor(List<Building> bs, List<Sat> allSats) {
        Boolean[] los = new Boolean[allSats.size()];
        for (int i = 0; i < allSats.size(); i++) {
            los[i] = LosData.los(allSats.get(i), pos, bs);
        }
        this.LOS = los;
    }

    public Point3D getLocation() {
        return pos;
    }

    public static void PrintArr(Boolean[] arr) {
        if (arr == null) {
            System.out.println("Array is null");
            return;
        }
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
        System.out.println();
    }

    public int getNumberOfMatchedSats() {
        if (LOS == null) return 0;
        int count = 0;
        for (Boolean b : LOS) {
            if (b != null && b) count++;
        }
        return count;
    }

    public boolean OutOfRegion(List<Building> bs, Point3D pivot1, Point3D pivot2) {
        // Check if particle is inside any building
        for (Building b : bs) {
            if (b.isContain(pos)) {
                OutOfRegion = true;
                return true;
            }
        }
        
        // Check if particle is outside the region defined by pivot points
        double minX = Math.min(pivot1.getX(), pivot2.getX());
        double maxX = Math.max(pivot1.getX(), pivot2.getX());
        double minY = Math.min(pivot1.getY(), pivot2.getY());
        double maxY = Math.max(pivot1.getY(), pivot2.getY());
        
        if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY) {
            OutOfRegion = true;
            return true;
        }
        
        OutOfRegion = false;
        return false;
    }

    public void EvaluateWeightsNoHistory(Boolean[] realLos) {
        if (realLos == null || LOS == null) {
            Weight = 0;
            return;
        }

        int matches = 0;
        int total = 0;

        for (int i = 0; i < realLos.length; i++) {
            if (realLos[i] != null && LOS[i] != null) {
                total++;
                if (realLos[i].equals(LOS[i])) {
                    matches++;
                }
            }
        }

        Weight = total > 0 ? (double) matches / total : 0;
    }
}