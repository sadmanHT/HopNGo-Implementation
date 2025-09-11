FROM alpine:3.18

# Install required packages for backup operations
RUN apk add --no-cache \
    postgresql15-client \
    mongodb-tools \
    redis \
    curl \
    jq \
    bash \
    gzip \
    tar \
    openssh-client \
    rsync \
    ca-certificates \
    tzdata

# Set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create backup user and directories
RUN addgroup -g 1000 backup && \
    adduser -D -u 1000 -G backup backup

# Create backup directories
RUN mkdir -p /backups /scripts /tmp/backup-work && \
    chown -R backup:backup /backups /scripts /tmp/backup-work

# Copy backup scripts
COPY scripts/ /scripts/
RUN chmod +x /scripts/*.sh && \
    chown -R backup:backup /scripts

# Switch to backup user
USER backup

# Set working directory
WORKDIR /scripts

# Default command
CMD ["/scripts/backup.sh"]