import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import {
  GraduationCap,
  User,
  Mail,
  Phone,
  MapPin,
  Calendar,
  Award,
  CheckCircle2,
  AlertCircle,
  Save,
  Plus,
  X,
  BookOpen,
  ArrowLeft,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

export default function StudentProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const studentId = user?.referenceId;

  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notification, setNotification] = useState(null);

  // Form State
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [branch, setBranch] = useState('');
  const [cgpa, setCgpa] = useState('');
  const [dob, setDob] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [phone, setPhone] = useState('');
  const [skills, setSkills] = useState([]);
  const [newSkillInput, setNewSkillInput] = useState('');

  const loadProfile = async () => {
    if (!studentId) {
      navigate('/login?role=STUDENT', { replace: true });
      return;
    }

    try {
      setLoading(true);
      const data = await api.getStudent(studentId);
      setStudent(data);
      setName(data.name || '');
      setEmail(data.email || '');
      setBranch(data.branch || '');
      setCgpa(data.cgpa != null ? String(data.cgpa) : '');
      setDob(data.dob || '');
      setStreet(data.street || '');
      setCity(data.city || '');
      setState(data.state || '');
      setPhone(data.phoneNumbers?.[0] || '');
      setSkills(Array.isArray(data.skills) ? data.skills : []);
    } catch (err) {
      console.error('Failed to load student profile:', err);
      setNotification({
        type: 'error',
        message: err.message || 'Failed to fetch student record from Oracle database.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [studentId]);

  const handleAddSkill = (e) => {
    e.preventDefault();
    const trimmed = newSkillInput.trim();
    if (trimmed && !skills.includes(trimmed)) {
      setSkills([...skills, trimmed]);
      setNewSkillInput('');
    }
  };

  const handleRemoveSkill = (skillToRemove) => {
    setSkills(skills.filter((s) => s !== skillToRemove));
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setNotification(null);

    try {
      const payload = {
        studentId,
        name,
        email,
        branch,
        cgpa: parseFloat(cgpa) || 0,
        dob,
        street,
        city,
        state,
        phoneNumbers: phone ? [phone] : [],
        skills,
      };

      const updated = await api.updateStudentProfile(studentId, payload);
      setStudent(updated);
      setNotification({
        type: 'success',
        message: 'Profile updated successfully and persisted to Oracle database (STUDENT & STUDENT_SKILL).',
      });
    } catch (err) {
      setNotification({
        type: 'error',
        message: err.message || 'Profile update failed.',
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-slate-700 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading Student Profile...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Breadcrumb Navigation */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/student" className="hover:text-slate-900 flex items-center gap-1 font-medium">
            <ArrowLeft className="w-3.5 h-3.5" /> Back to Student Dashboard
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Academic Profile Management</span>
        </div>

        <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-slate-100 text-slate-700 border border-slate-300">
          ID: {studentId}
        </span>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
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
              {notification.type === 'success' ? 'Profile Saved' : 'Update Notice'}
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

      {/* Header Banner */}
      <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-lg bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-700 shrink-0">
              <GraduationCap className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">{name || 'Student Profile'}</h1>
              <p className="text-xs text-slate-500 mt-0.5 font-medium">
                {branch} • VIT Placement Registration Record
              </p>
              <div className="flex items-center gap-3 mt-1.5 text-xs text-slate-600">
                <span>DOB: <strong className="text-slate-800">{dob}</strong></span>
                <span>•</span>
                <span>Derived Age: <strong className="text-slate-800">{student?.age ?? '21'} Years</strong> (DA1 Specification)</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
            <div className="text-right">
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
                Cumulative CGPA
              </span>
              <span className="text-2xl font-bold text-slate-900 font-mono">
                {cgpa ? Number(cgpa).toFixed(2) : '0.00'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Profile Form */}
      <form onSubmit={handleSave} className="space-y-6">
        {/* Academic & Personal Information */}
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-4 pb-2 border-b border-slate-100 flex items-center gap-2">
            <User className="w-4 h-4 text-slate-600" />
            1. Personal & Academic Credentials
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Full Legal Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Institutional Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900 bg-slate-50"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Academic Branch / Specialization</label>
              <select
                value={branch}
                onChange={(e) => setBranch(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900 bg-white"
              >
                <option value="Computer Science & Engineering">Computer Science & Engineering</option>
                <option value="Information Technology">Information Technology</option>
                <option value="Data Science">Data Science</option>
                <option value="Electronics & Communication">Electronics & Communication</option>
                <option value="Electrical & Electronics">Electrical & Electronics</option>
                <option value="Mechanical Engineering">Mechanical Engineering</option>
                <option value="Civil Engineering">Civil Engineering</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Cumulative Grade Point Average (CGPA)
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                max="10"
                value={cgpa}
                onChange={(e) => setCgpa(e.target.value)}
                required
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900 font-mono"
              />
              <span className="text-[11px] text-slate-500 mt-0.5 block">
                Validated server-side against recruitment drive cutoff thresholds.
              </span>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Date of Birth (DOB)</label>
              <input
                type="date"
                value={dob}
                onChange={(e) => setDob(e.target.value)}
                required
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
              <span className="text-[11px] text-slate-500 mt-0.5 block">
                Age is a derived attribute (omitted from physical storage per DA1 BCNF).
              </span>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Primary Contact Phone</label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+91-9876543210"
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
              <span className="text-[11px] text-slate-500 mt-0.5 block">
                Mapped to STUDENT_PHONE relational junction table.
              </span>
            </div>
          </div>
        </div>

        {/* Address Information */}
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-4 pb-2 border-b border-slate-100 flex items-center gap-2">
            <MapPin className="w-4 h-4 text-slate-600" />
            2. Permanent Residence Address
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-3">
              <label className="block text-xs font-semibold text-slate-700 mb-1">Street Address</label>
              <input
                type="text"
                value={street}
                onChange={(e) => setStreet(e.target.value)}
                placeholder="Door No, Street name, Area"
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">City</label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-semibold text-slate-700 mb-1">State / Province</label>
              <input
                type="text"
                value={state}
                onChange={(e) => setState(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
              />
            </div>
          </div>
        </div>

        {/* Technical Skills (STUDENT_SKILL) */}
        <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-800 mb-4 pb-2 border-b border-slate-100 flex items-center gap-2">
            <Award className="w-4 h-4 text-slate-600" />
            3. Technical Competencies & Skills (Oracle Table: STUDENT_SKILL)
          </h2>

          <div className="mb-4 flex flex-wrap gap-2">
            {skills.map((skill) => (
              <span
                key={skill}
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium bg-slate-100 text-slate-800 border border-slate-300"
              >
                <span>{skill}</span>
                <button
                  type="button"
                  onClick={() => handleRemoveSkill(skill)}
                  className="hover:text-rose-600 focus:outline-none"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </span>
            ))}
            {skills.length === 0 && (
              <p className="text-xs text-slate-400 italic">No skills registered yet. Add relevant competencies below.</p>
            )}
          </div>

          <div className="flex gap-2">
            <input
              type="text"
              value={newSkillInput}
              onChange={(e) => setNewSkillInput(e.target.value)}
              placeholder="e.g. Docker, Python, Spring Boot, AWS..."
              className="flex-1 px-3 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-1 focus:ring-slate-900"
            />
            <button
              type="button"
              onClick={handleAddSkill}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-md text-xs font-semibold inline-flex items-center gap-1 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Skill
            </button>
          </div>
        </div>

        {/* Submit Bar */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={loadProfile}
            className="px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-100 rounded-md transition-colors"
          >
            Reset Changes
          </button>
          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-md transition-colors shadow-sm disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {saving ? 'Persisting to Database...' : 'Save Profile Changes'}
          </button>
        </div>
      </form>
    </div>
  );
}
