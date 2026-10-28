import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import {
  Search,
  Database,
  Cpu,
  Sparkles,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Sliders,
  Briefcase,
  GraduationCap,
  FileText,
  Building2,
  ChevronRight,
  ExternalLink,
  Shield,
  Layers,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function SemanticSearchPage() {
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const [entityType, setEntityType] = useState(
    user?.role === 'ROLE_STUDENT' ? 'DRIVE' : 'ALL'
  );
  const [minScore, setMinScore] = useState(0.25);
  const [limit, setLimit] = useState(10);

  const [loading, setLoading] = useState(false);
  const [indexing, setIndexing] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [results, setResults] = useState(null);
  const [serviceStatus, setServiceStatus] = useState(null);
  const [statusLoading, setStatusLoading] = useState(true);
  const [error, setError] = useState(null);
  const [indexMessage, setIndexMessage] = useState(null);

  const isStudent = user?.role === 'ROLE_STUDENT';
  const isAdmin = user?.role === 'ROLE_ADMIN';

  // Fetch Qdrant service status on mount
  const fetchStatus = async () => {
    setStatusLoading(true);
    try {
      const res = await api.getSemanticSearchStatus();
      if (res?.success && res.data) {
        setServiceStatus(res.data);
      } else {
        setServiceStatus(null);
      }
    } catch (err) {
      console.warn('Failed to fetch Qdrant status:', err.message);
      setServiceStatus({
        enabled: false,
        connected: false,
        state: 'NOT_CONNECTED',
        message: err.message,
      });
    } finally {
      setStatusLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  const handleSearch = async (e) => {
    if (e) e.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setIndexMessage(null);

    try {
      const targetType = isStudent ? 'DRIVE' : entityType;
      const res = await api.executeSemanticSearch({
        query: query.trim(),
        entityType: targetType,
        limit: Number(limit),
        minScore: Number(minScore),
      });

      if (res?.success && res.data) {
        setResults(res.data);
      } else {
        throw new Error(res?.message || 'Search execution failed');
      }
    } catch (err) {
      setError(err.message || 'Error executing semantic search against Qdrant vector database');
      setResults(null);
    } finally {
      setLoading(false);
    }
  };

  const handleReindex = async () => {
    if (!isAdmin) return;
    setIndexing(true);
    setError(null);
    setIndexMessage(null);
    try {
      const res = await api.reindexSemanticSearch();
      if (res?.success && res.data) {
        setIndexMessage(
          `Successfully synchronized ${res.data.totalPointsInCollection} Oracle records into Qdrant collection in ${res.data.durationMs}ms.`
        );
        fetchStatus();
      } else {
        throw new Error(res?.message || 'Indexing failed');
      }
    } catch (err) {
      setError(err.message || 'Failed to reindex Oracle records into Qdrant');
    } finally {
      setIndexing(false);
    }
  };

  const handleReconnect = async () => {
    if (!isAdmin) return;
    setReconnecting(true);
    try {
      await api.reconnectQdrant();
      await fetchStatus();
    } catch (err) {
      setError(err.message || 'Reconnect failed');
    } finally {
      setReconnecting(false);
    }
  };

  const exampleQueries = isStudent
    ? [
        'Cloud microservices Kubernetes and distributed architecture',
        'Spring Boot backend development with Oracle SQL database',
        'Frontend React UI with TypeScript and Tailwind CSS',
        'Machine learning pipelines, Python, and data analytics',
      ]
    : [
        'Java Spring Boot developer with microservices and Docker experience',
        'Data science candidate proficient in Python, PyTorch, and NLP',
        'Competitive programmer with DSA, C++, and low-latency systems',
        'Full-stack developer experienced in React, Node.js, and PostgreSQL',
      ];

  const getConfidenceBadge = (confidence, score) => {
    const pct = Math.round(score * 100);
    if (confidence === 'HIGH') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
          <Sparkles className="w-3 h-3 text-emerald-600" />
          {pct}% Match • High Relevance
        </span>
      );
    }
    if (confidence === 'MEDIUM') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-300">
          {pct}% Match • Moderate Relevance
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-300">
        {pct}% Match • Contextual
      </span>
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-indigo-50 border border-indigo-200 text-indigo-700">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                Semantic Recruitment Search
              </h1>
              <p className="text-xs sm:text-sm text-slate-500">
                Qdrant Vector Database Engine • 384-Dimensional Neural Embeddings • Oracle Source-of-Truth
              </p>
            </div>
          </div>
        </div>

        {/* Live Vector Service Status Badge */}
        <div className="flex items-center gap-2.5 flex-wrap">
          {statusLoading ? (
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-medium bg-slate-100 text-slate-600 border border-slate-200">
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
              Checking Vector DB...
            </div>
          ) : serviceStatus?.enabled && serviceStatus?.connected ? (
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-300">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Qdrant Active • {serviceStatus.collection} ({serviceStatus.pointsCount || 0} vectors)</span>
            </div>
          ) : serviceStatus?.enabled && !serviceStatus?.connected ? (
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-300">
              <XCircle className="w-4 h-4 text-rose-600" />
              <span>Qdrant Unreachable</span>
            </div>
          ) : (
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-300">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              <span>Qdrant Disabled (Oracle OLTP Mode)</span>
            </div>
          )}

          {isAdmin && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleReindex}
                disabled={indexing || !serviceStatus?.connected}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white transition-colors disabled:opacity-50"
                title="Synchronize all students, drives, and resumes from Oracle to Qdrant"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${indexing ? 'animate-spin' : ''}`} />
                {indexing ? 'Syncing...' : 'Sync Oracle -> Qdrant'}
              </button>

              <button
                type="button"
                onClick={handleReconnect}
                disabled={reconnecting}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium border border-slate-300 hover:bg-slate-100 text-slate-700 transition-colors disabled:opacity-50"
                title="Re-verify Qdrant connection"
              >
                <Cpu className="w-3.5 h-3.5" />
                Reconnect
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Informational Alerts */}
      {indexMessage && (
        <div className="p-4 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs sm:text-sm flex items-start gap-2.5">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
          <div>{indexMessage}</div>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs sm:text-sm flex items-start gap-2.5">
          <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
          <div>
            <div className="font-semibold">Vector Database Request Error</div>
            <p className="mt-0.5 text-xs text-rose-700">{error}</p>
          </div>
        </div>
      )}

      {serviceStatus && !serviceStatus.enabled && (
        <div className="p-4 rounded-lg bg-amber-50 border border-amber-200 text-amber-900 text-xs sm:text-sm flex items-start gap-2.5">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">Qdrant Vector Database Is Currently Disabled</div>
            <p className="mt-1 text-xs text-amber-800">
              The application is running in authoritative Oracle OLTP mode. To enable Qdrant semantic search,
              set <code className="bg-amber-100 px-1 py-0.5 rounded font-mono">PORTAL_QDRANT_ENABLED=true</code> in your{' '}
              <code className="bg-amber-100 px-1 py-0.5 rounded font-mono">.env</code> and restart the portal launcher.
            </p>
          </div>
        </div>
      )}

      {/* Main Search Panel */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-4">
        <form onSubmit={handleSearch} className="space-y-4">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <Search className="w-5 h-5" />
            </div>
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={
                isStudent
                  ? "Describe desired job, skills, or tech stack (e.g. 'Cloud microservices Docker Kubernetes')..."
                  : "Search candidate profiles, resume keywords, or job requirements semantically..."
              }
              className="block w-full pl-11 pr-28 py-3 rounded-lg border border-slate-300 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors shadow-sm"
            />
            <div className="absolute inset-y-0 right-0 pr-2 flex items-center">
              <button
                type="submit"
                disabled={loading || !query.trim() || !serviceStatus?.connected}
                className="flex items-center gap-1.5 px-4 py-2 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow transition-colors disabled:opacity-50"
              >
                {loading ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    Searching...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-3.5 h-3.5" />
                    Search
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Search Controls & Filters */}
          <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-slate-100 text-xs">
            {/* Entity Filter */}
            <div className="flex items-center gap-2">
              <span className="text-slate-500 font-medium">Filter Target:</span>
              {isStudent ? (
                <span className="px-2.5 py-1 rounded-md bg-blue-50 text-blue-800 border border-blue-200 font-semibold flex items-center gap-1">
                  <Briefcase className="w-3.5 h-3.5" />
                  Placement Drives & Job Requirements
                </span>
              ) : (
                <div className="inline-flex rounded-md shadow-sm border border-slate-200 p-0.5 bg-slate-50">
                  {[
                    { id: 'ALL', label: 'All Entities' },
                    { id: 'STUDENT', label: 'Candidates' },
                    { id: 'DRIVE', label: 'Placement Drives' },
                    { id: 'RESUME', label: 'Resumes' },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => setEntityType(tab.id)}
                      className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
                        entityType === tab.id
                          ? 'bg-white text-slate-900 shadow-sm font-semibold'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Threshold & Max Results */}
            <div className="flex items-center gap-4 text-slate-600">
              <div className="flex items-center gap-2">
                <Sliders className="w-3.5 h-3.5 text-slate-400" />
                <label className="text-slate-500">Min Cosine Score:</label>
                <select
                  value={minScore}
                  onChange={(e) => setMinScore(Number(e.target.value))}
                  className="rounded border border-slate-300 py-0.5 px-2 text-xs bg-white text-slate-700"
                >
                  <option value={0.15}>0.15 (Broad)</option>
                  <option value={0.25}>0.25 (Balanced)</option>
                  <option value={0.4}>0.40 (Strict)</option>
                  <option value={0.6}>0.60 (High Precision)</option>
                </select>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-slate-500">Limit:</label>
                <select
                  value={limit}
                  onChange={(e) => setLimit(Number(e.target.value))}
                  className="rounded border border-slate-300 py-0.5 px-2 text-xs bg-white text-slate-700"
                >
                  <option value={5}>5</option>
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                </select>
              </div>
            </div>
          </div>
        </form>

        {/* Quick Example Queries */}
        <div className="pt-2">
          <div className="flex items-center gap-1.5 text-slate-400 text-xs mb-2">
            <Sparkles className="w-3 h-3 text-indigo-500" />
            <span>Try sample semantic recruitment queries:</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {exampleQueries.map((ex, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => {
                  setQuery(ex);
                }}
                className="text-xs px-2.5 py-1 rounded-md bg-slate-100 hover:bg-indigo-50 hover:text-indigo-700 text-slate-600 transition-colors border border-slate-200"
              >
                {ex}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results Section */}
      <div className="space-y-4">
        {loading && (
          <div className="p-12 text-center bg-white rounded-xl border border-slate-200">
            <div className="inline-flex p-3 rounded-full bg-indigo-50 text-indigo-600 animate-pulse mb-3">
              <Cpu className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-semibold text-slate-900">
              Generating Embeddings & Querying Qdrant...
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Calculating cosine distance across 384-dimensional vector space
            </p>
          </div>
        )}

        {!loading && results && (
          <div>
            {/* Result Stats Bar */}
            <div className="flex items-center justify-between text-xs text-slate-500 pb-2">
              <div>
                Found <span className="font-semibold text-slate-900">{results.totalFound}</span> semantic matches for{' '}
                <span className="italic font-medium text-slate-700">"{results.query}"</span>
              </div>
              <div className="flex items-center gap-3">
                <span>Database: <strong className="text-slate-700">Qdrant</strong></span>
                <span>Dim: <strong className="text-slate-700">{results.vectorDimension}</strong></span>
                <span>Latency: <strong className="text-slate-700">{results.executionTimeMs}ms</strong></span>
              </div>
            </div>

            {results.totalFound === 0 ? (
              <div className="p-12 text-center bg-white rounded-xl border border-slate-200">
                <Search className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                <h3 className="text-sm font-semibold text-slate-800">No Matching Vectors Found</h3>
                <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
                  No records met the minimum similarity score ({minScore}). Try lowering the score threshold
                  or indexing recent records into Qdrant.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {results.results.map((item, index) => (
                  <div
                    key={item.entityId + '-' + index}
                    className="p-5 bg-white rounded-xl border border-slate-200 hover:border-indigo-300 hover:shadow-md transition-all space-y-3"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`p-2 rounded-lg ${
                            item.entityType === 'STUDENT'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : item.entityType === 'DRIVE'
                              ? 'bg-blue-50 text-blue-700 border border-blue-200'
                              : 'bg-purple-50 text-purple-700 border border-purple-200'
                          }`}
                        >
                          {item.entityType === 'STUDENT' ? (
                            <GraduationCap className="w-5 h-5" />
                          ) : item.entityType === 'DRIVE' ? (
                            <Briefcase className="w-5 h-5" />
                          ) : (
                            <FileText className="w-5 h-5" />
                          )}
                        </div>

                        <div>
                          <div className="flex items-center gap-2 flex-wrap">
                            <h3 className="text-base font-bold text-slate-900 tracking-tight">
                              {item.title}
                            </h3>
                            <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
                              {item.entityId}
                            </span>
                          </div>
                          <p className="text-xs text-slate-500 font-medium">{item.subtitle}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        {getConfidenceBadge(item.matchConfidence, item.similarityScore)}
                      </div>
                    </div>

                    {/* Summary Context */}
                    {item.summary && (
                      <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-2.5 rounded-md border border-slate-100">
                        {item.summary}
                      </p>
                    )}

                    {/* Tags & Action */}
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1">
                      <div className="flex flex-wrap gap-1.5">
                        {item.tags?.map((tag, tIdx) => (
                          <span
                            key={tIdx}
                            className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-700 border border-slate-200"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>

                      {item.entityType === 'DRIVE' && (
                        <Link
                          to={`/student/drives/${item.entityId}`}
                          className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
                        >
                          View Placement Drive
                          <ChevronRight className="w-3.5 h-3.5" />
                        </Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
