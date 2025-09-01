-- Create Notification System Tables
-- This migration creates the tables needed for the notifications module

-- User notification preferences table
CREATE TABLE user_notification_preferences (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uuid UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT true,
    in_app_enabled BOOLEAN NOT NULL DEFAULT true,
    sms_enabled BOOLEAN NOT NULL DEFAULT false,
    push_enabled BOOLEAN NOT NULL DEFAULT true,
    digest_mode VARCHAR(20) NOT NULL DEFAULT 'IMMEDIATE',
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    CONSTRAINT uk_user_notification_category UNIQUE (user_uuid, category),
    CONSTRAINT chk_digest_mode CHECK (digest_mode IN ('IMMEDIATE', 'HOURLY', 'DAILY', 'WEEKLY', 'DISABLED')),
    CONSTRAINT chk_notification_category CHECK (category IN ('LEARNING_PROGRESS', 'ASSIGNMENTS_GRADING', 'COURSE_MANAGEMENT', 'SOCIAL_LEARNING', 'SYSTEM_ADMIN'))
);

-- Notification delivery log table
CREATE TABLE notification_delivery_log (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL UNIQUE,
    user_uuid UUID NOT NULL,
    recipient_email VARCHAR(255),
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    delivery_channel VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    template_used VARCHAR(100),
    organization_uuid UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'COURSE_ENROLLMENT_WELCOME',
        'COURSE_COMPLETION_CERTIFICATE', 
        'LEARNING_MILESTONE_ACHIEVED',
        'ASSIGNMENT_DUE_REMINDER',
        'ASSIGNMENT_SUBMITTED_CONFIRMATION',
        'ASSIGNMENT_GRADED',
        'ASSIGNMENT_RETURNED_FOR_REVISION',
        'NEW_STUDENT_ENROLLMENT',
        'NEW_ASSIGNMENT_SUBMISSION',
        'GRADING_REMINDER',
        'USER_INVITATION_SENT',
        'INVITATION_ACCEPTED',
        'INVITATION_DECLINED',
        'INVITATION_EXPIRY_REMINDER',
        'ACCOUNT_CREATED',
        'PASSWORD_RESET_REQUEST',
        'SECURITY_ALERT',
        'WEEKLY_PROGRESS_SUMMARY',
        'LEARNING_STREAK_ACHIEVEMENT',
        'PEER_ACHIEVEMENT_CELEBRATION'
    )),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_delivery_status CHECK (delivery_status IN ('QUEUED', 'PENDING', 'DELIVERED', 'FAILED', 'BLOCKED', 'EXPIRED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_user_notification_preferences_user_uuid ON user_notification_preferences (user_uuid);
CREATE INDEX idx_notification_log_user_uuid ON notification_delivery_log (user_uuid);
CREATE INDEX idx_notification_log_status ON notification_delivery_log (delivery_status);
CREATE INDEX idx_notification_log_type ON notification_delivery_log (notification_type);
CREATE INDEX idx_notification_log_sent_at ON notification_delivery_log (sent_at);
CREATE INDEX idx_notification_log_notification_id ON notification_delivery_log (notification_id);