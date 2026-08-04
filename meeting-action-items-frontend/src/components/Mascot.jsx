export default function Mascot({ size = 60 }) {
  return (
    <div className="relative inline-block">
      <svg width={size} height={size} viewBox="0 0 220 220" className="relative">
        <defs>
          <linearGradient id="bodyGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#818CF8" />
            <stop offset="100%" stopColor="#4F46E5" />
          </linearGradient>
        </defs>
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