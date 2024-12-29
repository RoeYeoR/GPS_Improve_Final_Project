package GNSS;

import java.util.List;

public class NMEATest {
    public static void main(String[] args) {
        String filePath = "c:/Users/A/Desktop/לימודים/FinalProjectGPS/Final_project-main-master/gnss_log_2024_12_24_10_57_30.nmea";
        
        // Get satellites directly from the NMEA file
        List<Sat> satellites = Sat.createFromNMEAFile(filePath);
        
        System.out.println("Found " + satellites.size() + " satellites:");
        for (Sat sat : satellites) {
            System.out.println(sat.toString());
            System.out.println("SNR History: " + sat.getSnrHistory());
            System.out.println("Average SNR: " + String.format("%.2f", sat.getAverageSNR()));
            System.out.println("--------------------");
        }
    }
}
