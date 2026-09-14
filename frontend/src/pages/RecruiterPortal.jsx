import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import { formatPackage } from "../lib/formatters";
import Modal from "../components/Modal";
import {
  Building2,
  Users,
  Calendar,
  CheckCircle2,
  UserCheck,
  Award,
  Filter,
  Search,
  ArrowUpRight,
  Phone,
  Mail,
  AlertCircle,
  PlusCircle,
  FileCheck,
  Clock,
  XCircle,
  ChevronRight,
  ExternalLink,
  ShieldAlert,
  FileText,
  Download,
  Star,
  Layers,
  RefreshCw,
} from "lucide-react";

export default function RecruiterPortal() {
  const { user, activeCompany, authorizedCompanies, selectCompany, fetchRecruiterCompanies } = useAuth();
  const navigate = useNavigate();

  const [drives, setDrives] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [applications, setApplications] = useState([]);
  const [students, setStudents] = useState([]);
  const [interviewers, setInterviewers] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filter States
  const [selectedCompanyId, setSelectedCompanyId] = useState(
    activeCompany?.companyId || user?.referenceId || (authorizedCompanies && authorizedCompanies.length > 0 ? authorizedCompanies[0].companyId : "")
  );
  const [selectedDriveId, setSelectedDriveId] = useState("ALL");
  const [activePipelineTab, setActivePipelineTab] = useState("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [notification, setNotification] = useState(null);

  // Modals
  const [scheduleModalApp, setScheduleModalApp] = useState(null);
  const [scoreModalApp, setScoreModalApp] = useState(null);
  const [offerModalApp, setOfferModalApp] = useState(null);
  const [viewOfferModalData, setViewOfferModalData] = useState(null);
  const [evaluationModalApp, setEvaluationModalApp] = useState(null);
  const [candidateResume, setCandidateResume] = useState(null);
  const [loadingResume, setLoadingResume] = useState(false);

  // ATS Evaluation Rubric Form State
  const [evalTechScore, setEvalTechScore] = useState(4);
  const [evalEduScore, setEvalEduScore] = useState(4);
  const [evalProjScore, setEvalProjScore] = useState(4);
  const [evalOverallScore, setEvalOverallScore] = useState(4);
  const [evalRecommendation, setEvalRecommendation] = useState("SHORTLIST");
  const [evalComments, setEvalComments] = useState("");

  // Recruiter Roles Management State
  const [roleStats, setRoleStats] = useState([]);
  const [activeMainTab, setActiveMainTab] = useState("ATS"); // "ATS" or "ROLES"

  // Schedule Interview Form State
  const [selectedRoundNo, setSelectedRoundNo] = useState(1);
  const [selectedInterviewer, setSelectedInterviewer] = useState("");
  const [interviewMode, setInterviewMode] = useState("Online");

  // Record Scores Form State
  const [activeInterviewToScore, setActiveInterviewToScore] = useState(null);
  const [roundScore, setRoundScore] = useState("");
  const [scoreOutcome, setScoreOutcome] = useState("CLEARED");

  // Offer Form State
  const [ctcLpa, setCtcLpa] = useState("");
  const [offerDate, setOfferDate] = useState(new Date().toISOString().split("T")[0]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [drvs, comps, apps, stus, ivs, allIvs, ofrs, rStats] = await Promise.all([
        api.getDrives(),
        api.getCompanies(),
        api.getApplications(),
        api.getAllStudents(),
        api.getInterviewers(),
        api.getInterviews(),
        api.getOffers(),
        api.getRecruiterRoleStats(selectedCompanyId),
      ]);

      setDrives(drvs || []);
      setCompanies(comps || []);
      setApplications(apps || []);
      setStudents(stus || []);
      setInterviewers(ivs || []);
      setInterviews(allIvs || []);
      setOffers(ofrs || []);
      setRoleStats(rStats || []);
    } catch (err) {
      console.error("Failed to load recruiter workspace:", err);
      setNotification({
        type: "error",
        message: err.message || "Unable to connect to database backend.",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    const handleMutation = () => {
      loadData();
    };
    window.addEventListener("portal:database-mutation", handleMutation);
    return () => {
      window.removeEventListener("portal:database-mutation", handleMutation);
    };
  }, []);

  // Sync selectedCompanyId when activeCompany updates
  useEffect(() => {
    if (activeCompany?.companyId && activeCompany.companyId !== selectedCompanyId) {
      setSelectedCompanyId(activeCompany.companyId);
    }
  }, [activeCompany]);

  // Refetch role stats when selectedCompanyId changes
  useEffect(() => {
    if (selectedCompanyId && selectedCompanyId !== "ALL") {
      api.getRecruiterRoleStats(selectedCompanyId)
        .then((rStats) => setRoleStats(rStats || []))
        .catch((err) => console.error("Failed to load role stats for company:", err));
    }
  }, [selectedCompanyId]);

  // Sync selected interviewer when round or interviewers change
  useEffect(() => {
    const roundNumber = Number(selectedRoundNo);
    const validInterviewers = interviewers.filter((iv) => iv.interviewRoundNo === roundNumber);
    if (validInterviewers.length > 0) {
      setSelectedInterviewer(validInterviewers[0].interviewerName);
    } else {
      setSelectedInterviewer("");
    }
  }, [selectedRoundNo, interviewers]);

  // Current selected company metadata
  const currentCompany =
    companies.find((c) => c.companyId === selectedCompanyId) ||
    (authorizedCompanies && authorizedCompanies.find((c) => c.companyId === selectedCompanyId)) ||
    activeCompany || {
      companyId: selectedCompanyId || "—",
      companyName: activeCompany?.companyName || user?.name || "Recruiting Organization",
      industry: activeCompany?.industry || "Technology & Services",
      email: activeCompany?.email || user?.email || "—",
      phoneNumbers: activeCompany?.phoneNumbers || "",
    };

  // Build maps for fast lookups
  const studentsMap = new Map(students.map((s) => [s.studentId, s]));
  const drivesMap = new Map(drives.map((d) => [d.driveId, d]));
  const offersByAppId = new Map(offers.map((o) => [o.applicationId, o]));

  // Active drives for the selected company
  const companyDrives = drives.filter(
    (d) => selectedCompanyId === "ALL" || d.companyId === selectedCompanyId
  );
  const companyDriveIds = new Set(companyDrives.map((d) => d.driveId));

  // Applications belonging to this company\'s drives
  const companyApplications = applications.filter((app) => {
    if (selectedCompanyId === "ALL") return true;
    return companyDriveIds.has(app.driveId);
  });

  // Filtered by drive dropdown, pipeline status tab, and text search
  const filteredApplications = companyApplications.filter((app) => {
    const matchesDrive = selectedDriveId === "ALL" || app.driveId === selectedDriveId;
    const matchesTab = activePipelineTab === "ALL" || app.status === activePipelineTab;
    const matchesQuery =
      searchQuery === "" ||
      (app.studentName && app.studentName.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (app.studentId && app.studentId.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (app.jobTitle && app.jobTitle.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesDrive && matchesTab && matchesQuery;
  });

  // Counts for pipeline tabs
  const tabCounts = {
    ALL: companyApplications.length,
    APPLIED: companyApplications.filter((a) => a.status === "APPLIED").length,
    SHORTLISTED: companyApplications.filter((a) => a.status === "SHORTLISTED").length,
    INTERVIEWING: companyApplications.filter((a) => a.status === "INTERVIEWING").length,
    SELECTED: companyApplications.filter((a) => a.status === "SELECTED").length,
    OFFERED: companyApplications.filter((a) => a.status === "OFFERED").length,
    REJECTED: companyApplications.filter((a) => a.status === "REJECTED").length,
  };

  // State Machine Handlers
  const handleShortlist = async (app) => {
    try {
      await api.updateApplicationStatus(app.applicationId, "SHORTLISTED");
      setNotification({
        type: "success",
        message: "Candidate " + app.studentName + " (" + app.studentId + ") marked as SHORTLISTED.",
      });
      await loadData();
    } catch (err) {
      setNotification({ type: "error", message: err.message });
    }
  };

  const handleReject = async (app) => {
    if (!window.confirm("Are you sure you want to mark application " + app.applicationId + " as REJECTED?")) {
      return;
    }
    try {
      await api.updateApplicationStatus(app.applicationId, "REJECTED");
      setNotification({
        type: "success",
        message: "Application " + app.applicationId + " transitioned to REJECTED.",
      });
      await loadData();
    } catch (err) {
      setNotification({ type: "error", message: err.message });
    }
  };

  const openScheduleModal = (app) => {
    setScheduleModalApp(app);
    const appInterviews = interviews.filter((i) => i.applicationId === app.applicationId);
    const scheduledRounds = new Set(
      appInterviews.map((iv) => iv.roundNo || iv.interviewRoundNo || 1)
    );

    let nextRound = 1;
    if (!scheduledRounds.has(1)) {
      nextRound = 1;
    } else if (!scheduledRounds.has(2)) {
      nextRound = 2;
    } else if (!scheduledRounds.has(3)) {
      nextRound = 3;
    } else {
      nextRound = 1;
    }

    setSelectedRoundNo(nextRound);
    const matchingInterviewer = interviewers.find(
      (iv) => (iv.interviewRoundNo || iv.roundNo) === nextRound
    );
    setSelectedInterviewer(matchingInterviewer ? matchingInterviewer.interviewerName : (interviewers[0]?.interviewerName || ""));
    setInterviewMode("Online");
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    if (!scheduleModalApp || !selectedInterviewer) return;
    setIsSubmitting(true);
    try {
      const rNo = Number(selectedRoundNo);
      await api.scheduleInterview({
        applicationId: scheduleModalApp.applicationId,
        interviewerName: selectedInterviewer,
        oa: rNo === 1,
        gd: rNo === 2,
        hr: rNo === 3,
        result: "PENDING",
        online: interviewMode === "Online",
        offline: interviewMode === "Offline",
      });
      setNotification({
        type: "success",
        message: "Interview successfully scheduled with " + selectedInterviewer + " (Round " + rNo + "). Status updated to INTERVIEWING.",
      });
      setScheduleModalApp(null);
      await loadData();
    } catch (err) {
      setNotification({ type: "error", message: err.message || "Scheduling failed" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const openScoreModal = (app) => {
    setScoreModalApp(app);
    const appInterviews = interviews.filter((i) => i.applicationId === app.applicationId);
    if (appInterviews.length > 0) {
      const pendingRound = appInterviews.find((iv) => !iv.result || iv.result === "PENDING");
      setActiveInterviewToScore(pendingRound || appInterviews[appInterviews.length - 1]);
    } else {
      setActiveInterviewToScore(null);
    }
    setRoundScore(88);
    setScoreOutcome("CLEARED");
  };

  const handleScoreSubmit = async (e) => {
    e.preventDefault();
    if (!scoreModalApp) return;

    const interviewerName = activeInterviewToScore?.interviewerName || selectedInterviewer || interviewers[0]?.interviewerName;
    const assignedRound = activeInterviewToScore?.roundNo || activeInterviewToScore?.interviewRoundNo || 1;

    setIsSubmitting(true);
    try {
      await api.recordInterviewResult(scoreModalApp.applicationId, interviewerName, {
        oa: assignedRound === 1 ? parseInt(roundScore, 10) : null,
        gd: assignedRound === 2 ? parseInt(roundScore, 10) : null,
        hr: assignedRound === 3 ? parseInt(roundScore, 10) : null,
        result: scoreOutcome,
      });

      const isAdvancing = scoreOutcome === "CLEARED" || scoreOutcome === "PASSED";
      const nextRoundHint = !isAdvancing
        ? " Application transitioned to REJECTED."
        : assignedRound < 3
        ? ` Candidate cleared Round ${assignedRound}! Please schedule Round ${assignedRound + 1}.`
        : " Candidate cleared all 3 rounds! Application transitioned to SELECTED.";

      setNotification({
        type: isAdvancing ? "success" : "info",
        message: "Round " + assignedRound + " score (" + roundScore + "/100) recorded for " + scoreModalApp.studentName + "." + nextRoundHint,
      });
      setScoreModalApp(null);
      await loadData();
    } catch (err) {
      setNotification({ type: "error", message: err.message || "Failed to record result" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const openOfferModal = (app) => {
    setOfferModalApp(app);
    const drv = drivesMap.get(app.driveId);
    const defaultCtc = drv?.ctc ?? drv?.packageLpa ?? drv?.startingCtcLpa;
    setCtcLpa(defaultCtc ? String(defaultCtc) : "12.0");
    setOfferDate(new Date().toISOString().split("T")[0]);
  };

  const handleOfferSubmit = async (e) => {
    e.preventDefault();
    if (!offerModalApp) return;
    setIsSubmitting(true);
    try {
      const payload = {
        applicationId: offerModalApp.applicationId,
        offerDate: offerDate,
      };
      if (ctcLpa && !isNaN(parseFloat(ctcLpa))) {
        payload.ctcLpa = parseFloat(ctcLpa);
      }
      const offer = await api.issueOffer(payload);
      setNotification({
        type: "success",
        message: `Official Offer ${offer.offerId} (${formatPackage(offer.ctcLpa)}) released successfully! Database trigger TRG_OFFER_APPLICATION_STATUS updated candidate to OFFERED.`,
      });
      setOfferModalApp(null);
      await loadData();
    } catch (err) {
      setNotification({ type: "error", message: err.message || "Failed to issue offer" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const openViewOfferModal = (app) => {
    const offer = offersByAppId.get(app.applicationId);
    if (offer) {
      setViewOfferModalData(offer);
    } else {
      setNotification({
        type: "error",
        message: "No offer record found for application " + app.applicationId + ".",
      });
    }
  };

  const openEvaluationModal = async (app) => {
    setEvaluationModalApp(app);
    setLoadingResume(true);
    setEvalTechScore(4);
    setEvalEduScore(4);
    setEvalProjScore(4);
    setEvalOverallScore(4);
    setEvalRecommendation(app.status === "APPLIED" ? "SHORTLIST" : "INTERVIEW");
    setEvalComments("");
    try {
      const resume = await api.getCurrentResume(app.studentId);
      setCandidateResume(resume);
    } catch (err) {
      setCandidateResume(null);
    } finally {
      setLoadingResume(false);
    }
  };

  const handleEvaluationSubmit = async (e) => {
    e.preventDefault();
    if (!evaluationModalApp) return;
    if (!candidateResume?.resumeId) {
      setNotification({
        type: "error",
        message: "No active resume record found for this candidate in Oracle. A submitted resume is required for ATS evaluation.",
      });
      return;
    }
    setIsSubmitting(true);
    try {
      await api.recordResumeEvaluation({
        resumeId: candidateResume.resumeId,
        applicationId: evaluationModalApp.applicationId,
        recruiterId: user?.username || user?.referenceId || "RECRUITER",
        technicalScore: evalTechScore,
        educationScore: evalEduScore,
        projectScore: evalProjScore,
        experienceScore: 4,
        overallScore: evalOverallScore,
        comments: evalComments ? `[Recommendation: ${evalRecommendation}] ${evalComments}` : `[Recommendation: ${evalRecommendation}]`,
      });

      // Advance candidate state machine if applicable
      if (evalRecommendation === "SHORTLIST" && evaluationModalApp.status === "APPLIED") {
        await api.updateApplicationStatus(evaluationModalApp.applicationId, "SHORTLISTED");
      } else if (evalRecommendation === "INTERVIEW" && (evaluationModalApp.status === "APPLIED" || evaluationModalApp.status === "SHORTLISTED")) {
        await api.updateApplicationStatus(evaluationModalApp.applicationId, "INTERVIEWING");
      } else if (evalRecommendation === "REJECT") {
        await api.updateApplicationStatus(evaluationModalApp.applicationId, "REJECTED");
      }

      setNotification({
        type: "success",
        message: `Evaluation recorded in Oracle database (RESUME_EVALUATION). Candidate recommendation: ${evalRecommendation}.`,
      });
      setEvaluationModalApp(null);
      await loadData();
    } catch (err) {
      setNotification({
        type: "error",
        message: err.message || "Failed to persist resume evaluation.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "OFFERED":
        return "bg-emerald-50 text-emerald-800 border-emerald-300 font-semibold";
      case "SELECTED":
        return "bg-teal-50 text-teal-800 border-teal-300 font-semibold";
      case "INTERVIEWING":
        return "bg-blue-50 text-blue-800 border-blue-300 font-semibold";
      case "SHORTLISTED":
        return "bg-purple-50 text-purple-800 border-purple-300 font-semibold";
      case "REJECTED":
        return "bg-rose-50 text-rose-800 border-rose-300 font-semibold";
      case "APPLIED":
      default:
        return "bg-amber-50 text-amber-800 border-amber-300 font-semibold";
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Applicant Tracking System...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Toast Notification */}
      {notification && (
        <div
          className={"p-4 rounded-lg border flex items-start gap-3 text-xs transition-all " +
            (notification.type === "success"
              ? "bg-emerald-50 text-emerald-900 border-emerald-300"
              : "bg-rose-50 text-rose-900 border-rose-300")}
        >
          {notification.type === "success" ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          )}
          <div className="flex-1">
            <span className="font-bold">
              {notification.type === "success" ? "Recruitment Action Completed" : "Operation Failed"}
            </span>
            <p className="mt-0.5">{notification.message}</p>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-xs font-semibold hover:opacity-75"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Recruiter Empty State Alert if No Authorized Companies */}
      {user?.role === "RECRUITER" && (!authorizedCompanies || authorizedCompanies.length === 0) && !loading && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-6 text-center shadow-sm">
          <ShieldAlert className="w-12 h-12 text-amber-600 mx-auto mb-3" />
          <h3 className="text-base font-bold text-amber-900">Pending Organization Authorization</h3>
          <p className="text-xs text-amber-800 max-w-xl mx-auto mt-1 leading-relaxed">
            Your recruiter account <strong>{user?.name || user?.username}</strong> is active, but you have not yet been granted access to any recruiting company profile. Please contact the Placement Cell Administrator to link your profile to your organization.
          </p>
        </div>
      )}

      {/* Recruiter Header & Company Scope Selector */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-lg bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-700 shrink-0 shadow-sm">
              <Building2 className="w-7 h-7" />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl font-bold text-slate-900">{currentCompany.companyName}</h1>
                <span className="px-2.5 py-0.5 rounded text-xs font-mono font-bold bg-slate-100 text-slate-700 border border-slate-300">
                  {currentCompany.companyId}
                </span>
                <span className="px-2.5 py-0.5 rounded text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                  Campus Recruitment Drive 2026-27
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1 font-medium">
                {currentCompany.industry || "Technology & Services"} • VIT Placement and Training Cell
              </p>
              <div className="mt-2.5 flex flex-wrap items-center gap-4 text-xs text-slate-600">
                <span className="flex items-center gap-1.5 font-semibold text-slate-800">
                  <Users className="w-3.5 h-3.5 text-blue-600" /> Recruiter: {user?.name || user?.username || "Recruiter"}
                </span>
                <span className="flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5 text-slate-400" /> {currentCompany.email || user?.email || "—"}
                </span>
                {currentCompany.phoneNumbers && (
                  <span className="flex items-center gap-1.5">
                    <Phone className="w-3.5 h-3.5 text-slate-400" /> {currentCompany.phoneNumbers}
                  </span>
                )}
                <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 text-[11px] font-medium border border-slate-200">
                  Role: Authorized Recruiter (ATS)
                </span>
              </div>
            </div>
          </div>

          {/* Company Switcher for Recruiter & Admin */}
          <div className="flex flex-col sm:flex-row items-end sm:items-center gap-3">
            <div className="text-right sm:text-left">
              <label className="block text-[11px] font-bold uppercase text-slate-500 mb-1">
                Recruiting Organization Scope
              </label>
              {user?.role === "RECRUITER" ? (
                <div className="flex items-center gap-2">
                  <select
                    value={selectedCompanyId}
                    onChange={(e) => {
                      const compId = e.target.value;
                      setSelectedCompanyId(compId);
                      setSelectedDriveId("ALL");
                      selectCompany(compId);
                    }}
                    disabled={!authorizedCompanies || authorizedCompanies.length <= 1}
                    className="text-xs border border-slate-300 rounded-md px-3 py-2 bg-white text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-slate-500 disabled:bg-slate-50 disabled:text-slate-600"
                  >
                    {authorizedCompanies && authorizedCompanies.length > 0 ? (
                      authorizedCompanies.map((c) => (
                        <option key={c.companyId} value={c.companyId}>
                          {c.companyName} ({c.companyId})
                        </option>
                      ))
                    ) : (
                      <option value="">No Authorized Organizations</option>
                    )}
                  </select>
                  {authorizedCompanies && authorizedCompanies.length > 1 && (
                    <button
                      type="button"
                      onClick={() => navigate("/recruiter/select-company")}
                      className="text-xs font-semibold px-3 py-2 rounded-md bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors flex items-center gap-1.5"
                      title="Switch active organization"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      Switch
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={loadData}
                    disabled={loading}
                    className="text-xs font-semibold px-3 py-2 rounded-md bg-white text-slate-700 border border-slate-300 hover:bg-slate-50 transition-colors flex items-center gap-1.5 shadow-2xs"
                    title="Reload latest applicants and drives directly from Oracle Database"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 text-blue-600 ${loading ? 'animate-spin' : ''}`} />
                    <span>Refresh</span>
                  </button>
                </div>
              ) : (
                <select
                  value={selectedCompanyId}
                  onChange={(e) => {
                    setSelectedCompanyId(e.target.value);
                    setSelectedDriveId("ALL");
                  }}
                  className="text-xs border border-slate-300 rounded-md px-3 py-2 bg-white text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-slate-500"
                >
                  <option value="ALL">All Organizations (Consolidated View)</option>
                  {companies.map((c) => (
                    <option key={c.companyId} value={c.companyId}>
                      {c.companyName} ({c.companyId})
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>
        </div>

        {/* ATS Metric Row */}
        <div className="mt-6 pt-6 border-t border-slate-100 grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Total Applicants
            </span>
            <div className="text-2xl font-bold font-mono text-slate-900 mt-1">
              {companyApplications.length}
            </div>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Shortlisted
            </span>
            <div className="text-2xl font-bold font-mono text-purple-700 mt-1">
              {tabCounts.SHORTLISTED}
            </div>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Interviewing
            </span>
            <div className="text-2xl font-bold font-mono text-blue-700 mt-1">
              {tabCounts.INTERVIEWING}
            </div>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              Offers Released
            </span>
            <div className="text-2xl font-bold font-mono text-emerald-700 mt-1">
              {tabCounts.OFFERED}
            </div>
          </div>
        </div>
      </div>

      {/* Active Campus Drives for this Company */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-slate-600" />
              Active Placement Drives ({companyDrives.length})
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Role requirements and eligibility criteria published on database
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {companyDrives.map((drv) => (
            <div
              key={drv.driveId}
              className="p-4 rounded-lg border border-slate-200 bg-slate-50/50 hover:bg-slate-50 transition-colors"
            >
              <div className="flex items-center justify-between text-xs mb-1.5">
                <span className="font-mono font-bold text-slate-700">{drv.driveId}</span>
                <span className="font-mono font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                  {formatPackage(drv.ctc ?? drv.packageLpa)}
                </span>
              </div>
              <h3 className="font-bold text-slate-900 text-sm">{drv.jobTitle}</h3>
              <p className="text-xs text-slate-600 font-medium mt-0.5">{drv.companyName}</p>
              <div className="mt-3 pt-3 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
                <span>Min CGPA: <strong className="text-slate-900">{drv.minCgpa?.toFixed(2)}</strong></span>
                <span>Openings: <strong className="text-slate-900">{drv.openings}</strong></span>
              </div>
            </div>
          ))}
          {companyDrives.length === 0 && (
            <div className="col-span-3 text-center py-6 text-xs text-slate-500">
              No placement drives registered for this organization scope.
            </div>
          )}
        </div>
      </div>

      {/* Recruiter Workspace Main Tab Switcher */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveMainTab("ATS")}
          className={"px-4 py-2 rounded-md text-xs font-bold transition-all flex items-center gap-2 " +
            (activeMainTab === "ATS"
              ? "bg-slate-900 text-white shadow-sm"
              : "text-slate-600 hover:bg-slate-100")}
        >
          <Users className="w-4 h-4" />
          <span>Candidate Pipeline (ATS)</span>
        </button>
        <button
          onClick={() => setActiveMainTab("ROLES")}
          className={"px-4 py-2 rounded-md text-xs font-bold transition-all flex items-center gap-2 " +
            (activeMainTab === "ROLES"
              ? "bg-slate-900 text-white shadow-sm"
              : "text-slate-600 hover:bg-slate-100")}
        >
          <Layers className="w-4 h-4" />
          <span>Company Roles & Stage Distribution ({roleStats.length})</span>
        </button>
      </div>

      {/* VIEW 1: Company Roles & Openings Table */}
      {activeMainTab === "ROLES" && (
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden space-y-4 p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Layers className="w-4 h-4 text-slate-700" />
                Corporate Placement Roles & Recruitment Progress
              </h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Aggregated stage counts computed live from database applications and interviews
              </p>
            </div>
            <span className="text-xs font-mono font-semibold text-slate-600 bg-slate-100 px-2.5 py-1 rounded border border-slate-300">
              Company Scope: {selectedCompanyId}
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="px-4 py-3 text-left">Drive ID</th>
                  <th className="px-4 py-3 text-left">Job Role</th>
                  <th className="px-4 py-3 text-left">Company</th>
                  <th className="px-4 py-3 text-left">Package</th>
                  <th className="px-4 py-3 text-left">Min CGPA</th>
                  <th className="px-4 py-3 text-left">Openings</th>
                  <th className="px-4 py-3 text-left">Deadline</th>
                  <th className="px-4 py-3 text-left">Applicant Funnel</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {roleStats.map((r) => (
                  <tr key={r.driveId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3.5 font-mono font-bold text-slate-900">{r.driveId}</td>
                    <td className="px-4 py-3.5 font-bold text-slate-900">{r.jobTitle}</td>
                    <td className="px-4 py-3.5 font-medium text-slate-700">{r.companyName}</td>
                    <td className="px-4 py-3.5 font-mono font-bold text-emerald-700">{formatPackage(r.ctc ?? r.packageLpa ?? r.startingCtcLpa)}</td>
                    <td className="px-4 py-3.5 font-mono text-slate-800">{r.minCgpa}</td>
                    <td className="px-4 py-3.5 font-mono text-slate-800">{r.openingsCount}</td>
                    <td className="px-4 py-3.5 text-slate-600">{r.applicationDeadline}</td>
                    <td className="px-4 py-3.5">
                      <div className="flex flex-wrap gap-1 text-[10px] font-mono">
                        <span className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-800 border border-slate-200">
                          Total: {r.totalApplicants}
                        </span>
                        <span className="px-1.5 py-0.5 rounded bg-purple-50 text-purple-800 border border-purple-200">
                          Short: {r.shortlistedCount}
                        </span>
                        <span className="px-1.5 py-0.5 rounded bg-blue-50 text-blue-800 border border-blue-200">
                          Int: {r.interviewingCount}
                        </span>
                        <span className="px-1.5 py-0.5 rounded bg-teal-50 text-teal-800 border border-teal-200">
                          Sel: {r.selectedCount}
                        </span>
                        <span className="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold">
                          Off: {r.offeredCount}
                        </span>
                      </div>
                    </td>
                  </tr>
                ))}
                {roleStats.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-slate-400">
                      No recruitment roles found for the active organization.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* VIEW 2: Candidate Pipeline ATS Workspace */}
      {activeMainTab === "ATS" && (
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Users className="w-4 h-4 text-slate-700" />
              Candidate Recruitment Pipeline (ATS)
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Governed by Candidate Pipeline State Machine (APPLIED ➔ SHORTLISTED ➔ INTERVIEWING ➔ SELECTED ➔ OFFERED)
            </p>
          </div>

          {/* Search & Drive Filter */}
          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search candidate or ID..."
                className="pl-8 pr-3 py-1.5 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-500 w-44"
              />
            </div>
            <select
              value={selectedDriveId}
              onChange={(e) => setSelectedDriveId(e.target.value)}
              className="text-xs border border-slate-300 rounded-md px-2.5 py-1.5 bg-white text-slate-700 focus:outline-none focus:ring-1 focus:ring-slate-500"
            >
              <option value="ALL">All Drives</option>
              {companyDrives.map((d) => (
                <option key={d.driveId} value={d.driveId}>
                  {d.driveId} - {d.jobTitle}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Pipeline Stage Tabs */}
        <div className="flex flex-wrap gap-1 bg-slate-100 p-1 rounded-md text-xs font-medium text-slate-600">
          {[
            { key: "ALL", label: "All Candidates" },
            { key: "APPLIED", label: "Applied" },
            { key: "SHORTLISTED", label: "Shortlisted" },
            { key: "INTERVIEWING", label: "Interviewing" },
            { key: "SELECTED", label: "Selected" },
            { key: "OFFERED", label: "Offered" },
            { key: "REJECTED", label: "Rejected" },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActivePipelineTab(tab.key)}
              className={"px-3 py-1.5 rounded text-xs transition-all flex items-center gap-1.5 " +
                (activePipelineTab === tab.key
                  ? "bg-white text-slate-900 font-bold shadow-sm"
                  : "hover:text-slate-900")}
            >
              <span>{tab.label}</span>
              <span
                className={"px-1.5 py-0.2 rounded-full text-[10px] font-mono " +
                  (activePipelineTab === tab.key
                    ? "bg-slate-200 text-slate-900"
                    : "bg-slate-200/60 text-slate-600")}
              >
                {tabCounts[tab.key]}
              </span>
            </button>
          ))}
        </div>

        {/* Candidate Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3">Candidate</th>
                <th className="px-4 py-3">Academic Info</th>
                <th className="px-4 py-3">Drive & Role</th>
                <th className="px-4 py-3">Current Status</th>
                <th className="px-4 py-3 text-right">Recruitment Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 text-slate-700">
              {filteredApplications.map((app) => {
                const stu = studentsMap.get(app.studentId);
                return (
                  <tr key={app.applicationId} className="hover:bg-slate-50/70 transition-colors">
                    <td className="px-4 py-3.5">
                      <div className="font-bold text-slate-900">{app.studentName}</div>
                      <div className="text-[11px] font-mono text-slate-500">
                        {app.studentId} • {app.applicationId}
                      </div>
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="font-mono font-bold text-slate-900">
                        CGPA: {stu?.cgpa ? stu.cgpa.toFixed(2) : "—"}
                      </div>
                      <div className="text-[11px] text-slate-500">
                        {stu?.branch || "—"}
                      </div>
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-slate-800">{app.jobTitle}</div>
                      <div className="text-[11px] text-slate-500 font-mono">
                        {app.driveId} • {app.companyName}
                      </div>
                    </td>
                    <td className="px-4 py-3.5">
                      <span
                        className={"inline-flex items-center px-2.5 py-0.5 rounded text-[11px] border " + getStatusBadge(app.status)}
                      >
                        {app.status}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* ATS Evaluation Rubric Button */}
                        <button
                          onClick={() => openEvaluationModal(app)}
                          className="px-2.5 py-1 rounded bg-slate-900 text-white hover:bg-slate-800 text-xs font-semibold flex items-center gap-1 transition-colors shadow-sm"
                          title="Evaluate Candidate Resume against Structured Rubric"
                        >
                          <FileText className="w-3.5 h-3.5" />
                          <span>Evaluate</span>
                        </button>

                        {/* State: APPLIED */}
                        {app.status === "APPLIED" && (
                          <>
                            <button
                              onClick={() => handleShortlist(app)}
                              className="px-2.5 py-1 rounded bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 text-xs font-semibold transition-colors"
                            >
                              Shortlist
                            </button>
                            <button
                              onClick={() => handleReject(app)}
                              className="px-2.5 py-1 rounded bg-slate-100 text-rose-700 hover:bg-rose-50 border border-slate-200 text-xs font-semibold transition-colors"
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {/* State: SHORTLISTED */}
                        {app.status === "SHORTLISTED" && (
                          <>
                            <button
                              onClick={() => openScheduleModal(app)}
                              className="px-2.5 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 text-xs font-semibold transition-colors"
                            >
                              Schedule Interview
                            </button>
                            <button
                              onClick={() => handleReject(app)}
                              className="px-2.5 py-1 rounded bg-slate-100 text-rose-700 hover:bg-rose-50 border border-slate-200 text-xs font-semibold transition-colors"
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {/* State: INTERVIEWING */}
                        {app.status === "INTERVIEWING" && (
                          <>
                            <button
                              onClick={() => openScoreModal(app)}
                              className="px-2.5 py-1 rounded bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-300 text-xs font-semibold transition-colors"
                            >
                              Record Result
                            </button>
                            <button
                              onClick={() => openScheduleModal(app)}
                              className="px-2.5 py-1 rounded bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-300 text-xs font-semibold transition-colors"
                            >
                              Next Round
                            </button>
                            <button
                              onClick={() => handleReject(app)}
                              className="px-2.5 py-1 rounded bg-slate-100 text-rose-700 hover:bg-rose-50 border border-slate-200 text-xs font-semibold transition-colors"
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {/* State: SELECTED */}
                        {app.status === "SELECTED" && (
                          <>
                            <button
                              onClick={() => openOfferModal(app)}
                              className="px-3 py-1 rounded bg-emerald-700 text-white hover:bg-emerald-800 text-xs font-semibold shadow-sm transition-colors"
                            >
                              Issue Offer
                            </button>
                            <button
                              onClick={() => handleReject(app)}
                              className="px-2.5 py-1 rounded bg-slate-100 text-rose-700 hover:bg-rose-50 border border-slate-200 text-xs font-semibold transition-colors"
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {/* State: OFFERED */}
                        {app.status === "OFFERED" && (
                          <button
                            onClick={() => openViewOfferModal(app)}
                            className="px-2.5 py-1 rounded bg-slate-100 text-emerald-800 hover:bg-emerald-50 border border-emerald-300 text-xs font-semibold flex items-center gap-1 transition-colors"
                          >
                            <FileCheck className="w-3.5 h-3.5" />
                            View Offer
                          </button>
                        )}

                        {/* State: REJECTED */}
                        {app.status === "REJECTED" && (
                          <span className="text-xs font-medium text-rose-600 flex items-center gap-1">
                            <XCircle className="w-3.5 h-3.5" />
                            Disqualified
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filteredApplications.length === 0 && (
                <tr>
                  <td colSpan={5} className="text-center py-8 text-xs text-slate-500">
                    No candidate applications match the selected criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      )}

      {/* MODAL 1: Schedule Interview (Strictly Pinned Interviewer Model) */}
      <Modal
        isOpen={!!scheduleModalApp}
        onClose={() => setScheduleModalApp(null)}
        title="Schedule Interview Round (PL/SQL SCHEDULE_INTERVIEW)"
      >
        {scheduleModalApp && (
          <form onSubmit={handleScheduleSubmit} className="space-y-4 text-xs">
            <div className="p-3 bg-slate-50 rounded-md border border-slate-200">
              <div className="font-bold text-slate-900">
                Candidate: {scheduleModalApp.studentName} ({scheduleModalApp.studentId})
              </div>
              <div className="text-slate-600 mt-0.5">
                Role: {scheduleModalApp.jobTitle} • Drive: {scheduleModalApp.driveId}
              </div>
            </div>

            {/* Round Selection */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">Select Interview Round</label>
              <select
                value={selectedRoundNo}
                onChange={(e) => setSelectedRoundNo(Number(e.target.value))}
                className="w-full p-2 border border-slate-300 rounded-md bg-white text-xs font-medium text-slate-800 focus:ring-1 focus:ring-slate-500"
              >
                <option value={1}>Round 1: Online Assessment (OA)</option>
                <option value={2}>Round 2: Group Discussion & Problem Solving (GD)</option>
                <option value={3}>Round 3: HR & Behavioral Evaluation (HR)</option>
              </select>
            </div>

            {/* Pinned Interviewer Dropdown */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                Designated Pinned Interviewer (INTERVIEWER_ROUND Schema Rule)
              </label>
              <select
                value={selectedInterviewer}
                onChange={(e) => setSelectedInterviewer(e.target.value)}
                className="w-full p-2 border border-slate-300 rounded-md bg-white text-xs font-medium text-slate-800 focus:ring-1 focus:ring-slate-500"
                required
              >
                {interviewers
                  .filter((iv) => iv.interviewRoundNo === Number(selectedRoundNo))
                  .map((iv, idx) => (
                    <option key={idx} value={iv.interviewerName}>
                      {iv.interviewerName} (Pinned to Round {iv.interviewRoundNo}: {iv.roundName})
                    </option>
                  ))}
              </select>
              <p className="text-[11px] text-slate-500 mt-1">
                * DA1 Normalization constraint: Each interviewer is permanently pinned to exactly one round number.
              </p>
            </div>

            {/* Delivery Mode */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">Interview Delivery Mode</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setInterviewMode("Online")}
                  className={"p-2 rounded-md border text-center font-medium " +
                    (interviewMode === "Online"
                      ? "border-blue-600 bg-blue-50 text-blue-800"
                      : "border-slate-300 text-slate-600 hover:bg-slate-50")}
                >
                  Online (Virtual Meet / Teams)
                </button>
                <button
                  type="button"
                  onClick={() => setInterviewMode("Offline")}
                  className={"p-2 rounded-md border text-center font-medium " +
                    (interviewMode === "Offline"
                      ? "border-blue-600 bg-blue-50 text-blue-800"
                      : "border-slate-300 text-slate-600 hover:bg-slate-50")}
                >
                  Offline (In-Person Campus Lab)
                </button>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-200 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setScheduleModalApp(null)}
                className="px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting || !selectedInterviewer}
                className="px-4 py-1.5 rounded-md bg-slate-900 text-white font-semibold hover:bg-slate-800 transition-colors disabled:opacity-50"
              >
                {isSubmitting ? "Scheduling..." : "Confirm & Schedule"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* MODAL 2: Record Interview Result (Shows ONLY score input for pinned round) */}
      <Modal
        isOpen={!!scoreModalApp}
        onClose={() => setScoreModalApp(null)}
        title="Record Interview Evaluation & Outcome"
      >
        {scoreModalApp && (
          <form onSubmit={handleScoreSubmit} className="space-y-4 text-xs">
            <div className="p-3 bg-slate-50 rounded-md border border-slate-200">
              <div className="font-bold text-slate-900">
                Candidate: {scoreModalApp.studentName} ({scoreModalApp.studentId})
              </div>
              <div className="text-slate-600 mt-0.5">Application: {scoreModalApp.applicationId}</div>
            </div>

            {/* Scheduled Interviews selector if multiple exist */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                Select Scheduled Round to Evaluate
              </label>
              {interviews.filter((i) => i.applicationId === scoreModalApp.applicationId).length > 0 ? (
                <div className="space-y-2">
                  {interviews
                    .filter((i) => i.applicationId === scoreModalApp.applicationId)
                    .map((iv, idx) => (
                      <div
                        key={idx}
                        onClick={() => setActiveInterviewToScore(iv)}
                        className={"p-2.5 rounded-md border cursor-pointer flex items-center justify-between transition-all " +
                          (activeInterviewToScore?.interviewerName === iv.interviewerName
                            ? "border-blue-600 bg-blue-50/50"
                            : "border-slate-200 hover:bg-slate-50")}
                      >
                        <div>
                          <div className="font-bold text-slate-900">
                            Round {iv.interviewRoundNo}: {iv.roundName}
                          </div>
                          <div className="text-[11px] text-slate-500">
                            Interviewer: {iv.interviewerName} • Mode: {iv.online === "Y" ? "Online" : "Offline"}
                          </div>
                        </div>
                        <span className="font-mono font-bold text-slate-700">
                          {iv.result || "PENDING"}
                        </span>
                      </div>
                    ))}
                </div>
              ) : (
                <div className="p-3 bg-amber-50 rounded-md border border-amber-200 text-amber-800 text-xs">
                  No previous interview round scheduled yet. Evaluation will create a completed interview evaluation record.
                </div>
              )}
            </div>

            {/* Specific Score Input for the Round */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">
                {(activeInterviewToScore?.roundNo || activeInterviewToScore?.interviewRoundNo || 1) === 1
                  ? "Round 1: Online Assessment (OA) Score (0 - 100)"
                  : (activeInterviewToScore?.roundNo || activeInterviewToScore?.interviewRoundNo) === 2
                  ? "Round 2: Group Discussion (GD) Score (0 - 100)"
                  : (activeInterviewToScore?.roundNo || activeInterviewToScore?.interviewRoundNo) === 3
                  ? "Round 3: HR & Cultural Fit Score (0 - 100)"
                  : "Round Evaluation Score (0 - 100)"}
              </label>
              <input
                type="number"
                min="0"
                max="100"
                value={roundScore}
                onChange={(e) => setRoundScore(e.target.value)}
                className="w-full p-2 border border-slate-300 rounded-md font-mono font-bold text-sm text-slate-900 focus:ring-1 focus:ring-slate-500"
                required
              />
            </div>

            {/* Outcome Selection */}
            <div>
              <label className="block font-bold text-slate-700 mb-1">Evaluation Outcome</label>
              <select
                value={scoreOutcome}
                onChange={(e) => setScoreOutcome(e.target.value)}
                className="w-full p-2 border border-slate-300 rounded-md bg-white text-xs font-semibold focus:ring-1 focus:ring-slate-500"
              >
                <option value="CLEARED">CLEARED (Clear Round & Proceed)</option>
                <option value="REJECTED">REJECTED (Eliminate Candidate at this Round)</option>
              </select>
              <p className="text-[11px] text-slate-500 mt-1">
                * Candidate must clear all 3 rounds (OA, GD/Tech, HR) to be Selected. Selecting REJECTED eliminates candidate immediately.
              </p>
            </div>

            <div className="pt-4 border-t border-slate-200 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setScoreModalApp(null)}
                className="px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-1.5 rounded-md bg-slate-900 text-white font-semibold hover:bg-slate-800 disabled:opacity-50"
              >
                {isSubmitting ? "Recording..." : "Submit Evaluation"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* MODAL 3: Issue Offer Letter (PL/SQL & Trigger TRG_OFFER_APPLICATION_STATUS) */}
      <Modal
        isOpen={!!offerModalApp}
        onClose={() => setOfferModalApp(null)}
        title="Issue Formal Offer Letter (PL/SQL & Trigger)"
      >
        {offerModalApp && (
          <form onSubmit={handleOfferSubmit} className="space-y-4 text-xs">
            <div className="p-3 bg-emerald-50 rounded-md border border-emerald-200">
              <div className="font-bold text-emerald-900">
                Award Offer: {offerModalApp.studentName} ({offerModalApp.studentId})
              </div>
              <div className="text-emerald-700 mt-0.5">
                Role: {offerModalApp.jobTitle} • {offerModalApp.companyName}
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">
                Annual Compensation Package (CTC in LPA)
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="0.5"
                  min="1"
                  value={ctcLpa}
                  onChange={(e) => setCtcLpa(e.target.value)}
                  className="w-full p-2.5 border border-slate-300 rounded-md text-sm font-mono font-bold text-slate-900 focus:ring-1 focus:ring-slate-500"
                  required
                />
                <span className="absolute right-3 top-2.5 text-xs text-slate-500 font-bold">
                  ₹ Lakhs / Year
                </span>
              </div>
            </div>

            <div>
              <label className="block font-bold text-slate-700 mb-1">Offer Release Date</label>
              <input
                type="date"
                value={offerDate}
                onChange={(e) => setOfferDate(e.target.value)}
                className="w-full p-2 border border-slate-300 rounded-md text-xs font-mono text-slate-800 focus:ring-1 focus:ring-slate-500"
                required
              />
            </div>

            <div className="p-3 bg-slate-50 rounded-md border border-slate-200 space-y-1 text-[11px] text-slate-600">
              <span className="font-bold text-slate-800 block">Database Automation Lifecycle:</span>
              <p>1. Executes Oracle PL/SQL procedure <code className="font-mono text-slate-800 font-semibold">ISSUE_OFFER</code></p>
              <p>2. Fires <code className="font-mono text-slate-800 font-semibold">AFTER INSERT ON OFFER_LETTER</code> database trigger</p>
              <p>3. Application status transitions automatically from <span className="font-bold text-blue-700">SELECTED</span> to <span className="font-bold text-emerald-700">OFFERED</span></p>
              <p>4. Trigger writes permanent entry to <code className="font-mono text-slate-800 font-semibold">APPLICATION_AUDIT</code> table</p>
            </div>

            <div className="pt-4 border-t border-slate-200 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setOfferModalApp(null)}
                className="px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-1.5 rounded-md bg-emerald-700 text-white font-semibold hover:bg-emerald-800 shadow-sm disabled:opacity-50"
              >
                {isSubmitting ? "Issuing Offer..." : "Confirm & Release Offer"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* MODAL 4: View Offer Letter Details */}
      <Modal
        isOpen={!!viewOfferModalData}
        onClose={() => setViewOfferModalData(null)}
        title="Formal Campus Placement Offer Record"
      >
        {viewOfferModalData && (
          <div className="space-y-4 text-xs">
            <div className="border border-slate-200 rounded-lg p-5 bg-white space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">{viewOfferModalData.companyName}</h3>
                  <p className="text-[11px] text-slate-500 font-medium">Campus Recruitment Division</p>
                </div>
                <div className="text-right">
                  <span className="font-mono font-bold text-xs bg-emerald-50 text-emerald-800 px-2 py-0.5 rounded border border-emerald-200">
                    {viewOfferModalData.offerId}
                  </span>
                  <div className="text-[10px] text-slate-400 mt-0.5">
                    Date: {viewOfferModalData.offerDate}
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 py-2 text-xs">
                <div>
                  <span className="text-slate-500 font-medium block">Candidate Name:</span>
                  <strong className="text-slate-900">{viewOfferModalData.studentName}</strong>
                </div>
                <div>
                  <span className="text-slate-500 font-medium block">Candidate ID:</span>
                  <strong className="text-slate-900 font-mono">{viewOfferModalData.studentId}</strong>
                </div>
                <div>
                  <span className="text-slate-500 font-medium block">Designation Offered:</span>
                  <strong className="text-slate-900">{viewOfferModalData.jobTitle}</strong>
                </div>
                <div>
                  <span className="text-slate-500 font-medium block">Annual CTC:</span>
                  <strong className="text-emerald-700 text-sm font-mono">
                    {formatPackage(viewOfferModalData.ctcLpa)}
                  </strong>
                </div>
              </div>

              <div className="p-3 bg-slate-50 rounded border border-slate-200 text-[11px] text-slate-600">
                This document confirms the official placement offer generated by the Oracle PL/SQL engine. The student has been notified and can view the appointment letter in their Student Portal.
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setViewOfferModalData(null)}
                className="px-4 py-1.5 rounded-md bg-slate-800 text-white font-semibold hover:bg-slate-700 text-xs"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* MODAL 5: ATS Resume Evaluation & Feedback Form */}
      <Modal
        isOpen={!!evaluationModalApp}
        onClose={() => setEvaluationModalApp(null)}
        title="ATS Candidate Resume Evaluation (Oracle Table: RESUME_EVALUATION)"
      >
        {evaluationModalApp && (
          <form onSubmit={handleEvaluationSubmit} className="space-y-4 text-xs">
            {/* Candidate Metadata Box */}
            <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Candidate Name:</span>
                <strong className="text-slate-900 text-sm">{evaluationModalApp.studentName}</strong>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Student & Application ID:</span>
                <strong className="text-slate-700 font-mono">{evaluationModalApp.studentId} • {evaluationModalApp.applicationId}</strong>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Applying For:</span>
                <strong className="text-slate-800">{evaluationModalApp.jobTitle} ({evaluationModalApp.companyName})</strong>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Current Pipeline Status:</span>
                <span className={"px-2 py-0.5 rounded text-[10px] uppercase font-bold " + getStatusBadge(evaluationModalApp.status)}>
                  {evaluationModalApp.status}
                </span>
              </div>
            </div>

            {/* Oracle BLOB Resume Stream / Inspection Link */}
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText className="w-5 h-5 text-blue-700 shrink-0" />
                <div>
                  <span className="font-bold text-blue-900 block">Candidate BLOB Resume Document</span>
                  <span className="text-[11px] text-blue-700">
                    {loadingResume
                      ? "Streaming from Oracle Database..."
                      : candidateResume
                      ? `${candidateResume.fileName} (v${candidateResume.versionNo} • ${(candidateResume.fileSizeBytes / 1024).toFixed(1)} KB)`
                      : "Standard University Academic Profile"}
                  </span>
                </div>
              </div>

              {candidateResume ? (
                <a
                  href={api.getResumeDownloadUrl(candidateResume.resumeId)}
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 bg-blue-700 hover:bg-blue-800 text-white rounded text-xs font-bold inline-flex items-center gap-1 transition-colors shrink-0"
                >
                  <Download className="w-3.5 h-3.5" /> Download BLOB
                </a>
              ) : (
                <a
                  href={api.getStudentCurrentResumeDownloadUrl(evaluationModalApp.studentId)}
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 bg-blue-700 hover:bg-blue-800 text-white rounded text-xs font-bold inline-flex items-center gap-1 transition-colors shrink-0"
                >
                  <Download className="w-3.5 h-3.5" /> Fetch Resume
                </a>
              )}
            </div>

            {/* Evaluation Rubric Grid */}
            <div className="border border-slate-200 rounded-lg p-4 space-y-3 bg-white">
              <h4 className="font-bold uppercase tracking-wider text-slate-800 text-[11px] flex items-center gap-1.5">
                <Star className="w-3.5 h-3.5 text-amber-500" />
                Multi-Criteria Evaluation Rubric (1 = Poor, 5 = Exceptional)
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Technical Skills</label>
                  <select
                    value={evalTechScore}
                    onChange={(e) => setEvalTechScore(Number(e.target.value))}
                    className="w-full p-1.5 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-slate-900 bg-white"
                  >
                    <option value={5}>5 - Outstanding</option>
                    <option value={4}>4 - Strong Competency</option>
                    <option value={3}>3 - Acceptable / Meets Bar</option>
                    <option value={2}>2 - Marginal Gaps</option>
                    <option value={1}>1 - Unsatisfactory</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Education Relevance</label>
                  <select
                    value={evalEduScore}
                    onChange={(e) => setEvalEduScore(Number(e.target.value))}
                    className="w-full p-1.5 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-slate-900 bg-white"
                  >
                    <option value={5}>5 - Excellent CGPA & Branch</option>
                    <option value={4}>4 - Meets Target Criteria</option>
                    <option value={3}>3 - Borderline Eligible</option>
                    <option value={2}>2 - Weak Academic Match</option>
                    <option value={1}>1 - Incompatible</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Project Experience</label>
                  <select
                    value={evalProjScore}
                    onChange={(e) => setEvalProjScore(Number(e.target.value))}
                    className="w-full p-1.5 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-slate-900 bg-white"
                  >
                    <option value={5}>5 - Production Grade / High Impact</option>
                    <option value={4}>4 - Solid Full-Stack / ML Projects</option>
                    <option value={3}>3 - Standard Coursework Projects</option>
                    <option value={2}>2 - Basic Tutorials Only</option>
                    <option value={1}>1 - No Projects Found</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Pipeline Action Recommendation</label>
                <select
                  value={evalRecommendation}
                  onChange={(e) => setEvalRecommendation(e.target.value)}
                  className="w-full p-2 border border-slate-300 rounded text-xs font-bold text-slate-900 focus:ring-1 focus:ring-slate-900 bg-white"
                >
                  <option value="SHORTLIST">SHORTLIST (Advance to Candidate Shortlist)</option>
                  <option value="INTERVIEW">INTERVIEW (Schedule Technical Round)</option>
                  <option value="REJECT">REJECT (Disqualify Application)</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Recruiter Evaluation Notes</label>
                <textarea
                  rows={2}
                  value={evalComments}
                  onChange={(e) => setEvalComments(e.target.value)}
                  placeholder="Record assessment notes, strengths, and specific areas to probe in interview rounds..."
                  className="w-full p-2 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-slate-900"
                />
              </div>
            </div>

            <div className="pt-3 border-t border-slate-200 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEvaluationModalApp(null)}
                className="px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-50 font-medium"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-4 py-1.5 rounded-md bg-slate-900 hover:bg-slate-800 text-white font-bold transition-colors shadow-sm disabled:opacity-50"
              >
                {isSubmitting ? "Persisting Evaluation..." : "Commit Evaluation to Oracle"}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
