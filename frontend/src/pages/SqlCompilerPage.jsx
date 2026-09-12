import React, { useState, useEffect } from 'react';
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
  {
    category: 'DQL (Queries)',
    title: 'Select High CGPA Students',
    sql: `-- Select students with CGPA >= 8.5 ordered by merit
SELECT Student_Id, Name, CGPA, Branch, Registration_No
FROM STUDENT
WHERE CGPA >= 8.50
ORDER BY CGPA DESC;`,
  },
  {
    category: 'DQL (Queries)',
    title: 'Active Placement Drives Catalog',
    sql: `-- View all active placement drives and packages
SELECT Drive_Id, Company_Name, Job_Title, Min_CGPA, Package_LPA, Openings, Application_Deadline
FROM PLACEMENT_DRIVE
ORDER BY Package_LPA DESC;`,
  },
  {
    category: 'JOINS',
    title: '5-Table Relational Placement JOIN',
    sql: `-- Multi-table relational join linking candidate to application, drive, and corporate partner
SELECT s.Student_Id, s.Name AS Candidate, s.CGPA,
       a.Application_Id, a.Status,
       d.Drive_Id, d.Job_Title,
       c.Company_Name, c.Industry
FROM STUDENT s
JOIN APPLICATION a ON s.Student_Id = a.Student_Id
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
ORDER BY a.Apply_Date DESC;`,
  },
  {
    category: 'AGGREGATION',
    title: 'GROUP BY & HAVING Summary',
    sql: `-- Aggregates student registration count by branch with HAVING filter
SELECT Branch, COUNT(*) AS Total_Students, ROUND(AVG(CGPA), 2) AS Average_CGPA
FROM STUDENT
GROUP BY Branch
HAVING COUNT(*) >= 1
ORDER BY Average_CGPA DESC;`,
  },
  {
    category: 'SUBQUERIES',
    title: 'Correlated Subquery (EXISTS)',
    sql: `-- Select students who have actively applied for at least one placement drive
SELECT s.Student_Id, s.Name, s.Email, s.CGPA
FROM STUDENT s
WHERE EXISTS (
    SELECT 1
    FROM APPLICATION a
    WHERE a.Student_Id = s.Student_Id
);`,
  },
  {
    category: 'DML',
    title: 'Temporary Table CRUD Lifecycle',
    sql: `-- 1. Create a test table
CREATE TABLE SQL_COMPILER_TEST (
    ID NUMBER PRIMARY KEY,
    NAME VARCHAR2(100),
    CREATED_AT DATE DEFAULT SYSDATE
);

-- 2. Insert test rows
INSERT INTO SQL_COMPILER_TEST (ID, NAME) VALUES (1, 'DBMS DA2 Demonstration');
INSERT INTO SQL_COMPILER_TEST (ID, NAME) VALUES (2, 'Oracle 23c Verification');

-- 3. Verify inserted rows
SELECT * FROM SQL_COMPILER_TEST;`,
  },
  {
    category: 'PL/SQL',
    title: 'Anonymous PL/SQL Block with DBMS_OUTPUT',
    sql: `DECLARE
    v_total_students NUMBER := 0;
    v_total_drives   NUMBER := 0;
BEGIN
    SELECT COUNT(*) INTO v_total_students FROM STUDENT;
    SELECT COUNT(*) INTO v_total_drives FROM PLACEMENT_DRIVE;

    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('   VIT PLACEMENT & TRAINING CELL — ORACLE ENGINE');
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE('Total Registered Students : ' || v_total_students);
    DBMS_OUTPUT.PUT_LINE('Active Campus Drives      : ' || v_total_drives);
    DBMS_OUTPUT.PUT_LINE('PL/SQL Engine Status      : OPERATIONAL');
    DBMS_OUTPUT.PUT_LINE('==================================================');
END;
/`,
  },
  {
    category: 'CURSORS',
    title: 'Explicit Cursor Traversal & DBMS_OUTPUT',
    sql: `DECLARE
    CURSOR c_students IS
        SELECT Student_Id, Name, CGPA
        FROM STUDENT
        ORDER BY CGPA DESC;
    v_id   STUDENT.Student_Id%TYPE;
    v_name STUDENT.Name%TYPE;
    v_cgpa STUDENT.CGPA%TYPE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Candidate Merit Roster (Cursor Loop):');
    OPEN c_students;
    LOOP
        FETCH c_students INTO v_id, v_name, v_cgpa;
        EXIT WHEN c_students%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(' • [' || v_id || '] ' || v_name || ' | CGPA: ' || TO_CHAR(v_cgpa, 'FM90.00'));
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Total Rows Processed: ' || c_students%ROWCOUNT);
    CLOSE c_students;
END;
/`,
  },
  {
    category: 'TCL',
    title: 'Transaction SAVEPOINT & ROLLBACK Scenario',
    sql: `-- Transaction Demonstration
SAVEPOINT demo_point;

-- Attempt an update
UPDATE STUDENT
SET CGPA = 9.99
WHERE Student_Id = 'STU023';

-- View changed row
SELECT Student_Id, Name, CGPA FROM STUDENT WHERE Student_Id = 'STU023';

-- Rollback change back to savepoint
ROLLBACK TO demo_point;

-- Verify original CGPA restored
SELECT Student_Id, Name, CGPA FROM STUDENT WHERE Student_Id = 'STU023';`,
  },
];

export default function SqlCompilerPage() {
  const [sql, setSql] = useState('SELECT * FROM STUDENT;');
  const [mode, setMode] = useState('QUERY');
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

  // Query History
  const [history, setHistory] = useState([]);

  // Active Sidebar View
  const [sidebarView, setSidebarView] = useState('schema'); // 'schema', 'samples', 'history'

  // Load connection info and schema metadata on mount
  useEffect(() => {
    loadConnectionInfo();
    loadSchema();
    const saved = localStorage.getItem('sql_compiler_history');
    if (saved) {
      try {
        setHistory(JSON.parse(saved));
      } catch (e) {}
    }
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

  const handleRun = async (overrideSql) => {
    const textToRun = typeof overrideSql === 'string' && overrideSql.trim() ? overrideSql.trim() : sql.trim();
    if (!textToRun || executing) return;

    try {
      setExecuting(true);
      setResult(null);
      setSelectedStatementIdx('all');

      const res = await api.executeSql({ sql: textToRun, mode });
      setResult(res);

      // Add to history
      const newHistory = [
        {
          id: Date.now(),
          sql: textToRun,
          mode,
          timestamp: new Date().toLocaleTimeString(),
          success: res.success,
          statementType: res.statementType,
        },
        ...history.slice(0, 49),
      ];
      setHistory(newHistory);
      localStorage.setItem('sql_compiler_history', JSON.stringify(newHistory));

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
      setResult({
        success: false,
        statementType: 'ERROR',
        errorCode: 'ORA-CLIENT',
        errorMessage: err.message || 'Execution failed',
        fullError: err.message || 'Network or execution error',
        executionTimeMs: 0,
        columns: null,
        rows: null,
      });
    } finally {
      setExecuting(false);
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
    // If sample contains multiple statements or PL/SQL with slash, set to appropriate mode
    if (sampleSql.includes('/') || (sampleSql.match(/;/g) || []).length > 1) {
      if (sampleSql.includes('/') && !sampleSql.includes('CREATE TABLE')) {
        setMode('QUERY');
      } else if ((sampleSql.match(/;/g) || []).length > 1) {
        setMode('SCRIPT');
      }
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
              onClick={() => setSidebarView('history')}
              className={`flex-1 py-2.5 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
                sidebarView === 'history'
                  ? 'border-slate-900 text-slate-900 bg-white'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <History className="w-3.5 h-3.5" />
              <span>History ({history.length})</span>
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
                                    <th className="pb-1 font-semibold">Key</th>
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
                                      <td className="py-1">
                                        {col.primaryKey && (
                                          <span className="px-1 py-0.2 rounded bg-amber-50 text-amber-700 border border-amber-200 text-[9px] font-bold mr-1">
                                            PK
                                          </span>
                                        )}
                                        {col.foreignKeyRef && (
                                          <span
                                            className="px-1 py-0.2 rounded bg-blue-50 text-blue-700 border border-blue-200 text-[9px] font-medium"
                                            title={`References ${col.foreignKeyRef}`}
                                          >
                                            FK
                                          </span>
                                        )}
                                      </td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
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

          {/* VIEW 3: QUERY HISTORY */}
          {sidebarView === 'history' && (
            <div className="flex-1 p-3 space-y-2 overflow-y-auto max-h-[580px]">
              <div className="flex items-center justify-between text-[11px] text-slate-500 font-medium px-1 pb-1">
                <span>Recent executions ({history.length})</span>
                {history.length > 0 && (
                  <button
                    type="button"
                    onClick={() => {
                      setHistory([]);
                      localStorage.removeItem('sql_compiler_history');
                    }}
                    className="text-rose-600 hover:underline text-[10px]"
                  >
                    Clear History
                  </button>
                )}
              </div>
              {history.map((h) => (
                <div
                  key={h.id}
                  onClick={() => {
                    setSql(h.sql);
                    setMode(h.mode || 'QUERY');
                  }}
                  className="p-2.5 rounded-lg border border-slate-200 hover:border-slate-400 bg-white hover:bg-slate-50 cursor-pointer transition-all space-y-1"
                >
                  <div className="flex items-center justify-between text-[10px]">
                    <span
                      className={`font-mono font-bold px-1.5 py-0.2 rounded ${
                        h.success ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                      }`}
                    >
                      {h.statementType || (h.success ? 'SUCCESS' : 'ERROR')}
                    </span>
                    <span className="text-slate-400 font-mono">{h.timestamp}</span>
                  </div>
                  <pre className="text-[11px] text-slate-800 font-mono line-clamp-2 overflow-hidden bg-slate-50 p-1 rounded">
                    {h.sql}
                  </pre>
                </div>
              ))}
              {history.length === 0 && (
                <div className="text-center py-12 text-xs text-slate-400">
                  No queries executed in this session yet.
                </div>
              )}
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
                        : 'bg-rose-100 text-rose-800 border border-rose-200'
                    }`}
                  >
                    {result.success ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <AlertCircle className="w-3.5 h-3.5 text-rose-600" />
                    )}
                    <span>{result.success ? 'STATUS: SUCCESS (200 OK)' : 'STATUS: ERROR'}</span>
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

              {/* ERROR DISPLAY */}
              {result && !result.success && (
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
