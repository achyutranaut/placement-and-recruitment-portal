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
} from 'lucide-react';

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
  const [filterEligibility, setFilterEligibility] = useState('ALL'); // ALL, ELIGIBLE, APPLIED

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

  const isEligible = (drive) => {
    const elig = eligibilityMap[drive.driveId];
    if (elig) {
      return elig.eligible;
    }
    const minCgpa = parseFloat(drive.minCgpa) || 0;
    return studentCgpa >= minCgpa;
  };

  const filteredDrives = drives.filter((drive) => {
    const matchesSearch =
      drive.jobTitle?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      drive.companyName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      drive.location?.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    if (filterEligibility === 'ELIGIBLE') {
      return isEligible(drive);
    }
    if (filterEligibility === 'APPLIED') {
      return appliedDriveIds.has(drive.driveId);
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
      {/* Breadcrumb Navigation */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/student" className="hover:text-slate-900 flex items-center gap-1 font-medium">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Student Dashboard
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Active Campus Placement Drives</span>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Verified CGPA:</span>
          <span className="px-2 py-0.5 rounded text-xs font-mono font-bold bg-slate-100 text-slate-900 border border-slate-300">
            {studentCgpa.toFixed(2)}
          </span>
        </div>
      </div>

      {/* Header Banner */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Campus Placement Drives</h1>
            <p className="text-xs text-slate-500 mt-1">
              Active recruitment opportunities for the graduating batch. Server-side validated against academic criteria.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-800 border border-blue-200">
              {drives.length} Active Drives
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
              {applications.length} Applications Filed
            </span>
          </div>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="bg-white rounded-lg border border-slate-200 p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by role, company name, or location..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter className="w-4 h-4 text-slate-500" />
          <select
            value={filterEligibility}
            onChange={(e) => setFilterEligibility(e.target.value)}
            className="px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900 bg-white"
          >
            <option value="ALL">All Drives ({drives.length})</option>
            <option value="ELIGIBLE">Eligible Only</option>
            <option value="APPLIED">Applied Drives ({applications.length})</option>
          </select>
        </div>
      </div>

      {/* Drives Grid / List */}
      <div className="space-y-4">
        {filteredDrives.map((drive) => {
          const elig = eligibilityMap[drive.driveId];
          const eligible = isEligible(drive);
          const hasApplied = appliedDriveIds.has(drive.driveId);
          const minCgpa = parseFloat(drive.minCgpa) || 0;

          return (
            <div
              key={drive.driveId}
              className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm hover:shadow-md transition-all flex flex-col md:flex-row md:items-center justify-between gap-6"
            >
              <div className="flex-1 space-y-3">
                <div className="flex flex-wrap items-center gap-2.5">
                  <span className="w-8 h-8 rounded bg-slate-100 border border-slate-300 flex items-center justify-center text-slate-700 font-bold font-mono text-xs shrink-0">
                    {drive.driveId}
                  </span>
                  <div>
                    <h2 className="text-base font-bold text-slate-900 leading-none">
                      {drive.jobTitle}
                    </h2>
                    <span className="text-xs font-semibold text-slate-600">
                      {drive.companyName || '—'}
                    </span>
                  </div>

                  {hasApplied ? (
                    <span className="ml-auto md:ml-0 px-2.5 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-300 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Applied
                    </span>
                  ) : eligible ? (
                    <span className="ml-auto md:ml-0 px-2.5 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-blue-50 text-blue-800 border border-blue-200 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5 text-blue-600" /> Eligible to Apply
                    </span>
                  ) : (
                    <span className="ml-auto md:ml-0 px-2.5 py-0.5 rounded text-[11px] font-semibold text-rose-700 bg-rose-50 border border-rose-200 flex items-center gap-1">
                      <XCircle className="w-3.5 h-3.5" /> {
                        elig && !elig.cgpaEligible ? `Ineligible: CGPA < ${minCgpa.toFixed(2)}`
                        : elig && !elig.programEligible ? `Ineligible: Program Not Eligible`
                        : elig && elig.deadlinePassed ? `Ineligible: Deadline Passed`
                        : `Ineligible: Requirements Not Met`
                      }
                    </span>
                  )}
                </div>

                {drive.jobDescription && (
                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                    {drive.jobDescription}
                  </p>
                )}

                <div className="flex flex-wrap gap-4 text-xs text-slate-600 pt-1">
                  <div className="flex items-center gap-1.5">
                    <Award className="w-4 h-4 text-emerald-600" />
                    <span>Package: <strong className="text-slate-900 font-mono">{formatPackage(drive.ctc ?? drive.packageLpa ?? drive.startingCtcLpa)}</strong></span>
                  </div>
                  {(drive.openings ?? drive.openingsCount) != null && (
                    <div className="flex items-center gap-1.5">
                      <Briefcase className="w-4 h-4 text-slate-400" />
                      <span>Openings: <strong className="text-slate-900 font-mono">{drive.openings ?? drive.openingsCount}</strong></span>
                    </div>
                  )}
                  <div className="flex items-center gap-1.5">
                    <Users className="w-4 h-4 text-slate-400" />
                    <span>Min CGPA: <strong className="text-slate-900 font-mono">{drive.minCgpa}</strong></span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-4 h-4 text-slate-400" />
                    <span>Drive Date: <strong className="text-slate-900">{drive.driveDate}</strong></span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-rose-500" />
                    <span>Deadline: <strong className="text-slate-900">{drive.applicationDeadline}</strong></span>
                  </div>
                  {drive.location && (
                    <div className="flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-slate-400" />
                      <span>{drive.location}</span>
                    </div>
                  )}
                </div>

                {(elig?.eligiblePrograms?.length > 0 || drive.eligibleBranches) && (
                  <div className="flex flex-wrap items-center gap-1.5 pt-1">
                    <span className="text-[11px] text-slate-400 uppercase font-semibold">Eligible:</span>
                    {(elig?.eligiblePrograms?.length > 0
                      ? elig.eligiblePrograms
                      : drive.eligibleBranches.split(',')
                    ).map((branch, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-0.5 bg-slate-100 text-slate-700 rounded text-[10px] font-medium border border-slate-200"
                      >
                        {branch.trim()}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex flex-col sm:flex-row md:flex-col justify-center gap-2 shrink-0 md:min-w-[160px]">
                <Link
                  to={`/student/drives/${drive.driveId}`}
                  className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-md text-xs font-bold transition-colors shadow-sm text-center"
                >
                  <span>View Role Details</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          );
        })}

        {filteredDrives.length === 0 && (
          <div className="bg-white rounded-lg border border-slate-200 p-12 text-center text-slate-400">
            <Briefcase className="w-10 h-10 mx-auto mb-2 text-slate-300" />
            <p className="text-xs">No placement drives match the specified search or filter criteria.</p>
          </div>
        )}
      </div>
    </div>
  );
}
