import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import {
  FileText,
  Upload,
  Download,
  Eye,
  CheckCircle2,
  AlertCircle,
  Clock,
  HardDrive,
  FileCheck,
  ArrowLeft,
  RefreshCw,
  ExternalLink,
  Trash2,
  AlertTriangle,
  Check,
  Lock,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import Modal from '../components/Modal';

export default function StudentResumePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const studentId = user?.referenceId;

  const [resumes, setResumes] = useState([]);
  const [currentResume, setCurrentResume] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [notification, setNotification] = useState(null);
  const [previewResumeId, setPreviewResumeId] = useState(null);
  const [previewNonce, setPreviewNonce] = useState(() => Date.now());
  const [resumeToDelete, setResumeToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [activatingId, setActivatingId] = useState(null);

  const fileInputRef = useRef(null);

  const isResumeActive = (r) =>
    Boolean(r && (r.isCurrent === 'Y' || r.isActive === 'Y' || r.active === true));

  const loadResumes = async ({ showSpinner = false, syncPreviewToActive = false } = {}) => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return null;
    }

    try {
      if (showSpinner) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }
      const [allResumes, activeResume] = await Promise.all([
        api.getStudentResumes(studentId),
        api.getCurrentResume(studentId),
      ]);
      const normalizedList = Array.isArray(allResumes) ? allResumes : [];
      const resolvedActive =
        activeResume || normalizedList.find((r) => isResumeActive(r)) || null;

      setResumes(normalizedList);
      setCurrentResume(resolvedActive);

      if (resolvedActive) {
        setPreviewResumeId((prev) => {
          const prevStillExists = prev && normalizedList.some((r) => r.resumeId === prev);
          if (syncPreviewToActive || !prevStillExists) {
            return resolvedActive.resumeId;
          }
          return prev;
        });
      } else {
        setPreviewResumeId(normalizedList[0]?.resumeId || null);
      }
      setPreviewNonce(Date.now());
      return { allResumes: normalizedList, activeResume: resolvedActive };
    } catch (err) {
      console.error('Failed to load resumes:', err);
      setNotification({
        type: 'error',
        title: 'Synchronization Error',
        message: err.message || 'Failed to retrieve resume history from Oracle BLOB storage.',
      });
      return null;
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadResumes({ showSpinner: true, syncPreviewToActive: true });
  }, [studentId]);

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate file format
      const validTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword'];
      if (!validTypes.includes(file.type) && !file.name.endsWith('.pdf') && !file.name.endsWith('.docx')) {
        setNotification({
          type: 'error',
          title: 'Upload Error',
          message: 'Invalid file format. Please upload a PDF or Word document (.pdf, .docx).',
        });
        return;
      }
      setSelectedFile(file);
      setNotification(null);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!selectedFile || uploading) return;

    setUploading(true);
    setNotification(null);
    try {
      const uploaded = await api.uploadResume(studentId, selectedFile);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      setCurrentResume(uploaded);
      setPreviewResumeId(uploaded.resumeId);
      await loadResumes({ showSpinner: false, syncPreviewToActive: true });
      window.dispatchEvent(new CustomEvent('portal:database-mutation', {
        detail: { type: 'RESUME_UPLOADED', resumeId: uploaded.resumeId, resume: uploaded }
      }));
      setNotification({
        type: 'success',
        title: 'Storage Acknowledged',
        message: `Successfully uploaded ${uploaded.fileName} (Version ${uploaded.versionNo}) directly into Oracle BLOB storage.`,
      });
    } catch (err) {
      setNotification({
        type: 'error',
        title: 'Upload Error',
        message: err.message || 'Resume upload failed.',
      });
    } finally {
      setUploading(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!resumeToDelete || deleting) return;
    setDeleting(true);
    try {
      const deletedTarget = resumeToDelete;
      await api.deleteResume(deletedTarget.resumeId);
      setResumeToDelete(null);
      await loadResumes({
        showSpinner: false,
        syncPreviewToActive: previewResumeId === deletedTarget.resumeId,
      });
      window.dispatchEvent(new CustomEvent('portal:database-mutation', {
        detail: { type: 'RESUME_DELETED', resumeId: deletedTarget.resumeId }
      }));
      setNotification({
        type: 'success',
        title: 'Record Purged',
        message: `Successfully deleted ${deletedTarget.fileName} (Version ${deletedTarget.versionNo}). Record and Oracle BLOB purged.`,
      });
    } catch (err) {
      setNotification({
        type: 'error',
        title: 'Deletion Error',
        message: err.message || 'Failed to delete resume from Oracle database.',
      });
    } finally {
      setDeleting(false);
    }
  };

  const handleActivate = async (resume) => {
    if (!resume?.resumeId || activatingId) return;

    setActivatingId(resume.resumeId);
    setNotification(null);
    try {
      const activated = await api.activateResume(resume.resumeId);
      if (!activated || !activated.resumeId) {
        throw new Error('Backend did not return valid activated resume metadata.');
      }

      // Apply authoritative backend response and refresh full state from Oracle
      setCurrentResume(activated);
      setPreviewResumeId(activated.resumeId);
      setPreviewNonce(Date.now());
      await loadResumes({ showSpinner: false, syncPreviewToActive: true });

      window.dispatchEvent(new CustomEvent('portal:database-mutation', {
        detail: { type: 'RESUME_ACTIVATED', resumeId: activated.resumeId, resume: activated }
      }));

      setNotification({
        type: 'success',
        title: 'Active Resume Updated',
        message: `Resume ${activated.fileName} (Version ${activated.versionNo}) is now your active resume in Oracle.`,
      });
    } catch (err) {
      // Preserve previously committed active resume state and re-verify from backend
      await loadResumes({ showSpinner: false, syncPreviewToActive: false });
      setNotification({
        type: 'error',
        title: 'Activation Failed',
        message: err.message || 'Failed to activate selected resume version.',
      });
    } finally {
      setActivatingId(null);
    }
  };

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const formatDate = (isoString) => {
    if (!isoString) return 'N/A';
    try {
      return new Date(isoString).toLocaleString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  const previewedResume = resumes.find((r) => r.resumeId === previewResumeId) || (
    currentResume?.resumeId === previewResumeId ? currentResume : null
  );

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Resumes from Oracle BLOB Storage...</span>
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
          <span className="text-slate-800 font-semibold">Resume Management (Oracle BLOB)</span>
        </div>

        <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
          Student ID: {studentId}
        </span>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          role="status"
          aria-live="polite"
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
              {notification.title || (notification.type === 'success' ? 'Storage Acknowledged' : 'Operation Error')}
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

      {/* Page Header */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">Institutional Resume Vault</h1>
            <p className="text-xs text-slate-500 mt-1">
              Binary documents are persisted directly in Oracle Database column <code className="text-slate-800 font-mono font-semibold">STUDENT_RESUME.Resume_Data (BLOB)</code>.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              disabled={refreshing || Boolean(activatingId)}
              onClick={() => loadResumes({ showSpinner: false, syncPreviewToActive: false })}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100 rounded-md border border-slate-300 transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>
      </div>

      {/* Upload Box & Active Resume Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Upload Card */}
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-2 flex items-center gap-2">
              <Upload className="w-4 h-4 text-slate-600" />
              Upload New Resume Version
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              Uploading automatically increments the version number. This version becomes active for upcoming applications.
            </p>

            <form onSubmit={handleUpload} className="space-y-4">
              <div className="border-2 border-dashed border-slate-300 rounded-lg p-6 text-center hover:border-slate-400 transition-colors bg-slate-50">
                <FileText className="w-10 h-10 text-slate-400 mx-auto mb-2" />
                <label className="cursor-pointer">
                  <span className="text-xs font-bold text-slate-800 hover:underline">Choose PDF or DOCX file</span>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,.docx,.doc,application/pdf"
                    onChange={handleFileChange}
                    className="hidden"
                  />
                </label>
                <p className="text-[11px] text-slate-400 mt-1">Maximum size: 10 MB</p>

                {selectedFile && (
                  <div className="mt-3 p-2 bg-white rounded border border-slate-200 text-xs font-medium text-slate-800 flex items-center justify-between">
                    <span className="truncate max-w-[200px]">{selectedFile.name}</span>
                    <span className="text-[11px] text-slate-500 font-mono">{formatBytes(selectedFile.size)}</span>
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={!selectedFile || uploading || Boolean(activatingId)}
                className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-md transition-colors shadow-sm disabled:opacity-50"
              >
                <Upload className="w-4 h-4" />
                {uploading ? 'Streaming to Oracle BLOB...' : 'Upload & Commit to Database'}
              </button>
            </form>
          </div>
        </div>

        {/* Current Active Resume Card */}
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm flex flex-col justify-between" data-testid="active-resume-card">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
                <FileCheck className="w-4 h-4 text-emerald-600" />
                Current Active Resume
              </h2>
              {currentResume && (
                <span
                  data-testid="active-version-badge"
                  className="px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200"
                >
                  Version {currentResume.versionNo} Active
                </span>
              )}
            </div>

            {currentResume ? (
              <div className="space-y-4">
                <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 space-y-2 text-xs">
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">File Name:</span>
                    <span data-testid="active-resume-filename" className="font-semibold text-slate-800 truncate max-w-[240px]">
                      {currentResume.fileName}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">File Size:</span>
                    <span className="font-mono text-slate-800">{formatBytes(currentResume.fileSizeBytes || currentResume.fileSize)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">Content Type:</span>
                    <span className="font-mono text-slate-600 text-[11px]">{currentResume.contentType}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">Uploaded On:</span>
                    <span className="text-slate-700">{formatDate(currentResume.uploadedAt)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">Oracle Record ID:</span>
                    <span data-testid="active-resume-id" className="font-mono text-slate-600 text-[11px]">
                      {currentResume.resumeId}
                    </span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={Boolean(activatingId)}
                    onClick={async () => {
                      try {
                        await api.downloadResumeBlob(currentResume.resumeId, currentResume.fileName);
                      } catch (err) {
                        setNotification({
                          type: 'error',
                          title: 'Download Failed',
                          message: err.message || 'Failed to download active resume.',
                        });
                      }
                    }}
                    className="flex-1 inline-flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-md text-xs font-semibold transition-colors disabled:opacity-50"
                  >
                    <Download className="w-3.5 h-3.5" /> Download Active Resume
                  </button>
                  <button
                    type="button"
                    disabled={Boolean(activatingId)}
                    onClick={() => {
                      setPreviewResumeId(currentResume.resumeId);
                      setPreviewNonce(Date.now());
                    }}
                    className="px-3 py-2 border border-slate-300 hover:bg-slate-100 text-slate-700 rounded-md text-xs font-semibold inline-flex items-center gap-1.5 transition-colors disabled:opacity-50"
                  >
                    <Eye className="w-3.5 h-3.5" /> Preview
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-8 text-center bg-slate-50 rounded-lg border border-slate-200">
                <FileText className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                <p className="text-xs text-slate-500">No active resume uploaded yet. Upload your first version to apply to drives.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Version History Table */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 flex items-center gap-2">
            <Clock className="w-4 h-4 text-slate-600" />
            Resume Version History ({resumes.length} Versions)
          </h2>
          <span className="text-xs text-slate-500">Table: STUDENT_RESUME</span>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-xs" data-testid="resume-history-table">
            <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="px-4 py-3 text-left">Version</th>
                <th className="px-4 py-3 text-left">File Name</th>
                <th className="px-4 py-3 text-left">Size</th>
                <th className="px-4 py-3 text-left">Upload Date</th>
                <th className="px-4 py-3 text-left">Status</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 bg-white">
              {resumes.map((r) => {
                const rowIsActive = isResumeActive(r);
                const isActivatingThisRow = activatingId === r.resumeId;
                return (
                  <tr
                    key={r.resumeId}
                    data-testid={`resume-row-${r.resumeId}`}
                    className={`transition-colors ${rowIsActive ? 'bg-emerald-50/20 hover:bg-emerald-50/40' : 'hover:bg-slate-50'}`}
                  >
                    <td className="px-4 py-3 font-mono font-bold text-slate-900">v{r.versionNo}</td>
                    <td className="px-4 py-3 font-medium text-slate-800 max-w-[200px] truncate">{r.fileName}</td>
                    <td className="px-4 py-3 font-mono text-slate-600">{formatBytes(r.fileSizeBytes || r.fileSize)}</td>
                    <td className="px-4 py-3 text-slate-600">{formatDate(r.uploadedAt)}</td>
                    <td className="px-4 py-3">
                      {rowIsActive ? (
                        <span
                          data-testid={`status-active-${r.resumeId}`}
                          className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200"
                        >
                          Active
                        </span>
                      ) : (
                        <span
                          data-testid={`status-archived-${r.resumeId}`}
                          className="px-2 py-0.5 rounded text-[10px] font-semibold text-slate-500 bg-slate-100 border border-slate-200"
                        >
                          Archived
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right space-x-1.5 whitespace-nowrap">
                      <button
                        type="button"
                        onClick={() => {
                          setPreviewResumeId(r.resumeId);
                          setPreviewNonce(Date.now());
                        }}
                        className="px-2 py-1 border border-slate-300 rounded text-slate-700 hover:bg-slate-100 font-medium inline-flex items-center gap-1 text-xs"
                        title="Preview this resume version in viewer"
                      >
                        <Eye className="w-3 h-3" /> View
                      </button>
                      <button
                        type="button"
                        onClick={async () => {
                          try {
                            await api.downloadResumeBlob(r.resumeId, r.fileName);
                          } catch (err) {
                            setNotification({
                              type: 'error',
                              title: 'Download Failed',
                              message: err.message || 'Failed to download selected resume version.',
                            });
                          }
                        }}
                        className="px-2 py-1 bg-slate-900 text-white rounded hover:bg-slate-800 font-medium inline-flex items-center gap-1 text-xs"
                        title="Download this specific resume version"
                      >
                        <Download className="w-3 h-3" /> Download
                      </button>

                      {/* Activation action: disabled on already-active version; active for archived versions */}
                      {rowIsActive ? (
                        <button
                          type="button"
                          disabled
                          data-testid={`active-indicator-btn-${r.resumeId}`}
                          className="px-2 py-1 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded font-semibold inline-flex items-center gap-1 text-xs cursor-not-allowed opacity-75"
                          title="This resume version is currently active"
                        >
                          <Check className="w-3 h-3 text-emerald-600" />
                          Active
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleActivate(r)}
                          disabled={Boolean(activatingId)}
                          data-testid={`set-active-btn-${r.resumeId}`}
                          className="px-2 py-1 bg-emerald-50 text-emerald-800 border border-emerald-300 hover:bg-emerald-100 rounded font-medium inline-flex items-center gap-1 text-xs transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                          title="Set this version as your active resume"
                        >
                          {isActivatingThisRow ? (
                            <>
                              <RefreshCw className="w-3 h-3 text-emerald-600 animate-spin" />
                              Activating...
                            </>
                          ) : (
                            <>
                              <Check className="w-3 h-3 text-emerald-600" />
                              Set Active
                            </>
                          )}
                        </button>
                      )}

                      {/* Delete action */}
                      {rowIsActive ? (
                        resumes.length <= 1 ? (
                          <button
                            type="button"
                            disabled
                            className="px-2 py-1 bg-slate-100 text-slate-400 border border-slate-200 rounded font-medium inline-flex items-center gap-1 text-xs cursor-not-allowed"
                            title="This is your active resume. Upload another resume and activate it before deleting this version."
                          >
                            <Trash2 className="w-3 h-3" /> Delete
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setNotification({
                                type: 'error',
                                title: 'Active Resume Protected',
                                message: 'This is your currently active resume. Please activate another resume version first before deleting this version.',
                              });
                            }}
                            className="px-2 py-1 bg-slate-100 text-slate-500 hover:text-slate-700 border border-slate-200 rounded font-medium inline-flex items-center gap-1 text-xs"
                            title="This is your active resume. Activate another version first to delete this."
                          >
                            <Lock className="w-3 h-3 text-slate-400" /> Delete
                          </button>
                        )
                      ) : (
                        <button
                          type="button"
                          disabled={Boolean(activatingId)}
                          onClick={() => setResumeToDelete(r)}
                          className="px-2 py-1 bg-white text-rose-600 border border-rose-200 hover:bg-rose-50 hover:border-rose-300 rounded font-medium inline-flex items-center gap-1 text-xs transition-colors disabled:opacity-50"
                          title="Permanently delete this resume version"
                        >
                          <Trash2 className="w-3 h-3" /> Delete
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {resumes.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-slate-400">
                    No resume files uploaded yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Live PDF/Document Preview Frame */}
      {previewResumeId && (
        <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-6 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-slate-200">
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4 text-slate-600" />
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800">
                Live Document Stream Preview (Resume ID: {previewResumeId})
              </h2>
              {previewedResume && (
                <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
                  v{previewedResume.versionNo} • {previewedResume.fileName}
                </span>
              )}
              {previewedResume && isResumeActive(previewedResume) && (
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200">
                  Active Document
                </span>
              )}
            </div>
            <a
              href={api.getResumeDownloadUrl(previewResumeId, previewNonce)}
              target="_blank"
              rel="noreferrer"
              className="text-xs font-semibold text-blue-700 hover:underline inline-flex items-center gap-1"
            >
              Open in new tab <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>

          <div className="w-full h-[500px] border border-slate-200 rounded-lg overflow-hidden bg-slate-100 flex items-center justify-center">
            <iframe
              key={`${previewResumeId}-${previewNonce}`}
              src={api.getResumeDownloadUrl(previewResumeId, previewNonce)}
              title="Resume Preview"
              className="w-full h-full"
            />
          </div>
        </div>
      )}

      {/* Confirmation Modal for Permanent Deletion */}
      <Modal
        isOpen={!!resumeToDelete}
        onClose={() => !deleting && setResumeToDelete(null)}
        title="Delete Resume Version Permanently?"
      >
        {resumeToDelete && (
          <div className="space-y-4 text-xs">
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-lg text-rose-950 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold text-sm text-rose-900">Permanent Database Purge</span>
                <p className="mt-1 text-xs text-rose-800 leading-relaxed">
                  Warning: The stored Oracle BLOB binary data for this resume will be <strong>permanently removed</strong> from the Oracle Database. This action cannot be undone.
                </p>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg space-y-2.5 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">File Name:</span>
                <span className="font-semibold text-slate-900 truncate max-w-[260px]">{resumeToDelete.fileName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Version Number:</span>
                <span className="font-mono font-bold text-slate-900 bg-slate-200/70 px-2 py-0.5 rounded">
                  Version {resumeToDelete.versionNo}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">File Size:</span>
                <span className="font-mono text-slate-700">{formatBytes(resumeToDelete.fileSize || resumeToDelete.fileSizeBytes)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Upload Date:</span>
                <span className="text-slate-700">{formatDate(resumeToDelete.uploadedAt)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">Oracle Record ID:</span>
                <span className="font-mono text-slate-600 text-[11px]">{resumeToDelete.resumeId}</span>
              </div>
            </div>

            <p className="text-slate-500 text-[11px] leading-relaxed">
              Historical version numbers are immutable and will not be renumbered. All other resume versions remain safe.
            </p>

            <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
              <button
                type="button"
                disabled={deleting}
                onClick={() => setResumeToDelete(null)}
                className="px-4 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-md font-semibold transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={handleDeleteConfirm}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-md font-bold transition-colors shadow-sm inline-flex items-center gap-1.5 disabled:opacity-50"
              >
                <Trash2 className="w-3.5 h-3.5" />
                {deleting ? 'Purging from Oracle BLOB...' : 'Delete Permanently'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
