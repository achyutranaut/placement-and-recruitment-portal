-- ============================================================
-- V27__enforce_placement_pipeline_state_machine.sql
-- Enforce Placement Pipeline State Machine & Round Completion Validation
-- 1. Data Reconciliation: Repair APP036 & STU023 before triggers
-- 2. Authoritative PL/SQL Function: GET_APPLICATION_READINESS
-- 3. Hardened PL/SQL Procedure: ISSUE_OFFER (Requires SELECTED + READY)
-- 4. Database Trigger: TRG_ENFORCE_APPLICATION_STATE_MACHINE
-- 5. Database Trigger: TRG_VALIDATE_OFFER_ISSUANCE
-- ============================================================

-- ------------------------------------------------------------
-- 1. DATA RECONCILIATION: Repair APP036 & STU023 before triggers
-- ------------------------------------------------------------
DECLARE
    v_exists NUMBER;
BEGIN
    -- Remove premature offer letters issued without round completion
    DELETE FROM OFFER_LETTER WHERE Application_Id = 'APP036';

    -- Revert Application status to legitimate intermediate state (INTERVIEWING)
    UPDATE APPLICATION 
    SET Status = 'INTERVIEWING' 
    WHERE Application_Id = 'APP036';

    -- Reset Student placement status to NOT_PLACED
    UPDATE STUDENT 
    SET Placement_Status = 'NOT_PLACED' 
    WHERE Student_Id = 'STU023';

    -- Log repair in APPLICATION_AUDIT
    INSERT INTO APPLICATION_AUDIT (Application_Id, Old_Status, New_Status, Changed_At, Changed_By)
    VALUES ('APP036', 'ACCEPTED', 'INTERVIEWING', CURRENT_TIMESTAMP, 'SYSTEM_STATE_MACHINE_REPAIR');
END;
/

-- ------------------------------------------------------------
-- 2. FUNCTION: GET_APPLICATION_READINESS
--    Authoritative PL/SQL check evaluating drive-configured mandatory rounds.
--    Accepts optional p_drive_id to prevent ORA-04091 mutating table errors
--    when invoked from row-level triggers on APPLICATION.
--    Returns 'READY' or 'NOT_READY: <reason>'
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION GET_APPLICATION_READINESS (
    p_application_id IN VARCHAR2,
    p_drive_id       IN VARCHAR2 DEFAULT NULL
) RETURN VARCHAR2 AS
    v_app_status        VARCHAR2(30);
    v_drive_id          VARCHAR2(20) := p_drive_id;
    v_selection_process VARCHAR2(500);
    v_total_rounds      NUMBER := 0;
    v_round_title       VARCHAR2(200);
    v_result            VARCHAR2(30);
    v_score             NUMBER;
BEGIN
    -- 1. Check Application / Drive
    IF v_drive_id IS NULL THEN
        BEGIN
            SELECT Status, Drive_Id 
            INTO v_app_status, v_drive_id
            FROM APPLICATION
            WHERE Application_Id = p_application_id;

            IF UPPER(v_app_status) = 'REJECTED' THEN
                RETURN 'NOT_READY: Application is REJECTED';
            END IF;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RETURN 'NOT_READY: Application ' || p_application_id || ' not found';
        END;
    END IF;

    IF v_drive_id IS NULL THEN
        RETURN 'NOT_READY: Application is not linked to any placement drive';
    END IF;

    -- 2. Check Drive Selection Process
    BEGIN
        SELECT NVL(Selection_Process, '')
        INTO v_selection_process
        FROM PLACEMENT_DRIVE
        WHERE Drive_Id = v_drive_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN 'NOT_READY: Placement drive ' || v_drive_id || ' not found';
    END;

    -- 3. Determine configured rounds from Selection_Process
    v_total_rounds := REGEXP_COUNT(v_selection_process, 'Round\s*[0-9]+', 1, 'i');
    IF v_total_rounds = 0 THEN
        v_total_rounds := 3;
    END IF;

    -- 4. Check each required round 1 .. v_total_rounds
    FOR r IN 1 .. v_total_rounds LOOP
        v_round_title := REGEXP_SUBSTR(v_selection_process, 'Round\s*' || r || '\s*:[^|]+', 1, 1, 'i');
        IF v_round_title IS NULL OR LENGTH(TRIM(v_round_title)) = 0 THEN
            v_round_title := 'Round ' || r;
        ELSE
            v_round_title := TRIM(v_round_title);
        END IF;

        v_result := NULL;
        v_score := NULL;

        -- Check interview records matching round number r
        SELECT MAX(i.Result),
               MAX(CASE 
                   WHEN r = 1 THEN NVL(i.OA, NVL(i.GD, i.HR))
                   WHEN r = 2 THEN NVL(i.GD, NVL(i.OA, i.HR))
                   WHEN r = 3 THEN NVL(i.HR, NVL(i.GD, i.OA))
                   ELSE NVL(i.OA, NVL(i.GD, i.HR))
               END)
        INTO v_result, v_score
        FROM INTERVIEW i
        JOIN INTERVIEWER_ROUND ir ON i.Interviewer_Name = ir.Interviewer_Name
        WHERE i.Application_Id = p_application_id
          AND ir.Interview_Round_No = r;

        -- Fallback: support single-row interview structure
        IF v_result IS NULL THEN
            SELECT MAX(Result),
                   MAX(CASE 
                       WHEN r = 1 THEN OA
                       WHEN r = 2 THEN GD
                       WHEN r = 3 THEN HR
                       ELSE NULL
                   END)
            INTO v_result, v_score
            FROM INTERVIEW
            WHERE Application_Id = p_application_id
              AND (
                  (r = 1 AND OA IS NOT NULL) OR
                  (r = 2 AND GD IS NOT NULL) OR
                  (r = 3 AND HR IS NOT NULL)
              );
        END IF;

        -- Evaluate round status
        IF v_result IS NULL THEN
            RETURN 'NOT_READY: ' || v_round_title || ' is pending / not scheduled';
        ELSIF UPPER(v_result) = 'PENDING' THEN
            RETURN 'NOT_READY: ' || v_round_title || ' is still pending';
        ELSIF UPPER(v_result) IN ('REJECTED', 'FAILED') THEN
            RETURN 'NOT_READY: ' || v_round_title || ' was failed/rejected';
        ELSIF UPPER(v_result) NOT IN ('CLEARED', 'PASSED') THEN
            RETURN 'NOT_READY: ' || v_round_title || ' has invalid status ' || v_result;
        ELSIF v_score IS NULL THEN
            RETURN 'NOT_READY: ' || v_round_title || ' score is missing';
        END IF;
    END LOOP;

    RETURN 'READY';
END GET_APPLICATION_READINESS;
/

-- ------------------------------------------------------------
-- 3. PROCEDURE: ISSUE_OFFER (Hardened with readiness validation)
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE ISSUE_OFFER (
    p_application_id IN VARCHAR2,
    p_ctc_lpa        IN NUMBER,
    p_offer_date     IN DATE DEFAULT SYSDATE,
    o_offer_id       OUT VARCHAR2
) AS
    v_app_status VARCHAR2(30);
    v_readiness  VARCHAR2(500);
    v_existing   NUMBER;
    v_next_num   NUMBER;
BEGIN
    SAVEPOINT sp_issue_offer;

    -- 1. Check Application Status
    BEGIN
        SELECT Status INTO v_app_status
        FROM APPLICATION
        WHERE Application_Id = p_application_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20032, 'Application ' || p_application_id || ' does not exist.');
    END;

    IF UPPER(v_app_status) != 'SELECTED' THEN
        RAISE_APPLICATION_ERROR(-20033, 'Cannot issue offer: Application ' || p_application_id || ' is in status ' || v_app_status || ', but must be SELECTED.');
    END IF;

    -- 2. Verify Application Readiness (all mandatory rounds passed)
    v_readiness := GET_APPLICATION_READINESS(p_application_id);
    IF v_readiness != 'READY' THEN
        RAISE_APPLICATION_ERROR(-20030, 'Cannot issue offer: ' || v_readiness);
    END IF;

    -- 3. Check if Offer already exists
    SELECT COUNT(*) INTO v_existing 
    FROM OFFER_LETTER 
    WHERE Application_Id = p_application_id;

    IF v_existing > 0 THEN
        RAISE_APPLICATION_ERROR(-20031, 'An offer has already been issued for this application.');
    END IF;

    -- 4. Generate Next Offer ID
    SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(Offer_Id, '[0-9]+'))), 0) + 1 
    INTO v_next_num 
    FROM OFFER_LETTER;

    o_offer_id := 'OFF' || LPAD(v_next_num, 3, '0');

    -- 5. Insert Offer Letter (Triggers will automatically validate and set APPLICATION.Status to 'OFFERED')
    INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA, Status)
    VALUES (o_offer_id, p_application_id, NVL(p_offer_date, SYSDATE), p_ctc_lpa, 'OFFERED');

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_issue_offer;
        RAISE;
END ISSUE_OFFER;
/

-- ------------------------------------------------------------
-- 4. TRIGGER: TRG_ENFORCE_APPLICATION_STATE_MACHINE
--    Guarantees that state transitions follow APPLIED -> SHORTLISTED ->
--    INTERVIEWING -> SELECTED -> OFFERED -> ACCEPTED.
--    Blocks invalid transitions, skips, and premature selections.
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ENFORCE_APPLICATION_STATE_MACHINE
BEFORE INSERT OR UPDATE OF Status ON APPLICATION
FOR EACH ROW
DECLARE
    v_readiness VARCHAR2(500);
    v_offer_cnt NUMBER := 0;
BEGIN
    IF INSERTING THEN
        IF :NEW.Status IS NOT NULL AND UPPER(:NEW.Status) NOT IN ('APPLIED', 'SHORTLISTED', 'INTERVIEWING') THEN
            RAISE_APPLICATION_ERROR(-20061, 'Invalid initial status: New applications must start as APPLIED. Cannot insert directly as ' || :NEW.Status);
        END IF;
        RETURN;
    END IF;

    -- No change in status -> allowed
    IF :OLD.Status = :NEW.Status THEN
        RETURN;
    END IF;

    -- Rejection is allowed from any non-finalized state
    IF UPPER(:NEW.Status) = 'REJECTED' THEN
        IF UPPER(:OLD.Status) IN ('ACCEPTED', 'DECLINED') THEN
            RAISE_APPLICATION_ERROR(-20055, 'Cannot reject application ' || :NEW.Application_Id || ' from finalized state ' || :OLD.Status);
        END IF;
        RETURN;
    END IF;

    -- Validate explicit state transitions
    CASE UPPER(:OLD.Status)
        WHEN 'APPLIED' THEN
            IF UPPER(:NEW.Status) != 'SHORTLISTED' THEN
                RAISE_APPLICATION_ERROR(-20051, 'Invalid state transition: Cannot transition application ' || :NEW.Application_Id || ' from APPLIED to ' || :NEW.Status || '. Candidate must be SHORTLISTED first.');
            END IF;

        WHEN 'SHORTLISTED' THEN
            IF UPPER(:NEW.Status) != 'INTERVIEWING' THEN
                RAISE_APPLICATION_ERROR(-20052, 'Invalid state transition: Cannot transition application ' || :NEW.Application_Id || ' from SHORTLISTED to ' || :NEW.Status || '. Candidate must begin INTERVIEWING first.');
            END IF;

        WHEN 'INTERVIEWING' THEN
            IF UPPER(:NEW.Status) != 'SELECTED' THEN
                RAISE_APPLICATION_ERROR(-20053, 'Invalid state transition: Cannot transition application ' || :NEW.Application_Id || ' from INTERVIEWING to ' || :NEW.Status || '. Must transition to SELECTED after clearing all rounds.');
            END IF;

            -- Candidate must be fully ready (all configured rounds cleared)
            v_readiness := GET_APPLICATION_READINESS(:NEW.Application_Id, :NEW.Drive_Id);
            IF v_readiness != 'READY' THEN
                RAISE_APPLICATION_ERROR(-20050, 'Integrity Violation: Candidate cannot be marked as SELECTED: ' || v_readiness);
            END IF;

        WHEN 'SELECTED' THEN
            IF UPPER(:NEW.Status) != 'OFFERED' THEN
                RAISE_APPLICATION_ERROR(-20054, 'Invalid state transition: Cannot transition application ' || :NEW.Application_Id || ' from SELECTED to ' || :NEW.Status || '. Valid transition is to OFFERED.');
            END IF;

        WHEN 'OFFERED' THEN
            IF UPPER(:NEW.Status) NOT IN ('ACCEPTED', 'DECLINED') THEN
                RAISE_APPLICATION_ERROR(-20056, 'Invalid state transition: Cannot transition application ' || :NEW.Application_Id || ' from OFFERED to ' || :NEW.Status || '. Valid transitions are ACCEPTED or DECLINED.');
            END IF;

            IF UPPER(:NEW.Status) = 'ACCEPTED' THEN
                -- Verify an offer letter exists in OFFERED or ACCEPTED state
                SELECT COUNT(*) INTO v_offer_cnt
                FROM OFFER_LETTER
                WHERE Application_Id = :NEW.Application_Id
                  AND Status IN ('OFFERED', 'ACCEPTED');
                IF v_offer_cnt = 0 THEN
                    RAISE_APPLICATION_ERROR(-20057, 'Integrity Violation: Cannot transition application to ACCEPTED without a valid released OFFER_LETTER.');
                END IF;
            END IF;

        WHEN 'ACCEPTED' THEN
            RAISE_APPLICATION_ERROR(-20058, 'Invalid state transition: Application ' || :NEW.Application_Id || ' is in finalized state ACCEPTED and cannot be modified.');

        WHEN 'DECLINED' THEN
            RAISE_APPLICATION_ERROR(-20059, 'Invalid state transition: Application ' || :NEW.Application_Id || ' is in finalized state DECLINED and cannot be modified.');

        ELSE
            RAISE_APPLICATION_ERROR(-20060, 'Unknown current status: ' || :OLD.Status);
    END CASE;
END TRG_ENFORCE_APPLICATION_STATE_MACHINE;
/

-- ------------------------------------------------------------
-- 5. TRIGGER: TRG_VALIDATE_OFFER_ISSUANCE
--    Guarantees that an offer can only be inserted for an application
--    in SELECTED status with all mandatory interview rounds completed.
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_VALIDATE_OFFER_ISSUANCE
BEFORE INSERT ON OFFER_LETTER
FOR EACH ROW
DECLARE
    v_app_status VARCHAR2(30);
    v_readiness  VARCHAR2(500);
BEGIN
    SELECT Status INTO v_app_status
    FROM APPLICATION
    WHERE Application_Id = :NEW.Application_Id;

    IF UPPER(v_app_status) != 'SELECTED' THEN
        RAISE_APPLICATION_ERROR(-20053, 'Cannot issue offer: Application ' || :NEW.Application_Id || ' status is ' || v_app_status || ', but must be strictly SELECTED.');
    END IF;

    v_readiness := GET_APPLICATION_READINESS(:NEW.Application_Id);
    IF v_readiness != 'READY' THEN
        RAISE_APPLICATION_ERROR(-20054, 'Cannot issue offer: ' || v_readiness);
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20055, 'Cannot issue offer: Application ' || :NEW.Application_Id || ' not found.');
END TRG_VALIDATE_OFFER_ISSUANCE;
/
