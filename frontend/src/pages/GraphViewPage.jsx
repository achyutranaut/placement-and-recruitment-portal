import React, { useState, useEffect, useRef } from 'react';
import { Network } from 'vis-network/standalone';
import { api } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import {
  Share2,
  Database,
  Search,
  Sparkles,
  Users,
  Briefcase,
  Layers,
  ChevronRight,
  Info,
  RefreshCw,
  Award,
  CheckCircle,
  XCircle,
  Clock,
  PlusCircle,
  Link2,
  Trash2,
  Edit3,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Filter,
  AlertTriangle,
  X
} from 'lucide-react';

export default function GraphViewPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const containerRef = useRef(null);
  const networkInstanceRef = useRef(null);

  // Active Target Student
  const [studentId, setStudentId] = useState(
    user?.role === 'ROLE_STUDENT' ? (user.referenceId || 'STU023') : 'STU023'
  );
  const [allStudents, setAllStudents] = useState([]);

  // Data States
  const [loading, setLoading] = useState(false);
  const [placementPathData, setPlacementPathData] = useState([]);
  const [eligibleDrivesData, setEligibleDrivesData] = useState([]);
  const [recommendationsData, setRecommendationsData] = useState([]);
  const [selectedNodeInfo, setSelectedNodeInfo] = useState(null);
  const [activeTab, setActiveTab] = useState('path'); // 'path', 'eligible', 'peers'
  const [backendInfo, setBackendInfo] = useState(null);

  // Interactive View Controls
  const [searchQuery, setSearchQuery] = useState('');
  const [visibleTypes, setVisibleTypes] = useState({
    Student: true,
    Application: true,
    Drive: true,
    Company: true,
    Offer: true,
    Interview: true,
  });

  // Admin Modals
  const [showAddModal, setShowAddModal] = useState(false);
  const [showConnectModal, setShowConnectModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [mutationLoading, setMutationLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  // Form States for Mutations
  const [newNodeLabel, setNewNodeLabel] = useState('Company');
  const [newNodeProps, setNewNodeProps] = useState({
    companyName: '',
    industry: '',
    email: '',
  });

  const [connectFrom, setConnectFrom] = useState({ label: 'Company', id: '' });
  const [connectTo, setConnectTo] = useState({ label: 'Drive', id: '' });
  const [connectRelType, setConnectRelType] = useState('HOSTS_DRIVE');

  const [editProps, setEditProps] = useState({});

  // Graph Meta for Summary Card
  const [graphMetrics, setGraphMetrics] = useState({ nodeCount: 0, edgeCount: 0, typeCounts: {} });

  useEffect(() => {
    loadBackendInfo();
    loadStudentDirectory();
  }, []);

  useEffect(() => {
    loadGraphData();
    return () => {
      if (networkInstanceRef.current) {
        networkInstanceRef.current.destroy();
        networkInstanceRef.current = null;
      }
    };
  }, [studentId]);

  const loadBackendInfo = async () => {
    try {
      const data = await api.getBackendInfo();
      setBackendInfo(data);
    } catch (err) {
      console.error('Failed to load backend info:', err);
    }
  };

  const loadStudentDirectory = async () => {
    try {
      const list = await api.getAllStudents();
      if (Array.isArray(list) && list.length > 0) {
        setAllStudents(list);
      }
    } catch (_) {}
  };

  const showToast = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 5000);
  };

  const loadGraphData = async () => {
    if (!studentId) return;
    try {
      setLoading(true);
      const [path, eligible, recs] = await Promise.allSettled([
        api.getPlacementPathGraph(studentId),
        api.getEligibleDrivesGraph(studentId),
        api.getRecommendationsGraph(studentId),
      ]);

      const pathData = path.status === 'fulfilled' ? path.value : [];
      const eligibleData = eligible.status === 'fulfilled' ? eligible.value : [];
      const recsData = recs.status === 'fulfilled' ? recs.value : [];

      setPlacementPathData(pathData);
      setEligibleDrivesData(eligibleData);
      setRecommendationsData(recsData);

      renderNetwork(activeTab, { pathData, eligibleData, recsData }, visibleTypes);
    } catch (err) {
      console.error('Error fetching graph data:', err);
      showToast('Error loading graph traversal: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const renderNetwork = (tab, dataBundle, typeFilter = visibleTypes) => {
    if (!containerRef.current) return;

    const pathData = dataBundle?.pathData || placementPathData;
    const eligibleData = dataBundle?.eligibleData || eligibleDrivesData;
    const recsData = dataBundle?.recsData || recommendationsData;

    const nodes = [];
    const edges = [];
    const addedNodes = new Set();
    const typeCounts = {};

    const addNode = (node) => {
      if (!typeFilter[node.nodeType]) return;
      if (!addedNodes.has(node.id)) {
        addedNodes.add(node.id);
        nodes.push(node);
        typeCounts[node.nodeType] = (typeCounts[node.nodeType] || 0) + 1;
      }
    };

    const addEdge = (edge) => {
      // Only include edge if both source and target are currently visible
      if (addedNodes.has(edge.from) && addedNodes.has(edge.to)) {
        edges.push(edge);
      }
    };

    // Edge formatting standards - crisp high-contrast pill badges
    const edgeBaseFont = {
      size: 11,
      color: '#0f172a',
      strokeWidth: 0,
      strokeColor: 'transparent',
      align: 'middle',
      background: 'rgba(255, 255, 255, 0.96)',
    };

    if (tab === 'path') {
      // 1. Student node (Level 0)
      const studentLabel = pathData.length > 0 ? pathData[0].studentName : studentId;
      addNode({
        id: studentId,
        label: `Student\n${studentLabel}\n[${studentId}]`,
        color: { background: '#2563eb', border: '#1d4ed8', highlight: { background: '#1d4ed8', border: '#60a5fa' } },
        shape: 'box',
        level: 0,
        margin: 12,
        font: { color: '#ffffff', bold: true, size: 13 },
        nodeType: 'Student',
        details: { id: studentId, name: studentLabel, program: pathData[0]?.programName || 'Academic Program' }
      });

      pathData.forEach((app) => {
        const appId = app.applicationId;
        // 2. Application node (Level 1)
        addNode({
          id: appId,
          label: `Application\n${appId}\n(${app.offerStatus || 'IN_PROGRESS'})`,
          color: { background: '#7c3aed', border: '#6d28d9', highlight: { background: '#6d28d9', border: '#c084fc' } },
          shape: 'ellipse',
          level: 1,
          margin: 10,
          font: { color: '#ffffff', size: 12 },
          nodeType: 'Application',
          details: app
        });

        addEdge({
          from: studentId,
          to: appId,
          label: 'SUBMITTED',
          arrows: 'to',
          color: { color: '#8b5cf6', highlight: '#7c3aed' },
          font: edgeBaseFont,
        });

        // 3. Drive node (Level 2)
        const driveId = app.driveId;
        addNode({
          id: driveId,
          label: `Placement Drive\n${app.jobTitle}\n[${driveId}]`,
          color: { background: '#d97706', border: '#b45309', highlight: { background: '#b45309', border: '#fcd34d' } },
          shape: 'box',
          level: 2,
          margin: 10,
          font: { color: '#ffffff', bold: true, size: 12 },
          nodeType: 'Drive',
          details: { driveId, jobTitle: app.jobTitle, companyName: app.companyName }
        });

        addEdge({
          from: appId,
          to: driveId,
          label: 'FOR_DRIVE',
          arrows: 'to',
          color: { color: '#d97706', highlight: '#b45309' },
          font: edgeBaseFont,
        });

        // 4. Company node (Level 3)
        const compId = `COM_${app.companyName.replace(/\s+/g, '_')}`;
        addNode({
          id: compId,
          label: `Company Partner\n${app.companyName}`,
          color: { background: '#059669', border: '#047857', highlight: { background: '#047857', border: '#6ee7b7' } },
          shape: 'box',
          level: 3,
          margin: 12,
          font: { color: '#ffffff', bold: true, size: 12 },
          nodeType: 'Company',
          details: { companyName: app.companyName }
        });

        addEdge({
          from: driveId,
          to: compId,
          label: 'HOSTED_BY',
          arrows: 'to',
          color: { color: '#059669', highlight: '#047857' },
          font: edgeBaseFont,
        });

        // 5. Interviews (Level 2 branch)
        if (app.interviewSteps && app.interviewSteps.length > 0) {
          app.interviewSteps.forEach((step, idx) => {
            const ivId = `${appId}_IV_${idx + 1}`;
            const isCleared = step.result === 'CLEARED' || step.result === 'PASSED';
            const bg = isCleared ? '#0d9488' : (step.result === 'REJECTED' ? '#e11d48' : '#eab308');
            addNode({
              id: ivId,
              label: `Interview Round ${step.roundNo}\n${step.interviewerName}\nResult: ${step.result}`,
              color: { background: bg, border: '#334155' },
              shape: 'ellipse',
              level: 2,
              font: { color: '#ffffff', size: 11 },
              nodeType: 'Interview',
              details: step
            });
            addEdge({
              from: appId,
              to: ivId,
              label: 'HAS_INTERVIEW',
              arrows: 'to',
              color: { color: '#64748b' },
              font: edgeBaseFont,
            });
          });
        }

        // 6. Offer (Level 3)
        if (app.offerId) {
          const offerNodeId = app.offerId;
          addNode({
            id: offerNodeId,
            label: `Official Offer\n${app.ctcLpa} LPA\n[Status: ${app.offerStatus || 'OFFERED'}]`,
            color: { background: '#10b981', border: '#047857', highlight: { background: '#059669', border: '#a7f3d0' } },
            shape: 'star',
            level: 3,
            size: 28,
            font: { color: '#ffffff', bold: true, size: 12 },
            nodeType: 'Offer',
            details: { offerId: app.offerId, ctc: app.ctcLpa, status: app.offerStatus }
          });
          addEdge({
            from: appId,
            to: offerNodeId,
            label: 'RESULTED_IN',
            arrows: 'to',
            width: 3,
            color: { color: '#10b981', highlight: '#059669' },
            font: edgeBaseFont,
          });
        }
      });
    } else if (tab === 'eligible') {
      // Hub-and-Spoke / Two-Tier Hierarchy
      addNode({
        id: studentId,
        label: `Target Candidate\n${studentId}`,
        color: { background: '#2563eb', border: '#1d4ed8' },
        shape: 'box',
        level: 0,
        margin: 14,
        font: { color: '#ffffff', bold: true, size: 13 },
        nodeType: 'Student'
      });

      eligibleData.forEach((drv) => {
        addNode({
          id: drv.driveId,
          label: `${drv.jobTitle}\n${drv.companyName}\nMin CGPA: ${drv.minCgpa} • CTC: ${drv.ctcLpa} LPA`,
          color: { background: '#d97706', border: '#b45309' },
          shape: 'box',
          level: 1,
          margin: 12,
          font: { color: '#ffffff', bold: true, size: 12 },
          nodeType: 'Drive',
          details: drv
        });
        addEdge({
          from: studentId,
          to: drv.driveId,
          label: 'ELIGIBLE_FOR',
          arrows: 'to',
          width: 2,
          color: { color: '#d97706' },
          font: edgeBaseFont,
        });
      });
    } else if (tab === 'peers') {
      // 3-Tier Recommendation Flow
      addNode({
        id: studentId,
        label: `Target Student\n${studentId}`,
        color: { background: '#2563eb', border: '#1d4ed8' },
        shape: 'box',
        level: 0,
        margin: 14,
        font: { color: '#ffffff', bold: true, size: 13 },
        nodeType: 'Student'
      });

      recsData.forEach((peer) => {
        const peerId = peer.studentId;
        addNode({
          id: peerId,
          label: `Peer Candidate\n${peer.studentName}\nCGPA: ${peer.cgpa} (Δ ${peer.cgpaDifference})`,
          color: { background: '#4f46e5', border: '#3730a3' },
          shape: 'ellipse',
          level: 1,
          margin: 10,
          font: { color: '#ffffff', size: 12 },
          nodeType: 'Student',
          details: peer
        });
        addEdge({
          from: studentId,
          to: peerId,
          label: `SIMILAR_PROFILE (Δ ${peer.cgpaDifference})`,
          dashes: true,
          color: { color: '#6366f1' },
          font: edgeBaseFont,
        });

        if (peer.companyName) {
          const compNodeId = `PEER_COM_${peer.companyName.replace(/\s+/g, '_')}`;
          addNode({
            id: compNodeId,
            label: `Placed Company\n${peer.companyName}\n${peer.jobTitle} • ${peer.ctcLpa} LPA`,
            color: { background: '#059669', border: '#047857' },
            shape: 'box',
            level: 2,
            margin: 12,
            font: { color: '#ffffff', bold: true, size: 12 },
            nodeType: 'Company'
          });
          addEdge({
            from: peerId,
            to: compNodeId,
            label: 'PLACED_AT',
            arrows: 'to',
            color: { color: '#059669' },
            font: edgeBaseFont,
          });
        }
      });
    }

    setGraphMetrics({
      nodeCount: nodes.length,
      edgeCount: edges.length,
      typeCounts
    });

    const data = { nodes, edges };

    // Hierarchical Directed Layout with spacious non-overlapping gaps
    const options = {
      layout: {
        hierarchical: {
          enabled: true,
          direction: 'LR', // Left-to-Right placement pipeline
          sortMethod: 'directed',
          levelSeparation: 420, // Generous 420px horizontal distance for full label readability
          nodeSpacing: 220,    // Generous 220px vertical distance between parallel branches
          treeSpacing: 260,    // Clear separation between distinct subtrees
          blockShifting: true,
          edgeMinimization: true,
          parentCentralization: true,
        }
      },
      physics: {
        enabled: false, // Turn off continuous physics to eliminate jitter & preserve stable alignment
      },
      edges: {
        smooth: {
          type: 'cubicBezier',
          forceDirection: 'horizontal',
          roundness: 0.25
        },
        arrows: {
          to: { enabled: true, scaleFactor: 0.85 }
        }
      },
      interaction: {
        hover: true,
        hoverConnectedEdges: true,
        navigationButtons: true,
        keyboard: true,
        zoomView: true,
        dragView: true,
      }
    };

    if (networkInstanceRef.current) {
      networkInstanceRef.current.destroy();
    }

    const network = new Network(containerRef.current, data, options);

    // Smoothly fit the graph with comfortable margins upon drawing
    network.once('afterDrawing', () => {
      network.fit({
        animation: {
          duration: 350,
          easingFunction: 'easeInOutQuad'
        }
      });
    });

    network.on('click', (params) => {
      if (params.nodes.length > 0) {
        const nodeId = params.nodes[0];
        const found = nodes.find((n) => n.id === nodeId);
        setSelectedNodeInfo(found || null);
      } else {
        setSelectedNodeInfo(null);
      }
    });

    networkInstanceRef.current = network;
  };

  const handleTabChange = (newTab) => {
    setActiveTab(newTab);
    renderNetwork(newTab, {
      pathData: placementPathData,
      eligibleData: eligibleDrivesData,
      recsData: recommendationsData,
    }, visibleTypes);
  };

  const handleTypeToggle = (typeKey) => {
    const updated = { ...visibleTypes, [typeKey]: !visibleTypes[typeKey] };
    setVisibleTypes(updated);
    renderNetwork(activeTab, {
      pathData: placementPathData,
      eligibleData: eligibleDrivesData,
      recsData: recommendationsData,
    }, updated);
  };

  // Zoom & Camera Controls
  const handleZoomIn = () => {
    if (!networkInstanceRef.current) return;
    const scale = networkInstanceRef.current.getScale();
    networkInstanceRef.current.moveTo({ scale: scale * 1.25, animation: { duration: 300 } });
  };

  const handleZoomOut = () => {
    if (!networkInstanceRef.current) return;
    const scale = networkInstanceRef.current.getScale();
    networkInstanceRef.current.moveTo({ scale: scale * 0.8, animation: { duration: 300 } });
  };

  const handleFitScreen = () => {
    if (!networkInstanceRef.current) return;
    networkInstanceRef.current.fit({ animation: { duration: 400, easingFunction: 'easeInOutQuad' } });
  };

  const handleSearchNode = (e) => {
    e.preventDefault();
    if (!networkInstanceRef.current || !searchQuery.trim()) return;
    const query = searchQuery.trim().toLowerCase();

    // Search in placement data or current nodes
    const foundId = [studentId, ...placementPathData.map(p => p.applicationId), ...placementPathData.map(p => p.driveId)]
      .find(id => id && id.toLowerCase().includes(query));

    if (foundId) {
      networkInstanceRef.current.selectNodes([foundId]);
      networkInstanceRef.current.focus(foundId, {
        scale: 1.3,
        animation: { duration: 500, easingFunction: 'easeInOutQuad' },
      });
      showToast(`Located node [${foundId}] on canvas`, 'success');
    } else {
      showToast(`Node '${searchQuery}' not found in active graph view.`, 'error');
    }
  };

  // Admin Mutations
  const handleCreateNode = async (e) => {
    e.preventDefault();
    try {
      setMutationLoading(true);
      await api.createGraphEntity(newNodeLabel, newNodeProps);
      showToast(`Node of type [${newNodeLabel}] successfully registered into Neo4j!`, 'success');
      setShowAddModal(false);
      setNewNodeProps({ companyName: '', industry: '', email: '' });
      await loadGraphData();
    } catch (err) {
      showToast(err.message || 'Failed to create graph entity', 'error');
    } finally {
      setMutationLoading(false);
    }
  };

  const handleConnectNodes = async (e) => {
    e.preventDefault();
    if (!connectFrom.id || !connectTo.id) {
      showToast('Both source and target node IDs are required.', 'error');
      return;
    }
    try {
      setMutationLoading(true);
      await api.createGraphRelationship(connectFrom.label, connectFrom.id, connectTo.label, connectTo.id, connectRelType);
      showToast(`Relationship [${connectRelType}] established successfully!`, 'success');
      setShowConnectModal(false);
      await loadGraphData();
    } catch (err) {
      showToast(err.message || 'Failed to establish relationship', 'error');
    } finally {
      setMutationLoading(false);
    }
  };

  const handleDeleteNode = async (node) => {
    if (!window.confirm(`Are you sure you want to delete entity node [${node.id}] from the Neo4j graph? Active dependent references will be checked.`)) {
      return;
    }
    try {
      setMutationLoading(true);
      await api.deleteGraphEntity(node.nodeType, node.id);
      showToast(`Entity [${node.id}] removed from database.`, 'success');
      setSelectedNodeInfo(null);
      await loadGraphData();
    } catch (err) {
      showToast(err.message || 'Failed to delete node', 'error');
    } finally {
      setMutationLoading(false);
    }
  };

  const handleUpdateNode = async (e) => {
    e.preventDefault();
    if (!selectedNodeInfo) return;
    try {
      setMutationLoading(true);
      await api.updateGraphEntity(selectedNodeInfo.nodeType, selectedNodeInfo.id, editProps);
      showToast(`Properties for [${selectedNodeInfo.id}] successfully updated!`, 'success');
      setShowEditModal(false);
      await loadGraphData();
    } catch (err) {
      showToast(err.message || 'Failed to update node', 'error');
    } finally {
      setMutationLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Toast Notification */}
      {notification && (
        <div className={`p-4 rounded-xl flex items-center justify-between border shadow-sm ${
          notification.type === 'error'
            ? 'bg-rose-50 border-rose-200 text-rose-800'
            : 'bg-emerald-50 border-emerald-200 text-emerald-800'
        }`}>
          <div className="flex items-center gap-2">
            {notification.type === 'error' ? <AlertTriangle className="w-5 h-5 text-rose-600" /> : <CheckCircle className="w-5 h-5 text-emerald-600" />}
            <span className="text-xs font-semibold">{notification.message}</span>
          </div>
          <button onClick={() => setNotification(null)} className="p-1 hover:bg-slate-200/50 rounded-lg">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-lg bg-indigo-50 text-indigo-600">
              <Share2 className="w-6 h-6" />
            </span>
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Placement Graph Explorer
            </h1>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300">
              <Database className="w-3.5 h-3.5 mr-1" />
              Neo4j Native Traversal
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Real-time interactive multi-hop graph visualization showing candidates, applications, drives, interviews, and offers connected as first-class relationships.
          </p>
        </div>

        {/* Candidate Selector */}
        <div className="flex items-center gap-2 flex-wrap">
          <label className="text-xs font-medium text-slate-600">Candidate:</label>
          {allStudents.length > 0 ? (
            <select
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              className="px-3 py-1.5 text-xs font-mono rounded-lg border border-slate-300 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {allStudents.map((s) => (
                <option key={s.studentId} value={s.studentId}>
                  {s.studentId} — {s.name} ({s.cgpa} CGPA)
                </option>
              ))}
            </select>
          ) : (
            <div className="relative">
              <input
                type="text"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value.trim().toUpperCase())}
                placeholder="e.g. STU023"
                className="pl-8 pr-3 py-1.5 text-xs font-mono rounded-lg border border-slate-300 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
            </div>
          )}

          <button
            onClick={loadGraphData}
            disabled={loading}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 shadow-sm transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Admin Graph Mutation Toolbar (Only for Admins) */}
      {isAdmin && (
        <div className="bg-gradient-to-r from-indigo-900 to-slate-900 text-white p-3.5 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow-md border border-indigo-800">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-lg bg-indigo-500/20 text-indigo-300">
              <Database className="w-4 h-4" />
            </span>
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">
                Placement Admin Controls
              </span>
              <p className="text-[11px] text-slate-300">
                Create new nodes, establish graph relationships, or edit entity properties in Neo4j.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowAddModal(true)}
              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition-colors"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              Add Node
            </button>
            <button
              onClick={() => setShowConnectModal(true)}
              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition-colors"
            >
              <Link2 className="w-3.5 h-3.5" />
              Connect Nodes
            </button>
          </div>
        </div>
      )}

      {/* Traversal Perspective Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-2.5 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-1.5 flex-wrap">
          <button
            onClick={() => handleTabChange('path')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'path'
                ? 'bg-indigo-600 text-white shadow-sm font-semibold'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            Full Placement Journey (Student ➔ App ➔ Drive ➔ Offer)
          </button>
          <button
            onClick={() => handleTabChange('eligible')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'eligible'
                ? 'bg-indigo-600 text-white shadow-sm font-semibold'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Briefcase className="w-3.5 h-3.5" />
            Eligible Drives via Traversal
          </button>
          <button
            onClick={() => handleTabChange('peers')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'peers'
                ? 'bg-indigo-600 text-white shadow-sm font-semibold'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            Similar Placed Students (Peer Recs)
          </button>
        </div>

        <div className="text-xs text-slate-500 font-mono">
          Focus: <span className="font-semibold text-slate-800">{studentId}</span>
        </div>
      </div>

      {/* Main Visualization Canvas & Node Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Network Canvas (3 cols) */}
        <div className="lg:col-span-3 bg-white border border-slate-200 rounded-xl shadow-sm p-4 flex flex-col">
          {/* Canvas Sub-Header: Search & Type Filter Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3 mb-3">
            {/* Search within canvas */}
            <form onSubmit={handleSearchNode} className="flex items-center gap-1.5">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Find node by ID / Title..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-7 pr-2.5 py-1 text-xs rounded-md border border-slate-300 focus:outline-none focus:ring-1 focus:ring-indigo-500 w-44"
                />
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2 top-1.5" />
              </div>
              <button
                type="submit"
                className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium rounded-md"
              >
                Focus
              </button>
            </form>

            {/* Type Filter Pills */}
            <div className="flex items-center gap-1.5 flex-wrap text-[11px]">
              <span className="text-slate-400 mr-1 flex items-center gap-1 font-medium">
                <Filter className="w-3 h-3" /> Filter:
              </span>
              {[
                { label: 'Student', color: 'bg-blue-500' },
                { label: 'Application', color: 'bg-purple-500' },
                { label: 'Drive', color: 'bg-amber-500' },
                { label: 'Company', color: 'bg-emerald-600' },
                { label: 'Offer', color: 'bg-teal-500' },
                { label: 'Interview', color: 'bg-slate-600' },
              ].map(({ label, color }) => (
                <button
                  key={label}
                  onClick={() => handleTypeToggle(label)}
                  className={`px-2 py-0.5 rounded-full border text-[10px] font-semibold flex items-center gap-1 transition-all ${
                    visibleTypes[label]
                      ? 'bg-slate-50 border-slate-300 text-slate-800'
                      : 'bg-slate-100/50 border-transparent text-slate-400 opacity-60'
                  }`}
                >
                  <span className={`w-2 h-2 rounded-full ${color}`} />
                  {label}
                </button>
              ))}
            </div>

            {/* Canvas Zoom Controls */}
            <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg">
              <button
                onClick={handleZoomIn}
                title="Zoom In"
                className="p-1 hover:bg-white text-slate-600 rounded"
              >
                <ZoomIn className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={handleZoomOut}
                title="Zoom Out"
                className="p-1 hover:bg-white text-slate-600 rounded"
              >
                <ZoomOut className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={handleFitScreen}
                title="Fit to Screen"
                className="p-1 hover:bg-white text-slate-600 rounded"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Vis Network Container */}
          <div className="w-full h-[560px] bg-slate-900 rounded-lg border border-slate-800 relative overflow-hidden">
            {loading && (
              <div className="absolute inset-0 bg-slate-900/80 backdrop-blur-sm flex items-center justify-center z-10 pointer-events-none">
                <div className="flex flex-col items-center gap-2 text-white">
                  <RefreshCw className="w-7 h-7 animate-spin text-indigo-400" />
                  <span className="text-xs font-medium">Computing Directed Graph Traversal...</span>
                </div>
              </div>
            )}
            <div
              ref={containerRef}
              className="w-full h-full cursor-grab active:cursor-grabbing"
            />
          </div>

          <div className="mt-2.5 flex items-center justify-between text-[11px] text-slate-500">
            <span>
              Tip: Drag nodes to rearrange • Click any node to open properties • Scroll to zoom
            </span>
            <span className="font-mono text-[10px]">
              Engine: <strong className="text-slate-700">Neo4j 5.x Index-Free Adjacency</strong>
            </span>
          </div>
        </div>

        {/* Node Property Inspector (1 col) */}
        <div className="bg-white border border-slate-200 rounded-xl shadow-sm p-4 flex flex-col justify-between">
          <div>
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-600 border-b border-slate-100 pb-2 mb-3 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <Info className="w-4 h-4 text-slate-500" />
                Graph Entity Inspector
              </span>
              {selectedNodeInfo && (
                <button
                  onClick={() => setSelectedNodeInfo(null)}
                  className="text-slate-400 hover:text-slate-600 text-[10px]"
                >
                  Clear Selection
                </button>
              )}
            </h2>

            {selectedNodeInfo ? (
              <div className="space-y-4">
                <div className="p-3 rounded-lg bg-indigo-50 border border-indigo-100">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600">
                      {selectedNodeInfo.nodeType || 'Node Entity'}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] bg-indigo-200/60 font-mono text-indigo-900">
                      ID: {selectedNodeInfo.id}
                    </span>
                  </div>
                  <p className="text-sm font-semibold text-slate-900 mt-1">
                    {selectedNodeInfo.label?.split('\n')[1] || selectedNodeInfo.id}
                  </p>
                </div>

                {/* Properties Table */}
                {selectedNodeInfo.details && (
                  <div className="space-y-2">
                    <span className="text-xs font-semibold text-slate-700">Database Properties:</span>
                    <div className="bg-slate-50 rounded-lg p-3 border border-slate-200 space-y-2 text-xs font-mono max-h-72 overflow-y-auto">
                      {Object.entries(selectedNodeInfo.details).map(([key, val]) => (
                        <div key={key} className="flex flex-col border-b border-slate-200/60 pb-1.5 last:border-0">
                          <span className="text-slate-500 text-[10px] uppercase">{key}:</span>
                          <span className="text-slate-900 break-words font-medium">
                            {typeof val === 'object' ? JSON.stringify(val) : String(val)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Admin Actions for selected node */}
                {isAdmin && (
                  <div className="pt-2 border-t border-slate-100 flex items-center gap-2">
                    <button
                      onClick={() => {
                        setEditProps(selectedNodeInfo.details || {});
                        setShowEditModal(true);
                      }}
                      className="flex-1 py-1.5 px-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg text-xs font-semibold flex items-center justify-center gap-1 border border-indigo-200 transition-colors"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                      Edit Properties
                    </button>
                    <button
                      onClick={() => handleDeleteNode(selectedNodeInfo)}
                      className="py-1.5 px-3 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-semibold flex items-center justify-center gap-1 border border-rose-200 transition-colors"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Delete
                    </button>
                  </div>
                )}
              </div>
            ) : (
              /* Informative Graph Overview when no node is selected */
              <div className="space-y-4">
                <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <div className="flex items-center gap-2 text-slate-800 font-semibold text-xs">
                    <Sparkles className="w-4 h-4 text-indigo-600" />
                    Current Graph View Overview
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-center pt-1">
                    <div className="bg-white p-2 rounded-lg border border-slate-200 shadow-2xs">
                      <div className="text-lg font-bold text-slate-900">{graphMetrics.nodeCount}</div>
                      <div className="text-[10px] text-slate-500">Rendered Nodes</div>
                    </div>
                    <div className="bg-white p-2 rounded-lg border border-slate-200 shadow-2xs">
                      <div className="text-lg font-bold text-slate-900">{graphMetrics.edgeCount}</div>
                      <div className="text-[10px] text-slate-500">Connected Edges</div>
                    </div>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wider">
                    Node Distribution
                  </span>
                  <div className="space-y-1 text-xs">
                    {Object.entries(graphMetrics.typeCounts).map(([type, count]) => (
                      <div key={type} className="flex items-center justify-between py-1 px-2 rounded bg-slate-50 border border-slate-100">
                        <span className="text-slate-600 font-medium">{type}</span>
                        <span className="font-bold text-slate-900 font-mono">{count}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <p className="text-[11px] text-slate-400 text-center italic">
                  Select any node on the canvas to inspect its database attributes and relationships.
                </p>
              </div>
            )}
          </div>

          {/* Quick Stats Box */}
          <div className="pt-4 border-t border-slate-100 space-y-2 mt-4">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
              Neo4j Architecture
            </span>
            <div className="text-[11px] text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-200 space-y-1">
              <div className="flex items-center gap-1 text-emerald-700 font-medium">
                <CheckCircle className="w-3.5 h-3.5" />
                <span>Zero Join Table Overhead</span>
              </div>
              <p className="text-slate-500 text-[10px]">
                Relational multi-table joins replaced by index-free adjacency edge hops.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL 1: ADD GRAPH NODE (ADMIN ONLY) */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <PlusCircle className="w-5 h-5 text-emerald-600" />
                <h3 className="font-bold text-slate-900 text-base">Create Graph Entity Node</h3>
              </div>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateNode} className="space-y-4 text-xs">
              <div>
                <label className="font-semibold text-slate-700 block mb-1">Entity Label:</label>
                <select
                  value={newNodeLabel}
                  onChange={(e) => setNewNodeLabel(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium bg-white"
                >
                  <option value="Company">Company</option>
                  <option value="Student">Student</option>
                  <option value="Drive">Drive</option>
                  <option value="Application">Application</option>
                  <option value="Offer">Offer</option>
                </select>
              </div>

              {newNodeLabel === 'Company' && (
                <>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Company Name:</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Google India"
                      value={newNodeProps.companyName || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, companyName: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Industry:</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Technology / Cloud"
                      value={newNodeProps.industry || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, industry: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Corporate Email:</label>
                    <input
                      type="email"
                      required
                      placeholder="e.g. university_recruiting@google.com"
                      value={newNodeProps.email || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, email: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                </>
              )}

              {newNodeLabel === 'Student' && (
                <>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Student Full Name:</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. John Doe"
                      value={newNodeProps.name || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, name: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">CGPA (0.00 - 10.00):</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      placeholder="8.75"
                      value={newNodeProps.cgpa || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, cgpa: parseFloat(e.target.value) })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Academic Branch:</label>
                    <input
                      type="text"
                      required
                      placeholder="B.Tech CSE"
                      value={newNodeProps.branch || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, branch: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                </>
              )}

              {newNodeLabel === 'Drive' && (
                <>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Job Title:</label>
                    <input
                      type="text"
                      required
                      placeholder="Software Development Engineer"
                      value={newNodeProps.jobTitle || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, jobTitle: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Package CTC (LPA):</label>
                    <input
                      type="number"
                      step="0.5"
                      required
                      placeholder="22.5"
                      value={newNodeProps.ctc || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, ctc: parseFloat(e.target.value) })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                  <div>
                    <label className="font-semibold text-slate-700 block mb-1">Minimum CGPA:</label>
                    <input
                      type="number"
                      step="0.1"
                      required
                      placeholder="7.5"
                      value={newNodeProps.minCgpa || ''}
                      onChange={(e) => setNewNodeProps({ ...newNodeProps, minCgpa: parseFloat(e.target.value) })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 font-medium"
                    />
                  </div>
                </>
              )}

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3.5 py-2 rounded-lg text-slate-600 hover:bg-slate-100 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={mutationLoading}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-bold flex items-center gap-1.5 shadow"
                >
                  {mutationLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <PlusCircle className="w-3.5 h-3.5" />}
                  Register in Neo4j
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: CONNECT GRAPH NODES (ADMIN ONLY) */}
      {showConnectModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Link2 className="w-5 h-5 text-indigo-600" />
                <h3 className="font-bold text-slate-900 text-base">Connect Graph Nodes</h3>
              </div>
              <button onClick={() => setShowConnectModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleConnectNodes} className="space-y-4 text-xs">
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                <span className="font-bold text-slate-800 text-[11px] uppercase tracking-wider block">
                  Source Entity (From)
                </span>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-[10px] text-slate-500 uppercase block mb-1">Label:</label>
                    <select
                      value={connectFrom.label}
                      onChange={(e) => setConnectFrom({ ...connectFrom, label: e.target.value })}
                      className="w-full px-2 py-1.5 rounded border border-slate-300 bg-white"
                    >
                      <option value="Company">Company</option>
                      <option value="Student">Student</option>
                      <option value="Application">Application</option>
                      <option value="Drive">Drive</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[10px] text-slate-500 uppercase block mb-1">Entity ID:</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. COM004 / STU023"
                      value={connectFrom.id}
                      onChange={(e) => setConnectFrom({ ...connectFrom, id: e.target.value.trim() })}
                      className="w-full px-2 py-1.5 rounded border border-slate-300 font-mono"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="font-semibold text-slate-700 block mb-1">Allowlisted Relationship Type:</label>
                <select
                  value={connectRelType}
                  onChange={(e) => setConnectRelType(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-300 font-mono font-semibold bg-white"
                >
                  <option value="HOSTS_DRIVE">HOSTS_DRIVE (Company ➔ Drive)</option>
                  <option value="ELIGIBLE_FOR">ELIGIBLE_FOR (Drive ➔ Program)</option>
                  <option value="SUBMITTED_APPLICATION">SUBMITTED_APPLICATION (Student ➔ Application)</option>
                  <option value="FOR_DRIVE">FOR_DRIVE (Application ➔ Drive)</option>
                  <option value="HAS_INTERVIEW">HAS_INTERVIEW (Application ➔ Interview)</option>
                  <option value="RECEIVED_OFFER">RECEIVED_OFFER (Application ➔ Offer)</option>
                </select>
              </div>

              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                <span className="font-bold text-slate-800 text-[11px] uppercase tracking-wider block">
                  Target Entity (To)
                </span>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-[10px] text-slate-500 uppercase block mb-1">Label:</label>
                    <select
                      value={connectTo.label}
                      onChange={(e) => setConnectTo({ ...connectTo, label: e.target.value })}
                      className="w-full px-2 py-1.5 rounded border border-slate-300 bg-white"
                    >
                      <option value="Drive">Drive</option>
                      <option value="Program">Program</option>
                      <option value="Application">Application</option>
                      <option value="Offer">Offer</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[10px] text-slate-500 uppercase block mb-1">Entity ID:</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. DRV001 / BTECH-CSE"
                      value={connectTo.id}
                      onChange={(e) => setConnectTo({ ...connectTo, id: e.target.value.trim() })}
                      className="w-full px-2 py-1.5 rounded border border-slate-300 font-mono"
                    />
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowConnectModal(false)}
                  className="px-3.5 py-2 rounded-lg text-slate-600 hover:bg-slate-100 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={mutationLoading}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-bold flex items-center gap-1.5 shadow"
                >
                  {mutationLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Link2 className="w-3.5 h-3.5" />}
                  Establish Edge
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 3: EDIT NODE PROPERTIES (ADMIN ONLY) */}
      {showEditModal && selectedNodeInfo && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <Edit3 className="w-5 h-5 text-indigo-600" />
                <h3 className="font-bold text-slate-900 text-base">
                  Edit Node Properties: {selectedNodeInfo.id}
                </h3>
              </div>
              <button onClick={() => setShowEditModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleUpdateNode} className="space-y-3 text-xs">
              <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                {Object.entries(editProps).map(([key, val]) => (
                  <div key={key}>
                    <label className="font-semibold text-slate-700 uppercase text-[10px] block mb-0.5">
                      {key}:
                    </label>
                    <input
                      type="text"
                      value={typeof val === 'object' ? JSON.stringify(val) : String(val)}
                      onChange={(e) => setEditProps({ ...editProps, [key]: e.target.value })}
                      className="w-full px-3 py-1.5 rounded-lg border border-slate-300 font-mono text-xs"
                    />
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="px-3.5 py-2 rounded-lg text-slate-600 hover:bg-slate-100 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={mutationLoading}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-bold flex items-center gap-1.5 shadow"
                >
                  {mutationLoading ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle className="w-3.5 h-3.5" />}
                  Save Properties
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
