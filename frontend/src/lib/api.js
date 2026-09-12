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
  } catch (err) {
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

async function handleResponse(res) {
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const errorMsg = data.message || `Request failed with status ${res.status}`;
    throw new Error(errorMsg);
  }
  return data.data !== undefined ? data.data : data;
}

export const api = {
  // Authentication
  async login(username, password) {
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
    const token = localStorage.getItem('portal_token');
    return token
      ? `${BASE_URL}/resumes/${resumeId}/download?token=${encodeURIComponent(token)}`
      : `${BASE_URL}/resumes/${resumeId}/download`;
  },

  getStudentCurrentResumeDownloadUrl(studentId) {
    const token = localStorage.getItem('portal_token');
    return token
      ? `${BASE_URL}/resumes/student/${studentId}/current/download?token=${encodeURIComponent(token)}`
      : `${BASE_URL}/resumes/student/${studentId}/current/download`;
  },

  async downloadResumeBlob(resumeId, filename = 'student_resume.pdf') {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/download`, { headers: getAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Resume download failed' }));
      throw new Error(err.message || 'Resume download failed');
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  },

  async viewResumeBlob(resumeId) {
    const res = await fetch(`${BASE_URL}/resumes/${resumeId}/download`, { headers: getAuthHeaders() });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Unable to open resume' }));
      throw new Error(err.message || 'Unable to open resume');
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    window.open(url, '_blank');
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
  async getDrives() {
    const res = await fetch(`${BASE_URL}/drives`, { headers: getAuthHeaders() });
    return handleResponse(res);
  },

  async getDriveById(driveId) {
    const res = await fetch(`${BASE_URL}/drives/${driveId}`, { headers: getAuthHeaders() });
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
        online: online ? 'Y' : 'N',
        offline: offline ? 'Y' : 'N',
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

  getOfferDownloadUrl(offerId) {
    const token = localStorage.getItem('portal_token');
    return token
      ? `${BASE_URL}/offers/${offerId}/download?token=${encodeURIComponent(token)}`
      : `${BASE_URL}/offers/${offerId}/download`;
  },

  async downloadOfferLetterPdf(offerId, filename = 'Offer_Letter.pdf') {
    const res = await fetch(`${BASE_URL}/offers/${offerId}/download`, { headers: getAuthHeaders() });
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
    window.URL.revokeObjectURL(url);
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
  async executeSql({ sql, mode = 'QUERY' }) {
    const res = await fetch(`${BASE_URL}/admin/sql/execute`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ sql, mode }),
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
};
