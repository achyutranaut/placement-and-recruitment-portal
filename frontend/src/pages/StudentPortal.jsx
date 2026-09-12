import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatPackage } from '../lib/formatters';
import Modal from '../components/Modal';
import {
  GraduationCap,
  Briefcase,
  Calendar,
  Building,
  CheckCircle2,
  Clock,
  Award,
  AlertCircle,
  FileCheck,
  Send,
  Printer,
  ChevronRight,
  Download,
} from 'lucide-react';

export default function StudentPortal() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [student, setStudent] = useState(null);
  const [programs, setPrograms] = useState([]);
  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);
  const [interviews, setInterviews] = useState([]);
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);
  const [selectedOffer, setSelectedOffer] = useState(null);
  const [confirmAcceptOffer, setConfirmAcceptOffer] = useState(null);
  const [acceptingOfferId, setAcceptingOfferId] = useState(null);
  const [applyingDriveId, setApplyingDriveId] = useState(null);

  const studentId = user?.referenceId;

  const loadData = async () => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return;
    }

    try {
      setLoading(true);
      const [stu, progs, drvs, apps, ofrs, myInterviews] = await Promise.all([
        api.getStudent(studentId),
        api.getPrograms(),
        api.getDrives(),
        api.getApplications(studentId),
        api.getMyOffers().catch(() => api.getOffers(studentId)),
        api.getStudentInterviews(studentId).catch(() => []),
      ]);
      setStudent(stu);
      setPrograms(progs || []);
      setDrives(drvs || []);
      setApplications(apps || []);
      setOffers(ofrs || []);
      setInterviews(myInterviews || []);
    } catch (err) {
      console.error('Failed to load student data:', err);
      setNotification({
        type: 'error',
        title: 'Connection Notice',
        message: err.message || 'Unable to connect to database backend.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [studentId]);

  const handleApply = async (drive) => {
    setApplyingDriveId(drive.driveId);
    setNotification(null);
    try {
      const newApp = await api.applyForDrive(studentId, drive.driveId);
      setNotification({
        type: 'success',
        message: `Successfully applied to ${drive.companyName || drive.jobTitle}! Application ID: ${newApp.applicationId || 'Submitted'}`,
      });
      await loadData();
    } catch (err) {
      setNotification({
        type: 'error',
        title: 'Application Eligibility Notice',
        message: err.message || 'Application submission failed',
      });
    } finally {
      setApplyingDriveId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Student Profile...</span>
        </div>
      </div>
    );
  }

  const appliedDriveIds = new Set(applications.map((a) => a.driveId));
  const todayStr = new Date().toISOString().split('T')[0];
  const localTodayStr = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}-${String(new Date().getDate()).padStart(2, '0')}`;
  const appliedToday = applications.find(
    (a) => a.applyDate === todayStr || a.applyDate === localTodayStr
  );
  const hasAppliedToday = !!appliedToday;

  const getStatusBadge = (status) => {
    switch (status) {
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

  const acceptedOffer = offers.find((o) => o.status === 'ACCEPTED');
  const activeOffers = offers.filter((o) => o.status === 'OFFERED');
  const placementStatusText = acceptedOffer
    ? 'Placed'
    : activeOffers.length > 0
    ? 'Offer Received'
    : 'Actively Applying';

  const highestOffer = offers.length > 0
    ? Math.max(...offers.map((o) => parseFloat(o.ctcLpa) || 0))
    : 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Notification Toast */}
      {notification && (
        <div
          className={`p-4 rounded-lg border flex items-start gap-3 text-sm transition-all ${
            notification.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-rose-50 text-rose-800 border-rose-200'
          }`}
        >
          {notification.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          ) : (
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          )}
          <div className="flex-1">
            <span className="font-semibold text-xs uppercase tracking-wide">
              {notification.title || (notification.type === 'success' ? 'System Notification' : 'Placement Notice')}
            </span>
            <p className="mt-0.5 text-xs">{notification.message}</p>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-xs font-semibold hover:opacity-75"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Sub-Portal Navigation Tabs */}
      <div className="bg-white rounded-lg border border-slate-200 p-2 shadow-sm flex flex-wrap gap-2 text-xs font-semibold">
        <span className="px-3 py-2 rounded bg-slate-900 text-white flex items-center gap-1.5 shadow-sm">
          <GraduationCap className="w-4 h-4" /> Overview Dashboard
        </span>
        <a
          href="/student/profile"
          className="px-3 py-2 rounded text-slate-700 hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
        >
          <Award className="w-4 h-4 text-slate-500" /> Academic Profile & Skills
        </a>
        <a
          href="/student/resume"
          className="px-3 py-2 rounded text-slate-700 hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
        >
          <FileCheck className="w-4 h-4 text-slate-500" /> Resume Vault (Oracle BLOB)
        </a>
        <a
          href="/student/drives"
          className="px-3 py-2 rounded text-slate-700 hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
        >
          <Briefcase className="w-4 h-4 text-slate-500" /> Placement Drives
        </a>
        <a
          href="/student/applications"
          className="px-3 py-2 rounded text-slate-700 hover:bg-slate-100 flex items-center gap-1.5 transition-colors"
        >
          <Clock className="w-4 h-4 text-slate-500" /> Applications Tracker
        </a>
      </div>

      {/* Student Profile Card */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 shrink-0">
              <GraduationCap className="w-8 h-8" />
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-xl font-bold text-slate-900">{student?.name}</h1>
                <span className="px-2 py-0.5 rounded text-xs font-bold font-mono bg-slate-100 text-slate-800 border border-slate-300">
                  {student?.studentId}
                </span>
                <span
                  className={`px-2 py-0.5 rounded text-xs font-semibold border ${
                    acceptedOffer
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                      : activeOffers.length > 0
                      ? 'bg-amber-50 text-amber-800 border-amber-300'
                      : 'bg-blue-50 text-blue-800 border-blue-300'
                  }`}
                >
                  {placementStatusText}
                </span>
              </div>
              <p className="text-xs text-slate-600 mt-1 font-medium">
                {student?.branch || 'Student'} • University Placement Cell
              </p>
              <div className="mt-2.5 flex flex-wrap gap-4 text-xs text-slate-600">
                <span>Email: <span className="font-medium text-slate-800">{student?.email || '—'}</span></span>
                <span>Phone: <span className="font-medium text-slate-800">{student?.phoneNumbers?.[0] || '—'}</span></span>
                <span>Location: <span className="font-medium text-slate-800">{student?.city && student?.state ? `${student.city}, ${student.state}` : (student?.city || student?.state || '—')}</span></span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-6 border-t md:border-t-0 pt-4 md:pt-0 border-slate-100">
            <div className="text-left md:text-right">
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                Cumulative CGPA
              </span>
              <span className="text-2xl font-bold text-slate-900 font-mono">
                {student?.cgpa ? Number(student.cgpa).toFixed(2) : '—'}
              </span>
            </div>
            <div className="h-10 w-px bg-slate-200 hidden md:block"></div>
            <div className="text-left md:text-right">
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                Active Offers
              </span>
              <span className="text-2xl font-bold text-emerald-700 font-mono">
                {activeOffers.length}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Summary Metric Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">
            Drives Available
          </span>
          <div className="text-xl font-bold text-slate-900 font-mono mt-1">
            {drives.length}
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">
            Applications Submitted
          </span>
          <div className="text-xl font-bold text-slate-900 font-mono mt-1">
            {applications.length}
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">
            Interviews Completed
          </span>
          <div className="text-xl font-bold text-slate-900 font-mono mt-1">
            {interviews.length}
          </div>
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <span className="text-[11px] font-medium text-slate-500 uppercase tracking-wider">
            Highest Offer
          </span>
          <div className="text-xl font-bold text-emerald-700 font-mono mt-1">
            {highestOffer > 0 ? formatPackage(highestOffer) : '—'}
          </div>
        </div>
      </div>

      {/* Dedicated Placement Offer Section: Placement Finalized, Multiple Offers, or Single Offer */}
      {acceptedOffer ? (
        <div className="bg-gradient-to-r from-emerald-900 to-teal-900 rounded-lg p-6 text-white shadow-md border border-emerald-700">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded text-xs font-bold uppercase tracking-wider bg-emerald-500/30 text-emerald-200 border border-emerald-400/30">
                  Placement Finalized ✓
                </span>
                <span className="text-xs text-emerald-200 font-mono">
                  Ref: {acceptedOffer.offerId} • Drive: {acceptedOffer.driveId || '—'}
                </span>
              </div>
              <h2 className="text-xl font-extrabold mt-2 text-white">
                Official Employment Offer Accepted with {acceptedOffer.companyName}
              </h2>
              <p className="text-xs text-emerald-100/80 mt-1">
                Role: <strong className="text-white">{acceptedOffer.jobTitle}</strong> • Offered Package: <strong className="text-emerald-300 font-mono text-sm">{formatPackage(acceptedOffer.ctcLpa)}</strong>
              </p>
              {acceptedOffer.acceptedAt && (
                <p className="text-[11px] text-emerald-200/70 mt-1">
                  Accepted on: {acceptedOffer.acceptedAt} • Atomic single-offer mutex enforced
                </p>
              )}
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setSelectedOffer(acceptedOffer)}
                className="px-3.5 py-2 rounded bg-white/10 hover:bg-white/20 text-white text-xs font-bold border border-white/20 transition-colors flex items-center gap-1.5"
              >
                <Award className="w-4 h-4" /> View Letter
              </button>
              <button
                type="button"
                onClick={async () => {
                  try {
                    await api.downloadOfferLetterPdf(acceptedOffer.offerId, `Offer_Letter_${acceptedOffer.offerId}.pdf`);
                  } catch (err) {
                    alert(err.message || 'Download failed');
                  }
                }}
                className="px-4 py-2 rounded bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-extrabold transition-colors shadow-sm flex items-center gap-1.5"
              >
                <Download className="w-4 h-4" /> Download Official PDF
              </button>
            </div>
          </div>
        </div>
      ) : activeOffers.length > 1 ? (
        <div className="bg-amber-50 rounded-lg border-2 border-amber-300 p-6 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-amber-200 pb-3">
            <div>
              <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-xs font-extrabold uppercase tracking-wide bg-amber-200 text-amber-900 border border-amber-400">
                <AlertCircle className="w-3.5 h-3.5" /> MULTIPLE OFFERS AVAILABLE ({activeOffers.length} Active Offers)
              </div>
              <h2 className="text-base font-bold text-amber-950 mt-1.5">
                Multiple Employment Offers Received — Final Placement Selection Required
              </h2>
              <p className="text-xs text-amber-800 mt-0.5">
                University placement rules strictly enforce a single accepted offer. Review your active offers below and explicitly select the offer you wish to finalize. Accepting an offer will permanently accept your chosen company and automatically decline all other competing offers.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {activeOffers.map((offer) => (
              <div
                key={offer.offerId}
                className="bg-white rounded-lg border border-amber-300 p-4 shadow-sm flex flex-col justify-between hover:border-amber-400 transition-all"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-bold text-slate-500">{offer.offerId}</span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200">
                      ACTIVE OFFER
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-slate-900 mt-2">{offer.companyName}</h3>
                  <div className="text-xs text-slate-600 mt-0.5">{offer.jobTitle}</div>
                  <div className="mt-3 p-2.5 rounded bg-slate-50 border border-slate-100">
                    <span className="text-[10px] uppercase font-semibold text-slate-500 block">Offered Package</span>
                    <span className="text-lg font-extrabold text-emerald-700 font-mono">
                      {formatPackage(offer.ctcLpa)}
                    </span>
                  </div>
                  <div className="mt-2 text-[11px] text-slate-500 font-mono">
                    Drive: {offer.driveId || '—'} • Date: {offer.offerDate}
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setSelectedOffer(offer)}
                    className="flex-1 px-3 py-1.5 rounded border border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs transition-colors"
                  >
                    View Letter
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfirmAcceptOffer(offer)}
                    className="flex-1 px-3 py-1.5 rounded bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs transition-colors shadow-sm"
                  >
                    Select Offer
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : activeOffers.length === 1 ? (
        <div className="bg-emerald-50 rounded-lg border border-emerald-300 p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-lg bg-emerald-100 border border-emerald-300 flex items-center justify-center text-emerald-700 shrink-0">
                <Award className="w-6 h-6" />
              </div>
              <div>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wide bg-emerald-100 text-emerald-800 border border-emerald-200">
                  Employment Offer Received
                </span>
                <h2 className="text-base font-bold text-slate-900 mt-1">
                  {activeOffers[0].companyName} — {activeOffers[0].jobTitle}
                </h2>
                <div className="text-xs text-slate-600 mt-0.5 flex flex-wrap items-center gap-3">
                  <span>Package: <strong className="text-emerald-700 font-mono">{formatPackage(activeOffers[0].ctcLpa)}</strong></span>
                  <span>•</span>
                  <span>Drive: <strong className="font-mono text-slate-800">{activeOffers[0].driveId || '—'}</strong></span>
                  <span>•</span>
                  <span>Offer Date: <strong className="text-slate-700">{activeOffers[0].offerDate}</strong></span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={() => setSelectedOffer(activeOffers[0])}
                className="px-3.5 py-1.5 rounded border border-slate-300 hover:bg-white text-slate-700 font-semibold text-xs transition-colors"
              >
                View Letter
              </button>
              <button
                type="button"
                onClick={() => setConfirmAcceptOffer(activeOffers[0])}
                className="px-4 py-1.5 rounded bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs transition-colors shadow-sm"
              >
                Accept Offer
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {/* Active Applications Pipeline Table */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm" id="applications">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Clock className="w-4 h-4 text-slate-700" />
              My Placement Applications
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Live status tracked across the university candidate evaluation pipeline
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-0.5 bg-slate-100 text-slate-700 rounded border border-slate-200">
            {applications.length} Records
          </span>
        </div>

        {applications.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            No applications submitted yet. Browse open placement drives below to apply.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Application ID</th>
                  <th className="px-4 py-3">Company Name</th>
                  <th className="px-4 py-3">Job Title</th>
                  <th className="px-4 py-3">Applied Date</th>
                  <th className="px-4 py-3">Application Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {applications.map((app) => (
                  <tr key={app.applicationId} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">
                      {app.applicationId}
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-900">
                      {app.companyName}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {app.jobTitle}
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-600">
                      {app.applyDate}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold border ${getStatusBadge(
                          app.status
                        )}`}
                      >
                        {app.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Scheduled Interviews Section - Enforces Pinned Interviewer Model */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm" id="interviews">
        <div className="px-6 py-4 border-b border-slate-200">
          <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <FileCheck className="w-4 h-4 text-slate-700" />
            Interview Evaluations & Scorecards
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Interviewer is permanently pinned to designated round per DA1 rule (INTERVIEWER_ROUND)
          </p>
        </div>

        {interviews.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            No interview evaluations scheduled for your active applications yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Application ID</th>
                  <th className="px-4 py-3">Designated Interviewer</th>
                  <th className="px-4 py-3">Round Name</th>
                  <th className="px-4 py-3">Mode</th>
                  <th className="px-4 py-3">Round Score</th>
                  <th className="px-4 py-3">Evaluation Result</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {interviews.map((iv, idx) => {
                  // Pinned score: only OA for Round 1, GD for Round 2, HR for Round 3
                  let roundDisplay = iv.roundName || '—';
                  let scoreDisplay = iv.score != null ? `${iv.score}/100` : '—';
                  if (!iv.score) {
                    if (iv.oa != null) scoreDisplay = `${iv.oa}/100 (OA)`;
                    else if (iv.gd != null) scoreDisplay = `${iv.gd}/100 (GD)`;
                    else if (iv.hr != null) scoreDisplay = `${iv.hr}/100 (HR)`;
                  }

                  return (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">
                        {iv.applicationId}
                      </td>
                      <td className="px-4 py-3 font-semibold text-slate-900">
                        {iv.interviewerName}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-700">
                        {roundDisplay}
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-medium">
                          {iv.online === 'Y' || iv.mode === 'Online' ? 'Online' : 'Offline'}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-mono font-bold text-slate-900">
                        {scoreDisplay}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold border ${
                            iv.result === 'PASSED' || iv.result === 'CLEARED'
                              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                              : iv.result === 'FAILED' || iv.result === 'REJECTED'
                              ? 'bg-rose-50 text-rose-800 border-rose-200'
                              : 'bg-amber-50 text-amber-800 border-amber-200'
                          }`}
                        >
                          {iv.result}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Eligible Placement Drives Hub */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm" id="drives">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Briefcase className="w-4 h-4 text-slate-700" />
              Active Campus Placement Drives
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Subject to CGPA criteria and university daily application limits
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-0.5 bg-slate-100 text-slate-700 rounded border border-slate-200">
            {drives.length} Drives
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3">Drive ID</th>
                <th className="px-4 py-3">Company & Job Title</th>
                <th className="px-4 py-3">Package (CTC)</th>
                <th className="px-4 py-3">Min CGPA</th>
                <th className="px-4 py-3">Openings</th>
                <th className="px-4 py-3">Deadline</th>
                <th className="px-4 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {drives.map((drive) => {
                const isApplied = appliedDriveIds.has(drive.driveId);
                const isEligible = (student?.cgpa || 0) >= drive.minCgpa;
                const isApplying = applyingDriveId === drive.driveId;

                return (
                  <tr key={drive.driveId} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">
                      {drive.driveId}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-bold text-slate-900">{drive.companyName}</div>
                      <div className="text-slate-500 text-[11px]">{drive.jobTitle}</div>
                    </td>
                    <td className="px-4 py-3 font-mono font-bold text-emerald-700">
                      {formatPackage(drive.ctc ?? drive.packageLpa)}
                    </td>
                    <td className="px-4 py-3 font-mono font-semibold text-slate-700">
                      {drive.minCgpa ? Number(drive.minCgpa).toFixed(2) : '—'}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {drive.openings != null ? `${drive.openings} positions` : '—'}
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-600">
                      {drive.applicationDeadline || '—'}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {isApplied ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 text-slate-600 font-semibold text-[11px]">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          Applied
                        </span>
                      ) : isEligible && hasAppliedToday ? (
                        <span
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-amber-50 text-amber-800 border border-amber-200 font-semibold text-[11px]"
                          title="Daily limit reached: University policy permits applying to at most one drive per calendar day."
                        >
                          <Clock className="w-3.5 h-3.5 text-amber-600" />
                          1 Drive/Day Limit
                        </span>
                      ) : isEligible ? (
                        <button
                          onClick={() => handleApply(drive)}
                          disabled={isApplying}
                          className="px-3 py-1 rounded bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs transition-colors disabled:opacity-50"
                        >
                          {isApplying ? 'Applying...' : 'Apply'}
                        </button>
                      ) : (
                        <span className="text-[11px] font-medium text-slate-400">
                          Ineligible (CGPA &lt; {Number(drive.minCgpa).toFixed(2)})
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

      {/* Official Offer Letters Section */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm" id="offers">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Award className="w-4 h-4 text-emerald-600" />
              Official Employment Offer Letters
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Verified records issued via university recruitment drives
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-0.5 bg-emerald-50 text-emerald-800 rounded border border-emerald-200">
            {offers.length} Released
          </span>
        </div>

        {offers.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            No offer letters issued yet. Candidates selected across final interview rounds will receive formal offer letters here.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Offer ID</th>
                  <th className="px-4 py-3">Company</th>
                  <th className="px-4 py-3">Designation</th>
                  <th className="px-4 py-3">Offered Package</th>
                  <th className="px-4 py-3">Offer Date</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {offers.map((offer) => (
                  <tr key={offer.offerId} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">
                      {offer.offerId}
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-900">
                      {offer.companyName}
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {offer.jobTitle}
                    </td>
                    <td className="px-4 py-3 font-mono font-bold text-emerald-700">
                      {formatPackage(offer.ctcLpa)}
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-600">
                      {offer.offerDate}
                    </td>
                    <td className="px-4 py-3">
                      {offer.status === 'ACCEPTED' ? (
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                          ACCEPTED ✓
                        </span>
                      ) : offer.status === 'OFFERED' ? (
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                          OFFERED (Active)
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-500 border border-slate-300">
                          {offer.status || 'DECLINED'}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right space-x-2">
                      <button
                        onClick={() => setSelectedOffer(offer)}
                        className="px-2.5 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs transition-colors"
                      >
                        View Letter
                      </button>
                      {offer.status === 'OFFERED' && !acceptedOffer && (
                        <button
                          onClick={() => setConfirmAcceptOffer(offer)}
                          className="px-2.5 py-1 rounded bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs transition-colors shadow-sm"
                        >
                          Accept
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Formal Offer Letter Modal Preview */}
      <Modal
        isOpen={!!selectedOffer}
        onClose={() => setSelectedOffer(null)}
        title="Official Employment Offer Letter"
        maxWidth="max-w-2xl"
      >
        {selectedOffer && (
          <div className="space-y-6 text-slate-800">
            <div className="border-b border-slate-200 pb-4 flex justify-between items-start">
              <div>
                <h2 className="text-lg font-bold text-slate-900">{selectedOffer.companyName}</h2>
                <p className="text-xs text-slate-500">University Campus Placement Office</p>
              </div>
              <div className="text-right font-mono text-xs text-slate-500">
                <div>Ref: {selectedOffer.offerId}</div>
                <div className="text-[11px]">Date: {selectedOffer.offerDate}</div>
              </div>
            </div>

            <div className="space-y-3 text-xs leading-relaxed text-slate-700">
              <p>
                Dear <strong>{student?.name}</strong> (Roll No: <code className="font-mono font-semibold">{student?.studentId}</code>),
              </p>
              <p>
                We are pleased to extend this formal offer of employment on behalf of <strong>{selectedOffer.companyName}</strong> based on your successful clearance of the campus recruitment process.
              </p>
              <div className="p-4 rounded-lg bg-slate-50 border border-slate-200 text-center">
                <div className="text-xs font-semibold text-slate-600 uppercase tracking-wide">Designation</div>
                <div className="text-sm font-bold text-slate-900 mt-0.5">{selectedOffer.jobTitle}</div>
                <div className="text-xl font-bold text-emerald-700 font-mono mt-2">
                  {formatPackage(selectedOffer.ctcLpa)}
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">
                  Cost to Company (Annual Package)
                </div>
              </div>
              <p>
                This offer is subject to maintaining good academic standing and meeting all prerequisite degree requirements before joining.
              </p>
            </div>

            <div className="pt-4 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3">
              <div className="text-[11px] text-slate-500 font-mono">
                Authoritative Record • Verified in Oracle Database
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
                  className="px-3.5 py-1.5 rounded bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold flex items-center gap-1.5 transition-colors shadow-sm"
                >
                  <Download className="w-3.5 h-3.5" />
                  Download Official PDF
                </button>
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="px-3 py-1.5 rounded bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-300 text-xs font-semibold flex items-center gap-1.5 transition-colors"
                >
                  <Printer className="w-3.5 h-3.5" />
                  Print Letter
                </button>
                {selectedOffer.status === 'OFFERED' && !acceptedOffer && (
                  <button
                    type="button"
                    onClick={() => {
                      const toAccept = selectedOffer;
                      setSelectedOffer(null);
                      setConfirmAcceptOffer(toAccept);
                    }}
                    className="px-3.5 py-1.5 rounded bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold transition-colors shadow-sm"
                  >
                    Accept Offer
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
      </Modal>

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
                  {activeOffers.length > 1 && (
                    <p className="mt-1 font-semibold text-rose-700">
                      Your other {activeOffers.length - 1} active offer(s) will be automatically DECLINED/WITHDRAWN in the database.
                    </p>
                  )}
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
                    setNotification({
                      type: 'success',
                      title: 'Offer Accepted Successfully',
                      message: `Congratulations! You have finalized your placement with ${confirmAcceptOffer.companyName}.`,
                    });
                    setConfirmAcceptOffer(null);
                    await loadData();
                  } catch (err) {
                    setNotification({
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
