-- ============================================================
-- V24__sql_execution_audit.sql
-- Oracle-backed SQL & PL/SQL Execution Audit Trail for Admin SQL Compiler
-- Stores authoritative execution records, timing, affected rows, and error states.
-- ============================================================

CREATE TABLE SQL_EXECUTION_AUDIT (
    Audit_Id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    Admin_Id          VARCHAR2(50) DEFAULT 'admin' NOT NULL,
    Executed_At       TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    Statement_Type    VARCHAR2(30) NOT NULL,
    SQL_Text          CLOB NOT NULL,
    Status            VARCHAR2(20) NOT NULL,
    Affected_Rows     NUMBER,
    Returned_Rows     NUMBER,
    Execution_Time_Ms NUMBER NOT NULL,
    Error_Code        VARCHAR2(50),
    Error_Message     VARCHAR2(1000)
);

CREATE INDEX IDX_SQL_AUDIT_TIME ON SQL_EXECUTION_AUDIT (Executed_At DESC);
CREATE INDEX IDX_SQL_AUDIT_ADMIN ON SQL_EXECUTION_AUDIT (Admin_Id);
