import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { History, LogOut } from 'lucide-react'
import Mascot from './Mascot'

const AVATAR_OPTIONS = ['🦊', '🐼', '🐸', '🐧', '🦄', '🐨', '🐯', '🦁']

export default function Header({ session, onLogout, onOpenHistory }) {
  const [showMenu, setShowMenu] = useState(false)
  const [avatar, setAvatar] = useState(
    localStorage.getItem('meetingiq_avatar') || '🦊'
  )

  function pickAvatar(emoji) {
    setAvatar(emoji)
    localStorage.setItem('meetingiq_avatar', emoji)
    setShowMenu(false)
  }

  function openHistory() {
    setShowMenu(false)
    onOpenHistory?.()
  }

  return (
    <header className="flex items-center justify-between px-8 py-5 border-b border-neutral-800 relative">
      <div className="flex items-center gap-2">
        <Mascot size={40} />
        <h1 className="text-xl font-bold">
          Meeting<span className="text-indigo-400">IQ</span>
        </h1>
      </div>

      <div className="relative">
        <button
          onClick={() => setShowMenu(!showMenu)}
          className="flex items-center gap-2.5 px-2 py-1.5 rounded-lg hover:bg-neutral-900 transition-colors"
        >
          <span className="text-xl w-8 h-8 flex items-center justify-center bg-neutral-800 rounded-full">
            {avatar}
          </span>
          <span className="text-sm text-neutral-400 hidden sm:inline">
            {session.user.email}
          </span>
        </button>

        {showMenu && (
          <div className="absolute right-0 top-full mt-2 w-64 bg-neutral-900 border border-neutral-800 rounded-xl shadow-xl p-4 z-10">
            <button
              onClick={openHistory}
              className="flex items-center gap-2 w-full rounded-lg px-3 py-2 text-sm text-neutral-300 hover:bg-neutral-800 transition-colors mb-3"
            >
              <History size={16} className="text-indigo-400" /> View history
            </button>
            <p className="text-xs text-neutral-500 uppercase tracking-wide mb-3">
              Choose your avatar
            </p>
            <div className="grid grid-cols-4 gap-2 mb-4">
              {AVATAR_OPTIONS.map((emoji) => (
                <button
                  key={emoji}
                  onClick={() => pickAvatar(emoji)}
                  className={`text-2xl w-10 h-10 flex items-center justify-center rounded-lg transition-colors ${
                    avatar === emoji ? 'bg-indigo-600' : 'bg-neutral-800 hover:bg-neutral-700'
                  }`}
                >
                  {emoji}
                </button>
              ))}
            </div>
            <Button
              onClick={onLogout}
              variant="ghost"
              className="w-full justify-start text-neutral-400 hover:text-white gap-2"
            >
              <LogOut size={16} /> Log out
            </Button>
          </div>
        )}
      </div>
    </header>
  )
}