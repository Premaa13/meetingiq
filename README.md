# MeetingIQ

Turn messy meeting transcripts into clear, confidence-ranked action items — automatically.

Paste a transcript, upload a file (`.txt`, `.vtt`, `.pdf`), record audio live, or upload an audio/video recording — MeetingIQ extracts the meeting title, date, key discussion points, and action items (task, owner, deadline), flagging anything that's missing an owner or deadline so nothing slips through.

## Screenshots

### Extraction Results
![Extraction results](./screenshots/result1.png)
![Confirmed and needs-review items](./screenshots/result2.png)
![Key points section](./screenshots/result3.png)
## Features

- 📝 **Multiple input methods** — paste text, upload `.txt`/`.vtt`/`.pdf` files, record audio live, or upload audio/video recordings
- ✅ **Confidence-gated action items** — automatically separates clearly-defined tasks ("Confirmed") from ones missing an owner or deadline ("Needs Review")
- 📌 **Key Points** — captures important context and casual notes mentioned in the meeting that aren't formal action items
- 🕘 **History** — every extraction is saved; browse and revisit past meetings anytime
- ⏰ **Deadline reminders** — browser notifications for action items due within 3 days
- 🔐 **Authenticated & secure** — Supabase Auth with row-level security, so users only ever see their own data

## Tech Stack

**Frontend:** React (Vite), Tailwind CSS, lucide-react
**Backend:** FastAPI (Python)
**Database & Auth:** Supabase (Postgres + Auth + RLS)
**AI Extraction:** LLM-based transcript parsing (text + audio)

## Project Structure

```
meeting-action-items/
├── server.py                          # FastAPI backend — API routes
├── extract.py                         # Extraction logic (text & audio)
├── requirements.txt                   # Python dependencies
├── .env                                # Backend secrets (not committed)
└── meeting-action-items-frontend/
    ├── src/
    │   ├── components/
    │   │   ├── Dashboard.jsx          # Main app screen
    │   │   ├── FloatingMenu.jsx       # Input-mode switcher
    │   │   ├── Header.jsx
    │   │   └── Mascot.jsx
    │   └── lib/
    │       └── supabase.js            # Supabase client
    ├── .env                            # Frontend secrets (not committed)
    └── package.json
```

## Getting Started

### Prerequisites

- Node.js (v18+)
- Python 3.10+
- A [Supabase](https://supabase.com) project (URL + service key)

### 1. Clone the repo

```bash
git clone https://github.com/Premaa13/meetingiq.git
cd meetingiq
```

### 2. Backend setup

```bash
python -m venv venv
venv\Scripts\activate        # Windows
# source venv/bin/activate   # macOS/Linux

pip install -r requirements.txt
```

Create a `.env` file in the project root:

```env
SUPABASE_URL=your_supabase_url
SUPABASE_SERVICE_KEY=your_supabase_service_key
```

Run the backend:

```bash
python server.py
```

### 3. Frontend setup

```bash
cd meeting-action-items-frontend
npm install
```

Create a `.env` file inside `meeting-action-items-frontend/`:

```env
VITE_SUPABASE_URL=your_supabase_url
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key
```

Run the frontend:

```bash
npm run dev
```

The app will be available at `http://localhost:5173`.

## Database Schema (Supabase)

| Table | Purpose |
|---|---|
| `meetings` | One row per extraction — title, date, transcript, user_id |
| `action_items` | Task, owner, deadline, confidence level — linked to a meeting |
| `key_points` | Casual notes / context captured alongside action items |

All tables use Row-Level Security so users only ever access their own data.

## Roadmap

- [ ] Live deployment
- [ ] Editable action items (mark complete, reassign owner)
- [ ] Team/shared meeting spaces

## License

This project currently has no license specified.