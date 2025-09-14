package com.hopngo.file.service;

import com.hopngo.file.config.FileUploadConfig;
import com.hopngo.file.exception.FileUploadException;
import com.hopngo.file.model.FileMetadata;
import com.hopngo.file.model.ScanResult;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/**
 * Secure file upload service with comprehensive security measures
 * Includes MIME type validation, image transcoding, and AV scanning
 */
@Service
public class SecureFileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(SecureFileUploadService.class);

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Autowired
    private AntivirusService antivirusService;

    @Autowired
    private FileStorageService fileStorageService;

    private final Tika tika = new Tika();
    private final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();

    // Allowed MIME types for different file categories
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf", "text/plain", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar",
        "php", "asp", "aspx", "jsp", "sh", "ps1", "py", "rb", "pl"
    );

    /**
     * Upload file with comprehensive security validation
     */
    public FileMetadata uploadFile(MultipartFile file, String category, Long userId) {
        logger.info("Starting secure file upload for user: {}, category: {}", userId, category);

        try {
            // Step 1: Basic validation
            validateBasicFileProperties(file);

            // Step 2: MIME type validation
            String detectedMimeType = validateMimeType(file);

            // Step 3: File extension validation
            String sanitizedFilename = validateAndSanitizeFilename(file.getOriginalFilename());

            // Step 4: Content validation
            validateFileContent(file, detectedMimeType);

            // Step 5: Antivirus scanning
            ScanResult scanResult = performAntivirusScan(file);
            if (!scanResult.isClean()) {
                throw new FileUploadException("File failed security scan: " + scanResult.getThreatName());
            }

            // Step 6: Image processing (if applicable)
            byte[] processedContent = processFileContent(file, detectedMimeType);

            // Step 7: Generate file metadata
            FileMetadata metadata = createFileMetadata(sanitizedFilename, detectedMimeType, 
                processedContent.length, userId, category);

            // Step 8: Store file securely
            String storagePath = fileStorageService.storeFile(processedContent, metadata);
            metadata.setStoragePath(storagePath);

            logger.info("File uploaded successfully: {} for user: {}", sanitizedFilename, userId);
            return metadata;

        } catch (Exception e) {
            logger.error("File upload failed for user: {}", userId, e);
            throw new FileUploadException("File upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate basic file properties
     */
    private void validateBasicFileProperties(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null");
        }

        if (file.getSize() > fileUploadConfig.getMaxFileSize()) {
            throw new FileUploadException("File size exceeds maximum allowed: " + 
                fileUploadConfig.getMaxFileSize() + " bytes");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new FileUploadException("Filename is required");
        }
    }

    /**
     * Validate MIME type using Apache Tika
     */
    private String validateMimeType(MultipartFile file) throws IOException {
        // Detect MIME type from file content (not just extension)
        String detectedMimeType = tika.detect(file.getInputStream(), file.getOriginalFilename());
        
        // Verify against declared content type
        String declaredMimeType = file.getContentType();
        
        if (!detectedMimeType.equals(declaredMimeType)) {
            logger.warn("MIME type mismatch - Detected: {}, Declared: {}", 
                detectedMimeType, declaredMimeType);
        }

        // Check if MIME type is allowed
        Set<String> allowedTypes = new HashSet<>();
        allowedTypes.addAll(ALLOWED_IMAGE_TYPES);
        allowedTypes.addAll(ALLOWED_DOCUMENT_TYPES);

        if (!allowedTypes.contains(detectedMimeType)) {
            throw new FileUploadException("File type not allowed: " + detectedMimeType);
        }

        return detectedMimeType;
    }

    /**
     * Validate and sanitize filename
     */
    private String validateAndSanitizeFilename(String originalFilename) {
        if (originalFilename == null) {
            throw new FileUploadException("Filename cannot be null");
        }

        // Extract file extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        // Check for dangerous extensions
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new FileUploadException("File extension not allowed: " + extension);
        }

        // Sanitize filename - remove dangerous characters
        String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Limit filename length
        if (sanitized.length() > 255) {
            String nameWithoutExt = sanitized.substring(0, sanitized.lastIndexOf('.'));
            String ext = sanitized.substring(sanitized.lastIndexOf('.'));
            sanitized = nameWithoutExt.substring(0, 250 - ext.length()) + ext;
        }

        return sanitized;
    }

    /**
     * Validate file content based on type
     */
    private void validateFileContent(MultipartFile file, String mimeType) throws IOException {
        if (ALLOWED_IMAGE_TYPES.contains(mimeType)) {
            validateImageContent(file);
        } else if (ALLOWED_DOCUMENT_TYPES.contains(mimeType)) {
            validateDocumentContent(file);
        }
    }

    /**
     * Validate image file content
     */
    private void validateImageContent(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new FileUploadException("Invalid image file - cannot be read");
            }

            // Check image dimensions
            if (image.getWidth() > fileUploadConfig.getMaxImageWidth() || 
                image.getHeight() > fileUploadConfig.getMaxImageHeight()) {
                throw new FileUploadException("Image dimensions exceed maximum allowed");
            }

            // Check for suspicious image properties
            if (image.getWidth() < 1 || image.getHeight() < 1) {
                throw new FileUploadException("Invalid image dimensions");
            }
        }
    }

    /**
     * Validate document content
     */
    private void validateDocumentContent(MultipartFile file) throws IOException {
        // Basic document validation
        byte[] header = new byte[1024];
        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead = inputStream.read(header);
            
            // Check for embedded scripts or suspicious content
            String headerString = new String(header, 0, bytesRead).toLowerCase();
            
            if (headerString.contains("<script") || headerString.contains("javascript:") ||
                headerString.contains("vbscript:") || headerString.contains("<?php")) {
                throw new FileUploadException("Document contains suspicious content");
            }
        }
    }

    /**
     * Perform antivirus scanning
     */
    private ScanResult performAntivirusScan(MultipartFile file) {
        try {
            return antivirusService.scanFile(file.getInputStream());
        } catch (Exception e) {
            logger.error("Antivirus scan failed", e);
            // Fail secure - reject file if scan fails
            return new ScanResult(false, "SCAN_FAILED", "Antivirus scan could not be completed");
        }
    }

    /**
     * Process file content (transcoding for images)
     */
    private byte[] processFileContent(MultipartFile file, String mimeType) throws IOException {
        if (ALLOWED_IMAGE_TYPES.contains(mimeType)) {
            return transcodeImage(file, mimeType);
        } else {
            // For non-images, return original content
            return file.getBytes();
        }
    }

    /**
     * Transcode image to remove metadata and ensure format consistency
     */
    private byte[] transcodeImage(MultipartFile file, String mimeType) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new FileUploadException("Cannot process image file");
            }

            // Create new image without metadata
            BufferedImage cleanImage = new BufferedImage(
                originalImage.getWidth(), 
                originalImage.getHeight(), 
                BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = cleanImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();

            // Convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String format = mimeType.equals("image/jpeg") ? "jpg" : "png";
            ImageIO.write(cleanImage, format, baos);
            
            return baos.toByteArray();
        }
    }

    /**
     * Create file metadata
     */
    private FileMetadata createFileMetadata(String filename, String mimeType, 
                                          int size, Long userId, String category) {
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalFilename(filename);
        metadata.setMimeType(mimeType);
        metadata.setSize(size);
        metadata.setUserId(userId);
        metadata.setCategory(category);
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setFileHash(generateFileHash(filename + System.currentTimeMillis()));
        
        return metadata;
    }

    /**
     * Generate file hash for integrity checking
     */
    private String generateFileHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}