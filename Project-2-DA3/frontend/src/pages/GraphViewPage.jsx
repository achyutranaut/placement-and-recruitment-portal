/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
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
  CheckCircle,
  PlusCircle,
  Link2,
  Trash2,
  Edit3,
  ZoomIn,
  ZoomOut,
  Maximize2,
  AlertTriangle,
  X,
  ArrowRight,
  ArrowLeft,
  Copy,
  Check,
  Compass,
  Network as NetworkIcon,
  RotateCcw,
  Sliders,
} from 'lucide-react';

// Neo4j Aura Iconic Node Palettes
const NEO4J_NODE_THEMES = {
  Student: {
    name: 'Student',
    background: '#2563eb', // Vibrant Blue
    border: '#1d4ed8',
    highlight: '#3b82f6',
    hover: '#1d4ed8',
    ring: '#93c5fd',
    badgeBg: 'bg-blue-500/15',
    badgeText: 'text-blue-400',
    badgeBorder: 'border-blue-500/30',
  },
  Company: {
    name: 'Company',
    background: '#059669', // Emerald Green
    border: '#047857',
    highlight: '#10b981',
    hover: '#047857',
    ring: '#6ee7b7',
    badgeBg: 'bg-emerald-500/15',
    badgeText: 'text-emerald-400',
    badgeBorder: 'border-emerald-500/30',
  },
  Drive: {
    name: 'Drive',
    background: '#d97706', // Warm Amber
    border: '#b45309',
    highlight: '#f59e0b',
    hover: '#b45309',
    ring: '#fcd34d',
    badgeBg: 'bg-amber-500/15',
    badgeText: 'text-amber-400',
    badgeBorder: 'border-amber-500/30',
  },
  Application: {
    name: 'Application',
    background: '#7c3aed', // Royal Violet
    border: '#6d28d9',
    highlight: '#8b5cf6',
    hover: '#6d28d9',
    ring: '#c4b5fd',
    badgeBg: 'bg-purple-500/15',
    badgeText: 'text-purple-400',
    badgeBorder: 'border-purple-500/30',
  },
  Offer: {
    name: 'Offer',
    background: '#0d9488', // Teal Mint
    border: '#0f766e',
    highlight: '#14b8a6',
    hover: '#0f766e',
    ring: '#5eead4',
    badgeBg: 'bg-teal-500/15',
    badgeText: 'text-teal-400',
    badgeBorder: 'border-teal-500/30',
  },
  Interview: {
    name: 'Interview',
    background: '#e11d48', // Crimson Rose
    border: '#be123c',
    highlight: '#f43f5e',
    hover: '#be123c',
    ring: '#fda4af',
    badgeBg: 'bg-rose-500/15',
    badgeText: 'text-rose-400',
    badgeBorder: 'border-rose-500/30',
  },
  Program: {
    name: 'Program',
    background: '#4f46e5', // Deep Indigo
    border: '#4338ca',
    highlight: '#6366f1',
    hover: '#4338ca',
    ring: '#a5b4fc',
    badgeBg: 'bg-indigo-500/15',
    badgeText: 'text-indigo-400',
    badgeBorder: 'border-indigo-500/30',
  },
};

export default function GraphViewPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const containerRef = useRef(null);
  const networkInstanceRef = useRef(null);

  // Active Target Student
  const [studentId, setStudentId] = useState(
    user?.role === 'ROLE_STUDENT' ? (user.referenceId || 'STU001') : 'STU001'
  );
  const [allStudents, setAllStudents] = useState([]);

  // Data States
  const [loading, setLoading] = useState(false);
  const [graphFetchError, setGraphFetchError] = useState(null);
  const dataBundleRef = useRef({
    pathData: [],
    eligibleData: [],
    recsData: [],
    wholeData: null,
  });

  // Selection & Inspector States
  const [selectedNodeInfo, setSelectedNodeInfo] = useState(null);
  const [copiedId, setCopiedId] = useState(null);
  const [activeTab, setActiveTab] = useState('path'); // 'path', 'eligible', 'peers', 'whole'
  const [layoutMode, setLayoutMode] = useState('force'); // 'force' (Organic Neo4j) or 'hierarchy' (Left-to-Right Pipeline)
  const [backendInfo, setBackendInfo] = useState(null);
  const [auraStatus, setAuraStatus] = useState(null);
  const [syncingAura, setSyncingAura] = useState(false);
  const [syncStatus, setSyncStatus] = useState(null);

  // Search & Type Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [visibleTypes, setVisibleTypes] = useState({
    Student: true,
    Application: true,
    Drive: true,
    Company: true,
    Offer: true,
    Interview: true,
    Program: true,
  });

  // Current graph elements cache for relationship lookups & inspector
  const [allGraphNodes, setAllGraphNodes] = useState([]);
  const [allGraphEdges, setAllGraphEdges] = useState([]);
  const renderedNodesRef = useRef([]);
  const renderedEdgesRef = useRef([]);

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

  const showToast = useCallback((message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 5000);
  }, []);

  const loadBackendInfo = useCallback(async () => {
    try {
      const [backend, status] = await Promise.allSettled([
        api.getBackendInfo(),
        api.getGraphStatus(),
      ]);
      if (backend.status === 'fulfilled') setBackendInfo(backend.value);
      if (status.status === 'fulfilled') setAuraStatus(status.value);
    } catch (err) {
      console.error('Failed to load backend info:', err);
    }
  }, []);

  const loadStudentDirectory = useCallback(async () => {
    if (user?.role === 'ROLE_STUDENT') {
      const selfStudent = { studentId: user.referenceId || 'STU001', name: user.name || 'Student' };
      setAllStudents([selfStudent]);
      return;
    }
    try {
      const list = await api.getAllStudents();
      if (Array.isArray(list) && list.length > 0) {
        setAllStudents(list);
      }
    } catch (err) {
      console.warn('Could not load student directory:', err);
    }
  }, [user]);

  // Construct nodes and edges from backend data
  const renderNetwork = useCallback((
    tab = activeTab,
    dataBundle = null,
    typeFilter = visibleTypes,
    mode = layoutMode
  ) => {
    if (!containerRef.current) return;

    const effectiveBundle = dataBundle || dataBundleRef.current;
    const pathData = effectiveBundle?.pathData || [];
    const eligibleData = effectiveBundle?.eligibleData || [];
    const recsData = effectiveBundle?.recsData || [];
    const wholeData = effectiveBundle?.wholeData || null;

    const rawNodes = [];
    const rawEdges = [];
    const addedNodeIds = new Set();
    const addedEdgeKeys = new Set();
    const addedEdgeIds = new Set();
    const typeCounts = {};

    const addRawNode = (node) => {
      if (!node || !node.id) return;
      if (!addedNodeIds.has(node.id)) {
        addedNodeIds.add(node.id);
        rawNodes.push(node);
        typeCounts[node.nodeType] = (typeCounts[node.nodeType] || 0) + 1;
      }
    };

    const addRawEdge = (edge) => {
      if (!edge || !edge.from || !edge.to) return;

      // Semantic identity key prevents duplicate records from repetitive join rows
      const detailStr = edge.details
        ? Object.entries(edge.details).sort().map(([k, v]) => `${k}:${v}`).join(',')
        : '';
      const semanticKey = `${edge.from}->${edge.to}[:${edge.label || 'REL'}]{${detailStr}}`;
      if (addedEdgeKeys.has(semanticKey)) {
        // True duplicate relationship from flattened join rows, skip safely
        return;
      }
      addedEdgeKeys.add(semanticKey);

      // Generate a stable unique ID that distinguishes legitimate parallel relationships
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

    if (tab === 'path') {
      // 1. Target Student Node
      const studentLabel = pathData.length > 0 ? (pathData[0].studentName || studentId) : studentId;
      addRawNode({
        id: studentId,
        nodeType: 'Student',
        name: studentLabel,
        subtitle: studentId,
        details: {
          studentId: studentId,
          studentName: studentLabel,
          program: pathData[0]?.programName || 'Degree Program',
          totalApplications: pathData.length,
          offersCount: pathData.filter((p) => p.offerId).length,
        },
      });

      pathData.forEach((app) => {
        const appId = app.applicationId;
        const driveId = app.driveId;
        const compId = `COM_${app.companyName ? app.companyName.replace(/\s+/g, '_') : 'PARTNER'}`;

        // 2. Application Node
        addRawNode({
          id: appId,
          nodeType: 'Application',
          name: appId,
          subtitle: app.offerStatus || 'IN_PROGRESS',
          details: {
            applicationId: appId,
            offerStatus: app.offerStatus || 'IN_PROGRESS',
            jobTitle: app.jobTitle,
            companyName: app.companyName,
            packageLpa: app.ctcLpa > 0 ? `${app.ctcLpa} LPA` : 'Pending',
          },
        });

        addRawEdge({
          id: `${studentId}->${appId}`,
          from: studentId,
          to: appId,
          label: 'SUBMITTED',
        });

        // 3. Drive Node
        addRawNode({
          id: driveId,
          nodeType: 'Drive',
          name: app.jobTitle,
          subtitle: driveId,
          details: {
            driveId: driveId,
            jobTitle: app.jobTitle,
            companyName: app.companyName,
            packageLpa: app.ctcLpa > 0 ? `${app.ctcLpa} LPA` : 'TBD',
          },
        });

        addRawEdge({
          id: `${appId}->${driveId}`,
          from: appId,
          to: driveId,
          label: 'FOR_DRIVE',
        });

        // 4. Company Node
        addRawNode({
          id: compId,
          nodeType: 'Company',
          name: app.companyName || 'Corporate Partner',
          subtitle: 'Corporate Partner',
          details: {
            companyId: compId,
            companyName: app.companyName,
          },
        });

        addRawEdge({
          id: `${driveId}->${compId}`,
          from: driveId,
          to: compId,
          label: 'HOSTED_BY',
        });

        // 5. Interview Rounds
        if (Array.isArray(app.interviewSteps) && app.interviewSteps.length > 0) {
          app.interviewSteps.forEach((step, idx) => {
            const ivId = `${appId}_IV_${step.roundNo || (idx + 1)}_${idx}`;
            addRawNode({
              id: ivId,
              nodeType: 'Interview',
              name: `Round ${step.roundNo}`,
              subtitle: step.result || 'PENDING',
              details: {
                interviewId: ivId,
                roundNo: step.roundNo,
                interviewerName: step.interviewerName,
                result: step.result,
                score: step.score,
                applicationId: appId,
              },
            });

            addRawEdge({
              id: `${appId}->${ivId}`,
              from: appId,
              to: ivId,
              label: 'HAS_INTERVIEW',
            });
          });
        }

        // 6. Offer Node
        if (app.offerId) {
          const offerNodeId = app.offerId;
          addRawNode({
            id: offerNodeId,
            nodeType: 'Offer',
            name: `₹${app.ctcLpa} LPA`,
            subtitle: app.offerStatus || 'OFFERED',
            details: {
              offerId: app.offerId,
              ctcLpa: `${app.ctcLpa} LPA`,
              status: app.offerStatus || 'OFFERED',
              companyName: app.companyName,
              jobTitle: app.jobTitle,
            },
          });

          addRawEdge({
            id: `${appId}->${offerNodeId}`,
            from: appId,
            to: offerNodeId,
            label: 'RESULTED_IN',
          });
        }
      });
    } else if (tab === 'eligible') {
      addRawNode({
        id: studentId,
        nodeType: 'Student',
        name: studentId,
        subtitle: 'Target Student',
        details: {
          studentId,
          role: 'Candidate Eligibility Focus',
          totalEligibleDrives: eligibleData.length,
        },
      });

      eligibleData.forEach((drv) => {
        const driveId = drv.driveId;
        const compId = `COM_${drv.companyName ? drv.companyName.replace(/\s+/g, '_') : 'PARTNER'}`;

        addRawNode({
          id: driveId,
          nodeType: 'Drive',
          name: drv.jobTitle,
          subtitle: `₹${drv.ctcLpa} LPA`,
          details: {
            driveId: drv.driveId,
            jobTitle: drv.jobTitle,
            companyName: drv.companyName,
            minCgpa: drv.minCgpa,
            ctcLpa: `${drv.ctcLpa} LPA`,
            applicationDeadline: drv.applicationDeadline || 'Active',
            traversalPath: Array.isArray(drv.traversalPath) ? drv.traversalPath.join(' ➔ ') : null,
          },
        });

        addRawEdge({
          id: `${studentId}->${driveId}`,
          from: studentId,
          to: driveId,
          label: 'ELIGIBLE_FOR',
        });

        addRawNode({
          id: compId,
          nodeType: 'Company',
          name: drv.companyName || 'Corporate Partner',
          subtitle: 'Hiring Partner',
          details: {
            companyId: compId,
            companyName: drv.companyName,
          },
        });

        addRawEdge({
          id: `${compId}->${driveId}`,
          from: compId,
          to: driveId,
          label: 'HOSTS_DRIVE',
        });
      });
    } else if (tab === 'peers') {
      addRawNode({
        id: studentId,
        nodeType: 'Student',
        name: studentId,
        subtitle: 'Target Student',
        details: {
          studentId,
          role: 'Reference Peer Profile',
        },
      });

      recsData.forEach((peer) => {
        const peerId = peer.peerId || peer.studentId;
        const compId = peer.companyName ? `COM_${peer.companyName.replace(/\s+/g, '_')}` : null;

        addRawNode({
          id: peerId,
          nodeType: 'Student',
          name: peer.peerName || peer.studentName || peerId,
          subtitle: `CGPA ${peer.peerCgpa || peer.cgpa || 'N/A'}`,
          details: {
            studentId: peerId,
            name: peer.peerName || peer.studentName,
            cgpa: peer.peerCgpa || peer.cgpa,
            companyName: peer.companyName,
            jobTitle: peer.jobTitle,
            ctcLpa: peer.ctcLpa ? `${peer.ctcLpa} LPA` : null,
            cgpaDifference: peer.cgpaDiff !== undefined ? `Δ ${peer.cgpaDiff}` : null,
          },
        });

        addRawEdge({
          id: `${studentId}->${peerId}`,
          from: studentId,
          to: peerId,
          label: peer.cgpaDiff !== undefined ? `SIMILAR_PROFILE (Δ${peer.cgpaDiff})` : 'SIMILAR_PROFILE',
        });

        if (compId) {
          addRawNode({
            id: compId,
            nodeType: 'Company',
            name: peer.companyName,
            subtitle: peer.jobTitle || 'Recruiter',
            details: {
              companyId: compId,
              companyName: peer.companyName,
              jobTitle: peer.jobTitle,
              ctcLpa: peer.ctcLpa ? `${peer.ctcLpa} LPA` : null,
            },
          });

          addRawEdge({
            id: `${peerId}->${compId}`,
            from: peerId,
            to: compId,
            label: 'PLACED_AT',
          });
        }
      });
    } else if (tab === 'whole') {
      if (wholeData?.nodes && Array.isArray(wholeData.nodes)) {
        wholeData.nodes.forEach((n) => {
          const grp = n.group || 'Node';
          addRawNode({
            id: n.id,
            nodeType: grp,
            name: n.label || n.id,
            subtitle: n.properties?.studentId || n.properties?.companyId || n.properties?.driveId || grp,
            details: n.properties || {},
          });
        });
      }
      if (wholeData?.edges && Array.isArray(wholeData.edges)) {
        wholeData.edges.forEach((e) => {
          addRawEdge({
            id: e.id || `${e.from}->${e.to}`,
            from: e.from,
            to: e.to,
            label: e.label || 'CONNECTED_TO',
            details: e.properties || {},
          });
        });
      }
    }

    setAllGraphNodes(rawNodes);
    setAllGraphEdges(rawEdges);

    setGraphMetrics({
      nodeCount: rawNodes.length,
      edgeCount: rawEdges.length,
      typeCounts,
    });

    // Filter nodes by active entity type toggles
    const filteredNodes = rawNodes.filter((n) => typeFilter[n.nodeType] !== false);
    const visibleIds = new Set(filteredNodes.map((n) => n.id));

    // Format nodes to Neo4j Aura Visual Standard
    const visNodes = filteredNodes.map((node) => {
      const theme = NEO4J_NODE_THEMES[node.nodeType] || NEO4J_NODE_THEMES.Program;
      const displayName = node.name.length > 20 ? node.name.substring(0, 18) + '…' : node.name;
      const displaySub = node.subtitle
        ? (node.subtitle.length > 18 ? node.subtitle.substring(0, 16) + '…' : node.subtitle)
        : '';
      const labelText = displaySub && displaySub !== displayName ? `${displayName}\n${displaySub}` : displayName;

      return {
        id: node.id,
        label: labelText,
        shape: 'circle',
        margin: 12,
        borderWidth: 2.5,
        borderWidthSelected: 4.5,
        shadow: {
          enabled: true,
          color: 'rgba(0, 0, 0, 0.45)',
          size: 8,
          x: 2,
          y: 3,
        },
        color: {
          background: theme.background,
          border: theme.border,
          highlight: {
            background: theme.highlight,
            border: '#ffffff',
          },
          hover: {
            background: theme.highlight,
            border: '#ffffff',
          },
        },
        font: {
          color: '#ffffff',
          size: 11,
          face: 'Inter, system-ui, -apple-system, sans-serif',
          bold: true,
          align: 'center',
        },
        nodeType: node.nodeType,
        details: node.details,
        rawName: node.name,
      };
    });

    // Format thin, directional relationship edges with crisp badge labels
    const visEdges = rawEdges
      .filter((e) => visibleIds.has(e.from) && visibleIds.has(e.to))
      .map((edge) => ({
        id: edge.id,
        from: edge.from,
        to: edge.to,
        label: edge.label,
        arrows: {
          to: {
            enabled: true,
            scaleFactor: 0.65,
            type: 'arrow',
          },
        },
        color: {
          color: '#64748b',
          highlight: '#38bdf8',
          hover: '#94a3b8',
          opacity: 0.85,
        },
        width: 1.5,
        selectionWidth: 2.5,
        hoverWidth: 2,
        font: {
          size: 10,
          color: '#cbd5e1',
          background: '#1a1d24',
          strokeWidth: 0,
          strokeColor: 'transparent',
          align: 'middle',
          face: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
        },
        smooth:
          mode === 'hierarchy'
            ? {
                type: 'cubicBezier',
                forceDirection: 'horizontal',
                roundness: 0.25,
              }
            : {
                enabled: true,
                type: 'continuous',
                roundness: 0.2,
              },
      }));

    renderedNodesRef.current = visNodes;
    renderedEdgesRef.current = visEdges;

    const data = { nodes: visNodes, edges: visEdges };

    const options =
      mode === 'hierarchy'
        ? {
            layout: {
              hierarchical: {
                enabled: true,
                direction: 'LR',
                sortMethod: 'directed',
                levelSeparation: 320,
                nodeSpacing: 180,
                treeSpacing: 220,
                blockShifting: true,
                edgeMinimization: true,
                parentCentralization: true,
              },
            },
            physics: {
              enabled: false,
            },
            interaction: {
              hover: true,
              hoverConnectedEdges: true,
              zoomView: true,
              dragView: true,
              navigationButtons: false,
              keyboard: true,
              multiselect: false,
              tooltipDelay: 150,
            },
          }
        : {
            layout: {
              hierarchical: {
                enabled: false,
              },
              randomSeed: 42,
            },
            physics: {
              enabled: true,
              solver: 'forceAtlas2Based',
              forceAtlas2Based: {
                gravitationalConstant: -75,
                centralGravity: 0.015,
                springLength: 130,
                springConstant: 0.08,
                damping: 0.45,
                avoidOverlap: 0.75,
              },
              stabilization: {
                enabled: true,
                iterations: 180,
                updateInterval: 25,
              },
            },
            interaction: {
              hover: true,
              hoverConnectedEdges: true,
              zoomView: true,
              dragView: true,
              navigationButtons: false,
              keyboard: true,
              multiselect: false,
              tooltipDelay: 150,
            },
          };

    if (networkInstanceRef.current) {
      networkInstanceRef.current.destroy();
      networkInstanceRef.current = null;
    }
    if (containerRef.current) {
      containerRef.current.innerHTML = '';
    }

    try {
      const network = new Network(containerRef.current, data, options);

      network.once('afterDrawing', () => {
        network.fit({
          animation: {
            duration: 400,
            easingFunction: 'easeInOutQuad',
          },
        });
      });

      network.on('click', (params) => {
        if (params.nodes && params.nodes.length > 0) {
          const clickedId = params.nodes[0];
          const found = rawNodes.find((n) => n.id === clickedId);
          setSelectedNodeInfo(found || null);
        } else {
          setSelectedNodeInfo(null);
        }
      });

      networkInstanceRef.current = network;
    } catch (networkErr) {
      console.error('vis-network instantiation failure:', networkErr);
      setGraphFetchError(networkErr.message || 'Failed to initialize graph visualization.');
      showToast('Graph visualization error: ' + networkErr.message, 'error');
    }
  }, [activeTab, layoutMode, studentId, visibleTypes, showToast]);

  const loadGraphData = useCallback(async () => {
    if (!studentId && activeTab !== 'whole') return;
    try {
      setLoading(true);
      setGraphFetchError(null);
      const isUserAdmin = user?.role === 'ROLE_ADMIN';
      const [path, eligible, recs, whole] = await Promise.allSettled([
        studentId ? api.getPlacementPathGraph(studentId) : Promise.resolve([]),
        studentId ? api.getEligibleDrivesGraph(studentId) : Promise.resolve([]),
        studentId ? api.getRecommendationsGraph(studentId) : Promise.resolve([]),
        isUserAdmin ? api.getGraphElements(150) : Promise.resolve(null),
      ]);

      const pathData = path.status === 'fulfilled' && Array.isArray(path.value) ? path.value : [];
      const eligibleData = eligible.status === 'fulfilled' && Array.isArray(eligible.value) ? eligible.value : [];
      const recsData = recs.status === 'fulfilled' && Array.isArray(recs.value) ? recs.value : [];
      const wholeData = whole.status === 'fulfilled' ? whole.value : null;

      dataBundleRef.current = { pathData, eligibleData, recsData, wholeData };

      if (activeTab === 'whole') {
        if (!isUserAdmin) {
          setGraphFetchError('Access denied: Global graph topology is restricted to administrators (ROLE_ADMIN). Placement journeys and eligibility paths remain accessible via the tabs above.');
        } else if (whole.status === 'rejected') {
          const errMsg = whole.reason?.message || 'Access denied: Unable to retrieve global graph topology.';
          setGraphFetchError(errMsg);
          showToast(errMsg, 'error');
        }
      }

      renderNetwork(activeTab, { pathData, eligibleData, recsData, wholeData }, visibleTypes, layoutMode);
    } catch (err) {
      console.error('Error fetching graph data:', err);
      setGraphFetchError(err.message || 'Error fetching graph data');
      showToast('Error loading graph traversal: ' + err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [studentId, activeTab, layoutMode, visibleTypes, renderNetwork, showToast, user]);

  useEffect(() => {
    loadBackendInfo();
    loadStudentDirectory();
  }, [loadBackendInfo, loadStudentDirectory]);

  useEffect(() => {
    loadGraphData();
    return () => {
      if (networkInstanceRef.current) {
        networkInstanceRef.current.destroy();
        networkInstanceRef.current = null;
      }
    };
  }, [loadGraphData]);

  const handleSyncAura = async () => {
    try {
      setSyncingAura(true);
      setSyncStatus(null);
      const res = await api.syncGraphFromOracle();
      if (res?.success) {
        setSyncStatus({
          type: 'success',
          text: res.message || 'Successfully synchronized Oracle data to Neo4j Aura.',
        });
        showToast(res.message || 'Successfully synchronized Oracle data to Neo4j Aura.', 'success');
      } else {
        setSyncStatus({
          type: 'warning',
          text: res?.message || 'Synchronization completed with warnings or failed.',
        });
        showToast(res?.message || 'Sync failed.', 'error');
      }
      await loadBackendInfo();
      await loadGraphData();
    } catch (err) {
      setSyncStatus({
        type: 'error',
        text: err?.message || 'Failed to trigger Aura synchronization.',
      });
      showToast(err?.message || 'Failed to trigger Aura synchronization.', 'error');
    } finally {
      setSyncingAura(false);
    }
  };

  const [reconnecting, setReconnecting] = useState(false);
  const handleReconnect = async () => {
    try {
      setReconnecting(true);
      const res = await api.reconnectGraph();
      if (res?.connected || res?.state === 'CONNECTED') {
        showToast('Successfully connected to Neo4j Aura!', 'success');
        await loadBackendInfo();
        await loadGraphData();
      } else {
        showToast(res?.message || 'Connection attempt failed. Check credentials.', 'error');
        await loadBackendInfo();
      }
    } catch (err) {
      showToast('Reconnect failed: ' + err.message, 'error');
    } finally {
      setReconnecting(false);
    }
  };

  const handleTabChange = (newTab) => {
    setActiveTab(newTab);
    setSelectedNodeInfo(null);
    renderNetwork(
      newTab,
      dataBundleRef.current,
      visibleTypes,
      layoutMode
    );
  };

  const handleToggleLayout = (newMode) => {
    setLayoutMode(newMode);
    renderNetwork(activeTab, dataBundleRef.current, visibleTypes, newMode);
  };

  const handleTypeToggle = (typeKey) => {
    const updated = { ...visibleTypes, [typeKey]: !visibleTypes[typeKey] };
    setVisibleTypes(updated);
    renderNetwork(activeTab, dataBundleRef.current, updated, layoutMode);
  };

  // Zoom & Camera Controls
  const handleZoomIn = () => {
    if (!networkInstanceRef.current) return;
    const scale = networkInstanceRef.current.getScale();
    networkInstanceRef.current.moveTo({ scale: scale * 1.3, animation: { duration: 250 } });
  };

  const handleZoomOut = () => {
    if (!networkInstanceRef.current) return;
    const scale = networkInstanceRef.current.getScale();
    networkInstanceRef.current.moveTo({ scale: scale * 0.75, animation: { duration: 250 } });
  };

  const handleFitScreen = () => {
    if (!networkInstanceRef.current) return;
    networkInstanceRef.current.fit({
      animation: { duration: 400, easingFunction: 'easeInOutQuad' },
    });
  };

  const handleResetLayout = () => {
    if (!networkInstanceRef.current) return;
    if (layoutMode === 'force') {
      networkInstanceRef.current.setData({
        nodes: renderedNodesRef.current,
        edges: renderedEdgesRef.current,
      });
      networkInstanceRef.current.stabilize();
    }
    networkInstanceRef.current.fit({
      animation: { duration: 400, easingFunction: 'easeInOutQuad' },
    });
    showToast('Graph layout reset and centered to viewport', 'success');
  };

  const handleClearSelection = useCallback(() => {
    setSelectedNodeInfo(null);
    if (networkInstanceRef.current) {
      networkInstanceRef.current.unselectAll();
    }
  }, []);

  const handleSelectConnectedNode = useCallback((nodeId) => {
    const targetNode = allGraphNodes.find((n) => n.id === nodeId);
    if (!targetNode) return;
    setSelectedNodeInfo(targetNode);
    if (networkInstanceRef.current) {
      networkInstanceRef.current.selectNodes([nodeId]);
      networkInstanceRef.current.focus(nodeId, {
        scale: 1.25,
        animation: { duration: 400, easingFunction: 'easeInOutQuad' },
      });
    }
  }, [allGraphNodes]);

  const handleSearchNode = (e) => {
    if (e) e.preventDefault();
    if (!networkInstanceRef.current || !searchQuery.trim()) return;
    const query = searchQuery.trim().toLowerCase();

    const found = allGraphNodes.find((n) => {
      if (n.id && n.id.toLowerCase().includes(query)) return true;
      if (n.name && n.name.toLowerCase().includes(query)) return true;
      if (n.subtitle && n.subtitle.toLowerCase().includes(query)) return true;
      if (n.nodeType && n.nodeType.toLowerCase().includes(query)) return true;
      if (n.details) {
        return Object.values(n.details).some((val) =>
          val && String(val).toLowerCase().includes(query)
        );
      }
      return false;
    });

    if (found) {
      handleSelectConnectedNode(found.id);
      showToast(`Focused on [${found.nodeType}] ${found.name || found.id}`, 'success');
    } else {
      showToast(`No node found matching "${searchQuery}" in active graph.`, 'error');
    }
  };

  const handleCopyId = (id) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Compute incoming and outgoing relationships for the selected node
  const { incomingEdges, outgoingEdges } = useMemo(() => {
    if (!selectedNodeInfo) return { incomingEdges: [], outgoingEdges: [] };
    const selectedId = selectedNodeInfo.id;
    const incoming = allGraphEdges.filter((e) => e.to === selectedId);
    const outgoing = allGraphEdges.filter((e) => e.from === selectedId);
    return { incomingEdges: incoming, outgoingEdges: outgoing };
  }, [selectedNodeInfo, allGraphEdges]);

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
      await api.createGraphRelationship(
        connectFrom.label,
        connectFrom.id,
        connectTo.label,
        connectTo.id,
        connectRelType
      );
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
    if (
      !window.confirm(
        `Are you sure you want to delete entity node [${node.id}] from the Neo4j graph? Active dependent references will be checked.`
      )
    ) {
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

  // Property Formatters
  const formatPropertyKey = (key) => {
    const map = {
      studentId: 'Student ID',
      studentName: 'Student Name',
      name: 'Full Name',
      programName: 'Degree Program',
      programId: 'Program Code',
      applicationId: 'Application ID',
      offerStatus: 'Offer Status',
      driveId: 'Placement Drive ID',
      jobTitle: 'Job Role / Designation',
      companyName: 'Company Name',
      companyId: 'Company ID',
      packageLpa: 'Package CTC',
      ctcLpa: 'Package CTC',
      minCgpa: 'Minimum CGPA Cutoff',
      cgpa: 'Cumulative GPA',
      branch: 'Academic Branch',
      department: 'Faculty Department',
      placementStatus: 'Placement Status',
      interviewerName: 'Evaluator / Interviewer',
      roundNo: 'Interview Round #',
      result: 'Round Result',
      score: 'Evaluation Score',
      location: 'Job Location',
      industry: 'Industry Sector',
      email: 'Corporate Email',
      applyDate: 'Application Date',
      offerDate: 'Offer Letter Date',
      applicationDeadline: 'Registration Deadline',
      traversalPath: 'Graph Multi-Hop Path',
      cgpaDifference: 'CGPA Delta',
      totalApplications: 'Total Applications',
      offersCount: 'Total Offers Received',
      partnershipType: 'Partnership Level',
      role: 'Candidate Perspective',
      totalEligibleDrives: 'Eligible Drives Count',
    };
    if (map[key]) return map[key];
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
  };

  const formatPropertyValue = (key, value) => {
    if (value === null || value === undefined || value === '') {
      return <span className="text-slate-500 italic">Not specified</span>;
    }
    if (typeof value === 'boolean') {
      return value ? 'Yes' : 'No';
    }
    if (key === 'cgpa' || key === 'minCgpa') {
      return (
        <span className="font-semibold text-sky-400 font-mono">
          {Number(value).toFixed(2)} / 10.0
        </span>
      );
    }
    if (key === 'ctcLpa' || key === 'packageLpa') {
      const val = typeof value === 'number' ? value : parseFloat(value);
      return isNaN(val) ? (
        <span className="font-semibold text-emerald-400 font-mono">{String(value)}</span>
      ) : (
        <span className="px-2 py-0.5 rounded font-mono text-xs font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
          ₹{val.toFixed(1)} LPA
        </span>
      );
    }
    if (key === 'offerStatus' || key === 'status' || key === 'result' || key === 'placementStatus') {
      const s = String(value).toUpperCase();
      if (s === 'OFFERED' || s === 'CLEARED' || s === 'PASSED' || s === 'PLACED') {
        return (
          <span className="px-2 py-0.5 rounded font-semibold text-[11px] bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            {s}
          </span>
        );
      }
      if (s === 'REJECTED' || s === 'FAILED') {
        return (
          <span className="px-2 py-0.5 rounded font-semibold text-[11px] bg-rose-500/20 text-rose-400 border border-rose-500/30">
            {s}
          </span>
        );
      }
      return (
        <span className="px-2 py-0.5 rounded font-semibold text-[11px] bg-amber-500/20 text-amber-400 border border-amber-500/30">
          {s}
        </span>
      );
    }
    if (typeof value === 'object') {
      return <span className="font-mono text-xs">{JSON.stringify(value)}</span>;
    }
    return <span className="font-medium text-slate-200">{String(value)}</span>;
  };

  const selectedTheme = selectedNodeInfo
    ? NEO4J_NODE_THEMES[selectedNodeInfo.nodeType] || NEO4J_NODE_THEMES.Program
    : null;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Toast Notification */}
      {notification && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between border shadow-sm ${
            notification.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'error' ? (
              <AlertTriangle className="w-5 h-5 text-rose-600" />
            ) : (
              <CheckCircle className="w-5 h-5 text-emerald-600" />
            )}
            <span className="text-xs font-semibold">{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="p-1 hover:bg-slate-200/50 rounded-lg"
          >
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
            {auraStatus?.state === 'CONNECTED' || auraStatus?.connected ? (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-pulse"></span>
                Neo4j Aura Connected
              </span>
            ) : auraStatus?.state === 'NOT_CONNECTED' || auraStatus?.enabled ? (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-300">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5"></span>
                Aura Standby (Enabled, Not Connected)
              </span>
            ) : (
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-300">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-400 mr-1.5"></span>
                Neo4j Disabled by Config
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Real-time interactive multi-hop graph visualization showing candidates, applications, drives, interviews, and offers connected as first-class relationships.
          </p>
        </div>

        {/* Candidate Selector & Controls */}
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
                placeholder="e.g. STU001"
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

      {/* Synchronization Status Banner */}
      {syncStatus && (
        <div
          className={`flex items-center justify-between p-3.5 rounded-xl border text-xs font-medium shadow-xs ${
            syncStatus.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : syncStatus.type === 'warning'
              ? 'bg-amber-50 border-amber-200 text-amber-900'
              : 'bg-rose-50 border-rose-200 text-rose-800'
          }`}
        >
          <div className="flex items-center gap-2">
            {syncStatus.type === 'success' ? (
              <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
            )}
            <span>{syncStatus.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setSyncStatus(null)}
            className="text-[11px] underline font-semibold ml-4 hover:opacity-80"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Aura Disabled Banner */}
      {auraStatus?.state === 'DISABLED' && (
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between gap-3 text-xs text-slate-700">
          <div className="flex items-center gap-2">
            <Info className="w-4 h-4 text-slate-500 shrink-0" />
            <span>
              <strong>Neo4j Aura projection disabled:</strong> All placement operations, student profiles, applications, and recruiter evaluations are running directly on primary Oracle Database 23ai. Set <code className="bg-slate-200 px-1 py-0.5 rounded text-[11px]">PORTAL_NEO4J_ENABLED=true</code> with cloud credentials to enable real-time Aura graph visualization.
            </span>
          </div>
        </div>
      )}

      {/* Aura Enabled But Not Connected Banner */}
      {auraStatus?.state === 'NOT_CONNECTED' && (
        <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 flex items-center justify-between gap-3 text-xs text-amber-900">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
            <span>
              <strong>Neo4j Aura enabled but not connected:</strong> Cloud connection could not be established to <code className="bg-amber-100 px-1 py-0.5 rounded text-[11px]">{auraStatus?.uri || auraStatus?.details?.uri || 'neo4j+s://...'}</code>. {auraStatus?.message && <span className="text-amber-850">({auraStatus.message})</span>}
            </span>
          </div>
          {isAdmin && (
            <button
              onClick={handleReconnect}
              disabled={reconnecting}
              className="px-2.5 py-1 bg-amber-700 hover:bg-amber-800 text-white rounded text-xs font-semibold flex items-center gap-1 shrink-0 disabled:opacity-50"
            >
              <RefreshCw className={`w-3 h-3 ${reconnecting ? 'animate-spin' : ''}`} />
              {reconnecting ? 'Connecting...' : 'Retry Connection'}
            </button>
          )}
        </div>
      )}

      {/* Admin Graph Controls Toolbar (Only for Admins) */}
      {isAdmin && (
        <div className="bg-gradient-to-r from-indigo-900 to-slate-900 text-white p-3.5 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow-md border border-indigo-800">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-lg bg-indigo-500/20 text-indigo-300">
              <Database className="w-4 h-4" />
            </span>
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">
                Aura Graph Projection Controls
              </span>
              <p className="text-[11px] text-slate-300">
                Project authoritative Oracle transactional data into Neo4j Aura cloud graph nodes and edges.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleSyncAura}
              disabled={syncingAura}
              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition-colors disabled:opacity-50"
              title="Synchronize Oracle transactional tables to Neo4j Aura"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${syncingAura ? 'animate-spin' : ''}`} />
              {syncingAura ? 'Syncing...' : 'Sync Oracle to Aura'}
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition-colors"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              Add Node
            </button>
            <button
              onClick={() => setShowConnectModal(true)}
              className="px-3 py-1.5 bg-indigo-700 hover:bg-indigo-600 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition-colors"
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
          <button
            onClick={() => handleTabChange('whole')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5 ${
              activeTab === 'whole'
                ? 'bg-indigo-600 text-white shadow-sm font-semibold'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            <NetworkIcon className="w-3.5 h-3.5" />
            Aura Whole Graph Topology
          </button>
        </div>

        <div className="text-xs text-slate-500 font-mono">
          Focus: <span className="font-semibold text-slate-800">{activeTab === 'whole' ? 'Entire Graph' : studentId}</span>
        </div>
      </div>

      {/* Main Visualization Canvas & Neo4j Aura Node Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Network Canvas (3 cols) */}
        <div className="lg:col-span-3 flex flex-col space-y-3">
          {/* Neo4j Aura Dark Canvas Container */}
          <div className="relative w-full h-[620px] bg-[#181b20] rounded-2xl border border-slate-800 shadow-2xl overflow-hidden flex flex-col">
            {/* Subtle dot matrix grid background overlay */}
            <div
              className="absolute inset-0 pointer-events-none opacity-40 z-0"
              style={{
                backgroundImage: 'radial-gradient(rgba(255, 255, 255, 0.12) 1px, transparent 1px)',
                backgroundSize: '24px 24px',
              }}
            />

            {/* Floating Top Controls Dock */}
            <div className="relative z-10 m-3 p-2 rounded-xl bg-slate-900/85 backdrop-blur-md border border-slate-700/60 flex flex-wrap items-center justify-between gap-2 shadow-lg">
              {/* Search Bar */}
              <form onSubmit={handleSearchNode} className="flex items-center gap-1.5">
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Search node ID or title..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-7 pr-7 py-1 text-xs rounded-lg border border-slate-700 bg-slate-800/90 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500 w-48 sm:w-56 font-mono"
                  />
                  <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2 top-2" />
                  {searchQuery && (
                    <button
                      type="button"
                      onClick={() => setSearchQuery('')}
                      className="absolute right-2 top-1.5 text-slate-400 hover:text-slate-200"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
                <button
                  type="submit"
                  className="px-2.5 py-1 bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold rounded-lg shadow-sm transition-colors"
                >
                  Find
                </button>
              </form>

              {/* Entity Type Filter Pills */}
              <div className="flex items-center gap-1 flex-wrap text-[11px]">
                <span className="text-slate-400 text-[10px] font-semibold uppercase tracking-wider mr-1 hidden xl:inline">
                  Filters:
                </span>
                {Object.entries(NEO4J_NODE_THEMES).map(([typeKey, theme]) => {
                  const isVisible = visibleTypes[typeKey] !== false;
                  const count = graphMetrics.typeCounts[typeKey] || 0;
                  return (
                    <button
                      key={typeKey}
                      type="button"
                      onClick={() => handleTypeToggle(typeKey)}
                      className={`px-2 py-0.5 rounded-full text-[10px] font-semibold flex items-center gap-1.5 transition-all border ${
                        isVisible
                          ? `${theme.badgeBg} ${theme.badgeText} ${theme.badgeBorder}`
                          : 'bg-slate-800/60 border-slate-700/50 text-slate-500 opacity-50'
                      }`}
                      title={`Toggle ${theme.name} nodes (Count: ${count})`}
                    >
                      <span
                        className="w-2 h-2 rounded-full"
                        style={{ backgroundColor: theme.background }}
                      />
                      <span>{theme.name}</span>
                      <span className="font-mono text-[9px] opacity-75">({count})</span>
                    </button>
                  );
                })}
              </div>

              {/* Canvas Controls: Layout Toggle, Zoom & Viewport */}
              <div className="flex items-center gap-1 bg-slate-800/90 p-0.5 rounded-lg border border-slate-700/60">
                {/* Layout Mode Toggle */}
                <div className="flex items-center border-r border-slate-700 pr-1 mr-0.5">
                  <button
                    type="button"
                    onClick={() => handleToggleLayout('force')}
                    className={`px-2 py-1 rounded text-[10px] font-semibold flex items-center gap-1 transition-all ${
                      layoutMode === 'force'
                        ? 'bg-sky-600 text-white shadow-xs'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                    title="Neo4j Force-Directed Organic Layout"
                  >
                    <Compass className="w-3 h-3" />
                    <span className="hidden sm:inline">Organic</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleToggleLayout('hierarchy')}
                    className={`px-2 py-1 rounded text-[10px] font-semibold flex items-center gap-1 transition-all ${
                      layoutMode === 'hierarchy'
                        ? 'bg-sky-600 text-white shadow-xs'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                    title="Directed Pipeline Hierarchical Layout"
                  >
                    <Sliders className="w-3 h-3" />
                    <span className="hidden sm:inline">Pipeline</span>
                  </button>
                </div>

                <button
                  type="button"
                  onClick={handleZoomIn}
                  title="Zoom In"
                  className="p-1 hover:bg-slate-700 text-slate-300 hover:text-white rounded transition-colors"
                >
                  <ZoomIn className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={handleZoomOut}
                  title="Zoom Out"
                  className="p-1 hover:bg-slate-700 text-slate-300 hover:text-white rounded transition-colors"
                >
                  <ZoomOut className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={handleFitScreen}
                  title="Fit to Viewport"
                  className="p-1 hover:bg-slate-700 text-slate-300 hover:text-white rounded transition-colors"
                >
                  <Maximize2 className="w-3.5 h-3.5" />
                </button>
                <button
                  type="button"
                  onClick={handleResetLayout}
                  title="Reset Layout & Re-center"
                  className="p-1 hover:bg-slate-700 text-slate-300 hover:text-white rounded transition-colors"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                </button>
                {selectedNodeInfo && (
                  <button
                    type="button"
                    onClick={handleClearSelection}
                    title="Clear Selected Node"
                    className="p-1 hover:bg-rose-900/60 text-rose-400 hover:text-rose-200 rounded transition-colors border-l border-slate-700 ml-0.5 pl-1.5"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>

            {/* Loading Spinner Overlay */}
            {loading && (
              <div className="absolute inset-0 bg-slate-900/75 backdrop-blur-xs flex items-center justify-center z-20 pointer-events-none">
                <div className="flex flex-col items-center gap-3 text-white">
                  <RefreshCw className="w-8 h-8 animate-spin text-sky-400" />
                  <span className="text-xs font-semibold tracking-wide">
                    Traversing Neo4j Aura Graph Projections...
                  </span>
                </div>
              </div>
            )}

            {/* Empty / Error State Overlay */}
            {!loading && allGraphNodes.length === 0 && (
              <div className="absolute inset-0 flex flex-col items-center justify-center z-15 text-slate-400 p-6 text-center bg-slate-900/60 backdrop-blur-2xs">
                <div className="p-3.5 rounded-full bg-slate-800 border border-slate-700 mb-3 text-slate-400 shadow-md">
                  <NetworkIcon className="w-8 h-8 text-sky-400" />
                </div>
                <h3 className="text-sm font-bold text-slate-100">No Graph Projection Elements</h3>
                <p className="text-xs text-slate-400 max-w-md mt-1.5 leading-relaxed">
                  {graphFetchError ||
                    (activeTab === 'whole'
                      ? 'No graph elements returned from Neo4j Aura topology endpoint. Synchronize Oracle data to Aura above.'
                      : `No placement graph traversal records found for student ${studentId} in this perspective.`)}
                </p>
                <button
                  type="button"
                  onClick={loadGraphData}
                  className="mt-4 px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-sky-600 hover:bg-sky-500 text-white shadow-sm transition-colors flex items-center gap-1.5"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Retry Traversal
                </button>
              </div>
            )}

            {/* Vis Network Interactive Canvas */}
            <div
              ref={containerRef}
              className="relative z-10 w-full flex-1 cursor-grab active:cursor-grabbing"
            />

            {/* Canvas Bottom Status Bar */}
            <div className="relative z-10 bg-slate-900/85 backdrop-blur-md border-t border-slate-800/80 px-4 py-2 flex items-center justify-between text-[11px] text-slate-400">
              <div className="flex items-center gap-3">
                <span className="flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                  <span className="text-slate-300 font-medium">Interactive Graph Canvas</span>
                </span>
                <span className="hidden md:inline text-slate-500">•</span>
                <span className="hidden md:inline text-slate-400">
                  Drag nodes to rearrange • Click any node to inspect properties & connected edges
                </span>
              </div>
              <div className="flex items-center gap-3 font-mono text-[10px]">
                <span className="text-slate-400">
                  Layout: <strong className="text-slate-200">{layoutMode === 'force' ? 'Force-Directed (Organic)' : 'Directed Pipeline'}</strong>
                </span>
                <span className="text-slate-500">•</span>
                <span className="text-slate-400">
                  Database: <strong className="text-slate-200">{backendInfo?.databaseProduct || 'Oracle Database 23ai'}</strong>
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Neo4j Aura Right-Hand Inspector Panel (1 col) */}
        <div className="lg:col-span-1 bg-[#1a1d24] border border-slate-800 rounded-2xl shadow-xl p-4 flex flex-col justify-between max-h-[620px] overflow-hidden text-slate-200">
          <div className="flex-1 overflow-y-auto pr-1 space-y-4">
            {/* Inspector Header */}
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Info className="w-4 h-4 text-sky-400" />
                <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                  Graph Inspector
                </h2>
              </div>
              {selectedNodeInfo && (
                <button
                  type="button"
                  onClick={handleClearSelection}
                  className="text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1"
                >
                  <X className="w-3.5 h-3.5" />
                  <span>Clear</span>
                </button>
              )}
            </div>

            {selectedNodeInfo ? (
              <div className="space-y-4">
                {/* Node Identity Card */}
                <div
                  className={`p-3 rounded-xl border ${selectedTheme.badgeBg} ${selectedTheme.badgeBorder} space-y-2`}
                >
                  <div className="flex items-center justify-between">
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${selectedTheme.badgeBg} ${selectedTheme.badgeText} border ${selectedTheme.badgeBorder}`}
                    >
                      {selectedNodeInfo.nodeType}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopyId(selectedNodeInfo.id)}
                      className="flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded bg-slate-900/60 hover:bg-slate-900 text-slate-300 transition-colors"
                      title="Copy entity ID"
                    >
                      {copiedId === selectedNodeInfo.id ? (
                        <>
                          <Check className="w-3 h-3 text-emerald-400" />
                          <span className="text-emerald-400 font-semibold">Copied!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3 h-3" />
                          <span>ID: {selectedNodeInfo.id}</span>
                        </>
                      )}
                    </button>
                  </div>
                  <h3 className="text-sm font-bold text-white tracking-tight break-words">
                    {selectedNodeInfo.rawName || selectedNodeInfo.name || selectedNodeInfo.id}
                  </h3>
                  {selectedNodeInfo.subtitle && selectedNodeInfo.subtitle !== selectedNodeInfo.rawName && (
                    <p className="text-xs text-slate-400">{selectedNodeInfo.subtitle}</p>
                  )}
                </div>

                {/* Database Properties Section */}
                {selectedNodeInfo.details && Object.keys(selectedNodeInfo.details).length > 0 && (
                  <div className="space-y-2">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                      <span>Database Properties</span>
                      <span className="text-[10px] font-mono text-slate-500">
                        {Object.keys(selectedNodeInfo.details).length} fields
                      </span>
                    </span>
                    <div className="bg-slate-900/80 rounded-xl p-3 border border-slate-800 space-y-2 text-xs font-mono max-h-56 overflow-y-auto">
                      {Object.entries(selectedNodeInfo.details).map(([key, val]) => (
                        <div
                          key={key}
                          className="flex flex-col border-b border-slate-800/80 pb-2 last:border-0 last:pb-0"
                        >
                          <span className="text-slate-400 text-[10px] uppercase font-semibold">
                            {formatPropertyKey(key)}:
                          </span>
                          <span className="mt-0.5 break-words">
                            {formatPropertyValue(key, val)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Outgoing Relationships Section */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                      <ArrowRight className="w-3 h-3 text-sky-500" />
                      Outgoing Relationships ({outgoingEdges.length})
                    </span>
                  </div>

                  {outgoingEdges.length === 0 ? (
                    <p className="text-[11px] text-slate-500 italic p-2 rounded-lg bg-slate-900/40 border border-slate-800">
                      No outgoing edges (terminal node in current view).
                    </p>
                  ) : (
                    <div className="space-y-1.5 max-h-48 overflow-y-auto pr-0.5">
                      {outgoingEdges.map((edge, idx) => {
                        const targetNode = allGraphNodes.find((n) => n.id === edge.to);
                        const targetTheme = targetNode
                          ? NEO4J_NODE_THEMES[targetNode.nodeType] || NEO4J_NODE_THEMES.Program
                          : NEO4J_NODE_THEMES.Program;
                        return (
                          <div
                            key={edge.id || `${edge.from}-${edge.to}-${idx}`}
                            onClick={() => handleSelectConnectedNode(edge.to)}
                            className="group p-2.5 rounded-lg bg-slate-900/60 hover:bg-slate-900 border border-slate-800 hover:border-sky-500/50 cursor-pointer transition-all space-y-1.5"
                          >
                            <div className="flex items-center justify-between text-[10px]">
                              <span className="font-mono font-bold text-sky-400 flex items-center gap-1">
                                <ArrowRight className="w-3 h-3 text-sky-500" />
                                [:{edge.label}]
                              </span>
                              <span className="text-[10px] text-slate-400 group-hover:text-sky-300 flex items-center gap-0.5">
                                Explore <ChevronRight className="w-2.5 h-2.5" />
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span
                                className="w-2.5 h-2.5 rounded-full shrink-0"
                                style={{ backgroundColor: targetTheme.background }}
                              />
                              <div className="min-w-0 flex-1">
                                <div className="text-xs font-semibold text-slate-200 truncate group-hover:text-white">
                                  {targetNode?.rawName || targetNode?.name || edge.to}
                                </div>
                                <div className="text-[10px] font-mono text-slate-400 truncate">
                                  {targetNode?.nodeType || 'Node'} • {edge.to}
                                </div>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Incoming Relationships Section */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
                      <ArrowLeft className="w-3 h-3 text-amber-500" />
                      Incoming Relationships ({incomingEdges.length})
                    </span>
                  </div>

                  {incomingEdges.length === 0 ? (
                    <p className="text-[11px] text-slate-500 italic p-2 rounded-lg bg-slate-900/40 border border-slate-800">
                      No incoming edges (root node in current view).
                    </p>
                  ) : (
                    <div className="space-y-1.5 max-h-48 overflow-y-auto pr-0.5">
                      {incomingEdges.map((edge, idx) => {
                        const sourceNode = allGraphNodes.find((n) => n.id === edge.from);
                        const sourceTheme = sourceNode
                          ? NEO4J_NODE_THEMES[sourceNode.nodeType] || NEO4J_NODE_THEMES.Program
                          : NEO4J_NODE_THEMES.Program;
                        return (
                          <div
                            key={edge.id || `${edge.from}-${edge.to}-${idx}`}
                            onClick={() => handleSelectConnectedNode(edge.from)}
                            className="group p-2.5 rounded-lg bg-slate-900/60 hover:bg-slate-900 border border-slate-800 hover:border-amber-500/50 cursor-pointer transition-all space-y-1.5"
                          >
                            <div className="flex items-center justify-between text-[10px]">
                              <span className="font-mono font-bold text-amber-400 flex items-center gap-1">
                                <ArrowLeft className="w-3 h-3 text-amber-500" />
                                [:{edge.label}]
                              </span>
                              <span className="text-[10px] text-slate-400 group-hover:text-amber-300 flex items-center gap-0.5">
                                Explore <ChevronRight className="w-2.5 h-2.5" />
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span
                                className="w-2.5 h-2.5 rounded-full shrink-0"
                                style={{ backgroundColor: sourceTheme.background }}
                              />
                              <div className="min-w-0 flex-1">
                                <div className="text-xs font-semibold text-slate-200 truncate group-hover:text-white">
                                  {sourceNode?.rawName || sourceNode?.name || edge.from}
                                </div>
                                <div className="text-[10px] font-mono text-slate-400 truncate">
                                  {sourceNode?.nodeType || 'Node'} • {edge.from}
                                </div>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Admin Actions for selected node */}
                {isAdmin && (
                  <div className="pt-2 border-t border-slate-800 flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setEditProps(selectedNodeInfo.details || {});
                        setShowEditModal(true);
                      }}
                      className="flex-1 py-1.5 px-2 bg-indigo-950/60 hover:bg-indigo-900/80 text-indigo-300 rounded-lg text-xs font-semibold flex items-center justify-center gap-1 border border-indigo-800 transition-colors"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                      Edit Properties
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDeleteNode(selectedNodeInfo)}
                      className="py-1.5 px-3 bg-rose-950/60 hover:bg-rose-900/80 text-rose-300 rounded-lg text-xs font-semibold flex items-center justify-center gap-1 border border-rose-800 transition-colors"
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
                {/* Topology Stats Card */}
                <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" />
                      Graph Topology Metrics
                    </span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                      Live Aura
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-center">
                    <div className="p-2.5 rounded-lg bg-slate-800/80 border border-slate-700/60">
                      <div className="text-xl font-extrabold text-white font-mono">
                        {graphMetrics.nodeCount}
                      </div>
                      <div className="text-[10px] text-slate-400 uppercase tracking-wider">Nodes</div>
                    </div>
                    <div className="p-2.5 rounded-lg bg-slate-800/80 border border-slate-700/60">
                      <div className="text-xl font-extrabold text-white font-mono">
                        {graphMetrics.edgeCount}
                      </div>
                      <div className="text-[10px] text-slate-400 uppercase tracking-wider">
                        Relationships
                      </div>
                    </div>
                  </div>
                </div>

                {/* Node Entity Distribution */}
                <div className="space-y-2">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
                    Entity Types in View
                  </span>
                  <div className="space-y-1.5">
                    {Object.entries(graphMetrics.typeCounts).map(([type, count]) => {
                      const theme = NEO4J_NODE_THEMES[type] || NEO4J_NODE_THEMES.Program;
                      const isVisible = visibleTypes[type] !== false;
                      return (
                        <button
                          key={type}
                          type="button"
                          onClick={() => handleTypeToggle(type)}
                          className={`w-full flex items-center justify-between p-2 rounded-lg border text-xs transition-all ${
                            isVisible
                              ? 'bg-slate-900/60 border-slate-800 hover:border-slate-700 text-slate-200'
                              : 'bg-slate-950/40 border-slate-900 text-slate-600 opacity-60'
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <span
                              className="w-2.5 h-2.5 rounded-full"
                              style={{ backgroundColor: theme.background }}
                            />
                            <span className="font-semibold">{type}</span>
                          </div>
                          <span className="font-mono font-bold text-slate-300 text-[11px] bg-slate-800/80 px-2 py-0.5 rounded">
                            {count}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Dual-Engine Architecture Info */}
                <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800/80 space-y-1.5 text-[11px] text-slate-400">
                  <div className="flex items-center gap-1.5 text-emerald-400 font-semibold">
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>Neo4j Aura Engine Active</span>
                  </div>
                  <p>
                    Dual-engine coexistence: transactional source of truth in Oracle Database 23ai is projected into index-free adjacency graph nodes and edges.
                  </p>
                </div>

                {/* Exploration Hint */}
                <div className="p-3 rounded-lg bg-indigo-950/40 border border-indigo-900/50 text-[11px] text-indigo-300 flex items-start gap-2">
                  <Info className="w-4 h-4 shrink-0 mt-0.5 text-indigo-400" />
                  <span>
                    Click any circular node or relationship on the canvas to inspect its database properties and traverse incoming and outgoing relationships.
                  </span>
                </div>
              </div>
            )}
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
                      placeholder="e.g. COM004 / STU001"
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
