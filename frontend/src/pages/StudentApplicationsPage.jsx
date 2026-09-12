import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatPackage } from '../lib/formatters';
import { Link, useNavigate } from 'react-router-dom';
import Modal from '../components/Modal';
import {
  FileCheck,
  Briefcase,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
  Award,
  ArrowLeft,
  XCircle,
  Building,
  User,
  ExternalLink,
  ChevronRight,
  Printer,
  Download,
} from 'lucide-react';

export default function StudentApplicationsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const studentId = user?.referenceId;

  const [applications, setApplications] = useState([]);
  const [drives, setDrives] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [selectedOffer, setSelectedOffer] = useState(null);
  const [confirmAcceptOffer, setConfirmAcceptOffer] = useState(null);
  const [acceptingOfferId, setAcceptingOfferId] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  const loadData = async () => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return;
    }

    try {
      setLoading(true);
      setFetchError(null);

      // Primary: must succeed. Uses Spring Security principal — no hardcoded studentId in URL.
      const apps = await api.getMyApplications();
      setApplications(apps || []);

      // Secondary: failures are tolerated — they enhance the view but don't block applications.
      const [drvs, myInterviews, ofrs] = await Promise.all([
        api.getDrives().catch(() => []),
        api.getStudentInterviews(studentId).catch(() => []),
        api.getMyOffers().catch(() => api.getOffers(studentId)),
      ]);
      setDrives(drvs || []);
      setOffers(ofrs || []);
      setInterviews(myInterviews || []);
    } catch (err) {
      console.error('Failed to load application tracker data:', err);
      setFetchError(err.message || 'Failed to load your application pipeline. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [studentId]);


  const pipelineStages = [
    { key: 'APPLIED', label: '1. Applied' },
    { key: 'SHORTLISTED', label: '2. Shortlisted' },
    { key: 'INTERVIEWING', label: '3. Interviewing' },
    { key: 'SELECTED', label: '4. Selected' },
    { key: 'OFFERED', label: '5. Offered' },
  ];

  const getStageIndex = (status) => {
    if (status === 'ACCEPTED') return pipelineStages.length - 1;
    const idx = pipelineStages.findIndex((s) => s.key === status);
    return idx !== -1 ? idx : 0;
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACCEPTED':
        return 'bg-emerald-100 text-emerald-900 border-emerald-400 font-bold';
      case 'DECLINED':
        return 'bg-slate-100 text-slate-600 border-slate-300 font-medium';
      case 'OFFERED':
        return 'bg-emerald-50 text-emerald-800 border-emerald-300';
      case 'SELECTED':
        return 'bg-teal-50 text-teal-800 border-teal-300';
      case 'INTERVIEWING':
        return 'bg-blue-50 text-blue-800 border-blue-300';
      case 'SHORTLISTED':
        return 'bg-purple-50 text-purple-800 border-purple-300';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-800 border-rose-300';
      default:
        return 'bg-amber-50 text-amber-800 border-amber-300';
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Application Pipeline...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Action Notification Alert */}
      {actionMessage && (
        <div
          className={`p-4 rounded-lg border flex items-start gap-3 text-xs transition-all ${
            actionMessage.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-rose-50 text-rose-800 border-rose-200'
          }`}
        >
          {actionMessage.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          )}
          <div className="flex-1">
            <span className="font-bold uppercase tracking-wide block">
              {actionMessage.title || (actionMessage.type === 'success' ? 'Success' : 'Error')}
            </span>
            <p className="mt-0.5">{actionMessage.message}</p>
          </div>
          <button
            onClick={() => setActionMessage(null)}
            className="font-semibold text-slate-500 hover:text-slate-800"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Breadcrumb Navigation */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/student" className="hover:text-slate-900 flex items-center gap-1 font-medium">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Student Dashboard
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Application Pipeline & Interview Tracker</span>
        </div>

        <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
          Total Applications: {applications.length}
        </span>
      </div>

      {/* Header Banner */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Application Pipeline Tracker</h1>
            <p className="text-xs text-slate-500 mt-1">
              Live status progression powered by Oracle PL/SQL state machine (<code className="font-mono text-slate-800">APPLICATION.Status</code>).
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              to="/student/drives"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-md text-xs font-semibold transition-colors shadow-sm"
            >
              <Briefcase className="w-3.5 h-3.5" /> Explore More Drives
            </Link>
          </div>
        </div>
      </div>

      {/* Applications List */}
      <div className="space-y-6">
        {applications.map((app) => {
          const drive = drives.find((d) => d.driveId === app.driveId);
          const appInterviews = (app.interviews && app.interviews.length > 0)
            ? app.interviews
            : interviews.filter((i) => i.applicationId === app.applicationId);
          const offer = offers.find((o) => o.applicationId === app.applicationId);
          const isAccepted = app.status === 'ACCEPTED' || offer?.status === 'ACCEPTED';
          const isDeclined = app.status === 'DECLINED' || offer?.status === 'DECLINED';
          const isRejected = app.status === 'REJECTED';
          const currentIndex = getStageIndex(app.status);

          const oaInterview = appInterviews.find((i) => i.roundNo === 1);
          const techInterview = appInterviews.find((i) => i.roundNo === 2);
          const hrInterview = appInterviews.find((i) => i.roundNo === 3);

          const oaScore = app.scores?.oa ?? oaInterview?.score;
          const techScore = app.scores?.technical ?? techInterview?.score;
          const hrScore = app.scores?.hr ?? hrInterview?.score;
          const overallScore = app.scores?.overall;

          const isOaCompleted = app.scoreCompletion?.oaCompleted ?? (oaScore != null && ['PASSED', 'CLEARED'].includes(oaInterview?.result));
          const isTechCompleted = app.scoreCompletion?.technicalCompleted ?? (techScore != null && ['PASSED', 'CLEARED'].includes(techInterview?.result));
          const isHrCompleted = app.scoreCompletion?.hrCompleted ?? (hrScore != null && ['PASSED', 'CLEARED'].includes(hrInterview?.result));

          const isOaRejected = ['REJECTED', 'FAILED'].includes(oaInterview?.result);
          const isTechRejected = ['REJECTED', 'FAILED'].includes(techInterview?.result);
          const isHrRejected = ['REJECTED', 'FAILED'].includes(hrInterview?.result);

          return (
            <div
              key={app.applicationId}
              className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-6"
            >
              {/* Top Row: Info & Status */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono text-xs font-bold text-slate-500">{app.applicationId}</span>
                    <h2 className="text-base font-bold text-slate-900">
                      {drive ? `${drive.jobTitle} • ${drive.companyName}` : app.driveId}
                    </h2>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-slate-500 mt-1">
                    <span>Applied on: <strong className="text-slate-700">{app.applyDate}</strong></span>
                    {offer ? (
                      <>
                        <span>•</span>
                        <span>Offered Package: <strong className="text-emerald-700 font-mono font-bold">{formatPackage(offer.ctcLpa)}</strong></span>
                      </>
                    ) : drive ? (
                      <>
                        <span>•</span>
                        <span>Package: <strong className="text-slate-900 font-mono">{formatPackage(drive.ctc ?? drive.packageLpa ?? drive.startingCtcLpa)}</strong></span>
                      </>
                    ) : null}
                    {drive && (
                      <>
                        <span>•</span>
                        <span>Drive Date: <strong className="text-slate-700">{drive.driveDate}</strong></span>
                      </>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={`px-3 py-1 rounded text-xs font-bold uppercase tracking-wider border ${getStatusBadge(
                      app.status
                    )}`}
                  >
                    {app.status}
                  </span>

                  {offer && (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setSelectedOffer(offer)}
                        className="inline-flex items-center gap-1 px-3 py-1 bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 rounded text-xs font-semibold transition-colors shadow-xs"
                      >
                        <Award className="w-3.5 h-3.5" /> View Offer Letter
                      </button>
                      {offer.status === 'OFFERED' && (
                        <button
                          onClick={() => setConfirmAcceptOffer(offer)}
                          className="inline-flex items-center gap-1 px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs font-bold transition-colors shadow-sm"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" /> Accept Offer
                        </button>
                      )}
                      {offer.status === 'ACCEPTED' && (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-emerald-100 text-emerald-800 border border-emerald-300 rounded text-xs font-bold">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> Offer Accepted
                        </span>
                      )}
                      {offer.status === 'DECLINED' && (
                        <span className="inline-flex items-center px-2 py-0.5 bg-slate-100 text-slate-500 border border-slate-200 rounded text-xs font-medium">
                          Offer Declined
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>

              {/* Pipeline Status / Visual Progress Stepper */}
              {isAccepted ? (
                <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-lg shadow-xs space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-emerald-600 text-white flex items-center justify-center font-bold shrink-0 shadow-xs">
                        <CheckCircle2 className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold uppercase tracking-wider text-emerald-950">
                            Placement Finalized • Offer Formally Accepted
                          </span>
                          {offer && (
                            <span className="text-[11px] font-mono font-semibold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-300">
                              Ref: {offer.offerId}
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-emerald-800 mt-0.5">
                          You have formally accepted the employment offer for <strong className="text-emerald-950">{drive ? `${drive.jobTitle} • ${drive.companyName}` : app.jobTitle}</strong> with annual compensation <strong className="font-mono text-emerald-950 font-bold">{formatPackage(offer?.ctcLpa ?? drive?.ctc)}</strong>.
                        </p>
                      </div>
                    </div>
                    {offer && (
                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          onClick={() => setSelectedOffer(offer)}
                          className="px-3 py-1.5 bg-white border border-emerald-300 hover:bg-emerald-100/60 text-emerald-900 rounded text-xs font-semibold transition-colors shadow-xs flex items-center gap-1.5"
                        >
                          <Award className="w-3.5 h-3.5 text-emerald-700" /> View Letter
                        </button>
                        <button
                          onClick={async () => {
                            try {
                              await api.downloadOfferLetterPdf(offer.offerId, `Offer_Letter_${offer.offerId}.pdf`);
                            } catch (err) {
                              alert(err.message || 'Download failed');
                            }
                          }}
                          className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs font-bold transition-colors shadow-xs flex items-center gap-1.5"
                        >
                          <Download className="w-3.5 h-3.5" /> Download PDF
                        </button>
                      </div>
                    )}
                  </div>

                  <div className="pt-2.5 border-t border-emerald-200/80 flex flex-wrap items-center gap-2 text-[11px] text-emerald-800">
                    <span className="font-bold uppercase tracking-wider text-[10px] text-emerald-900">Pipeline Cleared:</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-100/80 px-2 py-0.5 rounded font-medium">✓ Applied</span>
                    <span>→</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-100/80 px-2 py-0.5 rounded font-medium">✓ Shortlisted</span>
                    <span>→</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-100/80 px-2 py-0.5 rounded font-medium">✓ Interviewing</span>
                    <span>→</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-100/80 px-2 py-0.5 rounded font-medium">✓ Selected</span>
                    <span>→</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-100/80 px-2 py-0.5 rounded font-medium">✓ Offered</span>
                    <span>→</span>
                    <span className="inline-flex items-center gap-1 bg-emerald-600 text-white px-2 py-0.5 rounded font-bold">✓ Accepted</span>
                  </div>
                </div>
              ) : isDeclined ? (
                <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-md text-xs text-slate-600 flex items-center gap-2.5">
                  <XCircle className="w-4 h-4 text-slate-400 shrink-0" />
                  <div>
                    <span className="font-bold text-slate-700">Application Closed (Offer Declined): </span>
                    <span>Student finalized placement with another company under the university single-offer policy.</span>
                  </div>
                </div>
              ) : isRejected ? (
                <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-md text-xs text-rose-800 flex items-center gap-2.5">
                  <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>Application marked as REJECTED during screening or interview rounds.</span>
                </div>
              ) : (
                <div>
                  <div className="relative">
                    <div className="overflow-hidden h-2 mb-4 text-xs flex rounded bg-slate-100">
                      <div
                        style={{ width: `${((currentIndex + 1) / pipelineStages.length) * 100}%` }}
                        className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-slate-900 transition-all duration-500"
                      ></div>
                    </div>
                  </div>

                  <div className="grid grid-cols-5 gap-2 text-center text-xs">
                    {pipelineStages.map((stage, idx) => {
                      const isCompleted = idx <= currentIndex;
                      const isCurrent = idx === currentIndex;
                      return (
                        <div key={stage.key} className="space-y-1">
                          <div
                            className={`w-6 h-6 mx-auto rounded-full flex items-center justify-center text-[10px] font-bold ${
                              isCurrent
                                ? 'bg-slate-900 text-white ring-4 ring-slate-200'
                                : isCompleted
                                ? 'bg-emerald-600 text-white'
                                : 'bg-slate-200 text-slate-500'
                            }`}
                          >
                            {isCompleted ? '✓' : idx + 1}
                          </div>
                          <span
                            className={`block text-[11px] font-semibold truncate ${
                              isCurrent ? 'text-slate-900 font-bold' : isCompleted ? 'text-slate-700' : 'text-slate-400'
                            }`}
                          >
                            {stage.label}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Candidate Interview Performance & Overall Score Section */}
              {(appInterviews.length > 0 || app.scores?.overall != null) && (
                <div className="bg-slate-50 rounded-lg border border-slate-200 p-4 space-y-4">
                  {/* Header & Overall Score */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-200">
                    <div>
                      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                        <Award className="w-4 h-4 text-indigo-600" />
                        Candidate Interview Performance
                      </h3>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Authoritative evaluation records from Oracle DB • Calculated from completed scored rounds ({app.scores?.completedRounds ?? 0} evaluated)
                      </p>
                    </div>

                    <div>
                      {overallScore != null ? (
                        <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-emerald-50 border border-emerald-300 rounded-lg shadow-sm">
                          <span className="text-xs font-bold text-emerald-900 uppercase tracking-wide">Overall Score:</span>
                          <span className="font-mono text-sm font-extrabold text-emerald-700">{Number(overallScore).toFixed(2)} / 100</span>
                        </div>
                      ) : (
                        <div className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 border border-slate-300 rounded-lg text-slate-600 text-xs font-medium">
                          <Clock className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                          <span>Overall Score: <strong className="text-slate-700">Not Available</strong></span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* 3-Column Round Performance Grid */}
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    {/* Round 1: OA */}
                    <div className="bg-white p-3 rounded-lg border border-slate-200 flex flex-col justify-between shadow-xs">
                      <div>
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Round #1 — OA</span>
                          {isOaCompleted ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                              PASSED ✓
                            </span>
                          ) : isOaRejected ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                              REJECTED ✕
                            </span>
                          ) : oaScore != null ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                              PENDING
                            </span>
                          ) : (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500">
                              {oaInterview ? 'SCHEDULED' : '— Pending'}
                            </span>
                          )}
                        </div>
                        <div className="mt-2 flex items-baseline gap-1">
                          <span className="text-xs text-slate-500 font-medium">OA Score:</span>
                          <span className="font-mono text-base font-bold text-slate-900">
                            {oaScore != null ? `${oaScore} / 100` : '—'}
                          </span>
                        </div>
                      </div>
                      <div className="text-[10px] text-slate-400 mt-2">
                        Online Assessment
                      </div>
                    </div>

                    {/* Round 2: Technical */}
                    <div className="bg-white p-3 rounded-lg border border-slate-200 flex flex-col justify-between shadow-xs">
                      <div>
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Round #2 — GD / Technical</span>
                          {isTechCompleted ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                              PASSED ✓
                            </span>
                          ) : isTechRejected ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                              REJECTED ✕
                            </span>
                          ) : techScore != null ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                              PENDING
                            </span>
                          ) : (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500">
                              {techInterview ? 'SCHEDULED' : '— Pending'}
                            </span>
                          )}
                        </div>
                        <div className="mt-2 flex items-baseline gap-1">
                          <span className="text-xs text-slate-500 font-medium">Tech Score:</span>
                          <span className="font-mono text-base font-bold text-slate-900">
                            {techScore != null ? `${techScore} / 100` : '—'}
                          </span>
                        </div>
                      </div>
                      <div className="text-[10px] text-slate-400 mt-2">
                        Technical / Coding Interview
                      </div>
                    </div>

                    {/* Round 3: HR */}
                    <div className="bg-white p-3 rounded-lg border border-slate-200 flex flex-col justify-between shadow-xs">
                      <div>
                        <div className="flex items-center justify-between">
                          <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wide">Round #3 — HR</span>
                          {isHrCompleted ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                              PASSED ✓
                            </span>
                          ) : isHrRejected ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                              REJECTED ✕
                            </span>
                          ) : hrScore != null ? (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                              PENDING
                            </span>
                          ) : (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500">
                              {hrInterview ? 'SCHEDULED' : '— Pending'}
                            </span>
                          )}
                        </div>
                        <div className="mt-2 flex items-baseline gap-1">
                          <span className="text-xs text-slate-500 font-medium">HR Score:</span>
                          <span className="font-mono text-base font-bold text-slate-900">
                            {hrScore != null ? `${hrScore} / 100` : '—'}
                          </span>
                        </div>
                      </div>
                      <div className="text-[10px] text-slate-400 mt-2">
                        HR & Behavioral Interview
                      </div>
                    </div>
                  </div>

                  {/* Chronological Interview Logs */}
                  <div className="space-y-2 pt-2">
                    <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                      Evaluation Logs & Interviewers (Pinned via INTERVIEWER_ROUND)
                    </div>
                    <div className="space-y-2">
                      {appInterviews.map((interview, iIdx) => {
                        const oaSc = interview.oa ?? interview.oaScore;
                        const gdSc = interview.gd ?? interview.gdScore;
                        const hrSc = interview.hr ?? interview.hrScore;
                        const primaryScore = interview.score ?? (
                          interview.roundNo === 1 ? oaSc
                          : interview.roundNo === 2 ? gdSc
                          : interview.roundNo === 3 ? hrSc
                          : (oaSc ?? gdSc ?? hrSc)
                        );

                        return (
                          <div
                            key={iIdx}
                            className="p-3 bg-white rounded border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                          >
                            <div>
                              <div className="flex flex-wrap items-center gap-2">
                                <span className="font-semibold text-slate-800">
                                  Interviewer: {interview.interviewerName}
                                </span>
                                <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
                                  Round #{interview.roundNo || '1'}
                                </span>
                                <span className="px-1.5 py-0.5 rounded text-[10px] bg-slate-100 text-slate-600">
                                  {interview.online === 'Y' || interview.mode === 'Online' ? 'Online Video' : 'In-Person'}
                                </span>
                                {interview.roundName && (
                                  <span className="px-1.5 py-0.5 rounded text-[10px] bg-blue-50 text-blue-700 border border-blue-200 font-medium">
                                    {interview.roundName}
                                  </span>
                                )}
                              </div>

                              <div className="flex flex-wrap items-center gap-2.5 text-[11px] text-slate-500 mt-1.5">
                                {primaryScore != null ? (
                                  <span className="inline-flex items-center gap-1 font-mono font-bold text-slate-900 bg-slate-50 px-2 py-0.5 rounded border border-slate-300">
                                    {interview.roundNo === 1 ? 'OA Score'
                                      : interview.roundNo === 2 ? 'Tech Score'
                                      : interview.roundNo === 3 ? 'HR Score'
                                      : 'Score'}:{' '}
                                    <span className="text-emerald-700">{primaryScore} / 100</span>
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                                    Evaluation Pending
                                  </span>
                                )}

                                {interview.comments && (
                                  <>
                                    <span className="text-slate-300 hidden sm:inline">•</span>
                                    <span className="italic text-slate-600 truncate max-w-xs sm:max-w-md">
                                      "{interview.comments}"
                                    </span>
                                  </>
                                )}
                              </div>
                            </div>

                            <div>
                              <span
                                className={`px-2.5 py-1 rounded text-xs font-bold uppercase tracking-wider ${
                                  interview.result === 'PASSED' || interview.result === 'CLEARED'
                                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                                    : interview.result === 'FAILED' || interview.result === 'REJECTED'
                                    ? 'bg-rose-50 text-rose-800 border border-rose-200'
                                    : 'bg-amber-50 text-amber-800 border border-amber-200'
                                }`}
                              >
                                {interview.result || 'SCHEDULED'}
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {fetchError && (
          <div className="bg-rose-50 border border-rose-200 rounded-lg p-6 flex items-start gap-4">
            <AlertCircle className="w-5 h-5 text-rose-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-semibold text-rose-800">Failed to load your applications</p>
              <p className="text-xs text-rose-600 mt-1">{fetchError}</p>
              <button
                onClick={loadData}
                className="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 bg-rose-700 hover:bg-rose-800 text-white rounded text-xs font-semibold transition-colors"
              >
                Retry
              </button>
            </div>
          </div>
        )}

        {!fetchError && applications.length === 0 && (
          <div className="bg-white rounded-lg border border-slate-200 p-12 text-center text-slate-400">
            <FileCheck className="w-10 h-10 mx-auto mb-2 text-slate-300" />
            <p className="text-xs font-medium text-slate-600">No applications registered yet.</p>
            <p className="text-xs text-slate-400 mt-1">
              Browse available placement drives and submit an application to start tracking your recruitment journey.
            </p>
            <Link
              to="/student/drives"
              className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-md text-xs font-bold transition-colors shadow-sm"
            >
              <Briefcase className="w-3.5 h-3.5" /> View Active Drives
            </Link>
          </div>
        )}
      </div>


      {/* Offer Letter Inspection Modal */}
      {selectedOffer && (
        <Modal
          isOpen={!!selectedOffer}
          onClose={() => setSelectedOffer(null)}
          title="Official University Placement Offer Letter"
        >
          <div className="space-y-4 text-xs">
            <div className="border border-slate-300 p-6 rounded bg-slate-50 text-slate-800 font-serif leading-relaxed space-y-4 print:p-0 print:border-none">
              <div className="text-center border-b pb-4 border-slate-300">
                <h3 className="text-lg font-bold uppercase tracking-wider font-sans text-slate-900">
                  Letter of Campus Placement Offer
                </h3>
                <p className="text-[11px] text-slate-500 font-sans mt-0.5">
                  Centre for Career Planning & University Industry Relations
                </p>
                <p className="text-[10px] text-slate-400 font-mono mt-1">
                  Reference: {selectedOffer.offerId} • Issued: {selectedOffer.offerDate}
                </p>
              </div>

              <div className="space-y-3">
                <p>Dear <strong>Student ({studentId})</strong>,</p>
                <p>
                  We are pleased to formally offer you the designated engineering position with an annual
                  compensation of <strong className="text-slate-900 font-mono text-sm">{formatPackage(selectedOffer.ctcLpa)}</strong>.
                </p>
                <p>
                  This offer has been recorded and verified through the University Placement Portal database
                  under Oracle PL/SQL stored procedure <code className="font-mono">ISSUE_OFFER</code>.
                </p>
              </div>

              <div className="pt-6 flex justify-between items-end border-t border-slate-300 text-[11px] font-sans">
                <div>
                  <p className="font-bold text-slate-900">Corporate Talent Acquisition</p>
                  <p className="text-slate-500">Authorized Recruiter</p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-slate-900">Director of Placements</p>
                  <p className="text-slate-500">University Placement Cell</p>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-2 pt-2">
              <div className="text-[11px] text-slate-500 font-mono">
                Official Institutional Offer • Verified in Oracle
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      await api.downloadOfferLetterPdf(selectedOffer.offerId, `Offer_Letter_${selectedOffer.offerId}.pdf`);
                    } catch (err) {
                      alert(err.message || 'Download failed');
                    }
                  }}
                  className="px-3.5 py-2 bg-emerald-700 hover:bg-emerald-800 text-white rounded text-xs font-semibold inline-flex items-center gap-1.5 transition-colors shadow-sm"
                >
                  <Download className="w-3.5 h-3.5" /> Download Official PDF
                </button>
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="px-3.5 py-2 border border-slate-300 hover:bg-slate-100 rounded text-xs font-semibold inline-flex items-center gap-1.5 transition-colors"
                >
                  <Printer className="w-3.5 h-3.5" /> Print
                </button>
                {selectedOffer.status === 'OFFERED' && (
                  <button
                    type="button"
                    onClick={() => {
                      const toAccept = selectedOffer;
                      setSelectedOffer(null);
                      setConfirmAcceptOffer(toAccept);
                    }}
                    className="px-3.5 py-2 bg-emerald-700 hover:bg-emerald-800 text-white rounded text-xs font-bold transition-colors shadow-sm inline-flex items-center gap-1.5"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" /> Accept Offer
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setSelectedOffer(null)}
                  className="px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded text-xs font-semibold transition-colors"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </Modal>
      )}

      {/* Confirm Offer Acceptance Modal */}
      <Modal
        isOpen={!!confirmAcceptOffer}
        onClose={() => !acceptingOfferId && setConfirmAcceptOffer(null)}
        title="Confirm Placement Offer Acceptance"
        maxWidth="max-w-lg"
      >
        {confirmAcceptOffer && (
          <div className="space-y-4 text-slate-800">
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="text-xs text-amber-900 space-y-1">
                <div className="font-bold">Final Placement Selection Rule</div>
                <div>
                  Accepting this offer will finalize your placement with <strong>{confirmAcceptOffer.companyName}</strong>.
                  <p className="mt-1">
                    Accepting this offer will finalize your placement selection. Other active offers may be declined/withdrawn. Do you want to continue?
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 text-center">
              <div className="text-xs text-slate-500 uppercase font-semibold">Selected Offer</div>
              <div className="text-base font-bold text-slate-900 mt-0.5">{confirmAcceptOffer.companyName}</div>
              <div className="text-xs text-slate-600">{confirmAcceptOffer.jobTitle}</div>
              <div className="text-xl font-bold text-emerald-700 font-mono mt-2">
                {formatPackage(confirmAcceptOffer.ctcLpa)}
              </div>
              <div className="text-[11px] text-slate-500 font-mono mt-1">
                Drive ID: {confirmAcceptOffer.driveId || '—'} • Ref: {confirmAcceptOffer.offerId}
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={acceptingOfferId}
                onClick={() => setConfirmAcceptOffer(null)}
                className="px-4 py-2 rounded border border-slate-300 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={acceptingOfferId}
                onClick={async () => {
                  try {
                    setAcceptingOfferId(confirmAcceptOffer.offerId);
                    await api.acceptOffer(confirmAcceptOffer.offerId);
                    setActionMessage({
                      type: 'success',
                      title: 'Offer Accepted Successfully',
                      message: `Congratulations! You have finalized your placement with ${confirmAcceptOffer.companyName}.`,
                    });
                    setConfirmAcceptOffer(null);
                    await loadData();
                  } catch (err) {
                    setActionMessage({
                      type: 'error',
                      title: 'Offer Acceptance Failed',
                      message: err.message || 'Unable to accept offer.',
                    });
                  } finally {
                    setAcceptingOfferId(null);
                  }
                }}
                className="px-4 py-2 rounded bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold transition-colors shadow-sm disabled:opacity-50 flex items-center gap-1.5"
              >
                {acceptingOfferId ? 'Processing...' : 'Confirm Offer Selection'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
