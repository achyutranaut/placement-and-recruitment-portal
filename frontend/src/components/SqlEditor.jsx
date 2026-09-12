import React, { useRef, useState } from 'react';
import { Play, RotateCcw, Copy, Check, Wand2, Terminal, Zap, Layers } from 'lucide-react';

const SQL_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'EXISTS', 'LIKE', 'BETWEEN',
  'IS', 'NULL', 'ORDER BY', 'GROUP BY', 'HAVING', 'JOIN', 'INNER JOIN', 'LEFT JOIN',
  'RIGHT JOIN', 'FULL JOIN', 'ON', 'AS', 'DISTINCT', 'COUNT', 'SUM', 'AVG', 'MIN', 'MAX',
  'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM', 'CREATE TABLE', 'ALTER TABLE',
  'DROP TABLE', 'TRUNCATE TABLE', 'CREATE VIEW', 'CREATE INDEX', 'CREATE OR REPLACE',
  'PRIMARY KEY', 'FOREIGN KEY', 'REFERENCES', 'CHECK', 'UNIQUE', 'DEFAULT',
  'VARCHAR2', 'NUMBER', 'DATE', 'TIMESTAMP', 'CLOB', 'BLOB', 'CHAR',
  'DECLARE', 'BEGIN', 'END', 'EXCEPTION', 'WHEN', 'THEN', 'LOOP', 'FOR', 'WHILE',
  'CURSOR', 'IS', 'OPEN', 'FETCH', 'INTO', 'CLOSE', 'EXIT', 'IF', 'ELSIF', 'ELSE',
  'PROCEDURE', 'FUNCTION', 'RETURN', 'TRIGGER', 'BEFORE', 'AFTER', 'EACH ROW',
  'COMMIT', 'ROLLBACK', 'SAVEPOINT', 'DBMS_OUTPUT.PUT_LINE', 'DBMS_OUTPUT.ENABLE'
];

export default function SqlEditor({
  value,
  onChange,
  onRun,
  loading = false,
  mode = 'QUERY',
  onModeChange,
}) {
  const textareaRef = useRef(null);
  const lineNumbersRef = useRef(null);
  const [copied, setCopied] = useState(false);
  const [selectedText, setSelectedText] = useState('');

  // Synchronize vertical scrolling between textarea and line numbers gutter
  const handleScroll = () => {
    if (textareaRef.current && lineNumbersRef.current) {
      lineNumbersRef.current.scrollTop = textareaRef.current.scrollTop;
    }
  };

  // Check for highlighted / selected text in textarea
  const handleSelectCheck = () => {
    if (textareaRef.current) {
      const { selectionStart, selectionEnd, value: textVal } = textareaRef.current;
      if (selectionEnd > selectionStart) {
        const sel = textVal.substring(selectionStart, selectionEnd).trim();
        setSelectedText(sel);
      } else {
        setSelectedText('');
      }
    }
  };

  // Keyboard handler for Tab indentation and Ctrl/Cmd + Enter
  const handleKeyDown = (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      e.preventDefault();
      if (!loading && onRun) {
        if (selectedText) {
          onRun(selectedText);
        } else {
          onRun();
        }
      }
      return;
    }

    if (e.key === 'Tab') {
      e.preventDefault();
      const textarea = textareaRef.current;
      if (!textarea) return;

      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const val = textarea.value;

      // Insert 2 spaces
      const updated = val.substring(0, start) + '  ' + val.substring(end);
      onChange(updated);

      // Restore cursor position
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + 2;
      }, 0);
    }
  };

  // Format SQL: capitalize keywords and organize standard indentation
  const handleFormat = () => {
    if (!value || !value.trim()) return;

    let formatted = value;
    SQL_KEYWORDS.forEach((kw) => {
      const regex = new RegExp(`\\b${kw}\\b`, 'gi');
      formatted = formatted.replace(regex, kw);
    });

    onChange(formatted);
  };

  // Copy to clipboard
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy SQL:', err);
    }
  };

  // Clear editor
  const handleClear = () => {
    onChange('');
    setSelectedText('');
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  };

  // Approximate statement count
  const countStatements = (code) => {
    if (!code || !code.trim()) return 0;
    const clean = code.replace(/--.*$/gm, '').trim();
    const stmts = clean.split(/;|\n\/\s*$/m).filter((s) => s.trim().length > 0);
    return Math.max(stmts.length, 1);
  };

  const lines = (value || '').split('\n');
  const lineCount = Math.max(lines.length, 1);
  const totalStatements = countStatements(value);
  const isMultiQuery = totalStatements > 1;

  return (
    <div className="flex flex-col border border-slate-700 rounded-lg overflow-hidden bg-slate-950 shadow-md">
      {/* Top Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 bg-slate-900 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-slate-400 font-mono text-[11px]">
            <Terminal className="w-3.5 h-3.5 text-blue-400" />
            <span className="font-semibold text-slate-200">Oracle SQL / PLSQL Terminal</span>
          </div>

          {/* Multi-Query Indicator Badge */}
          <div
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full font-mono text-[10px] font-bold ${
              isMultiQuery
                ? 'bg-purple-900/60 text-purple-300 border border-purple-700'
                : 'bg-slate-800 text-slate-400 border border-slate-700'
            }`}
            title="Multiple queries separated by semicolons (;) or slashes (/) will execute sequentially in a go"
          >
            <Layers className="w-3 h-3" />
            <span>
              {isMultiQuery
                ? `Multi-Query Mode (${totalStatements} Statements)`
                : 'Single Statement'}
            </span>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={handleFormat}
            className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white font-medium transition-colors"
            title="Format SQL keywords & indentation"
          >
            <Wand2 className="w-3.5 h-3.5 text-indigo-400" />
            <span className="hidden sm:inline">Format</span>
          </button>

          <button
            type="button"
            onClick={handleCopy}
            className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white font-medium transition-colors"
            title="Copy SQL code"
          >
            {copied ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span className="text-emerald-400">Copied</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5 text-slate-400" />
                <span className="hidden sm:inline">Copy</span>
              </>
            )}
          </button>

          <button
            type="button"
            onClick={handleClear}
            className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-rose-950/60 text-slate-400 hover:text-rose-300 font-medium transition-colors"
            title="Clear editor contents"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Clear</span>
          </button>

          {/* Conditional "RUN SELECTION" button if text is highlighted */}
          {selectedText && (
            <button
              type="button"
              disabled={loading}
              onClick={() => onRun && onRun(selectedText)}
              className="flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-bold text-white bg-blue-600 hover:bg-blue-500 active:bg-blue-700 shadow-sm transition-all"
              title="Execute only highlighted SQL block"
            >
              <Zap className="w-3.5 h-3.5 fill-current text-yellow-300" />
              <span>RUN SELECTION</span>
            </button>
          )}

          {/* Primary Execute Button (Runs ALL or Single) */}
          <button
            type="button"
            disabled={loading || !value?.trim()}
            onClick={() => onRun && onRun()}
            className={`flex items-center gap-1.5 px-3.5 py-1 rounded-md text-xs font-bold text-white shadow-sm transition-all ${
              loading
                ? 'bg-slate-700 cursor-not-allowed text-slate-400'
                : isMultiQuery
                ? 'bg-purple-600 hover:bg-purple-500 active:bg-purple-700'
                : 'bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700'
            }`}
            title="Execute (Ctrl/Cmd + Enter) - Runs all queries in the terminal"
          >
            {loading ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Executing...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>
                  {isMultiQuery
                    ? `RUN ALL (${totalStatements})`
                    : 'RUN QUERY'}
                </span>
                <span className="hidden lg:inline text-[10px] font-normal opacity-75 ml-1">
                  (⌘↵)
                </span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Editor Body: Line Numbers + Textarea */}
      <div className="relative flex min-h-[220px] max-h-[380px] bg-slate-950 font-mono text-sm leading-6">
        {/* Line Numbers Column */}
        <div
          ref={lineNumbersRef}
          className="select-none py-3 px-3 bg-slate-900/90 text-slate-500 text-right border-r border-slate-800/80 overflow-hidden font-mono text-xs leading-6"
          style={{ width: `${Math.max(String(lineCount).length * 9 + 20, 42)}px` }}
        >
          {Array.from({ length: lineCount }, (_, i) => (
            <div key={i + 1} className="h-6 leading-6">
              {i + 1}
            </div>
          ))}
        </div>

        {/* Text Input Area */}
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          onScroll={handleScroll}
          onSelect={handleSelectCheck}
          onMouseUp={handleSelectCheck}
          onKeyUp={handleSelectCheck}
          spellCheck={false}
          autoCapitalize="off"
          autoComplete="off"
          placeholder="-- Type one or multiple SQL queries / DDL / DML / PL/SQL statements here&#10;-- Separate multiple queries with semicolons (;) or PL/SQL blocks with slashes (/)&#10;-- Click RUN ALL or press Cmd + Enter to execute all queries in a go!&#10;&#10;SELECT * FROM STUDENT;&#10;SELECT * FROM PLACEMENT_DRIVE;"
          className="flex-1 p-3 bg-transparent text-slate-100 placeholder:text-slate-600 font-mono text-xs md:text-sm leading-6 resize-y focus:outline-none overflow-auto whitespace-pre tab-[2]"
          style={{ minHeight: '220px', maxHeight: '380px' }}
        />
      </div>

      {/* Editor Footer Status Bar */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-slate-900 border-t border-slate-800 text-[11px] text-slate-400 font-mono">
        <div className="flex items-center gap-3">
          <span>Lines: <strong className="text-slate-200">{lineCount}</strong></span>
          <span>Statements: <strong className="text-purple-400">{totalStatements}</strong></span>
          {selectedText && (
            <span className="text-blue-400 font-semibold">
              Selection: {selectedText.length} chars
            </span>
          )}
        </div>
        <div className="flex items-center gap-3 text-slate-500">
          <span>Engine: <span className="text-emerald-400 font-semibold">Oracle 23c Free</span></span>
          <span className="hidden sm:inline">
            Execute: <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300">Ctrl/Cmd + Enter</kbd>
          </span>
        </div>
      </div>
    </div>
  );
}
