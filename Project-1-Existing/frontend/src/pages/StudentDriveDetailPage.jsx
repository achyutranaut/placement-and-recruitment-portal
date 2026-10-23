import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatPackage } from '../lib/formatters';
import Modal from '../components/Modal';
import {
  Briefcase,
  Building,
  Calendar,
  Award,
  CheckCircle2,
  XCircle,
  Clock,
  MapPin,
  Users,
  FileCheck,
  ArrowLeft,
  ShieldCheck,
  AlertCircle,
  FileText,
  Send,
  Layers,
} from 'lucide-react';

export default function StudentDriveDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const studentId = user?.referenceId;

  const [drive, setDrive] = useState(null);
  const [student, setStudent] = useState(null);
  const [currentResume, setCurrentResume] = useState(null);
  const [applications, setApplications] = useState([]);
  const [eligibility, setEligibility] = useState(null); // from backend
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [agreedPolicy, setAgreedPolicy] = useState(false);
  const [notification, setNotification] = useState(null);

  const loadData = async () => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return;
    }

    try {
      setLoading(true);
      // 1. Fetch recruitment drive independently first
      try {
        const drv = await api.getDriveById(id);
        setDrive(drv);
      } catch (drvErr) {
        console.error('Failed to load drive details:', drvErr);
        setDrive(null);
        setNotification({
          type: 'error',
          message: drvErr.message || `Recruitment drive ${id} could not be retrieved from Oracle.`,
        });
        return;
      }

      // 2. Fetch secondary student context without blocking drive render
      if (studentId) {
        const [stuRes, resumeRes, appsRes, eligRes] = await Promise.allSettled([
          api.getStudent(studentId),
          api.getCurrentResume(studentId),
          api.getMyApplications(),
          api.getDriveEligibility(id),
        ]);

        if (stuRes.status === 'fulfilled') setStudent(stuRes.value);
        if (resumeRes.status === 'fulfilled') setCurrentResume(resumeRes.value);
        if (appsRes.status === 'fulfilled') setApplications(appsRes.value || []);
        if (eligRes.status === 'fulfilled') setEligibility(eligRes.value);
      }
    } catch (err) {
      console.error('Failed to load drive details:', err);
      setNotification({
        type: 'error',
        message: err.message || 'Unable to retrieve placement drive details.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id, studentId]);


  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Placement Drive Specifications...</span>
        </div>
      </div>
    );
  }

  if (!drive) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 text-center">
        <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-slate-900">Placement Drive Not Found</h2>
        <p className="text-xs text-slate-500 mt-1">The requested recruitment drive ID {id} does not exist in the database.</p>
        <Link to="/student/drives" className="mt-4 inline-block text-xs font-semibold text-blue-700 hover:underline">
          Return to All Drives
        </Link>
      </div>
    );
  }

  // --- Authoritative Eligibility from Backend (no client-side string matching) ---
  const studentCgpa = eligibility?.studentCgpa ?? (student?.cgpa ? parseFloat(student.cgpa) : 0);
  const minCgpa = eligibility?.minimumCgpa ?? parseFloat(drive?.minCgpa) ?? 0;
  const isCgpaEligible = eligibility ? eligibility.cgpaEligible : studentCgpa >= minCgpa;
  const isBranchEligible = eligibility ? eligibility.programEligible : true;
  const isEligible = eligibility ? eligibility.eligible : (isCgpaEligible && isBranchEligible);

  // Display values
  const studentProgramDisplay = eligibility?.studentProgram ?? student?.branch ?? 'Computer Science & Engineering';
  const eligibleProgramsDisplay = eligibility?.eligiblePrograms?.join(', ') ?? drive?.eligibleBranches ?? 'All Engineering Branches';

  const existingApp = applications.find((a) => a.driveId === drive.driveId);
  const hasApplied = !!existingApp;

  // University Institutional Policy / Oracle Daily Routine Constraint: 1 Application per calendar day
  const todayStr = new Date().toISOString().split('T')[0];
  const localTodayStr = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}-${String(new Date().getDate()).padStart(2, '0')}`;
  const appliedToday = applications.find(
    (a) => (a.applyDate === todayStr || a.applyDate === localTodayStr) && a.driveId !== drive.driveId
  );

  const handleConfirmApplication = async () => {
    if (!agreedPolicy) return;
    setApplying(true);
    setNotification(null);
    try {
      const result = await api.applyForDrive(studentId, drive.driveId);
      setShowConfirmModal(false);
      setNotification({
        type: 'success',
        message: `Application submitted successfully via Oracle PL/SQL APPLY_FOR_DRIVE! Application ID: ${result.applicationId}`,
      });
      await loadData();
    } catch (err) {
      setNotification({
        type: 'error',
        message: err.message || 'Application submission failed.',
      });
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/student/drives" className="hover:text-slate-900 flex items-center gap-1 font-medium">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to All Drives
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">{drive.jobTitle}</span>
        </div>

        <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
          Drive ID: {drive.driveId}
        </span>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          className={`p-4 rounded-lg border flex items-start gap-3 text-sm ${
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
              {notification.type === 'success' ? 'Application Registered' : 'Validation Notice'}
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

      {/* Hero Header */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-lg bg-slate-100 border border-slate-300 flex items-center justify-center text-slate-700 font-bold font-mono text-base shrink-0">
              {drive.driveId}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="text-2xl font-bold text-slate-900">{drive.jobTitle}</h1>
                <span className="px-2.5 py-0.5 rounded text-xs font-bold uppercase tracking-wider bg-slate-100 text-slate-800 border border-slate-300">
                  {drive.companyName || '—'}
                </span>
              </div>
              <p className="text-xs text-slate-600 mt-1 font-medium flex items-center gap-4">
                <span>Location: <strong className="text-slate-800">{drive.location || '—'}</strong></span>
                <span>•</span>
                <span>Openings: <strong className="text-slate-800 font-mono">{(drive.openings ?? drive.openingsCount) != null ? `${drive.openings ?? drive.openingsCount} positions` : '—'}</strong></span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4 border-t md:border-t-0 pt-4 md:pt-0 border-slate-100">
            <div className="text-left md:text-right">
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                Compensation (CTC)
              </span>
              <span className="text-2xl font-bold text-slate-900 font-mono">
                {formatPackage(drive.ctc ?? drive.packageLpa ?? drive.startingCtcLpa)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Eligibility Evaluation Banner */}
      <div
        className={`rounded-lg border p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
          hasApplied
            ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
            : appliedToday && isEligible
            ? 'bg-amber-50 border-amber-300 text-amber-900'
            : isEligible
            ? 'bg-blue-50 border-blue-200 text-blue-900'
            : 'bg-rose-50 border-rose-300 text-rose-900'
        }`}
      >
        <div className="flex items-start gap-3">
          {hasApplied ? (
            <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0 mt-0.5" />
          ) : appliedToday && isEligible ? (
            <Clock className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
          ) : isEligible ? (
            <CheckCircle2 className="w-6 h-6 text-blue-600 shrink-0 mt-0.5" />
          ) : (
            <XCircle className="w-6 h-6 text-rose-600 shrink-0 mt-0.5" />
          )}

          <div>
            <h2 className="text-sm font-bold">
              {hasApplied
                ? `Application Filed (Status: ${existingApp.status})`
                : appliedToday && isEligible
                ? '✓ Academic Eligibility Met • Daily Application Limit Reached'
                : isEligible
                ? '✓ You Meet All Academic Eligibility Criteria'
                : '✕ Academic Eligibility Requirements Not Met'}
            </h2>
            <div className="mt-1 text-xs space-y-1">
              {appliedToday && isEligible && (
                <p className="font-semibold text-amber-800 bg-amber-100/60 p-2 rounded border border-amber-200">
                  You have already applied for a placement drive today ({appliedToday.companyName || appliedToday.driveId}{appliedToday.jobTitle ? ` • ${appliedToday.jobTitle}` : ''}). In accordance with university placement policy, students may submit at most one drive application per calendar day. You may apply to this drive tomorrow.
                </p>
              )}
              <p>
                Minimum Required CGPA: <strong>{Number(minCgpa).toFixed(2)}</strong> | Your CGPA:{' '}
                <strong className={isCgpaEligible ? 'text-emerald-700' : 'text-rose-700'}>
                  {Number(studentCgpa).toFixed(2)} {isCgpaEligible ? '✓' : '✕'}
                </strong>
              </p>
              <p>
                Eligible Programs:{' '}
                <strong>
                  {eligibility?.eligiblePrograms?.length > 4
                    ? `${eligibility.eligiblePrograms.slice(0, 3).join(', ')} (+${eligibility.eligiblePrograms.length - 3} more streams)`
                    : eligibleProgramsDisplay}
                </strong>{' '}
                | Your Program:{' '}
                <strong className={isBranchEligible ? 'text-emerald-700' : 'text-rose-700'}>
                  {studentProgramDisplay} {isBranchEligible ? '✓' : '✕'}
                </strong>
              </p>
            </div>
          </div>
        </div>

        <div>
          {hasApplied ? (
            <Link
              to={`/student/applications?driveId=${drive.driveId}`}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold transition-colors shadow-sm"
            >
              <FileCheck className="w-4 h-4" /> View in Pipeline
            </Link>
          ) : appliedToday && isEligible ? (
            <button
              disabled
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-md bg-amber-100 text-amber-900 border border-amber-300 text-xs font-semibold cursor-not-allowed whitespace-nowrap shadow-sm"
              title="You have already applied for a recruitment drive today. University policy restricts applications to 1 per day."
            >
              <Clock className="w-4 h-4 text-amber-700" /> Daily Limit Reached (1/Day)
            </button>
          ) : isEligible ? (
            <button
              onClick={() => setShowConfirmModal(true)}
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-md bg-blue-700 hover:bg-blue-800 text-white text-xs font-bold transition-colors shadow-sm"
            >
              <Send className="w-4 h-4" /> Apply for This Drive
            </button>
          ) : (
            <button
              disabled
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-slate-200 text-slate-500 text-xs font-semibold cursor-not-allowed"
            >
              <XCircle className="w-4 h-4" /> Applications Restricted
            </button>
          )}
        </div>
      </div>

      {/* Role Specifications Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Main Details (2 cols) */}
        <div className="md:col-span-2 space-y-6">
          {/* Job Description */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-3">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Briefcase className="w-4 h-4 text-slate-600" />
              Role Description & Specifications
            </h2>
            <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
              {drive.jobDescription || 'No specific role description provided for this placement drive.'}
            </p>
          </div>

          {/* Selection Process Stages */}
          {drive.selectionProcess && (
            <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <Layers className="w-4 h-4 text-slate-600" />
                Recruitment Process Breakdown
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                {drive.selectionProcess.split('|').map((step, idx) => (
                  <div key={idx} className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-1">
                    <span className="font-bold text-slate-900 block">{step.trim()}</span>
                    <p className="text-[11px] text-slate-500">Evaluation Stage {idx + 1}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Eligible Degree Programs (Junction table resolution) */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Award className="w-4 h-4 text-slate-600" />
              Eligible Degree Programs & Disciplines
            </h2>

            <div className="flex flex-wrap gap-2">
              {(eligibility?.eligiblePrograms || (drive.eligibleBranches ? drive.eligibleBranches.split(',').map((s) => s.trim()) : [])).map((prog, idx) => {
                const isStudentProg =
                  prog.toLowerCase() === studentProgramDisplay.toLowerCase() ||
                  (student?.branch && prog.toLowerCase() === student.branch.toLowerCase());
                return (
                  <span
                    key={idx}
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs border transition-colors ${
                      isStudentProg
                        ? 'bg-emerald-50 text-emerald-800 border-emerald-300 font-bold'
                        : 'bg-slate-50 text-slate-700 border-slate-200 font-medium'
                    }`}
                  >
                    <CheckCircle2 className={`w-3.5 h-3.5 ${isStudentProg ? 'text-emerald-600' : 'text-slate-400'}`} />
                    {prog}
                    {isStudentProg && <span className="text-[10px] text-emerald-700 font-semibold">(Your Program)</span>}
                  </span>
                );
              })}
            </div>
          </div>
        </div>

        {/* Sidebar Info (1 col) */}
        <div className="space-y-6">
          {/* Key Dates & Requirements */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-4">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-slate-600" />
              Important Drive Dates
            </h2>

            <div className="space-y-3 text-xs">
              <div>
                <span className="text-slate-400 block text-[11px]">Application Deadline</span>
                <span className="font-semibold text-slate-800 text-sm">{drive.applicationDeadline}</span>
              </div>
              <div className="h-px bg-slate-100"></div>
              <div>
                <span className="text-slate-400 block text-[11px]">Drive Commencement Date</span>
                <span className="font-semibold text-slate-800 text-sm">{drive.driveDate}</span>
              </div>
              <div className="h-px bg-slate-100"></div>
              <div>
                <span className="text-slate-400 block text-[11px]">Drive Location / Mode</span>
                <span className="font-semibold text-slate-800">{drive.location || '—'}</span>
              </div>
            </div>
          </div>

          {/* Attached Active Resume */}
          <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm space-y-3">
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
              <FileCheck className="w-4 h-4 text-slate-600" />
              Active Resume for Submission
            </h2>

            {currentResume ? (
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1.5">
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-slate-800 truncate max-w-[160px]">{currentResume.fileName}</span>
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 font-mono">
                    v{currentResume.versionNo}
                  </span>
                </div>
                <p className="text-[11px] text-slate-500">Stored in Oracle Database BLOB</p>
                <Link
                  to="/student/resume"
                  className="text-xs font-semibold text-blue-700 hover:underline block pt-1"
                >
                  Manage or Upload New Version &rarr;
                </Link>
              </div>
            ) : (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800 space-y-1">
                <p className="font-semibold">No resume uploaded!</p>
                <p className="text-[11px]">You must upload a resume before applying.</p>
                <Link to="/student/resume" className="font-bold underline block pt-1">
                  Upload Resume Now &rarr;
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Confirmation Dialog Modal */}
      <Modal
        isOpen={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        title="Confirm Placement Drive Application"
      >
        <div className="space-y-4 text-xs text-slate-700">
          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
            <div className="flex justify-between">
              <span className="text-slate-500">Company & Role:</span>
              <strong className="text-slate-900">{drive.companyName} • {drive.jobTitle}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Package (CTC):</span>
              <strong className="text-slate-900 font-mono">{formatPackage(drive.ctc ?? drive.packageLpa ?? drive.startingCtcLpa)}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Candidate Name:</span>
              <strong className="text-slate-900">{student?.name} ({studentId})</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Verified CGPA:</span>
              <strong className="text-slate-900 font-mono">{studentCgpa.toFixed(2)} (Req: {minCgpa.toFixed(2)})</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Active Resume:</span>
              <strong className="text-slate-900 font-mono">
                {currentResume ? `${currentResume.fileName} (v${currentResume.versionNo})` : 'Standard Profile Record'}
              </strong>
            </div>
          </div>

          <div className="p-3 bg-amber-50 rounded-md border border-amber-200 text-amber-900 space-y-1 text-[11px]">
            <p className="font-semibold">Institutional Policy Acknowledgment:</p>
            <p>
              By applying, your official verified academic record and BLOB resume will be dispatched to the corporate recruiter.
              Once submitted, your application initiates the Oracle stored procedure <code className="font-mono">APPLY_FOR_DRIVE</code>.
            </p>
          </div>

          <label className="flex items-start gap-2 pt-1 cursor-pointer">
            <input
              type="checkbox"
              checked={agreedPolicy}
              onChange={(e) => setAgreedPolicy(e.target.checked)}
              className="mt-0.5 rounded border-slate-300 text-slate-900 focus:ring-slate-900"
            />
            <span className="text-xs text-slate-800">
              I agree to abide by the University Placement Code of Conduct and confirm my availability for all scheduled selection rounds.
            </span>
          </label>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-200">
            <button
              type="button"
              onClick={() => setShowConfirmModal(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 rounded-md transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={!agreedPolicy || applying}
              onClick={handleConfirmApplication}
              className="inline-flex items-center gap-1.5 px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-md text-xs font-bold transition-colors shadow-sm disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              {applying ? 'Invoking APPLY_FOR_DRIVE...' : 'Confirm & Submit Application'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
