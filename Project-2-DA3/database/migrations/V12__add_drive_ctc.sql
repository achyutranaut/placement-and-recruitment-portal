-- V12__add_drive_ctc.sql
-- Add numeric CTC attribute to PLACEMENT_DRIVE with validation constraint and default value

ALTER TABLE PLACEMENT_DRIVE ADD (
    CTC NUMBER(10,2) DEFAULT 12.00 NOT NULL,
    CONSTRAINT CHK_DRIVE_CTC CHECK (CTC > 0)
);

-- Seed realistic recruitment drive CTC compensation packages (in LPA)
UPDATE PLACEMENT_DRIVE SET CTC = 44.00 WHERE Drive_Id = 'DRV001'; -- Microsoft SDE I
UPDATE PLACEMENT_DRIVE SET CTC = 7.50  WHERE Drive_Id = 'DRV002'; -- Accenture ASE / TCS ASE
UPDATE PLACEMENT_DRIVE SET CTC = 9.50  WHERE Drive_Id = 'DRV003'; -- Infosys Specialist
UPDATE PLACEMENT_DRIVE SET CTC = 18.50 WHERE Drive_Id = 'DRV004'; -- Amazon Cloud Support
UPDATE PLACEMENT_DRIVE SET CTC = 16.00 WHERE Drive_Id = 'DRV005'; -- Oracle Assoc App Eng
UPDATE PLACEMENT_DRIVE SET CTC = 24.00 WHERE Drive_Id = 'DRV006'; -- Google Tech Analyst
UPDATE PLACEMENT_DRIVE SET CTC = 20.00 WHERE Drive_Id = 'DRV007'; -- Cisco Backend Eng
UPDATE PLACEMENT_DRIVE SET CTC = 15.00 WHERE Drive_Id = 'DRV008'; -- Cisco Network Eng

COMMIT;
