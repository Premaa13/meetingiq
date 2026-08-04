import { useState, useRef, useEffect } from 'react'
import { FileText, Upload, Mic, AudioLines, History as HistoryIcon } from 'lucide-react'
import Mascot from './Mascot'

export default function FloatingMenu({ onSelectMode }) {
  const [position, setPosition] = useState({ x: window.innerWidth - 100, y: window.innerHeight - 150 })
  const [dragging, setDragging] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [showHint, setShowHint] = useState(true)
  const offset = useRef({ x: 0, y: 0 })
  const movedRef = useRef(false)

  useEffect(() => {
    const timer = setTimeout(() => setShowHint(false), 6000)
    return () => clearTimeout(timer)
  }, [])

  function handleMouseDown(e) {
    movedRef.current = false
    offset.current = { x: e.clientX - position.x, y: e.clientY - position.y }
    setDragging(true)
    setShowHint(false)
  }

  useEffect(() => {
    function handleMouseMove(e) {
      if (!dragging) return
      movedRef.current = true
      setPosition({
        x: e.clientX - offset.current.x,
        y: e.clientY - offset.current.y,
      })
    }

    function handleMouseUp() {
      if (dragging) {
        setDragging(false)
        if (!movedRef.current) {
          setMenuOpen((prev) => !prev)
        }
      }
    }

    if (dragging) {
      window.addEventListener('mousemove', handleMouseMove)
      window.addEventListener('mouseup', handleMouseUp)
    }

    return () => {
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }
  }, [dragging])

  function pick(mode) {
    onSelectMode(mode)
    setMenuOpen(false)
  }

  return (
    <div
      onMouseDown={handleMouseDown}
      style={{
        position: 'fixed',
        left: `${position.x}px`,
        top: `${position.y}px`,
        zIndex: 50,
        cursor: dragging ? 'grabbing' : 'grab',
        userSelect: 'none',
      }}
    >
      {showHint && !menuOpen && (
        <div className="absolute bottom-1/2 right-full mr-3 translate-y-1/2 whitespace-nowrap bg-indigo-600 text-white text-sm font-medium px-3 py-2 rounded-lg shadow-lg animate-bounce">
          👋 Tap me for more options!
          <div className="absolute top-1/2 -right-1.5 -translate-y-1/2 w-3 h-3 bg-indigo-600 rotate-45" />
        </div>
      )}

      {menuOpen && (
        <div
          onMouseDown={(e) => e.stopPropagation()}
          className="absolute bottom-20 right-0 bg-neutral-900 border border-neutral-800 rounded-xl shadow-xl p-2 w-56"
        >
          <button onClick={() => pick('paste')} className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-neutral-200 hover:bg-neutral-800 transition-colors">
            <FileText size={16} className="text-indigo-400" /> Paste text
          </button>
          <button onClick={() => pick('upload')} className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-neutral-200 hover:bg-neutral-800 transition-colors">
            <Upload size={16} className="text-indigo-400" /> Upload file
          </button>
          <button onClick={() => pick('record')} className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-neutral-200 hover:bg-neutral-800 transition-colors">
            <Mic size={16} className="text-indigo-400" /> Record audio
          </button>
          <button onClick={() => pick('audio-upload')} className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-neutral-200 hover:bg-neutral-800 transition-colors">
            <AudioLines size={16} className="text-indigo-400" /> Upload audio file
          </button>
          <button onClick={() => pick('history')} className="w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm text-neutral-200 hover:bg-neutral-800 transition-colors">
            <HistoryIcon size={16} className="text-indigo-400" /> View history
          </button>
        </div>
      )}

      <div className="relative">
        <div className="absolute inset-0 rounded-full bg-indigo-500 opacity-40 animate-ping" style={{ animationDuration: '2s' }} />
        <div className="relative drop-shadow-2xl">
          <Mascot size={64} />
        </div>
      </div>
    </div>
  )
}