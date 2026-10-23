-- ============================================================
-- V16__align_drive_and_offer_packages.sql
-- Synchronize Recruitment Drive CTC and Offer Letter CTC
-- Resolves data discrepancy between Drive packages and Offer Letter packages
-- ============================================================

-- 1. Align PLACEMENT_DRIVE base CTC packages to industry standards
-- DRV001: Microsoft India - Software Development Engineer I is 44.00 LPA
UPDATE PLACEMENT_DRIVE SET CTC = 44.00 WHERE Drive_Id = 'DRV001';

-- 2. Synchronize all OFFER_LETTER records to match their corresponding PLACEMENT_DRIVE CTC
-- via canonical APPLICATION.Drive_Id relation
UPDATE OFFER_LETTER o
SET o.CTC_LPA = (
    SELECT d.CTC
    FROM APPLICATION a
    JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
    WHERE a.Application_Id = o.Application_Id
)
WHERE EXISTS (
    SELECT 1
    FROM APPLICATION a
    JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
    WHERE a.Application_Id = o.Application_Id
);

-- 3. Explicit update fallbacks for seeded offer letters to guarantee exact consistency
UPDATE OFFER_LETTER SET CTC_LPA = 44.00 WHERE Offer_Id IN ('OFF001', 'OFF004', 'OFF006', 'OFF008', 'OFF009', 'OFF011', 'OFF013', 'OFF014');
UPDATE OFFER_LETTER SET CTC_LPA = 7.50  WHERE Offer_Id = 'OFF012';
UPDATE OFFER_LETTER SET CTC_LPA = 9.50  WHERE Offer_Id IN ('OFF002', 'OFF003', 'OFF007', 'OFF010');
UPDATE OFFER_LETTER SET CTC_LPA = 18.50 WHERE Offer_Id IN ('OFF005', 'OFF015');

COMMIT;
