package GNSS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NMEAParser {
    private Map<Integer, Sat> satellites = new HashMap<>();
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss.SS");
    private long currentTimestamp = 0;
    private long startTimestamp = 0;
    
    public List<Sat> parseSatellitesFromNMEA(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("NMEA,")) {
                    line = line.substring(line.indexOf("NMEA,") + 5);
                }
                
                if (line.startsWith("$GPRMC")) {
                    processRMCSentence(line);
                } else if (line.startsWith("$GPGSV") || line.startsWith("$GLGSV")) {
                    processGSVSentence(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new ArrayList<>(satellites.values());
    }
    
    private void processRMCSentence(String sentence) {
        String[] parts = sentence.split(",");
        if (parts.length >= 2) {
            try {
                Date time = timeFormat.parse(parts[1]);
                currentTimestamp = time.getTime();
                if (startTimestamp == 0) {
                    startTimestamp = currentTimestamp;
                }
                
                // Update all satellites with current SNR for this timestamp
                for (Sat sat : satellites.values()) {
                    sat.addMeasurement(currentTimestamp - startTimestamp,
                        null, null, null, sat.getCurrentSnr(),
                        null, null, null, null,
                        sat.getAzimuth(), sat.getElevation());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void processGSVSentence(String sentence) {
        String[] parts = sentence.split(",");
        if (parts.length < 7) return;
        
        try {
            int startIndex = 4;  // Index where satellite data begins
            while (startIndex + 3 < parts.length && !parts[startIndex].contains("*")) {
                int satId = Integer.parseInt(parts[startIndex]);
                double elevation = Double.parseDouble(parts[startIndex + 1]);
                double azimuth = Double.parseDouble(parts[startIndex + 2]);
                int snr = parts[startIndex + 3].isEmpty() ? 0 : Integer.parseInt(parts[startIndex + 3]);
                
                Sat sat = satellites.get(satId);
                if (sat == null) {
                    sat = new Sat('G', satId); // 'G' for GPS satellites
                    sat.setAzimuth(azimuth);
                    sat.setElevation(elevation);
                    satellites.put(satId, sat);
                } else {
                    sat.setAzimuth(azimuth);
                    sat.setElevation(elevation);
                }
                
                // Only update SNR if we have a valid timestamp
                if (currentTimestamp > 0) {
                    sat.addMeasurement(currentTimestamp - startTimestamp,
                        null, null, null, (double)snr,
                        null, null, null, null,
                        azimuth, elevation);
                }
                
                startIndex += 4;
            }
        } catch (NumberFormatException e) {
            // Skip malformed data
        }
    }
    
    public Map<Integer, Sat> getSatellites() {
        return satellites;
    }
}
