import React, { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { formatPackage } from "../lib/formatters";
import {
  BarChart,
  DonutChart,
  TimelineChart,
  CompanyPerformanceAnalytics,
  MetricCard,
} from "../components/Charts";
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
  RefreshCw,
  Download,
  Filter,
  ArrowUpRight,
  Activity,
  CheckCircle,
} from "lucide-react";

export default function AdminPortal() {
  const [overview, setOverview] = useState(null);
  const [students, setStudents] = useState([]);
  const [drives, setDrives] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [applications, setApplications] = useState([]);
  const [offers, setOffers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [recruiters, setRecruiters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("overview");
  const [notification, setNotification] = useState(null);

  // Recruiter authorization modal / action state
  const [assigningRecruiterId, setAssigningRecruiterId] = useState(null);
  const [selectedCompanyToAssign, setSelectedCompanyToAssign] = useState("");
  const [recruiterActionLoading, setRecruiterActionLoading] = useState(false);

  // Filter States
  const [studentSearch, setStudentSearch] = useState("");
  const [studentBranchFilter, setStudentBranchFilter] = useState("ALL");
  const [studentStatusFilter, setStudentStatusFilter] = useState("ALL");
  const [driveSearch, setDriveSearch] = useState("");
  const [offerSearch, setOfferSearch] = useState("");

  // Database Console State
  const [selectedQueryKey, setSelectedQueryKey] = useState("DQL_JOIN");
  const [customSql, setCustomSql] = useState("");
  const [isCustomMode, setIsCustomMode] = useState(false);
  const [consoleExecuting, setConsoleExecuting] = useState(false);
  const [consoleResult, setConsoleResult] = useState(null);

  // SQL Compiler Execution Audit Log (SQL_EXECUTION_AUDIT)
  const [sqlAuditLogs, setSqlAuditLogs] = useState([]);
  const [sqlAuditLoading, setSqlAuditLoading] = useState(false);

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

  const loadSqlAudit = async () => {
    try {
      setSqlAuditLoading(true);
      const data = await api.getSqlHistory(50);
      setSqlAuditLogs(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to load SQL execution audit:", err);
    } finally {
      setSqlAuditLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    loadRecruiters();
    loadSqlAudit();

    // Real-time synchronization: reload all metrics whenever database mutations occur
    const handleMutation = () => {
      loadData();
      loadRecruiters();
      loadSqlAudit();
    };
    window.addEventListener("portal:database-mutation", handleMutation);

    let channel;
    try {
      channel = new BroadcastChannel("portal-database-channel");
      channel.onmessage = () => {
        handleMutation();
      };
    } catch (err) {
      // BroadcastChannel unsupported or restricted in this environment
      console.debug("BroadcastChannel not supported:", err);
    }

    return () => {
      window.removeEventListener("portal:database-mutation", handleMutation);
      if (channel) {
        channel.close();
      }
    };
  }, []);

  useEffect(() => {
    loadData();
    if (activeTab === "database") {
      loadSqlAudit();
    }
  }, [activeTab]);

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

  // Execute database query
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

  const exportOffersCsv = () => {
    if (!offers || offers.length === 0) return;
    const headers = ["Offer ID", "Student ID", "Candidate Name", "Company Name", "Role", "CTC (LPA)", "Offer Date", "Status"];
    const rows = offers.map((o) => [
      o.offerId,
      o.studentId,
      `"${o.studentName || ""}"`,
      `"${o.companyName || ""}"`,
      `"${o.jobTitle || ""}"`,
      o.ctcLpa,
      o.offerDate,
      o.status || "OFFERED",
    ]);
    const csvContent = [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `released_offers_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Distinct academic branches for filtering
  const distinctBranches = useMemo(() => {
    const set = new Set();
    students.forEach((s) => {
      if (s.branch) set.add(s.branch);
    });
    return Array.from(set).sort();
  }, [students]);

  // Filtered Students
  const filteredStudents = useMemo(() => {
    return students.filter((s) => {
      const matchesSearch =
        !studentSearch ||
        (s.name && s.name.toLowerCase().includes(studentSearch.toLowerCase())) ||
        (s.studentId && s.studentId.toLowerCase().includes(studentSearch.toLowerCase())) ||
        (s.branch && s.branch.toLowerCase().includes(studentSearch.toLowerCase())) ||
        (s.email && s.email.toLowerCase().includes(studentSearch.toLowerCase()));

      if (!matchesSearch) return false;

      if (studentBranchFilter !== "ALL" && s.branch !== studentBranchFilter) {
        return false;
      }

      const isPlaced = offers.some((o) => o.studentId === s.studentId);
      if (studentStatusFilter === "PLACED" && !isPlaced) return false;
      if (studentStatusFilter === "PARTICIPATING" && isPlaced) return false;

      return true;
    });
  }, [students, studentSearch, studentBranchFilter, studentStatusFilter, offers]);

  // Filtered Drives
  const filteredDrives = useMemo(() => {
    return drives.filter((d) => {
      if (!driveSearch) return true;
      const q = driveSearch.toLowerCase();
      return (
        (d.jobTitle && d.jobTitle.toLowerCase().includes(q)) ||
        (d.companyName && d.companyName.toLowerCase().includes(q)) ||
        (d.driveId && d.driveId.toLowerCase().includes(q)) ||
        (d.location && d.location.toLowerCase().includes(q))
      );
    });
  }, [drives, driveSearch]);

  // Filtered Offers
  const filteredOffers = useMemo(() => {
    return offers.filter((o) => {
      if (!offerSearch) return true;
      const q = offerSearch.toLowerCase();
      return (
        (o.offerId && o.offerId.toLowerCase().includes(q)) ||
        (o.studentName && o.studentName.toLowerCase().includes(q)) ||
        (o.studentId && o.studentId.toLowerCase().includes(q)) ||
        (o.companyName && o.companyName.toLowerCase().includes(q)) ||
        (o.jobTitle && o.jobTitle.toLowerCase().includes(q))
      );
    });
  }, [offers, offerSearch]);

  // Bar chart data from overview report CTC distribution (Phase 6.1)
  const salaryTierData = (overview?.ctcDistribution || []).map((t, idx) => ({
    name: t.tier,
    value: t.count,
    color: idx === 0 ? "bg-emerald-600" : idx === 1 ? "bg-indigo-600" : "bg-amber-600",
  }));

  // Canonical Oracle application stages for DonutChart (Phase 6.2)
  const STAGE_METADATA = {
    ACCEPTED: { name: "Accepted", color: "#059669", order: 1 },
    OFFERED: { name: "Offered", color: "#10b981", order: 2 },
    SELECTED: { name: "Selected", color: "#0d9488", order: 3 },
    INTERVIEWING: { name: "Interviewing", color: "#2563eb", order: 4 },
    SHORTLISTED: { name: "Shortlisted", color: "#7c3aed", order: 5 },
    APPLIED: { name: "Applied", color: "#d97706", order: 6 },
    DECLINED: { name: "Declined", color: "#64748b", order: 7 },
    REJECTED: { name: "Rejected", color: "#e11d48", order: 8 },
  };

  const appStatusCounts = overview?.applicationStatusCounts || {};
  const statusDonutData = Object.entries(appStatusCounts)
    .map(([statusKey, val]) => {
      const upperKey = statusKey.trim().toUpperCase();
      const meta = STAGE_METADATA[upperKey] || {
        name: upperKey.charAt(0) + upperKey.slice(1).toLowerCase(),
        color: "#94a3b8",
        order: 99,
      };
      return {
        name: meta.name,
        value: Number(val) || 0,
        color: meta.color,
        order: meta.order,
      };
    })
    .filter((d) => d.value > 0)
    .sort((a, b) => a.order - b.order);

  // Branch-wise placement share data (Phase 6.3)
  const branchPlacementData = (overview?.programPlacement || [])
    .filter((p) => p.placed > 0)
    .map((p) => ({
      name: p.programName,
      value: p.placed,
      enrolled: p.enrolled,
      rate: p.rate,
    }))
    .sort((a, b) => b.value - a.value);

  // Preset queries for database console
  const PRESET_QUERIES = [
    { key: "DQL_JOIN", label: "5-Table INNER JOIN", type: "DQL" },
    { key: "DQL_GROUP_HAVING", label: "GROUP BY & HAVING", type: "DQL" },
    { key: "DQL_DENSE_RANK", label: "DENSE_RANK Analytic", type: "DQL" },
    { key: "PLSQL_PKG_REPORTS", label: "PKG_PLACEMENT_REPORTS", type: "PL/SQL" },
    { key: "TCL_SCENARIO", label: "Transaction / Savepoint", type: "TCL" },
  ];

  if (loading && !overview) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Placement Analytics from Oracle Database...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Toast Notification */}
      {notification && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between text-xs shadow-xs ${
            notification.type === "success"
              ? "bg-emerald-50 text-emerald-900 border-emerald-300"
              : "bg-rose-50 text-rose-900 border-rose-300"
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === "success" ? (
              <CheckCircle className="w-4 h-4 text-emerald-600" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600" />
            )}
            <span>{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="font-bold hover:opacity-75 ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Header & Subtitle */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Placement Cell Administrator Console
            </h1>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-300 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Oracle Database 23ai Connected
            </span>
            <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-300">
              Authority: Dean / DBA
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Authoritative university placement reporting generated directly from Oracle PL/SQL package functions and relation tables.
          </p>
        </div>

        {/* Tab Navigation & Refresh Button */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => {
              loadData();
              loadRecruiters();
              loadSqlAudit();
            }}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold shadow-2xs transition-all shrink-0 cursor-pointer"
            title="Reload all metrics and tables directly from Oracle Database"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-blue-600 ${loading ? "animate-spin" : ""}`} />
            <span>Refresh from Oracle</span>
          </button>

          <div className="flex flex-wrap bg-slate-100 p-1 rounded-lg text-xs font-medium text-slate-600">
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
                className={`px-3 py-1.5 rounded-md transition-all cursor-pointer ${
                  activeTab === tab.id
                    ? "bg-white text-slate-900 font-bold shadow-2xs"
                    : "hover:text-slate-900"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* FIRST ROW: KPI METRIC CARDS (Phase 5.2 & 5.3) */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3.5">
        <MetricCard
          title="Eligible Students"
          value={overview?.eligibleStudents ?? overview?.totalStudents ?? 0}
          subtitle="Verified Registrations"
          note="Total Candidates"
          icon={Users}
          color="slate"
        />

        <MetricCard
          title="Students Placed"
          value={overview?.uniqueStudentsPlaced || 0}
          subtitle="Unique Hired Candidates"
          note="Offer Verified"
          icon={CheckCircle2}
          color="emerald"
        />

        <MetricCard
          title="Placement Rate"
          value={`${overview?.placementRate ? overview.placementRate.toFixed(1) : "0.0"}%`}
          subtitle="PL/SQL Function Output"
          note="GET_PLACEMENT_RATE"
          icon={TrendingUp}
          color="emerald"
        />

        <MetricCard
          title="Average Package"
          value={`₹ ${overview?.averagePackage ? overview.averagePackage.toFixed(2) : "0.00"} L`}
          subtitle="Mean CTC Across Offers"
          note="GET_AVERAGE_PACKAGE"
          icon={Award}
          color="blue"
        />

        <MetricCard
          title="Highest Package"
          value={`₹ ${overview?.highestPackage ? overview.highestPackage.toFixed(2) : "0.00"} L`}
          subtitle="Top CTC Released"
          note="Campus Super Dream"
          icon={Briefcase}
          color="purple"
        />

        <MetricCard
          title="Offers Released"
          value={overview?.totalOffers || 0}
          subtitle="Total Offer Letters"
          note="OFFER_LETTER table"
          icon={FileText}
          color="indigo"
        />
      </div>

      {/* TAB 1: OVERVIEW & ANALYTICS */}
      {activeTab === "overview" && (
        <div className="space-y-8">
          {/* SECOND ROW: PLACEMENT ANALYTICS (Phase 5.3 Row 2 & Phase 6.1, 6.2) */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Compensation Tier Distribution (Phase 6.1) */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                      <Award className="w-4 h-4 text-emerald-600" />
                      Compensation Tier Distribution
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Live offer categorization: Super Dream (≥ ₹20 LPA), Dream (₹10–20 LPA), Regular (&lt; ₹10 LPA)
                    </p>
                  </div>
                  <span className="text-xs font-mono font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded border border-slate-300">
                    {overview?.totalOffers || 0} Offers
                  </span>
                </div>
                <div className="pt-2">
                  <BarChart
                    data={salaryTierData}
                    xKey="name"
                    yKey="value"
                    unit="Offers"
                    emptyMessage="No compensation tier records available"
                  />
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500 font-mono">
                <span>Highest Offer: ₹{overview?.highestPackage?.toFixed(2) || "0.00"} LPA</span>
                <span>Average Offer: ₹{overview?.averagePackage?.toFixed(2) || "0.00"} LPA</span>
              </div>
            </div>

            {/* Candidate Application Pipeline (Phase 6.2) */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                      <Layers className="w-4 h-4 text-blue-600" />
                      Candidate Application Pipeline
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Distribution of all {overview?.totalApplications || 0} applications across authoritative Oracle state machine stages
                    </p>
                  </div>
                  <span className="text-xs font-mono font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded border border-slate-300">
                    {overview?.totalApplications || 0} Total Apps
                  </span>
                </div>
                <div className="pt-2">
                  <DonutChart
                    data={statusDonutData}
                    size={190}
                    centerLabel="Total Apps"
                    emptyMessage="No applications currently submitted"
                  />
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span>Unique Hires Placed: <strong className="text-emerald-700 font-mono">{overview?.uniqueStudentsPlaced || 0}</strong></span>
                <span>Interviews Conducted: <strong className="text-slate-800 font-mono">{overview?.totalInterviews || 0}</strong></span>
              </div>
            </div>
          </div>

          {/* THIRD ROW: ACADEMIC AND COMPANY ANALYTICS (Phase 5.3 Row 3 & Phase 6.3, 6.4) */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Branch-wise Placement Distribution (Phase 6.3) */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                      <Users className="w-4 h-4 text-purple-600" />
                      Branch-wise Placement Share
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Proportion of successfully placed candidates across academic disciplines and degree programs
                    </p>
                  </div>
                  <span className="text-xs font-mono font-bold text-purple-800 bg-purple-50 px-2.5 py-1 rounded border border-purple-200">
                    {branchPlacementData.length} Branches Placed
                  </span>
                </div>
                <div className="pt-2">
                  <DonutChart
                    data={branchPlacementData}
                    size={190}
                    centerLabel="Placed Students"
                    emptyMessage="No students placed across academic branches yet"
                  />
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span>Total Placed: <strong className="text-emerald-700 font-mono">{overview?.uniqueStudentsPlaced || 0}</strong></span>
                <span>Active Disciplines: <strong className="text-slate-800 font-mono">{overview?.programPlacement?.length || 0}</strong></span>
              </div>
            </div>

            {/* Corporate Partner Placement Performance (Phase 6.4) */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                      <Building className="w-4 h-4 text-blue-600" />
                      Company Placement Performance
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Hiring activity comparison: drives hosted, applications evaluated, candidates hired, and conversion rate
                    </p>
                  </div>
                  <span className="text-xs font-mono font-bold text-blue-800 bg-blue-50 px-2.5 py-1 rounded border border-blue-200">
                    {companies.length} Partner Companies
                  </span>
                </div>

                <div className="pt-1">
                  <CompanyPerformanceAnalytics companies={overview?.companyAnalytics || []} />
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                <span>Active Drives: <strong className="text-slate-800 font-mono">{overview?.activeDrives || 0}</strong></span>
                <span>Total Corporate Offers: <strong className="text-emerald-700 font-mono">{overview?.totalOffers || 0}</strong></span>
              </div>
            </div>
          </div>

          {/* FOURTH ROW: TIME-BASED ANALYTICS (Phase 5.3 Row 4 & Phase 6.5) */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
              <div>
                <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-emerald-600" />
                  Monthly & Cumulative Placement Timeline
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Chronological progression of verified placement offer letters based on official issuance date (<code className="font-mono text-[11px]">OFFER_LETTER.Offer_Date</code>)
                </p>
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs font-mono font-bold text-slate-700 bg-slate-100 px-3 py-1 rounded border border-slate-300">
                  {overview?.totalOffers || 0} Total Verified Offers
                </span>
              </div>
            </div>

            <div className="pt-2">
              <TimelineChart
                timelineData={overview?.monthlyTimeline || []}
                height={260}
                emptyMessage="No historical offer timeline records recorded in OFFER_LETTER table yet"
              />
            </div>
          </div>

          {/* FIFTH ROW: OPERATIONAL INFORMATION (Phase 5.3 Row 5 & Phase 6.6) */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Recent Placement Activity Stream */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                    <Activity className="w-4 h-4 text-blue-600" />
                    Recent Placement Activity
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Live stream of newly issued offer letters and automated database trigger state transitions
                  </p>
                </div>
                <span className="text-xs font-mono font-bold text-slate-600 bg-slate-100 px-2.5 py-1 rounded">
                  Latest Events
                </span>
              </div>

              <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                {offers.slice(0, 6).map((offer, idx) => (
                  <div
                    key={offer.offerId || idx}
                    className="p-3 rounded-lg border border-slate-100 bg-slate-50/60 hover:bg-white hover:border-slate-200 transition-all flex items-center justify-between gap-3 text-xs"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-full bg-emerald-100 border border-emerald-200 text-emerald-800 flex items-center justify-center font-bold text-xs shrink-0">
                        ₹
                      </div>
                      <div className="min-w-0">
                        <div className="font-bold text-slate-900 truncate">
                          {offer.studentName} ({offer.studentId})
                        </div>
                        <div className="text-[11px] text-slate-500 truncate">
                          {offer.companyName} • {offer.jobTitle}
                        </div>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <div className="font-mono font-bold text-emerald-700">
                        {formatPackage(offer.ctcLpa)}
                      </div>
                      <div className="text-[10px] text-slate-400 font-mono">
                        {offer.offerDate}
                      </div>
                    </div>
                  </div>
                ))}

                {offers.length === 0 && (
                  <div className="py-8 text-center text-xs text-slate-400 italic">
                    No recent placement offers issued yet.
                  </div>
                )}
              </div>
            </div>

            {/* Academic Program Placement Performance Table (Phase 6.6) */}
            <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-purple-600" />
                    Academic Program Performance Table
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Registration count, placed candidates, and conversion rate by degree stream
                  </p>
                </div>
                <span className="text-xs font-mono font-bold text-purple-700 bg-purple-50 px-2.5 py-1 rounded border border-purple-200">
                  {overview?.programPlacement?.length || 0} Programs
                </span>
              </div>

              <div className="overflow-x-auto max-h-96 overflow-y-auto rounded-lg border border-slate-100">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider sticky top-0">
                    <tr>
                      <th className="px-3.5 py-2.5">Program Name</th>
                      <th className="px-3 py-2.5 text-center">Enrolled</th>
                      <th className="px-3 py-2.5 text-center">Placed</th>
                      <th className="px-3.5 py-2.5 text-right">Placement Rate</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                    {(overview?.programPlacement || []).map((prog, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/70 transition-colors">
                        <td className="px-3.5 py-2.5 font-bold text-slate-900">{prog.programName}</td>
                        <td className="px-3 py-2.5 text-center font-mono text-slate-600">{prog.enrolled}</td>
                        <td className="px-3 py-2.5 text-center font-mono font-bold text-emerald-700">{prog.placed}</td>
                        <td className="px-3.5 py-2.5 text-right font-mono font-bold text-slate-900">
                          {prog.rate ? prog.rate.toFixed(1) : "0.0"}%
                        </td>
                      </tr>
                    ))}
                    {(!overview?.programPlacement || overview.programPlacement.length === 0) && (
                      <tr>
                        <td colSpan={4} className="text-center py-8 text-xs text-slate-400 italic">
                          No academic program registration records found.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: STUDENTS DIRECTORY (Phase 9) */}
      {activeTab === "students" && (
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-2 border-b border-slate-100">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Users className="w-5 h-5 text-blue-600" />
                Registered Students Directory ({filteredStudents.length} of {students.length})
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Authoritative academic records, CGPA cutoffs, and university placement recruitment status
              </p>
            </div>

            {/* Filter controls */}
            <div className="flex flex-wrap items-center gap-2.5">
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  value={studentSearch}
                  onChange={(e) => setStudentSearch(e.target.value)}
                  placeholder="Search name, ID, branch..."
                  className="pl-8 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-slate-500 w-56"
                />
              </div>

              <select
                value={studentBranchFilter}
                onChange={(e) => setStudentBranchFilter(e.target.value)}
                className="px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 font-medium focus:outline-none"
              >
                <option value="ALL">All Branches</option>
                {distinctBranches.map((b) => (
                  <option key={b} value={b}>{b}</option>
                ))}
              </select>

              <select
                value={studentStatusFilter}
                onChange={(e) => setStudentStatusFilter(e.target.value)}
                className="px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 font-medium focus:outline-none"
              >
                <option value="ALL">All Statuses</option>
                <option value="PLACED">Placed Candidates Only</option>
                <option value="PARTICIPATING">Actively Participating</option>
              </select>
            </div>
          </div>

          <div className="overflow-x-auto rounded-lg border border-slate-200">
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
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filteredStudents.map((s) => {
                  const studentOffer = offers.find((o) => o.studentId === s.studentId);
                  return (
                    <tr key={s.studentId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{s.studentId}</td>
                      <td className="px-4 py-3">
                        <div className="font-bold text-slate-900">{s.name}</div>
                        <div className="text-[11px] text-slate-500 font-mono">{s.email}</div>
                      </td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">
                        {s.cgpa ? Number(s.cgpa).toFixed(2) : "—"}
                      </td>
                      <td className="px-4 py-3 text-slate-600">{s.branch || "—"}</td>
                      <td className="px-4 py-3 text-slate-500">
                        {s.city && s.state ? `${s.city}, ${s.state}` : (s.city || s.state || "—")}
                      </td>
                      <td className="px-4 py-3">
                        {studentOffer ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[11px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-300">
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            Placed ({formatPackage(studentOffer.ctcLpa)})
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-600 border border-slate-200">
                            Participating
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {filteredStudents.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center py-10 text-xs text-slate-400 italic">
                      No students match the specified search or filter parameters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: COMPANIES & DRIVES (Phase 9) */}
      {activeTab === "drives" && (
        <div className="space-y-6">
          {/* Active Placement Drives */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Briefcase className="w-5 h-5 text-blue-600" />
                  Campus Placement Drives ({filteredDrives.length})
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Published placement opportunities, minimum academic cutoffs, and compensation packages
                </p>
              </div>

              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  value={driveSearch}
                  onChange={(e) => setDriveSearch(e.target.value)}
                  placeholder="Search drive, company, role..."
                  className="pl-8 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-slate-500 w-56"
                />
              </div>
            </div>

            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Drive ID</th>
                    <th className="px-4 py-3">Company Name</th>
                    <th className="px-4 py-3">Job Title / Role</th>
                    <th className="px-4 py-3">Min CGPA</th>
                    <th className="px-4 py-3">Package (CTC)</th>
                    <th className="px-4 py-3">Openings</th>
                    <th className="px-4 py-3">Drive Date</th>
                    <th className="px-4 py-3">Deadline</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                  {filteredDrives.map((d) => (
                    <tr key={d.driveId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{d.driveId}</td>
                      <td className="px-4 py-3 font-bold text-slate-900">{d.companyName}</td>
                      <td className="px-4 py-3 text-slate-700">{d.jobTitle}</td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">{d.minCgpa?.toFixed(2)}</td>
                      <td className="px-4 py-3 font-mono font-bold text-emerald-700">{formatPackage(d.ctc ?? d.packageLpa)}</td>
                      <td className="px-4 py-3 font-mono text-slate-700">{d.openings}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{d.driveDate}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{d.applicationDeadline}</td>
                    </tr>
                  ))}
                  {filteredDrives.length === 0 && (
                    <tr>
                      <td colSpan={8} className="text-center py-8 text-xs text-slate-400 italic">
                        No placement drives found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Partner Companies */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Building className="w-5 h-5 text-indigo-600" />
                Corporate Recruitment Partners ({companies.length})
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Registered hiring organizations authorized in the university placement registry
              </p>
            </div>

            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Company ID</th>
                    <th className="px-4 py-3">Company Name</th>
                    <th className="px-4 py-3">Industry Domain</th>
                    <th className="px-4 py-3">Recruiter Contact</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                  {companies.map((c) => (
                    <tr key={c.companyId} className="hover:bg-slate-50/70 transition-colors">
                      <td className="px-4 py-3 font-mono font-bold text-slate-800">{c.companyId}</td>
                      <td className="px-4 py-3 font-bold text-slate-900">{c.companyName}</td>
                      <td className="px-4 py-3 text-slate-600">{c.industry}</td>
                      <td className="px-4 py-3 text-slate-500 font-mono">{c.email}</td>
                    </tr>
                  ))}
                  {companies.length === 0 && (
                    <tr>
                      <td colSpan={4} className="text-center py-8 text-xs text-slate-400 italic">
                        No corporate partners registered.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: RELEASED OFFERS (Phase 9) */}
      {activeTab === "offers" && (
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Award className="w-5 h-5 text-emerald-600" />
                Issued Placement Offers ({filteredOffers.length} of {offers.length})
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Official offer letters recorded in <code className="font-mono text-[11px]">OFFER_LETTER</code> table via PL/SQL procedure <code className="font-mono text-[11px]">ISSUE_OFFER</code>
              </p>
            </div>

            <div className="flex items-center gap-2.5">
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
                <input
                  type="text"
                  value={offerSearch}
                  onChange={(e) => setOfferSearch(e.target.value)}
                  placeholder="Search student, company, offer..."
                  className="pl-8 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-slate-500 w-52"
                />
              </div>

              <button
                type="button"
                onClick={exportOffersCsv}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-800 border border-emerald-300 hover:bg-emerald-100 font-bold text-xs transition-colors shadow-2xs"
                title="Download CSV export of all released offer letters"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Export CSV</span>
              </button>

              <span className="text-xs font-mono font-bold px-3 py-1.5 bg-slate-100 text-slate-800 rounded-lg border border-slate-200">
                Total Value: {formatPackage(offers.reduce((sum, o) => sum + (parseFloat(o.ctcLpa) || 0), 0))}
              </span>
            </div>
          </div>

          <div className="overflow-x-auto rounded-lg border border-slate-200">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Offer ID</th>
                  <th className="px-4 py-3">Candidate</th>
                  <th className="px-4 py-3">Recruiter Company</th>
                  <th className="px-4 py-3">Role / Designation</th>
                  <th className="px-4 py-3">Compensation (CTC)</th>
                  <th className="px-4 py-3">Offer Date</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                {filteredOffers.map((o) => (
                  <tr key={o.offerId} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-4 py-3 font-mono font-bold text-emerald-800">{o.offerId}</td>
                    <td className="px-4 py-3">
                      <div className="font-bold text-slate-900">{o.studentName}</div>
                      <div className="text-[11px] font-mono text-slate-500">{o.studentId}</div>
                    </td>
                    <td className="px-4 py-3 font-bold text-slate-800">{o.companyName}</td>
                    <td className="px-4 py-3 text-slate-600">{o.jobTitle}</td>
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">{formatPackage(o.ctcLpa)}</td>
                    <td className="px-4 py-3 text-slate-500 font-mono">{o.offerDate}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        o.status === "ACCEPTED"
                          ? "bg-emerald-100 text-emerald-800 border border-emerald-300"
                          : o.status === "DECLINED"
                          ? "bg-slate-100 text-slate-600 border border-slate-200"
                          : "bg-amber-100 text-amber-800 border border-amber-300"
                      }`}>
                        {o.status || "OFFERED"}
                      </span>
                    </td>
                  </tr>
                ))}
                {filteredOffers.length === 0 && (
                  <tr>
                    <td colSpan={7} className="text-center py-10 text-xs text-slate-400 italic">
                      No placement offer records match the search criteria.
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
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Terminal className="w-5 h-5 text-slate-700" />
                  Interactive Database Console (DA2 Syllabus Demonstrator)
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Executes live SQL joins, aggregation with HAVING, analytic window functions, and PL/SQL packages against the database engine.
                </p>
              </div>
              <Link
                to="/admin/sql-compiler"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-slate-900 text-white font-bold text-xs hover:bg-slate-800 transition-all shadow-xs shrink-0"
              >
                <Terminal className="w-4 h-4 text-emerald-400" />
                <span>Open Dedicated SQL Compiler</span>
                <ExternalLink className="w-3.5 h-3.5 opacity-75" />
              </Link>
            </div>

            {/* Preset Query Buttons */}
            <div className="flex flex-wrap gap-2">
              {PRESET_QUERIES.map((q) => (
                <button
                  key={q.key}
                  onClick={() => handleQuerySelect(q.key)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border cursor-pointer ${
                    !isCustomMode && selectedQueryKey === q.key
                      ? "bg-slate-900 text-white border-slate-900 shadow-2xs"
                      : "bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100"
                  }`}
                >
                  <span className="opacity-70 text-[10px] uppercase font-mono mr-1.5">[{q.type}]</span>
                  {q.label}
                </button>
              ))}
              <button
                onClick={() => setIsCustomMode(true)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border cursor-pointer ${
                  isCustomMode
                    ? "bg-slate-900 text-white border-slate-900 shadow-2xs"
                    : "bg-slate-50 text-slate-700 border-slate-300 hover:bg-slate-100"
                }`}
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
                  className="w-full p-3 font-mono text-xs border border-slate-300 rounded-lg bg-slate-900 text-emerald-400 focus:outline-none focus:ring-1 focus:ring-slate-500"
                />
                <button
                  type="submit"
                  disabled={consoleExecuting}
                  className="px-4 py-2 rounded-lg bg-slate-900 text-white font-bold text-xs hover:bg-slate-800 disabled:opacity-50 cursor-pointer"
                >
                  {consoleExecuting ? "Executing Query..." : "Execute Custom Query"}
                </button>
              </form>
            )}

            {/* Executed Query Metadata & SQL Block */}
            {consoleResult && (
              <div className="space-y-3 pt-3 border-t border-slate-100">
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div>
                    <span className="font-bold text-slate-900">{consoleResult.queryTitle}</span>
                    <span className="text-slate-500 ml-2">({consoleResult.category})</span>
                  </div>
                  <div className="flex items-center gap-3 font-mono text-[11px]">
                    <span className="text-slate-500">Time: <strong className="text-slate-800">{consoleResult.executionTimeMs} ms</strong></span>
                    <span className="text-slate-500">Rows: <strong className="text-slate-800">{consoleResult.rowCount}</strong></span>
                    <span className={`px-2 py-0.5 rounded font-bold ${
                      consoleResult.status?.startsWith("SUCCESS")
                        ? "bg-emerald-50 text-emerald-800 border border-emerald-200"
                        : "bg-rose-50 text-rose-800 border border-rose-200"
                    }`}>
                      {consoleResult.status}
                    </span>
                  </div>
                </div>

                <pre className="p-3 bg-slate-900 text-emerald-400 text-xs font-mono rounded-lg overflow-x-auto border border-slate-800">
                  {consoleResult.sql}
                </pre>
                <p className="text-[11px] text-slate-500 italic">{consoleResult.notes}</p>

                {/* Query Results Table */}
                <div className="overflow-x-auto rounded-lg border border-slate-200 max-h-80 overflow-y-auto">
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
                    <tbody className="divide-y divide-slate-100 text-slate-700 font-mono text-[11px]">
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
                          <td colSpan={consoleResult.columns.length || 1} className="text-center py-6 text-slate-400 italic font-sans text-xs">
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
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Clock className="w-5 h-5 text-slate-700" />
                  Database Trigger Audit Trail (APPLICATION_AUDIT)
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Automated audit entries logged by database triggers (<code className="font-mono text-[11px]">TRG_APPLICATION_AUDIT</code> and <code className="font-mono text-[11px]">TRG_OFFER_APPLICATION_STATUS</code>)
                </p>
              </div>
              <span className="text-xs font-mono font-bold px-2.5 py-1 bg-slate-100 text-slate-700 rounded-lg">
                {auditLogs.length} Audit Entries
              </span>
            </div>

            <div className="overflow-x-auto rounded-lg border border-slate-200">
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
                <tbody className="divide-y divide-slate-100 text-slate-700 font-mono text-[11px]">
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
                            className={`px-2 py-0.5 rounded text-[10px] font-bold font-mono ${
                              log.newStatus === "OFFERED"
                                ? "bg-emerald-100 text-emerald-800"
                                : log.newStatus === "SELECTED"
                                ? "bg-teal-100 text-teal-800"
                                : log.newStatus === "INTERVIEWING"
                                ? "bg-blue-100 text-blue-800"
                                : "bg-purple-100 text-purple-800"
                            }`}
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
                      <td colSpan={5} className="text-center py-8 text-xs text-slate-400 italic font-sans">
                        No audit records currently found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Section C: Oracle SQL Compiler Execution Audit Trail */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Terminal className="w-5 h-5 text-purple-700" />
                  SQL Compiler Execution Audit Trail (SQL_EXECUTION_AUDIT)
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Persistent Oracle database audit entries logged by the Admin SQL Compiler with execution latency and outcome status
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={loadSqlAudit}
                  disabled={sqlAuditLoading}
                  className="flex items-center gap-1 text-slate-600 hover:text-slate-900 text-xs font-semibold px-2.5 py-1 rounded-lg border border-slate-200 bg-slate-50 cursor-pointer"
                  title="Refresh SQL Execution Audit Log"
                >
                  <RefreshCw className={`w-3 h-3 ${sqlAuditLoading ? "animate-spin" : ""}`} />
                  <span>Refresh</span>
                </button>
                <span className="text-xs font-mono font-bold px-2.5 py-1 bg-purple-50 text-purple-700 rounded-lg border border-purple-200">
                  {sqlAuditLogs.length} SQL Audit Entries
                </span>
              </div>
            </div>

            <div className="overflow-x-auto rounded-lg border border-slate-200">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-3">Audit ID</th>
                    <th className="px-4 py-3">Executed At</th>
                    <th className="px-4 py-3">Admin</th>
                    <th className="px-4 py-3">Type</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Rows</th>
                    <th className="px-4 py-3">Latency</th>
                    <th className="px-4 py-3">SQL Query</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700 font-mono text-[11px]">
                  {sqlAuditLogs.map((log) => (
                    <tr key={log.auditId} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-bold text-slate-500">#{log.auditId}</td>
                      <td className="px-4 py-3 whitespace-nowrap text-slate-500">
                        {log.executedAt ? new Date(log.executedAt).toLocaleString() : "—"}
                      </td>
                      <td className="px-4 py-3 font-bold text-slate-800">{log.adminId}</td>
                      <td className="px-4 py-3">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-800 font-bold text-[10px]">
                          {log.statementType}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            log.status === "SUCCESS"
                              ? "bg-emerald-100 text-emerald-800"
                              : log.status === "BLOCKED"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-rose-100 text-rose-800"
                          }`}
                        >
                          {log.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        {log.affectedRows !== null && log.affectedRows !== undefined
                          ? `Aff: ${log.affectedRows}`
                          : log.returnedRows !== null && log.returnedRows !== undefined
                          ? `Ret: ${log.returnedRows}`
                          : "—"}
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap font-bold text-slate-800">
                        {log.executionTimeMs} ms
                      </td>
                      <td className="px-4 py-3 max-w-xs truncate text-slate-600" title={log.sqlText}>
                        {log.sqlText}
                      </td>
                    </tr>
                  ))}
                  {sqlAuditLogs.length === 0 && (
                    <tr>
                      <td colSpan={8} className="text-center py-8 text-xs text-slate-400 italic font-sans">
                        {sqlAuditLoading ? "Loading audit records..." : "No SQL compiler audit records found."}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* RECRUITER AUTHORIZATION TAB (Phase 9) */}
      {activeTab === "recruiters" && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-blue-600" />
                  Recruiter Identity & Organization Authorizations
                </h2>
                <p className="text-xs text-slate-500 mt-1">
                  Manage corporate campus recruiters and grant or revoke access to recruiting organizations backed by <code className="font-mono text-[11px]">RECRUITER_COMPANY</code> junction table
                </p>
              </div>
              <span className="text-xs font-mono font-bold px-3 py-1 bg-blue-50 text-blue-700 border border-blue-200 rounded-lg">
                {recruiters.length} Registered Recruiters
              </span>
            </div>

            <div className="mt-6 overflow-x-auto rounded-lg border border-slate-200">
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
                <tbody className="divide-y divide-slate-100 font-medium">
                  {recruiters.map((r) => (
                    <tr key={r.userId} className="hover:bg-slate-50/75 transition-colors">
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
                                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold bg-slate-100 text-slate-800 border border-slate-200"
                              >
                                <span>{c.companyName} ({c.companyId})</span>
                                <button
                                  type="button"
                                  disabled={recruiterActionLoading}
                                  onClick={() => handleRevokeCompany(r.userId, c.companyId)}
                                  className="text-slate-400 hover:text-rose-600 font-bold transition-colors ml-0.5 cursor-pointer"
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
                              className="text-xs border border-slate-300 rounded-lg px-2 py-1 bg-white font-medium focus:outline-none"
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
                              className="px-2.5 py-1 text-xs font-bold bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 cursor-pointer shadow-2xs"
                            >
                              Assign
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setAssigningRecruiterId(null);
                                setSelectedCompanyToAssign("");
                              }}
                              className="px-2 py-1 text-xs text-slate-500 hover:text-slate-800 cursor-pointer"
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
                            className="inline-flex items-center gap-1 px-3 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-700 hover:bg-blue-50 hover:text-blue-700 transition-colors border border-slate-200 cursor-pointer"
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
                      <td colSpan={5} className="text-center py-8 text-xs text-slate-400 italic">
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
