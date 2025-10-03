-- Migration: Add color_code column to instructor_availability table
-- Author: Wilfred Njuguna
-- Date: 2024-10-03
-- Description: Adds support for color coding blocked times for UI visualization

ALTER TABLE instructor_availability
    ADD COLUMN color_code VARCHAR(7);

COMMENT ON COLUMN instructor_availability.color_code IS 'Hex color code for UI visualization (e.g., "#FF6B6B")';