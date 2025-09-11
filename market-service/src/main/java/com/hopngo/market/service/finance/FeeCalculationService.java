package com.hopngo.market.service.finance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeCalculationService {

    /**
     * Calculate platform fee based on order value
     */
    public BigDecimal calculatePlatformFee(BigDecimal orderAmount, String orderType) {
        log.debug("Calculating platform fee for amount: {}, type: {}", orderAmount, orderType);
        
        BigDecimal feeRate = getPlatformFeeRate(orderType);
        BigDecimal fee = orderAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        
        // Minimum fee
        BigDecimal minimumFee = new BigDecimal("1.00");
        if (fee.compareTo(minimumFee) < 0) {
            fee = minimumFee;
        }
        
        log.debug("Platform fee calculated: {} (rate: {})", fee, feeRate);
        return fee;
    }

    /**
     * Calculate processing fee for payment
     */
    public BigDecimal calculateProcessingFee(BigDecimal amount, String paymentMethod) {
        log.debug("Calculating processing fee for amount: {}, method: {}", amount, paymentMethod);
        
        BigDecimal feeRate = getProcessingFeeRate(paymentMethod);
        BigDecimal fixedFee = getFixedProcessingFee(paymentMethod);
        
        BigDecimal percentageFee = amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee = percentageFee.add(fixedFee);
        
        log.debug("Processing fee calculated: {} (percentage: {}, fixed: {})", totalFee, percentageFee, fixedFee);
        return totalFee;
    }

    /**
     * Calculate shipping fee based on weight and distance
     */
    public BigDecimal calculateShippingFee(BigDecimal weight, String shippingMethod, String origin, String destination) {
        log.debug("Calculating shipping fee for weight: {}, method: {}, from: {} to: {}", 
                weight, shippingMethod, origin, destination);
        
        BigDecimal baseFee = getBaseShippingFee(shippingMethod);
        BigDecimal weightFee = weight.multiply(getWeightRate(shippingMethod));
        BigDecimal distanceFee = calculateDistanceFee(origin, destination, shippingMethod);
        
        BigDecimal totalFee = baseFee.add(weightFee).add(distanceFee);
        
        log.debug("Shipping fee calculated: {} (base: {}, weight: {}, distance: {})", 
                totalFee, baseFee, weightFee, distanceFee);
        return totalFee;
    }

    private BigDecimal getPlatformFeeRate(String orderType) {
        switch (orderType.toUpperCase()) {
            case "RENTAL":
                return new BigDecimal("0.08"); // 8% for rentals
            case "PURCHASE":
                return new BigDecimal("0.05"); // 5% for purchases
            case "SERVICE":
                return new BigDecimal("0.10"); // 10% for services
            default:
                return new BigDecimal("0.06"); // 6% default
        }
    }

    private BigDecimal getProcessingFeeRate(String paymentMethod) {
        switch (paymentMethod.toUpperCase()) {
            case "CREDIT_CARD":
            case "STRIPE":
                return new BigDecimal("0.029"); // 2.9%
            case "BKASH":
                return new BigDecimal("0.018"); // 1.8%
            case "NAGAD":
                return new BigDecimal("0.015"); // 1.5%
            case "BANK_TRANSFER":
                return new BigDecimal("0.005"); // 0.5%
            default:
                return new BigDecimal("0.025"); // 2.5% default
        }
    }

    private BigDecimal getFixedProcessingFee(String paymentMethod) {
        switch (paymentMethod.toUpperCase()) {
            case "CREDIT_CARD":
            case "STRIPE":
                return new BigDecimal("0.30"); // $0.30
            case "BKASH":
            case "NAGAD":
                return new BigDecimal("0.00"); // No fixed fee
            case "BANK_TRANSFER":
                return new BigDecimal("1.00"); // $1.00
            default:
                return new BigDecimal("0.25"); // $0.25 default
        }
    }

    private BigDecimal getBaseShippingFee(String shippingMethod) {
        switch (shippingMethod.toUpperCase()) {
            case "STANDARD":
                return new BigDecimal("5.00");
            case "EXPRESS":
                return new BigDecimal("15.00");
            case "OVERNIGHT":
                return new BigDecimal("25.00");
            case "PICKUP":
                return BigDecimal.ZERO;
            default:
                return new BigDecimal("7.50");
        }
    }

    private BigDecimal getWeightRate(String shippingMethod) {
        switch (shippingMethod.toUpperCase()) {
            case "STANDARD":
                return new BigDecimal("1.50"); // $1.50 per kg
            case "EXPRESS":
                return new BigDecimal("2.50"); // $2.50 per kg
            case "OVERNIGHT":
                return new BigDecimal("4.00"); // $4.00 per kg
            default:
                return new BigDecimal("2.00"); // $2.00 per kg
        }
    }

    private BigDecimal calculateDistanceFee(String origin, String destination, String shippingMethod) {
        // Simplified distance calculation - in production would use actual distance calculation
        boolean isLocalDelivery = origin.equals(destination);
        if (isLocalDelivery) {
            return BigDecimal.ZERO;
        }
        
        // Base distance fee
        BigDecimal distanceMultiplier = switch (shippingMethod.toUpperCase()) {
            case "STANDARD" -> new BigDecimal("0.50");
            case "EXPRESS" -> new BigDecimal("1.00");
            case "OVERNIGHT" -> new BigDecimal("2.00");
            default -> new BigDecimal("0.75");
        };
        
        return distanceMultiplier; // Simplified - would calculate actual distance
    }
}