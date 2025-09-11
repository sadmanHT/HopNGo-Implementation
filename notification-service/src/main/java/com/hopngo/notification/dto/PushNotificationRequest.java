package com.hopngo.notification.dto;

import java.util.Map;

public class PushNotificationRequest {
    private String token;
    private String title;
    private String body;
    private String imageUrl;
    private Map<String, String> data;
    private String priority; // "normal" or "high"
    private String sound;
    private String clickAction;
    private Integer badge;
    private String tag;
    private String color;
    private Boolean contentAvailable;
    private Boolean mutableContent;

    // Private constructor for builder pattern
    private PushNotificationRequest() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PushNotificationRequest request = new PushNotificationRequest();

        public Builder token(String token) {
            request.token = token;
            return this;
        }

        public Builder title(String title) {
            request.title = title;
            return this;
        }

        public Builder body(String body) {
            request.body = body;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            request.imageUrl = imageUrl;
            return this;
        }

        public Builder data(Map<String, String> data) {
            request.data = data;
            return this;
        }

        public Builder priority(String priority) {
            request.priority = priority;
            return this;
        }

        public Builder sound(String sound) {
            request.sound = sound;
            return this;
        }

        public Builder clickAction(String clickAction) {
            request.clickAction = clickAction;
            return this;
        }

        public Builder badge(Integer badge) {
            request.badge = badge;
            return this;
        }

        public Builder tag(String tag) {
            request.tag = tag;
            return this;
        }

        public Builder color(String color) {
            request.color = color;
            return this;
        }

        public Builder contentAvailable(Boolean contentAvailable) {
            request.contentAvailable = contentAvailable;
            return this;
        }

        public Builder mutableContent(Boolean mutableContent) {
            request.mutableContent = mutableContent;
            return this;
        }

        public PushNotificationRequest build() {
            return request;
        }
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getPriority() {
        return priority;
    }

    public String getSound() {
        return sound;
    }

    public String getClickAction() {
        return clickAction;
    }

    public Integer getBadge() {
        return badge;
    }

    public String getTag() {
        return tag;
    }

    public String getColor() {
        return color;
    }

    public Boolean getContentAvailable() {
        return contentAvailable;
    }

    public Boolean getMutableContent() {
        return mutableContent;
    }

    // Setters (for JSON deserialization)
    public void setToken(String token) {
        this.token = token;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public void setClickAction(String clickAction) {
        this.clickAction = clickAction;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setContentAvailable(Boolean contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    public void setMutableContent(Boolean mutableContent) {
        this.mutableContent = mutableContent;
    }

    @Override
    public String toString() {
        return "PushNotificationRequest{" +
                "token='" + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : null) + "'" +
                ", title='" + title + "'" +
                ", body='" + body + "'" +
                ", imageUrl='" + imageUrl + "'" +
                ", data=" + data +
                ", priority='" + priority + "'" +
                ", sound='" + sound + "'" +
                ", clickAction='" + clickAction + "'" +
                ", badge=" + badge +
                ", tag='" + tag + "'" +
                ", color='" + color + "'" +
                ", contentAvailable=" + contentAvailable +
                ", mutableContent=" + mutableContent +
                '}';
    }
}