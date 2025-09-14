package com.hopngo.service;

import com.hopngo.entity.*;
import com.hopngo.repository.*;
import com.hopngo.util.FinancialCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service for generating tax documentation and compliance reports
 * based on jurisdiction requirements.
 */
@Service
@Transactional
public class TaxDocumentationService {

    private static final Logger logger = LoggerFactory.getLogger(TaxDocumentationService.class);

    @Autowired
    private TaxDocumentRepository taxDocumentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupportTicketService supportTicketService;

    // Tax rates by jurisdiction (configurable)
    private static final Map<String, TaxConfiguration> TAX_CONFIGURATIONS = new HashMap<>();
    
    static {
        // Bangladesh tax configuration
        TAX_CONFIGURATIONS.put("BD", new TaxConfiguration(
            "BD", "Bangladesh", 
            new BigDecimal("15.00"), // VAT rate
            new BigDecimal("10.00"), // Income tax rate
            new BigDecimal("5.00"),  // Withholding tax rate
            new BigDecimal("100000"), // Tax-free threshold (BDT)
            "BDT"
        ));
        
        // US tax configuration
        TAX_CONFIGURATIONS.put("US", new TaxConfiguration(
            "US", "United States",
            new BigDecimal("8.25"), // Sales tax rate (average)
            new BigDecimal("21.00"), // Corporate tax rate
            new BigDecimal("24.00"), // Backup withholding rate
            new BigDecimal("12000"), // Standard deduction (USD)
            "USD"
        ));
        
        // EU tax configuration (general)
        TAX_CONFIGURATIONS.put("EU", new TaxConfiguration(
            "EU", "European Union",
            new BigDecimal("20.00"), // VAT rate (standard)
            new BigDecimal("25.00"), // Corporate tax rate (average)
            new BigDecimal("20.00"), // Withholding tax rate
            new BigDecimal("10000"), // Tax-free threshold (EUR)
            "EUR"
        ));
    }

    /**
     * Scheduled job to generate yearly tax documents
     * Runs on February 1st at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 1 2 ?")
    public void generateYearlyTaxDocuments() {
        logger.info("Starting scheduled yearly tax document generation");
        
        try {
            int previousYear = LocalDate.now().getYear() - 1;
            
            // Generate tax documents for each jurisdiction
            for (String jurisdiction : TAX_CONFIGURATIONS.keySet()) {
                generateTaxDocumentsForJurisdiction(jurisdiction, previousYear);
            }
            
            logger.info("Yearly tax document generation completed successfully");
            
        } catch (Exception e) {
            logger.error("Error generating yearly tax documents", e);
            
            // Create support ticket for failed generation
            supportTicketService.createReportGenerationTicket(
                "Tax Documentation",
                String.valueOf(LocalDate.now().getYear() - 1),
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Generate tax documents for a specific jurisdiction and year
     */
    public List<TaxDocument> generateTaxDocumentsForJurisdiction(String jurisdiction, int year) {
        logger.info("Generating tax documents for jurisdiction: {} year: {}", jurisdiction, year);
        
        TaxConfiguration config = TAX_CONFIGURATIONS.get(jurisdiction);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported jurisdiction: " + jurisdiction);
        }
        
        List<TaxDocument> documents = new ArrayList<>();
        
        try {
            // Generate different types of tax documents
            documents.add(generateIncomeStatement(jurisdiction, year, config));
            documents.add(generateVATReport(jurisdiction, year, config));
            documents.add(generateWithholdingTaxReport(jurisdiction, year, config));
            documents.add(generateTransactionSummary(jurisdiction, year, config));
            documents.add(generateProviderEarningsReport(jurisdiction, year, config));
            
            // Send notification
            notificationService.sendReportGenerationNotification(
                "Tax Documents",
                String.valueOf(year),
                true,
                String.format("Tax documents for %s (%d) have been generated. %d documents created.",
                    config.jurisdictionName, year, documents.size())
            );
            
            logger.info("Generated {} tax documents for {} ({})", documents.size(), jurisdiction, year);
            return documents;
            
        } catch (Exception e) {
            logger.error("Error generating tax documents for {} ({})", jurisdiction, year, e);
            throw new RuntimeException("Failed to generate tax documents", e);
        }
    }

    /**
     * Generate income statement for tax purposes
     */
    private TaxDocument generateIncomeStatement(String jurisdiction, int year, TaxConfiguration config) {
        logger.info("Generating income statement for {} ({})", jurisdiction, year);
        
        LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(LocalDate.of(year, 1, 1));
        LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(LocalDate.of(year, 12, 31));
        
        // Calculate income metrics
        IncomeStatementData data = calculateIncomeStatementData(startDate, endDate, config);
        
        // Create tax document
        TaxDocument document = new TaxDocument();
        document.setDocumentType(TaxDocument.DocumentType.INCOME_STATEMENT);
        document.setJurisdiction(jurisdiction);
        document.setTaxYear(year);
        document.setStartDate(startDate.toLocalDate());
        document.setEndDate(endDate.toLocalDate());
        document.setStatus(TaxDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
        
        // Set financial data
        document.setGrossRevenue(data.grossRevenue);
        document.setNetRevenue(data.netRevenue);
        document.setTotalExpenses(data.totalExpenses);
        document.setTaxableIncome(data.taxableIncome);
        document.setEstimatedTaxLiability(data.estimatedTaxLiability);
        
        // Generate document content
        try {
            String content = generateIncomeStatementContent(data, config, year);
            document.setDocumentContent(content.getBytes());
            document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error generating income statement content", e);
            document.setStatus(TaxDocument.DocumentStatus.FAILED);
            document.setNotes("Content generation failed: " + e.getMessage());
        }
        
        return taxDocumentRepository.save(document);
    }

    /**
     * Generate VAT report for tax purposes
     */
    private TaxDocument generateVATReport(String jurisdiction, int year, TaxConfiguration config) {
        logger.info("Generating VAT report for {} ({})", jurisdiction, year);
        
        LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(LocalDate.of(year, 1, 1));
        LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(LocalDate.of(year, 12, 31));
        
        // Calculate VAT metrics
        VATReportData data = calculateVATReportData(startDate, endDate, config);
        
        // Create tax document
        TaxDocument document = new TaxDocument();
        document.setDocumentType(TaxDocument.DocumentType.VAT_REPORT);
        document.setJurisdiction(jurisdiction);
        document.setTaxYear(year);
        document.setStartDate(startDate.toLocalDate());
        document.setEndDate(endDate.toLocalDate());
        document.setStatus(TaxDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
        
        // Set VAT data
        document.setVatCollected(data.vatCollected);
        document.setVatPaid(data.vatPaid);
        document.setNetVatLiability(data.netVatLiability);
        
        // Generate document content
        try {
            String content = generateVATReportContent(data, config, year);
            document.setDocumentContent(content.getBytes());
            document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error generating VAT report content", e);
            document.setStatus(TaxDocument.DocumentStatus.FAILED);
            document.setNotes("Content generation failed: " + e.getMessage());
        }
        
        return taxDocumentRepository.save(document);
    }

    /**
     * Generate withholding tax report
     */
    private TaxDocument generateWithholdingTaxReport(String jurisdiction, int year, TaxConfiguration config) {
        logger.info("Generating withholding tax report for {} ({})", jurisdiction, year);
        
        LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(LocalDate.of(year, 1, 1));
        LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(LocalDate.of(year, 12, 31));
        
        // Calculate withholding tax data
        WithholdingTaxData data = calculateWithholdingTaxData(startDate, endDate, config);
        
        // Create tax document
        TaxDocument document = new TaxDocument();
        document.setDocumentType(TaxDocument.DocumentType.WITHHOLDING_TAX);
        document.setJurisdiction(jurisdiction);
        document.setTaxYear(year);
        document.setStartDate(startDate.toLocalDate());
        document.setEndDate(endDate.toLocalDate());
        document.setStatus(TaxDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
        
        // Set withholding tax data
        document.setWithholdingTaxAmount(data.totalWithholdingTax);
        
        // Generate document content
        try {
            String content = generateWithholdingTaxContent(data, config, year);
            document.setDocumentContent(content.getBytes());
            document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error generating withholding tax content", e);
            document.setStatus(TaxDocument.DocumentStatus.FAILED);
            document.setNotes("Content generation failed: " + e.getMessage());
        }
        
        return taxDocumentRepository.save(document);
    }

    /**
     * Generate transaction summary for tax purposes
     */
    private TaxDocument generateTransactionSummary(String jurisdiction, int year, TaxConfiguration config) {
        logger.info("Generating transaction summary for {} ({})", jurisdiction, year);
        
        LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(LocalDate.of(year, 1, 1));
        LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(LocalDate.of(year, 12, 31));
        
        // Get transaction data
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(startDate, endDate);
        
        // Create tax document
        TaxDocument document = new TaxDocument();
        document.setDocumentType(TaxDocument.DocumentType.TRANSACTION_SUMMARY);
        document.setJurisdiction(jurisdiction);
        document.setTaxYear(year);
        document.setStartDate(startDate.toLocalDate());
        document.setEndDate(endDate.toLocalDate());
        document.setStatus(TaxDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
        
        // Generate document content
        try {
            String content = generateTransactionSummaryContent(transactions, config, year);
            document.setDocumentContent(content.getBytes());
            document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error generating transaction summary content", e);
            document.setStatus(TaxDocument.DocumentStatus.FAILED);
            document.setNotes("Content generation failed: " + e.getMessage());
        }
        
        return taxDocumentRepository.save(document);
    }

    /**
     * Generate provider earnings report for tax purposes
     */
    private TaxDocument generateProviderEarningsReport(String jurisdiction, int year, TaxConfiguration config) {
        logger.info("Generating provider earnings report for {} ({})", jurisdiction, year);
        
        LocalDateTime startDate = FinancialCalculationUtil.getStartOfDay(LocalDate.of(year, 1, 1));
        LocalDateTime endDate = FinancialCalculationUtil.getEndOfDay(LocalDate.of(year, 12, 31));
        
        // Get provider earnings data
        List<ProviderEarningsData> providerEarnings = calculateProviderEarnings(startDate, endDate, config);
        
        // Create tax document
        TaxDocument document = new TaxDocument();
        document.setDocumentType(TaxDocument.DocumentType.PROVIDER_EARNINGS);
        document.setJurisdiction(jurisdiction);
        document.setTaxYear(year);
        document.setStartDate(startDate.toLocalDate());
        document.setEndDate(endDate.toLocalDate());
        document.setStatus(TaxDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
        
        // Generate document content
        try {
            String content = generateProviderEarningsContent(providerEarnings, config, year);
            document.setDocumentContent(content.getBytes());
            document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("Error generating provider earnings content", e);
            document.setStatus(TaxDocument.DocumentStatus.FAILED);
            document.setNotes("Content generation failed: " + e.getMessage());
        }
        
        return taxDocumentRepository.save(document);
    }

    // ==================== CALCULATION METHODS ====================

    private IncomeStatementData calculateIncomeStatementData(LocalDateTime startDate, LocalDateTime endDate, TaxConfiguration config) {
        IncomeStatementData data = new IncomeStatementData();
        
        // Get all successful transactions
        List<Transaction> transactions = transactionRepository.findSuccessfulByDateRange(
            Transaction.TransactionStatus.SUCCESS, startDate, endDate);
        
        // Calculate gross revenue
        List<BigDecimal> amounts = transactions.stream()
            .map(Transaction::getAmount)
            .collect(Collectors.toList());
        data.grossRevenue = FinancialCalculationUtil.sumAmounts(amounts, config.currency);
        
        // Calculate platform fees (our revenue)
        List<BigDecimal> platformFees = transactions.stream()
            .map(Transaction::getPlatformFee)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        data.platformRevenue = FinancialCalculationUtil.sumAmounts(platformFees, config.currency);
        
        // Calculate processing fees (expenses)
        List<BigDecimal> processingFees = transactions.stream()
            .map(Transaction::getProcessingFee)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        data.processingExpenses = FinancialCalculationUtil.sumAmounts(processingFees, config.currency);
        
        // Calculate other expenses (operational costs)
        data.operationalExpenses = FinancialCalculationUtil.normalizeAmount(new BigDecimal("50000"), config.currency); // Estimated
        
        // Calculate totals
        data.totalExpenses = FinancialCalculationUtil.addAmounts(data.processingExpenses, data.operationalExpenses, config.currency);
        data.netRevenue = FinancialCalculationUtil.subtractAmounts(data.platformRevenue, data.totalExpenses, config.currency);
        
        // Calculate taxable income (after deductions)
        BigDecimal deductions = FinancialCalculationUtil.normalizeAmount(config.taxFreeThreshold, config.currency);
        data.taxableIncome = FinancialCalculationUtil.subtractAmounts(data.netRevenue, deductions, config.currency);
        if (data.taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            data.taxableIncome = BigDecimal.ZERO;
        }
        
        // Calculate estimated tax liability
        data.estimatedTaxLiability = FinancialCalculationUtil.calculatePercentage(data.taxableIncome, config.incomeTaxRate, config.currency);
        
        return data;
    }

    private VATReportData calculateVATReportData(LocalDateTime startDate, LocalDateTime endDate, TaxConfiguration config) {
        VATReportData data = new VATReportData();
        
        // Get all successful transactions
        List<Transaction> transactions = transactionRepository.findSuccessfulByDateRange(
            Transaction.TransactionStatus.SUCCESS, startDate, endDate);
        
        // Calculate VAT collected on platform fees
        List<BigDecimal> platformFees = transactions.stream()
            .map(Transaction::getPlatformFee)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        BigDecimal totalPlatformFees = FinancialCalculationUtil.sumAmounts(platformFees, config.currency);
        data.vatCollected = FinancialCalculationUtil.calculatePercentage(totalPlatformFees, config.vatRate, config.currency);
        
        // Calculate VAT paid on expenses (estimated)
        BigDecimal totalExpenses = FinancialCalculationUtil.normalizeAmount(new BigDecimal("30000"), config.currency);
        data.vatPaid = FinancialCalculationUtil.calculatePercentage(totalExpenses, config.vatRate, config.currency);
        
        // Calculate net VAT liability
        data.netVatLiability = FinancialCalculationUtil.subtractAmounts(data.vatCollected, data.vatPaid, config.currency);
        
        return data;
    }

    private WithholdingTaxData calculateWithholdingTaxData(LocalDateTime startDate, LocalDateTime endDate, TaxConfiguration config) {
        WithholdingTaxData data = new WithholdingTaxData();
        
        // Get provider earnings that require withholding tax
        List<Transaction> transactions = transactionRepository.findSuccessfulByDateRange(
            Transaction.TransactionStatus.SUCCESS, startDate, endDate);
        
        // Group by provider and calculate withholding tax
        Map<Transaction.PaymentProvider, List<Transaction>> transactionsByProvider = transactions.stream()
            .filter(t -> t.getPaymentProvider() != null)
            .collect(Collectors.groupingBy(Transaction::getPaymentProvider));
        
        BigDecimal totalWithholding = BigDecimal.ZERO;
        
        for (Map.Entry<Transaction.PaymentProvider, List<Transaction>> entry : transactionsByProvider.entrySet()) {
            List<BigDecimal> providerEarnings = entry.getValue().stream()
                .map(t -> FinancialCalculationUtil.subtractAmounts(t.getAmount(), t.getPlatformFee(), config.currency))
                .collect(Collectors.toList());
            
            BigDecimal totalEarnings = FinancialCalculationUtil.sumAmounts(providerEarnings, config.currency);
            
            // Apply withholding tax if earnings exceed threshold
            if (totalEarnings.compareTo(config.taxFreeThreshold) > 0) {
                BigDecimal withholdingTax = FinancialCalculationUtil.calculatePercentage(totalEarnings, config.withholdingTaxRate, config.currency);
                totalWithholding = FinancialCalculationUtil.addAmounts(totalWithholding, withholdingTax, config.currency);
                
                data.providerWithholdings.put((long) entry.getKey().ordinal(), withholdingTax);
            }
        }
        
        data.totalWithholdingTax = totalWithholding;
        return data;
    }

    private List<ProviderEarningsData> calculateProviderEarnings(LocalDateTime startDate, LocalDateTime endDate, TaxConfiguration config) {
        List<ProviderEarningsData> providerEarnings = new ArrayList<>();
        
        // Get all payment providers from enum
        List<Transaction.PaymentProvider> paymentProviders = Arrays.asList(Transaction.PaymentProvider.values());
        
        for (Transaction.PaymentProvider paymentProvider : paymentProviders) {
            List<Transaction> providerTransactions = transactionRepository.findByDateRangeAndProvider(
                startDate, endDate, paymentProvider
            );
            
            if (!providerTransactions.isEmpty()) {
                ProviderEarningsData earnings = new ProviderEarningsData();
                earnings.providerId = (long) paymentProvider.ordinal();
                earnings.providerName = paymentProvider.getDisplayName();
                earnings.providerEmail = paymentProvider.getCode() + "@system.com";
                
                // Calculate total earnings
                List<BigDecimal> earningsAmounts = providerTransactions.stream()
                    .map(t -> FinancialCalculationUtil.subtractAmounts(t.getAmount(), t.getPlatformFee(), config.currency))
                    .collect(Collectors.toList());
                earnings.totalEarnings = FinancialCalculationUtil.sumAmounts(earningsAmounts, config.currency);
                
                // Calculate transaction count
                earnings.transactionCount = providerTransactions.size();
                
                // Calculate withholding tax if applicable
                if (earnings.totalEarnings.compareTo(config.taxFreeThreshold) > 0) {
                    earnings.withholdingTax = FinancialCalculationUtil.calculatePercentage(
                        earnings.totalEarnings, config.withholdingTaxRate, config.currency
                    );
                } else {
                    earnings.withholdingTax = BigDecimal.ZERO;
                }
                
                // Calculate net earnings
                earnings.netEarnings = FinancialCalculationUtil.subtractAmounts(
                    earnings.totalEarnings, earnings.withholdingTax, config.currency
                );
                
                providerEarnings.add(earnings);
            }
        }
        
        return providerEarnings;
    }

    // ==================== CONTENT GENERATION METHODS ====================

    private String generateIncomeStatementContent(IncomeStatementData data, TaxConfiguration config, int year) {
        StringBuilder content = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        content.append("INCOME STATEMENT\n");
        content.append("================\n\n");
        content.append("Company: HopNGo Platform\n");
        content.append("Jurisdiction: ").append(config.jurisdictionName).append("\n");
        content.append("Tax Year: ").append(year).append("\n");
        content.append("Currency: ").append(config.currency).append("\n");
        content.append("Generated: ").append(FinancialCalculationUtil.formatForReport(FinancialCalculationUtil.getCurrentApplicationTime())).append("\n\n");
        
        content.append("REVENUE\n");
        content.append("-------\n");
        content.append("Gross Revenue: ").append(FinancialCalculationUtil.formatAmount(data.grossRevenue, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Platform Revenue: ").append(FinancialCalculationUtil.formatAmount(data.platformRevenue, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("EXPENSES\n");
        content.append("--------\n");
        content.append("Processing Fees: ").append(FinancialCalculationUtil.formatAmount(data.processingExpenses, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Operational Expenses: ").append(FinancialCalculationUtil.formatAmount(data.operationalExpenses, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Total Expenses: ").append(FinancialCalculationUtil.formatAmount(data.totalExpenses, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("NET INCOME\n");
        content.append("----------\n");
        content.append("Net Revenue: ").append(FinancialCalculationUtil.formatAmount(data.netRevenue, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Taxable Income: ").append(FinancialCalculationUtil.formatAmount(data.taxableIncome, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Estimated Tax Liability: ").append(FinancialCalculationUtil.formatAmount(data.estimatedTaxLiability, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("TAX CALCULATION\n");
        content.append("---------------\n");
        content.append("Income Tax Rate: ").append(config.incomeTaxRate).append("%\n");
        content.append("Tax-Free Threshold: ").append(FinancialCalculationUtil.formatAmount(config.taxFreeThreshold, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("This document is generated automatically and may require review by a qualified tax professional.\n");
        
        return content.toString();
    }

    private String generateVATReportContent(VATReportData data, TaxConfiguration config, int year) {
        StringBuilder content = new StringBuilder();
        
        content.append("VAT REPORT\n");
        content.append("==========\n\n");
        content.append("Company: HopNGo Platform\n");
        content.append("Jurisdiction: ").append(config.jurisdictionName).append("\n");
        content.append("Tax Year: ").append(year).append("\n");
        content.append("Currency: ").append(config.currency).append("\n");
        content.append("Generated: ").append(FinancialCalculationUtil.formatForReport(FinancialCalculationUtil.getCurrentApplicationTime())).append("\n\n");
        
        content.append("VAT SUMMARY\n");
        content.append("-----------\n");
        content.append("VAT Rate: ").append(config.vatRate).append("%\n");
        content.append("VAT Collected: ").append(FinancialCalculationUtil.formatAmount(data.vatCollected, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("VAT Paid: ").append(FinancialCalculationUtil.formatAmount(data.vatPaid, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Net VAT Liability: ").append(FinancialCalculationUtil.formatAmount(data.netVatLiability, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("This document is generated automatically and may require review by a qualified tax professional.\n");
        
        return content.toString();
    }

    private String generateWithholdingTaxContent(WithholdingTaxData data, TaxConfiguration config, int year) {
        StringBuilder content = new StringBuilder();
        
        content.append("WITHHOLDING TAX REPORT\n");
        content.append("=====================\n\n");
        content.append("Company: HopNGo Platform\n");
        content.append("Jurisdiction: ").append(config.jurisdictionName).append("\n");
        content.append("Tax Year: ").append(year).append("\n");
        content.append("Currency: ").append(config.currency).append("\n");
        content.append("Generated: ").append(FinancialCalculationUtil.formatForReport(FinancialCalculationUtil.getCurrentApplicationTime())).append("\n\n");
        
        content.append("WITHHOLDING TAX SUMMARY\n");
        content.append("-----------------------\n");
        content.append("Withholding Tax Rate: ").append(config.withholdingTaxRate).append("%\n");
        content.append("Total Withholding Tax: ").append(FinancialCalculationUtil.formatAmount(data.totalWithholdingTax, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Number of Providers: ").append(data.providerWithholdings.size()).append("\n\n");
        
        if (!data.providerWithholdings.isEmpty()) {
            content.append("PROVIDER BREAKDOWN\n");
            content.append("------------------\n");
            for (Map.Entry<Long, BigDecimal> entry : data.providerWithholdings.entrySet()) {
                content.append("Provider ID ").append(entry.getKey()).append(": ")
                    .append(FinancialCalculationUtil.formatAmount(entry.getValue(), config.currency))
                    .append(" ").append(config.currency).append("\n");
            }
            content.append("\n");
        }
        
        content.append("This document is generated automatically and may require review by a qualified tax professional.\n");
        
        return content.toString();
    }

    private String generateTransactionSummaryContent(List<Transaction> transactions, TaxConfiguration config, int year) {
        StringBuilder content = new StringBuilder();
        
        content.append("TRANSACTION SUMMARY\n");
        content.append("==================\n\n");
        content.append("Company: HopNGo Platform\n");
        content.append("Jurisdiction: ").append(config.jurisdictionName).append("\n");
        content.append("Tax Year: ").append(year).append("\n");
        content.append("Currency: ").append(config.currency).append("\n");
        content.append("Generated: ").append(FinancialCalculationUtil.formatForReport(FinancialCalculationUtil.getCurrentApplicationTime())).append("\n\n");
        
        // Calculate summary statistics
        long totalTransactions = transactions.size();
        List<BigDecimal> amounts = transactions.stream()
            .map(Transaction::getAmount)
            .collect(Collectors.toList());
        BigDecimal totalAmount = FinancialCalculationUtil.sumAmounts(amounts, config.currency);
        BigDecimal averageAmount = totalTransactions > 0 ? 
            FinancialCalculationUtil.averageAmounts(amounts, config.currency) : BigDecimal.ZERO;
        
        content.append("SUMMARY STATISTICS\n");
        content.append("------------------\n");
        content.append("Total Transactions: ").append(totalTransactions).append("\n");
        content.append("Total Amount: ").append(FinancialCalculationUtil.formatAmount(totalAmount, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Average Amount: ").append(FinancialCalculationUtil.formatAmount(averageAmount, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        // Group by payment provider
        Map<String, List<Transaction>> transactionsByProvider = transactions.stream()
            .collect(Collectors.groupingBy(t -> t.getPaymentProvider().toString()));
        
        content.append("BY PAYMENT PROVIDER\n");
        content.append("-------------------\n");
        for (Map.Entry<String, List<Transaction>> entry : transactionsByProvider.entrySet()) {
            List<BigDecimal> providerAmounts = entry.getValue().stream()
                .map(Transaction::getAmount)
                .collect(Collectors.toList());
            BigDecimal providerTotal = FinancialCalculationUtil.sumAmounts(providerAmounts, config.currency);
            
            content.append(entry.getKey()).append(": ")
                .append(entry.getValue().size()).append(" transactions, ")
                .append(FinancialCalculationUtil.formatAmount(providerTotal, config.currency))
                .append(" ").append(config.currency).append("\n");
        }
        
        content.append("\nThis document is generated automatically and may require review by a qualified tax professional.\n");
        
        return content.toString();
    }

    private String generateProviderEarningsContent(List<ProviderEarningsData> providerEarnings, TaxConfiguration config, int year) {
        StringBuilder content = new StringBuilder();
        
        content.append("PROVIDER EARNINGS REPORT\n");
        content.append("=======================\n\n");
        content.append("Company: HopNGo Platform\n");
        content.append("Jurisdiction: ").append(config.jurisdictionName).append("\n");
        content.append("Tax Year: ").append(year).append("\n");
        content.append("Currency: ").append(config.currency).append("\n");
        content.append("Generated: ").append(FinancialCalculationUtil.formatForReport(FinancialCalculationUtil.getCurrentApplicationTime())).append("\n\n");
        
        // Calculate totals
        List<BigDecimal> totalEarningsList = providerEarnings.stream()
            .map(p -> p.totalEarnings)
            .collect(Collectors.toList());
        List<BigDecimal> withholdingTaxList = providerEarnings.stream()
            .map(p -> p.withholdingTax)
            .collect(Collectors.toList());
        
        BigDecimal grandTotalEarnings = FinancialCalculationUtil.sumAmounts(totalEarningsList, config.currency);
        BigDecimal grandTotalWithholding = FinancialCalculationUtil.sumAmounts(withholdingTaxList, config.currency);
        
        content.append("SUMMARY\n");
        content.append("-------\n");
        content.append("Total Providers: ").append(providerEarnings.size()).append("\n");
        content.append("Total Earnings: ").append(FinancialCalculationUtil.formatAmount(grandTotalEarnings, config.currency)).append(" ").append(config.currency).append("\n");
        content.append("Total Withholding Tax: ").append(FinancialCalculationUtil.formatAmount(grandTotalWithholding, config.currency)).append(" ").append(config.currency).append("\n\n");
        
        content.append("PROVIDER DETAILS\n");
        content.append("----------------\n");
        for (ProviderEarningsData provider : providerEarnings) {
            content.append("Provider: ").append(provider.providerName).append(" (ID: ").append(provider.providerId).append(")\n");
            content.append("  Email: ").append(provider.providerEmail).append("\n");
            content.append("  Transactions: ").append(provider.transactionCount).append("\n");
            content.append("  Total Earnings: ").append(FinancialCalculationUtil.formatAmount(provider.totalEarnings, config.currency)).append(" ").append(config.currency).append("\n");
            content.append("  Withholding Tax: ").append(FinancialCalculationUtil.formatAmount(provider.withholdingTax, config.currency)).append(" ").append(config.currency).append("\n");
            content.append("  Net Earnings: ").append(FinancialCalculationUtil.formatAmount(provider.netEarnings, config.currency)).append(" ").append(config.currency).append("\n\n");
        }
        
        content.append("This document is generated automatically and may require review by a qualified tax professional.\n");
        
        return content.toString();
    }

    // ==================== PUBLIC API METHODS ====================

    public List<TaxDocument> getTaxDocumentsByJurisdiction(String jurisdiction) {
        return taxDocumentRepository.findByJurisdiction(jurisdiction);
    }

    public List<TaxDocument> getTaxDocumentsByYear(int year) {
        return taxDocumentRepository.findByTaxYear(year);
    }

    public Optional<TaxDocument> getTaxDocumentById(Long documentId) {
        return taxDocumentRepository.findById(documentId);
    }

    public List<TaxDocument> getPendingTaxDocuments() {
        return taxDocumentRepository.findByStatus(TaxDocument.DocumentStatus.DRAFT);
    }

    public byte[] exportTaxDocumentContent(Long documentId) throws IOException {
        Optional<TaxDocument> documentOpt = taxDocumentRepository.findById(documentId);
        if (!documentOpt.isPresent()) {
            throw new IllegalArgumentException("Tax document not found: " + documentId);
        }
        
        TaxDocument document = documentOpt.get();
        return document.getDocumentContent();
    }

    public TaxDocument regenerateTaxDocument(Long documentId) {
        Optional<TaxDocument> documentOpt = taxDocumentRepository.findById(documentId);
        if (!documentOpt.isPresent()) {
            throw new IllegalArgumentException("Tax document not found: " + documentId);
        }
        
        TaxDocument document = documentOpt.get();
        
        // Delete old document and regenerate
        taxDocumentRepository.delete(document);
        
        List<TaxDocument> newDocuments = generateTaxDocumentsForJurisdiction(
            document.getJurisdiction(), 
            document.getTaxYear()
        );
        
        // Return the document of the same type
        return newDocuments.stream()
            .filter(d -> d.getDocumentType() == document.getDocumentType())
            .findFirst()
            .orElse(newDocuments.get(0));
    }

    // ==================== DATA CLASSES ====================

    private static class TaxConfiguration {
        final String jurisdictionCode;
        final String jurisdictionName;
        final BigDecimal vatRate;
        final BigDecimal incomeTaxRate;
        final BigDecimal withholdingTaxRate;
        final BigDecimal taxFreeThreshold;
        final String currency;
        
        TaxConfiguration(String jurisdictionCode, String jurisdictionName, 
                        BigDecimal vatRate, BigDecimal incomeTaxRate, 
                        BigDecimal withholdingTaxRate, BigDecimal taxFreeThreshold, 
                        String currency) {
            this.jurisdictionCode = jurisdictionCode;
            this.jurisdictionName = jurisdictionName;
            this.vatRate = vatRate;
            this.incomeTaxRate = incomeTaxRate;
            this.withholdingTaxRate = withholdingTaxRate;
            this.taxFreeThreshold = taxFreeThreshold;
            this.currency = currency;
        }
    }

    private static class IncomeStatementData {
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal platformRevenue = BigDecimal.ZERO;
        BigDecimal processingExpenses = BigDecimal.ZERO;
        BigDecimal operationalExpenses = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal taxableIncome = BigDecimal.ZERO;
        BigDecimal estimatedTaxLiability = BigDecimal.ZERO;
    }

    private static class VATReportData {
        BigDecimal vatCollected = BigDecimal.ZERO;
        BigDecimal vatPaid = BigDecimal.ZERO;
        BigDecimal netVatLiability = BigDecimal.ZERO;
    }

    private static class WithholdingTaxData {
        BigDecimal totalWithholdingTax = BigDecimal.ZERO;
        Map<Long, BigDecimal> providerWithholdings = new HashMap<>();
    }

    private static class ProviderEarningsData {
        Long providerId;
        String providerName;
        String providerEmail;
        BigDecimal totalEarnings = BigDecimal.ZERO;
        BigDecimal withholdingTax = BigDecimal.ZERO;
        BigDecimal netEarnings = BigDecimal.ZERO;
        Integer transactionCount = 0;
    }
}