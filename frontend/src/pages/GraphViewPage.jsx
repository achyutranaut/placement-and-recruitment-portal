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
  Clock
} from 'lucide-react';

export default function GraphViewPage() {
  const { user } = useAuth();
  const containerRef = useRef(null);
  const networkInstanceRef = useRef(null);

  const [studentId, setStudentId] = useState(user?.role === 'ROLE_STUDENT' ? (user.referenceId || 'STU023') : 'STU023');
  const [loading, setLoading] = useState(false);
  const [placementPathData, setPlacementPathData] = useState([]);
  const [eligibleDrivesData, setEligibleDrivesData] = useState([]);
  const [recommendationsData, setRecommendationsData] = useState([]);
  const [selectedNodeInfo, setSelectedNodeInfo] = useState(null);
  const [activeTab, setActiveTab] = useState('path'); // 'path', 'eligible', 'peers'
  const [backendInfo, setBackendInfo] = useState(null);

  useEffect(() => {
    loadBackendInfo();
    loadGraphData();
  }, [studentId]);

  const loadBackendInfo = async () => {
    try {
      const data = await api.getBackendInfo();
      setBackendInfo(data);
    } catch (err) {
      console.error('Failed to load backend info:', err);
    }
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

      renderNetwork(activeTab, { pathData, eligibleData, recsData });
    } catch (err) {
      console.error('Error fetching graph data:', err);
    } finally {
      setLoading(false);
    }
  };

  const renderNetwork = (tab, dataBundle) => {
    if (!containerRef.current) return;

    const pathData = dataBundle?.pathData || placementPathData;
    const eligibleData = dataBundle?.eligibleData || eligibleDrivesData;
    const recsData = dataBundle?.recsData || recommendationsData;

    const nodes = [];
    const edges = [];
    const addedNodes = new Set();

    const addNode = (node) => {
      if (!addedNodes.has(node.id)) {
        addedNodes.add(node.id);
        nodes.push(node);
      }
    };

    if (tab === 'path') {
      // Student node
      const studentLabel = pathData.length > 0 ? pathData[0].studentName : studentId;
      addNode({
        id: studentId,
        label: `Student: ${studentLabel}\n(${studentId})`,
        color: { background: '#3b82f6', border: '#1d4ed8' },
        shape: 'box',
        font: { color: '#ffffff', bold: true },
        nodeType: 'Student',
        details: { id: studentId, name: studentLabel }
      });

      pathData.forEach((app) => {
        // Application node
        const appId = app.applicationId;
        addNode({
          id: appId,
          label: `Application: ${appId}`,
          color: { background: '#8b5cf6', border: '#6d28d9' },
          shape: 'ellipse',
          font: { color: '#ffffff' },
          nodeType: 'Application',
          details: app
        });
        edges.push({
          from: studentId,
          to: appId,
          label: 'SUBMITTED',
          arrows: 'to',
          color: { color: '#94a3b8' }
        });

        // Drive node
        const driveId = app.driveId;
        addNode({
          id: driveId,
          label: `Drive: ${app.jobTitle}\n(${driveId})`,
          color: { background: '#f59e0b', border: '#d97706' },
          shape: 'box',
          font: { color: '#ffffff' },
          nodeType: 'Drive',
          details: { driveId, jobTitle: app.jobTitle, companyName: app.companyName }
        });
        edges.push({
          from: appId,
          to: driveId,
          label: 'FOR_DRIVE',
          arrows: 'to',
          color: { color: '#94a3b8' }
        });

        // Company node
        const compId = `COM_${app.companyName}`;
        addNode({
          id: compId,
          label: `Company: ${app.companyName}`,
          color: { background: '#10b981', border: '#059669' },
          shape: 'box',
          font: { color: '#ffffff', bold: true },
          nodeType: 'Company',
          details: { companyName: app.companyName }
        });
        edges.push({
          from: compId,
          to: driveId,
          label: 'HOSTS_DRIVE',
          arrows: 'to',
          color: { color: '#10b981' }
        });

        // Interviews
        if (app.interviewSteps) {
          app.interviewSteps.forEach((step, idx) => {
            const ivId = `${appId}_IV_${idx + 1}`;
            const resultColor = step.result === 'CLEARED' ? '#10b981' : (step.result === 'REJECTED' ? '#ef4444' : '#eab308');
            addNode({
              id: ivId,
              label: `Round ${step.roundNo}: ${step.interviewerName}\n[${step.result} - Score: ${step.score}]`,
              color: { background: resultColor, border: '#334155' },
              shape: 'ellipse',
              font: { color: '#ffffff' },
              nodeType: 'Interview',
              details: step
            });
            edges.push({
              from: appId,
              to: ivId,
              label: 'HAS_INTERVIEW',
              arrows: 'to',
              color: { color: '#cbd5e1' }
            });
          });
        }

        // Offer
        if (app.offerId) {
          const offerNodeId = app.offerId;
          addNode({
            id: offerNodeId,
            label: `Offer: ${app.offerId}\n[${app.ctcLpa} LPA - ${app.offerStatus}]`,
            color: { background: '#059669', border: '#047857' },
            shape: 'star',
            font: { color: '#ffffff', bold: true },
            nodeType: 'Offer',
            details: { offerId: app.offerId, ctc: app.ctcLpa, status: app.offerStatus }
          });
          edges.push({
            from: appId,
            to: offerNodeId,
            label: 'RESULTED_IN',
            arrows: 'to',
            color: { color: '#059669', width: 2 }
          });
        }
      });
    } else if (tab === 'eligible') {
      addNode({
        id: studentId,
        label: `Student: ${studentId}`,
        color: { background: '#3b82f6', border: '#1d4ed8' },
        shape: 'box',
        font: { color: '#ffffff', bold: true },
        nodeType: 'Student'
      });

      eligibleData.forEach((drv) => {
        addNode({
          id: drv.driveId,
          label: `${drv.jobTitle}\n(${drv.companyName})\nMin CGPA: ${drv.minCgpa} | CTC: ${drv.ctcLpa} LPA`,
          color: { background: '#f59e0b', border: '#b45309' },
          shape: 'box',
          font: { color: '#ffffff' },
          nodeType: 'Drive',
          details: drv
        });
        edges.push({
          from: studentId,
          to: drv.driveId,
          label: 'ELIGIBLE_FOR',
          arrows: 'to',
          color: { color: '#f59e0b', width: 2 }
        });
      });
    } else if (tab === 'peers') {
      addNode({
        id: studentId,
        label: `Target Student: ${studentId}`,
        color: { background: '#3b82f6', border: '#1d4ed8' },
        shape: 'box',
        font: { color: '#ffffff', bold: true },
        nodeType: 'Student'
      });

      recsData.forEach((peer) => {
        const peerId = peer.studentId;
        addNode({
          id: peerId,
          label: `Peer: ${peer.studentName}\nCGPA: ${peer.cgpa} (Δ ${peer.cgpaDifference})`,
          color: { background: '#6366f1', border: '#4338ca' },
          shape: 'ellipse',
          font: { color: '#ffffff' },
          nodeType: 'PeerStudent',
          details: peer
        });
        edges.push({
          from: studentId,
          to: peerId,
          label: 'SIMILAR_PROFILE',
          dashes: true,
          color: { color: '#818cf8' }
        });

        if (peer.companyName) {
          const compNodeId = `PEER_COM_${peer.companyName}`;
          addNode({
            id: compNodeId,
            label: `Placed: ${peer.companyName}\nRole: ${peer.jobTitle}\n${peer.ctcLpa} LPA`,
            color: { background: '#10b981', border: '#047857' },
            shape: 'box',
            font: { color: '#ffffff' },
            nodeType: 'Company'
          });
          edges.push({
            from: peerId,
            to: compNodeId,
            label: 'PLACED_AT',
            arrows: 'to',
            color: { color: '#10b981' }
          });
        }
      });
    }

    const data = { nodes, edges };
    const options = {
      physics: {
        stabilization: true,
        barnesHut: {
          gravitationalConstant: -3000,
          springLength: 120,
          springConstant: 0.04
        }
      },
      interaction: {
        hover: true,
        navigationButtons: true,
        keyboard: true
      }
    };

    if (networkInstanceRef.current) {
      networkInstanceRef.current.destroy();
    }

    const network = new Network(containerRef.current, data, options);
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
    });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
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

        {/* Candidate Selector for Admins */}
        <div className="flex items-center gap-2">
          <label className="text-xs font-medium text-slate-600">Student ID:</label>
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
          <button
            onClick={loadGraphData}
            disabled={loading}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 shadow-sm transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh Graph
          </button>
        </div>
      </div>

      {/* Traversal Perspective Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-2.5 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => handleTabChange('path')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'path'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            Full Placement Journey (Student → App → Drive → Offer)
          </button>
          <button
            onClick={() => handleTabChange('eligible')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'eligible'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Briefcase className="w-3.5 h-3.5" />
            Eligible Drives via Graph Traversal
          </button>
          <button
            onClick={() => handleTabChange('peers')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'peers'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            Similar Placed Students (Peer Recs)
          </button>
        </div>

        <div className="text-xs text-slate-500 font-mono">
          Active Student: <span className="font-semibold text-slate-800">{studentId}</span>
        </div>
      </div>

      {/* Main Visualization Canvas & Node Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Network Canvas (3 cols) */}
        <div className="lg:col-span-3 bg-white border border-slate-200 rounded-xl shadow-sm p-4 flex flex-col">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-3">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-600">
                Interactive Graph Network Canvas
              </h2>
            </div>
            <div className="flex items-center gap-3 text-[11px] text-slate-500">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-blue-500"></span> Student
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-purple-500"></span> Application
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-amber-500"></span> Drive
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-emerald-500"></span> Company / Offer
              </span>
            </div>
          </div>

          <div
            ref={containerRef}
            className="w-full h-[540px] bg-slate-900 rounded-lg border border-slate-800 relative cursor-grab active:cursor-grabbing"
          >
            {loading && (
              <div className="absolute inset-0 bg-slate-900/80 backdrop-blur-sm flex items-center justify-center z-10">
                <div className="flex flex-col items-center gap-2 text-white">
                  <RefreshCw className="w-7 h-7 animate-spin text-indigo-400" />
                  <span className="text-xs font-medium">Traversing Neo4j Graph Relationships...</span>
                </div>
              </div>
            )}
          </div>
          <p className="mt-2 text-[11px] text-slate-400 text-center">
            Tip: Scroll to zoom • Drag to pan • Click any node or relationship to inspect its graph properties.
          </p>
        </div>

        {/* Node Property Inspector (1 col) */}
        <div className="bg-white border border-slate-200 rounded-xl shadow-sm p-4 flex flex-col">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-600 border-b border-slate-100 pb-2 mb-3 flex items-center gap-1.5">
            <Info className="w-4 h-4 text-slate-500" />
            Graph Entity Inspector
          </h2>

          {selectedNodeInfo ? (
            <div className="space-y-4">
              <div className="p-3 rounded-lg bg-indigo-50 border border-indigo-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600">
                  {selectedNodeInfo.nodeType || 'Node Entity'}
                </span>
                <p className="text-sm font-semibold text-slate-900 mt-0.5">
                  {selectedNodeInfo.id}
                </p>
              </div>

              {selectedNodeInfo.details && (
                <div className="space-y-2">
                  <span className="text-xs font-semibold text-slate-700">Properties:</span>
                  <div className="bg-slate-50 rounded-lg p-3 border border-slate-200 space-y-1.5 text-xs font-mono max-h-80 overflow-y-auto">
                    {Object.entries(selectedNodeInfo.details).map(([key, val]) => (
                      <div key={key} className="flex flex-col border-b border-slate-200/60 pb-1 last:border-0">
                        <span className="text-slate-500 text-[10px] uppercase">{key}:</span>
                        <span className="text-slate-900 break-words">
                          {typeof val === 'object' ? JSON.stringify(val) : String(val)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="h-64 flex flex-col items-center justify-center text-center p-4 text-slate-400">
              <Share2 className="w-10 h-10 stroke-1 text-slate-300 mb-2" />
              <p className="text-xs font-medium">Select any node on the graph canvas to inspect its Neo4j properties.</p>
            </div>
          )}

          {/* Quick Stats Box */}
          <div className="mt-auto pt-4 border-t border-slate-100 space-y-2">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
              Graph Query Advantages
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
    </div>
  );
}
