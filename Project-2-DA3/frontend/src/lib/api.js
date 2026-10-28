const BASE_URL = '/api/v1';

let backendAvailable = false;

export async function checkBackendHealth() {
  try {
    const res = await fetch('/actuator/health', {
      method: 'GET',
      signal: AbortSignal.timeout(2000),
    });
    backendAvailable = res.ok;
    return backendAvailable;
  } catch {
    try {
      const fallbackRes = await fetch(`${BASE_URL}/drives`, {
        method: 'GET',
        signal: AbortSignal.timeout(1500),
      });
      backendAvailable = fallbackRes.ok || fallbackRes.status === 401 || fallbackRes.status === 403;
      return backendAvailable;
    } catch {
      backendAvailable = false;
      return false;
    }
  }
}

export function isBackendLive() {
  return backendAvailable;
}

function getAuthHeaders() {
  const token = localStorage.getItem('portal_token');
  return {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

function getBinaryAuthHeaders() {
  const token = localStorage.getItem('portal_token');
  return {
    Accept: 'application/pdf, application/octet-stream, */*',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function handleResponse(res) {
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    let errorMsg = data.message;
    if (res.status === 401) {
      localStorage.removeItem('portal_token');
      localStorage.removeItem('portal_user');
      window.dispatchEvent(new CustomEvent('portal:auth-expired'));
      errorMsg = errorMsg || 'Session Expired (401): Please log in again to continue.';
    } else if (res.status === 403) {
      errorMsg = errorMsg || 'Access Denied (403): You do not have permission to perform this action or your session has expired. Please sign in with appropriate credentials.';
    } else if (!errorMsg) {
      errorMsg = `Request failed with status ${res.status}`;
    }
    throw new Error(errorMsg);
  }
  return data.data !== undefined ? data.data : data;
}

export const api = {
  // Authentication
  async login(username, password) {
    localStorage.removeItem('portal_token');
    localStorage.removeItem('portal_user');
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const data = await handleResponse(res);
    if (data.token) {
      localStorage.setItem('portal_token', data.token);
    }
    return data;
  },

  async registerStudent(registerData) {
    const res = await fetch(`${BASE_URL}/auth/student/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(registerData),
    });
    const data = await handleResponse(res);
    if (data && data.token) {
      localStorage.setItem('portal_token', data.token);
    }
    return data;
  },

  async registerRecruiter(recruiterData) {
    const res = await fetch(`${BASE_URL}/auth/recruiter/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(recruiterData),
    });
    const data = await handleResponse(res);
    if (data && data.token) {
      localStorage.setItem('portal_token', data.token);
    }
    return data;
  },

  async getMyCompanies() {
    const res = await fetch(`${BASE_URL}/recruiter/companies`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async selectActiveCompany(companyId) {
    const res = await fetch(`${BASE_URL}/recruiter/company/select`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ companyId }),
    });
    const data = await handleResponse(res);
    if (data && data.token) {
      localStorage.setItem('portal_token', data.token);
    }
    return data;
  },

  async getAdminRecruiters() {
    const res = await fetch(`${BASE_URL}/admin/recruiters`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async assignRecruiterCompany(userId, companyId) {
    const res = await fetch(`${BASE_URL}/admin/recruiters/${encodeURIComponent(userId)}/companies/${encodeURIComponent(companyId)}`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async revokeRecruiterCompany(userId, companyId) {
    const res = await fetch(`${BASE_URL}/admin/recruiters/${encodeURIComponent(userId)}/companies/${encodeURIComponent(companyId)}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Students
  async getMyProfile() {
    const res = await fetch(`${BASE_URL}/students/me`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async updateMyProfile(profileData) {
    const res = await fetch(`${BASE_URL}/students/me`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(profileData),
    });
    return handleResponse(res);
  },

  async getStudent(studentId) {
    const res = await fetch(`${BASE_URL}/students/${studentId}`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getAllStudents() {
    const res = await fetch(`${BASE_URL}/students`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async updateStudentProfile(studentId, profileData) {
    const res = await fetch(`${BASE_URL}/students/${studentId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(profileData),
    });
    return handleResponse(res);
  },

  // Student Resumes (Oracle BLOB storage)
  async uploadResume(studentId, file) {
    const token = localStorage.getItem('portal_token');
    const formData = new FormData();
    if (studentId) {
      formData.append('studentId', studentId);
    }
    formData.append('file', file);
    const res = await fetch(`${BASE_URL}/resumes/upload`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    });
    return handleResponse(res);
  },

  async getStudentResumes(studentId) {
    const res = await fetch(`${BASE_URL}/resumes/student/${studentId}`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getCurrentResume(studentId) {
    const res = await fetch(`${BASE_URL}/resumes/student/${studentId}/current`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  getResumeDownloadUrl(resumeId) {
    return `${BASE_URL}/resumes/${resumeId}/download`;
  },

  getStudentCurrentResumeDownloadUrl(studentId) {
    return `${BASE_URL}/resumes/student/${studentId}/current/download`;
  },

  async fetchResumeBlobUrl(resumeId) {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/download`, { headers: getBinaryAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Resume download failed' }));
      throw new Error(err.message || `Resume download failed with status ${res.status}`);
    }
    const blob = await res.blob();
    const contentType = (res.headers?.get ? res.headers.get('content-type') : (res.headers?.['content-type'] || res.headers?.['Content-Type'] || '')) || blob.type || '';
    if (contentType.toLowerCase().includes('pdf') || blob.size < 200) {
      if (blob.size < 150) {
        throw new Error(`The stored PDF resume in Oracle BLOB storage is truncated or corrupt (${blob.size} bytes). Please upload a complete PDF.`);
      }
      try {
        const headerBuffer = await blob.slice(0, 5).arrayBuffer();
        const headerStr = String.fromCharCode(...new Uint8Array(headerBuffer));
        if (headerStr !== '%PDF-') {
          throw new Error('The stored resume binary in Oracle BLOB storage is missing a valid PDF header (%PDF-).');
        }
      } catch (validationErr) {
        if (validationErr.message && validationErr.message.includes('PDF')) {
          throw validationErr;
        }
      }
    }
    const urlHelper = typeof window !== 'undefined' && window.URL ? window.URL : globalThis.URL;
    return urlHelper.createObjectURL(blob);
  },

  async downloadResumeBlob(resumeId, filename = 'student_resume.pdf') {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/download`, { headers: getBinaryAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Resume download failed' }));
      throw new Error(err.message || `Resume download failed with status ${res.status}`);
    }
    const blob = await res.blob();
    const urlHelper = typeof window !== 'undefined' && window.URL ? window.URL : globalThis.URL;
    const url = urlHelper.createObjectURL(blob);
    if (typeof document !== 'undefined') {
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
    }
    setTimeout(() => urlHelper.revokeObjectURL(url), 200);
  },

  async viewResumeBlob(resumeId) {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/download`, { headers: getBinaryAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Unable to open resume' }));
      throw new Error(err.message || `Unable to open resume: status ${res.status}`);
    }
    const blob = await res.blob();
    const urlHelper = typeof window !== 'undefined' && window.URL ? window.URL : globalThis.URL;
    const url = urlHelper.createObjectURL(blob);
    if (typeof window !== 'undefined' && window.open) {
      window.open(url, '_blank');
    }
  },

  async deleteResume(resumeId) {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async activateResume(resumeId) {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/activate`, {
      method: 'PUT',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Academic Programs & Batches
  async getPrograms() {
    const res = await fetch(`${BASE_URL}/programs`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Companies
  async getCompanies() {
    const res = await fetch(`${BASE_URL}/companies`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Placement Drives
  async getDrives(includeExpired = false) {
    const url = includeExpired ? `${BASE_URL}/drives?includeExpired=true` : `${BASE_URL}/drives`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getDriveById(driveId) {
    const res = await fetch(`${BASE_URL}/drives/${driveId}`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async createDrive(driveData) {
    const res = await fetch(`${BASE_URL}/drives`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(driveData),
    });
    return handleResponse(res);
  },

  async updateDrive(driveId, driveData) {
    const res = await fetch(`${BASE_URL}/drives/${driveId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(driveData),
    });
    return handleResponse(res);
  },

  async deleteDrive(driveId) {
    const res = await fetch(`${BASE_URL}/drives/${driveId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Applications
  async getApplications(studentId = null) {
    const url = studentId
      ? `${BASE_URL}/applications/student/${studentId}`
      : `${BASE_URL}/applications`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Authoritative: derives student from Spring Security Principal (no URL param)
  async getMyApplications() {
    const res = await fetch(`${BASE_URL}/applications/my`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Drive Eligibility (authoritative — calls backend, no client-side string matching)
  async getDriveEligibility(driveId, studentId = null) {
    const url = studentId
      ? `${BASE_URL}/drives/${driveId}/eligibility/${studentId}`
      : `${BASE_URL}/drives/${driveId}/eligibility`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getAllDrivesEligibility() {
    const res = await fetch(`${BASE_URL}/drives/eligibility`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Apply for Drive (triggers Oracle PL/SQL APPLY_FOR_DRIVE)
  async applyForDrive(studentId, driveId, applyDate = null) {
    const res = await fetch(`${BASE_URL}/applications`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        studentId,
        driveId,
        applyDate: applyDate || new Date().toISOString().split('T')[0],
      }),
    });
    return handleResponse(res);
  },

  // Update Application Status (enforces state machine)
  async updateApplicationStatus(applicationId, status) {
    const res = await fetch(`${BASE_URL}/applications/${applicationId}/status`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
      body: JSON.stringify({ status }),
    });
    return handleResponse(res);
  },

  // Interviews
  async getInterviews(applicationId = null) {
    const url = applicationId
      ? `${BASE_URL}/interviews/application/${applicationId}`
      : `${BASE_URL}/interviews`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getStudentInterviews(studentId) {
    const res = await fetch(`${BASE_URL}/interviews/student/${studentId}`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // List pinned interviewers and designated round numbers
  async getInterviewers() {
    const res = await fetch(`${BASE_URL}/interviews/interviewers`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Schedule Interview (executes Oracle PL/SQL SCHEDULE_INTERVIEW)
  async scheduleInterview({ applicationId, interviewerName, oa, gd, hr, result, online, offline }) {
    const isOnline = online === true || online === 'Y' || online === 'true';
    const isOffline = offline === true || offline === 'Y' || offline === 'true';
    const res = await fetch(`${BASE_URL}/interviews/schedule`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        applicationId,
        interviewerName,
        oa: oa ? 'Y' : 'N',
        gd: gd ? 'Y' : 'N',
        hr: hr ? 'Y' : 'N',
        result: result || 'PENDING',
        online: isOnline ? 'Y' : 'N',
        offline: isOffline ? 'Y' : (isOnline ? 'N' : 'Y'),
      }),
    });
    return handleResponse(res);
  },

  // Record Interview Result & Score
  async recordInterviewResult(applicationId, interviewerName, { oa, gd, hr, result }) {
    const res = await fetch(`${BASE_URL}/interviews/${applicationId}/${encodeURIComponent(interviewerName)}/result`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
      body: JSON.stringify({ oa, gd, hr, result }),
    });
    return handleResponse(res);
  },

  // Offers
  async getOffers(studentId = null) {
    const url = studentId ? `${BASE_URL}/offers/student/${studentId}` : `${BASE_URL}/offers`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getMyOffers() {
    const res = await fetch(`${BASE_URL}/offers/my`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async acceptOffer(offerId) {
    const res = await fetch(`${BASE_URL}/offers/${offerId}/accept`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async declineOffer(offerId) {
    const res = await fetch(`${BASE_URL}/offers/${offerId}/decline`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  getOfferDownloadUrl(offerId) {
    return `${BASE_URL}/offers/${offerId}/download`;
  },

  async downloadOfferLetterPdf(offerId, filename = 'Offer_Letter.pdf') {
    const res = await fetch(`${BASE_URL}/offers/${offerId}/download`, { headers: getBinaryAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Offer download failed' }));
      throw new Error(err.message || 'Offer download failed');
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => window.URL.revokeObjectURL(url), 200);
  },

  // Issue Offer (executes Oracle PL/SQL ISSUE_OFFER, triggers TRG_OFFER_APPLICATION_STATUS)
  async issueOffer({ applicationId, ctcLpa, offerDate }) {
    const res = await fetch(`${BASE_URL}/offers`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        applicationId,
        ctcLpa: parseFloat(ctcLpa),
        offerDate: offerDate || new Date().toISOString().split('T')[0],
      }),
    });
    return handleResponse(res);
  },

  // Recruiter Operations (ATS Candidate Evaluation & Role Stats)
  async recordResumeEvaluation(evaluationData) {
    const res = await fetch(`${BASE_URL}/recruiter/evaluations`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(evaluationData),
    });
    return handleResponse(res);
  },

  async getEvaluationsByApplication(applicationId) {
    const res = await fetch(`${BASE_URL}/recruiter/evaluations/application/${applicationId}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getRecruiterRoleStats(companyId = '') {
    const url = companyId && companyId !== 'ALL'
      ? `${BASE_URL}/recruiter/roles?companyId=${encodeURIComponent(companyId)}`
      : `${BASE_URL}/recruiter/roles`;
    const res = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Single authoritative reports overview endpoint
  async getOverviewReport() {
    const res = await fetch(`${BASE_URL}/reports/overview`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getPlacementSummary() {
    const res = await fetch(`${BASE_URL}/reports/summary`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Audit Logs (populated by database triggers)
  async getAuditLogs() {
    const res = await fetch(`${BASE_URL}/admin/audit`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  // Database Console execution (legacy presets)
  async executeDatabaseScript({ queryKey, customSql, scriptType, sql }) {
    const res = await fetch(`${BASE_URL}/admin/database-console/execute`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        queryKey: queryKey || scriptType,
        customSql: customSql || sql,
      }),
    });
    return handleResponse(res);
  },

  // Dedicated SQL & PL/SQL Compiler APIs
  async executeSql({ sql, mode = 'QUERY', strictMode = true }) {
    const res = await fetch(`${BASE_URL}/admin/sql/execute`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ sql, mode, strictMode }),
    });
    return handleResponse(res);
  },

  async getSqlHistory(limit = 50) {
    const res = await fetch(`${BASE_URL}/admin/sql/history?limit=${limit}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getSqlSchema() {
    const res = await fetch(`${BASE_URL}/admin/sql/schema`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getSqlConnectionInfo() {
    const res = await fetch(`${BASE_URL}/admin/sql/connection-info`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getBackendInfo() {
    const res = await fetch(`${BASE_URL}/system/backend-info`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Neo4j Aura Graph APIs
  async getGraphStatus() {
    const res = await fetch(`${BASE_URL}/graph/status`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async reconnectGraph() {
    const res = await fetch(`${BASE_URL}/graph/reconnect`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async syncGraphFromOracle() {
    const res = await fetch(`${BASE_URL}/graph/sync`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async reconcileGraph() {
    const res = await fetch(`${BASE_URL}/graph/reconcile`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getGraphJourney(studentId) {
    const res = await fetch(`${BASE_URL}/graph/journey/${studentId}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getGraphEligibleDrives(studentId) {
    const res = await fetch(`${BASE_URL}/graph/eligible-drives/${studentId}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getGraphRecommendations(studentId) {
    const res = await fetch(`${BASE_URL}/graph/recommendations/${studentId}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Aliases for GraphViewPage
  async getPlacementPathGraph(studentId) {
    return this.getGraphJourney(studentId);
  },

  async getEligibleDrivesGraph(studentId) {
    return this.getGraphEligibleDrives(studentId);
  },

  async getRecommendationsGraph(studentId) {
    return this.getGraphRecommendations(studentId);
  },

  async getGraphElements(limit = 100) {
    const res = await fetch(`${BASE_URL}/graph/elements?limit=${limit}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Neo4j Graph Mutations (Admin only)
  async createGraphEntity(label, properties) {
    const res = await fetch(`${BASE_URL}/graph/nodes`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ label, properties }),
    });
    return handleResponse(res);
  },

  async createGraphRelationship(fromLabel, fromId, toLabel, toId, relType) {
    const res = await fetch(`${BASE_URL}/graph/relationships`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ fromLabel, fromId, toLabel, toId, relType }),
    });
    return handleResponse(res);
  },

  async deleteGraphEntity(nodeType, id) {
    const res = await fetch(`${BASE_URL}/graph/nodes/${encodeURIComponent(nodeType)}/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async updateGraphEntity(nodeType, id, properties) {
    const res = await fetch(`${BASE_URL}/graph/nodes/${encodeURIComponent(nodeType)}/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ properties }),
    });
    return handleResponse(res);
  },

  // Dedicated Neo4j Cypher Compiler APIs
  async executeCypher({ cypher, mode = 'QUERY' }) {
    const res = await fetch(`${BASE_URL}/admin/cypher/execute`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ sql: cypher, mode }),
    });
    return handleResponse(res);
  },

  async getCypherHistory(limit = 50) {
    const res = await fetch(`${BASE_URL}/admin/cypher/history?limit=${limit}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getCypherSchema() {
    const res = await fetch(`${BASE_URL}/admin/cypher/schema`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async getCypherConnectionInfo() {
    const res = await fetch(`${BASE_URL}/admin/cypher/connection-info`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  // Qdrant Vector Database Semantic Search APIs
  async getSemanticSearchStatus() {
    const res = await fetch(`${BASE_URL}/semantic-search/status`, {
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async executeSemanticSearch({ query, entityType = 'ALL', limit = 10, minScore = 0.2 }) {
    const res = await fetch(`${BASE_URL}/semantic-search`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ query, entityType, limit, minScore }),
    });
    return handleResponse(res);
  },

  async reindexSemanticSearch() {
    const res = await fetch(`${BASE_URL}/semantic-search/index`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },

  async reconnectQdrant() {
    const res = await fetch(`${BASE_URL}/semantic-search/reconnect`, {
      method: 'POST',
      headers: getAuthHeaders(),
    });
    return handleResponse(res);
  },
};


