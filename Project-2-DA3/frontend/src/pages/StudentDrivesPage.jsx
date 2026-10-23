import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import { formatPackage } from '../lib/formatters';
import { Link, useNavigate } from 'react-router-dom';
import {
  Briefcase,
  Building,
  Calendar,
  Award,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowRight,
  ArrowLeft,
  MapPin,
  Users,
  ChevronDown,
  ChevronUp,
  AlertTriangle,
  GraduationCap,
} from 'lucide-react';

const INITIAL_PROGRAM_LIMIT = 4;

export default function StudentDrivesPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const studentId = user?.referenceId;

  const [drives, setDrives] = useState([]);
  const [student, setStudent] = useState(null);
  const [applications, setApplications] = useState([]);
  const [eligibilityMap, setEligibilityMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterEligibility, setFilterEligibility] = useState('ALL'); // ALL, ELIGIBLE, APPLIED, CLOSING_SOON, PAST
  const [expandedPrograms, setExpandedPrograms] = useState({});

  const loadData = async () => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return;
    }

    try {
      setLoading(true);
      const [drvs, stu, apps, eligMap] = await Promise.all([
        api.getDrives(),
        api.getStudent(studentId),
        api.getMyApplications(),
        api.getAllDrivesEligibility().catch(() => ({})),
      ]);
      setDrives(drvs || []);
      setStudent(stu);
      setApplications(apps || []);
      setEligibilityMap(eligMap || {});
    } catch (err) {
      console.error('Failed to load placement drives:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    const handleMutation = () => {
      loadData();
    };
    window.addEventListener('portal:database-mutation', handleMutation);
    return () => {
      window.removeEventListener('portal:database-mutation', handleMutation);
    };
  }, [studentId]);

  const studentCgpa = student?.cgpa ? parseFloat(student.cgpa) : 0;
  const appliedDriveIds = new Set(applications.map((a) => a.driveId));
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const toggleExpand = (driveId) => {
    setExpandedPrograms((prev) => ({
      ...prev,
      [driveId]: !prev[driveId],
    }));
  };

  const isEligible = (drive) => {
    const elig = eligibilityMap[drive.driveId];
    if (elig) {
      return elig.eligible;
    }
    const minCgpa = parseFloat(drive.minCgpa) || 0;
    return studentCgpa >= minCgpa;
  };

  const getDriveDeadlineStatus = (drive) => {
    if (!drive.applicationDeadline) return { passed: false, approaching: false, daysLeft: null };
    const deadlineDate = new Date(drive.applicationDeadline);
    deadlineDate.setHours(23, 59, 59, 999);
    const diffTime = deadlineDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return {
      passed: diffDays < 0,
      approaching: diffDays >= 0 && diffDays <= 4,
      daysLeft: diffDays,
    };
  };

  const filteredDrives = drives.filter((drive) => {
    const matchesSearch =
      drive.jobTitle?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      drive.companyName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      drive.location?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (drive.eligibleBranches && drive.eligibleBranches.toLowerCase().includes(searchQuery.toLowerCase()));

    if (!matchesSearch) return false;

    const { passed, approaching } = getDriveDeadlineStatus(drive);

    if (filterEligibility === 'ELIGIBLE') {
      return isEligible(drive) && !passed;
    }
    if (filterEligibility === 'APPLIED') {
      return appliedDriveIds.has(drive.driveId);
    }
    if (filterEligibility === 'CLOSING_SOON') {
      return approaching && !passed;
    }
    if (filterEligibility === 'PAST') {
      return passed;
    }
    return true;
  });

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Placement Drives...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Breadcrumb Navigation & Student CGPA Indicator */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/student" className="hover:text-slate-900 flex items-center gap-1 font-medium transition-colors">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Student Dashboard
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Active Campus Placement Drives</span>
        </div>

        <div className="flex items-center gap-2.5 bg-white px-3 py-1.5 rounded-lg border border-slate-200 shadow-2xs self-start sm:self-auto">
          <GraduationCap className="w-4 h-4 text-slate-500" />
          <span className="text-xs text-slate-500 font-medium">Verified CGPA:</span>
          <span className="px-2 py-0.5 rounded text-xs font-mono font-bold bg-slate-100 text-slate-900 border border-slate-300">
            {studentCgpa.toFixed(2)}
          </span>
          {student?.branch && (
            <span className="text-[11px] text-slate-500 hidden md:inline border-l border-slate-200 pl-2">
              {student.branch}
            </span>
          )}
        </div>
      </div>

      {/* Header Banner */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-blue-50 text-blue-700">
                <Briefcase className="w-5 h-5" />
              </span>
              <h1 className="text-xl font-bold text-slate-900">Campus Placement Drives</h1>
            </div>
            <p className="text-xs text-slate-500 mt-1 max-w-2xl">
              Verified campus recruitment drives scheduled for the graduating batch. Academic eligibility is validated server-side against minimum CGPA cutoffs and participating degree programs.
            </p>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-800 border border-blue-200">
              {drives.length} Drives Available
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
              {applications.length} Applications Filed
            </span>
          </div>
        </div>
      </div>

      {/* Search & Quick Filters Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-3.5 shadow-xs flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search drives by role, company name, location, or eligible stream..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-slate-900 bg-slate-50/50 focus:bg-white transition-colors"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 w-full md:w-auto overflow-x-auto pb-1 md:pb-0">
          {[
            { id: 'ALL', label: `All Active (${drives.length})` },
            { id: 'ELIGIBLE', label: 'Eligible to Apply' },
            { id: 'APPLIED', label: `Applied (${applications.length})` },
            { id: 'CLOSING_SOON', label: 'Deadline Approaching' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterEligibility(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all border ${
                filterEligibility === tab.id
                  ? 'bg-slate-900 text-white border-slate-900 shadow-2xs'
                  : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Placement Drives List */}
      <div className="space-y-4">
        {filteredDrives.map((drive) => {
          const elig = eligibilityMap[drive.driveId];
          const eligible = isEligible(drive);
          const hasApplied = appliedDriveIds.has(drive.driveId);
          const minCgpa = parseFloat(drive.minCgpa) || 0;
          const { passed: deadlinePassed, approaching: deadlineApproaching, daysLeft } = getDriveDeadlineStatus(drive);
          const isExpanded = !!expandedPrograms[drive.driveId];

          // Parse full program list returned by backend
          const programList = elig?.eligiblePrograms?.length > 0
            ? elig.eligiblePrograms
            : (drive.eligibleBranches ? drive.eligibleBranches.split(',').map((s) => s.trim()).filter(Boolean) : []);

          const visiblePrograms = isExpanded ? programList : programList.slice(0, INITIAL_PROGRAM_LIMIT);
          const remainingProgramCount = Math.max(0, programList.length - INITIAL_PROGRAM_LIMIT);

          return (
            <div
              key={drive.driveId}
              className={`bg-white rounded-xl border p-5 shadow-xs transition-all duration-150 hover:shadow-sm ${
                hasApplied
                  ? 'border-emerald-200 bg-emerald-50/10'
                  : deadlinePassed
                  ? 'border-slate-200 bg-slate-50/40 opacity-90'
                  : 'border-slate-200 hover:border-slate-300'
              }`}
            >
              {/* Card Header (Phase 2.1) */}
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 pb-3 border-b border-slate-100">
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  {/* Drive ID Secondary Identifier */}
                  <span className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold font-mono text-xs shrink-0 mt-0.5">
                    {drive.driveId}
                  </span>

                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-base sm:text-lg font-bold text-slate-900 leading-snug truncate">
                        <Link
                          to={`/student/drives/${drive.driveId}`}
                          className="hover:text-blue-700 transition-colors"
                        >
                          {drive.jobTitle}
                        </Link>
                      </h2>
                    </div>

                    <div className="flex items-center gap-2 text-xs font-semibold text-slate-600 mt-0.5">
                      <Building className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{drive.companyName || 'Campus Recruiter'}</span>
                      {drive.location && (
                        <>
                          <span className="text-slate-300">•</span>
                          <span className="text-slate-500 font-normal flex items-center gap-1">
                            <MapPin className="w-3 h-3 text-slate-400" />
                            {drive.location}
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                {/* Status Badges (Phase 2.2) */}
                <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
                  {hasApplied ? (
                    <span className="px-2.5 py-1 rounded-md text-[11px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-300 flex items-center gap-1.5 shadow-2xs">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                      Applied
                    </span>
                  ) : deadlinePassed ? (
                    <span className="px-2.5 py-1 rounded-md text-[11px] font-bold uppercase tracking-wider bg-slate-100 text-slate-600 border border-slate-300 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      Deadline Passed
                    </span>
                  ) : eligible ? (
                    <span className={`px-2.5 py-1 rounded-md text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 shadow-2xs ${
                      deadlineApproaching
                        ? 'bg-amber-50 text-amber-800 border border-amber-300'
                        : 'bg-blue-50 text-blue-800 border border-blue-200'
                    }`}>
                      {deadlineApproaching ? (
                        <>
                          <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                          <span>Closing Soon ({daysLeft}d)</span>
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="w-3.5 h-3.5 text-blue-600" />
                          <span>Eligible to Apply</span>
                        </>
                      )}
                    </span>
                  ) : (
                    <span className="px-2.5 py-1 rounded-md text-[11px] font-semibold text-rose-700 bg-rose-50 border border-rose-200 flex items-center gap-1.5" title={elig?.message || 'Academic criteria not met'}>
                      <XCircle className="w-3.5 h-3.5 text-rose-500" />
                      {elig && !elig.cgpaEligible
                        ? `CGPA < ${minCgpa.toFixed(2)}`
                        : elig && !elig.programEligible
                        ? 'Program Ineligible'
                        : `Ineligible`}
                    </span>
                  )}
                </div>
              </div>

              {/* Job Description (Phase 2.3) */}
              {drive.jobDescription && (
                <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed pt-2.5">
                  {drive.jobDescription}
                </p>
              )}

              {/* Key Information Metadata Row (Phase 2.4) */}
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-2.5 pt-3 text-xs">
                {/* Compensation Package */}
                <div className="bg-slate-50/75 rounded-lg p-2 border border-slate-200/80">
                  <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                    Package (CTC)
                  </span>
                  <span className="font-mono font-bold text-slate-900 text-xs sm:text-sm text-emerald-700">
                    {formatPackage(drive.ctc ?? drive.packageLpa ?? drive.startingCtcLpa)}
                  </span>
                </div>

                {/* Openings */}
                <div className="bg-slate-50/75 rounded-lg p-2 border border-slate-200/80">
                  <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                    Openings
                  </span>
                  <span className="font-mono font-bold text-slate-800 text-xs sm:text-sm">
                    {(drive.openings ?? drive.openingsCount) != null ? `${drive.openings ?? drive.openingsCount} seats` : '—'}
                  </span>
                </div>

                {/* Minimum CGPA */}
                <div className="bg-slate-50/75 rounded-lg p-2 border border-slate-200/80">
                  <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                    Min CGPA
                  </span>
                  <span className={`font-mono font-bold text-xs sm:text-sm ${
                    studentCgpa >= minCgpa ? 'text-slate-800' : 'text-rose-600'
                  }`}>
                    {minCgpa.toFixed(2)}
                  </span>
                </div>

                {/* Drive Date */}
                <div className="bg-slate-50/75 rounded-lg p-2 border border-slate-200/80">
                  <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                    Drive Date
                  </span>
                  <span className="font-medium text-slate-800 text-xs truncate block" title={drive.driveDate}>
                    {drive.driveDate || '—'}
                  </span>
                </div>

                {/* Application Deadline */}
                <div className={`rounded-lg p-2 border ${
                  deadlinePassed
                    ? 'bg-rose-50/50 border-rose-200 text-rose-900'
                    : deadlineApproaching
                    ? 'bg-amber-50/50 border-amber-200 text-amber-900'
                    : 'bg-slate-50/75 border-slate-200/80 text-slate-800'
                }`}>
                  <span className="text-[10px] uppercase font-bold tracking-wider block opacity-75">
                    Deadline
                  </span>
                  <span className="font-medium text-xs truncate block" title={drive.applicationDeadline}>
                    {drive.applicationDeadline || 'Open'}
                  </span>
                </div>

                {/* Selection Process Stages */}
                <div className="bg-slate-50/75 rounded-lg p-2 border border-slate-200/80">
                  <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider block">
                    Selection Mode
                  </span>
                  <span className="font-medium text-slate-800 text-xs truncate block" title={drive.selectionProcess || 'Campus Evaluation'}>
                    {drive.selectionProcess ? drive.selectionProcess.split('|')[0].trim() : 'Assessment'}
                  </span>
                </div>
              </div>

              {/* Eligible Academic Programs — CRITICAL (Phase 2.5) */}
              {programList.length > 0 && (
                <div className="pt-3 border-t border-slate-100 mt-3">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mr-1 flex items-center gap-1">
                      <GraduationCap className="w-3 h-3 text-slate-400" />
                      Eligible Programs ({programList.length}):
                    </span>

                    {visiblePrograms.map((program, idx) => {
                      const isStudentProg =
                        student?.branch &&
                        (program.toLowerCase().includes(student.branch.toLowerCase()) ||
                         student.branch.toLowerCase().includes(program.toLowerCase()));

                      return (
                        <span
                          key={idx}
                          className={`px-2 py-0.5 rounded text-[10px] font-medium border transition-colors ${
                            isStudentProg
                              ? 'bg-emerald-50 text-emerald-800 border-emerald-300 font-bold'
                              : 'bg-slate-100 text-slate-700 border-slate-200'
                          }`}
                        >
                          {program}
                          {isStudentProg && <span className="ml-1 text-[9px] text-emerald-600 font-bold">(Your Program)</span>}
                        </span>
                      );
                    })}

                    {/* Expand / Collapse Button (+24 more) */}
                    {remainingProgramCount > 0 && !isExpanded && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.preventDefault();
                          toggleExpand(drive.driveId);
                        }}
                        className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 transition-colors inline-flex items-center gap-1 cursor-pointer"
                        aria-expanded={false}
                        aria-label={`Expand ${remainingProgramCount} more eligible academic programs`}
                      >
                        <span>+{remainingProgramCount} more</span>
                        <ChevronDown className="w-3 h-3" />
                      </button>
                    )}

                    {isExpanded && remainingProgramCount > 0 && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.preventDefault();
                          toggleExpand(drive.driveId);
                        }}
                        className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-300 transition-colors inline-flex items-center gap-1 cursor-pointer ml-1"
                        aria-expanded={true}
                        aria-label="Collapse eligible academic programs"
                      >
                        <span>Show less</span>
                        <ChevronUp className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* Primary Action Button (Phase 2.6) */}
              <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3.5 mt-3 border-t border-slate-100">
                <div className="text-[11px] text-slate-500">
                  {hasApplied ? (
                    <span className="text-emerald-700 font-medium flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Official application recorded in database
                    </span>
                  ) : deadlinePassed ? (
                    <span className="text-slate-500 italic">
                      Application window closed on {drive.applicationDeadline}. Specifications available for reference.
                    </span>
                  ) : !eligible ? (
                    <span className="text-rose-600 font-medium">
                      {elig?.message || 'Academic eligibility criteria not satisfied for this role.'}
                    </span>
                  ) : (
                    <span className="text-blue-700 font-medium">
                      Verified eligible. Open for application submission.
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                  <Link
                    to={`/student/drives/${drive.driveId}`}
                    className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-bold transition-colors shadow-2xs text-center"
                  >
                    <span>View Role Specifications</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          );
        })}

        {drives.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-500 shadow-xs">
            <Calendar className="w-12 h-12 mx-auto mb-3 text-slate-300" />
            <h3 className="text-sm font-bold text-slate-800">No Active Placement Drives</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto mt-1 leading-relaxed">
              There are currently no active campus placement drives open for applications. Drives whose application deadline has passed are automatically excluded from active listings. New opportunities will appear here once announced by authorized recruiters.
            </p>
          </div>
        ) : filteredDrives.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-500 shadow-xs">
            <Briefcase className="w-10 h-10 mx-auto mb-2 text-slate-300" />
            <p className="text-xs font-medium">No placement drives match your active search or filter selection.</p>
            <button
              onClick={() => {
                setSearchQuery('');
                setFilterEligibility('ALL');
              }}
              className="mt-3 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-md text-xs font-semibold transition-colors"
            >
              Reset Filters
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
