package com.hopngo.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TotpService {
    
    private static final String ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP = 30; // 30 seconds
    private static final int DIGITS = 6;
    private static final String ISSUER = "HopNGo";
    
    /**
     * Generate a new TOTP secret key
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }
    
    /**
     * Generate TOTP URI for QR code
     */
    public String generateTotpUri(String secret, String userEmail) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            ISSUER, userEmail, secret, ISSUER
        );
    }
    
    /**
     * Generate QR code image as Base64 string
     */
    public String generateQrCode(String totpUri) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(totpUri, BarcodeFormat.QR_CODE, 200, 200);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    /**
     * Verify TOTP code
     */
    public boolean verifyTotp(String secret, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000L;
            long timeWindow = currentTime / TIME_STEP;
            
            // Check current window and ±1 window for clock skew tolerance
            for (int i = -1; i <= 1; i++) {
                String expectedCode = generateTotpCode(secret, timeWindow + i);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate TOTP code for a specific time window
     */
    private String generateTotpCode(String secret, long timeWindow) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Base32 base32 = new Base32();
        byte[] decodedKey = base32.decode(secret);
        
        // Convert time window to byte array
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeWindow);
        byte[] timeBytes = buffer.array();
        
        // Generate HMAC
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(timeBytes);
        
        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0x0F;
        int code = ((hash[offset] & 0x7F) << 24) |
                   ((hash[offset + 1] & 0xFF) << 16) |
                   ((hash[offset + 2] & 0xFF) << 8) |
                   (hash[offset + 3] & 0xFF);
        
        code = code % (int) Math.pow(10, DIGITS);
        
        return String.format("%0" + DIGITS + "d", code);
    }
}