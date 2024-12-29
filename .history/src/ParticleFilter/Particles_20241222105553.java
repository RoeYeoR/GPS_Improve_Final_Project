package ParticleFilter;

import GNSS.Sat;
import Geometry.Building;
import Geometry.Point3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Particles {
    private List<Particle> ParticleList;
    public static final int NumberOfParticles = 700;
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

    public void ComputeWeightsNoHistory(Boolean[] realLos) {
        for (Particle p : ParticleList) {
            p.EvaluateWeightsNoHistory(realLos);
        }
    }

    public void OutFfRegion(List<Building> buildings, Point3D p1, Point3D p2) {
        for (Particle p : ParticleList) {
            for (Building b : buildings) {
                if (b.isContain(p.pos)) {
                    p.OutOfRegion = true;
                    break;
                }
            }
        }
    }

    public double[] Normal_Weights() {
        double sum = 0;
        for (Particle p : ParticleList) {
            if (!p.OutOfRegion) {
                sum += p.getWeight();
            }
        }
        
        double[] normalizedWeights = new double[ParticleList.size()];
        if (sum > 0) {
            for (int i = 0; i < ParticleList.size(); i++) {
                Particle p = ParticleList.get(i);
                if (!p.OutOfRegion) {
                    normalizedWeights[i] = p.getWeight() / sum;
                    p.setWeight(normalizedWeights[i]);
                } else {
                    normalizedWeights[i] = 0;
                }
            }
        }
        return normalizedWeights;
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

    public void Print3DPoints() {
        for (Particle p : ParticleList) {
            System.out.println(p.getLocation());
        }
    }

    public void SetAfterResample(List<Point3D> newLocations) {
        ParticleList.clear();
        for (Point3D loc : newLocations) {
            ParticleList.add(new Particle(loc));
        }
    }

    public void ourMoveParticleWithError(ActionFunction action) {
        for (Particle p : ParticleList) {
            Point3D newPos = action.apply(p.pos);
            // Add some random noise
            double noiseX = R1.nextGaussian() * 0.5; // Adjust standard deviation as needed
            double noiseY = R1.nextGaussian() * 0.5;
            double noiseZ = R1.nextGaussian() * 0.1;
            p.pos = new Point3D(
                newPos.getX() + noiseX,
                newPos.getY() + noiseY,
                newPos.getZ() + noiseZ
            );
        }
    }

    public void ComputeAndPrintErrors(Point3D realPoint) {
        double sumError = 0;
        int validParticles = 0;
        
        for (Particle p : ParticleList) {
            if (!p.OutOfRegion) {
                double error = p.pos.distance2D(realPoint);
                sumError += error;
                validParticles++;
            }
        }
        
        if (validParticles > 0) {
            double avgError = sumError / validParticles;
            System.out.printf("Average Error: %.2f meters\n", avgError);
        } else {
            System.out.println("No valid particles to compute error");
        }
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