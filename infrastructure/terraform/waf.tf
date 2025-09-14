# AWS WAF v2 Configuration for HopNGo Platform
# Provides comprehensive protection against common web attacks

resource "aws_wafv2_web_acl" "hopngo_waf" {
  name  = "hopngo-web-acl"
  description = "WAF for HopNGo platform with comprehensive security rules"
  scope = "CLOUDFRONT"  # Use REGIONAL for ALB

  default_action {
    allow {}
  }

  # Rule 1: AWS Managed Core Rule Set (OWASP Top 10)
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 1

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"

        # Exclude specific rules if needed
        excluded_rule {
          name = "SizeRestrictions_BODY"
        }
        excluded_rule {
          name = "GenericRFI_BODY"
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "CommonRuleSetMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 2: Known Bad Inputs (SQLi, XSS, etc.)
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "KnownBadInputsMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 3: SQL Injection Protection
  rule {
    name     = "AWSManagedRulesSQLiRuleSet"
    priority = 3

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesSQLiRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "SQLiRuleSetMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 4: Bot Control (Advanced Bot Protection)
  rule {
    name     = "AWSManagedRulesBotControlRuleSet"
    priority = 4

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesBotControlRuleSet"
        vendor_name = "AWS"

        managed_rule_group_configs {
          aws_managed_rules_bot_control_rule_set {
            inspection_level = "TARGETED"  # COMMON or TARGETED
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "BotControlMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 5: Rate Limiting for API endpoints
  rule {
    name     = "RateLimitRule"
    priority = 5

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = 2000  # requests per 5-minute window
        aggregate_key_type = "IP"

        scope_down_statement {
          byte_match_statement {
            search_string = "/api/"
            field_to_match {
              uri_path {}
            }
            text_transformation {
              priority = 1
              type     = "LOWERCASE"
            }
            positional_constraint = "STARTS_WITH"
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "RateLimitMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 6: Aggressive Rate Limiting for Auth endpoints
  rule {
    name     = "AuthRateLimitRule"
    priority = 6

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = 100  # requests per 5-minute window
        aggregate_key_type = "IP"

        scope_down_statement {
          or_statement {
            statement {
              byte_match_statement {
                search_string = "/api/auth/login"
                field_to_match {
                  uri_path {}
                }
                text_transformation {
                  priority = 1
                  type     = "LOWERCASE"
                }
                positional_constraint = "EXACTLY"
              }
            }
            statement {
              byte_match_statement {
                search_string = "/api/auth/register"
                field_to_match {
                  uri_path {}
                }
                text_transformation {
                  priority = 1
                  type     = "LOWERCASE"
                }
                positional_constraint = "EXACTLY"
              }
            }
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AuthRateLimitMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 7: Block Known Malicious IPs
  rule {
    name     = "AWSManagedRulesAmazonIpReputationList"
    priority = 7

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "IpReputationMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 8: Geographic Restrictions (if needed)
  rule {
    name     = "GeoBlockRule"
    priority = 8

    action {
      block {}
    }

    statement {
      geo_match_statement {
        # Block requests from high-risk countries
        country_codes = ["CN", "RU", "KP", "IR"]  # Adjust as needed
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "GeoBlockMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 9: Custom File Upload Protection
  rule {
    name     = "FileUploadProtection"
    priority = 9

    action {
      block {}
    }

    statement {
      and_statement {
        statement {
          byte_match_statement {
            search_string = "/api/upload"
            field_to_match {
              uri_path {}
            }
            text_transformation {
              priority = 1
              type     = "LOWERCASE"
            }
            positional_constraint = "STARTS_WITH"
          }
        }
        statement {
          size_constraint_statement {
            field_to_match {
              body {}
            }
            comparison_operator = "GT"
            size                = 10485760  # 10MB limit
            text_transformation {
              priority = 1
              type     = "NONE"
            }
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "FileUploadProtectionMetric"
      sampled_requests_enabled   = true
    }
  }

  # Rule 10: Admin Panel Protection
  rule {
    name     = "AdminPanelProtection"
    priority = 10

    action {
      block {}
    }

    statement {
      and_statement {
        statement {
          byte_match_statement {
            search_string = "/admin"
            field_to_match {
              uri_path {}
            }
            text_transformation {
              priority = 1
              type     = "LOWERCASE"
            }
            positional_constraint = "STARTS_WITH"
          }
        }
        statement {
          not_statement {
            statement {
              ip_set_reference_statement {
                arn = aws_wafv2_ip_set.admin_whitelist.arn
              }
            }
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AdminPanelProtectionMetric"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "HopNGoWAF"
    sampled_requests_enabled   = true
  }

  tags = {
    Name        = "hopngo-waf"
    Environment = var.environment
    Project     = "hopngo"
  }
}

# IP Set for Admin Whitelist
resource "aws_wafv2_ip_set" "admin_whitelist" {
  name  = "admin-whitelist"
  description = "Whitelisted IPs for admin access"
  scope = "CLOUDFRONT"
  ip_address_version = "IPV4"

  addresses = [
    "203.0.113.0/24",  # Office IP range
    "198.51.100.0/24", # VPN IP range
    # Add your admin IP addresses here
  ]

  tags = {
    Name        = "admin-whitelist"
    Environment = var.environment
    Project     = "hopngo"
  }
}

# CloudWatch Log Group for WAF
resource "aws_cloudwatch_log_group" "waf_log_group" {
  name              = "/aws/wafv2/hopngo"
  retention_in_days = 30

  tags = {
    Name        = "hopngo-waf-logs"
    Environment = var.environment
    Project     = "hopngo"
  }
}

# WAF Logging Configuration
resource "aws_wafv2_web_acl_logging_configuration" "waf_logging" {
  resource_arn            = aws_wafv2_web_acl.hopngo_waf.arn
  log_destination_configs = [aws_cloudwatch_log_group.waf_log_group.arn]

  redacted_fields {
    single_header {
      name = "authorization"
    }
  }

  redacted_fields {
    single_header {
      name = "cookie"
    }
  }
}

# CloudWatch Alarms for WAF Monitoring
resource "aws_cloudwatch_metric_alarm" "waf_blocked_requests" {
  alarm_name          = "hopngo-waf-blocked-requests-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "BlockedRequests"
  namespace           = "AWS/WAFV2"
  period              = "300"
  statistic           = "Sum"
  threshold           = "100"
  alarm_description   = "This metric monitors blocked requests by WAF"
  alarm_actions       = [aws_sns_topic.security_alerts.arn]

  dimensions = {
    WebACL = aws_wafv2_web_acl.hopngo_waf.name
    Region = "CloudFront"
  }

  tags = {
    Name        = "hopngo-waf-blocked-requests"
    Environment = var.environment
    Project     = "hopngo"
  }
}

# SNS Topic for Security Alerts
resource "aws_sns_topic" "security_alerts" {
  name = "hopngo-security-alerts"

  tags = {
    Name        = "hopngo-security-alerts"
    Environment = var.environment
    Project     = "hopngo"
  }
}

# Output WAF ARN for association with CloudFront/ALB
output "waf_acl_arn" {
  description = "ARN of the WAF Web ACL"
  value       = aws_wafv2_web_acl.hopngo_waf.arn
}

output "waf_acl_id" {
  description = "ID of the WAF Web ACL"
  value       = aws_wafv2_web_acl.hopngo_waf.id
}