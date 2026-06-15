-- V1 baseline (SPEC §11 M0): empty schema baseline.
-- Entities and their tables land in M1 (V2__*.sql onward). This file exists so Flyway
-- establishes its history table and the migration chain starts from a known empty point.
SELECT 1;
