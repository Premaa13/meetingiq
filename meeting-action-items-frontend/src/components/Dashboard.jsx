import Header from './Header'
import FloatingMenu from './FloatingMenu'
import { useState, useRef, useEffect } from 'react'
import { supabase } from '../lib/supabase'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { ArrowLeft, Calendar, CheckCircle2, AlertTriangle, User, Clock, Mic, Square, FileText, Upload, AudioLines, History as HistoryIcon } from 'lucide-react'

function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  if (hour < 21) return 'Good evening'
  return 'Working late?'
}

function MeetingResultsView({ result, sourceName = 'Meeting summary' }) {
  const confirmed = (result.action_items || []).filter((item) => item.confidence === 'high')
  const needsReview = (result.action_items || []).filter((item) => item.confidence === 'low')
  const allConfirmed = needsReview.length === 0

  return (
    <div className="mb-14 pb-14 border-b border-neutral-800 last:border-b-0">
      <div className="mb-6">
        <span className="inline-block text-xs font-medium text-indigo-400 bg-indigo-950/50 border border-indigo-800/50 rounded-full px-3 py-1 mb-3">
          📄 {sourceName}
        </span>
        <h2 className="text-3xl font-bold tracking-tight">{result.title}</h2>
        <p className="text-neutral-500 text-sm mt-2 flex items-center gap-1.5">
          <Calendar size={14} /> {result.meeting_date}
        </p>
      </div>
      {result.key_points && result.key_points.length > 0 && (
        <section className="mb-10">
          <div className="flex items-center gap-2 mb-4">
            <span className="text-lg">📌</span>
            <h3 className="text-sm font-semibold text-indigo-400 uppercase tracking-widest">Key Points</h3>
          </div>
          <div className="bg-indigo-950/20 border border-indigo-900/40 rounded-xl p-5">
            <ul className="space-y-2">
              {result.key_points.map((point, i) => (
                <li key={i} className="flex items-start gap-2 text-sm text-neutral-300">
                  <span className="text-indigo-400 mt-0.5">•</span>
                  {point}
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}
      {allConfirmed && (
        <div className="mb-8 bg-emerald-950/40 border border-emerald-800/50 rounded-xl p-5 flex items-center gap-3">
          <span className="text-2xl">🎉</span>
          <div>
            <p className="font-semibold text-emerald-300">Nice, everything&apos;s covered!</p>
            <p className="text-sm text-emerald-500/80">Every task has a clear owner and deadline.</p>
          </div>
        </div>
      )}
      {confirmed.length > 0 && (
        <section className="mb-10">
          <div className="flex items-center gap-2 mb-4">
            <CheckCircle2 size={18} className="text-emerald-400" />
            <h3 className="text-sm font-semibold text-emerald-400 uppercase tracking-widest">Confirmed</h3>
            <span className="text-xs text-neutral-600 font-medium">{confirmed.length}</span>
          </div>
          <div className="space-y-3">
            {confirmed.map((item, i) => (
              <div key={i} className="group bg-neutral-900/60 border border-emerald-900/40 rounded-xl p-5 hover:border-emerald-700/60 transition-colors">
                <p className="font-medium text-[15px] text-neutral-100 leading-snug">{item.task}</p>
                <div className="flex items-center gap-5 mt-3">
                  <span className="flex items-center gap-1.5 text-sm text-neutral-400"><User size={14} /> {item.owner}</span>
                  <span className="flex items-center gap-1.5 text-sm text-neutral-400"><Clock size={14} /> {item.deadline}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
      {needsReview.length > 0 && (
        <section>
          <div className="flex items-center gap-2 mb-4">
            <AlertTriangle size={18} className="text-amber-400" />
            <h3 className="text-sm font-semibold text-amber-400 uppercase tracking-widest">Needs Review</h3>
            <span className="text-xs text-neutral-600 font-medium">{needsReview.length}</span>
          </div>
          <div className="space-y-3">
            {needsReview.map((item, i) => (
              <div key={i} className="group bg-neutral-900/60 border border-amber-900/40 rounded-xl p-5 hover:border-amber-700/60 transition-colors">
                <p className="font-medium text-[15px] text-neutral-100 leading-snug">{item.task}</p>
                <div className="flex items-center gap-5 mt-3">
                  <span className={`flex items-center gap-1.5 text-sm ${item.owner ? 'text-neutral-400' : 'text-amber-400 font-medium'}`}><User size={14} /> {item.owner || 'No owner — assign one'}</span>
                  <span className={`flex items-center gap-1.5 text-sm ${item.deadline ? 'text-neutral-400' : 'text-amber-400 font-medium'}`}><Clock size={14} /> {item.deadline || 'No deadline — set one'}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

export default function Dashboard({ session }) {
  const [transcript, setTranscript] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [results, setResults] = useState(null)
  const [view, setView] = useState('dashboard')
  const [historyMeetings, setHistoryMeetings] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState('')
  const [selectedMeeting, setSelectedMeeting] = useState(null)
  const [inputMode, setInputMode] = useState('paste')
  const [uploadedFiles, setUploadedFiles] = useState([])
  const [isRecording, setIsRecording] = useState(false)
  const [recordedBlob, setRecordedBlob] = useState(null)
  const [audioFile, setAudioFile] = useState(null)
  const mediaRecorderRef = useRef(null)
  const audioChunksRef = useRef([])

  const backgroundStyles = `
    @keyframes floatSlow {
      0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
      50% { transform: translate3d(0, -16px, 0) scale(1.04); }
    }
  `

  async function fetchHistoryMeetings() {
    if (!session?.access_token) return

    setHistoryLoading(true)
    setHistoryError('')

    try {
      const res = await fetch('http://127.0.0.1:8000/meetings', {
        headers: { Authorization: `Bearer ${session.access_token}` },
      })
      if (!res.ok) throw new Error('Unable to load your meeting history.')
      const data = await res.json()
      setHistoryMeetings(data)
    } catch (err) {
      setHistoryError(err.message)
    } finally {
      setHistoryLoading(false)
    }
  }

  function handleFileUpload(e) {
    const files = Array.from(e.target.files)
    if (files.length === 0) return
    files.forEach((file) => {
      const reader = new FileReader()
      reader.onload = (event) => {
        let text = event.target.result
        if (file.name.endsWith('.vtt')) {
          text = text.split('\n').filter((line) => !line.includes('-->') && line.trim() !== 'WEBVTT').join('\n')
        }
        setUploadedFiles((prev) => [...prev, { name: file.name, content: text.trim() }])
      }
      reader.readAsText(file)
    })
  }

  function removeFile(index) {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index))
  }

  async function startRecording() {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const recorder = new MediaRecorder(stream)
    audioChunksRef.current = []
    recorder.ondataavailable = (e) => audioChunksRef.current.push(e.data)
    recorder.onstop = () => {
      const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' })
      setRecordedBlob(blob)
      stream.getTracks().forEach((track) => track.stop())
    }
    recorder.start()
    mediaRecorderRef.current = recorder
    setIsRecording(true)
  }

  function stopRecording() {
    mediaRecorderRef.current?.stop()
    setIsRecording(false)
  }

  function handleAudioFileSelect(e) {
    const file = e.target.files[0]
    if (file) setAudioFile(file)
  }

  async function extractOne(transcriptText) {
    const res = await fetch('http://127.0.0.1:8000/extract', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${session.access_token}` },
      body: JSON.stringify({ transcript: transcriptText }),
    })
    if (!res.ok) throw new Error('Extraction failed. Please try again.')
    return res.json()
  }

  async function extractAudio(blobOrFile, name) {
    const formData = new FormData()
    formData.append('file', blobOrFile, name)
    const res = await fetch('http://127.0.0.1:8000/extract-audio', {
      method: 'POST',
      headers: { Authorization: `Bearer ${session.access_token}` },
      body: formData,
    })
    if (!res.ok) throw new Error('Audio extraction failed. Please try again.')
    return res.json()
  }

  async function handleExtract() {
    setLoading(true)
    setError('')
    try {
      if (inputMode === 'upload') {
        if (uploadedFiles.length === 0) { setError('Please upload at least one file.'); setLoading(false); return }
        const allResults = []
        for (const file of uploadedFiles) {
          const data = await extractOne(file.content)
          allResults.push({ sourceName: file.name, ...data })
        }
        setResults(allResults)
        setView('results')
      } else if (inputMode === 'record') {
        if (!recordedBlob) { setError('Please record something first.'); setLoading(false); return }
        const data = await extractAudio(recordedBlob, 'recording.webm')
        setResults([{ sourceName: 'Live recording', ...data }])
        setView('results')
      } else if (inputMode === 'audio-upload') {
        if (!audioFile) { setError('Please select an audio file first.'); setLoading(false); return }
        const data = await extractAudio(audioFile, audioFile.name)
        setResults([{ sourceName: audioFile.name, ...data }])
        setView('results')
      } else {
        if (!transcript.trim()) { setError('Please paste a transcript first.'); setLoading(false); return }
        const data = await extractOne(transcript)
        setResults([{ sourceName: 'Pasted meeting', ...data }])
        setView('results')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleLogout() {
    await supabase.auth.signOut()
  }

  async function checkUpcomingDeadlines() {
    const today = new Date()
    const in3Days = new Date()
    in3Days.setDate(today.getDate() + 3)

    const { data, error } = await supabase
      .from('action_items')
      .select('id, task, deadline_date')
      .eq('user_id', session.user.id)
      .eq('is_completed', false)
      .gte('deadline_date', today.toISOString().split('T')[0])
      .lte('deadline_date', in3Days.toISOString().split('T')[0])

    if (error || !data || data.length === 0) return

    const notifiedIds = JSON.parse(localStorage.getItem('notifiedDeadlines') || '[]')
    const newItems = data.filter((item) => !notifiedIds.includes(item.id))

    if (newItems.length === 0) return

    async function fireNotifications() {
      newItems.forEach((item) => {
        new Notification('⏰ Deadline coming up', {
          body: `"${item.task}" is due ${item.deadline_date}`,
        })
      })
      const updatedIds = [...notifiedIds, ...newItems.map((i) => i.id)]
      localStorage.setItem('notifiedDeadlines', JSON.stringify(updatedIds))
    }

    if (Notification.permission === 'granted') {
      fireNotifications()
    } else if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission()
      if (permission === 'granted') fireNotifications()
    }
  }

  useEffect(() => {
    checkUpcomingDeadlines()
    fetchHistoryMeetings()
  }, [])

  function startOver() {
    setResults(null)
    setSelectedMeeting(null)
    setTranscript('')
    setUploadedFiles([])
    setRecordedBlob(null)
    setAudioFile(null)
    setInputMode('paste')
    setView('dashboard')
  }

  function handleModeSelect(mode) {
    setResults(null)
    setSelectedMeeting(null)
    setTranscript('')
    setUploadedFiles([])
    setRecordedBlob(null)
    setAudioFile(null)
    setInputMode(mode)

    if (mode === 'history') {
      setView('history')
      fetchHistoryMeetings()
      return
    }

    setView('dashboard')
  }

  function openHistory() {
    setSelectedMeeting(null)
    setView('history')
    fetchHistoryMeetings()
  }

  async function openMeeting(meeting) {
    try {
      const res = await fetch(`http://127.0.0.1:8000/meetings/${meeting.meeting_id}`, {
        headers: { Authorization: `Bearer ${session.access_token}` },
      })
      if (!res.ok) throw new Error('Unable to load this meeting.')
      const data = await res.json()
      setSelectedMeeting(data)
      setView('history-detail')
    } catch (err) {
      setHistoryError(err.message)
    }
  }

  if (view === 'results' && results) {
    return (
      <div className="relative min-h-screen overflow-hidden bg-neutral-950 text-white">
        <style>{backgroundStyles}</style>
        <div className="absolute inset-0 z-0 pointer-events-none">
          <div className="absolute inset-0 opacity-40" style={{ backgroundImage: 'radial-gradient(rgba(255,255,255,0.15) 1px, transparent 1px)', backgroundSize: '24px 24px' }} />
          <div className="absolute -top-24 -right-24 h-72 w-72 rounded-full bg-indigo-500/20 blur-3xl" style={{ animation: 'floatSlow 12s ease-in-out infinite' }} />
        </div>
        <div className="relative z-10 min-h-screen">
          <Header session={session} onLogout={handleLogout} onOpenHistory={openHistory} />
          <main className="max-w-2xl mx-auto px-6 py-14">
            <button onClick={startOver} className="flex items-center gap-1.5 text-sm text-neutral-500 hover:text-white transition-colors mb-8">
              <ArrowLeft size={16} /> New meeting
            </button>
            {results.map((result, resultIndex) => (
              <MeetingResultsView key={resultIndex} result={result} sourceName={result.sourceName} />
            ))}
          </main>
          <FloatingMenu onSelectMode={handleModeSelect} />
        </div>
      </div>
    )
  }

  if (view === 'history' || view === 'history-detail') {
    return (
      <div className="min-h-screen bg-neutral-950 text-white">
        <Header session={session} onLogout={handleLogout} onOpenHistory={openHistory} />
        <main className="max-w-2xl mx-auto px-6 py-12">
          <div className="flex items-center justify-between mb-8">
            <div>
              <p className="text-indigo-400 font-medium mb-1 flex items-center gap-2">
                <HistoryIcon size={16} /> History
              </p>
              <h2 className="text-2xl font-bold">Your past meetings</h2>
            </div>
            <Button onClick={startOver} variant="ghost" className="text-neutral-300 hover:text-white">
              <ArrowLeft size={16} className="mr-2" /> New meeting
            </Button>
          </div>

          {view === 'history-detail' ? (
            <>
              <button onClick={() => setView('history')} className="flex items-center gap-1.5 text-sm text-neutral-500 hover:text-white transition-colors mb-8">
                <ArrowLeft size={16} /> Back to history
              </button>
              {selectedMeeting ? (
                <MeetingResultsView result={selectedMeeting} sourceName="Past meeting" />
              ) : (
                <p className="text-neutral-400">Select a meeting to view the full summary.</p>
              )}
            </>
          ) : (
            <div className="space-y-3">
              {historyLoading && <p className="text-neutral-400">Loading your history…</p>}
              {historyError && <p className="text-amber-400">{historyError}</p>}
              {!historyLoading && !historyError && historyMeetings.length === 0 && (
                <div className="rounded-xl border border-neutral-800 bg-neutral-900/60 p-6 text-sm text-neutral-400">
                  No meetings yet. Extract one to start building your history.
                </div>
              )}
              {historyMeetings.map((meeting) => {
                const confirmedCount = (meeting.action_items || []).filter((item) => item.confidence === 'high').length
                const needsReviewCount = (meeting.action_items || []).filter((item) => item.confidence === 'low').length

                return (
                  <button
                    key={meeting.meeting_id}
                    onClick={() => openMeeting(meeting)}
                    className="w-full text-left rounded-xl border border-neutral-800 bg-neutral-900/70 p-5 transition-colors hover:border-indigo-500/60 hover:bg-neutral-900"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <h3 className="text-lg font-semibold text-white">{meeting.title}</h3>
                        <p className="mt-1 flex items-center gap-1.5 text-sm text-neutral-500">
                          <Calendar size={14} /> {meeting.meeting_date}
                        </p>
                      </div>
                      <span className="text-sm font-medium text-indigo-400">Open</span>
                    </div>
                    <div className="mt-4 flex flex-wrap gap-3 text-sm text-neutral-400">
                      <span>{(meeting.action_items || []).length} action items</span>
                      <span>{confirmedCount} confirmed</span>
                      <span>{needsReviewCount} needs review</span>
                    </div>
                  </button>
                )
              })}
            </div>
          )}
        </main>
        <FloatingMenu onSelectMode={handleModeSelect} />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <Header session={session} onLogout={handleLogout} onOpenHistory={openHistory} />
      <main className="max-w-2xl mx-auto px-6 py-12">
        <p className="text-indigo-400 font-medium mb-1">{getGreeting()} 👋</p>
        <h2 className="text-2xl font-bold mb-6">Add your meeting notes</h2>
        <div className="flex flex-wrap gap-1 mb-4 bg-neutral-900 border border-neutral-800 rounded-lg p-1 w-fit">
          <button onClick={() => setInputMode('paste')} className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors ${inputMode === 'paste' ? 'bg-indigo-600 text-white' : 'text-neutral-400 hover:text-white'}`}>
            <FileText size={14} /> Paste text
          </button>
          <button onClick={() => setInputMode('upload')} className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors ${inputMode === 'upload' ? 'bg-indigo-600 text-white' : 'text-neutral-400 hover:text-white'}`}>
            <Upload size={14} /> Upload file
          </button>
          <button onClick={() => setInputMode('record')} className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors ${inputMode === 'record' ? 'bg-indigo-600 text-white' : 'text-neutral-400 hover:text-white'}`}>
            <Mic size={14} /> Record audio
          </button>
          <button onClick={() => setInputMode('audio-upload')} className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-sm font-medium transition-colors ${inputMode === 'audio-upload' ? 'bg-indigo-600 text-white' : 'text-neutral-400 hover:text-white'}`}>
            <AudioLines size={14} /> Upload audio
          </button>
        </div>
        {inputMode === 'paste' && (
          <Textarea value={transcript} onChange={(e) => setTranscript(e.target.value)} placeholder="Paste your meeting transcript here..." className="bg-neutral-900 border-neutral-700 text-white min-h-[240px] placeholder:text-neutral-500" />
        )}
        {inputMode === 'upload' && (
          <div>
            <label className="flex flex-col items-center justify-center gap-3 min-h-[180px] border-2 border-dashed border-neutral-700 rounded-lg cursor-pointer hover:border-indigo-500 transition-colors bg-neutral-900/50">
              <input type="file" accept=".txt,.vtt" multiple onChange={handleFileUpload} className="hidden" />
              <span className="text-4xl">📎</span>
              <span className="text-neutral-300 font-medium">Click to upload .txt or .vtt files</span>
              <span className="text-neutral-500 text-sm">or drag and drop — multiple files supported</span>
            </label>
            {uploadedFiles.length > 0 && (
              <div className="mt-3 space-y-2">
                {uploadedFiles.map((file, i) => (
                  <div key={i} className="flex items-center justify-between bg-neutral-900 border border-neutral-800 rounded-lg px-4 py-2.5">
                    <span className="text-sm text-neutral-300 truncate">✅ {file.name}</span>
                    <button onClick={() => removeFile(i)} className="text-neutral-500 hover:text-red-400 text-sm ml-3 shrink-0">Remove</button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {inputMode === 'record' && (
          <div className="flex flex-col items-center justify-center gap-4 min-h-[240px] border-2 border-dashed border-neutral-700 rounded-lg bg-neutral-900/50">
            {!isRecording && !recordedBlob && (
              <>
                <button onClick={startRecording} className="w-16 h-16 rounded-full bg-red-600 hover:bg-red-500 flex items-center justify-center transition-colors">
                  <Mic size={26} />
                </button>
                <p className="text-neutral-400 text-sm">Tap to start recording</p>
              </>
            )}
            {isRecording && (
              <>
                <button onClick={stopRecording} className="w-16 h-16 rounded-full bg-red-600 flex items-center justify-center animate-pulse">
                  <Square size={22} fill="white" />
                </button>
                <p className="text-red-400 text-sm">Recording... tap to stop</p>
              </>
            )}
            {recordedBlob && !isRecording && (
              <>
                <p className="text-emerald-400 text-sm">✅ Recording captured</p>
                <button onClick={() => setRecordedBlob(null)} className="text-neutral-500 hover:text-white text-sm underline">Record again</button>
              </>
            )}
          </div>
        )}
        {inputMode === 'audio-upload' && (
          <div>
            <label className="flex flex-col items-center justify-center gap-3 min-h-[180px] border-2 border-dashed border-neutral-700 rounded-lg cursor-pointer hover:border-indigo-500 transition-colors bg-neutral-900/50">
              <input type="file" accept="audio/*,video/mp4,video/webm" onChange={handleAudioFileSelect} className="hidden" />
              <span className="text-4xl">🎵</span>
              <span className="text-neutral-300 font-medium">{audioFile ? `✅ ${audioFile.name}` : 'Click to upload an audio file'}</span>
              <span className="text-neutral-500 text-sm">mp3, wav, m4a, or Zoom/Meet recordings (mp4)</span>
            </label>
          </div>
        )}
        {error && <p className="text-sm text-amber-400 mt-3">{error}</p>}
        <Button onClick={handleExtract} disabled={loading} className="mt-4 h-11 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-6">
          {loading ? 'Extracting...' : 'Extract Action Items'}
        </Button>
      </main>
    </div>
  )
}