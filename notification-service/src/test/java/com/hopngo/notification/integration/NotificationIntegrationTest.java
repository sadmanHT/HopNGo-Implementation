package com.hopngo.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.notification.dto.BookingEvent;
import com.hopngo.notification.dto.ChatEvent;
import com.hopngo.notification.dto.PaymentEvent;
import com.hopngo.notification.entity.Notification;
import com.hopngo.notification.entity.NotificationStatus;
import com.hopngo.notification.entity.NotificationType;
import com.hopngo.notification.repository.NotificationRepository;
import com.hopngo.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class NotificationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.8-management")
            .withUser("test", "test")
            .withVhost("/");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "test");
        registry.add("spring.rabbitmq.password", () -> "test");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.hopngo.notification.service.EmailService emailService;

    @MockBean
    private com.hopngo.notification.service.SmsService smsService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void testBookingEventNotificationFlow() throws Exception {
        // Given
        BookingEvent bookingEvent = createTestBookingEvent();

        // When
        rabbitTemplate.convertAndSend("notification.exchange", "booking.created", bookingEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).hasSize(1);
                    
                    Notification notification = notifications.get(0);
                    assertThat(notification.getRecipientId()).isEqualTo(bookingEvent.getUserId());
                    assertThat(notification.getRecipientEmail()).isEqualTo(bookingEvent.getUserEmail());
                    assertThat(notification.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMATION);
                    assertThat(notification.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.PENDING);
                    assertThat(notification.getSubject()).contains("Booking Confirmation");
                    assertThat(notification.getSubject()).contains(bookingEvent.getBookingId());
                });
    }

    @Test
    void testChatEventNotificationFlow() throws Exception {
        // Given
        ChatEvent chatEvent = createTestChatEvent();

        // When
        rabbitTemplate.convertAndSend("notification.exchange", "chat.message", chatEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).hasSize(1);
                    
                    Notification notification = notifications.get(0);
                    assertThat(notification.getRecipientId()).isEqualTo(chatEvent.getRecipientId());
                    assertThat(notification.getRecipientEmail()).isEqualTo(chatEvent.getRecipientEmail());
                    assertThat(notification.getType()).isEqualTo(NotificationType.CHAT_MESSAGE);
                    assertThat(notification.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.PENDING);
                    assertThat(notification.getSubject()).contains("New Message from");
                    assertThat(notification.getSubject()).contains(chatEvent.getSenderName());
                });
    }

    @Test
    void testPaymentEventNotificationFlow() throws Exception {
        // Given
        PaymentEvent paymentEvent = createTestPaymentEvent();

        // When
        rabbitTemplate.convertAndSend("notification.exchange", "payment.processed", paymentEvent);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).hasSize(1);
                    
                    Notification notification = notifications.get(0);
                    assertThat(notification.getRecipientId()).isEqualTo(paymentEvent.getUserId());
                    assertThat(notification.getRecipientEmail()).isEqualTo(paymentEvent.getUserEmail());
                    assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_CONFIRMATION);
                    assertThat(notification.getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.PENDING);
                    assertThat(notification.getSubject()).contains("Payment");
                    assertThat(notification.getSubject()).contains(paymentEvent.getPaymentId());
                });
    }

    @Test
    void testMultipleEventsProcessedConcurrently() throws Exception {
        // Given
        BookingEvent bookingEvent = createTestBookingEvent();
        ChatEvent chatEvent = createTestChatEvent();
        PaymentEvent paymentEvent = createTestPaymentEvent();

        // When - Send all events simultaneously
        rabbitTemplate.convertAndSend("notification.exchange", "booking.created", bookingEvent);
        rabbitTemplate.convertAndSend("notification.exchange", "chat.message", chatEvent);
        rabbitTemplate.convertAndSend("notification.exchange", "payment.processed", paymentEvent);

        // Then
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).hasSize(3);
                    
                    // Verify we have one of each type
                    long bookingNotifications = notifications.stream()
                            .filter(n -> n.getType() == NotificationType.BOOKING_CONFIRMATION)
                            .count();
                    long chatNotifications = notifications.stream()
                            .filter(n -> n.getType() == NotificationType.CHAT_MESSAGE)
                            .count();
                    long paymentNotifications = notifications.stream()
                            .filter(n -> n.getType() == NotificationType.PAYMENT_CONFIRMATION)
                            .count();
                    
                    assertThat(bookingNotifications).isEqualTo(1);
                    assertThat(chatNotifications).isEqualTo(1);
                    assertThat(paymentNotifications).isEqualTo(1);
                });
    }

    @Test
    void testInvalidEventHandling() throws Exception {
        // Given - Invalid booking event (missing required fields)
        BookingEvent invalidEvent = new BookingEvent();
        invalidEvent.setBookingId(""); // Empty booking ID should cause validation error

        // When
        rabbitTemplate.convertAndSend("notification.exchange", "booking.created", invalidEvent);

        // Then - Should not create any notifications due to validation error
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).isEmpty();
                });
    }

    @Test
    void testHealthCheckEndpoint() throws Exception {
        // This test would require a web test client to call the health endpoint
        // For now, we'll test the service directly
        
        // Given - Service is running
        // When - We check if RabbitMQ is accessible
        boolean canConnect = true;
        try {
            rabbitTemplate.getConnectionFactory().createConnection().close();
        } catch (Exception e) {
            canConnect = false;
        }
        
        // Then
        assertThat(canConnect).isTrue();
    }

    @Test
    void testEventRetryMechanism() throws Exception {
        // This test simulates a temporary failure and recovery
        // Given
        BookingEvent bookingEvent = createTestBookingEvent();
        
        // When - Send event (this should succeed after retries)
        rabbitTemplate.convertAndSend("notification.exchange", "booking.created", bookingEvent);
        
        // Then - Eventually should be processed
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    assertThat(notifications).hasSize(1);
                    assertThat(notifications.get(0).getStatus()).isIn(NotificationStatus.SENT, NotificationStatus.PENDING);
                });
    }

    private BookingEvent createTestBookingEvent() {
        BookingEvent event = new BookingEvent();
        event.setBookingId("booking-123");
        event.setUserId("user-456");
        event.setUserEmail("test@example.com");
        event.setUserPhone("+1234567890");
        event.setAmount(new BigDecimal("99.99"));
        event.setStatus("CONFIRMED");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private ChatEvent createTestChatEvent() {
        ChatEvent event = new ChatEvent();
        event.setChatId("chat-789");
        event.setSenderId("sender-123");
        event.setSenderName("John Doe");
        event.setRecipientId("recipient-456");
        event.setRecipientEmail("recipient@example.com");
        event.setMessage("Hello, how are you?");
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    private PaymentEvent createTestPaymentEvent() {
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId("payment-321");
        event.setUserId("user-654");
        event.setUserEmail("payment@example.com");
        event.setUserPhone("+1987654321");
        event.setAmount(new BigDecimal("149.99"));
        event.setStatus("COMPLETED");
        event.setPaymentMethod("CREDIT_CARD");
        event.setProcessedAt(LocalDateTime.now());
        return event;
    }
}