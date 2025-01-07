package GNSS;

import java.io.File;
import java.util.List;

public class NMEATest {
    public static void main(String[] args) {
        // Get the current working directory (src folder)
        String srcDir = new File("").getAbsolutePath();
        // Go up one level to the project root
        String projectDir = new File(srcDir).getParent();
        String filePath = new File(projectDir, "logs/gnss_log_2024_12_24_10_57_30.nmea").getAbsolutePath();
        
        // Parse satellites from the NMEA file using NMEAParser
        NMEAParser parser = new NMEAParser();
        List<Sat> satellites = parser.parseSatellitesFromNMEA(filePath);
        
        System.out.println("Found " + satellites.size() + " satellites:");
        for (Sat sat : satellites) {
            System.out.println(sat.toString());
            System.out.println("--------------------");
        }
    }
}
