import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../lib/api';
import {
  GraduationCap,
  ArrowRight,
  Database,
  Lock,
  User,
  Mail,
  Hash,
  BookOpen,
  Award,
  Phone,
  MapPin,
  AlertCircle,
  CheckCircle2,
  Search,
  ChevronDown,
  X,
  Check,
  Loader2,
  RefreshCw,
} from 'lucide-react';

const DEGREE_ORDER = ["Bachelor's", "Master's", "Integrated", "Doctoral"];

export default function StudentRegisterPage() {
  const navigate = useNavigate();
  const { isBackendLive, register } = useAuth();

  const [programs, setPrograms] = useState([]);
  const [loadingPrograms, setLoadingPrograms] = useState(true);
  const [programsError, setProgramsError] = useState('');
  const [selectedProgram, setSelectedProgram] = useState(null);

  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const dropdownRef = useRef(null);
  const searchInputRef = useRef(null);

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    registrationNo: '',
    programId: '',
    branch: '',
    password: '',
    confirmPassword: '',
    cgpa: '',
    dob: '',
    phone: '',
    street: '',
    city: '',
    state: '',
    skills: '',
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Fetch complete academic program catalog from Oracle via Spring Boot API
  const fetchPrograms = async () => {
    setLoadingPrograms(true);
    setProgramsError('');
    try {
      const res = await api.getPrograms();
      const list = Array.isArray(res) ? res : (res?.data || []);
      setPrograms(list);
      if (list.length > 0) {
        // Default to B.Tech CSE if present, else first program
        const defaultProg = list.find((p) => p.programId === 'BTECH-CSE') || list[0];
        setSelectedProgram(defaultProg);
        setFormData((prev) => ({
          ...prev,
          programId: defaultProg.programId,
          branch: defaultProg.programName,
        }));
      }
    } catch (err) {
      console.error('Failed to load academic program catalog:', err);
      setProgramsError('Unable to load programs. Please try again.');
    } finally {
      setLoadingPrograms(false);
    }
  };

  useEffect(() => {
    fetchPrograms();
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Filtered programs based on search input
  const filteredPrograms = useMemo(() => {
    if (!searchQuery.trim()) return programs;
    const q = searchQuery.toLowerCase().trim();
    return programs.filter((p) => {
      const nameMatch = (p.programName || '').toLowerCase().includes(q);
      const idMatch = (p.programId || '').toLowerCase().includes(q);
      const specMatch = (p.specialization || '').toLowerCase().includes(q);
      const famMatch = (p.disciplineFamily || '').toLowerCase().includes(q);
      const levelMatch = (p.programLevel || '').toLowerCase().includes(q);
      return nameMatch || idMatch || specMatch || famMatch || levelMatch;
    });
  }, [programs, searchQuery]);

  // Grouped programs by Degree Level
  const groupedPrograms = useMemo(() => {
    const groups = {};
    DEGREE_ORDER.forEach((lvl) => {
      groups[lvl] = [];
    });

    filteredPrograms.forEach((p) => {
      const lvl = p.programLevel || "Bachelor's";
      if (!groups[lvl]) groups[lvl] = [];
      groups[lvl].push(p);
    });

    return groups;
  }, [filteredPrograms]);

  // Flattened items for keyboard navigation
  const flatItems = useMemo(() => {
    const items = [];
    DEGREE_ORDER.forEach((lvl) => {
      if (groupedPrograms[lvl] && groupedPrograms[lvl].length > 0) {
        groupedPrograms[lvl].forEach((p) => items.push(p));
      }
    });
    return items;
  }, [groupedPrograms]);

  const handleSelectProgram = (p) => {
    setSelectedProgram(p);
    setFormData((prev) => ({
      ...prev,
      programId: p.programId,
      branch: p.programName,
    }));
    setIsOpen(false);
    setSearchQuery('');
    setError('');
  };

  const handleKeyDown = (e) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        setIsOpen(true);
        e.preventDefault();
      }
      return;
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev < flatItems.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : flatItems.length - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (highlightedIndex >= 0 && highlightedIndex < flatItems.length) {
        handleSelectProgram(flatItems[highlightedIndex]);
      }
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  // Dynamic Registration Number Validation based on Oracle PROGRAM metadata
  const regNoUpper = (formData.registrationNo || '').trim().toUpperCase();
  const regPattern = selectedProgram?.regPattern
    ? new RegExp(selectedProgram.regPattern)
    : /^2[0-9][A-Z]{2,5}[0-9]{3,5}$/;
  const isRegNoValid = regPattern.test(regNoUpper);
  const regExample = selectedProgram?.regExample || '21BCE1234';
  const isEmailValid = (formData.email || '').trim().toLowerCase().endsWith('@vitstudent.ac.in');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');

    if (!selectedProgram) {
      setError('Please select an academic program from the curriculum catalog.');
      return;
    }

    if (!isEmailValid) {
      setError('Registration requires an official VIT student email ending with @vitstudent.ac.in');
      return;
    }

    if (!isRegNoValid) {
      setError(
        `Invalid registration number for ${selectedProgram.programName}. Format must match pattern (e.g. ${regExample}).`
      );
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must contain at least 6 characters.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match. Please re-enter your password.');
      return;
    }

    const numCgpa = parseFloat(formData.cgpa);
    if (isNaN(numCgpa) || numCgpa < 0 || numCgpa > 10) {
      setError('Please enter a valid CGPA between 0.00 and 10.00.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        name: formData.name.trim(),
        email: formData.email.trim().toLowerCase(),
        registrationNo: regNoUpper,
        programId: selectedProgram.programId,
        branch: selectedProgram.programName,
        password: formData.password,
        cgpa: numCgpa,
        dob: formData.dob,
        phone: formData.phone.trim(),
        street: formData.street ? formData.street.trim() : '',
        city: formData.city ? formData.city.trim() : '',
        state: formData.state ? formData.state.trim() : '',
        skills: formData.skills
          ? formData.skills.split(',').map((s) => s.trim()).filter(Boolean)
          : [],
      };

      const res = await register(payload);
      if (res) {
        navigate('/student', { replace: true });
      } else {
        navigate('/login?role=STUDENT&registered=true');
      }
    } catch (err) {
      setError(err.message || 'Registration failed. Please check your information and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-10 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-xl">
        <div className="flex justify-center">
          <div className="w-12 h-12 rounded-lg bg-slate-900 flex items-center justify-center text-white shadow-sm">
            <GraduationCap className="w-7 h-7" />
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-slate-900">
          Student Placement Registration
        </h2>
        <p className="mt-1 text-center text-xs text-slate-500 font-medium">
          Vellore Institute of Technology • Centre for Career Planning & Placements
        </p>

        <div className="mt-3 flex justify-center">
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full text-xs font-semibold border ${
              isBackendLive
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                : 'bg-slate-100 text-slate-600 border-slate-300'
            }`}
          >
            <Database className="w-3.5 h-3.5" />
            {isBackendLive ? 'Oracle Database • Connected' : 'Checking Backend Connection (:8080)...'}
          </span>
        </div>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-xl">
        <div className="bg-white py-6 px-4 shadow-sm sm:rounded-lg sm:px-8 border border-slate-200">
          {error && (
            <div className="mb-4 rounded-md bg-rose-50 p-3 border border-rose-200 flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-rose-600 mt-0.5 shrink-0" />
              <div className="text-xs text-rose-700 font-medium">{error}</div>
            </div>
          )}

          <form onSubmit={handleRegister} className="space-y-4">
            {/* Full Name & Email Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700">Full Name *</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <User className="h-4 w-4" />
                  </div>
                  <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                    placeholder="e.g. Aarav Sharma"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Institutional Email *
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Mail className="h-4 w-4" />
                  </div>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    placeholder="student@vitstudent.ac.in"
                    className={`block w-full pl-9 pr-3 py-2 border rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900 ${
                      formData.email && !isEmailValid ? 'border-amber-400 bg-amber-50/30' : 'border-slate-300'
                    }`}
                  />
                </div>
                {formData.email && !isEmailValid && (
                  <p className="mt-1 text-[11px] text-amber-700">Must end with @vitstudent.ac.in</p>
                )}
              </div>
            </div>

            {/* Program / Course Searchable Selector */}
            <div>
              <div className="flex items-center justify-between">
                <label className="block text-xs font-semibold text-slate-700">
                  Program / Course *
                </label>
                {programs.length > 0 && (
                  <span className="text-[10px] text-slate-500 font-medium">
                    {programs.length} curriculum programs available
                  </span>
                )}
              </div>

              {loadingPrograms ? (
                <div className="mt-1 flex items-center gap-2 p-2.5 border border-slate-200 rounded-md bg-slate-50 text-xs text-slate-500">
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-slate-600" />
                  <span>Loading curriculum catalog from university database...</span>
                </div>
              ) : programsError ? (
                <div className="mt-1 p-3 border border-rose-200 rounded-md bg-rose-50 flex items-center justify-between text-xs text-rose-700">
                  <span>{programsError}</span>
                  <button
                    type="button"
                    onClick={fetchPrograms}
                    className="inline-flex items-center gap-1 font-semibold text-rose-800 underline hover:no-underline"
                  >
                    <RefreshCw className="w-3 h-3" /> Retry
                  </button>
                </div>
              ) : programs.length === 0 ? (
                <div className="mt-1 p-2.5 border border-slate-200 rounded-md bg-slate-50 text-xs text-slate-500">
                  No programs currently available.
                </div>
              ) : (
                <div className="mt-1 relative" ref={dropdownRef}>
                  {/* Selector Input Trigger */}
                  <div
                    onClick={() => {
                      setIsOpen(!isOpen);
                      if (!isOpen) {
                        setTimeout(() => searchInputRef.current?.focus(), 50);
                      }
                    }}
                    onKeyDown={handleKeyDown}
                    tabIndex={0}
                    className={`w-full flex items-center justify-between pl-3 pr-2 py-2 border rounded-md text-xs cursor-pointer transition-all bg-white ${
                      isOpen ? 'ring-1 ring-slate-900 border-slate-900' : 'border-slate-300 hover:border-slate-400'
                    }`}
                  >
                    <div className="flex items-center gap-2 truncate">
                      <BookOpen className="w-4 h-4 text-slate-400 shrink-0" />
                      {selectedProgram ? (
                        <div className="flex items-center gap-1.5 truncate">
                          <span className="font-semibold text-slate-900 truncate">
                            {selectedProgram.programName}
                          </span>
                          <span className="shrink-0 px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
                            {selectedProgram.programLevel}
                          </span>
                        </div>
                      ) : (
                        <span className="text-slate-400">Search and select your academic program...</span>
                      )}
                    </div>
                    <div className="flex items-center gap-1 text-slate-400">
                      {selectedProgram && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedProgram(null);
                            setFormData((prev) => ({ ...prev, programId: '', branch: '' }));
                            setIsOpen(true);
                            setTimeout(() => searchInputRef.current?.focus(), 50);
                          }}
                          className="p-1 hover:text-slate-700 rounded"
                          title="Clear selection"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      )}
                      <ChevronDown className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
                    </div>
                  </div>

                  {/* Dropdown Menu */}
                  {isOpen && (
                    <div className="absolute z-50 left-0 right-0 mt-1 bg-white border border-slate-200 rounded-lg shadow-lg overflow-hidden animate-in fade-in slide-in-from-top-1 duration-150">
                      {/* Search Bar */}
                      <div className="p-2 border-b border-slate-100 bg-slate-50/50">
                        <div className="relative">
                          <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                          <input
                            ref={searchInputRef}
                            type="text"
                            value={searchQuery}
                            onChange={(e) => {
                              setSearchQuery(e.target.value);
                              setHighlightedIndex(0);
                            }}
                            onKeyDown={handleKeyDown}
                            placeholder="Type to search (e.g. CSE, AI & ML, Mechanical, MBA, MCA)..."
                            className="w-full pl-8 pr-3 py-1.5 text-xs bg-white border border-slate-200 rounded-md text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:ring-slate-900"
                          />
                          {searchQuery && (
                            <button
                              type="button"
                              onClick={() => setSearchQuery('')}
                              className="absolute right-2.5 top-2 text-slate-400 hover:text-slate-600"
                            >
                              <X className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                        {searchQuery && (
                          <div className="mt-1 text-[10px] text-slate-500 font-medium px-1">
                            Found {filteredPrograms.length} matching programs
                          </div>
                        )}
                      </div>

                      {/* Grouped Options List */}
                      <div className="max-h-60 overflow-y-auto divide-y divide-slate-100">
                        {filteredPrograms.length === 0 ? (
                          <div className="p-4 text-center text-xs text-slate-500">
                            No programs found matching &ldquo;{searchQuery}&rdquo;.
                          </div>
                        ) : (
                          DEGREE_ORDER.map((lvl) => {
                            const list = groupedPrograms[lvl] || [];
                            if (list.length === 0) return null;
                            const groupHeader =
                              lvl === "Bachelor's"
                                ? "BACHELOR'S PROGRAMS"
                                : lvl === "Master's"
                                ? "MASTER'S PROGRAMS"
                                : lvl === "Integrated"
                                ? "INTEGRATED PROGRAMS"
                                : "DOCTORAL PROGRAMS";

                            return (
                              <div key={lvl} className="py-1">
                                <div className="px-3 py-1 text-[10px] font-bold tracking-wider text-slate-500 uppercase bg-slate-50/70 sticky top-0 border-y border-slate-100/80">
                                  {groupHeader} ({list.length})
                                </div>
                                {list.map((p) => {
                                  const isSelected = selectedProgram?.programId === p.programId;
                                  const isHighlighted =
                                    highlightedIndex >= 0 && flatItems[highlightedIndex]?.programId === p.programId;

                                  return (
                                    <div
                                      key={p.programId}
                                      onClick={() => handleSelectProgram(p)}
                                      onMouseEnter={() => {
                                        const idx = flatItems.findIndex((it) => it.programId === p.programId);
                                        setHighlightedIndex(idx);
                                      }}
                                      className={`px-3 py-2 text-xs cursor-pointer flex items-center justify-between transition-colors ${
                                        isSelected
                                          ? 'bg-slate-900 text-white font-medium'
                                          : isHighlighted
                                          ? 'bg-slate-100 text-slate-900'
                                          : 'text-slate-800 hover:bg-slate-50'
                                      }`}
                                    >
                                      <div className="flex flex-col truncate pr-2">
                                        <div className="flex items-center gap-1.5 truncate">
                                          <span className="font-semibold truncate">{p.programName}</span>
                                          {p.specialization && (
                                            <span
                                              className={`text-[9px] px-1 py-0.2 rounded font-medium truncate ${
                                                isSelected
                                                  ? 'bg-slate-800 text-slate-200 border border-slate-700'
                                                  : 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                                              }`}
                                            >
                                              {p.specialization}
                                            </span>
                                          )}
                                        </div>
                                        <div
                                          className={`text-[10px] mt-0.5 flex items-center gap-2 ${
                                            isSelected ? 'text-slate-300' : 'text-slate-500'
                                          }`}
                                        >
                                          <span>{p.disciplineFamily}</span>
                                          <span>•</span>
                                          <span>{p.duration}</span>
                                          <span>•</span>
                                          <span className="font-mono text-[9px]">{p.programId}</span>
                                        </div>
                                      </div>
                                      {isSelected && <Check className="w-4 h-4 text-white shrink-0" />}
                                    </div>
                                  );
                                })}
                              </div>
                            );
                          })
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Registration Number & CGPA Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Registration Number *
                </label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Hash className="h-4 w-4" />
                  </div>
                  <input
                    type="text"
                    name="registrationNo"
                    value={formData.registrationNo}
                    onChange={handleChange}
                    required
                    placeholder={`e.g. ${regExample}`}
                    className={`block w-full pl-9 pr-3 py-2 border rounded-md text-xs font-medium uppercase text-slate-900 focus:ring-1 focus:ring-slate-900 ${
                      regNoUpper && !isRegNoValid ? 'border-rose-300 bg-rose-50/30' : 'border-slate-300'
                    }`}
                  />
                </div>
                <div className="mt-1 flex items-center justify-between text-[11px]">
                  <span className="text-slate-500">Format: {regExample}</span>
                  {regNoUpper && (
                    <span className={isRegNoValid ? 'text-emerald-700 font-semibold' : 'text-rose-600 font-medium'}>
                      {isRegNoValid ? '✓ Valid Pattern' : '✗ Invalid Pattern'}
                    </span>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700">Cumulative CGPA *</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Award className="h-4 w-4" />
                  </div>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="10"
                    name="cgpa"
                    value={formData.cgpa}
                    onChange={handleChange}
                    required
                    placeholder="e.g. 8.75"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>
            </div>

            {/* DOB & Phone Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700">Date of Birth *</label>
                <input
                  type="date"
                  name="dob"
                  value={formData.dob}
                  onChange={handleChange}
                  required
                  className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700">Mobile Phone *</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Phone className="h-4 w-4" />
                  </div>
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleChange}
                    required
                    placeholder="+91-9876543210"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>
            </div>

            {/* City & State */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700">City</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <MapPin className="h-4 w-4" />
                  </div>
                  <input
                    type="text"
                    name="city"
                    value={formData.city}
                    onChange={handleChange}
                    placeholder="e.g. Vellore"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700">State</label>
                <input
                  type="text"
                  name="state"
                  value={formData.state}
                  onChange={handleChange}
                  placeholder="e.g. Tamil Nadu"
                  className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                />
              </div>
            </div>

            {/* Skills */}
            <div>
              <label className="block text-xs font-semibold text-slate-700">
                Core Technical Skills (comma separated)
              </label>
              <input
                type="text"
                name="skills"
                value={formData.skills}
                onChange={handleChange}
                placeholder="e.g. Java, Python, SQL, Docker, React, Spring Boot"
                className="mt-1 block w-full px-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
              />
            </div>

            {/* Passwords */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
              <div>
                <label className="block text-xs font-semibold text-slate-700">Password *</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    placeholder="••••••••"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700">Confirm Password *</label>
                <div className="mt-1 relative rounded-md shadow-sm">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                    <Lock className="h-4 w-4" />
                  </div>
                  <input
                    type="password"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    required
                    placeholder="••••••••"
                    className="block w-full pl-9 pr-3 py-2 border border-slate-300 rounded-md text-xs font-medium text-slate-900 focus:ring-1 focus:ring-slate-900"
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 py-2.5 px-4 border border-transparent rounded-md text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-slate-500 transition-all disabled:opacity-50 shadow-sm cursor-pointer"
              >
                {loading ? (
                  <span>Registering with University Database...</span>
                ) : (
                  <>
                    <span>Complete Placement Registration</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Already registered */}
          <div className="mt-6 pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
            <span>Already registered with Placement Cell? </span>
            <Link to="/login?role=STUDENT" className="font-semibold text-slate-900 hover:underline">
              Sign In to Student Portal
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
