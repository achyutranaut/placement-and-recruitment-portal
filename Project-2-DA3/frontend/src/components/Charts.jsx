import { useState, useMemo } from 'react';

// Reusable Metric Card (Phase 5.2)
export function MetricCard({ title, value, subtitle, icon: Icon, color = 'indigo', trend, note }) {
  const colorStyles = {
    indigo: 'bg-indigo-50 text-indigo-700 border-indigo-100',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    blue: 'bg-blue-50 text-blue-700 border-blue-100',
    purple: 'bg-purple-50 text-purple-700 border-purple-100',
    amber: 'bg-amber-50 text-amber-700 border-amber-100',
    rose: 'bg-rose-50 text-rose-700 border-rose-100',
    slate: 'bg-slate-50 text-slate-700 border-slate-200',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs hover:shadow-sm transition-all duration-150 flex flex-col justify-between">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
          {title}
        </span>
        {Icon && (
          <div className={`p-2 rounded-lg border ${colorStyles[color] || colorStyles.indigo}`}>
            <Icon className="w-4 h-4" />
          </div>
        )}
      </div>

      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 font-mono">
          {value !== null && value !== undefined ? value : <span className="text-slate-400 font-sans text-lg">N/A</span>}
        </span>
        {trend && (
          <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
            {trend}
          </span>
        )}
      </div>

      {(subtitle || note) && (
        <div className="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-[11px]">
          {subtitle && <p className="text-slate-500 font-medium truncate">{subtitle}</p>}
          {note && <span className="text-slate-400 font-mono text-[10px] shrink-0 ml-1">{note}</span>}
        </div>
      )}
    </div>
  );
}

// Custom Horizontal Bar Chart (Compensation Tier Distribution, Branch Distribution)
export function BarChart({ data = [], xKey = 'name', yKey = 'value', unit = '', emptyMessage = 'No placement records recorded' }) {
  const [hovered, setHovered] = useState(null);
  const maxValue = Math.max(...data.map((d) => Number(d[yKey]) || 0), 1);
  const total = data.reduce((sum, d) => sum + (Number(d[yKey]) || 0), 0);

  if (!data || data.length === 0 || total === 0) {
    return (
      <div className="w-full py-8 text-center text-xs text-slate-400 italic">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="w-full space-y-3.5">
      {data.map((item, idx) => {
        const val = Number(item[yKey]) || 0;
        const percentage = total > 0 ? Math.round((val / total) * 100) : 0;
        const barWidth = Math.max(Math.round((val / maxValue) * 100), val > 0 ? 3 : 0);
        const isHovered = hovered === idx;

        return (
          <div
            key={idx}
            className="group cursor-pointer"
            onMouseEnter={() => setHovered(idx)}
            onMouseLeave={() => setHovered(null)}
          >
            <div className="flex justify-between items-center text-xs mb-1">
              <span className="font-semibold text-slate-700 truncate max-w-[240px] group-hover:text-slate-900 transition-colors">
                {item[xKey]}
              </span>
              <div className="flex items-center gap-2 font-mono shrink-0">
                <span className="text-slate-400 text-[11px]">({percentage}%)</span>
                <span className="font-bold text-slate-900">
                  {val} {unit}
                </span>
              </div>
            </div>

            <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden flex">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  item.color || (idx % 3 === 0 ? 'bg-indigo-600' : idx % 3 === 1 ? 'bg-blue-600' : 'bg-emerald-600')
                } ${isHovered ? 'brightness-110 shadow-xs' : ''}`}
                style={{ width: `${barWidth}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}

const DEFAULT_DONUT_COLORS = [
  '#2563eb', // blue-600
  '#7c3aed', // violet-600
  '#059669', // emerald-600
  '#d97706', // amber-600
  '#0891b2', // cyan-600
  '#db2777', // pink-600
  '#4f46e5', // indigo-600
  '#e11d48', // rose-600
  '#64748b', // slate-500
];

function computeDonutSlices(data, total, circumference, defaultColors = DEFAULT_DONUT_COLORS) {
  const slices = [];
  let currentAngle = 0;
  const list = data || [];
  for (let idx = 0; idx < list.length; idx++) {
    const item = list[idx];
    const val = Number(item.value) || 0;
    if (val <= 0) continue;
    const sliceFraction = val / total;
    slices.push({
      item,
      idx,
      strokeDasharray: `${sliceFraction * circumference} ${circumference}`,
      strokeDashoffset: -currentAngle * circumference,
      color: item.color || defaultColors[idx % defaultColors.length],
    });
    currentAngle += sliceFraction;
  }
  return slices;
}

// Custom SVG Donut / Pie Chart (Phase 6.2 Application Pipeline & 6.3 Branch Share)
export function DonutChart({
  data = [],
  size = 200,
  centerLabel = 'Total',
  emptyMessage = 'No distribution data available',
}) {
  const [hoveredIdx, setHoveredIdx] = useState(null);
  const rawTotal = data.reduce((sum, item) => sum + (Number(item.value) || 0), 0);
  const total = rawTotal > 0 ? rawTotal : 1;
  const radius = size / 2 - 20;
  const circumference = 2 * Math.PI * radius;

  const slices = useMemo(
    () => computeDonutSlices(data, total, circumference, DEFAULT_DONUT_COLORS),
    [data, total, circumference]
  );

  if (!data || data.length === 0 || rawTotal === 0) {
    return (
      <div className="w-full py-10 flex flex-col items-center justify-center text-xs text-slate-400 italic">
        <div className="w-16 h-16 rounded-full border-4 border-dashed border-slate-200 mb-2"></div>
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="flex flex-col sm:flex-row items-center justify-center gap-6 w-full">
      {/* SVG Donut Canvas */}
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke="#f1f5f9"
            strokeWidth="24"
          />
          {slices.map((slice) => {
            const isHovered = hoveredIdx === slice.idx;

            return (
              <circle
                key={slice.idx}
                cx={size / 2}
                cy={size / 2}
                r={radius}
                fill="transparent"
                stroke={slice.color}
                strokeWidth={isHovered ? 28 : 24}
                strokeDasharray={slice.strokeDasharray}
                strokeDashoffset={slice.strokeDashoffset}
                onMouseEnter={() => setHoveredIdx(slice.idx)}
                onMouseLeave={() => setHoveredIdx(null)}
                className="transition-all duration-300 cursor-pointer"
                style={{ opacity: hoveredIdx !== null && !isHovered ? 0.6 : 1 }}
              />
            );
          })}
        </svg>

        {/* Center Readout */}
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          <span className="text-2xl font-extrabold text-slate-900 font-mono">
            {hoveredIdx !== null ? data[hoveredIdx].value : rawTotal}
          </span>
          <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
            {hoveredIdx !== null ? data[hoveredIdx].name : centerLabel}
          </span>
        </div>
      </div>

      {/* Interactive Legend with Percentage Badges */}
      <div className="flex flex-col gap-2 w-full max-w-xs max-h-56 overflow-y-auto pr-1">
        {data.map((item, idx) => {
          const val = Number(item.value) || 0;
          const pct = rawTotal > 0 ? Math.round((val / rawTotal) * 100) : 0;
          const isHovered = hoveredIdx === idx;

          return (
            <div
              key={idx}
              onMouseEnter={() => setHoveredIdx(idx)}
              onMouseLeave={() => setHoveredIdx(null)}
              className={`flex items-center justify-between text-xs p-1.5 rounded-md transition-colors cursor-pointer ${
                isHovered ? 'bg-slate-100/80 font-bold' : 'hover:bg-slate-50'
              }`}
            >
              <div className="flex items-center gap-2 min-w-0">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: item.color || DEFAULT_DONUT_COLORS[idx % DEFAULT_DONUT_COLORS.length] }}
                />
                <span className="text-slate-700 truncate font-medium">{item.name}</span>
              </div>

              <div className="flex items-center gap-2 font-mono shrink-0 ml-2">
                <span className="text-slate-400 text-[10px] font-semibold">{pct}%</span>
                <span className="font-bold text-slate-900">{val}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// Custom SVG Timeline Chart (Phase 6.5 Monthly & Cumulative Placement Timeline)
export function TimelineChart({
  timelineData = [],
  height = 240,
  emptyMessage = 'No historical monthly placement records recorded yet',
}) {
  const [hoveredPoint, setHoveredPoint] = useState(null);
  const [activeSeries, setActiveSeries] = useState('both'); // 'both', 'monthly', 'cumulative'

  if (!timelineData || timelineData.length === 0) {
    return (
      <div className="w-full py-12 flex flex-col items-center justify-center text-xs text-slate-400 italic">
        <svg className="w-12 h-12 text-slate-200 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" />
        </svg>
        {emptyMessage}
      </div>
    );
  }

  const width = 640;
  const paddingLeft = 45;
  const paddingRight = 30;
  const paddingTop = 25;
  const paddingBottom = 40;
  const chartWidth = width - paddingLeft - paddingRight;
  const chartHeight = height - paddingTop - paddingBottom;

  const maxVal = Math.max(
    ...timelineData.map((d) => Math.max(d.cumulativeOffers || 0, d.offersCount || 0)),
    5
  );

  const getX = (idx) => {
    if (timelineData.length === 1) return paddingLeft + chartWidth / 2;
    return paddingLeft + (idx / (timelineData.length - 1)) * chartWidth;
  };

  const getY = (val) => {
    return paddingTop + chartHeight - (val / maxVal) * chartHeight;
  };

  // Build SVG path strings
  const monthlyPoints = timelineData.map((d, i) => ({ x: getX(i), y: getY(d.offersCount || 0), data: d }));
  const cumulativePoints = timelineData.map((d, i) => ({ x: getX(i), y: getY(d.cumulativeOffers || 0), data: d }));

  const generateLinePath = (points) => {
    if (points.length === 0) return '';
    return points.reduce((acc, pt, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${pt.x} ${pt.y}`, '');
  };

  const generateAreaPath = (points) => {
    if (points.length === 0) return '';
    const line = generateLinePath(points);
    const bottom = paddingTop + chartHeight;
    return `${line} L ${points[points.length - 1].x} ${bottom} L ${points[0].x} ${bottom} Z`;
  };

  return (
    <div className="w-full space-y-3">
      {/* Series Filter Toggle Controls */}
      <div className="flex items-center justify-between text-xs pb-1">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setActiveSeries('both')}
            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${
              activeSeries === 'both' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All Trajectories
          </button>
          <button
            type="button"
            onClick={() => setActiveSeries('cumulative')}
            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${
              activeSeries === 'cumulative' ? 'bg-emerald-700 text-white' : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block"></span>
            Cumulative Placement Total
          </button>
          <button
            type="button"
            onClick={() => setActiveSeries('monthly')}
            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${
              activeSeries === 'monthly' ? 'bg-indigo-700 text-white' : 'bg-indigo-50 text-indigo-800 hover:bg-indigo-100'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-indigo-500 inline-block"></span>
            Monthly Offer Intake
          </button>
        </div>

        {hoveredPoint && (
          <div className="hidden sm:flex items-center gap-2 font-mono text-[11px] font-bold bg-slate-100 px-2 py-0.5 rounded border border-slate-200 text-slate-800">
            <span>{hoveredPoint.data.month}:</span>
            <span className="text-emerald-700">Cumul: {hoveredPoint.data.cumulativeOffers}</span>
            <span className="text-slate-300">•</span>
            <span className="text-indigo-700">Month: {hoveredPoint.data.offersCount}</span>
          </div>
        )}
      </div>

      {/* SVG Canvas */}
      <div className="relative w-full overflow-x-auto">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto overflow-visible"
          style={{ minWidth: 480 }}
        >
          <defs>
            <linearGradient id="cumulGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#10b981" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#10b981" stopOpacity="0.0" />
            </linearGradient>
            <linearGradient id="monthGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#6366f1" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#6366f1" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((ratio, i) => {
            const y = paddingTop + chartHeight * (1 - ratio);
            const valLabel = Math.round(maxVal * ratio);
            return (
              <g key={i}>
                <line
                  x1={paddingLeft}
                  y1={y}
                  x2={width - paddingRight}
                  y2={y}
                  stroke="#f1f5f9"
                  strokeWidth="1"
                />
                <text
                  x={paddingLeft - 8}
                  y={y + 3}
                  textAnchor="end"
                  fill="#94a3b8"
                  fontSize="10"
                  fontFamily="monospace"
                >
                  {valLabel}
                </text>
              </g>
            );
          })}

          {/* Cumulative Area & Line */}
          {(activeSeries === 'both' || activeSeries === 'cumulative') && (
            <>
              <path d={generateAreaPath(cumulativePoints)} fill="url(#cumulGrad)" />
              <path
                d={generateLinePath(cumulativePoints)}
                fill="none"
                stroke="#10b981"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              {cumulativePoints.map((pt, i) => (
                <circle
                  key={`cumul-${i}`}
                  cx={pt.x}
                  cy={pt.y}
                  r={hoveredPoint?.idx === i ? 5 : 3.5}
                  fill="#ffffff"
                  stroke="#10b981"
                  strokeWidth="2.5"
                  onMouseEnter={() => setHoveredPoint({ idx: i, data: pt.data })}
                  onMouseLeave={() => setHoveredPoint(null)}
                  className="cursor-pointer transition-all"
                />
              ))}
            </>
          )}

          {/* Monthly Area & Line */}
          {(activeSeries === 'both' || activeSeries === 'monthly') && (
            <>
              <path d={generateAreaPath(monthlyPoints)} fill="url(#monthGrad)" />
              <path
                d={generateLinePath(monthlyPoints)}
                fill="none"
                stroke="#6366f1"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              {monthlyPoints.map((pt, i) => (
                <circle
                  key={`month-${i}`}
                  cx={pt.x}
                  cy={pt.y}
                  r={hoveredPoint?.idx === i ? 5 : 3.5}
                  fill="#ffffff"
                  stroke="#6366f1"
                  strokeWidth="2.5"
                  onMouseEnter={() => setHoveredPoint({ idx: i, data: pt.data })}
                  onMouseLeave={() => setHoveredPoint(null)}
                  className="cursor-pointer transition-all"
                />
              ))}
            </>
          )}

          {/* X-Axis labels */}
          {timelineData.map((d, i) => (
            <text
              key={i}
              x={getX(i)}
              y={height - paddingBottom + 18}
              textAnchor="middle"
              fill="#64748b"
              fontSize="10"
              fontWeight="600"
            >
              {d.month}
            </text>
          ))}
        </svg>
      </div>
    </div>
  );
}

// Company Placement Performance Comparison Table/Bar Component (Phase 6.4)
export function CompanyPerformanceAnalytics({ companies = [] }) {
  const [sortBy, setSortBy] = useState('offers'); // 'offers', 'rate', 'ctc'

  if (!companies || companies.length === 0) {
    return (
      <div className="py-8 text-center text-xs text-slate-400 italic">
        No corporate recruitment partner statistics available yet.
      </div>
    );
  }

  const sorted = [...companies].sort((a, b) => {
    if (sortBy === 'rate') return b.conversionRate - a.conversionRate;
    if (sortBy === 'ctc') return b.averageCtc - a.averageCtc;
    return b.offersReleased - a.offersReleased;
  });

  const maxOffers = Math.max(...companies.map((c) => c.offersReleased || 0), 1);

  return (
    <div className="space-y-4">
      {/* Sort Buttons */}
      <div className="flex items-center justify-between text-xs pb-1">
        <span className="text-slate-500 font-medium">Rank Companies By:</span>
        <div className="flex items-center gap-1.5">
          {[
            { id: 'offers', label: 'Total Offers' },
            { id: 'rate', label: 'Conversion Rate' },
            { id: 'ctc', label: 'Average Package' },
          ].map((s) => (
            <button
              key={s.id}
              onClick={() => setSortBy(s.id)}
              className={`px-2.5 py-1 rounded-md text-[11px] font-semibold border transition-colors ${
                sortBy === s.id
                  ? 'bg-slate-900 text-white border-slate-900'
                  : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
              }`}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        {sorted.slice(0, 8).map((comp, idx) => {
          const widthPct = Math.max(Math.round(((comp.offersReleased || 0) / maxOffers) * 100), comp.offersReleased > 0 ? 5 : 0);

          return (
            <div key={idx} className="p-3 rounded-lg border border-slate-100 hover:border-slate-200 bg-slate-50/50 hover:bg-white transition-all space-y-2">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 text-xs">
                <div>
                  <span className="font-bold text-slate-900">{comp.companyName}</span>
                  <span className="text-slate-400 text-[11px] ml-2">({comp.industry || 'Technology'})</span>
                </div>
                <div className="flex items-center gap-3 font-mono text-[11px]">
                  <span className="text-slate-500">Apps: <strong>{comp.applicationsReceived}</strong></span>
                  <span className="text-slate-500">Placed: <strong className="text-emerald-700">{comp.candidatesPlaced}</strong></span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-800 border border-blue-200">
                    {comp.conversionRate}% Conv
                  </span>
                  <span className="font-bold text-slate-900">
                    ₹ {comp.averageCtc.toFixed(2)} L
                  </span>
                </div>
              </div>

              {/* Progress bar showing offers share */}
              <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden flex">
                <div
                  className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                  style={{ width: `${widthPct}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
