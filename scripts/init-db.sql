-- HopNGo Database Initialization Script
-- This script sets up the initial database schema for the HopNGo platform

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Simple initialization - let Hibernate create the tables
-- This file is kept minimal to avoid conflicts with JPA entity definitions

COMMIT;