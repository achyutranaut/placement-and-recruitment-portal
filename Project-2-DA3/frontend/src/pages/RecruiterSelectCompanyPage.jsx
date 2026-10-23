import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
  Building2,
  CheckCircle2,
  ArrowRight,
  ShieldCheck,
  AlertCircle,
  Briefcase,
  LogOut,
} from "lucide-react";

export default function RecruiterSelectCompanyPage() {
  const { user, authorizedCompanies, activeCompany, selectCompany, fetchRecruiterCompanies, logout } = useAuth();
  const [selectedId, setSelectedId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [companies, setCompanies] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    async function load() {
      let list = authorizedCompanies;
      if (!list || list.length === 0) {
        list = await fetchRecruiterCompanies();
      }
      setCompanies(list || []);

      if (activeCompany) {
        setSelectedId(activeCompany.companyId);
      } else if (list && list.length > 0) {
        setSelectedId(list[0].companyId);
      }
    }
    load();
  }, []);

  const handleContinue = async (e) => {
    if (e) e.preventDefault();
    if (!selectedId) {
      setError("Please select an organization to proceed.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      await selectCompany(selectedId);
      navigate("/recruiter");
    } catch (err) {
      setError(err.message || "Failed to establish company context.");
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
          Select Recruiting Organization
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500 font-medium">
          Recruiter: <span className="font-bold text-slate-800">{user?.name || user?.username}</span>
        </p>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm sm:rounded-lg sm:px-8 border border-slate-200">
          {error && (
            <div className="mb-4 p-3 rounded-md bg-rose-50 border border-rose-200 flex items-start gap-2 text-xs text-rose-800">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {companies.length === 0 ? (
            <div className="space-y-4">
              <div className="p-4 rounded-md bg-amber-50 border border-amber-200 text-xs text-amber-800 space-y-2">
                <div className="flex items-center gap-2 font-bold text-amber-900 text-sm">
                  <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
                  <span>No Authorized Organizations Found</span>
                </div>
                <p>
                  Your recruiter account <span className="font-mono font-bold">({user?.username})</span> is not currently authorized to represent any hiring organizations in the system.
                </p>
                <p className="text-[11px] text-amber-700">
                  Please contact the university Placement & Training Cell administrator to map your account to your hiring organization.
                </p>
              </div>

              <button
                onClick={logout}
                className="w-full flex items-center justify-center gap-2 py-2 px-4 rounded-md text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-300 transition-all"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Return to Login / Sign Out</span>
              </button>
            </div>
          ) : (
            <form onSubmit={handleContinue} className="space-y-4">
              <div className="text-xs font-semibold text-slate-700 mb-2">
                You are authorized to recruit for:
              </div>

              <div className="space-y-2.5">
                {companies.map((c) => {
                  const isChecked = selectedId === c.companyId;
                  return (
                    <div
                      key={c.companyId}
                      onClick={() => setSelectedId(c.companyId)}
                      className={"p-3.5 rounded-lg border transition-all cursor-pointer flex items-center justify-between " +
                        (isChecked
                          ? "border-slate-900 bg-slate-50/70 shadow-sm"
                          : "border-slate-200 hover:border-slate-300 bg-white")}
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={"w-4 h-4 rounded-full border flex items-center justify-center transition-all " +
                            (isChecked ? "border-slate-900 bg-slate-900 text-white" : "border-slate-400 bg-white")}
                        >
                          {isChecked && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 text-xs">
                            {c.companyName}
                          </div>
                          <div className="text-[11px] text-slate-500 font-mono">
                            {c.companyId} • {c.industry || "Recruiting Partner"}
                          </div>
                        </div>
                      </div>
                      <Building2 className={"w-4 h-4 " + (isChecked ? "text-slate-900" : "text-slate-400")} />
                    </div>
                  );
                })}
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                <button
                  type="button"
                  onClick={logout}
                  className="text-xs font-semibold text-slate-500 hover:text-slate-800"
                >
                  Switch Account
                </button>
                <button
                  type="submit"
                  disabled={loading || !selectedId}
                  className="flex items-center gap-1.5 py-2 px-5 rounded-md text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 transition-all disabled:opacity-50 shadow-sm"
                >
                  <span>{loading ? "Establishing Context..." : "Continue to Dashboard"}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
