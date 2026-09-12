import React, { useState, useEffect } from "react";
import { api } from "../lib/api";
import { formatPackage } from "../lib/formatters";
import { BarChart, DonutChart } from "../components/Charts";
import {
  ShieldCheck,
  TrendingUp,
  Award,
  Users,
  Building,
  Database,
  Search,
  Code2,
  FileText,
  Clock,
  CheckCircle2,
  Calendar,
  Layers,
  Briefcase,
  Play,
  Terminal,
  AlertCircle,
  ExternalLink,
  UserCheck,
  Shield,
  Trash2,
  Plus,
} from "lucide-react";

export default function AdminPortal() {
  const [overview, setOverview] = useState(null);
  const [students, setStudents] = useState([]);
  const [drives, setDrives] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [applications, setApplications] = useState([]);
  const [offers, setOffers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("overview"); // overview, students, drives, offers, database, recruiters
  const [studentSearch, setStudentSearch] = useState("");
  const [notification, setNotification] = useState(null);

  // Recruiter Authorization Management State
  const [recruiters, setRecruiters] = useState([]);
  const [assigningRecruiterId, setAssigningRecruiterId] = useState(null);
  const [selectedCompanyToAssign, setSelectedCompanyToAssign] = useState("");
  const [recruiterActionLoading, setRecruiterActionLoading] = useState(false);

  // Database Console State
  const [selectedQueryKey, setSelectedQueryKey] = useState("DQL_JOIN");
  const [customSql, setCustomSql] = useState("");
  const [isCustomMode, setIsCustomMode] = useState(false);
  const [consoleExecuting, setConsoleExecuting] = useState(false);
  const [consoleResult, setConsoleResult] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ovw, stus, drvs, comps, apps, ofrs, audits] = await Promise.all([
        api.getOverviewReport(),
        api.getAllStudents(),
        api.getDrives(),
        api.getCompanies(),
        api.getApplications(),
        api.getOffers(),
        api.getAuditLogs(),
      ]);
      setOverview(ovw);
      setStudents(stus || []);
      setDrives(drvs || []);
      setCompanies(comps || []);
      setApplications(apps || []);
      setOffers(ofrs || []);
      setAuditLogs(audits || []);
    } catch (err) {
      console.error("Failed to load admin data:", err);
      setNotification({
        type: "error",
        message: err.message || "Failed to fetch database reports.",
      });
    } finally {
      setLoading(false);
    }
  };

  const loadRecruiters = async () => {
    try {
      const data = await api.getAdminRecruiters();
      setRecruiters(data || []);
    } catch (err) {
      console.error("Failed to load recruiters:", err);
    }
  };

  useEffect(() => {
    loadData();
    loadRecruiters();
  }, []);

  const handleAssignCompany = async (userId, companyId) => {
    if (!companyId) return;
    try {
      setRecruiterActionLoading(true);
      await api.assignRecruiterCompany(userId, companyId);
      await loadRecruiters();
      setAssigningRecruiterId(null);
      setSelectedCompanyToAssign("");
      setNotification({
        type: "success",
        message: `Successfully authorized company ${companyId} for recruiter.`,
      });
    } catch (err) {
      setNotification({
        type: "error",
        message: err.message || "Failed to assign company authorization.",
      });
    } finally {
      setRecruiterActionLoading(false);
    }
  };

  const handleRevokeCompany = async (userId, companyId) => {
    if (!window.confirm(`Revoke authorization for company ${companyId}?`)) return;
    try {
      setRecruiterActionLoading(true);
      await api.revokeRecruiterCompany(userId, companyId);
      await loadRecruiters();
      setNotification({
        type: "success",
        message: `Successfully revoked company ${companyId} authorization.`,
      });
    } catch (err) {
      setNotification({
        type: "error",
        message: err.message || "Failed to revoke company authorization.",
      });
    } finally {
      setRecruiterActionLoading(false);
    }
  };

  // Execute console query on mount or query change
  const executeQuery = async (key = selectedQueryKey, custom = isCustomMode) => {
    try {
      setConsoleExecuting(true);
      const res = await api.executeDatabaseScript({
        queryKey: custom ? null : key,
        customSql: custom ? customSql : null,
      });
      setConsoleResult(res);
    } catch (err) {
      setConsoleResult({
        category: custom ? "Custom SQL" : "Preset Query",
        queryTitle: "Execution Failure",
        sql: custom ? customSql : key,
        columns: [],
        rows: [],
        rowCount: 0,
        executionTimeMs: 0,
        status: "ERROR: " + err.message,
        notes: "Database query execution halted.",
      });
    } finally {
      setConsoleExecuting(false);
    }
  };

  useEffect(() => {
    if (activeTab === "database" && !consoleResult) {
      executeQuery("DQL_JOIN", false);
    }
  }, [activeTab]);

  const handleQuerySelect = (key) => {
    setSelectedQueryKey(key);
    setIsCustomMode(false);
    executeQuery(key, false);
  };

  const handleCustomExecute = (e) => {
    e.preventDefault();
    if (!customSql.trim()) return;
    setIsCustomMode(true);
    executeQuery(null, true);
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Placement Analytics...</span>
        </div>
      </div>
    );
  }

  // Bar chart data from overview report CTC distribution
  const salaryTierData = (overview?.ctcDistribution || []).map((t, idx) => ({
    name: t.tier,
    value: t.count,
    color: idx === 0 ? "bg-emerald-600" : idx === 1 ? "bg-indigo-600" : "bg-amber-600",
  }));

  // Donut chart data from overview report application status counts
  const appStatusCounts = overview?.applicationStatusCounts || {};
  const statusDonutData = [
    { name: "Offered", value: appStatusCounts.OFFERED || 0, color: "#10b981" },
    { name: "Selected", value: appStatusCounts.SELECTED || 0, color: "#0d9488" },
    { name: "Interviewing", value: appStatusCounts.INTERVIEWING || 0, color: "#2563eb" },
    { name: "Shortlisted", value: appStatusCounts.SHORTLISTED || 0, color: "#7c3aed" },
    { name: "Applied", value: appStatusCounts.APPLIED || 0, color: "#d97706" },
    { name: "Rejected", value: appStatusCounts.REJECTED || 0, color: "#e11d48" },
  ].filter((d) => d.value > 0);

  // Filtered Students
  const filteredStudents = students.filter(
    (s) =>
      (s.name && s.name.toLowerCase().includes(studentSearch.toLowerCase())) ||
      (s.studentId && s.studentId.toLowerCase().includes(studentSearch.toLowerCase())) ||
      (s.branch && s.branch.toLowerCase().includes(studentSearch.toLowerCase()))
  );

  const PRESET_QUERIES = [
    { key: "DQL_JOIN", label: "5-Table INNER JOIN", type: "DQL" },
    { key: "DQL_GROUP_HAVING", label: "GROUP BY & HAVING", type: "DQL" },
    { key: "DQL_DENSE_RANK", label: "DENSE_RANK Analytic", type: "DQL" },
    { key: "PLSQL_PKG_REPORTS", label: "PKG_PLACEMENT_REPORTS", type: "PL/SQL" },
    { key: "TCL_SCENARIO", label: "Transaction / Savepoint", type: "TCL" },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Toast Notification */}
      {notification && (
        <div className="p-4 rounded-lg border bg-rose-50 text-rose-900 border-rose-300 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600" />
            <span>{notification.message}</span>
          </div>
          <button onClick={() => setNotification(null)} className="font-semibold text-xs">Dismiss</button>
        </div>
      )}

      {/* Header & Subtitle */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-xl font-bold text-slate-900">Placement Cell Administrator Console</h1>
            <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-800 border border-slate-300">
              Authority: Dean / DBA
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Authoritative institutional reporting generated directly from Oracle SQL & PL/SQL functions
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex flex-wrap bg-slate-100 p-1 rounded-md text-xs font-medium text-slate-600">
          {[
            { id: "overview", label: "Overview & Analytics" },
            { id: "students", label: "Students Directory" },
            { id: "drives", label: "Companies & Drives" },
            { id: "offers", label: "Released Offers" },
            { id: "recruiters", label: "Recruiter Authorization" },
            { id: "database", label: "Database Console & Audit" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={"px-3 py-1.5 rounded transition-all " +
                (activeTab === tab.id
                  ? "bg-white text-slate-900 font-bold shadow-sm"
                  : "hover:text-slate-900")}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* TOP 6 METRICS ROW (Authoritative from GET /api/v1/reports/overview) */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Eligible Students
          </span>
          <div className="text-xl font-bold font-mono text-slate-900 mt-1">
            {overview?.eligibleStudents ?? overview?.totalStudents ?? 0}
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Verified Company Registrations</span>
        </div>

        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Students Placed
          </span>
          <div className="text-xl font-bold font-mono text-emerald-700 mt-1">
            {overview?.uniqueStudentsPlaced || 0}
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Unique Hired Candidates</span>
        </div>

        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Placement Rate
          </span>
          <div className="text-xl font-bold font-mono text-emerald-700 mt-1">
            {overview?.placementRate ? overview.placementRate.toFixed(1) : "0.0"}%
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">PL/SQL GET_PLACEMENT_RATE</span>
        </div>

        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Average Package
          </span>
          <div className="text-xl font-bold font-mono text-blue-700 mt-1">
            ₹ {overview?.averagePackage ? overview.averagePackage.toFixed(2) : "0.00"} L
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">PL/SQL GET_AVERAGE_PACKAGE</span>
        </div>

        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Highest Package
          </span>
          <div className="text-xl font-bold font-mono text-purple-700 mt-1">
            ₹ {overview?.highestPackage ? overview.highestPackage.toFixed(2) : "0.00"} L
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Top Package Released</span>
        </div>

        <div className="p-4 bg-white rounded-lg border border-slate-200 shadow-sm">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500 block">
            Offers Released
          </span>
          <div className="text-xl font-bold font-mono text-slate-900 mt-1">
            {overview?.totalOffers || 0}
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Total Offer Letters</span>
        </div>
      </div>

      {/* TAB 1: OVERVIEW & ANALYTICS */}
      {activeTab === "overview" && (
        <div className="space-y-6">
          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Salary Tier Breakdown */}
            <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                    Compensation Tier Distribution
                  </h3>
                  <p className="text-xs text-slate-500">Live offer breakdown across Super Dream, Dream, and Regular tiers</p>
                </div>
                <span className="text-xs font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded border border-slate-300">
                  {overview?.totalOffers || 0} Offers
                </span>
              </div>
              <BarChart data={salaryTierData} xKey="name" yKey="value" unit="Offers" />
            </div>

            {/* Application Funnel */}
            <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                    Candidate Application Pipeline
                  </h3>
                  <p className="text-xs text-slate-500">Distribution of all applications across state machine stages</p>
                </div>
                <span className="text-xs font-mono font-bold text-slate-700 bg-slate-100 px-2 py-0.5 rounded border border-slate-300">
                  {overview?.totalApplications || 0} Total
                </span>
              </div>
              <div className="pt-2">
                <DonutChart data={statusDonutData} size={180} />
              </div>
            </div>
          </div>

          {/* Academic Program Placement Table */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-1">
              Academic Program Placement Performance
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Cross-tabulation of student registrations and placement conversion by program
            </p>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-2.5">Program Name</th>
                    <th className="px-4 py-2.5 text-center">Enrolled Students</th>
                    <th className="px-4 py-2.5 text-center">Placed Candidates</th>
                    <th className="px-4 py-2.5 text-right">Placement Rate (%)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {(overview?.programPlacement || []).map((prog, idx) => (
                    <tr key={idx} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-semibold text-slate-900">{prog.programName}</td>
                      <td className="px-4 py-3 text-center font-mono">{prog.enrolled}</td>
                      <td className="px-4 py-3 text-center font-mono font-bold text-emerald-700">{prog.placed}</td>
                      <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                        {prog.rate ? prog.rate.toFixed(1) : "0.0"}%
                      </td>
                    </tr>
                  ))}
                  {(!overview?.programPlacement || overview.programPlacement.length === 0) && (
                    <tr>
                      <td colSpan={4} className="text-center py-6 text-xs text-slate-500">
                        No academic program registration data recorded.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Corporate Partner Hiring Summary */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-1">
              Corporate Partner Hiring Statistics
            </h3>
            <p className="text-xs text-slate-500 mb-4">
              Top hiring organizations, roles offered, and average package awarded
            </p>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-2.5">Company Name</th>
                    <th className="px-4 py-2.5">Industry Sector</th>
                    <th className="px-4 py-2.5 text-center">Offers Released</th>
                    <th className="px-4 py-2.5 text-right">Average Package (CTC)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {(overview?.companyHiring || []).map((comp, idx) => (
                    <tr key={idx} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-semibold text-slate-900">{comp.companyName}</td>
                      <td className="px-4 py-3 text-slate-600">{comp.industry || "—"}</td>
                      <td className="px-4 py-3 text-center font-mono font-bold text-emerald-700">{comp.hires}</td>
                      <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                        {formatPackage(comp.avgPackage)}
                      </td>
                    </tr>
                  ))}
                  {(!overview?.companyHiring || overview.companyHiring.length === 0) && (
                    <tr>
                      <td colSpan={4} className="text-center py-6 text-xs text-slate-500">
                        No corporate hiring records yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: STUDENTS DIRECTORY */}
      {activeTab === "students" && (
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Registered Students Directory ({students.length})
              </h2>
              <p className="text-xs text-slate-500">
                Authoritative academic records, CGPA cutoffs, and placement status
              </p>
            </div>
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                value={studentSearch}
                onChange={(e) => setStudentSearch(e.target.value)}
                placeholder="Search by name, ID or branch..."
                className="pl-8 pr-3 py-1.5 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-500 w-64"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Student ID</th>
                  <th className="px-4 py-3">Name & Email</th>
                  <th className="px-4 py-3">CGPA</th>
                  <th className="px-4 py-3">Branch</th>
                  <th className="px-4 py-3">Location</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-slate-700">
                {filteredStudents.map((s) => {
                  const studentOffer = offers.find((o) => o.studentId === s.studentId);
                  return (
                    <tr key={s.studentId} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{s.studentId}</td>
                      <td className="px-4 py-3">
                        <div className="font-bold text-slate-900">{s.name}</div>
                        <div className="text-[11px] text-slate-500">{s.email}</div>
                      </td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">
                        {s.cgpa ? s.cgpa.toFixed(2) : "—"}
                      </td>
                      <td className="px-4 py-3 text-slate-600">{s.branch || "—"}</td>
                      <td className="px-4 py-3 text-slate-500">{s.city && s.state ? `${s.city}, ${s.state}` : (s.city || s.state || "—")}</td>
                      <td className="px-4 py-3">
                        {studentOffer ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            Placed ({formatPackage(studentOffer.ctcLpa)})
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-600">
                            Participating
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: COMPANIES & DRIVES */}
      {activeTab === "drives" && (
        <div className="space-y-6">
          {/* Active Placement Drives */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Campus Placement Drives ({drives.length})
              </h2>
              <p className="text-xs text-slate-500">
                Published placement openings, eligibility criteria, and compensation packages
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Drive ID</th>
                    <th className="px-4 py-3">Company Name</th>
                    <th className="px-4 py-3">Job Title / Role</th>
                    <th className="px-4 py-3">Min CGPA</th>
                    <th className="px-4 py-3">Package (LPA)</th>
                    <th className="px-4 py-3">Openings</th>
                    <th className="px-4 py-3">Drive Date</th>
                    <th className="px-4 py-3">Deadline</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {drives.map((d) => (
                    <tr key={d.driveId} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{d.driveId}</td>
                      <td className="px-4 py-3 font-semibold text-slate-900">{d.companyName}</td>
                      <td className="px-4 py-3 text-slate-700">{d.jobTitle}</td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">{d.minCgpa?.toFixed(2)}</td>
                      <td className="px-4 py-3 font-mono font-bold text-emerald-700">{formatPackage(d.ctc ?? d.packageLpa)}</td>
                      <td className="px-4 py-3 font-mono text-slate-700">{d.openings}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{d.driveDate}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{d.applicationDeadline}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Partner Companies */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Corporate Recruitment Partners ({companies.length})
              </h2>
              <p className="text-xs text-slate-500">
                Registered recruiting organizations in the database
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Company ID</th>
                    <th className="px-4 py-3">Company Name</th>
                    <th className="px-4 py-3">Industry Domain</th>
                    <th className="px-4 py-3">Recruiter Contact</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {companies.map((c) => (
                    <tr key={c.companyId} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{c.companyId}</td>
                      <td className="px-4 py-3 font-semibold text-slate-900">{c.companyName}</td>
                      <td className="px-4 py-3 text-slate-600">{c.industry}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{c.email}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: RELEASED OFFERS */}
      {activeTab === "offers" && (
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Issued Placement Offers ({offers.length})
              </h2>
              <p className="text-xs text-slate-500">
                Official offer letters recorded in OFFER_LETTER table via PL/SQL ISSUE_OFFER
              </p>
            </div>
            <span className="text-xs font-mono font-bold px-2.5 py-1 bg-emerald-50 text-emerald-800 rounded border border-emerald-200">
              Total Value: {formatPackage(offers.reduce((sum, o) => sum + (parseFloat(o.ctcLpa) || 0), 0))}
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Offer ID</th>
                  <th className="px-4 py-3">Candidate</th>
                  <th className="px-4 py-3">Recruiter Company</th>
                  <th className="px-4 py-3">Role / Designation</th>
                  <th className="px-4 py-3">Compensation (CTC)</th>
                  <th className="px-4 py-3">Offer Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-slate-700">
                {offers.map((o) => (
                  <tr key={o.offerId} className="hover:bg-slate-50/70">
                    <td className="px-4 py-3 font-mono font-bold text-emerald-800">{o.offerId}</td>
                    <td className="px-4 py-3">
                      <div className="font-bold text-slate-900">{o.studentName}</div>
                      <div className="text-[11px] font-mono text-slate-500">{o.studentId}</div>
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-800">{o.companyName}</td>
                    <td className="px-4 py-3 text-slate-600">{o.jobTitle}</td>
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">{formatPackage(o.ctcLpa)}</td>
                    <td className="px-4 py-3 text-slate-500 font-mono">{o.offerDate}</td>
                  </tr>
                ))}
                {offers.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center py-6 text-xs text-slate-500">
                      No offers issued yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 5: DATABASE CONSOLE & TRIGGER AUDIT TRAIL */}
      {activeTab === "database" && (
        <div className="space-y-8">
          {/* Section A: Interactive DA2 SQL / PLSQL Runner */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-5">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <Terminal className="w-4 h-4 text-slate-700" />
                Interactive Database Console (DA2 Syllabus Demonstrator)
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Executes live SQL joins, aggregation with HAVING, analytic window functions, and PL/SQL packages against the database engine.
              </p>
            </div>

            {/* Preset Query Buttons */}
            <div className="flex flex-wrap gap-2">
              {PRESET_QUERIES.map((q) => (
                <button
                  key={q.key}
                  onClick={() => handleQuerySelect(q.key)}
                  className={"px-3 py-1.5 rounded-md text-xs font-semibold transition-all border " +
                    (!isCustomMode && selectedQueryKey === q.key
                      ? "bg-slate-900 text-white border-slate-900 shadow-sm"
                      : "bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100")}
                >
                  <span className="opacity-70 text-[10px] uppercase font-mono mr-1.5">[{q.type}]</span>
                  {q.label}
                </button>
              ))}
              <button
                onClick={() => setIsCustomMode(true)}
                className={"px-3 py-1.5 rounded-md text-xs font-semibold transition-all border " +
                  (isCustomMode
                    ? "bg-slate-900 text-white border-slate-900 shadow-sm"
                    : "bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100")}
              >
                Custom SQL Runner
              </button>
            </div>

            {/* Custom SQL input box if in custom mode */}
            {isCustomMode && (
              <form onSubmit={handleCustomExecute} className="space-y-2">
                <label className="block text-xs font-bold text-slate-700">Enter SQL Query:</label>
                <textarea
                  rows={4}
                  value={customSql}
                  onChange={(e) => setCustomSql(e.target.value)}
                  placeholder="e.g. SELECT * FROM STUDENT WHERE CGPA >= 9.0"
                  className="w-full p-3 font-mono text-xs border border-slate-300 rounded-md bg-slate-900 text-emerald-400 focus:outline-none focus:ring-1 focus:ring-slate-500"
                />
                <button
                  type="submit"
                  disabled={consoleExecuting}
                  className="px-4 py-1.5 rounded-md bg-slate-900 text-white font-semibold text-xs hover:bg-slate-800 disabled:opacity-50"
                >
                  {consoleExecuting ? "Executing..." : "Execute Custom Query"}
                </button>
              </form>
            )}

            {/* Executed Query Metadata & SQL Block */}
            {consoleResult && (
              <div className="space-y-3 pt-2 border-t border-slate-100">
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div>
                    <span className="font-bold text-slate-900">{consoleResult.queryTitle}</span>
                    <span className="text-slate-500 ml-2">({consoleResult.category})</span>
                  </div>
                  <div className="flex items-center gap-3 font-mono text-[11px]">
                    <span className="text-slate-500">Time: <strong className="text-slate-800">{consoleResult.executionTimeMs} ms</strong></span>
                    <span className="text-slate-500">Rows: <strong className="text-slate-800">{consoleResult.rowCount}</strong></span>
                    <span className={"px-2 py-0.5 rounded font-bold " + (consoleResult.status.startsWith("SUCCESS") ? "bg-emerald-50 text-emerald-800 border border-emerald-200" : "bg-rose-50 text-rose-800 border border-rose-200")}>
                      {consoleResult.status}
                    </span>
                  </div>
                </div>

                {/* SQL Code Block */}
                <pre className="p-3 bg-slate-900 text-emerald-400 text-xs font-mono rounded-md overflow-x-auto border border-slate-800">
                  {consoleResult.sql}
                </pre>
                <p className="text-[11px] text-slate-500 italic">{consoleResult.notes}</p>

                {/* Query Results Table */}
                <div className="overflow-x-auto rounded-md border border-slate-200 max-h-80 overflow-y-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200 sticky top-0">
                      <tr>
                        {consoleResult.columns.map((col, idx) => (
                          <th key={idx} className="px-3 py-2 font-mono text-[11px]">
                            {col}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 text-slate-700 font-mono text-[11px]">
                      {consoleResult.rows.map((row, rIdx) => (
                        <tr key={rIdx} className="hover:bg-slate-50">
                          {consoleResult.columns.map((col, cIdx) => (
                            <td key={cIdx} className="px-3 py-1.5 whitespace-nowrap">
                              {row[col] !== null && row[col] !== undefined ? String(row[col]) : "NULL"}
                            </td>
                          ))}
                        </tr>
                      ))}
                      {consoleResult.rows.length === 0 && (
                        <tr>
                          <td colSpan={consoleResult.columns.length || 1} className="text-center py-4 text-slate-500">
                            No rows returned by this query execution.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* Section B: Database Trigger Audit Trail */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-slate-700" />
                  Database Trigger Audit Trail (APPLICATION_AUDIT)
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Automated audit entries logged by database triggers (TRG_APPLICATION_AUDIT and TRG_OFFER_APPLICATION_STATUS)
                </p>
              </div>
              <span className="text-xs font-mono font-bold px-2.5 py-1 bg-slate-100 text-slate-700 rounded">
                {auditLogs.length} Audit Entries
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Audit ID</th>
                    <th className="px-4 py-3">Application ID</th>
                    <th className="px-4 py-3">State Transition</th>
                    <th className="px-4 py-3">Timestamp</th>
                    <th className="px-4 py-3">Trigger / Actor</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700 font-mono text-[11px]">
                  {auditLogs.map((log) => (
                    <tr key={log.auditId} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-bold text-slate-500">#{log.auditId}</td>
                      <td className="px-4 py-3 font-bold text-slate-800">{log.applicationId}</td>
                      <td className="px-4 py-3 font-sans">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-600 text-[10px] font-semibold font-mono">
                            {log.oldStatus}
                          </span>
                          <span className="text-slate-400">➔</span>
                          <span
                            className={"px-2 py-0.5 rounded text-[10px] font-bold font-mono " +
                              (log.newStatus === "OFFERED"
                                ? "bg-emerald-100 text-emerald-800"
                                : log.newStatus === "SELECTED"
                                ? "bg-teal-100 text-teal-800"
                                : log.newStatus === "INTERVIEWING"
                                ? "bg-blue-100 text-blue-800"
                                : "bg-purple-100 text-purple-800")}
                          >
                            {log.newStatus}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-slate-500">{log.changedAt}</td>
                      <td className="px-4 py-3 font-sans">
                        <div className="font-semibold text-slate-800 text-xs">{log.changedBy}</div>
                        <div className="text-[10px] text-slate-400 font-mono">
                          Source: {log.triggerSource || "DATABASE_TRIGGER"}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {auditLogs.length === 0 && (
                    <tr>
                      <td colSpan={5} className="text-center py-6 text-xs text-slate-500">
                        No audit records currently found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* RECRUITER AUTHORIZATION TAB */}
      {activeTab === "recruiters" && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-blue-600" />
                  Recruiter Identity & Organization Authorizations
                </h2>
                <p className="text-xs text-slate-500 mt-1">
                  Manage campus recruiters and grant or revoke access to recruiting organizations backed by RECRUITER_COMPANY
                </p>
              </div>
              <span className="text-xs font-mono font-bold px-3 py-1 bg-blue-50 text-blue-700 border border-blue-200 rounded-md">
                {recruiters.length} Registered Recruiters
              </span>
            </div>

            <div className="mt-6 overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Recruiter</th>
                    <th className="px-4 py-3">User ID & Username</th>
                    <th className="px-4 py-3">Email</th>
                    <th className="px-4 py-3">Authorized Organizations</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {recruiters.map((r) => (
                    <tr key={r.userId} className="hover:bg-slate-50/75">
                      <td className="px-4 py-3">
                        <div className="font-bold text-slate-900">{r.fullName || r.username}</div>
                        <div className="text-[11px] text-slate-500">Recruiter Account</div>
                      </td>
                      <td className="px-4 py-3 font-mono text-[11px]">
                        <span className="font-bold text-slate-800">{r.userId}</span>
                        <span className="text-slate-400 block text-[10px]">@{r.username}</span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {r.email || "—"}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1.5">
                          {r.authorizedCompanies && r.authorizedCompanies.length > 0 ? (
                            r.authorizedCompanies.map((c) => (
                              <span
                                key={c.companyId}
                                className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-800 border border-slate-200"
                              >
                                <span>{c.companyName} ({c.companyId})</span>
                                <button
                                  type="button"
                                  disabled={recruiterActionLoading}
                                  onClick={() => handleRevokeCompany(r.userId, c.companyId)}
                                  className="text-slate-400 hover:text-rose-600 font-bold transition-colors ml-0.5"
                                  title={`Revoke authorization for ${c.companyName}`}
                                >
                                  ×
                                </button>
                              </span>
                            ))
                          ) : (
                            <span className="text-amber-600 text-[11px] font-medium italic">
                              No organizations authorized
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-right">
                        {assigningRecruiterId === r.userId ? (
                          <div className="flex items-center justify-end gap-2">
                            <select
                              value={selectedCompanyToAssign}
                              onChange={(e) => setSelectedCompanyToAssign(e.target.value)}
                              className="text-xs border border-slate-300 rounded px-2 py-1 bg-white font-medium focus:outline-none"
                            >
                              <option value="">Select Company...</option>
                              {companies
                                .filter(
                                  (comp) =>
                                    !r.authorizedCompanies?.some(
                                      (ac) => ac.companyId === comp.companyId
                                    )
                                )
                                .map((comp) => (
                                  <option key={comp.companyId} value={comp.companyId}>
                                    {comp.companyName} ({comp.companyId})
                                  </option>
                                ))}
                            </select>
                            <button
                              type="button"
                              disabled={!selectedCompanyToAssign || recruiterActionLoading}
                              onClick={() =>
                                handleAssignCompany(r.userId, selectedCompanyToAssign)
                              }
                              className="px-2.5 py-1 text-xs font-semibold bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                            >
                              Assign
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setAssigningRecruiterId(null);
                                setSelectedCompanyToAssign("");
                              }}
                              className="px-2 py-1 text-xs text-slate-500 hover:text-slate-800"
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setAssigningRecruiterId(r.userId);
                              setSelectedCompanyToAssign("");
                            }}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-semibold bg-slate-100 text-slate-700 hover:bg-blue-50 hover:text-blue-700 transition-colors border border-slate-200"
                          >
                            <Plus className="w-3.5 h-3.5" />
                            Authorize Company
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {recruiters.length === 0 && (
                    <tr>
                      <td colSpan={5} className="text-center py-8 text-xs text-slate-500">
                        No recruiter accounts registered.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
