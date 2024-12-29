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
}