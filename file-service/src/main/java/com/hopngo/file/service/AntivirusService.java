package com.hopngo.file.service;

import com.hopngo.file.model.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Antivirus service using ClamAV for file scanning
 * Provides malware detection capabilities for uploaded files
 */
@Service
public class AntivirusService {

    private static final Logger logger = LoggerFactory.getLogger(AntivirusService.class);

    @Value("${clamav.host:localhost}")
    private String clamavHost;

    @Value("${clamav.port:3310}")
    private int clamavPort;

    @Value("${clamav.timeout:30000}")
    private int clamavTimeout;

    @Value("${clamav.enabled:true}")
    private boolean clamavEnabled;

    private static final String CLEAN_RESPONSE = "stream: OK";
    private static final String FOUND_RESPONSE = "FOUND";
    private static final int CHUNK_SIZE = 2048;

    /**
     * Scan file for malware using ClamAV
     */
    public ScanResult scanFile(InputStream fileStream) {
        if (!clamavEnabled) {
            logger.debug("ClamAV scanning is disabled, allowing file");
            return new ScanResult(true, null, "Scanning disabled");
        }

        try {
            logger.debug("Starting antivirus scan");
            
            // Convert InputStream to byte array for scanning
            byte[] fileBytes = readAllBytes(fileStream);
            
            if (fileBytes.length == 0) {
                return new ScanResult(false, "EMPTY_FILE", "File is empty");
            }

            // Perform ClamAV scan
            String scanResponse = performClamAVScan(fileBytes);
            
            return parseScanResponse(scanResponse);
            
        } catch (Exception e) {
            logger.error("Antivirus scan failed", e);
            return new ScanResult(false, "SCAN_ERROR", "Scan failed: " + e.getMessage());
        }
    }

    /**
     * Perform ClamAV scan using INSTREAM command
     */
    private String performClamAVScan(byte[] fileBytes) throws IOException {
        try (Socket socket = new Socket(clamavHost, clamavPort)) {
            socket.setSoTimeout(clamavTimeout);
            
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Send INSTREAM command
                out.writeBytes("zINSTREAM\0");
                out.flush();
                
                // Send file data in chunks
                sendFileInChunks(out, fileBytes);
                
                // Send zero-length chunk to indicate end of data
                out.writeInt(0);
                out.flush();
                
                // Read response
                String response = in.readLine();
                logger.debug("ClamAV response: {}", response);
                
                return response;
            }
        }
    }

    /**
     * Send file data in chunks to ClamAV
     */
    private void sendFileInChunks(DataOutputStream out, byte[] fileBytes) throws IOException {
        int offset = 0;
        
        while (offset < fileBytes.length) {
            int chunkSize = Math.min(CHUNK_SIZE, fileBytes.length - offset);
            
            // Send chunk size (4 bytes, network byte order)
            out.writeInt(chunkSize);
            
            // Send chunk data
            out.write(fileBytes, offset, chunkSize);
            out.flush();
            
            offset += chunkSize;
        }
    }

    /**
     * Parse ClamAV scan response
     */
    private ScanResult parseScanResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return new ScanResult(false, "NO_RESPONSE", "No response from antivirus scanner");
        }

        response = response.trim();
        
        if (response.contains(CLEAN_RESPONSE)) {
            logger.debug("File scan result: CLEAN");
            return new ScanResult(true, null, "File is clean");
        } else if (response.contains(FOUND_RESPONSE)) {
            // Extract threat name
            String threatName = extractThreatName(response);
            logger.warn("File scan result: INFECTED - {}", threatName);
            return new ScanResult(false, threatName, "Malware detected: " + threatName);
        } else {
            logger.warn("Unknown scan response: {}", response);
            return new ScanResult(false, "UNKNOWN_RESPONSE", "Unknown scanner response: " + response);
        }
    }

    /**
     * Extract threat name from ClamAV response
     */
    private String extractThreatName(String response) {
        try {
            // ClamAV response format: "stream: ThreatName FOUND"
            String[] parts = response.split(":");
            if (parts.length > 1) {
                String threatPart = parts[1].trim();
                return threatPart.replace(" FOUND", "").trim();
            }
        } catch (Exception e) {
            logger.warn("Failed to extract threat name from response: {}", response, e);
        }
        
        return "UNKNOWN_THREAT";
    }

    /**
     * Read all bytes from InputStream
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[CHUNK_SIZE];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }

    /**
     * Check if ClamAV service is available
     */
    public boolean isServiceAvailable() {
        if (!clamavEnabled) {
            return false;
        }

        try (Socket socket = new Socket(clamavHost, clamavPort)) {
            socket.setSoTimeout(5000); // 5 second timeout for health check
            
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Send PING command
                out.writeBytes("zPING\0");
                out.flush();
                
                String response = in.readLine();
                return "PONG".equals(response);
            }
        } catch (Exception e) {
            logger.warn("ClamAV service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get ClamAV version information
     */
    public String getVersion() {
        if (!clamavEnabled) {
            return "Disabled";
        }

        try (Socket socket = new Socket(clamavHost, clamavPort)) {
            socket.setSoTimeout(5000);
            
            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Send VERSION command
                out.writeBytes("zVERSION\0");
                out.flush();
                
                return in.readLine();
            }
        } catch (Exception e) {
            logger.warn("Failed to get ClamAV version: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Scan file with timeout
     */
    public ScanResult scanFileWithTimeout(InputStream fileStream, long timeoutSeconds) {
        try {
            // Create a task for scanning
            java.util.concurrent.Future<ScanResult> future = 
                java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> scanFile(fileStream));
            
            // Wait for result with timeout
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("Antivirus scan timed out after {} seconds", timeoutSeconds);
            return new ScanResult(false, "SCAN_TIMEOUT", "Scan timed out");
        } catch (Exception e) {
            logger.error("Antivirus scan failed with timeout", e);
            return new ScanResult(false, "SCAN_ERROR", "Scan failed: " + e.getMessage());
        }
    }

    /**
     * Batch scan multiple files
     */
    public java.util.Map<String, ScanResult> scanFiles(java.util.Map<String, InputStream> files) {
        java.util.Map<String, ScanResult> results = new java.util.HashMap<>();
        
        for (java.util.Map.Entry<String, InputStream> entry : files.entrySet()) {
            String filename = entry.getKey();
            InputStream fileStream = entry.getValue();
            
            try {
                ScanResult result = scanFile(fileStream);
                results.put(filename, result);
                
                if (!result.isClean()) {
                    logger.warn("File {} failed antivirus scan: {}", filename, result.getThreatName());
                }
            } catch (Exception e) {
                logger.error("Failed to scan file: {}", filename, e);
                results.put(filename, new ScanResult(false, "SCAN_ERROR", e.getMessage()));
            }
        }
        
        return results;
    }

    /**
     * Get scan statistics
     */
    public ScanStats getScanStats() {
        // This would typically be implemented with metrics collection
        // For now, return basic service status
        ScanStats stats = new ScanStats();
        stats.setServiceEnabled(clamavEnabled);
        stats.setServiceAvailable(isServiceAvailable());
        stats.setVersion(getVersion());
        
        return stats;
    }

    public static class ScanStats {
        private boolean serviceEnabled;
        private boolean serviceAvailable;
        private String version;
        private long totalScans;
        private long cleanFiles;
        private long infectedFiles;
        private long scanErrors;

        // Getters and setters
        public boolean isServiceEnabled() { return serviceEnabled; }
        public void setServiceEnabled(boolean serviceEnabled) { this.serviceEnabled = serviceEnabled; }
        
        public boolean isServiceAvailable() { return serviceAvailable; }
        public void setServiceAvailable(boolean serviceAvailable) { this.serviceAvailable = serviceAvailable; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public long getTotalScans() { return totalScans; }
        public void setTotalScans(long totalScans) { this.totalScans = totalScans; }
        
        public long getCleanFiles() { return cleanFiles; }
        public void setCleanFiles(long cleanFiles) { this.cleanFiles = cleanFiles; }
        
        public long getInfectedFiles() { return infectedFiles; }
        public void setInfectedFiles(long infectedFiles) { this.infectedFiles = infectedFiles; }
        
        public long getScanErrors() { return scanErrors; }
        public void setScanErrors(long scanErrors) { this.scanErrors = scanErrors; }
    }
}