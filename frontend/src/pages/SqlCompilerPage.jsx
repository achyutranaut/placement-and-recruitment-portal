import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import SqlEditor from '../components/SqlEditor';
import {
  Terminal,
  Database,
  Play,
  Clock,
  Table as TableIcon,
  Layers,
  FileCode,
  Zap,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  ShieldAlert,
  History,
  BookOpen,
  ChevronRight,
  ChevronDown,
  RefreshCw,
  Download,
  Key,
  Link as LinkIcon,
  Radio,
  Search,
} from 'lucide-react';

const SAMPLE_QUERIES = [
  // 1. DQL (QUERIES)
  {
    category: 'DQL (Queries)',
    title: 'DQL — High CGPA Students',
    sql: `-- Select students meeting top-tier placement criteria (CGPA >= 8.5)
SELECT Student_Id, Name, Email, CGPA, Branch, Program_Id
FROM STUDENT
WHERE CGPA >= 8.50
ORDER BY CGPA DESC;`,
  },
  {
    category: 'DQL (Queries)',
    title: 'DQL — Active Placement Drives Catalog',
    sql: `-- View active campus drives with corporate partner names, eligibility, and packages
SELECT d.Drive_Id, ec.Company_Name, d.Job_Title, d.Min_CGPA, d.CTC AS Package_LPA, d.Openings, d.Application_Deadline
FROM PLACEMENT_DRIVE d
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
ORDER BY d.CTC DESC;`,
  },
  {
    category: 'DQL (Queries)',
    title: 'DQL — Distinct Branches & Placement Statuses',
    sql: `-- Demonstrates SELECT DISTINCT across participating placement departments
SELECT DISTINCT Branch, Placement_Status
FROM STUDENT
WHERE Branch IS NOT NULL
ORDER BY Branch;`,
  },

  // 2. JOINS
  {
    category: 'JOINS',
    title: 'JOIN — Multi-Table Relational Placement Journey',
    sql: `-- 5-table relational join linking candidate, application, drive, job role, and corporate partner
SELECT s.Student_Id, s.Name AS Candidate, s.CGPA,
       a.Application_Id, a.Status AS App_Status,
       d.Drive_Id, d.Job_Title, d.CTC AS Package_LPA,
       ec.Company_Name, c.Industry
FROM STUDENT s
JOIN APPLICATION a ON s.Student_Id = a.Student_Id
JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
ORDER BY a.Apply_Date DESC;`,
  },
  {
    category: 'JOINS',
    title: 'JOIN — Left Outer Join for Drive Application Coverage',
    sql: `-- Left Outer Join displaying all campus drives even if no candidate applications have been filed
SELECT d.Drive_Id, d.Job_Title, d.CTC AS Package_LPA,
       a.Application_Id, a.Student_Id, a.Status
FROM PLACEMENT_DRIVE d
LEFT JOIN APPLICATION a ON d.Drive_Id = a.Drive_Id
ORDER BY d.Drive_Id, a.Application_Id;`,
  },

  // 3. AGGREGATION
  {
    category: 'AGGREGATION',
    title: 'AGGREGATION — Academic Metrics by Branch (GROUP BY & HAVING)',
    sql: `-- Aggregates candidate count, average, minimum, and maximum CGPA per branch with HAVING filter
SELECT Branch,
       COUNT(*) AS Total_Students,
       ROUND(AVG(CGPA), 2) AS Average_CGPA,
       MIN(CGPA) AS Min_CGPA,
       MAX(CGPA) AS Max_CGPA
FROM STUDENT
WHERE Branch IS NOT NULL
GROUP BY Branch
HAVING COUNT(*) >= 1
ORDER BY Average_CGPA DESC;`,
  },
  {
    category: 'AGGREGATION',
    title: 'AGGREGATION — Recruitment Intake & Compensation Statistics',
    sql: `-- Summarizes recruitment capacity and compensation metrics across job titles
SELECT d.Job_Title,
       COUNT(*) AS Drive_Count,
       SUM(d.Openings) AS Total_Openings,
       ROUND(AVG(d.CTC), 2) AS Average_CTC,
       MAX(d.CTC) AS Peak_CTC
FROM PLACEMENT_DRIVE d
GROUP BY d.Job_Title
HAVING AVG(d.CTC) >= 10.00
ORDER BY Average_CTC DESC;`,
  },

  // 4. SUBQUERIES
  {
    category: 'SUBQUERIES',
    title: 'SUBQUERY — Candidates with Active Applications (EXISTS)',
    sql: `-- Correlated subquery selecting students who have applied for at least one placement drive
SELECT s.Student_Id, s.Name, s.Email, s.CGPA
FROM STUDENT s
WHERE EXISTS (
    SELECT 1
    FROM APPLICATION a
    WHERE a.Student_Id = s.Student_Id
);`,
  },
  {
    category: 'SUBQUERIES',
    title: 'SUBQUERY — High Intake Recruitment Drives (IN)',
    sql: `-- Nested scalar subquery isolating placement drives offering above-average intake openings
SELECT Drive_Id, Job_Title, Min_CGPA, CTC AS Package_LPA, Openings
FROM PLACEMENT_DRIVE
WHERE Drive_Id IN (
    SELECT Drive_Id
    FROM PLACEMENT_DRIVE
    WHERE Openings >= (SELECT AVG(Openings) FROM PLACEMENT_DRIVE)
)
ORDER BY Openings DESC;`,
  },
  {
    category: 'SUBQUERIES',
    title: 'SUBQUERY — Students Above Branch Average CGPA (Correlated)',
    sql: `-- Correlated subquery evaluating each student's CGPA against their branch average
SELECT s.Student_Id, s.Name, s.Branch, s.CGPA
FROM STUDENT s
WHERE s.CGPA >= (
    SELECT AVG(s2.CGPA)
    FROM STUDENT s2
    WHERE s2.Branch = s.Branch
)
ORDER BY s.Branch, s.CGPA DESC;`,
  },

  // 5. SET OPERATIONS
  {
    category: 'SET OPERATIONS',
    title: 'SET OPERATIONS — Candidate Cohort Union (UNION)',
    sql: `-- Combines distinct candidate subsets: high-merit students and B.Tech CSE candidates
SELECT Student_Id, Name, 'High Merit' AS Cohort
FROM STUDENT
WHERE CGPA >= 9.00
UNION
SELECT Student_Id, Name, 'B.Tech CSE' AS Cohort
FROM STUDENT
WHERE Program_Id = 'BTECH-CSE'
ORDER BY Student_Id;`,
  },
  {
    category: 'SET OPERATIONS',
    title: 'SET OPERATIONS — High Merit Applicants Intersection (INTERSECT)',
    sql: `-- Identifies students who achieve high academic merit AND have submitted placement applications
SELECT Student_Id FROM STUDENT WHERE CGPA >= 8.50
INTERSECT
SELECT Student_Id FROM APPLICATION;`,
  },
  {
    category: 'SET OPERATIONS',
    title: 'SET OPERATIONS — Unapplied Candidates Difference (MINUS)',
    sql: `-- Computes difference set between all registered students and candidates who applied
SELECT Student_Id, Name FROM STUDENT
MINUS
SELECT s.Student_Id, s.Name FROM STUDENT s JOIN APPLICATION a ON s.Student_Id = a.Student_Id;`,
  },

  // 6. DML (SAFE DEMONSTRATION)
  {
    category: 'DML',
    title: 'DML — Safe Demonstration Student Lifecycle (INSERT/SELECT/UPDATE/DELETE)',
    sql: `-- Safe Demonstration DML — Strict NOT-NULL Guard Compliant (Mode: SCRIPT)
-- 1. Insert test candidate providing all mandatory NOT NULL columns without default
INSERT INTO STUDENT (
    Student_Id, Name, Email, Street, City, State, DOB, CGPA, Branch, Registration_No, Program_Id
) VALUES (
    'STU_DEMO_999',
    'Arun Kumar',
    'arun.kumar2026@vitstudent.ac.in',
    '14 Kelambakkam Highway',
    'Chennai',
    'Tamil Nadu',
    TO_DATE('2004-05-15', 'YYYY-MM-DD'),
    8.75,
    'Computer Science and Engineering',
    '25BCE9999',
    'BTECH-CSE'
);

-- 2. Verify inserted candidate in Oracle
SELECT Student_Id, Name, Email, CGPA, Branch, Program_Id
FROM STUDENT
WHERE Student_Id = 'STU_DEMO_999';

-- 3. Safe update respecting CHECK constraints and non-null rules
UPDATE STUDENT
SET CGPA = 9.10
WHERE Student_Id = 'STU_DEMO_999';

-- 4. Clean up demonstration record to preserve database integrity
DELETE FROM STUDENT
WHERE Student_Id = 'STU_DEMO_999';

COMMIT;`,
  },
  {
    category: 'DML',
    title: 'DML — Safe Single-Record Drive Capacity UPDATE',
    sql: `-- Safe Demonstration DML — Updates recruitment intake capacity for an active drive
UPDATE PLACEMENT_DRIVE
SET Openings = 15
WHERE Drive_Id = 'DRV001';

-- Verify updated capacity
SELECT Drive_Id, Job_Title, CTC AS Package_LPA, Openings
FROM PLACEMENT_DRIVE
WHERE Drive_Id = 'DRV001';`,
  },

  // 7. DDL
  {
    category: 'DDL',
    title: 'DDL — Safe Demonstration Table Lifecycle (CREATE/INSPECT/DROP)',
    sql: `-- Safe Demonstration DDL (Mode: SCRIPT)
-- 1. Create demonstration table in Oracle schema
CREATE TABLE DEMO_INTERVIEW_NOTE (
    Note_Id    NUMBER PRIMARY KEY,
    Note_Title VARCHAR2(100) NOT NULL,
    Drive_Id   VARCHAR2(20) NOT NULL,
    Created_At DATE DEFAULT SYSDATE NOT NULL
);

-- 2. Inspect created table metadata from USER_TAB_COLUMNS
SELECT Column_Name, Data_Type, Data_Length, Nullable
FROM USER_TAB_COLUMNS
WHERE Table_Name = 'DEMO_INTERVIEW_NOTE'
ORDER BY Column_Id;

-- 3. Cleanly drop demonstration table
DROP TABLE DEMO_INTERVIEW_NOTE;`,
  },

  // 8. TCL
  {
    category: 'TCL',
    title: 'TCL — Transaction SAVEPOINT & ROLLBACK Scenario',
    sql: `-- Transaction Control Language Demonstration (Mode: SCRIPT)
SAVEPOINT demo_point;

-- 1. Attempt in-flight update on demonstration candidate
UPDATE STUDENT
SET CGPA = 9.50
WHERE Student_Id = 'STU024';

-- 2. View modified in-flight row state
SELECT Student_Id, Name, CGPA
FROM STUDENT
WHERE Student_Id = 'STU024';

-- 3. Rollback uncommitted changes back to savepoint
ROLLBACK TO demo_point;

-- 4. Verify original CGPA restored cleanly
SELECT Student_Id, Name, CGPA
FROM STUDENT
WHERE Student_Id = 'STU024';`,
  },

  // 9. PL/SQL
  {
    category: 'PL/SQL',
    title: 'PL/SQL — Placement Statistics & Drive Health Evaluation',
    sql: `-- Anonymous PL/SQL Block with variables, SELECT INTO, conditional logic, and DBMS_OUTPUT
DECLARE
    v_total_students   NUMBER := 0;
    v_total_drives     NUMBER := 0;
    v_placed_students  NUMBER := 0;
    v_avg_ctc          NUMBER := 0;
    v_placement_rate   NUMBER := 0;
    v_health_status    VARCHAR2(50);
BEGIN
    SELECT COUNT(*) INTO v_total_students FROM STUDENT;
    SELECT COUNT(*) INTO v_total_drives FROM PLACEMENT_DRIVE;
    SELECT COUNT(*) INTO v_placed_students FROM STUDENT WHERE Placement_Status = 'PLACED';
    SELECT NVL(ROUND(AVG(CTC), 2), 0) INTO v_avg_ctc FROM PLACEMENT_DRIVE;

    IF v_total_students > 0 THEN
        v_placement_rate := ROUND((v_placed_students / v_total_students) * 100, 2);
    ELSE
        v_placement_rate := 0;
    END IF;

    IF v_placement_rate >= 50.0 THEN
        v_health_status := 'OPTIMAL RECRUITMENT METRIC';
    ELSE
        v_health_status := 'ONGOING RECRUITMENT CYCLE';
    END IF;

    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('   VIT PLACEMENT & RECRUITMENT CELL — ORACLE ENGINE');
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('Total Registered Students : ' || v_total_students);
    DBMS_OUTPUT.PUT_LINE('Active Recruitment Drives : ' || v_total_drives);
    DBMS_OUTPUT.PUT_LINE('Placed Candidates         : ' || v_placed_students);
    DBMS_OUTPUT.PUT_LINE('Average Drive CTC (LPA)   : ' || v_avg_ctc || ' LPA');
    DBMS_OUTPUT.PUT_LINE('Placement Success Rate    : ' || v_placement_rate || '%');
    DBMS_OUTPUT.PUT_LINE('Recruitment Campaign State: ' || v_health_status);
    DBMS_OUTPUT.PUT_LINE('==================================================');
END;
/`,
  },
  {
    category: 'PL/SQL',
    title: 'PL/SQL — Explicit Cursor Roster Traversal',
    sql: `-- Explicit Cursor Traversal with %ROWTYPE, %NOTFOUND, %ROWCOUNT, and DBMS_OUTPUT
DECLARE
    CURSOR c_students IS
        SELECT Student_Id, Name, CGPA, Branch, Placement_Status
        FROM STUDENT
        ORDER BY CGPA DESC;
    v_stu c_students%ROWTYPE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('         STUDENT MERIT ROSTER (CURSOR LOOP)       ');
    DBMS_OUTPUT.PUT_LINE('==================================================');
    OPEN c_students;
    LOOP
        FETCH c_students INTO v_stu;
        EXIT WHEN c_students%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(' • [' || v_stu.Student_Id || '] ' || RPAD(v_stu.Name, 20) ||
                             ' | CGPA: ' || TO_CHAR(v_stu.CGPA, 'FM90.00') ||
                             ' | Status: ' || v_stu.Placement_Status);
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('--------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('Total Processed Candidates: ' || c_students%ROWCOUNT);
    DBMS_OUTPUT.PUT_LINE('==================================================');
    CLOSE c_students;
END;
/`,
  },
  {
    category: 'PL/SQL',
    title: 'PL/SQL — Invocation of Stored Function (GET_AVERAGE_PACKAGE)',
    sql: `-- Invocation of real Oracle stored function GET_AVERAGE_PACKAGE with DBMS_OUTPUT logging
DECLARE
    v_it_avg_ctc     NUMBER;
    v_global_avg_ctc NUMBER;
BEGIN
    v_it_avg_ctc := GET_AVERAGE_PACKAGE('IT Services & Consulting');
    v_global_avg_ctc := GET_AVERAGE_PACKAGE(NULL);

    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('       ORACLE STORED FUNCTION ANALYTICS           ');
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('IT Services Average CTC : ' || v_it_avg_ctc || ' LPA');
    DBMS_OUTPUT.PUT_LINE('All Sectors Average CTC : ' || v_global_avg_ctc || ' LPA');
    DBMS_OUTPUT.PUT_LINE('==================================================');
END;
/`,
  },
];

export default function SqlCompilerPage() {
  const [sql, setSql] = useState('SELECT * FROM STUDENT;');
  const [mode, setMode] = useState('QUERY');
  const [strictMode, setStrictMode] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [result, setResult] = useState(null);
  const [selectedStatementIdx, setSelectedStatementIdx] = useState('all'); // 'all' or number (0, 1, 2...)

  // Database Connection Info
  const [connInfo, setConnInfo] = useState(null);

  // Schema Explorer
  const [schema, setSchema] = useState(null);
  const [schemaLoading, setSchemaLoading] = useState(false);
  const [schemaFilter, setSchemaFilter] = useState('');
  const [expandedTables, setExpandedTables] = useState({});
  const [activeSchemaTab, setActiveSchemaTab] = useState('tables'); // 'tables', 'views', 'procedures', 'triggers'

  // Oracle-backed Query Execution Audit History
  const [oracleHistory, setOracleHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState(null);
  const [historyFilter, setHistoryFilter] = useState('');
  const [historyStatusFilter, setHistoryStatusFilter] = useState('ALL'); // 'ALL', 'SUCCESS', 'ERROR'

  // Active Sidebar View
  const [sidebarView, setSidebarView] = useState('schema'); // 'schema', 'samples', 'history'

  // Load connection info, schema metadata, and audit history on mount
  useEffect(() => {
    loadConnectionInfo();
    loadSchema();
    loadOracleHistory();
  }, []);

  const loadConnectionInfo = async () => {
    try {
      const data = await api.getSqlConnectionInfo();
      setConnInfo(data);
    } catch (err) {
      console.error('Failed to load connection info:', err);
    }
  };

  const loadSchema = async () => {
    try {
      setSchemaLoading(true);
      const data = await api.getSqlSchema();
      setSchema(data);
    } catch (err) {
      console.error('Failed to load schema metadata:', err);
    } finally {
      setSchemaLoading(false);
    }
  };

  const loadOracleHistory = async () => {
    try {
      setHistoryLoading(true);
      setHistoryError(null);
      const data = await api.getSqlHistory(50);
      setOracleHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load Oracle execution history:', err);
      setHistoryError(err?.message || 'Failed to load Oracle execution history.');
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleQuickAdminLogin = async () => {
    try {
      setHistoryLoading(true);
      await api.login('admin', 'admin123');
      setHistoryError(null);
      await Promise.allSettled([loadOracleHistory(), loadSchema(), loadConnectionInfo()]);
    } catch (err) {
      setHistoryError('Failed to sign in as admin: ' + (err?.message || err));
    } finally {
      setHistoryLoading(false);
    }
  };

  const formatAuditTimestamp = (ts) => {
    if (!ts) return '';
    try {
      const d = new Date(ts);
      if (isNaN(d.getTime())) return String(ts).substring(11, 19);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch (_) {
      return '';
    }
  };

  const filteredHistory = useMemo(() => {
    return oracleHistory.filter((h) => {
      if (historyStatusFilter === 'SUCCESS' && h.status !== 'SUCCESS') return false;
      if (historyStatusFilter === 'ERROR' && h.status === 'SUCCESS') return false;
      if (!historyFilter.trim()) return true;
      const q = historyFilter.toLowerCase();
      return (
        (h.sqlText && h.sqlText.toLowerCase().includes(q)) ||
        (h.statementType && h.statementType.toLowerCase().includes(q)) ||
        (h.adminId && h.adminId.toLowerCase().includes(q)) ||
        (h.errorCode && h.errorCode.toLowerCase().includes(q))
      );
    });
  }, [oracleHistory, historyFilter, historyStatusFilter]);

  const handleRun = async (overrideSql) => {
    const textToRun = typeof overrideSql === 'string' && overrideSql.trim() ? overrideSql.trim() : sql.trim();
    if (!textToRun || executing) return;

    try {
      setExecuting(true);
      setResult(null);
      setSelectedStatementIdx('all');

      const res = await api.executeSql({ sql: textToRun, mode, strictMode });
      setResult(res);

      // Immediately refresh live execution history from Oracle
      loadOracleHistory();

      // Dispatch global mutation event so Student, Recruiter, and Admin portals reflect any database changes immediately
      if (res.success && ['INSERT', 'UPDATE', 'DELETE', 'MERGE'].includes(res.statementType?.toUpperCase())) {
        window.dispatchEvent(new CustomEvent('portal:database-mutation', {
          detail: { statementType: res.statementType, sql: textToRun, timestamp: Date.now() }
        }));
        try {
          const channel = new BroadcastChannel('portal-database-channel');
          channel.postMessage({ statementType: res.statementType, sql: textToRun, timestamp: Date.now() });
          channel.close();
        } catch (_) {}
      }

      // If DDL was run, refresh schema metadata
      const upper = textToRun.toUpperCase();
      if (
        upper.includes('CREATE ') ||
        upper.includes('DROP ') ||
        upper.includes('ALTER ') ||
        upper.includes('TRUNCATE ')
      ) {
        loadSchema();
      }
    } catch (err) {
      const isAuthError = err.message && (err.message.includes('403') || err.message.includes('Access Denied') || err.message.includes('401'));
      setResult({
        success: false,
        statementType: 'ERROR',
        errorCode: isAuthError ? 'ORA-403-ACCESS-DENIED' : 'ORA-CLIENT',
        errorMessage: err.message || 'Execution failed',
        fullError: isAuthError
          ? `${err.message}\n\n[Action Required]: The SQL Compiler requires active Placement Admin privileges (ROLE_ADMIN). If you recently logged in with a Student or Recruiter account in another tab, your session token was updated. Please re-login with the Admin credentials (admin / admin123).`
          : (err.message || 'Network or execution error'),
        executionTimeMs: 0,
        columns: null,
        rows: null,
        isAuthError,
      });
    } finally {
      setExecuting(false);
      loadOracleHistory();
    }
  };

  const handleSelectTable = (tableName) => {
    setSql(`SELECT *\nFROM ${tableName};`);
    setMode('QUERY');
  };

  const toggleTableExpand = (tableName) => {
    setExpandedTables((prev) => ({
      ...prev,
      [tableName]: !prev[tableName],
    }));
  };

  const handleLoadSample = (sampleSql) => {
    setSql(sampleSql);
    const cleaned = sampleSql.replace(/--[^\n]*/g, '').trim();
    const isPlsql = /^\s*(DECLARE|BEGIN)/i.test(cleaned);
    if (isPlsql) {
      setMode('QUERY');
    } else if ((cleaned.match(/;/g) || []).length > 1) {
      setMode('SCRIPT');
    } else {
      setMode('QUERY');
    }
  };

  const exportDataAsCsv = (cols, rws, filenamePrefix = 'query_result') => {
    if (!cols || !rws) return;
    const header = cols.join(',');
    const rows = rws.map((row) =>
      row
        .map((cell) => {
          if (cell === null) return '';
          const str = String(cell);
          return str.includes(',') ? `"${str.replace(/"/g, '""')}"` : str;
        })
        .join(',')
    );
    const csvContent = [header, ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${filenamePrefix}_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportAsCsv = () => {
    if (!result || !result.columns || !result.rows) return;
    exportDataAsCsv(result.columns, result.rows, 'oracle_query_result');
  };

  // Filtered tables for schema explorer
  const filteredTables = (schema?.tables || []).filter((t) =>
    t.tableName.toLowerCase().includes(schemaFilter.toLowerCase())
  );
  const filteredViews = (schema?.views || []).filter((v) =>
    v.viewName.toLowerCase().includes(schemaFilter.toLowerCase())
  );
  const filteredProcedures = (schema?.procedures || []).filter((p) =>
    p.procedureName.toLowerCase().includes(schemaFilter.toLowerCase())
  );
  const filteredFunctions = (schema?.functions || []).filter((f) =>
    f.functionName.toLowerCase().includes(schemaFilter.toLowerCase())
  );
  const filteredTriggers = (schema?.triggers || []).filter((t) =>
    t.triggerName.toLowerCase().includes(schemaFilter.toLowerCase())
  );

  // Helper to render interactive relational data tables
  const renderDataTable = (columns, rows, tableId = 'query_result') => {
    if (!columns || columns.length === 0) return null;
    return (
      <div className="space-y-2 flex-1">
        <div className="flex items-center justify-between text-xs">
          <span className="font-bold text-slate-800 font-mono">
            Query Result Set ({rows ? rows.length : 0} rows)
          </span>
          <button
            type="button"
            onClick={() => exportDataAsCsv(columns, rows, tableId)}
            className="flex items-center gap-1 px-2.5 py-0.5 rounded bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold text-[11px] shadow-2xs transition-colors"
            title="Download this result set as CSV"
          >
            <Download className="w-3 h-3" />
            <span>CSV</span>
          </button>
        </div>

        <div className="overflow-x-auto rounded-md border border-slate-200 max-h-[380px] overflow-y-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-100 text-slate-700 font-bold border-b border-slate-200 sticky top-0 z-10 shadow-2xs">
              <tr>
                <th className="px-3 py-2 text-slate-400 text-[10px] border-r border-slate-200 w-10 text-center">
                  #
                </th>
                {columns.map((col, idx) => (
                  <th key={idx} className="px-3.5 py-2 whitespace-nowrap text-slate-800 font-extrabold">
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-slate-800">
              {(rows || []).map((row, rIdx) => (
                <tr key={rIdx} className="hover:bg-blue-50/40 transition-colors">
                  <td className="px-3 py-1.5 text-center text-[10px] text-slate-400 bg-slate-50/50 border-r border-slate-200">
                    {rIdx + 1}
                  </td>
                  {row.map((cell, cIdx) => (
                    <td key={cIdx} className="px-3.5 py-1.5 whitespace-nowrap">
                      {cell === null || cell === undefined ? (
                        <span className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-400 font-mono text-[10px] font-semibold border border-slate-200">
                          NULL
                        </span>
                      ) : cell === '[BLOB]' ? (
                        <span className="px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 font-mono text-[10px] font-bold border border-blue-200">
                          [BLOB]
                        </span>
                      ) : (
                        <span>{String(cell)}</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
              {(!rows || rows.length === 0) && (
                <tr>
                  <td
                    colSpan={columns.length + 1}
                    className="text-center py-6 text-slate-400 font-sans text-xs"
                  >
                    Query executed successfully. 0 rows returned.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-slate-900 text-white shadow-sm">
              <Terminal className="w-5 h-5 text-emerald-400" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
                Oracle SQL & PL/SQL Compiler
                <span className="text-xs px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-mono font-semibold border border-emerald-200">
                  DA2 Environment
                </span>
              </h1>
              <p className="text-xs text-slate-500 font-medium">
                Authoritative query & procedural code execution engine operating on real local Oracle Database instance
              </p>
            </div>
          </div>
        </div>

        {/* Live Oracle Connection Status Badge */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border bg-white border-slate-200 shadow-sm text-xs">
            <Radio className="w-3.5 h-3.5 text-emerald-500 animate-pulse" />
            <div>
              <div className="font-bold text-slate-800 flex items-center gap-1.5">
                <span>Oracle Database 23c Free</span>
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                <span className="text-[10px] text-emerald-700 font-semibold uppercase">Connected</span>
              </div>
              <div className="text-[11px] text-slate-500 font-mono">
                Schema: <strong className="text-slate-700">{connInfo?.user || 'C##PLACEMENT_ADMIN'}</strong> •{' '}
                {connInfo?.host || 'localhost'}:{connInfo?.port || 1521}/{connInfo?.serviceName || 'FREEPDB1'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Grid: Left Sidebar + Center Workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* LEFT SIDEBAR (Cols 4) */}
        <div className="lg:col-span-4 bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-[640px]">
          {/* Sidebar Nav Tabs */}
          <div className="flex border-b border-slate-200 bg-slate-50 text-xs font-semibold">
            <button
              type="button"
              onClick={() => setSidebarView('schema')}
              className={`flex-1 py-2.5 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
                sidebarView === 'schema'
                  ? 'border-slate-900 text-slate-900 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Database className="w-3.5 h-3.5" />
              <span>Schema ({schema?.tables?.length || 0})</span>
            </button>
            <button
              type="button"
              onClick={() => setSidebarView('samples')}
              className={`flex-1 py-2.5 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
                sidebarView === 'samples'
                  ? 'border-slate-900 text-slate-900 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <BookOpen className="w-3.5 h-3.5" />
              <span>Samples ({SAMPLE_QUERIES.length})</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setSidebarView('history');
                loadOracleHistory();
              }}
              className={`flex-1 py-2.5 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
                sidebarView === 'history'
                  ? 'border-slate-900 text-slate-900 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <History className="w-3.5 h-3.5" />
              <span>History ({oracleHistory.length})</span>
            </button>
          </div>

          {/* VIEW 1: SCHEMA EXPLORER */}
          {sidebarView === 'schema' && (
            <div className="flex-1 p-3 flex flex-col space-y-3 overflow-hidden">
              {/* Search & Refresh */}
              <div className="flex items-center gap-2">
                <div className="relative flex-1">
                  <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                  <input
                    type="text"
                    value={schemaFilter}
                    onChange={(e) => setSchemaFilter(e.target.value)}
                    placeholder="Filter tables, views..."
                    className="w-full pl-8 pr-2.5 py-1.5 text-xs rounded border border-slate-300 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
                <button
                  type="button"
                  onClick={loadSchema}
                  disabled={schemaLoading}
                  className="p-1.5 rounded border border-slate-300 hover:bg-slate-100 text-slate-600 transition-colors"
                  title="Refresh Oracle schema metadata"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${schemaLoading ? 'animate-spin' : ''}`} />
                </button>
              </div>

              {/* Subtabs for Schema Objects */}
              <div className="flex gap-1 text-[11px] font-semibold border-b border-slate-100 pb-1.5">
                {[
                  { id: 'tables', label: `Tables (${filteredTables.length})` },
                  { id: 'views', label: `Views (${filteredViews.length})` },
                  { id: 'procedures', label: `Proc (${filteredProcedures.length + filteredFunctions.length})` },
                  { id: 'triggers', label: `Trg (${filteredTriggers.length})` },
                ].map((st) => (
                  <button
                    key={st.id}
                    onClick={() => setActiveSchemaTab(st.id)}
                    className={`px-2 py-0.5 rounded transition-all ${
                      activeSchemaTab === st.id
                        ? 'bg-slate-900 text-white font-bold'
                        : 'text-slate-500 hover:text-slate-800'
                    }`}
                  >
                    {st.label}
                  </button>
                ))}
              </div>

              {/* Object List */}
              <div className="flex-1 overflow-y-auto space-y-1 pr-1 max-h-[520px]">
                {activeSchemaTab === 'tables' && (
                  <>
                    {filteredTables.map((t) => {
                      const isExpanded = !!expandedTables[t.tableName];
                      return (
                        <div key={t.tableName} className="border border-slate-100 rounded-md overflow-hidden text-xs">
                          <div className="flex items-center justify-between px-2.5 py-1.5 bg-slate-50/70 hover:bg-slate-100 transition-colors">
                            <button
                              type="button"
                              onClick={() => toggleTableExpand(t.tableName)}
                              className="flex items-center gap-1.5 text-left font-mono font-bold text-slate-800 flex-1 truncate"
                            >
                              {isExpanded ? (
                                <ChevronDown className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                              ) : (
                                <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                              )}
                              <span className="truncate">{t.tableName}</span>
                              <span className="text-[10px] text-slate-400 font-sans font-normal">
                                ({t.columns.length})
                              </span>
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSelectTable(t.tableName)}
                              className="px-2 py-0.5 rounded text-[10px] font-bold font-mono bg-white border border-slate-200 text-blue-700 hover:bg-blue-50 transition-colors shadow-2xs shrink-0"
                              title={`Generate SELECT * FROM ${t.tableName}`}
                            >
                              SELECT *
                            </button>
                          </div>

                          {/* Expanded Columns Table */}
                          {isExpanded && (
                            <div className="p-2 bg-white border-t border-slate-100 overflow-x-auto">
                              <table className="w-full text-[11px] font-mono">
                                <thead>
                                  <tr className="text-slate-400 border-b border-slate-100 text-left">
                                    <th className="pb-1 font-semibold">Column</th>
                                    <th className="pb-1 font-semibold">Type</th>
                                    <th className="pb-1 font-semibold">Nullable</th>
                                    <th className="pb-1 font-semibold">Key/Attrs</th>
                                    <th className="pb-1 font-semibold">Default</th>
                                  </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50">
                                  {t.columns.map((col) => (
                                    <tr key={col.columnName} className="hover:bg-slate-50/80">
                                      <td className="py-1 text-slate-800 font-semibold pr-2">
                                        {col.columnName}
                                      </td>
                                      <td className="py-1 text-slate-500 pr-2">
                                        {col.dataType}
                                        {col.dataLength ? `(${col.dataLength})` : ''}
                                      </td>
                                      <td className="py-1 pr-2">
                                        {col.nullable === false ? (
                                          <span className="px-1.5 py-0.5 rounded bg-rose-50 text-rose-700 border border-rose-200 text-[9px] font-extrabold" title="Mandatory NOT NULL Column">
                                            NOT NULL
                                          </span>
                                        ) : (
                                          <span className="px-1.5 py-0.5 rounded bg-slate-50 text-slate-500 border border-slate-200 text-[9px] font-medium" title="Nullable Column">
                                            NULLABLE
                                          </span>
                                        )}
                                      </td>
                                      <td className="py-1 pr-2">
                                        {col.primaryKey && (
                                          <span className="px-1 py-0.2 rounded bg-amber-50 text-amber-700 border border-amber-200 text-[9px] font-bold mr-1">
                                            PK
                                          </span>
                                        )}
                                        {col.foreignKeyRef && (
                                          <span
                                            className="px-1 py-0.2 rounded bg-blue-50 text-blue-700 border border-blue-200 text-[9px] font-medium mr-1"
                                            title={`References ${col.foreignKeyRef}`}
                                          >
                                            FK
                                          </span>
                                        )}
                                        {col.isUnique && (
                                          <span className="px-1 py-0.2 rounded bg-purple-50 text-purple-700 border border-purple-200 text-[9px] font-bold mr-1">
                                            UQ
                                          </span>
                                        )}
                                      </td>
                                      <td className="py-1 text-slate-500 text-[10px] truncate max-w-[80px]" title={col.dataDefault}>
                                        {col.dataDefault || '—'}
                                      </td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>

                              {/* Unique & Check Constraints */}
                              {((t.checkConstraints && t.checkConstraints.length > 0) || (t.uniqueConstraints && t.uniqueConstraints.length > 0)) && (
                                <div className="mt-2 pt-2 border-t border-slate-100 space-y-1 text-[10px] font-mono">
                                  {t.checkConstraints && t.checkConstraints.map((cc, idx) => (
                                    <div key={idx} className="text-slate-600 bg-amber-50/50 p-1 rounded border border-amber-100 flex items-start gap-1">
                                      <span className="font-bold text-amber-700 shrink-0">CHECK:</span>
                                      <span className="truncate">{cc.searchCondition || cc.constraintName}</span>
                                    </div>
                                  ))}
                                  {t.uniqueConstraints && t.uniqueConstraints.map((uc, idx) => (
                                    <div key={idx} className="text-slate-600 bg-purple-50/50 p-1 rounded border border-purple-100 flex items-start gap-1">
                                      <span className="font-bold text-purple-700 shrink-0">UNIQUE:</span>
                                      <span className="truncate">{uc.constraintName} ({(uc.columns || []).join(', ')})</span>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                    {filteredTables.length === 0 && (
                      <div className="text-center py-8 text-xs text-slate-400">
                        No matching tables found.
                      </div>
                    )}
                  </>
                )}

                {activeSchemaTab === 'views' && (
                  <div className="space-y-1">
                    {filteredViews.map((v) => (
                      <div
                        key={v.viewName}
                        className="flex items-center justify-between p-2 rounded bg-slate-50 hover:bg-slate-100 text-xs border border-slate-100"
                      >
                        <div className="font-mono font-semibold text-slate-800 flex items-center gap-1.5">
                          <Layers className="w-3.5 h-3.5 text-indigo-500" />
                          <span>{v.viewName}</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleSelectTable(v.viewName)}
                          className="px-2 py-0.5 rounded text-[10px] font-bold font-mono bg-white border border-slate-200 text-blue-700 hover:bg-blue-50"
                        >
                          SELECT *
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {activeSchemaTab === 'procedures' && (
                  <div className="space-y-2">
                    <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1">
                      Stored Procedures
                    </div>
                    {filteredProcedures.map((p) => (
                      <div
                        key={p.procedureName}
                        onClick={() =>
                          setSql(`-- Execute Stored Procedure: ${p.procedureName}\nBEGIN\n    ${p.procedureName};\nEND;\n/`)
                        }
                        className="p-2 rounded bg-slate-50 hover:bg-slate-100 cursor-pointer text-xs border border-slate-100 flex items-center justify-between"
                      >
                        <span className="font-mono font-semibold text-slate-800">{p.procedureName}</span>
                        <span className="text-[10px] px-1.5 rounded bg-emerald-50 text-emerald-700 font-bold border border-emerald-200">
                          {p.status}
                        </span>
                      </div>
                    ))}

                    <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-1 pt-2">
                      PL/SQL Functions
                    </div>
                    {filteredFunctions.map((f) => (
                      <div
                        key={f.functionName}
                        onClick={() =>
                          setSql(`-- Call PL/SQL Function: ${f.functionName}\nSELECT ${f.functionName}() FROM DUAL;`)
                        }
                        className="p-2 rounded bg-slate-50 hover:bg-slate-100 cursor-pointer text-xs border border-slate-100 flex items-center justify-between"
                      >
                        <span className="font-mono font-semibold text-slate-800">{f.functionName}</span>
                        <span className="text-[10px] px-1.5 rounded bg-emerald-50 text-emerald-700 font-bold border border-emerald-200">
                          {f.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                {activeSchemaTab === 'triggers' && (
                  <div className="space-y-1">
                    {filteredTriggers.map((t) => (
                      <div key={t.triggerName} className="p-2 rounded bg-slate-50 text-xs border border-slate-100 space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-mono font-bold text-purple-800">{t.triggerName}</span>
                          <span className="text-[10px] px-1.5 rounded bg-purple-50 text-purple-700 font-bold border border-purple-200">
                            {t.status}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500 font-mono">
                          Table: <strong className="text-slate-700">{t.tableName}</strong> • Event: {t.triggeringEvent}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* VIEW 2: SAMPLE QUERIES LIBRARY */}
          {sidebarView === 'samples' && (
            <div className="flex-1 p-3 space-y-2 overflow-y-auto max-h-[580px]">
              <div className="text-[11px] text-slate-500 font-medium px-1 pb-1">
                Click any standard DBMS DA2 query or PL/SQL snippet to load it into the editor:
              </div>
              {SAMPLE_QUERIES.map((s, idx) => (
                <div
                  key={idx}
                  onClick={() => handleLoadSample(s.sql)}
                  className="p-2.5 rounded-lg border border-slate-200 hover:border-blue-400 bg-white hover:bg-blue-50/50 cursor-pointer transition-all space-y-1 group"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-slate-100 text-slate-700">
                      {s.category}
                    </span>
                    <span className="text-[10px] text-blue-600 font-semibold opacity-0 group-hover:opacity-100 transition-opacity">
                      Load ➔
                    </span>
                  </div>
                  <div className="font-bold text-xs text-slate-900">{s.title}</div>
                  <pre className="text-[10px] text-slate-500 font-mono line-clamp-2 overflow-hidden bg-slate-50 p-1.5 rounded">
                    {s.sql}
                  </pre>
                </div>
              ))}
            </div>
          )}

          {/* VIEW 3: ORACLE QUERY AUDIT HISTORY */}
          {sidebarView === 'history' && (
            <div className="flex-1 p-3 space-y-2.5 overflow-y-auto max-h-[580px] flex flex-col">
              <div className="flex items-center justify-between text-[11px] text-slate-500 font-medium px-1 pb-1 border-b border-slate-100">
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-slate-700">Oracle Execution Audit</span>
                  <span className="px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 text-[10px] font-mono font-bold">
                    {oracleHistory.length}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={loadOracleHistory}
                  disabled={historyLoading}
                  className="flex items-center gap-1 text-slate-600 hover:text-slate-900 text-[10px] font-semibold transition-colors"
                  title="Refresh live from Oracle SQL_EXECUTION_AUDIT table"
                >
                  <RefreshCw className={`w-3 h-3 ${historyLoading ? 'animate-spin' : ''}`} />
                  <span>Refresh</span>
                </button>
              </div>

              {/* Error banner if history load failed */}
              {historyError && (
                <div className="p-2.5 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-800 space-y-2">
                  <div className="flex items-start gap-2">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <div className="text-[11px] font-medium leading-4">
                      {historyError}
                    </div>
                  </div>
                  {(historyError.includes('403') || historyError.includes('401') || historyError.includes('Access Denied')) && (
                    <div className="flex items-center gap-2 pt-1">
                      <button
                        type="button"
                        onClick={handleQuickAdminLogin}
                        disabled={historyLoading}
                        className="px-2.5 py-1 rounded bg-slate-900 hover:bg-slate-800 text-white text-[10px] font-semibold transition-colors cursor-pointer"
                      >
                        Sign in as Admin (admin123)
                      </button>
                      <Link
                        to="/login"
                        className="text-[10px] text-blue-600 underline font-semibold"
                      >
                        Login Page
                      </Link>
                    </div>
                  )}
                </div>
              )}

              {/* Filter and Search controls */}
              {oracleHistory.length > 0 && (
                <div className="space-y-1.5">
                  <div className="relative">
                    <Search className="w-3 h-3 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <input
                      type="text"
                      value={historyFilter}
                      onChange={(e) => setHistoryFilter(e.target.value)}
                      placeholder="Search query, admin, statement type..."
                      className="w-full text-xs pl-7 pr-2.5 py-1.5 rounded-md border border-slate-200 focus:outline-none focus:ring-1 focus:ring-slate-400 bg-slate-50 placeholder:text-slate-400"
                    />
                  </div>
                  <div className="flex items-center gap-1 text-[10px]">
                    {['ALL', 'SUCCESS', 'ERROR'].map((filterMode) => (
                      <button
                        key={filterMode}
                        type="button"
                        onClick={() => setHistoryStatusFilter(filterMode)}
                        className={`px-2 py-0.5 rounded font-semibold transition-colors cursor-pointer ${
                          historyStatusFilter === filterMode
                            ? 'bg-slate-900 text-white'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                        }`}
                      >
                        {filterMode === 'ALL'
                          ? `All (${oracleHistory.length})`
                          : filterMode === 'SUCCESS'
                          ? `Success (${oracleHistory.filter((h) => h.status === 'SUCCESS').length})`
                          : `Errors (${oracleHistory.filter((h) => h.status !== 'SUCCESS').length})`}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* History Cards */}
              <div className="space-y-2 flex-1 overflow-y-auto">
                {filteredHistory.map((h) => (
                  <div
                    key={h.auditId}
                    onClick={() => {
                      setSql(h.sqlText);
                      if (h.statementType === 'PLSQL' || h.statementType === 'SCRIPT') {
                        setMode(h.statementType);
                      } else {
                        setMode('QUERY');
                      }
                    }}
                    className="p-2.5 rounded-lg border border-slate-200 hover:border-blue-400 bg-white hover:bg-blue-50/40 cursor-pointer transition-all space-y-1.5 group shadow-2xs"
                    title="Click to load SQL into editor"
                  >
                    <div className="flex items-center justify-between text-[10px]">
                      <div className="flex items-center gap-1.5 font-mono">
                        <span className="font-bold text-slate-500">#{h.auditId}</span>
                        <span
                          className={`font-mono font-bold px-1.5 py-0.2 rounded text-[9px] ${
                            h.status === 'SUCCESS'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : h.status === 'BLOCKED'
                              ? 'bg-amber-50 text-amber-800 border border-amber-200'
                              : 'bg-rose-50 text-rose-700 border border-rose-200'
                          }`}
                        >
                          {h.status}
                        </span>
                        <span className="px-1 py-0.2 rounded bg-slate-100 text-slate-700 text-[9px] font-semibold">
                          {h.statementType}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-slate-400 font-mono text-[10px]">
                          {h.executionTimeMs}ms
                        </span>
                        <span className="text-[10px] text-blue-600 font-semibold opacity-0 group-hover:opacity-100 transition-opacity">
                          Load ➔
                        </span>
                      </div>
                    </div>

                    <pre className="text-[11px] text-slate-800 font-mono line-clamp-2 overflow-hidden bg-slate-50 p-1.5 rounded border border-slate-100">
                      {h.sqlText}
                    </pre>

                    <div className="flex items-center justify-between text-[10px] text-slate-500 font-mono">
                      <span>
                        Admin: <strong className="text-slate-700">{h.adminId}</strong>
                        {h.executedAt && (
                          <span className="text-slate-400 font-sans ml-1.5">
                            • {formatAuditTimestamp(h.executedAt)}
                          </span>
                        )}
                      </span>
                      <span>
                        {h.affectedRows !== null && h.affectedRows !== undefined && h.affectedRows > 0
                          ? `Aff: ${h.affectedRows}`
                          : h.returnedRows !== null && h.returnedRows !== undefined
                          ? `Rows: ${h.returnedRows}`
                          : ''}
                      </span>
                    </div>

                    {h.errorCode && (
                      <div className="text-[10px] text-rose-600 font-mono bg-rose-50/60 p-1 rounded border border-rose-100 break-all">
                        {h.errorCode}: {h.errorMessage}
                      </div>
                    )}
                  </div>
                ))}

                {oracleHistory.length === 0 && !historyError && (
                  <div className="text-center py-12 text-xs text-slate-400">
                    {historyLoading ? 'Fetching Oracle execution history...' : 'No audit entries logged yet.'}
                  </div>
                )}

                {oracleHistory.length > 0 && filteredHistory.length === 0 && (
                  <div className="text-center py-8 text-xs text-slate-400">
                    No execution logs match filter "{historyFilter}".
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* RIGHT WORKSPACE: EDITOR + RESULTS (Cols 8) */}
        <div className="lg:col-span-8 space-y-6">
          {/* SQL / PLSQL CODE EDITOR */}
          <SqlEditor
            value={sql}
            onChange={setSql}
            onRun={handleRun}
            loading={executing}
            mode={mode}
            onModeChange={setMode}
            strictMode={strictMode}
            onStrictModeChange={setStrictMode}
          />

          {/* EXECUTION RESULT / CONSOLE AREA */}
          <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden min-h-[320px] flex flex-col">
            {/* Results Header Bar */}
            <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 bg-slate-50 border-b border-slate-200 text-xs">
              <div className="flex items-center gap-3">
                <span className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">
                  Execution Output
                </span>

                {result && (
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full font-bold font-mono text-[11px] ${
                      result.success
                        ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                        : result.statementType === 'BLOCKED'
                        ? 'bg-amber-100 text-amber-900 border border-amber-300'
                        : 'bg-rose-100 text-rose-800 border border-rose-200'
                    }`}
                  >
                    {result.success ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    ) : result.statementType === 'BLOCKED' ? (
                      <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                    ) : (
                      <AlertCircle className="w-3.5 h-3.5 text-rose-600" />
                    )}
                    <span>
                      {result.success
                        ? 'STATUS: SUCCESS (200 OK)'
                        : result.statementType === 'BLOCKED'
                        ? 'STATUS: BLOCKED (INTEGRITY GUARD)'
                        : 'STATUS: ERROR'}
                    </span>
                  </span>
                )}

                {result && result.transactionState && result.transactionState !== 'NONE' && (
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded font-mono font-bold text-[10px] uppercase border ${
                      result.transactionState === 'COMMITTED'
                        ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                        : 'bg-rose-100 text-rose-800 border-rose-300'
                    }`}
                    title={`Oracle Transaction Status: ${result.transactionState}`}
                  >
                    TX: {result.transactionState}
                  </span>
                )}
              </div>

              {result && (
                <div className="flex items-center gap-3 font-mono text-[11px] text-slate-600">
                  {result.statementType && (
                    <span className="px-2 py-0.5 rounded bg-slate-200 text-slate-800 font-bold">
                      {result.statementType}
                    </span>
                  )}
                  <span>
                    Time: <strong className="text-slate-900">{result.executionTimeMs} ms</strong>
                  </span>
                  {result.rowCount !== null && result.rowCount !== undefined && (
                    <span>
                      Rows: <strong className="text-slate-900">{result.rowCount}</strong>
                    </span>
                  )}
                  {result.rowsAffected !== null && result.rowsAffected !== undefined && (
                    <span>
                      Rows Affected: <strong className="text-slate-900">{result.rowsAffected}</strong>
                    </span>
                  )}

                  {result.columns && result.rows && result.rows.length > 0 && (
                    <button
                      type="button"
                      onClick={exportAsCsv}
                      className="flex items-center gap-1 px-2 py-0.5 rounded bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold"
                      title="Download results as CSV"
                    >
                      <Download className="w-3 h-3" />
                      <span>CSV</span>
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Results Body */}
            <div className="p-4 flex-1 flex flex-col space-y-4">
              {/* If no query executed yet */}
              {!result && !executing && (
                <div className="flex-1 flex flex-col items-center justify-center py-16 text-center text-slate-400 space-y-2">
                  <Terminal className="w-8 h-8 text-slate-300" />
                  <div className="text-xs font-medium">No query executed yet.</div>
                  <div className="text-[11px] text-slate-400 max-w-sm">
                    Write your query above and click <strong>RUN QUERY</strong> or press{' '}
                    <kbd className="px-1 py-0.5 bg-slate-100 rounded border border-slate-200 text-slate-600">
                      Cmd + Enter
                    </kbd>
                  </div>
                </div>
              )}

              {/* Executing Spinner */}
              {executing && (
                <div className="flex-1 flex flex-col items-center justify-center py-16 text-center text-slate-500 space-y-3">
                  <div className="w-8 h-8 border-4 border-slate-800 border-t-transparent rounded-full animate-spin"></div>
                  <div className="text-xs font-semibold">Executing statement against Oracle Database...</div>
                </div>
              )}

              {/* STRICT NOT-NULL INTEGRITY VALIDATION BLOCKED DISPLAY */}
              {result && (!result.validationPassed || result.statementType === 'BLOCKED') && (
                <div className="rounded-lg border-2 border-amber-400 bg-amber-50/90 p-4 text-xs space-y-2.5 shadow-sm">
                  <div className="flex items-center gap-2 text-amber-900 font-bold">
                    <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                    <span className="text-sm">Strict NOT-NULL Guard: Execution Blocked</span>
                    <span className="font-mono px-2 py-0.5 rounded bg-amber-200 text-amber-900 text-[10px] font-extrabold">
                      ORA-VALIDATION-BLOCKED
                    </span>
                  </div>
                  <p className="text-amber-900 font-medium leading-relaxed font-mono">
                    {result.message}
                  </p>
                  {result.validationMessage && (
                    <div className="mt-2 p-2.5 rounded bg-amber-100/70 border border-amber-300 font-mono text-[11px] text-amber-950 whitespace-pre-wrap">
                      {result.validationMessage}
                    </div>
                  )}
                  <div className="text-[11px] text-amber-800 flex items-center gap-1.5 pt-1">
                    <span>💡 To execute anyway without pre-validation checks, toggle <strong>Strict NOT-NULL Guard</strong> to OFF in the editor toolbar.</span>
                  </div>
                </div>
              )}

              {/* ERROR DISPLAY (Non-validation errors) */}
              {result && !result.success && result.statementType !== 'BLOCKED' && result.validationPassed && (
                <div className="rounded-lg border border-rose-300 bg-rose-50/80 p-4 text-xs space-y-2">
                  <div className="flex items-center gap-2 text-rose-900 font-bold">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                    <span>Oracle Database Execution Error</span>
                    {result.errorCode && (
                      <span className="font-mono px-2 py-0.5 rounded bg-rose-200/80 text-rose-900 text-[11px] font-extrabold">
                        {result.errorCode}
                      </span>
                    )}
                  </div>
                  <pre className="font-mono text-rose-800 whitespace-pre-wrap break-all bg-white/70 p-3 rounded border border-rose-200 leading-5">
                    {result.fullError || result.errorMessage || 'Unknown error during execution'}
                  </pre>
                  {result.isAuthError && (
                    <div className="pt-2">
                      <Link
                        to="/login"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs shadow-sm transition-colors"
                      >
                        Sign in as Admin (admin / admin123)
                      </Link>
                    </div>
                  )}
                </div>
              )}

              {/* SUCCESS MESSAGE BANNER (for DML / DDL / TCL) */}
              {result && result.success && result.message && (
                <div className="flex items-center gap-2 p-3 rounded-md bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 font-medium font-mono">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{result.message}</span>
                </div>
              )}

              {/* DBMS_OUTPUT CONSOLE PANEL */}
              {result && result.dbmsOutput && result.dbmsOutput.trim().length > 0 && (
                <div className="rounded-lg border border-slate-800 bg-slate-950 text-slate-100 p-4 space-y-2 shadow-inner">
                  <div className="flex items-center justify-between text-xs font-mono text-emerald-400 border-b border-slate-800 pb-2">
                    <div className="flex items-center gap-2 font-bold">
                      <Terminal className="w-3.5 h-3.5" />
                      <span>DBMS_OUTPUT Stream (Oracle Buffer Captured)</span>
                    </div>
                    <span className="text-[10px] text-slate-400">
                      Buffer Size: 1,000,000 bytes
                    </span>
                  </div>
                  <pre className="font-mono text-xs text-emerald-300 whitespace-pre-wrap leading-5 max-h-72 overflow-y-auto">
                    {result.dbmsOutput}
                  </pre>
                </div>
              )}

              {/* MULTI-QUERY RESULTS DASHBOARD (when scriptResults exists) */}
              {result && result.scriptResults && result.scriptResults.length > 0 && (
                <div className="space-y-4">
                  {/* Statement Navigation Bar */}
                  <div className="flex items-center gap-1.5 overflow-x-auto pb-1 border-b border-slate-200">
                    <button
                      type="button"
                      onClick={() => setSelectedStatementIdx('all')}
                      className={`px-3 py-1 rounded-md text-xs font-bold transition-all whitespace-nowrap ${
                        selectedStatementIdx === 'all'
                          ? 'bg-purple-600 text-white shadow-xs'
                          : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      }`}
                    >
                      All Statements ({result.scriptResults.length})
                    </button>

                    {result.scriptResults.map((sr, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setSelectedStatementIdx(idx)}
                        className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-mono font-medium transition-all whitespace-nowrap ${
                          selectedStatementIdx === idx
                            ? 'bg-blue-600 text-white shadow-xs'
                            : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                        }`}
                      >
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            sr.success ? 'bg-emerald-500' : 'bg-rose-500'
                          }`}
                        />
                        <span>
                          #{idx + 1}: {sr.statementType}
                          {sr.rowCount !== null && sr.rowCount !== undefined
                            ? ` (${sr.rowCount})`
                            : sr.rowsAffected !== null && sr.rowsAffected !== undefined
                            ? ` (${sr.rowsAffected})`
                            : ''}
                        </span>
                      </button>
                    ))}
                  </div>

                  {/* "ALL STATEMENTS" WATERFALL VIEW */}
                  {selectedStatementIdx === 'all' && (
                    <div className="space-y-4">
                      {result.scriptResults.map((sr, idx) => (
                        <div
                          key={idx}
                          className="p-4 rounded-lg border border-slate-200 bg-white shadow-xs space-y-3"
                        >
                          {/* Statement Header */}
                          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                            <div className="flex items-center gap-2 font-mono text-xs">
                              <span className="font-extrabold text-slate-800">
                                Statement #{idx + 1}
                              </span>
                              <span
                                className={`px-2 py-0.5 rounded font-bold text-[11px] ${
                                  sr.statementType === 'SELECT'
                                    ? 'bg-blue-100 text-blue-800'
                                    : sr.statementType === 'INSERT'
                                    ? 'bg-emerald-100 text-emerald-800'
                                    : sr.statementType === 'UPDATE'
                                    ? 'bg-amber-100 text-amber-800'
                                    : sr.statementType === 'DELETE'
                                    ? 'bg-purple-100 text-purple-800'
                                    : sr.statementType === 'PLSQL'
                                    ? 'bg-cyan-100 text-cyan-800'
                                    : 'bg-slate-100 text-slate-800'
                                }`}
                              >
                                {sr.statementType}
                              </span>
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  sr.success
                                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                    : 'bg-rose-50 text-rose-700 border border-rose-200'
                                }`}
                              >
                                {sr.success ? 'SUCCESS' : 'FAILED'}
                              </span>
                            </div>
                            <span className="text-[11px] font-mono text-slate-500">
                              Time: <strong className="text-slate-800">{sr.executionTimeMs} ms</strong>
                            </span>
                          </div>

                          {/* Statement SQL Preview */}
                          {sr.sql && (
                            <pre className="font-mono text-xs bg-slate-900 text-slate-200 p-2.5 rounded border border-slate-800 overflow-x-auto whitespace-pre-wrap">
                              {sr.sql}
                            </pre>
                          )}

                          {/* Statement Execution Message */}
                          {sr.message && (
                            <div
                              className={`p-2.5 rounded text-xs font-mono flex items-center gap-2 ${
                                sr.success
                                  ? 'bg-emerald-50/70 text-emerald-900 border border-emerald-100'
                                  : 'bg-rose-50 text-rose-900 border border-rose-200'
                              }`}
                            >
                              {sr.success ? (
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                              ) : (
                                <AlertCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />
                              )}
                              <span>{sr.message}</span>
                            </div>
                          )}

                          {/* Interactive Table for this specific statement (if SELECT) */}
                          {sr.columns && sr.rows && sr.rows.length > 0 && (
                            <div className="pt-2">
                              {renderDataTable(sr.columns, sr.rows, `query_${idx + 1}`)}
                            </div>
                          )}

                          {/* DBMS_OUTPUT stream for this specific statement */}
                          {sr.dbmsOutput && sr.dbmsOutput.trim().length > 0 && (
                            <div className="p-3 bg-slate-950 text-emerald-300 font-mono text-xs rounded border border-slate-800">
                              <div className="text-[10px] text-slate-400 border-b border-slate-800 pb-1 mb-1 font-sans">
                                DBMS_OUTPUT:
                              </div>
                              <pre className="whitespace-pre-wrap">{sr.dbmsOutput}</pre>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}

                  {/* FOCUSED STATEMENT VIEW */}
                  {selectedStatementIdx !== 'all' &&
                    result.scriptResults[selectedStatementIdx] && (
                      <div className="p-4 rounded-lg border border-slate-200 bg-white shadow-xs space-y-4">
                        {(() => {
                          const sr = result.scriptResults[selectedStatementIdx];
                          return (
                            <>
                              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                                <div className="flex items-center gap-2 font-mono text-xs">
                                  <span className="font-extrabold text-slate-800">
                                    Statement #{selectedStatementIdx + 1}
                                  </span>
                                  <span
                                    className={`px-2 py-0.5 rounded font-bold text-[11px] ${
                                      sr.statementType === 'SELECT'
                                        ? 'bg-blue-100 text-blue-800'
                                        : sr.statementType === 'INSERT'
                                        ? 'bg-emerald-100 text-emerald-800'
                                        : sr.statementType === 'UPDATE'
                                        ? 'bg-amber-100 text-amber-800'
                                        : sr.statementType === 'DELETE'
                                        ? 'bg-purple-100 text-purple-800'
                                        : sr.statementType === 'PLSQL'
                                        ? 'bg-cyan-100 text-cyan-800'
                                        : 'bg-slate-100 text-slate-800'
                                    }`}
                                  >
                                    {sr.statementType}
                                  </span>
                                  <span
                                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                      sr.success
                                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                        : 'bg-rose-50 text-rose-700 border border-rose-200'
                                    }`}
                                  >
                                    {sr.success ? 'SUCCESS' : 'FAILED'}
                                  </span>
                                </div>
                                <span className="text-[11px] font-mono text-slate-500">
                                  Time: <strong className="text-slate-800">{sr.executionTimeMs} ms</strong>
                                </span>
                              </div>

                              {sr.sql && (
                                <pre className="font-mono text-xs bg-slate-900 text-slate-200 p-3 rounded border border-slate-800 overflow-x-auto whitespace-pre-wrap">
                                  {sr.sql}
                                </pre>
                              )}

                              {sr.message && (
                                <div
                                  className={`p-3 rounded text-xs font-mono flex items-center gap-2 ${
                                    sr.success
                                      ? 'bg-emerald-50/70 text-emerald-900 border border-emerald-100'
                                      : 'bg-rose-50 text-rose-900 border border-rose-200'
                                  }`}
                                >
                                  {sr.success ? (
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                                  ) : (
                                    <AlertCircle className="w-3.5 h-3.5 text-rose-600 shrink-0" />
                                  )}
                                  <span>{sr.message}</span>
                                </div>
                              )}

                              {sr.columns && sr.rows && sr.rows.length > 0 && (
                                <div className="pt-2">
                                  {renderDataTable(
                                    sr.columns,
                                    sr.rows,
                                    `query_${selectedStatementIdx + 1}`
                                  )}
                                </div>
                              )}

                              {sr.dbmsOutput && sr.dbmsOutput.trim().length > 0 && (
                                <div className="p-3 bg-slate-950 text-emerald-300 font-mono text-xs rounded border border-slate-800">
                                  <div className="text-[10px] text-slate-400 border-b border-slate-800 pb-1 mb-1 font-sans">
                                    DBMS_OUTPUT:
                                  </div>
                                  <pre className="whitespace-pre-wrap">{sr.dbmsOutput}</pre>
                                </div>
                              )}
                            </>
                          );
                        })()}
                      </div>
                    )}
                </div>
              )}

              {/* SINGLE QUERY RELATIONAL TABLE (when not a multi-statement script) */}
              {result &&
                (!result.scriptResults || result.scriptResults.length === 0) &&
                result.columns &&
                result.columns.length > 0 &&
                renderDataTable(result.columns, result.rows, 'oracle_query_result')}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
