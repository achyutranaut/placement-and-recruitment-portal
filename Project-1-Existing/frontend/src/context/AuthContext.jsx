import React, { createContext, useContext, useState, useEffect } from 'react';
import { api, checkBackendHealth } from '../lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('portal_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [activeCompany, setActiveCompany] = useState(() => {
    const saved = localStorage.getItem('portal_active_company');
    return saved ? JSON.parse(saved) : null;
  });

  const [authorizedCompanies, setAuthorizedCompanies] = useState([]);
  const [isBackendLive, setIsBackendLive] = useState(false);
  const [checkingBackend, setCheckingBackend] = useState(true);

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
      const live = await checkBackendHealth();
      setIsBackendLive(live);
      setCheckingBackend(false);
      if (user?.role === 'ROLE_RECRUITER') {
        await fetchRecruiterCompanies(user);
      }
    }
    initAuth();
    const interval = setInterval(async () => {
      const live = await checkBackendHealth();
      setIsBackendLive(live);
    }, 15000);
    return () => clearInterval(interval);
  }, []);

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

  const logout = () => {
    localStorage.removeItem('portal_user');
    localStorage.removeItem('portal_token');
    localStorage.removeItem('portal_active_company');
    setUser(null);
    setActiveCompany(null);
    setAuthorizedCompanies([]);
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
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
