package com.hopngo.service;

import com.hopngo.entity.*;
import com.hopngo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaxDocumentationService
 */
@ExtendWith(MockitoExtension.class)
class TaxDocumentationServiceTest {

    @Mock
    private TaxDocumentRepository taxDocumentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SupportTicketService supportTicketService;

    @InjectMocks
    private TaxDocumentationService taxDocumentationService;

    private List<Transaction> mockTransactions;
    private List<Provider> mockProviders;
    private TaxDocument mockTaxDocument;

    @BeforeEach
    void setUp() {
        // Setup mock transactions
        mockTransactions = createMockTransactions();
        
        // Setup mock providers
        mockProviders = createMockProviders();
        
        // Setup mock tax document
        mockTaxDocument = createMockTaxDocument();
    }

    @Test
    void testGenerateTaxDocumentsForJurisdiction_Success() {
        // Arrange
        String jurisdiction = "BD";
        int year = 2023;
        
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenReturn(mockTaxDocument);
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction(jurisdiction, year);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size()); // Should generate 5 types of documents
        verify(taxDocumentRepository, times(5)).save(any(TaxDocument.class));
        verify(notificationService).sendFinancialReportAlert(anyString(), anyString(), anyString());
    }

    @Test
    void testGenerateTaxDocumentsForJurisdiction_UnsupportedJurisdiction() {
        // Arrange
        String jurisdiction = "INVALID";
        int year = 2023;
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taxDocumentationService.generateTaxDocumentsForJurisdiction(jurisdiction, year);
        });
        
        assertEquals("Unsupported jurisdiction: INVALID", exception.getMessage());
    }

    @Test
    void testGenerateTaxDocumentsForJurisdiction_WithException() {
        // Arrange
        String jurisdiction = "BD";
        int year = 2023;
        
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taxDocumentationService.generateTaxDocumentsForJurisdiction(jurisdiction, year);
        });
        
        assertEquals("Failed to generate tax documents", exception.getMessage());
    }

    @Test
    void testGenerateYearlyTaxDocuments_Success() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenReturn(mockTaxDocument);
        
        // Act
        assertDoesNotThrow(() -> {
            taxDocumentationService.generateYearlyTaxDocuments();
        });
        
        // Assert
        verify(taxDocumentRepository, atLeastOnce()).save(any(TaxDocument.class));
    }

    @Test
    void testGenerateYearlyTaxDocuments_WithException() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        assertDoesNotThrow(() -> {
            taxDocumentationService.generateYearlyTaxDocuments();
        });
        
        // Assert
        verify(supportTicketService).createFinancialReportTicket(
            eq("TAX_DOCUMENT_GENERATION_FAILED"),
            eq("Yearly tax document generation failed"),
            eq("HIGH"),
            anyString()
        );
    }

    @Test
    void testGetTaxDocumentsByJurisdiction() {
        // Arrange
        String jurisdiction = "BD";
        List<TaxDocument> expectedDocuments = Arrays.asList(mockTaxDocument);
        when(taxDocumentRepository.findByJurisdiction(jurisdiction)).thenReturn(expectedDocuments);
        
        // Act
        List<TaxDocument> result = taxDocumentationService.getTaxDocumentsByJurisdiction(jurisdiction);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDocuments, result);
        verify(taxDocumentRepository).findByJurisdiction(jurisdiction);
    }

    @Test
    void testGetTaxDocumentsByYear() {
        // Arrange
        int year = 2023;
        List<TaxDocument> expectedDocuments = Arrays.asList(mockTaxDocument);
        when(taxDocumentRepository.findByTaxYear(year)).thenReturn(expectedDocuments);
        
        // Act
        List<TaxDocument> result = taxDocumentationService.getTaxDocumentsByYear(year);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDocuments, result);
        verify(taxDocumentRepository).findByTaxYear(year);
    }

    @Test
    void testGetTaxDocumentById_Found() {
        // Arrange
        Long documentId = 1L;
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.of(mockTaxDocument));
        
        // Act
        Optional<TaxDocument> result = taxDocumentationService.getTaxDocumentById(documentId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(mockTaxDocument, result.get());
        verify(taxDocumentRepository).findById(documentId);
    }

    @Test
    void testGetTaxDocumentById_NotFound() {
        // Arrange
        Long documentId = 999L;
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.empty());
        
        // Act
        Optional<TaxDocument> result = taxDocumentationService.getTaxDocumentById(documentId);
        
        // Assert
        assertFalse(result.isPresent());
        verify(taxDocumentRepository).findById(documentId);
    }

    @Test
    void testGetPendingTaxDocuments() {
        // Arrange
        List<TaxDocument> expectedDocuments = Arrays.asList(mockTaxDocument);
        when(taxDocumentRepository.findByStatus(TaxDocument.DocumentStatus.DRAFT)).thenReturn(expectedDocuments);
        
        // Act
        List<TaxDocument> result = taxDocumentationService.getPendingTaxDocuments();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDocuments, result);
        verify(taxDocumentRepository).findByStatus(TaxDocument.DocumentStatus.DRAFT);
    }

    @Test
    void testExportTaxDocumentContent_Success() throws Exception {
        // Arrange
        Long documentId = 1L;
        byte[] expectedContent = "Tax document content".getBytes();
        mockTaxDocument.setDocumentContent(expectedContent);
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.of(mockTaxDocument));
        
        // Act
        byte[] result = taxDocumentationService.exportTaxDocumentContent(documentId);
        
        // Assert
        assertNotNull(result);
        assertArrayEquals(expectedContent, result);
        verify(taxDocumentRepository).findById(documentId);
    }

    @Test
    void testExportTaxDocumentContent_DocumentNotFound() {
        // Arrange
        Long documentId = 999L;
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taxDocumentationService.exportTaxDocumentContent(documentId);
        });
        
        assertEquals("Tax document not found: 999", exception.getMessage());
    }

    @Test
    void testRegenerateTaxDocument_Success() {
        // Arrange
        Long documentId = 1L;
        mockTaxDocument.setJurisdiction("BD");
        mockTaxDocument.setTaxYear(2023);
        mockTaxDocument.setDocumentType(TaxDocument.DocumentType.INCOME_STATEMENT);
        
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.of(mockTaxDocument));
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenReturn(mockTaxDocument);
        
        // Act
        TaxDocument result = taxDocumentationService.regenerateTaxDocument(documentId);
        
        // Assert
        assertNotNull(result);
        verify(taxDocumentRepository).delete(mockTaxDocument);
        verify(taxDocumentRepository, atLeastOnce()).save(any(TaxDocument.class));
    }

    @Test
    void testRegenerateTaxDocument_DocumentNotFound() {
        // Arrange
        Long documentId = 999L;
        when(taxDocumentRepository.findById(documentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taxDocumentationService.regenerateTaxDocument(documentId);
        });
        
        assertEquals("Tax document not found: 999", exception.getMessage());
    }

    @Test
    void testIncomeStatementGeneration() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(doc -> doc.getDocumentType() == TaxDocument.DocumentType.INCOME_STATEMENT));
        
        // Verify income statement document has financial data
        TaxDocument incomeStatement = result.stream()
            .filter(doc -> doc.getDocumentType() == TaxDocument.DocumentType.INCOME_STATEMENT)
            .findFirst()
            .orElse(null);
        
        assertNotNull(incomeStatement);
        assertNotNull(incomeStatement.getGrossRevenue());
        assertNotNull(incomeStatement.getNetRevenue());
        assertNotNull(incomeStatement.getTaxableIncome());
    }

    @Test
    void testVATReportGeneration() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(doc -> doc.getDocumentType() == TaxDocument.DocumentType.VAT_REPORT));
        
        // Verify VAT report document has VAT data
        TaxDocument vatReport = result.stream()
            .filter(doc -> doc.getDocumentType() == TaxDocument.DocumentType.VAT_REPORT)
            .findFirst()
            .orElse(null);
        
        assertNotNull(vatReport);
        assertNotNull(vatReport.getVatCollected());
        assertNotNull(vatReport.getVatPaid());
        assertNotNull(vatReport.getNetVatLiability());
    }

    @Test
    void testWithholdingTaxReportGeneration() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(doc -> doc.getDocumentType() == TaxDocument.DocumentType.WITHHOLDING_TAX));
        
        // Verify withholding tax report has withholding data
        TaxDocument withholdingReport = result.stream()
            .filter(doc -> doc.getDocumentType() == TaxDocument.DocumentType.WITHHOLDING_TAX)
            .findFirst()
            .orElse(null);
        
        assertNotNull(withholdingReport);
        assertNotNull(withholdingReport.getWithholdingTaxAmount());
    }

    @Test
    void testProviderEarningsReportGeneration() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(doc -> doc.getDocumentType() == TaxDocument.DocumentType.PROVIDER_EARNINGS));
    }

    @Test
    void testTransactionSummaryGeneration() {
        // Arrange
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(doc -> doc.getDocumentType() == TaxDocument.DocumentType.TRANSACTION_SUMMARY));
    }

    @Test
    void testDifferentJurisdictions() {
        // Test BD jurisdiction
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenReturn(mockTaxDocument);
        
        // Test BD
        List<TaxDocument> bdResult = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        assertNotNull(bdResult);
        assertEquals(5, bdResult.size());
        
        // Test US
        List<TaxDocument> usResult = taxDocumentationService.generateTaxDocumentsForJurisdiction("US", 2023);
        assertNotNull(usResult);
        assertEquals(5, usResult.size());
        
        // Test EU
        List<TaxDocument> euResult = taxDocumentationService.generateTaxDocumentsForJurisdiction("EU", 2023);
        assertNotNull(euResult);
        assertEquals(5, euResult.size());
    }

    @Test
    void testDocumentContentGeneration() {
        // Arrange
        when(transactionRepository.findSuccessfulByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions);
        when(transactionRepository.findByProviderAndDateRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(mockTransactions.subList(0, 1));
        when(providerRepository.findAll()).thenReturn(mockProviders);
        when(taxDocumentRepository.save(any(TaxDocument.class))).thenAnswer(invocation -> {
            TaxDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        
        // Act
        List<TaxDocument> result = taxDocumentationService.generateTaxDocumentsForJurisdiction("BD", 2023);
        
        // Assert
        assertNotNull(result);
        for (TaxDocument document : result) {
            assertNotNull(document.getDocumentContent());
            assertTrue(document.getDocumentContent().length > 0);
            assertEquals(TaxDocument.DocumentStatus.COMPLETED, document.getStatus());
        }
    }

    // ==================== HELPER METHODS ====================

    private List<Transaction> createMockTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Create mock provider
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setName("Test Provider");
        provider.setEmail("provider@test.com");
        
        // Transaction 1
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("1000.00"));
        transaction1.setPlatformFee(new BigDecimal("50.00"));
        transaction1.setProcessingFee(new BigDecimal("30.00"));
        transaction1.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction1.setPaymentProvider(Transaction.PaymentProvider.STRIPE);
        transaction1.setProvider(provider);
        transaction1.setCreatedAt(LocalDateTime.now().minusDays(30));
        transactions.add(transaction1);
        
        // Transaction 2
        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("2000.00"));
        transaction2.setPlatformFee(new BigDecimal("100.00"));
        transaction2.setProcessingFee(new BigDecimal("60.00"));
        transaction2.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction2.setPaymentProvider(Transaction.PaymentProvider.BKASH);
        transaction2.setProvider(provider);
        transaction2.setCreatedAt(LocalDateTime.now().minusDays(15));
        transactions.add(transaction2);
        
        return transactions;
    }

    private List<Provider> createMockProviders() {
        List<Provider> providers = new ArrayList<>();
        
        Provider provider1 = new Provider();
        provider1.setId(1L);
        provider1.setName("Test Provider 1");
        provider1.setEmail("provider1@test.com");
        providers.add(provider1);
        
        Provider provider2 = new Provider();
        provider2.setId(2L);
        provider2.setName("Test Provider 2");
        provider2.setEmail("provider2@test.com");
        providers.add(provider2);
        
        return providers;
    }

    private TaxDocument createMockTaxDocument() {
        TaxDocument document = new TaxDocument();
        document.setId(1L);
        document.setDocumentType(TaxDocument.DocumentType.INCOME_STATEMENT);
        document.setJurisdiction("BD");
        document.setTaxYear(2023);
        document.setStartDate(LocalDate.of(2023, 1, 1));
        document.setEndDate(LocalDate.of(2023, 12, 31));
        document.setStatus(TaxDocument.DocumentStatus.COMPLETED);
        document.setGrossRevenue(new BigDecimal("100000.00"));
        document.setNetRevenue(new BigDecimal("80000.00"));
        document.setTaxableIncome(new BigDecimal("70000.00"));
        document.setEstimatedTaxLiability(new BigDecimal("7000.00"));
        document.setVatCollected(new BigDecimal("12000.00"));
        document.setVatPaid(new BigDecimal("4500.00"));
        document.setNetVatLiability(new BigDecimal("7500.00"));
        document.setWithholdingTaxAmount(new BigDecimal("5000.00"));
        document.setDocumentContent("Sample tax document content".getBytes());
        document.setCreatedAt(LocalDateTime.now());
        return document;
    }
}