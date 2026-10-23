-- V18: Recruiter-Company Authorization Junction Table & Recruiter Profile Support
-- Supports many-to-many recruiter ↔ company relationships
-- Replaces the single Reference_Id limitation in PORTAL_USER

-- 1. Extend PORTAL_USER with optional Full_Name and Email
ALTER TABLE PORTAL_USER ADD (Full_Name VARCHAR2(100), Email VARCHAR2(100));

-- 2. Create normalized junction table
CREATE TABLE RECRUITER_COMPANY (
    User_Id       VARCHAR2(50)   NOT NULL,
    Company_Id    VARCHAR2(20)   NOT NULL,
    Authorized_At TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_RECRUITER_COMPANY PRIMARY KEY (User_Id, Company_Id),
    CONSTRAINT FK_RC_USER FOREIGN KEY (User_Id) REFERENCES PORTAL_USER(User_Id) ON DELETE CASCADE,
    CONSTRAINT FK_RC_COMPANY FOREIGN KEY (Company_Id) REFERENCES COMPANY(Company_Id) ON DELETE CASCADE
);

-- 3. Populate existing recruiter profile display names
UPDATE PORTAL_USER SET Full_Name = 'Vikram Singh', Email = 'vikram.singh@microsoft.com' WHERE User_Id = 'USR003';
UPDATE PORTAL_USER SET Full_Name = 'Neha Sharma', Email = 'neha.sharma@tcs.com' WHERE User_Id = 'USR004';

-- 4. Migrate existing recruiter Reference_Id values into the junction table
INSERT INTO RECRUITER_COMPANY (User_Id, Company_Id)
SELECT pu.User_Id, pu.Reference_Id
FROM PORTAL_USER pu
WHERE pu.Role = 'ROLE_RECRUITER'
  AND pu.Reference_Id IS NOT NULL
  AND EXISTS (SELECT 1 FROM COMPANY c WHERE c.Company_Id = pu.Reference_Id);

-- 5. Authorize recruiter1 (USR003) for COM001 (TCS) in addition to COM004 (Microsoft)
-- to allow instant multi-company recruiter switching verification
INSERT INTO RECRUITER_COMPANY (User_Id, Company_Id)
SELECT 'USR003', 'COM001'
FROM DUAL
WHERE EXISTS (SELECT 1 FROM PORTAL_USER WHERE User_Id = 'USR003')
  AND EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM001')
  AND NOT EXISTS (SELECT 1 FROM RECRUITER_COMPANY WHERE User_Id = 'USR003' AND Company_Id = 'COM001');

-- 6. Ensure every registered company in COMPANY has an active recruiter in PORTAL_USER
INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id, Full_Name, Email)
SELECT 
    'USR_' || c.Company_Id,
    LOWER(c.Company_Id) || '_recruiter',
    '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
    'ROLE_RECRUITER',
    c.Company_Id,
    NVL((SELECT ec.Company_Name FROM EMAIL_COMPANY ec WHERE ec.Email = c.Email AND ROWNUM = 1), c.Company_Id) || ' Recruiter',
    c.Email
FROM COMPANY c
WHERE NOT EXISTS (
    SELECT 1 FROM PORTAL_USER pu WHERE pu.Reference_Id = c.Company_Id AND pu.Role = 'ROLE_RECRUITER'
);

-- 7. Ensure all recruiter accounts are authorized for their company in RECRUITER_COMPANY
INSERT INTO RECRUITER_COMPANY (User_Id, Company_Id)
SELECT pu.User_Id, pu.Reference_Id
FROM PORTAL_USER pu
WHERE pu.Role = 'ROLE_RECRUITER'
  AND pu.Reference_Id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM RECRUITER_COMPANY rc WHERE rc.User_Id = pu.User_Id AND rc.Company_Id = pu.Reference_Id
  );

COMMIT;
