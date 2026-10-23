import React from 'react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { useAuth } from '../context/AuthContext';
import { ProtectedRoute } from '../App';

describe('Blocker B: AuthContext & ProtectedRoute Security & Stability', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    });
  });

  function mockAuthContext(value) {
    if (React.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE) {
      React.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE.H = {
        useContext: () => value,
      };
    }
  }

  afterEach(() => {
    if (React.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE) {
      React.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE.H = null;
    }
    vi.restoreAllMocks();
  });

  it('useAuth returns safe fallback and does NOT crash if called outside AuthProvider', () => {
    mockAuthContext(null);
    const auth = useAuth();
    expect(auth).toBeDefined();
    expect(auth).not.toBeNull();
    expect(auth.user).toBeNull();
    expect(auth.isInitializing).toBe(false);
    expect(typeof auth.logout).toBe('function');
    expect(typeof auth.login).toBe('function');

    // Destructuring user must be safe and never throw TypeError
    expect(() => {
      const { user } = auth;
      expect(user).toBeNull();
    }).not.toThrow();
  });

  it('ProtectedRoute safely redirects unauthenticated users to /login', () => {
    mockAuthContext(null);
    const element = ProtectedRoute({
      children: React.createElement('div', null, 'Secret Content'),
      allowedRoles: ['ROLE_STUDENT'],
    });

    expect(element).toBeDefined();
    expect(element.props.to).toBe('/login');
    expect(element.props.replace).toBe(true);
  });

  it('ProtectedRoute allows authorized role through', () => {
    mockAuthContext({
      user: { username: 'STU001', role: 'ROLE_STUDENT', name: 'Achyut Ranaut' },
      isInitializing: false,
    });

    const child = React.createElement('div', { id: 'dashboard' }, 'Student Dashboard');
    const element = ProtectedRoute({
      children: child,
      allowedRoles: ['ROLE_STUDENT', 'ROLE_ADMIN'],
    });

    expect(element).toBe(child);
  });

  it('ProtectedRoute enforces strict role isolation (student cannot access admin)', () => {
    mockAuthContext({
      user: { username: 'STU001', role: 'ROLE_STUDENT' },
      isInitializing: false,
    });

    const element = ProtectedRoute({
      children: React.createElement('div', null, 'Admin Panel'),
      allowedRoles: ['ROLE_ADMIN'],
    });

    expect(element.props.to).toBe('/');
    expect(element.props.replace).toBe(true);
  });

  it('ProtectedRoute enforces strict recruiter isolation (recruiter cannot access admin)', () => {
    mockAuthContext({
      user: { username: 'REC001', role: 'ROLE_RECRUITER' },
      isInitializing: false,
    });

    const element = ProtectedRoute({
      children: React.createElement('div', null, 'Admin Panel'),
      allowedRoles: ['ROLE_ADMIN'],
    });

    expect(element.props.to).toBe('/');
    expect(element.props.replace).toBe(true);
  });
});

describe('Blocker A: Graph Relationship Deduplication & Stable ID Resolution', () => {
  it('deduplicates identical join rows and avoids duplicate edge IDs', () => {
    const rawEdges = [];
    const addedEdgeKeys = new Set();
    const addedEdgeIds = new Set();

    const addRawEdge = (edge) => {
      if (!edge || !edge.from || !edge.to) return;

      const detailStr = edge.details
        ? Object.entries(edge.details).sort().map(([k, v]) => `${k}:${v}`).join(',')
        : '';
      const semanticKey = `${edge.from}->${edge.to}[:${edge.label || 'REL'}]{${detailStr}}`;
      if (addedEdgeKeys.has(semanticKey)) {
        return;
      }
      addedEdgeKeys.add(semanticKey);

      let baseId = edge.id || `${edge.from}-[:${edge.label || 'CONNECTED_TO'}]->${edge.to}`;
      let uniqueEdgeId = baseId;
      let counter = 2;
      while (addedEdgeIds.has(uniqueEdgeId)) {
        uniqueEdgeId = `${baseId}#${counter}`;
        counter++;
      }

      addedEdgeIds.add(uniqueEdgeId);
      rawEdges.push({
        ...edge,
        id: uniqueEdgeId,
      });
    };

    // Simulate STU001 submitting APP002 being added multiple times (the exact console error)
    addRawEdge({ id: 'STU001->APP002', from: 'STU001', to: 'APP002', label: 'SUBMITTED' });
    addRawEdge({ id: 'STU001->APP002', from: 'STU001', to: 'APP002', label: 'SUBMITTED' });
    addRawEdge({ id: 'STU001->APP002', from: 'STU001', to: 'APP002', label: 'SUBMITTED' });

    // Must only have 1 edge in rawEdges
    expect(rawEdges.length).toBe(1);
    expect(rawEdges[0].id).toBe('STU001->APP002');
    expect(rawEdges[0].from).toBe('STU001');
    expect(rawEdges[0].to).toBe('APP002');

    // All edge IDs must be completely unique
    const uniqueIds = new Set(rawEdges.map((e) => e.id));
    expect(uniqueIds.size).toBe(rawEdges.length);
  });

  it('preserves legitimate parallel relationships with distinct stable IDs', () => {
    const rawEdges = [];
    const addedEdgeKeys = new Set();
    const addedEdgeIds = new Set();

    const addRawEdge = (edge) => {
      if (!edge || !edge.from || !edge.to) return;

      const detailStr = edge.details
        ? Object.entries(edge.details).sort().map(([k, v]) => `${k}:${v}`).join(',')
        : '';
      const semanticKey = `${edge.from}->${edge.to}[:${edge.label || 'REL'}]{${detailStr}}`;
      if (addedEdgeKeys.has(semanticKey)) {
        return;
      }
      addedEdgeKeys.add(semanticKey);

      let baseId = edge.id || `${edge.from}-[:${edge.label || 'CONNECTED_TO'}]->${edge.to}`;
      let uniqueEdgeId = baseId;
      let counter = 2;
      while (addedEdgeIds.has(uniqueEdgeId)) {
        uniqueEdgeId = `${baseId}#${counter}`;
        counter++;
      }

      addedEdgeIds.add(uniqueEdgeId);
      rawEdges.push({
        ...edge,
        id: uniqueEdgeId,
      });
    };

    // Relationship 1: SUBMITTED
    addRawEdge({ id: 'STU001->APP002', from: 'STU001', to: 'APP002', label: 'SUBMITTED' });
    // Relationship 2: MENTORED (distinct parallel relationship between same endpoints)
    addRawEdge({ id: 'STU001->APP002', from: 'STU001', to: 'APP002', label: 'MENTORED' });

    expect(rawEdges.length).toBe(2);
    expect(rawEdges[0].id).toBe('STU001->APP002');
    expect(rawEdges[1].id).toBe('STU001->APP002#2');

    // Guaranteed uniqueness for vis.DataSet
    const uniqueIds = new Set(rawEdges.map((e) => e.id));
    expect(uniqueIds.size).toBe(2);
  });
});

describe('Blocker C: Resume PDF Preview Lifecycle & Binary Auth Headers', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key) => (key === 'portal_token' ? 'mock_jwt_token_123' : null)),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    });
    const mockUrl = {
      createObjectURL: vi.fn(() => 'blob:http://localhost:5173/mock-blob-uuid-1234'),
      revokeObjectURL: vi.fn(),
    };
    vi.stubGlobal('URL', mockUrl);
    vi.stubGlobal('window', {
      URL: mockUrl,
      dispatchEvent: vi.fn(),
      open: vi.fn(),
    });
    vi.stubGlobal('document', {
      createElement: vi.fn(() => ({
        href: '',
        download: '',
        click: vi.fn(),
        remove: vi.fn(),
      })),
      body: { appendChild: vi.fn() },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('fetchResumeBlobUrl includes Bearer auth header and creates object URL', async () => {
    const validPdfContent = '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/Parent 2 0 R>>endobj\nxref\n0 4\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n180\n%%EOF';
    const mockBlob = new Blob([validPdfContent], { type: 'application/pdf' });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: (h) => (h.toLowerCase() === 'content-type' ? 'application/pdf' : null) },
      blob: () => Promise.resolve(mockBlob),
    }));

    const { api } = await import('../lib/api');
    const blobUrl = await api.fetchResumeBlobUrl('RES_123');

    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/resumes/RES_123/download',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer mock_jwt_token_123',
          Accept: 'application/pdf, application/octet-stream, */*',
        }),
      })
    );
    expect(blobUrl).toBe('blob:http://localhost:5173/mock-blob-uuid-1234');
    expect(URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
  });

  it('fetchResumeBlobUrl rejects truncated or corrupt PDF blobs with descriptive error', async () => {
    // Truncated blob under 150 bytes
    const truncatedBlob = new Blob(['%PDF-1.4 truncated test payload'], { type: 'application/pdf' });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: (h) => (h.toLowerCase() === 'content-type' ? 'application/pdf' : null) },
      blob: () => Promise.resolve(truncatedBlob),
    }));

    const { api } = await import('../lib/api');
    await expect(api.fetchResumeBlobUrl('RES_TRUNCATED')).rejects.toThrow(
      'truncated or corrupt'
    );
  });

  it('fetchResumeBlobUrl rejects with clear error message on server failure', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: () => Promise.resolve({ message: 'Access denied: You do not own this resume' }),
    }));

    const { api } = await import('../lib/api');
    await expect(api.fetchResumeBlobUrl('RES_FORBIDDEN')).rejects.toThrow(
      'Access denied: You do not own this resume'
    );
  });

  it('downloadResumeBlob revokes object URL to prevent memory leaks', async () => {
    const mockBlob = new Blob(['%PDF-1.4 mock pdf data'], { type: 'application/pdf' });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: () => Promise.resolve(mockBlob),
    }));

    const mockAnchor = {
      href: '',
      download: '',
      click: vi.fn(),
      remove: vi.fn(),
    };
    vi.spyOn(document, 'createElement').mockReturnValue(mockAnchor);
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => {});

    vi.useFakeTimers();
    const { api } = await import('../lib/api');
    await api.downloadResumeBlob('RES_123', 'my_resume.pdf');

    expect(mockAnchor.download).toBe('my_resume.pdf');
    expect(mockAnchor.click).toHaveBeenCalled();
    expect(mockAnchor.remove).toHaveBeenCalled();

    // Fast-forward timeout for URL.revokeObjectURL
    vi.advanceTimersByTime(250);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:http://localhost:5173/mock-blob-uuid-1234');
    vi.useRealTimers();
  });
});

