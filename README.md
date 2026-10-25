# MeetingIQ

Turn messy meeting transcripts into clear, confidence-ranked action items — automatically.

MeetingIQ extracts meeting titles, dates, key discussion points, and action items (task, owner, and deadline) from meeting transcripts and recordings. It separates confirmed tasks from items that need review because information is missing.

## Features

* **Multiple input methods** — Paste transcripts, upload `.txt`, `.vtt`, or `.pdf` files, record audio, or upload audio/video recordings.
* **Confidence-gated action items** — Separates confirmed tasks from items missing an owner or deadline.
* **Key points** — Captures important discussion points and contextual notes.
* **Meeting history** — Saves and displays previous extractions.
* **Deadline reminders** — Browser notifications for action items due within three days.
* **Authentication** — Supabase Auth with user-scoped data access.

## Screenshots

### Extraction Results

![Extraction results](./screenshots/result1.png)

![Confirmed and needs-review items](./screenshots/result2.png)

![Key points section](./screenshots/result3.png)

## Tech Stack

| Component        | Technology                              |
| ---------------- | --------------------------------------- |
| Frontend         | React, Vite, Tailwind CSS, lucide-react |
| Java Backend     | Java 17, Spring Boot 3, Maven           |
| Original Backend | Python, FastAPI                         |
| AI Extraction    | Gemini API                              |
| Database         | Supabase PostgreSQL                     |
| Authentication   | Supabase Auth and Row-Level Security    |

## Project Structure

```text
meetingiq/
├── server.py
├── extract.py
├── requirements.txt
├── .gitignore
├── README.md
├── meetingiq-backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/meetingiq/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── config/
│       │   │   ├── dto/
│       │   │   └── exception/
│       │   └── resources/
│       │       └── application.properties
│       └── test/
└── meeting-action-items-frontend/
    ├── src/
    ├── package.json
    └── .gitignore
```

## Getting Started

### Prerequisites

* Node.js 18 or later
* Java 17
* Maven
* A Supabase project
* A Gemini API key for AI extraction

### 1. Clone the Repository

```bash
git clone https://github.com/Premaa13/meetingiq.git
cd meetingiq
```

### 2. Configure Environment Variables

Configure the following variables for the Java backend:

```env
SUPABASE_URL=your_supabase_url
SUPABASE_SERVICE_KEY=your_supabase_service_key
GEMINI_API_KEY=your_gemini_api_key
```

Provide these values through your local environment or your supported local configuration. Never commit real API keys or service-role keys to GitHub.

For the frontend, create `meeting-action-items-frontend/.env`:

```env
VITE_SUPABASE_URL=your_supabase_url
VITE_SUPABASE_ANON_KEY=your_supabase_anon_key
VITE_API_URL=http://localhost:8080
```

Use your actual Supabase project URL and public anon key. Keep the service-role key exclusively on the backend.

### 3. Run the Java Backend

From the repository root:

```bash
cd meetingiq-backend
mvn spring-boot:run
```

The Java backend runs at:

`http://localhost:8080`

To build and test the backend:

```bash
mvn test
mvn package
```

The API includes endpoints for retrieving meetings, extracting action items from transcripts, and processing audio recordings.

### 4. Run the Frontend

Open a second terminal at the repository root:

```bash
cd meeting-action-items-frontend
npm install
npm run dev
```

Open the local URL printed by Vite, usually:

`http://localhost:5173`

Ensure the frontend environment variables are configured and restart the development server after changing them.

### 5. Original Python Backend

The original Python backend files are preserved in the repository. To run that backend separately, install its dependencies:

```bash
python -m venv venv
```

On Windows:

```powershell
venv\Scripts\activate
pip install -r requirements.txt
python server.py
```

Use the environment variables required by the Python backend. Do not expose backend secrets in frontend code.

## Database Schema

| Table          | Purpose                                                          |
| -------------- | ---------------------------------------------------------------- |
| `meetings`     | Stores meeting titles, dates, transcripts, and user associations |
| `action_items` | Stores tasks, owners, deadlines, and confidence information      |
| `key_points`   | Stores important discussion points and contextual notes          |

Row-Level Security (RLS) policies help ensure users can access only the data permitted by the configured policies.

## Roadmap

* [ ] Deploy the application
* [ ] Allow users to edit and complete action items
* [ ] Add shared meeting spaces

## License

No license has been specified yet.
