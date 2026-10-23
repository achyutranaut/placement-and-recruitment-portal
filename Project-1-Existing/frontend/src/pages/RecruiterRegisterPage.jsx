import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import {
  Building2,
  Lock,
  User,
  Mail,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";

export default function RecruiterRegisterPage() {
  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [selectedCompanyId, setSelectedCompanyId] = useState("");
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const { registerRecruiter } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    async function loadCompanies() {
      try {
        const list = await api.getCompanies();
        setCompanies(Array.isArray(list) ? list : []);
        if (list && list.length > 0) {
          setSelectedCompanyId(list[0].companyId);
        }
      } catch (e) {
        console.warn("Could not load company catalog:", e);
      }
    }
    loadCompanies();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("Passwords do not match. Please verify your password entry.");
      return;
    }

    if (password.length < 6) {
      setError("Password must contain at least 6 characters.");
      return;
    }

    setLoading(true);
    try {
      await registerRecruiter({
        username: username.trim(),
        password,
        fullName: fullName.trim(),
        email: email.trim(),
        companyId: selectedCompanyId,
      });
      setSuccess(true);
    } catch (err) {
      setError(err.message || "Recruiter registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <img
            src="/vit-logo.jpg"
            alt="VIT Logo"
            className="w-14 h-14 rounded-full object-cover shadow-sm"
          />
        </div>
        <h2 className="mt-4 text-center text-xl font-bold tracking-tight text-slate-900">
          Register Corporate Recruiter
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500 font-medium">
          VIT Placement & Training Cell • Corporate Hiring Division
        </p>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm sm:rounded-lg sm:px-8 border border-slate-200">
          {success ? (
            <div className="space-y-4">
              <div className="p-4 rounded-md bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 space-y-2">
                <div className="flex items-center gap-2 font-bold text-sm text-emerald-900">
                  <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                  <span>Recruiter Account Created</span>
                </div>
                <p>
                  Your recruiter account has been registered and authorized for the selected hiring organization.
                </p>
                <div className="p-2.5 bg-white/80 rounded border border-emerald-200 font-mono text-[11px]">
                  <strong>Username:</strong> {username}
                  <br />
                  <strong>Name:</strong> {fullName}
                  <br />
                  <strong>Authorized Organization:</strong> {companies.find((c) => c.companyId === selectedCompanyId)?.companyName || selectedCompanyId}
                </div>
                <p className="text-[11px] text-emerald-700">
                  Your credentials and organizational permissions are saved in Oracle. You can now access your company's drives and applicant pool.
                </p>
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  onClick={() => navigate("/recruiter")}
                  className="w-full flex items-center justify-center gap-2 py-2 px-4 rounded-md text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 transition-all"
                >
                  <span>Enter Recruiter Workspace</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-3 rounded-md bg-rose-50 border border-rose-200 flex items-start gap-2 text-xs text-rose-800">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
                  <span>{error}</span>
                </div>
              )}

              {/* Full Name */}
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Recruiter Full Name
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <User className="h-4 w-4" />
                  </div>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                    placeholder="e.g. Recruiter Full Name"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              {/* Corporate Email */}
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Corporate Work Email
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Mail className="h-4 w-4" />
                  </div>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    placeholder="e.g. recruiter@company.com"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              {/* Account Username */}
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Account Username
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
                    placeholder="e.g. corporate_recruiter"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              {/* Company Affiliation / Request */}
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Affiliated Organization (Oracle Master Catalog)
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Building2 className="h-4 w-4" />
                  </div>
                  <select
                    value={selectedCompanyId}
                    onChange={(e) => setSelectedCompanyId(e.target.value)}
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500 bg-white"
                  >
                    {companies.map((c) => (
                      <option key={c.companyId} value={c.companyId}>
                        {c.companyName} ({c.companyId})
                      </option>
                    ))}
                  </select>
                </div>
                <p className="mt-1 text-[11px] text-slate-500">
                  * Selected organization is submitted for administrative authorization verification.
                </p>
              </div>

              {/* Password */}
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
                    placeholder="••••••••"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-xs font-semibold text-slate-700">Confirm Password</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    placeholder="••••••••"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:outline-none focus:ring-1 focus:ring-slate-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 py-2 px-4 border border-transparent rounded-md text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-500 transition-all disabled:opacity-50"
              >
                {loading ? (
                  <span>Registering Recruiter Profile...</span>
                ) : (
                  <>
                    <span>Complete Registration</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            </form>
          )}

          <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
            <span className="text-slate-500">Already registered?</span>
            <Link
              to="/login?role=RECRUITER"
              className="font-semibold text-slate-900 hover:underline"
            >
              Sign In to Recruiter Portal
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
