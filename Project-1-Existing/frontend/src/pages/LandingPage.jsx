import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  GraduationCap,
  Building2,
  ShieldCheck,
  ArrowRight,
  Database,
  FileCheck,
  Award,
  Users,
  CheckCircle2,
  BookOpen,
  Briefcase,
  Layers,
} from 'lucide-react';

export default function LandingPage() {
  const { user, isBackendLive } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Top University Placement Header */}
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img
              src="/vit-logo.jpg"
              alt="VIT Logo"
              className="w-11 h-11 rounded-full object-cover shadow-sm"
            />
            <div>
              <div className="flex items-center gap-2">
                <span className="text-base font-bold tracking-tight text-slate-900">
                  VIT Placement & Training Cell
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-slate-100 text-slate-700 border border-slate-300">
                  DA2 Enterprise
                </span>
              </div>
              <p className="text-xs text-slate-500 font-medium">
                Vellore Institute of Technology • Centre for Career Planning & University Industry Relations
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span
              className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${
                isBackendLive
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                  : 'bg-slate-100 text-slate-600 border-slate-300'
              }`}
            >
              <Database className="w-3.5 h-3.5" />
              {isBackendLive ? 'Oracle Database • Connected' : 'Checking Backend (:8080)...'}
            </span>

            {user ? (
              <button
                onClick={() => {
                  if (user.role === 'ROLE_STUDENT') navigate('/student');
                  else if (user.role === 'ROLE_RECRUITER') navigate('/recruiter');
                  else navigate('/admin');
                }}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition-colors shadow-sm"
              >
                Go to Dashboard ({user.username})
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            ) : (
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition-colors shadow-sm"
              >
                Portal Sign In
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            )}
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <div className="bg-white border-b border-slate-200 py-12 sm:py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-100 text-slate-700 border border-slate-200 text-xs font-medium mb-4">
            <Layers className="w-3.5 h-3.5 text-slate-500" />
            <span>Academic Year 2025–2026 Campus Placement Drive Cycle</span>
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-slate-900 max-w-4xl mx-auto">
            Integrated Campus Placement & Recruitment Management System
          </h1>
          <p className="mt-4 text-sm sm:text-base text-slate-600 max-w-2xl mx-auto leading-relaxed">
            Institutional career placement infrastructure powered by Oracle SQL/PLSQL relational architecture, Spring Boot security, and comprehensive candidate lifecycle tracking.
          </p>

          <div className="mt-6 flex flex-wrap items-center justify-center gap-3 text-xs text-slate-500">
            <span className="flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Strict DA1 BCNF Relational Schema
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Oracle BLOB Resume Vault
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Server-side Eligibility Verification
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Multi-Round ATS Pipeline
            </span>
          </div>
        </div>
      </div>

      {/* Dedicated Portal Entry Points */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 flex-1">
        <div className="text-center mb-10">
          <h2 className="text-xl font-bold text-slate-900">Select Dedicated Access Portal</h2>
          <p className="text-xs text-slate-500 mt-1">
            Choose your designated gateway for authenticated university placement workflows
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto">
          {/* Student Portal Card */}
          <div className="bg-white rounded-xl border border-slate-200 p-8 shadow-sm hover:shadow-md transition-all flex flex-col justify-between group">
            <div>
              <div className="w-14 h-14 rounded-lg bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-700 mb-5 group-hover:bg-blue-100 transition-colors">
                <GraduationCap className="w-8 h-8" />
              </div>
              <div className="flex items-center gap-2 mb-2">
                <h3 className="text-xl font-bold text-slate-900">Student Placement Portal</h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-blue-50 text-blue-800 border border-blue-200">
                  Undergraduate & PG
                </span>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed mb-6">
                Discover active campus placement drives, verify CGPA eligibility, maintain versioned resumes stored directly in Oracle BLOB, and monitor application milestones.
              </p>

              <div className="space-y-2.5 mb-8 text-xs text-slate-700">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Verified academic record & skills profile management</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Resume upload with BLOB persistence and preview</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Real-time drive discovery & CGPA eligibility validation</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>PL/SQL driven application confirmation & offer tracking</span>
                </div>
              </div>
            </div>

            <Link
              to="/login?role=STUDENT"
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 rounded-lg bg-blue-700 hover:bg-blue-800 text-white text-xs font-bold tracking-wide transition-colors shadow-sm"
            >
              <span>Enter Student Portal</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Recruiter Portal Card */}
          <div className="bg-white rounded-xl border border-slate-200 p-8 shadow-sm hover:shadow-md transition-all flex flex-col justify-between group">
            <div>
              <div className="w-14 h-14 rounded-lg bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-700 mb-5 group-hover:bg-indigo-100 transition-colors">
                <Building2 className="w-8 h-8" />
              </div>
              <div className="flex items-center gap-2 mb-2">
                <h3 className="text-xl font-bold text-slate-900">Corporate Recruiter Portal</h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-indigo-50 text-indigo-800 border border-indigo-200">
                  Industry Partners
                </span>
              </div>
              <p className="text-xs text-slate-600 leading-relaxed mb-6">
                Manage recruitment drives, review student profiles, evaluate candidate resumes with structured scoring rubrics, manage interview rounds, and issue offer letters.
              </p>

              <div className="space-y-2.5 mb-8 text-xs text-slate-700">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0" />
                  <span>Role opening status & applicant stage distribution</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0" />
                  <span>ATS-style multi-criteria resume evaluation rubric</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0" />
                  <span>DA1 pinned interviewer round scoring (OA / Tech / HR)</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-indigo-600 shrink-0" />
                  <span>PL/SQL automated offer generation & audit logging</span>
                </div>
              </div>
            </div>

            <Link
              to="/login?role=RECRUITER"
              className="w-full inline-flex items-center justify-center gap-2 px-5 py-3 rounded-lg bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold tracking-wide transition-colors shadow-sm"
            >
              <span>Enter Recruiter Portal</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {/* Administrative Placement Cell Gateway */}
        <div className="mt-8 max-w-5xl mx-auto">
          <div className="bg-slate-100 rounded-lg border border-slate-200 p-4 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3 text-left">
              <div className="w-9 h-9 rounded-md bg-white border border-slate-300 flex items-center justify-center text-slate-700 shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <span className="text-xs font-bold text-slate-900">Placement Cell Administration & Faculty Officers</span>
                <p className="text-[11px] text-slate-500">
                  Access institutional analytics, Oracle PL/SQL stored procedure console, trigger audits, and compliance reports.
                </p>
              </div>
            </div>
            <Link
              to="/login?role=ADMIN"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-white hover:bg-slate-50 text-slate-800 border border-slate-300 text-xs font-semibold shrink-0 transition-colors shadow-sm"
            >
              <span>Admin Access</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
