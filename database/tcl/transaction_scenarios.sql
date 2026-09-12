-- ============================================================
-- database/tcl/transaction_scenarios.sql
-- Oracle TCL (Transaction Control Language) Demonstration Scripts
-- Demonstrating COMMIT, SAVEPOINT, ROLLBACK, and ROLLBACK TO SAVEPOINT
-- ============================================================

-- ------------------------------------------------------------
-- Scenario 1: Successful Transaction with Multiple Operations & COMMIT
-- ------------------------------------------------------------
SET SERVEROUTPUT ON;

DECLARE
    v_test_student_id VARCHAR2(20) := 'STU_TCL_01';
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- SCENARIO 1: STARTING TRANSACTION ---');

    -- Insert student profile
    INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
    VALUES (v_test_student_id, 'TCL Test Student', 'tcl.student@univ.edu.in', 'Testing Lane', 'Pune', 'Maharashtra', TO_DATE('2003-01-01', 'YYYY-MM-DD'), 8.50);

    -- Insert phone
    INSERT INTO STUDENT_PHONE (Student_Id, Phone)
    VALUES (v_test_student_id, '+91-9999988881');

    -- Commit transaction to persist permanently
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Scenario 1: Transaction successfully committed.');
END;
/

-- ------------------------------------------------------------
-- Scenario 2: Savepoint and Partial Rollback (ROLLBACK TO SAVEPOINT)
-- ------------------------------------------------------------
DECLARE
    v_test_student_id VARCHAR2(20) := 'STU_TCL_02';
    v_app_count       NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- SCENARIO 2: DEMONSTRATING SAVEPOINT & PARTIAL ROLLBACK ---');

    -- Step 1: Insert valid student profile
    INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
    VALUES (v_test_student_id, 'Partial Rollback Student', 'partial.rollback@univ.edu.in', 'Audit Road', 'Bengaluru', 'Karnataka', TO_DATE('2003-05-15', 'YYYY-MM-DD'), 9.00);

    -- Create Savepoint A after student creation
    SAVEPOINT sp_after_student;
    DBMS_OUTPUT.PUT_LINE('Savepoint sp_after_student created.');

    -- Step 2: Attempt an invalid or conflicted operation inside transaction
    -- Create temporary registration
    INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date)
    VALUES (v_test_student_id, 'PROG01', SYSDATE);

    -- Create Savepoint B
    SAVEPOINT sp_after_prog1;

    -- Rollback only to savepoint B or A
    DBMS_OUTPUT.PUT_LINE('Rolling back to savepoint sp_after_student...');
    ROLLBACK TO SAVEPOINT sp_after_student;

    -- Verify that student still exists in current transaction, but registration was rolled back
    SELECT COUNT(*) INTO v_app_count FROM REGISTERS WHERE Student_Id = v_test_student_id;
    DBMS_OUTPUT.PUT_LINE('Registrations after rollback to savepoint: ' || v_app_count || ' (Expected: 0)');

    -- Clean up test student
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('Final full rollback executed to leave DB clean.');
END;
/

-- ------------------------------------------------------------
-- Scenario 3: Complete Transaction Failure and Full ROLLBACK
-- ------------------------------------------------------------
DECLARE
BEGIN
    DBMS_OUTPUT.PUT_LINE('--- SCENARIO 3: DEMONSTRATING FULL ROLLBACK ON ERROR ---');

    -- Begin speculative batch inserts
    INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
    VALUES ('STU_TEMP_FAIL', 'Temp Fail Student', 'fail@univ.edu.in', 'Error St', 'Delhi', 'Delhi', TO_DATE('2003-01-01', 'YYYY-MM-DD'), 7.00);

    -- Force an intentional rollback (simulating exception handling)
    ROLLBACK;

    DBMS_OUTPUT.PUT_LINE('Full rollback complete. No uncommitted records persisted.');
END;
/
