import { useState, useEffect } from 'react'
import { supabase } from './lib/supabase'
import Dashboard from './components/Dashboard'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 12) return "Good morning"
  if (hour < 17) return "Good afternoon"
  if (hour < 21) return "Good evening"
  return "Working late?"
}

function getEmoji() {
  const hour = new Date().getHours()
  if (hour < 12) return "☀️"
  if (hour < 17) return "👋"
  if (hour < 21) return "🌆"
  return "🌙"
}

function Mascot() {
  return (
    <div className="relative">
      <div className="absolute inset-0 bg-indigo-500 blur-3xl opacity-30 rounded-full" />
      <svg width="220" height="220" viewBox="0 0 220 220" className="relative drop-shadow-2xl">
        <defs>
          <linearGradient id="bodyGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#818CF8" />
            <stop offset="100%" stopColor="#4F46E5" />
          </linearGradient>
        </defs>
        <ellipse cx="110" cy="195" rx="50" ry="9" fill="black" opacity="0.3" />
        <g style={{ transformOrigin: '110px 120px', animation: 'bob 2.4s ease-in-out infinite' }}>
          <circle cx="110" cy="115" r="65" fill="url(#bodyGrad)" stroke="#3730A3" strokeWidth="2.5" />
          <circle cx="78" cy="128" r="9" fill="#F472B6" opacity="0.5" />
          <circle cx="142" cy="128" r="9" fill="#F472B6" opacity="0.5" />
          <circle cx="90" cy="110" r="8" fill="white" />
          <circle cx="130" cy="110" r="8" fill="white" />
          <circle cx="92" cy="112" r="4" fill="#1E1B4B" />
          <circle cx="132" cy="112" r="4" fill="#1E1B4B" />
          <path d="M85 135 Q110 158 135 135" stroke="white" strokeWidth="6" strokeLinecap="round" fill="none" />
          <ellipse cx="55" cy="145" rx="12" ry="22" fill="url(#bodyGrad)" stroke="#3730A3" strokeWidth="2" transform="rotate(20 55 145)" />
          <g style={{ transformOrigin: '158px 120px', animation: 'wave 1.6s ease-in-out infinite' }}>
            <ellipse cx="158" cy="145" rx="12" ry="26" fill="url(#bodyGrad)" stroke="#3730A3" strokeWidth="2" />
          </g>
          <rect x="104" y="45" width="12" height="18" rx="6" fill="#FCD34D" />
          <circle cx="110" cy="42" r="8" fill="#FBBF24" />
        </g>
      </svg>
      <style>{`
        @keyframes wave { 0%, 100% { transform: rotate(0deg); } 30% { transform: rotate(-35deg); } 60% { transform: rotate(-15deg); } }
        @keyframes bob { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-6px); } }
      `}</style>
    </div>
  )
}

function App() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [session, setSession] = useState(null)

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session)
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session)
    })

    return () => subscription.unsubscribe()
  }, [])

  async function handleLogin(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    const { error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) setError(error.message)
    setLoading(false)
  }

  async function handleSignup(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    const { error } = await supabase.auth.signUp({ email, password })
    if (error) setError(error.message)
    else setError('Check your email to confirm your account!')
    setLoading(false)
  }

  if (session) {
  return <Dashboard session={session} />
  }

  return (
    <div className="min-h-screen flex bg-neutral-950">
      <div className="hidden md:flex w-1/2 flex-col items-center justify-center bg-gradient-to-br from-indigo-950 via-neutral-950 to-neutral-950 border-r border-neutral-800">
        <Mascot />
        <h1 className="mt-6 text-3xl font-bold text-white tracking-tight">
          Meeting<span className="text-indigo-400">IQ</span>
        </h1>
        <p className="mt-2 text-neutral-400 text-center max-w-xs">
          Turn messy meeting notes into clear action items — automatically.
        </p>
      </div>

      <div className="flex-1 flex items-center justify-center px-6">
        <div className="w-full max-w-sm">
          <p className="text-indigo-400 font-medium mb-1">
            {getGreeting()} {getEmoji()}
          </p>
          <h2 className="text-2xl font-bold text-white mb-6">
            Sign in to your workspace
          </h2>

          <form className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email" className="text-neutral-300">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                className="bg-neutral-900 border-neutral-700 text-white placeholder:text-neutral-500 h-11"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="password" className="text-neutral-300">Password</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="bg-neutral-900 border-neutral-700 text-white placeholder:text-neutral-500 h-11"
              />
            </div>

            {error && <p className="text-sm text-amber-400">{error}</p>}

            <div className="flex gap-3 pt-2">
              <Button onClick={handleLogin} disabled={loading} className="flex-1 h-11 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold">
                Sign In
              </Button>
              <Button onClick={handleSignup} disabled={loading} className="flex-1 h-11 bg-neutral-800 hover:bg-neutral-700 text-white font-semibold border border-neutral-600">
                Sign Up
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default App