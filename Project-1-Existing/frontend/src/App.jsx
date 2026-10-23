import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import StudentRegisterPage from './pages/StudentRegisterPage';
import StudentPortal from './pages/StudentPortal';
import StudentProfilePage from './pages/StudentProfilePage';
import StudentResumePage from './pages/StudentResumePage';
import StudentDrivesPage from './pages/StudentDrivesPage';
import StudentDriveDetailPage from './pages/StudentDriveDetailPage';
import StudentApplicationsPage from './pages/StudentApplicationsPage';
import RecruiterPortal from './pages/RecruiterPortal';
import RecruiterRegisterPage from './pages/RecruiterRegisterPage';
import RecruiterSelectCompanyPage from './pages/RecruiterSelectCompanyPage';
import AdminPortal from './pages/AdminPortal';
import SqlCompilerPage from './pages/SqlCompilerPage';

function ProtectedRoute({ children, allowedRoles }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}

function MainLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      <Navbar />
      <main className="flex-1 pb-16">{children}</main>
      <Footer />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Landing & Authentication Pages */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/student/login" element={<LoginPage />} />
          <Route path="/recruiter/login" element={<LoginPage />} />
          <Route path="/student/register" element={<StudentRegisterPage />} />
          <Route path="/recruiter/register" element={<RecruiterRegisterPage />} />
          <Route
            path="/recruiter/select-company"
            element={
              <ProtectedRoute allowedRoles={['ROLE_RECRUITER', 'ROLE_ADMIN']}>
                <RecruiterSelectCompanyPage />
              </ProtectedRoute>
            }
          />
          <Route path="/recruiter/dashboard" element={<Navigate to="/recruiter" replace />} />

          {/* Student Dedicated Portal Routes */}
          <Route
            path="/student"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentPortal />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/profile"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentProfilePage />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/resume"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentResumePage />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/drives"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentDrivesPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/drives/:id"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentDriveDetailPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/applications"
            element={
              <ProtectedRoute allowedRoles={['ROLE_STUDENT', 'ROLE_ADMIN']}>
                <MainLayout>
                  <StudentApplicationsPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* Recruiter Portal Route */}
          <Route
            path="/recruiter"
            element={
              <ProtectedRoute allowedRoles={['ROLE_RECRUITER', 'ROLE_ADMIN']}>
                <MainLayout>
                  <RecruiterPortal />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* Administrative / Placement Cell Route */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <MainLayout>
                  <AdminPortal />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/sql-compiler"
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <MainLayout>
                  <SqlCompilerPage />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* Catch-all redirect */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
