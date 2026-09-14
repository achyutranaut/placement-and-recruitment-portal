import React, { useState } from 'react';

// Reusable Metric Card
export function MetricCard({ title, value, subtitle, icon: Icon, color = 'indigo', trend }) {
  const colorStyles = {
    indigo: 'bg-indigo-50 text-indigo-600 border-indigo-100',
    emerald: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    blue: 'bg-blue-50 text-blue-600 border-blue-100',
    purple: 'bg-purple-50 text-purple-600 border-purple-100',
    amber: 'bg-amber-50 text-amber-600 border-amber-100',
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
          {title}
        </span>
        <div className={`p-2.5 rounded-xl border ${colorStyles[color] || colorStyles.indigo}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
      <div className="mt-4 flex items-baseline gap-2">
        <span className="text-3xl font-bold tracking-tight text-slate-900">{value}</span>
        {trend && (
          <span className="text-xs font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
            {trend}
          </span>
        )}
      </div>
      {subtitle && <p className="mt-1 text-xs text-slate-500 font-medium">{subtitle}</p>}
    </div>
  );
}

// Custom SVG Horizontal/Vertical Bar Chart
export function BarChart({ data, xKey = 'name', yKey = 'value', height = 220, unit = '' }) {
  const [hovered, setHovered] = useState(null);
  const maxValue = Math.max(...data.map((d) => d[yKey] || 0), 1);

  return (
    <div className="w-full">
      <div className="space-y-3">
        {data.map((item, idx) => {
          const val = item[yKey] || 0;
          const percentage = Math.round((val / maxValue) * 100);
          const isHovered = hovered === idx;

          return (
            <div
              key={idx}
              className="group cursor-pointer"
              onMouseEnter={() => setHovered(idx)}
              onMouseLeave={() => setHovered(null)}
            >
              <div className="flex justify-between items-center text-xs mb-1">
                <span className="font-medium text-slate-700 truncate max-w-[200px]">
                  {item[xKey]}
                </span>
                <span className="font-bold text-slate-900 font-mono">
                  {val} {unit}
                </span>
              </div>
              <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden flex">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    item.color || (idx % 2 === 0 ? 'bg-indigo-600' : 'bg-blue-500')
                  } ${isHovered ? 'brightness-110 shadow-sm' : ''}`}
                  style={{ width: `${Math.max(percentage, 4)}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// Custom SVG Donut / Pie Chart
export function DonutChart({ data = [], size = 180 }) {
  const rawTotal = data.reduce((sum, item) => sum + (Number(item.value) || 0), 0);
  const total = rawTotal > 0 ? rawTotal : 1; // used only for SVG dash array to prevent division by zero
  const radius = size / 2 - 16;
  const circumference = 2 * Math.PI * radius;
  let accumulatedAngle = 0;

  const colors = ['#4f46e5', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#64748b'];

  return (
    <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke="#f1f5f9"
            strokeWidth="20"
          />
          {rawTotal > 0 &&
            data.map((item, idx) => {
              const val = Number(item.value) || 0;
              if (val <= 0) return null;
              const strokeDasharray = `${(val / total) * circumference} ${circumference}`;
              const strokeDashoffset = -accumulatedAngle * circumference;
              accumulatedAngle += val / total;

              return (
                <circle
                  key={idx}
                  cx={size / 2}
                  cy={size / 2}
                  r={radius}
                  fill="transparent"
                  stroke={item.color || colors[idx % colors.length]}
                  strokeWidth="20"
                  strokeDasharray={strokeDasharray}
                  strokeDashoffset={strokeDashoffset}
                  className="transition-all duration-700 hover:opacity-80"
                />
              );
            })}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          <span className="text-2xl font-bold text-slate-800">{rawTotal}</span>
          <span className="text-[10px] uppercase font-semibold text-slate-400">Total</span>
        </div>
      </div>

      {/* Legend */}
      <div className="flex flex-col gap-2 min-w-[140px]">
        {data.length === 0 || rawTotal === 0 ? (
          <div className="text-xs text-slate-400 italic">No applications recorded</div>
        ) : (
          data.map((item, idx) => (
            <div key={idx} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: item.color || colors[idx % colors.length] }}
                />
                <span className="text-slate-600 font-medium">{item.name}</span>
              </div>
              <span className="font-semibold text-slate-900 font-mono ml-3">{item.value}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

