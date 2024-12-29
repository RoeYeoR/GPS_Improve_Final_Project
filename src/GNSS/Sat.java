package GNSS;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a satellite with its position, velocity, azimuth, elevation, and SNR history.
 */
public class Sat {

    private double x, y, z;
    private double vx, vy, vz;
    private double azimuth;
    private double elevetion;
    private int satID;
    private List<Integer> snrHistory;  // List to store SNR values for each second
    private double averageSNR;         // Average SNR over all measurements
    private long firstTimestamp;       // First timestamp in milliseconds
    private int currentSNR;           // Current SNR value
    
    /**
     * Constructs a new Sat object with the given azimuth, elevation, satellite ID, and initial SNR.
     *
     * @param azimuth  the azimuth angle of the satellite
     * @param elevetion the elevation angle of the satellite
     * @param satID    the identifier of the satellite
     * @param snr      the initial SNR value
     */
    public Sat(double azimuth, double elevetion, int satID, int snr) {
        this.azimuth = azimuth;
        this.elevetion = elevetion;
        this.satID = satID;
        this.snrHistory = new ArrayList<>();
        this.firstTimestamp = 0;
        this.currentSNR = snr;
    }

    /**
     * Creates a list of satellites from an NMEA log file.
     * 
     * @param nmeaFilePath Path to the NMEA log file
     * @return List of Sat objects with their complete SNR history
     */
    public static List<Sat> createFromNMEAFile(String nmeaFilePath) {
        NMEAParser parser = new NMEAParser();
        return parser.parseSatellitesFromNMEA(nmeaFilePath);
    }

    /**
     * Updates the SNR for a specific timestamp.
     *
     * @param snr       the new SNR value
     * @param timestamp the timestamp in milliseconds
     */
    public void updateSNR(int snr, long timestamp) {
        this.currentSNR = snr;
        
        if (firstTimestamp == 0) {
            firstTimestamp = timestamp;
        }
        
        // Calculate second index based on timestamp difference
        int secondIndex = (int)(timestamp / 1000);
        
        // Ensure the list has enough capacity
        while (snrHistory.size() <= secondIndex) {
            snrHistory.add(currentSNR);  // Fill with current SNR value
        }
        
        // Update SNR at the correct second
        snrHistory.set(secondIndex, snr);
        
        // Recalculate average SNR
        updateAverageSNR();
    }
    
    /**
     * Updates the average SNR based on the current SNR history.
     */
    private void updateAverageSNR() {
        if (snrHistory.isEmpty()) {
            averageSNR = 0;
            return;
        }
        
        double sum = 0;
        int count = 0;
        for (int snr : snrHistory) {
            if (snr > 0) {  // Only count non-zero SNR values
                sum += snr;
                count++;
            }
        }
        averageSNR = count > 0 ? sum / count : 0;
    }

    /**
     * Gets the x-coordinate of the satellite's position.
     *
     * @return the x-coordinate
     */
    public double getX() { return x; }
    /**
     * Sets the x-coordinate of the satellite's position.
     *
     * @param x the new x-coordinate
     */
    public void setX(double x) { this.x = x; }
    
    /**
     * Gets the y-coordinate of the satellite's position.
     *
     * @return the y-coordinate
     */
    public double getY() { return y; }
    /**
     * Sets the y-coordinate of the satellite's position.
     *
     * @param y the new y-coordinate
     */
    public void setY(double y) { this.y = y; }
    
    /**
     * Gets the z-coordinate of the satellite's position.
     *
     * @return the z-coordinate
     */
    public double getZ() { return z; }
    /**
     * Sets the z-coordinate of the satellite's position.
     *
     * @param z the new z-coordinate
     */
    public void setZ(double z) { this.z = z; }
    
    /**
     * Gets the x-component of the satellite's velocity.
     *
     * @return the x-component
     */
    public double getVx() { return vx; }
    /**
     * Sets the x-component of the satellite's velocity.
     *
     * @param vx the new x-component
     */
    public void setVx(double vx) { this.vx = vx; }
    
    /**
     * Gets the y-component of the satellite's velocity.
     *
     * @return the y-component
     */
    public double getVy() { return vy; }
    /**
     * Sets the y-component of the satellite's velocity.
     *
     * @param vy the new y-component
     */
    public void setVy(double vy) { this.vy = vy; }
    
    /**
     * Gets the z-component of the satellite's velocity.
     *
     * @return the z-component
     */
    public double getVz() { return vz; }
    /**
     * Sets the z-component of the satellite's velocity.
     *
     * @param vz the new z-component
     */
    public void setVz(double vz) { this.vz = vz; }
    
    /**
     * Gets the azimuth angle of the satellite.
     *
     * @return the azimuth angle
     */
    public double getAzimuth() { return azimuth; }
    /**
     * Sets the azimuth angle of the satellite.
     *
     * @param azimuth the new azimuth angle
     */
    public void setAzimuth(double azimuth) { this.azimuth = azimuth; }
    
    /**
     * Gets the elevation angle of the satellite.
     *
     * @return the elevation angle
     */
    public double getElevetion() { return elevetion; }
    /**
     * Sets the elevation angle of the satellite.
     *
     * @param elevetion the new elevation angle
     */
    public void setElevetion(double elevetion) { this.elevetion = elevetion; }
    
    /**
     * Gets the identifier of the satellite.
     *
     * @return the satellite ID
     */
    public int getSatID() { return satID; }
    
    /**
     * Gets the SNR history of the satellite.
     *
     * @return a copy of the SNR history list
     */
    public List<Integer> getSnrHistory() { return new ArrayList<>(snrHistory); }
    
    /**
     * Gets the average SNR of the satellite.
     *
     * @return the average SNR
     */
    public double getAverageSNR() { return averageSNR; }
    
    /**
     * Gets the single SNR value of the satellite.
     *
     * @return the single SNR value
     */
    public int getSingleSNR() { return currentSNR; }
    
    /**
     * Sets the single SNR value of the satellite.
     *
     * @param snr the new single SNR value
     */
    public void setSingleSNR(int snr) {
        this.currentSNR = snr;
    }
    
    @Override
    public String toString() {
        return "Satellite ID: " + satID + 
               ", Azimuth: " + String.format("%.2f", azimuth) + 
               ", Elevation: " + String.format("%.2f", elevetion) + 
               ", Current SNR: " + currentSNR +
               ", History Size: " + snrHistory.size() + " seconds" +
               ", Average SNR: " + String.format("%.2f", averageSNR);
    }
}
