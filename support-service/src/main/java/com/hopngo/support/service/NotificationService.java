package com.hopngo.support.service;

import com.hopngo.support.entity.Ticket;
import com.hopngo.support.entity.TicketMessage;
import com.hopngo.support.enums.TicketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    
    @Value("${notification.service.url:http://localhost:8091}")
    private String notificationServiceUrl;
    
    @Value("${notification.service.enabled:true}")
    private boolean notificationEnabled;

    @Autowired
    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notifyNewTicket(Ticket ticket) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_TICKET");
            notification.put("title", "New Support Ticket");
            notification.put("message", "New ticket created: " + ticket.getSubject());
            notification.put("priority", ticket.getPriority().name());
            notification.put("ticketId", ticket.getId());
            notification.put("userEmail", ticket.getEmail());
            notification.put("recipients", new String[]{"ROLE_AGENT", "ROLE_ADMIN"});
            
            sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send new ticket notification for ticket {}", ticket.getId(), e);
        }
    }

    public void notifyNewUserMessage(Ticket ticket, TicketMessage message) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_USER_MESSAGE");
            notification.put("title", "New User Message");
            notification.put("message", "New message on ticket: " + ticket.getSubject());
            notification.put("ticketId", ticket.getId());
            notification.put("messageId", message.getId());
            notification.put("userEmail", ticket.getEmail());
            
            // Notify assigned agent or all agents if unassigned
            if (ticket.getAssignedAgentId() != null) {
                notification.put("recipients", new String[]{ticket.getAssignedAgentId()});
            } else {
                notification.put("recipients", new String[]{"ROLE_AGENT", "ROLE_ADMIN"});
            }
            
            sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send user message notification for ticket {}", ticket.getId(), e);
        }
    }

    public void notifyAgentReply(Ticket ticket, TicketMessage message) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            // Notify user if they have a user ID (registered user)
            if (ticket.getUserId() != null) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "AGENT_REPLY");
                notification.put("title", "Support Team Reply");
                notification.put("message", "You have a new reply on your support ticket: " + ticket.getSubject());
                notification.put("ticketId", ticket.getId());
                notification.put("messageId", message.getId());
                notification.put("recipients", new String[]{ticket.getUserId()});
                
                sendNotification(notification);
            }
            
            // For anonymous users, you might send email notifications here
            // This would require integration with an email service
            
        } catch (Exception e) {
            logger.error("Failed to send agent reply notification for ticket {}", ticket.getId(), e);
        }
    }

    public void notifyTicketAssigned(Ticket ticket, String agentId) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "TICKET_ASSIGNED");
            notification.put("title", "Ticket Assigned");
            notification.put("message", "Ticket assigned to you: " + ticket.getSubject());
            notification.put("ticketId", ticket.getId());
            notification.put("priority", ticket.getPriority().name());
            notification.put("recipients", new String[]{agentId});
            
            sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send ticket assignment notification for ticket {}", ticket.getId(), e);
        }
    }

    public void notifyStatusChange(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            // Notify user if status changed to resolved or closed
            if ((newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) && 
                ticket.getUserId() != null) {
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "STATUS_CHANGE");
                notification.put("title", "Ticket Status Updated");
                notification.put("message", "Your support ticket status changed to: " + newStatus.getDisplayName());
                notification.put("ticketId", ticket.getId());
                notification.put("oldStatus", oldStatus.name());
                notification.put("newStatus", newStatus.name());
                notification.put("recipients", new String[]{ticket.getUserId()});
                
                sendNotification(notification);
            }
            
            // Notify assigned agent of status changes
            if (ticket.getAssignedAgentId() != null) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "AGENT_STATUS_CHANGE");
                notification.put("title", "Ticket Status Changed");
                notification.put("message", "Ticket status changed from " + oldStatus.getDisplayName() + 
                                          " to " + newStatus.getDisplayName() + ": " + ticket.getSubject());
                notification.put("ticketId", ticket.getId());
                notification.put("oldStatus", oldStatus.name());
                notification.put("newStatus", newStatus.name());
                notification.put("recipients", new String[]{ticket.getAssignedAgentId()});
                
                sendNotification(notification);
            }
            
        } catch (Exception e) {
            logger.error("Failed to send status change notification for ticket {}", ticket.getId(), e);
        }
    }

    public void notifyEscalation(Ticket ticket, String reason) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "TICKET_ESCALATION");
            notification.put("title", "Ticket Escalation");
            notification.put("message", "Ticket escalated: " + ticket.getSubject() + ". Reason: " + reason);
            notification.put("ticketId", ticket.getId());
            notification.put("priority", ticket.getPriority().name());
            notification.put("reason", reason);
            notification.put("recipients", new String[]{"ROLE_ADMIN"});
            
            sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send escalation notification for ticket {}", ticket.getId(), e);
        }
    }

    private void sendNotification(Map<String, Object> notification) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(notification, headers);
            
            String url = notificationServiceUrl + "/api/v1/notifications";
            restTemplate.postForEntity(url, request, String.class);
            
            logger.debug("Notification sent successfully: {}", notification.get("type"));
            
        } catch (Exception e) {
            logger.error("Failed to send notification to notification service", e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    public void notifyBulk(String type, String title, String message, String[] recipients) {
        if (!notificationEnabled) {
            return;
        }
        
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("recipients", recipients);
            
            sendNotification(notification);
            
        } catch (Exception e) {
            logger.error("Failed to send bulk notification", e);
        }
    }
}