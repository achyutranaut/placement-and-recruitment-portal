import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  GraduationCap,
  Building2,
  ShieldCheck,
  LogOut,
  Radio,
  UserCheck,
} from 'lucide-react';

export default function Navbar() {
  const { user, logout, isBackendLive } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  if (!user) return null;

  const getRoleBadge = () => {
    switch (user.role) {
      case 'ROLE_STUDENT':
        return {
          label: 'Student',
          color: 'bg-slate-100 text-slate-800 border-slate-300',
          icon: GraduationCap,
        };
      case 'ROLE_RECRUITER':
        return {
          label: 'Recruiter',
          color: 'bg-blue-50 text-blue-800 border-blue-200',
          icon: Building2,
        };
      case 'ROLE_ADMIN':
        return {
          label: 'Placement Admin',
          color: 'bg-emerald-50 text-emerald-800 border-emerald-200',
          icon: ShieldCheck,
        };
      default:
        return {
          label: 'Portal User',
          color: 'bg-slate-50 text-slate-700 border-slate-200',
          icon: UserCheck,
        };
    }
  };

  const badge = getRoleBadge();
  const Icon = badge.icon;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Brand Logo & Academic Identity */}
          <div className="flex items-center gap-4">
            <Link to="/" className="flex items-center gap-3">
              <img
                src="/vit-logo.jpg"
                alt="VIT Logo"
                className="w-10 h-10 rounded-full object-cover"
              />
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-bold text-base text-slate-900 tracking-tight">
                    VIT Placement & Training Cell
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 font-medium">
                  Vellore Institute of Technology • Academic Year 2025–26
                </p>
              </div>
            </Link>
          </div>

          {/* Strict Role-Specific Navigation */}
          <nav className="hidden md:flex items-center gap-1">
            {user.role === 'ROLE_STUDENT' && (
              <>
                <Link
                  to="/student"
                  className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    location.pathname === '/student'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Dashboard
                </Link>
                <Link
                  to="/student/profile"
                  className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    location.pathname === '/student/profile'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Academic Profile
                </Link>
                <Link
                  to="/student/resume"
                  className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    location.pathname === '/student/resume'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Resume Vault
                </Link>
                <Link
                  to="/student/drives"
                  className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    location.pathname.startsWith('/student/drives')
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Placement Drives
                </Link>
                <Link
                  to="/student/applications"
                  className={`px-2.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                    location.pathname === '/student/applications'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Applications Tracker
                </Link>
              </>
            )}

            {user.role === 'ROLE_RECRUITER' && (
              <>
                <Link
                  to="/recruiter"
                  className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                    location.pathname === '/recruiter'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Candidate Tracking
                </Link>
              </>
            )}

            {user.role === 'ROLE_ADMIN' && (
              <>
                <Link
                  to="/admin"
                  className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                    location.pathname === '/admin'
                      ? 'bg-slate-100 text-slate-900 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  Placement Office & Database
                </Link>
              </>
            )}
          </nav>

          {/* Backend Health & Persona Menu */}
          <div className="flex items-center gap-3">
            {/* Live Database Connection Indicator */}
            <div
              className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border ${
                isBackendLive
                  ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                  : 'bg-rose-50 text-rose-800 border-rose-200'
              }`}
            >
              <Radio
                className={`w-3.5 h-3.5 ${
                  isBackendLive ? 'text-emerald-600 animate-pulse' : 'text-rose-500'
                }`}
              />
              <span className="hidden sm:inline">
                {isBackendLive ? 'Oracle Database • Connected' : 'Disconnected'}
              </span>
            </div>

            {/* Persona Switcher & Profile */}
            <div className="relative group">
              <button
                className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-semibold border ${badge.color} hover:bg-slate-100 transition-colors`}
              >
                <Icon className="w-4 h-4" />
                <span>{badge.label}</span>
                <span className="text-[10px] opacity-75">({user.referenceId})</span>
              </button>

              {/* Dropdown Menu */}
              <div className="absolute right-0 mt-1 w-60 bg-white border border-slate-200 rounded-lg shadow-lg py-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 z-50">
                <div className="px-3 py-1.5 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  Signed in as {user.username}
                </div>
                <div className="px-3 py-1 text-xs text-slate-700 font-medium">
                  {user.name}
                </div>
                <div className="px-3 py-1 text-[11px] text-slate-500">
                  Role: <span className="font-semibold text-slate-800">{badge.label}</span>
                </div>
                {user.referenceId && (
                  <div className="px-3 py-1 text-[11px] font-mono text-slate-500">
                    ID: {user.referenceId}
                  </div>
                )}
                <div className="border-t border-slate-100 my-1.5"></div>
                <button
                  onClick={logout}
                  className="w-full px-3 py-1.5 text-left text-xs text-rose-600 hover:bg-rose-50 flex items-center gap-2 font-medium"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Sign Out
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
