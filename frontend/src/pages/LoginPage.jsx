import { useState } from "react";
import { Link, useNavigate, useSearchParams, useLocation } from "react-router-dom";

import { useAuth } from "../context/AuthContext";
import {
  GraduationCap,
  Building2,
  ShieldCheck,
  ArrowRight,
  Database,
  Lock,
  User,
  AlertCircle,
  UserPlus,
} from "lucide-react";

export default function LoginPage() {
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const roleParam = searchParams.get("role") || (location.pathname.includes("student") ? "STUDENT" : location.pathname.includes("recruiter") ? "RECRUITER" : "");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { login, isBackendLive } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    if (e) e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const authResult = await login(username.trim(), password);
      const user = authResult.user || authResult;
      const authorizedComps = authResult.authorizedCompanies || [];

      if (user.role === "ROLE_STUDENT") {
        navigate("/student");
      } else if (user.role === "ROLE_RECRUITER") {
        if (authorizedComps.length > 1) {
          navigate("/recruiter/select-company");
        } else {
          navigate("/recruiter");
        }
      } else {
        navigate("/admin");
      }
    } catch (err) {
      setError(err.message || "Authentication failed. Please verify credentials.");
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        {/* Institutional Monogram */}
        <div className="flex justify-center">
          <img
            src="/vit-logo.jpg"
            alt="VIT Logo"
            className="w-14 h-14 rounded-full object-cover shadow-sm"
          />
        </div>
        <h2 className="mt-4 text-center text-xl font-bold tracking-tight text-slate-900">
          VIT Placement & Training Cell
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500 font-medium">
          Vellore Institute of Technology • Spring Boot & Oracle Database
        </p>

        {/* Backend Status */}
        <div className="mt-3 flex justify-center">
          <span
            className={"inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full text-xs font-semibold border " +
              (isBackendLive
                ? "bg-emerald-50 text-emerald-800 border-emerald-300"
                : "bg-slate-100 text-slate-600 border-slate-300")}
          >
            <Database className="w-3.5 h-3.5" />
            {isBackendLive
              ? "Backend Online (Spring Boot API :8080)"
              : "Checking Backend Connection (:8080)..."}
          </span>
        </div>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm sm:rounded-lg sm:px-8 border border-slate-200">
          {error && (
            <div className="mb-4 p-3 rounded-md bg-rose-50 border border-rose-200 flex items-start gap-2 text-xs text-rose-800">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700">
                {roleParam === "STUDENT"
                  ? "VIT Email / Registration No / Username"
                  : roleParam === "RECRUITER"
                  ? "Recruiter Username / Corporate Email / Full Name"
                  : "Account Username"}
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <User className="h-4 w-4" />
                </div>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  placeholder={
                    roleParam === "STUDENT"
                      ? "e.g. 21BCE1001 or name@vitstudent.ac.in"
                      : roleParam === "RECRUITER"
                      ? "e.g. username, email, or full name"
                      : "e.g. admin"
                  }
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700">Password</label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Lock className="h-4 w-4" />
                </div>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-2 px-4 border border-transparent rounded-md text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-500 transition-all disabled:opacity-50"
            >
              {loading ? (
                <span>Authenticating with Backend...</span>
              ) : (
                <>
                  <span>Sign In to Portal</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </form>

          {/* Student & Recruiter Registration Callouts */}
          <div className="mt-4 pt-3 border-t border-slate-100 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="text-slate-500">New VIT Student?</span>
              <Link
                to="/student/register"
                className="font-semibold text-slate-900 hover:underline inline-flex items-center gap-1 text-xs"
              >
                <UserPlus className="w-3.5 h-3.5" />
                Register for Placements
              </Link>
            </div>
            <div className="flex items-center justify-between pt-1">
              <span className="text-slate-500">Corporate Recruiter?</span>
              <Link
                to="/recruiter/register"
                className="font-semibold text-slate-900 hover:underline inline-flex items-center gap-1 text-xs"
              >
                <Building2 className="w-3.5 h-3.5" />
                Register as Recruiter
              </Link>
            </div>
          </div>

          {/* Database Architecture Summary */}
          <div className="mt-6 pt-4 border-t border-slate-100 text-[11px] text-slate-500 space-y-1">
            <p className="font-semibold text-slate-700">Academic Architecture Reference:</p>
            <p>• 13 Base BCNF Tables + 2 Junction Tables (Phase 1 Locked)</p>
            <p>• Pinned Interviewers: INTERVIEWER_ROUND (1: OA, 2: GD, 3: HR)</p>
            <p>• Triggers TRG_APPLICATION_AUDIT & TRG_OFFER_APPLICATION_STATUS</p>
          </div>
        </div>
      </div>
    </div>
  );
}
