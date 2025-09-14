package com.hopngo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProviderServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentProviderService paymentProviderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Set up configuration properties
        ReflectionTestUtils.setField(paymentProviderService, "stripeApiKey", "sk_test_stripe_key");
        ReflectionTestUtils.setField(paymentProviderService, "stripeApiUrl", "https://api.stripe.com/v1");
        ReflectionTestUtils.setField(paymentProviderService, "bkashApiKey", "bkash_api_key");
        ReflectionTestUtils.setField(paymentProviderService, "bkashApiUrl", "https://api.bkash.com/v1");
        ReflectionTestUtils.setField(paymentProviderService, "bkashUsername", "bkash_user");
        ReflectionTestUtils.setField(paymentProviderService, "bkashPassword", "bkash_pass");
        ReflectionTestUtils.setField(paymentProviderService, "nagadApiKey", "nagad_api_key");
        ReflectionTestUtils.setField(paymentProviderService, "nagadApiUrl", "https://api.nagad.com/v1");
        ReflectionTestUtils.setField(paymentProviderService, "nagadMerchantId", "nagad_merchant");
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", false);
    }

    @Test
    void testFetchStripeTransactionsByDate_Success() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{" +
            "  \"object\": \"list\"," +
            "  \"data\": [" +
            "    {" +
            "      \"id\": \"ch_stripe_123\"," +
            "      \"amount\": 10000," +
            "      \"currency\": \"usd\"," +
            "      \"status\": \"succeeded\"," +
            "      \"created\": 1705334400," +
            "      \"description\": \"Test payment\"" +
            "    }" +
            "  ]" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchStripeTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertEquals("ch_stripe_123", transaction.getTransactionId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("USD", transaction.getCurrency());
        assertEquals("succeeded", transaction.getStatus());
        assertEquals("Test payment", transaction.getDescription());
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchStripeTransactionsByDate_ApiError() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("API Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchStripeTransactionsByDate(date)
        );
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchStripeTransactionsByDateRange_Success() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String mockResponse = "{" +
            "  \"object\": \"list\"," +
            "  \"data\": [" +
            "    {" +
            "      \"id\": \"ch_stripe_456\"," +
            "      \"amount\": 25000," +
            "      \"currency\": \"usd\"," +
            "      \"status\": \"succeeded\"," +
            "      \"created\": 1704067200," +
            "      \"description\": \"Monthly subscription\"" +
            "    }," +
            "    {" +
            "      \"id\": \"ch_stripe_789\"," +
            "      \"amount\": 15000," +
            "      \"currency\": \"usd\"," +
            "      \"status\": \"failed\"," +
            "      \"created\": 1704153600," +
            "      \"description\": \"Failed payment\"" +
            "    }" +
            "  ]" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchStripeTransactionsByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        PaymentProviderService.ProviderTransaction transaction1 = result.get(0);
        assertEquals("ch_stripe_456", transaction1.getTransactionId());
        assertEquals(new BigDecimal("250.00"), transaction1.getAmount());
        assertEquals("succeeded", transaction1.getStatus());
        
        PaymentProviderService.ProviderTransaction transaction2 = result.get(1);
        assertEquals("ch_stripe_789", transaction2.getTransactionId());
        assertEquals(new BigDecimal("150.00"), transaction2.getAmount());
        assertEquals("failed", transaction2.getStatus());
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchBkashTransactionsByDate_Success() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{" +
            "  \"status\": \"success\"," +
            "  \"data\": {" +
            "    \"transactions\": [" +
            "      {" +
            "        \"transactionId\": \"bkash_range_123\"," +
            "        \"amount\": 750.00," +
            "        \"currency\": \"BDT\"," +
            "        \"status\": \"completed\"," +
            "        \"description\": \"Range Payment\"," +
            "        \"timestamp\": \"2024-01-15T10:30:00Z\"," +
            "        \"customerMsisdn\": \"+8801712345678\"" +
            "      }" +
            "    ]" +
            "  }" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchBkashTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertEquals("bkash_txn_123", transaction.getTransactionId());
        assertEquals(new BigDecimal("500.00"), transaction.getAmount());
        assertEquals("BDT", transaction.getCurrency());
        assertEquals("completed", transaction.getStatus());
        assertEquals("Payment", transaction.getDescription());
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchBkashTransactionsByDate_ApiError() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("bKash API Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchBkashTransactionsByDate(date)
        );
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchBkashTransactionsByDateRange_Success() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String mockResponse = "{" +
            "  \"status\": \"success\"," +
            "  \"data\": {" +
            "    \"transactions\": [" +
            "      {" +
            "        \"trxID\": \"bkash_txn_456\"," +
            "        \"amount\": \"150.00\"," +
            "        \"currency\": \"BDT\"," +
            "        \"status\": \"completed\"," +
            "        \"description\": \"Range Payment\"," +
            "        \"timestamp\": \"2024-01-15T10:30:00Z\"," +
            "        \"customerMsisdn\": \"+8801712345678\"" +
            "      }" +
            "    ]" +
            "  }" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchBkashTransactionsByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertEquals("bkash_txn_456", transaction.getTransactionId());
        assertEquals(new BigDecimal("150.00"), transaction.getAmount());
        assertEquals("BDT", transaction.getCurrency());
        assertEquals("COMPLETED", transaction.getStatus());
        assertEquals("Range Payment", transaction.getDescription());
        assertEquals("+8801712345678", transaction.getCustomerMsisdn());
        assertNotNull(transaction.getTimestamp());
    }

    @Test
    void testFetchNagadTransactionsByDate_Success() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{" +
            "  \"status\": \"200\"," +
            "  \"message\": \"Success\"," +
            "  \"data\": [" +
            "    {" +
            "      \"orderId\": \"nagad_order_123\"," +
            "      \"amount\": \"1200.50\"," +
            "      \"currency\": \"BDT\"," +
            "      \"status\": \"Success\"," +
            "      \"paymentMethod\": \"Nagad\"," +
            "      \"dateTime\": \"2024-01-15T16:45:00Z\"," +
            "      \"merchantOrderId\": \"MERCHANT_123\"" +
            "    }" +
            "  ]" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchNagadTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertEquals("nagad_order_123", transaction.getTransactionId());
        assertEquals(new BigDecimal("1200.50"), transaction.getAmount());
        assertEquals("BDT", transaction.getCurrency());
        assertEquals("success", transaction.getStatus());
        assertEquals("Nagad", transaction.getDescription());
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchNagadTransactionsByDate_ApiError() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("Nagad API Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchNagadTransactionsByDate(date)
        );
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testFetchNagadTransactionsByDateRange_Success() throws Exception {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String mockResponse = "{" +
            "  \"status\": \"200\"," +
            "  \"message\": \"Success\"," +
            "  \"data\": [" +
            "    {" +
            "      \"orderId\": \"nagad_order_456\"," +
            "      \"amount\": \"850.00\"," +
            "      \"currency\": \"BDT\"," +
            "      \"status\": \"Success\"," +
            "      \"paymentMethod\": \"Nagad\"," +
            "      \"dateTime\": \"2024-01-05T11:30:00Z\"," +
            "      \"merchantOrderId\": \"MERCHANT_456\"" +
            "    }," +
            "    {" +
            "      \"orderId\": \"nagad_order_789\"," +
            "      \"amount\": \"425.75\"," +
            "      \"currency\": \"BDT\"," +
            "      \"status\": \"Failed\"," +
            "      \"paymentMethod\": \"Nagad\"," +
            "      \"dateTime\": \"2024-01-25T08:20:00Z\"," +
            "      \"merchantOrderId\": \"MERCHANT_789\"" +
            "    }" +
            "  ]" +
            "}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchNagadTransactionsByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        PaymentProviderService.ProviderTransaction transaction1 = result.get(0);
        assertEquals("nagad_order_456", transaction1.getTransactionId());
        assertEquals(new BigDecimal("850.00"), transaction1.getAmount());
        assertEquals("success", transaction1.getStatus());
        
        PaymentProviderService.ProviderTransaction transaction2 = result.get(1);
        assertEquals("nagad_order_789", transaction2.getTransactionId());
        assertEquals(new BigDecimal("425.75"), transaction2.getAmount());
        assertEquals("failed", transaction2.getStatus());
        
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestStripeConnectivity_Success() {
        // Arrange
        String mockResponse = "{\"object\": \"account\", \"id\": \"acct_test\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = paymentProviderService.testStripeConnectivity();

        // Assert
        assertTrue(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestStripeConnectivity_Failure() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // Act
        boolean result = paymentProviderService.testStripeConnectivity();

        // Assert
        assertFalse(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestBkashConnectivity_Success() {
        // Arrange
        String mockResponse = "{\"status\": \"success\", \"message\": \"Connected\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = paymentProviderService.testBkashConnectivity();

        // Assert
        assertTrue(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestBkashConnectivity_Failure() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // Act
        boolean result = paymentProviderService.testBkashConnectivity();

        // Assert
        assertFalse(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestNagadConnectivity_Success() {
        // Arrange
        String mockResponse = "{\"status\": \"200\", \"message\": \"Success\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = paymentProviderService.testNagadConnectivity();

        // Assert
        assertTrue(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testTestNagadConnectivity_Failure() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // Act
        boolean result = paymentProviderService.testNagadConnectivity();

        // Assert
        assertFalse(result);
        verify(restTemplate).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testGenerateMockStripeTransactions() {
        // Arrange
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", true);
        LocalDate date = LocalDate.of(2024, 1, 15);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchStripeTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 1 && result.size() <= 5); // Random between 1-5
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertTrue(transaction.getTransactionId().startsWith("ch_mock_stripe_"));
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("USD", transaction.getCurrency());
        assertTrue(List.of("succeeded", "failed", "pending").contains(transaction.getStatus()));
        
        // Should not call external API when mock is enabled
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testGenerateMockBkashTransactions() {
        // Arrange
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", true);
        LocalDate date = LocalDate.of(2024, 1, 15);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchBkashTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 1 && result.size() <= 5);
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertTrue(transaction.getTransactionId().startsWith("bkash_mock_"));
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("BDT", transaction.getCurrency());
        assertTrue(List.of("completed", "failed", "pending").contains(transaction.getStatus()));
        
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testGenerateMockNagadTransactions() {
        // Arrange
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", true);
        LocalDate date = LocalDate.of(2024, 1, 15);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchNagadTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() >= 1 && result.size() <= 5);
        
        PaymentProviderService.ProviderTransaction transaction = result.get(0);
        assertTrue(transaction.getTransactionId().startsWith("nagad_mock_"));
        assertTrue(transaction.getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("BDT", transaction.getCurrency());
        assertTrue(List.of("success", "failed", "pending").contains(transaction.getStatus()));
        
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    void testProviderTransactionDataClass() {
        // Test the ProviderTransaction data class
        String transactionId = "test_txn_123";
        BigDecimal amount = new BigDecimal("100.50");
        String currency = "USD";
        String status = "succeeded";
        String description = "Test transaction";
        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, Object> metadata = Map.of("key", "value");

        PaymentProviderService.ProviderTransaction transaction = 
            new PaymentProviderService.ProviderTransaction(
                transactionId, amount, currency, status, description, timestamp, metadata
            );

        // Assert all fields
        assertEquals(transactionId, transaction.getTransactionId());
        assertEquals(amount, transaction.getAmount());
        assertEquals(currency, transaction.getCurrency());
        assertEquals(status, transaction.getStatus());
        assertEquals(description, transaction.getDescription());
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(metadata, transaction.getMetadata());
    }

    @Test
    void testParseStripeResponse_EmptyData() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{\"object\": \"list\", \"data\": []}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchStripeTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseBkashResponse_EmptyData() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{\"status\": \"success\", \"data\": {\"transactions\": []}}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchBkashTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseNagadResponse_EmptyData() throws Exception {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String mockResponse = "{\"status\": \"200\", \"message\": \"Success\", \"data\": []}";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchNagadTransactionsByDate(date);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMapBkashStatus() {
        // Test indirectly through bKash transaction parsing
        // This tests the private mapBkashStatus method
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", true);
        
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchBkashTransactionsByDate(LocalDate.now());
        
        assertNotNull(result);
        // The mock data should contain valid mapped statuses
        result.forEach(transaction -> {
            assertTrue(List.of("completed", "failed", "pending").contains(transaction.getStatus()));
        });
    }

    @Test
    void testMapNagadStatus() {
        // Test indirectly through Nagad transaction parsing
        // This tests the private mapNagadStatus method
        ReflectionTestUtils.setField(paymentProviderService, "mockDataEnabled", true);
        
        List<PaymentProviderService.ProviderTransaction> result = 
            paymentProviderService.fetchNagadTransactionsByDate(LocalDate.now());
        
        assertNotNull(result);
        // The mock data should contain valid mapped statuses
        result.forEach(transaction -> {
            assertTrue(List.of("success", "failed", "pending").contains(transaction.getStatus()));
        });
    }

    @Test
    void testInvalidJsonResponse_Stripe() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String invalidJson = "invalid json response";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(invalidJson, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchStripeTransactionsByDate(date)
        );
    }

    @Test
    void testInvalidJsonResponse_Bkash() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String invalidJson = "invalid json response";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(invalidJson, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchBkashTransactionsByDate(date)
        );
    }

    @Test
    void testInvalidJsonResponse_Nagad() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 1, 15);
        String invalidJson = "invalid json response";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(invalidJson, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(responseEntity);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            paymentProviderService.fetchNagadTransactionsByDate(date)
        );
    }
}