/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api, checkBackendHealth } from '../lib/api';

export const DEFAULT_AUTH_STATE = Object.freeze({
  user: null,
  activeCompany: null,
  authorizedCompanies: [],
  selectCompany: async () => {},
  login: async () => {},
  register: async () => {},
  registerRecruiter: async () => {},
  fetchRecruiterCompanies: async () => [],
  logout: () => {},
  isBackendLive: false,
  checkingBackend: false,
  isInitializing: false,
});

const AuthContext = createContext(DEFAULT_AUTH_STATE);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('portal_user');
      return saved ? JSON.parse(saved) : null;
    } catch (e) {
      console.warn('Failed to parse portal_user from localStorage:', e);
      return null;
    }
  });

  const [activeCompany, setActiveCompany] = useState(() => {
    try {
      const saved = localStorage.getItem('portal_active_company');
      return saved ? JSON.parse(saved) : null;
    } catch (e) {
      console.warn('Failed to parse portal_active_company from localStorage:', e);
      return null;
    }
  });

  const [authorizedCompanies, setAuthorizedCompanies] = useState([]);
  const [isBackendLive, setIsBackendLive] = useState(false);
  const [checkingBackend, setCheckingBackend] = useState(true);
  const [isInitializing, setIsInitializing] = useState(true);

  const logout = useCallback(() => {
    localStorage.removeItem('portal_user');
    localStorage.removeItem('portal_token');
    localStorage.removeItem('portal_active_company');
    setUser(null);
    setActiveCompany(null);
    setAuthorizedCompanies([]);
  }, []);

  const fetchRecruiterCompanies = async (currentUser) => {
    if (currentUser?.role === 'ROLE_RECRUITER') {
      try {
        const comps = await api.getMyCompanies();
        const list = Array.isArray(comps) ? comps : [];
        setAuthorizedCompanies(list);

        // Auto-select or validate active company
        const savedComp = localStorage.getItem('portal_active_company');
        let selected = savedComp ? JSON.parse(savedComp) : null;

        if (selected && !list.some((c) => c.companyId === selected.companyId)) {
          selected = null;
        }

        if (!selected && list.length === 1) {
          selected = list[0];
        }

        if (selected) {
          setActiveCompany(selected);
          localStorage.setItem('portal_active_company', JSON.stringify(selected));
        } else {
          setActiveCompany(null);
          localStorage.removeItem('portal_active_company');
        }
        return list;
      } catch (err) {
        console.warn('Could not fetch recruiter authorized companies:', err);
        setAuthorizedCompanies([]);
        return [];
      }
    } else {
      setAuthorizedCompanies([]);
      setActiveCompany(null);
      localStorage.removeItem('portal_active_company');
      return [];
    }
  };

  useEffect(() => {
    async function initAuth() {
      try {
        const live = await checkBackendHealth();
        setIsBackendLive(live);
        setCheckingBackend(false);
        if (user?.role === 'ROLE_RECRUITER') {
          await fetchRecruiterCompanies(user);
        }
      } catch (err) {
        console.warn('Backend health check error during initAuth:', err);
      } finally {
        setIsInitializing(false);
      }
    }
    initAuth();

    const handleExpired = () => {
      logout();
    };
    window.addEventListener('portal:auth-expired', handleExpired);

    const interval = setInterval(async () => {
      const live = await checkBackendHealth();
      setIsBackendLive(live);
    }, 15000);
    return () => {
      clearInterval(interval);
      window.removeEventListener('portal:auth-expired', handleExpired);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [logout]);

  const handleAuthSuccess = async (authData) => {
    let displayName = authData.name || authData.username;
    if (authData.role === 'ROLE_STUDENT') {
      try {
        if (authData.referenceId) {
          const student = await api.getStudent(authData.referenceId);
          if (student && student.name) {
            displayName = student.name;
          }
        }
      } catch (e) {
        console.warn('Could not fetch student details for name:', e);
      }
    }

    const updatedUser = {
      username: authData.username,
      role: authData.role,
      referenceId: authData.referenceId,
      name: displayName,
      email: authData.email,
    };
    setUser(updatedUser);
    localStorage.setItem('portal_user', JSON.stringify(updatedUser));

    if (updatedUser.role === 'ROLE_RECRUITER') {
      const comps = await fetchRecruiterCompanies(updatedUser);
      return { user: updatedUser, authorizedCompanies: comps };
    }

    return { user: updatedUser, authorizedCompanies: [] };
  };

  const login = async (username, password) => {
    const authData = await api.login(username, password);
    return await handleAuthSuccess(authData);
  };

  const register = async (registerData) => {
    const authData = await api.registerStudent(registerData);
    return await handleAuthSuccess(authData);
  };

  const registerRecruiter = async (registerData) => {
    const result = await api.registerRecruiter(registerData);
    // Map RecruiterRegistrationResult to the shape handleAuthSuccess expects
    const authData = {
      token: result.token,
      username: result.username,
      role: result.role,
      referenceId: result.companyId,
      name: result.fullName,
      email: result.email,
    };
    return await handleAuthSuccess(authData);
  };

  const selectCompany = async (companyId) => {
    const res = await api.selectActiveCompany(companyId);
    const company = res.company;
    setActiveCompany(company);
    localStorage.setItem('portal_active_company', JSON.stringify(company));

    setUser((prev) => {
      const updated = { ...prev, referenceId: company.companyId };
      localStorage.setItem('portal_user', JSON.stringify(updated));
      return updated;
    });

    return company;
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeCompany,
        authorizedCompanies,
        selectCompany,
        login,
        register,
        registerRecruiter,
        fetchRecruiterCompanies: () => fetchRecruiterCompanies(user),
        logout,
        isBackendLive,
        checkingBackend,
        isInitializing,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    if (import.meta.env?.DEV) {
      console.error(
        'useAuth() was invoked outside of an <AuthProvider>. Ensure the component is wrapped in <AuthProvider>.'
      );
    }
    return DEFAULT_AUTH_STATE;
  }
  return context;
}
