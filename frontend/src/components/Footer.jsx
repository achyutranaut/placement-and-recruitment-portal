import React from 'react';
import { Database } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-white py-6 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-xs text-slate-500">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 font-medium text-slate-700">
            <Database className="w-4 h-4 text-slate-600" />
            <span>Student Placement and Recruitment Portal — Academic Year 2025-26</span>
          </div>

          <div className="text-slate-500 text-xs">
            Database Systems Project (DA2) — Department of Computer Science & Engineering
          </div>
        </div>
      </div>
    </footer>
  );
}
