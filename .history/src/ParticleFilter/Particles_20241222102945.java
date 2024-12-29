package ParticleFilter;

import GNSS.Sat;
import Geometry.Building;
import Geometry.Point3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Particles {
    private List<Particle> ParticleList;
    public static final int NumberOfParticles = 2025;
    private Random R1;

    public Particles() {
        ParticleList = new ArrayList<Particle>();
        R1 = new Random();
    }

    public List<Particle> getParticleList() {
        return ParticleList;
    }

    public void setParticleList(List<Particle> particleList) {
        ParticleList = particleList;
    }

    public void initParticles(Point3D p1, Point3D p2) {
        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double maxY = Math.max(p1.getY(), p2.getY());

        for (int i = 0; i < NumberOfParticles; i++) {
            double x = minX + R1.nextDouble() * (maxX - minX);
            double y = minY + R1.nextDouble() * (maxY - minY);
            double z = p1.getZ(); // Assuming same height
            ParticleList.add(new Particle(new Point3D(x, y, z)));
        }
    }

    public void initParticlesBetweenPoints(Point3D start, Point3D end) {
        for (int i = 0; i < NumberOfParticles; i++) {
            double t = R1.nextDouble();
            double x = start.getX() + t * (end.getX() - start.getX());
            double y = start.getY() + t * (end.getY() - start.getY());
            double z = start.getZ() + t * (end.getZ() - start.getZ());
            ParticleList.add(new Particle(new Point3D(x, y, z)));
        }
    }

    public void MessureSignalFromSats(List<Building> buildings, List<Sat> satellites) {
        for (Particle p : ParticleList) {
            p.MessureSesnor(buildings, satellites);
        }
    }

    public void ComputeWeights(Boolean[] RecordedLos) {
        for (Particle p : ParticleList) {
            double weight = 0;
            for (int j = 0; j < p.getLOS().length; j++) {
                if (p.getLOS()[j] == RecordedLos[j]) {
                    weight += 1.0;
                }
            }
            weight = weight / RecordedLos.length;
            p.setWeight(weight);
        }
    }

    public void OutFfRegion(List<Building> buildings, Point3D p1, Point3D p2) {
        for (Particle p : ParticleList) {
            for (Building b : buildings) {
                if (b.isInside(p.pos)) {
                    p.OutOfRegion = true;
                    break;
                }
            }
        }
    }

    public void Normal_Weights() {
        double sum = 0;
        for (Particle p : ParticleList) {
            if (!p.OutOfRegion) {
                sum += p.getWeight();
            }
        }
        if (sum > 0) {
            for (Particle p : ParticleList) {
                if (!p.OutOfRegion) {
                    p.setWeight(p.getWeight() / sum);
                }
            }
        }
    }

    public void Resample() {
        List<Particle> newParticles = new ArrayList<>();
        double[] cumSum = new double[ParticleList.size()];
        cumSum[0] = ParticleList.get(0).getWeight();
        
        for (int i = 1; i < ParticleList.size(); i++) {
            cumSum[i] = cumSum[i-1] + ParticleList.get(i).getWeight();
        }

        for (int i = 0; i < NumberOfParticles; i++) {
            double r = R1.nextDouble();
            for (int j = 0; j < ParticleList.size(); j++) {
                if (r <= cumSum[j]) {
                    newParticles.add(new Particle(ParticleList.get(j).pos));
                    break;
                }
            }
        }
        ParticleList = newParticles;
    }

    public Point3D GetParticleWithMaxWeight() {
        double maxWeight = -1;
        Point3D bestPos = null;
        
        for (Particle p : ParticleList) {
            if (!p.OutOfRegion && p.getWeight() > maxWeight) {
                maxWeight = p.getWeight();
                bestPos = p.pos;
            }
        }
        return bestPos;
    }
}