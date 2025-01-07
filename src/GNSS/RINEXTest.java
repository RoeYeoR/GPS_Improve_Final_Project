package GNSS;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RINEXTest {
    public static void main(String[] args) {
        RINEXParser parser = new RINEXParser();
        // Get the current working directory (src folder)
        String srcDir = new File("").getAbsolutePath();
        // Go up one level to the project root
        String projectDir = new File(srcDir).getParent();
        String filePath = new File(projectDir, "logs/gnss_log_2024_12_29_10_48_55.24o").getAbsolutePath();
        
        try {
            // Get list of satellites first
            List<String> satelliteList = parser.getSatelliteList(filePath);
            System.out.println("Satellites found in file: " + satelliteList.size());
            for (String sat : satelliteList) {
                System.out.println(sat);
            }
            System.out.println("\nDetailed measurements:");
            
            // Then get detailed measurements
            parser.parseSatellitesFromRINEX(filePath);
            List<RINEXParser.SatelliteMeasurement> measurements = parser.getLatestMeasurements();
            
            for (RINEXParser.SatelliteMeasurement measurement : measurements) {
                System.out.println("Satellite: " + measurement.getSatId());
                System.out.println("Pseudorange: " + String.format("%.2f meters", measurement.getPseudorange()));
                System.out.println("Carrier Phase: " + String.format("%.2f cycles", measurement.getCarrierPhase()));
                System.out.println("Doppler: " + String.format("%.2f Hz", measurement.getDoppler()));
                
                double[] snrHistory = measurement.getSnrHistory();
                System.out.println("SNR History:");
                for (int i = 0; i < Math.min(5, snrHistory.length); i++) {
                    System.out.printf("  Time %d: %.2f dB-Hz%n", i, snrHistory[i]);
                }
                
                if (snrHistory.length > 5) {
                    System.out.println("  ... (" + (snrHistory.length - 5) + " more measurements)");
                }
                
                System.out.println("--------------------");
            }
            
        } catch (IOException e) {
            System.err.println("Error reading RINEX file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
