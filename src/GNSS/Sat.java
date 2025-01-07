package GNSS;

import java.util.ArrayList;
import java.util.List;

public class Sat {
    // Basic satellite information
    private double x, y, z;           // Position coordinates
    private double vx, vy, vz;        // Velocity components
    private double azimuth;           // Azimuth angle (will be calculated later)
    private double elevation;         // Elevation angle (will be calculated later)
    private int satID;               // Satellite ID
    private char system;             // Satellite system (G=GPS, R=GLONASS, E=Galileo, C=BeiDou)
    
    // Signal measurements
    private List<Double> l1cPhase;    // L1 carrier phase measurements
    private List<Double> l5qPhase;    // L5 carrier phase measurements
    private List<Double> c1cRange;    // L1 pseudorange measurements
    private List<Double> c5qRange;    // L5 pseudorange measurements
    private List<Double> d1cDoppler;  // L1 Doppler measurements
    private List<Double> d5qDoppler;  // L5 Doppler measurements
    private List<Double> s1cSnr;      // L1 SNR measurements
    private List<Double> s5qSnr;      // L5 SNR measurements
    
    // Time information
    private long firstTimestamp;      // First measurement timestamp
    private List<Long> timestamps;    // List of measurement timestamps
    
    // Lists for azimuth and elevation history
    private List<Double> azimuthHistory;
    private List<Double> elevationHistory;
    
    public Sat(char system, int satID) {
        this.system = system;
        this.satID = satID;
        
        // Initialize measurement lists
        this.l1cPhase = new ArrayList<>();
        this.l5qPhase = new ArrayList<>();
        this.c1cRange = new ArrayList<>();
        this.c5qRange = new ArrayList<>();
        this.d1cDoppler = new ArrayList<>();
        this.d5qDoppler = new ArrayList<>();
        this.s1cSnr = new ArrayList<>();
        this.s5qSnr = new ArrayList<>();
        this.timestamps = new ArrayList<>();
        this.azimuthHistory = new ArrayList<>();
        this.elevationHistory = new ArrayList<>();
    }
    
    // Constructor for satellite with position in spherical coordinates (azimuth, elevation, distance)
    public Sat(double azimuth, double elevation, double distance) {
        this('G', 0); // Default to GPS system with ID 0
        
        // Convert spherical coordinates to Cartesian
        double azimuthRad = Math.toRadians(azimuth);
        double elevationRad = Math.toRadians(elevation);
        
        this.x = distance * Math.cos(elevationRad) * Math.cos(azimuthRad);
        this.y = distance * Math.cos(elevationRad) * Math.sin(azimuthRad);
        this.z = distance * Math.sin(elevationRad);
        
        this.azimuth = azimuth;
        this.elevation = elevation;
    }
    
    // Add a complete measurement set
    public void addMeasurement(long timestamp, 
                             Double c1c, Double l1c, Double d1c, Double s1c,
                             Double c5q, Double l5q, Double d5q, Double s5q,
                             Double azimuth, Double elevation) {
        if (firstTimestamp == 0) {
            firstTimestamp = timestamp;
        }
        
        timestamps.add(timestamp);
        
        // Add L1 measurements
        c1cRange.add(c1c != null ? c1c : Double.NaN);
        l1cPhase.add(l1c != null ? l1c : Double.NaN);
        d1cDoppler.add(d1c != null ? d1c : Double.NaN);
        s1cSnr.add(s1c != null && s1c > 0 ? s1c : Double.NaN);
        
        // Add L5 measurements
        c5qRange.add(c5q != null ? c5q : Double.NaN);
        l5qPhase.add(l5q != null ? l5q : Double.NaN);
        d5qDoppler.add(d5q != null ? d5q : Double.NaN);
        s5qSnr.add(s5q != null && s5q > 0 ? s5q : Double.NaN);
        
        // Azimuth and elevation will be calculated later based on satellite position
    }
    
    // Get average SNR over all measurements
    public double getAverageSnr() {
        double sum = 0;
        int count = 0;
        
        // Consider both L1 and L5 SNR values
        for (Double snr : s1cSnr) {
            if (!snr.isNaN()) {
                sum += snr;
                count++;
            }
        }
        
        for (Double snr : s5qSnr) {
            if (!snr.isNaN()) {
                sum += snr;
                count++;
            }
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    // Get the most recent SNR measurement
    public double getCurrentSnr() {
        if (!s1cSnr.isEmpty() && !s1cSnr.get(s1cSnr.size() - 1).isNaN()) {
            return s1cSnr.get(s1cSnr.size() - 1);
        }
        if (!s5qSnr.isEmpty() && !s5qSnr.get(s5qSnr.size() - 1).isNaN()) {
            return s5qSnr.get(s5qSnr.size() - 1);
        }
        return 0;
    }
    
    // Get satellite identifier (e.g., G01, R02)
    public String getSatId() {
        return system + String.format("%02d", satID);
    }
    
    // Get latest pseudorange measurement
    public double getLatestPseudorange() {
        if (!c1cRange.isEmpty() && !c1cRange.get(c1cRange.size() - 1).isNaN()) {
            return c1cRange.get(c1cRange.size() - 1);
        }
        if (!c5qRange.isEmpty() && !c5qRange.get(c5qRange.size() - 1).isNaN()) {
            return c5qRange.get(c5qRange.size() - 1);
        }
        return Double.NaN;
    }
    
    // Get latest carrier phase measurement
    public double getLatestCarrierPhase() {
        if (!l1cPhase.isEmpty() && !l1cPhase.get(l1cPhase.size() - 1).isNaN()) {
            return l1cPhase.get(l1cPhase.size() - 1);
        }
        if (!l5qPhase.isEmpty() && !l5qPhase.get(l5qPhase.size() - 1).isNaN()) {
            return l5qPhase.get(l5qPhase.size() - 1);
        }
        return Double.NaN;
    }
    
    // Get latest Doppler measurement
    public double getLatestDoppler() {
        if (!d1cDoppler.isEmpty() && !d1cDoppler.get(d1cDoppler.size() - 1).isNaN()) {
            return d1cDoppler.get(d1cDoppler.size() - 1);
        }
        if (!d5qDoppler.isEmpty() && !d5qDoppler.get(d5qDoppler.size() - 1).isNaN()) {
            return d5qDoppler.get(d5qDoppler.size() - 1);
        }
        return Double.NaN;
    }
    
    // Get latest azimuth
    public double getLatestAzimuth() {
        if (!azimuthHistory.isEmpty()) {
            return azimuthHistory.get(azimuthHistory.size() - 1);
        }
        return Double.NaN;
    }
    
    // Get latest elevation
    public double getLatestElevation() {
        if (!elevationHistory.isEmpty()) {
            return elevationHistory.get(elevationHistory.size() - 1);
        }
        return Double.NaN;
    }
    
    // Get all SNR measurements as array
    public double[] getSnrArray() {
        double[] snrArray = new double[s1cSnr.size()];
        for (int i = 0; i < s1cSnr.size(); i++) {
            if (!s1cSnr.get(i).isNaN()) {
                snrArray[i] = s1cSnr.get(i);
            } else if (i < s5qSnr.size() && !s5qSnr.get(i).isNaN()) {
                snrArray[i] = s5qSnr.get(i);
            } else {
                snrArray[i] = 0.0;
            }
        }
        return snrArray;
    }
    
    // Getters and setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public double getVx() { return vx; }
    public void setVx(double vx) { this.vx = vx; }
    
    public double getVy() { return vy; }
    public void setVy(double vy) { this.vy = vy; }
    
    public double getVz() { return vz; }
    public void setVz(double vz) { this.vz = vz; }
    
    public double getAzimuth() { return azimuth; }
    public void setAzimuth(double azimuth) { this.azimuth = azimuth; }
    
    public double getElevation() { return elevation; }
    public void setElevation(double elevation) { this.elevation = elevation; }
    
    public int getSatID() { return satID; }
    public char getSystem() { return system; }
    
    public List<Double> getL1cPhase() { return new ArrayList<>(l1cPhase); }
    public List<Double> getL5qPhase() { return new ArrayList<>(l5qPhase); }
    public List<Double> getC1cRange() { return new ArrayList<>(c1cRange); }
    public List<Double> getC5qRange() { return new ArrayList<>(c5qRange); }
    public List<Double> getD1cDoppler() { return new ArrayList<>(d1cDoppler); }
    public List<Double> getD5qDoppler() { return new ArrayList<>(d5qDoppler); }
    public List<Double> getS1cSnr() { return new ArrayList<>(s1cSnr); }
    public List<Double> getS5qSnr() { return new ArrayList<>(s5qSnr); }
    public List<Long> getTimestamps() { return new ArrayList<>(timestamps); }
    
    public List<Double> getAzimuthHistory() { return new ArrayList<>(azimuthHistory); }
    public List<Double> getElevationHistory() { return new ArrayList<>(elevationHistory); }
    
    public void setSatPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public static List<Sat> createFromRINEXFile(String rinexFilePath) {
        RINEXParser parser = new RINEXParser();
        return parser.parseSatellitesFromRINEX(rinexFilePath);
    }
    
    @Override
    public String toString() {
        return String.format("%c%02d - Az: %.2f°, El: %.2f°, SNR: %.1f, Measurements: %d",
            system, satID, azimuth, elevation, getCurrentSnr(), timestamps.size());
    }
}
