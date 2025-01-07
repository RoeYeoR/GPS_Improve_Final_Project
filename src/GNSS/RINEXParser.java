package GNSS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RINEXParser {
    private static final Pattern EPOCH_PATTERN = Pattern.compile(
        "^>(\\s*\\d{4})(\\s*\\d{1,2})(\\s*\\d{1,2})(\\s*\\d{1,2})(\\s*\\d{1,2})(\\s*\\d+\\.\\d+)\\s*\\d+\\s*\\d+\\s*");
    
    // Updated pattern for azimuth and elevation (assuming they are in degrees with decimal points)
    private static final Pattern AZ_EL_PATTERN = Pattern.compile(
        ".*\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s*$");
    
    private Map<String, Sat> satellites = new HashMap<>();
    private long currentEpochTime = 0;  // Move to class level
    
    public List<Sat> parseSatellitesFromRINEX(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean headerEnd = false;
            Map<Character, List<String>> observationTypes = new HashMap<>();
            
            // Parse header
            while ((line = reader.readLine()) != null && !headerEnd) {
                if (line.contains("END OF HEADER")) {
                    headerEnd = true;
                    continue;
                }
                
                // Parse observation types for each satellite system
                if (line.contains("SYS / # / OBS TYPES")) {
                    char system = line.charAt(0);
                    String[] parts = line.substring(1).trim().split("\\s+");
                    List<String> types = new ArrayList<>();
                    for (int i = 1; i < parts.length && !parts[i].equals("SYS"); i++) {
                        types.add(parts[i]);
                    }
                    observationTypes.put(system, types);
                }
            }
            
            // Parse observations
            while ((line = reader.readLine()) != null) {
                Matcher epochMatcher = EPOCH_PATTERN.matcher(line);
                if (epochMatcher.find()) {
                    // Parse epoch timestamp
                    int year = Integer.parseInt(epochMatcher.group(1).trim());
                    int month = Integer.parseInt(epochMatcher.group(2).trim());
                    int day = Integer.parseInt(epochMatcher.group(3).trim());
                    int hour = Integer.parseInt(epochMatcher.group(4).trim());
                    int minute = Integer.parseInt(epochMatcher.group(5).trim());
                    double seconds = Double.parseDouble(epochMatcher.group(6).trim());
                    
                    LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, (int)seconds);
                    currentEpochTime = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
                    continue;
                }
                
                // Parse satellite measurements
                if (line.length() >= 3 && Character.isLetter(line.charAt(0))) {
                    parseMeasurementLine(line);
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new ArrayList<>(satellites.values());
    }
    
    private List<Double> parseMeasurements(String line) {
        List<Double> measurements = new ArrayList<>();
        int start = 3; // Skip system and satellite ID
        char system = line.charAt(0);
        
        while (start < line.length()) {
            int end = Math.min(start + 16, line.length());
            String measurement = line.substring(start, end).trim();
            
            if (measurement.isEmpty()) {
                measurements.add(Double.NaN);
            } else {
                try {
                    double value = Double.parseDouble(measurement);
                    measurements.add(value);
                } catch (NumberFormatException e) {
                    measurements.add(Double.NaN);
                }
            }
            
            start = end;
        }
        
        return measurements;
    }
    
    private void parseMeasurementLine(String line) {
        if (line.trim().isEmpty()) return;
        
        char system = line.charAt(0);
        int satId = Integer.parseInt(line.substring(1, 3).trim());
        List<Double> measurements = parseMeasurements(line);
        
        // Get or create satellite
        String satKey = String.format("%c%02d", system, satId);
        Sat sat = satellites.computeIfAbsent(satKey, k -> new Sat(system, satId));
        
        // Parse measurements based on satellite system
        Double c1c = null, l1c = null, d1c = null, s1c = null;
        Double c5q = null, l5q = null, d5q = null, s5q = null;
        
        if (system == 'C') {  // BeiDou
            // Use C2I, L2I, D2I, S2I instead of C1C, L1C, D1C, S1C
            if (measurements.size() >= 4) {
                c1c = measurements.get(0);  // C2I
                l1c = measurements.get(1);  // L2I
                d1c = measurements.get(2);  // D2I
                s1c = measurements.get(3);  // S2I
            }
            if (measurements.size() >= 8) {
                c5q = measurements.get(4);  // C5P
                l5q = measurements.get(5);  // L5P
                d5q = measurements.get(6);  // D5P
                s5q = measurements.get(7);  // S5P
            }
        } else {  // GPS, GLONASS, Galileo
            if (measurements.size() >= 4) {
                c1c = measurements.get(0);
                l1c = measurements.get(1);
                d1c = measurements.get(2);
                s1c = measurements.get(3);
            }
            if (measurements.size() >= 8) {
                c5q = measurements.get(4);
                l5q = measurements.get(5);
                d5q = measurements.get(6);
                s5q = measurements.get(7);
            }
        }
        
        // Add measurements to satellite
        sat.addMeasurement(currentEpochTime, c1c, l1c, d1c, s1c, c5q, l5q, d5q, s5q, null, null);
    }
    
    // Get the latest measurements for all satellites
    public List<SatelliteMeasurement> getLatestMeasurements() {
        List<SatelliteMeasurement> measurements = new ArrayList<>();
        for (Sat sat : satellites.values()) {
            measurements.add(new SatelliteMeasurement(
                sat.getSatId(),
                sat.getLatestPseudorange(),
                sat.getLatestCarrierPhase(),
                sat.getLatestDoppler(),
                sat.getSnrArray(),
                sat.getLatestAzimuth(),
                sat.getLatestElevation()
            ));
        }
        return measurements;
    }
    
    // Get a list of satellites from a RINEX file
    public List<String> getSatelliteList(String filePath) throws IOException {
        Set<String> uniqueSatellites = new TreeSet<>();  // Using TreeSet for sorted output
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean headerEnd = false;
            
            // Skip header
            while ((line = reader.readLine()) != null) {
                if (line.contains("END OF HEADER")) {
                    headerEnd = true;
                    break;
                }
            }
            
            // Process measurements
            if (headerEnd) {
                while ((line = reader.readLine()) != null) {
                    // Skip epoch headers
                    if (line.startsWith(">")) {
                        continue;
                    }
                    
                    // Parse satellite ID if line starts with a system identifier
                    if (line.length() >= 3 && Character.isLetter(line.charAt(0))) {
                        char system = line.charAt(0);
                        String satId = line.substring(0, 3).trim();
                        String systemName = getSystemName(system);
                        uniqueSatellites.add(String.format("%s (%s)", satId, systemName));
                    }
                }
            }
        }
        
        return new ArrayList<>(uniqueSatellites);
    }
    
    private String getSystemName(char system) {
        switch (system) {
            case 'G': return "GPS";
            case 'R': return "GLONASS";
            case 'E': return "Galileo";
            case 'C': return "BeiDou";
            default: return "Unknown";
        }
    }
    
    // Inner class to hold satellite measurements
    public static class SatelliteMeasurement {
        private final String satId;
        private final double pseudorange;
        private final double carrierPhase;
        private final double doppler;
        private final double[] snrHistory;
        private final double azimuth;
        private final double elevation;
        
        public SatelliteMeasurement(String satId, double pseudorange, double carrierPhase, 
                                  double doppler, double[] snrHistory, double azimuth, double elevation) {
            this.satId = satId;
            this.pseudorange = pseudorange;
            this.carrierPhase = carrierPhase;
            this.doppler = doppler;
            this.snrHistory = snrHistory;
            this.azimuth = azimuth;
            this.elevation = elevation;
        }
        
        // Getters
        public String getSatId() { return satId; }
        public double getPseudorange() { return pseudorange; }
        public double getCarrierPhase() { return carrierPhase; }
        public double getDoppler() { return doppler; }
        public double[] getSnrHistory() { return snrHistory; }
        public double getAzimuth() { return azimuth; }
        public double getElevation() { return elevation; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Satellite: %s\n", satId));
            sb.append(String.format("Pseudorange: %.2f meters\n", pseudorange));
            sb.append(String.format("Carrier Phase: %.2f cycles\n", carrierPhase));
            
            if (!Double.isNaN(doppler)) {
                sb.append(String.format("Doppler: %.2f Hz\n", doppler));
            } else {
                sb.append("Doppler: Not Available\n");
            }
            
            sb.append("SNR History:\n");
            for (int i = 0; i < snrHistory.length; i++) {
                if (!Double.isNaN(snrHistory[i]) && snrHistory[i] > 0) {
                    sb.append(String.format("  Time %d: %.2f dB-Hz\n", i, snrHistory[i]));
                }
            }
            
            return sb.toString();
        }
    }
}
