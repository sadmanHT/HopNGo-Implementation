package com.hopngo.service;

import com.hopngo.entity.*;
import com.hopngo.entity.ReconciliationDiscrepancy.DiscrepancySeverity;
import com.hopngo.entity.ReconciliationDiscrepancy.DiscrepancyType;
import com.hopngo.entity.ReconciliationJob.ReconciliationStatus;
import com.hopngo.entity.Transaction.PaymentProvider;
import com.hopngo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.hopngo.util.FinancialCalculationUtil;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    @Autowired
    private ReconciliationJobRepository reconciliationJobRepository;

    @Autowired
    private ReconciliationDiscrepancyRepository discrepancyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private PaymentProviderService paymentProviderService;

    /**
     * Daily reconciliation job - runs at 3 AM every day
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void performDailyReconciliation() {
        logger.info("Starting daily reconciliation at {}", LocalDateTime.now());
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // Reconcile each payment provider
        reconcileProvider(PaymentProvider.STRIPE, yesterday);
        reconcileProvider(PaymentProvider.BKASH, yesterday);
        reconcileProvider(PaymentProvider.NAGAD, yesterday);
        
        logger.info("Daily reconciliation completed");
    }

    /**
     * Reconcile transactions for a specific provider and date
     */
    @Transactional
    public ReconciliationJob reconcileProvider(PaymentProvider provider, LocalDate reconciliationDate) {
        logger.info("Starting reconciliation for {} on {}", provider, reconciliationDate);
        
        // Create reconciliation job
        ReconciliationJob job = new ReconciliationJob();
        job.setProvider(provider);
        job.setReconciliationDate(reconciliationDate.atStartOfDay());
        job.setStartedAt(FinancialCalculationUtil.getCurrentUtcTime());
        job.setStatus(ReconciliationStatus.PROCESSING);
        job = reconciliationJobRepository.save(job);
        
        try {
            // Get our transactions for the date
            LocalDateTime startDateTime = FinancialCalculationUtil.getStartOfDay(reconciliationDate);
            LocalDateTime endDateTime = FinancialCalculationUtil.getStartOfDay(reconciliationDate.plusDays(1));
            List<Transaction> ourTransactions = transactionRepository.findByDateRangeAndProvider(
                startDateTime,
                endDateTime,
                provider
            );
            
            // Get provider transactions for the date
            List<ProviderTransaction> providerTransactions = paymentProviderService.getTransactionsForDate(provider, reconciliationDate);
            
            job.setTotalOurTransactions(ourTransactions.size());
            job.setTotalProviderTransactions(providerTransactions.size());
            
            // Calculate totals with proper precision
            String currency = ourTransactions.isEmpty() ? "USD" : ourTransactions.get(0).getCurrency();
            List<BigDecimal> ourAmounts = ourTransactions.stream()
                .map(Transaction::getAmount)
                .collect(Collectors.toList());
            List<BigDecimal> providerAmounts = providerTransactions.stream()
                .map(ProviderTransaction::getAmount)
                .collect(Collectors.toList());
            
            BigDecimal ourTotal = FinancialCalculationUtil.sumAmounts(ourAmounts, currency);
            BigDecimal providerTotal = FinancialCalculationUtil.sumAmounts(providerAmounts, currency);
            
            job.setOurTotalAmount(ourTotal);
            job.setProviderTotalAmount(providerTotal);
            
            // Perform reconciliation
            List<ReconciliationDiscrepancy> discrepancies = performReconciliation(job, ourTransactions, providerTransactions);
            
            job.setDiscrepanciesFound(discrepancies.size());
            job.setEndTime(FinancialCalculationUtil.getCurrentUtcTime());
            
            if (discrepancies.isEmpty()) {
                job.setStatus(ReconciliationStatus.COMPLETED);
                logger.info("Reconciliation completed successfully for {} - no discrepancies found", provider);
            } else {
                job.setStatus(ReconciliationStatus.COMPLETED_WITH_DISCREPANCIES);
                logger.warn("Reconciliation completed for {} with {} discrepancies", provider, discrepancies.size());
                
                // Send notification and create support tickets for high severity discrepancies
                handleDiscrepancies(job, discrepancies);
            }
            
        } catch (Exception e) {
            logger.error("Reconciliation failed for {}", provider, e);
            job.setStatus(ReconciliationStatus.FAILED);
            job.setEndTime(FinancialCalculationUtil.getCurrentUtcTime());
            job.setNotes("Error: " + e.getMessage());
            
            notificationService.sendReconciliationAlert(
                provider.toString(),
                0,
                "Reconciliation job failed: " + e.getMessage()
            );
        }
        
        return reconciliationJobRepository.save(job);
    }

    /**
     * Perform the actual reconciliation between our transactions and provider transactions
     */
    private List<ReconciliationDiscrepancy> performReconciliation(
            ReconciliationJob job, 
            List<Transaction> ourTransactions, 
            List<ProviderTransaction> providerTransactions) {
        
        List<ReconciliationDiscrepancy> discrepancies = new java.util.ArrayList<>();
        
        // Create maps for easier lookup
        Map<String, Transaction> ourTransactionMap = ourTransactions.stream()
            .collect(Collectors.toMap(Transaction::getTransactionId, t -> t));
        
        Map<String, ProviderTransaction> providerTransactionMap = providerTransactions.stream()
            .collect(Collectors.toMap(ProviderTransaction::getTransactionId, t -> t));
        
        // Check for transactions in provider but not in our system
        for (ProviderTransaction providerTxn : providerTransactions) {
            Transaction ourTxn = ourTransactionMap.get(providerTxn.getTransactionId());
            
            if (ourTxn == null) {
                // Missing transaction in our system
                ReconciliationDiscrepancy discrepancy = createDiscrepancy(
                    job, null, DiscrepancyType.EXTRA_TRANSACTION,
                    "Transaction found in provider but missing in our system",
                    providerTxn.getAmount(), BigDecimal.ZERO
                );
                discrepancy.setProviderTransactionId(providerTxn.getTransactionId());
                discrepancies.add(discrepancy);
            } else {
                // Check for amount discrepancies with tolerance
                String currency = ourTxn.getCurrency();
                BigDecimal tolerance = FinancialCalculationUtil.normalizeAmount(new BigDecimal("0.01"), currency);
                if (!FinancialCalculationUtil.isWithinTolerance(ourTxn.getAmount(), providerTxn.getAmount(), tolerance, currency)) {
                    ReconciliationDiscrepancy discrepancy = createDiscrepancy(
                        job, ourTxn, DiscrepancyType.AMOUNT_MISMATCH,
                        "Amount mismatch between our system and provider",
                        providerTxn.getAmount(), ourTxn.getAmount()
                    );
                    discrepancy.setProviderTransactionId(providerTxn.getTransactionId());
                    discrepancies.add(discrepancy);
                }
                
                // Check for status discrepancies
                if (!ourTxn.getStatus().toString().equals(providerTxn.getStatus())) {
                    ReconciliationDiscrepancy discrepancy = createDiscrepancy(
                        job, ourTxn, DiscrepancyType.STATUS_MISMATCH,
                        String.format("Status mismatch: Our=%s, Provider=%s", 
                            ourTxn.getStatus(), providerTxn.getStatus()),
                        BigDecimal.ZERO, BigDecimal.ZERO
                    );
                    discrepancy.setProviderTransactionId(providerTxn.getTransactionId());
                    discrepancies.add(discrepancy);
                }
            }
        }
        
        // Check for transactions in our system but not in provider
        for (Transaction ourTxn : ourTransactions) {
            if (!providerTransactionMap.containsKey(ourTxn.getTransactionId())) {
                ReconciliationDiscrepancy discrepancy = createDiscrepancy(
                    job, ourTxn, DiscrepancyType.MISSING_TRANSACTION,
                    "Transaction found in our system but missing in provider",
                    BigDecimal.ZERO, ourTxn.getAmount()
                );
                discrepancies.add(discrepancy);
            }
        }
        
        // Save all discrepancies
        return discrepancyRepository.saveAll(discrepancies);
    }

    /**
     * Create a reconciliation discrepancy
     */
    private ReconciliationDiscrepancy createDiscrepancy(
            ReconciliationJob job, 
            Transaction transaction, 
            DiscrepancyType type,
            String description, 
            BigDecimal providerAmount, 
            BigDecimal ourAmount) {
        
        ReconciliationDiscrepancy discrepancy = new ReconciliationDiscrepancy();
        discrepancy.setReconciliationJob(job);
        discrepancy.setTransaction(transaction);
        discrepancy.setDiscrepancyType(type);
        discrepancy.setDescription(description);
        discrepancy.setProviderAmount(providerAmount);
        discrepancy.setOurAmount(ourAmount);
        discrepancy.setAmountDifference(providerAmount.subtract(ourAmount));
        // createdAt is automatically set by @CreationTimestamp
        
        // Determine severity
        BigDecimal absoluteDifference = discrepancy.getAmountDifference().abs();
        if (type == DiscrepancyType.EXTRA_TRANSACTION || type == DiscrepancyType.MISSING_TRANSACTION) {
            discrepancy.setSeverity(DiscrepancySeverity.HIGH);
        } else if (absoluteDifference.compareTo(new BigDecimal("100.00")) > 0) {
            discrepancy.setSeverity(DiscrepancySeverity.HIGH);
        } else if (absoluteDifference.compareTo(new BigDecimal("10.00")) > 0) {
            discrepancy.setSeverity(DiscrepancySeverity.MEDIUM);
        } else {
            discrepancy.setSeverity(DiscrepancySeverity.LOW);
        }
        
        return discrepancy;
    }

    /**
     * Handle discrepancies by sending notifications and creating support tickets
     */
    private void handleDiscrepancies(ReconciliationJob job, List<ReconciliationDiscrepancy> discrepancies) {
        // Send notification
        String details = discrepancies.stream()
            .map(d -> String.format("%s: %s (Amount: %s)", 
                d.getDiscrepancyType(), d.getDescription(), d.getAmountDifference()))
            .collect(Collectors.joining("\n"));
        
        notificationService.sendReconciliationAlert(
            job.getProvider().toString(),
            discrepancies.size(),
            details
        );
        
        // Create support tickets for high severity discrepancies
        List<ReconciliationDiscrepancy> highSeverityDiscrepancies = discrepancies.stream()
            .filter(d -> d.getSeverity() == DiscrepancySeverity.HIGH)
            .collect(Collectors.toList());
        
        for (ReconciliationDiscrepancy discrepancy : highSeverityDiscrepancies) {
            String ticketDescription = String.format(
                "High severity reconciliation discrepancy detected:\n\n" +
                "Provider: %s\n" +
                "Type: %s\n" +
                "Description: %s\n" +
                "Amount Difference: %s\n" +
                "Transaction ID: %s\n" +
                "Detected At: %s\n\n" +
                "Please investigate and resolve this discrepancy immediately.",
                job.getProvider(),
                discrepancy.getDiscrepancyType(),
                discrepancy.getDescription(),
                discrepancy.getAmountDifference(),
                discrepancy.getTransaction() != null ? discrepancy.getTransaction().getTransactionId() : "N/A",
                discrepancy.getCreatedAt()
            );
            
            String ticketId = supportTicketService.createAutoTicket(
                "Reconciliation Discrepancy",
                ticketDescription,
                "HIGH"
            );
            
            discrepancy.setSupportTicketId(ticketId);
            discrepancyRepository.save(discrepancy);
        }
    }

    /**
     * Manual reconciliation for a specific provider and date range
     */
    @Transactional
    public ReconciliationJob performManualReconciliation(PaymentProvider provider, LocalDate startDate, LocalDate endDate) {
        logger.info("Starting manual reconciliation for {} from {} to {}", provider, startDate, endDate);
        
        ReconciliationJob job = new ReconciliationJob();
        job.setProvider(provider);
        job.setReconciliationDate(startDate.atStartOfDay());
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(ReconciliationStatus.PROCESSING);
        job.setNotes(String.format("Manual reconciliation from %s to %s", startDate, endDate));
        job = reconciliationJobRepository.save(job);
        
        try {
            // Get transactions for the date range
            List<Transaction> ourTransactions = transactionRepository.findByDateRangeAndProvider(
                FinancialCalculationUtil.getStartOfDay(startDate),
                FinancialCalculationUtil.getStartOfDay(endDate.plusDays(1)),
                provider
            );
            
            List<ProviderTransaction> providerTransactions = paymentProviderService.getTransactionsForDateRange(
                provider, startDate, endDate
            );
            
            job.setTotalOurTransactions(ourTransactions.size());
            job.setTotalProviderTransactions(providerTransactions.size());
            
            BigDecimal ourTotal = ourTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal providerTotal = providerTransactions.stream()
                .map(ProviderTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            job.setOurTotalAmount(ourTotal);
            job.setProviderTotalAmount(providerTotal);
            
            List<ReconciliationDiscrepancy> discrepancies = performReconciliation(job, ourTransactions, providerTransactions);
            
            job.setDiscrepanciesFound(discrepancies.size());
            job.setEndTime(LocalDateTime.now());
            
            if (discrepancies.isEmpty()) {
                job.setStatus(ReconciliationStatus.COMPLETED);
            } else {
                job.setStatus(ReconciliationStatus.COMPLETED_WITH_DISCREPANCIES);
                handleDiscrepancies(job, discrepancies);
            }
            
        } catch (Exception e) {
            logger.error("Manual reconciliation failed for {}", provider, e);
            job.setStatus(ReconciliationStatus.FAILED);
            job.setEndTime(LocalDateTime.now());
            job.setNotes(job.getNotes() + "\nError: " + e.getMessage());
        }
        
        return reconciliationJobRepository.save(job);
    }

    /**
     * Get reconciliation summary for a date range
     */
    public ReconciliationSummary getReconciliationSummary(LocalDate startDate, LocalDate endDate) {
        List<ReconciliationJob> jobs = reconciliationJobRepository.findByReconciliationDateBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        
        int totalJobs = jobs.size();
        int successfulJobs = (int) jobs.stream().filter(j -> j.getStatus() == ReconciliationStatus.COMPLETED).count();
        int jobsWithDiscrepancies = (int) jobs.stream().filter(j -> j.getStatus() == ReconciliationStatus.COMPLETED_WITH_DISCREPANCIES).count();
        int failedJobs = (int) jobs.stream().filter(j -> j.getStatus() == ReconciliationStatus.FAILED).count();
        
        int totalDiscrepancies = jobs.stream().mapToInt(ReconciliationJob::getDiscrepanciesFound).sum();
        
        return new ReconciliationSummary(
            totalJobs, successfulJobs, jobsWithDiscrepancies, failedJobs, totalDiscrepancies
        );
    }

    /**
     * Resolve a reconciliation discrepancy
     */
    @Transactional
    public void resolveDiscrepancy(Long discrepancyId, String resolutionNotes) {
        ReconciliationDiscrepancy discrepancy = discrepancyRepository.findById(discrepancyId)
            .orElseThrow(() -> new RuntimeException("Discrepancy not found: " + discrepancyId));
        
        discrepancy.setResolved(true);
        discrepancy.setResolvedAt(LocalDateTime.now());
        discrepancy.setResolutionNotes(resolutionNotes);
        
        discrepancyRepository.save(discrepancy);
        
        logger.info("Reconciliation discrepancy {} resolved: {}", discrepancyId, resolutionNotes);
    }

    /**
     * Summary class for reconciliation results
     */
    public static class ReconciliationSummary {
        private final int totalJobs;
        private final int successfulJobs;
        private final int jobsWithDiscrepancies;
        private final int failedJobs;
        private final int totalDiscrepancies;

        public ReconciliationSummary(int totalJobs, int successfulJobs, int jobsWithDiscrepancies, 
                                   int failedJobs, int totalDiscrepancies) {
            this.totalJobs = totalJobs;
            this.successfulJobs = successfulJobs;
            this.jobsWithDiscrepancies = jobsWithDiscrepancies;
            this.failedJobs = failedJobs;
            this.totalDiscrepancies = totalDiscrepancies;
        }

        // Getters
        public int getTotalJobs() { return totalJobs; }
        public int getSuccessfulJobs() { return successfulJobs; }
        public int getJobsWithDiscrepancies() { return jobsWithDiscrepancies; }
        public int getFailedJobs() { return failedJobs; }
        public int getTotalDiscrepancies() { return totalDiscrepancies; }
    }

    /**
     * Provider transaction class for external data
     */
    public static class ProviderTransaction {
        private String transactionId;
        private BigDecimal amount;
        private String status;
        private LocalDateTime timestamp;

        public ProviderTransaction(String transactionId, BigDecimal amount, String status, LocalDateTime timestamp) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public BigDecimal getAmount() { return amount; }
        public String getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}