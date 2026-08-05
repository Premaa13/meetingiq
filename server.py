import os

from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from supabase import create_client

from extract import (
    extract_action_items,
    extract_action_items_from_audio,
)

load_dotenv()

app = FastAPI()

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "https://meetingiq.vercel.app",  # replace with your actual Vercel URL once you have it
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Supabase Client
supabase = create_client(
    os.getenv("SUPABASE_URL"),
    os.getenv("SUPABASE_SERVICE_KEY")
)


class TranscriptRequest(BaseModel):
    transcript: str


def get_authenticated_user_id(authorization: str = Header(None)) -> str:
    if not authorization:
        raise HTTPException(
            status_code=401,
            detail="Missing auth token",
        )

    token = authorization.replace("Bearer ", "").strip()
    if not token:
        raise HTTPException(
            status_code=401,
            detail="Missing auth token",
        )

    try:
        user_response = supabase.auth.get_user(token)
        return user_response.user.id
    except Exception:
        raise HTTPException(
            status_code=401,
            detail="Invalid or expired token",
        )


def build_meeting_payload(meeting, action_items, key_points):
    return {
        "meeting_id": meeting["id"],
        "title": meeting["title"],
        "meeting_date": meeting["meeting_date"],
        "created_at": meeting["created_at"],
        "action_items": [
            {
                "task": item["task"],
                "owner": item["owner"],
                "deadline": item["deadline"],
                "deadline_date": item.get("deadline_date"),
                "confidence": item["confidence"],
                "is_completed": item.get("is_completed", False),
            }
            for item in action_items
        ],
        "key_points": [point["point"] for point in key_points],
    }


def get_meetings_for_user(user_id: str):
    meetings_response = (
        supabase.table("meetings")
        .select("id, user_id, title, meeting_date, created_at")
        .eq("user_id", user_id)
        .order("created_at", desc=True)
        .execute()
    )

    if not meetings_response.data:
        return []

    meeting_ids = [meeting["id"] for meeting in meetings_response.data]

    action_items_response = (
        supabase.table("action_items")
        .select("meeting_id, task, owner, deadline, deadline_date, confidence, is_completed")
        .eq("user_id", user_id)
        .in_("meeting_id", meeting_ids)
        .execute()
    )

    key_points_response = (
        supabase.table("key_points")
        .select("meeting_id, point")
        .eq("user_id", user_id)
        .in_("meeting_id", meeting_ids)
        .execute()
    )

    action_items_by_meeting = {}
    for item in action_items_response.data or []:
        action_items_by_meeting.setdefault(item["meeting_id"], []).append(item)

    key_points_by_meeting = {}
    for point in key_points_response.data or []:
        key_points_by_meeting.setdefault(point["meeting_id"], []).append(point)

    return [
        build_meeting_payload(
            meeting,
            action_items_by_meeting.get(meeting["id"], []),
            key_points_by_meeting.get(meeting["id"], []),
        )
        for meeting in meetings_response.data
    ]


@app.get("/meetings")
def get_meetings(authorization: str = Header(None)):
    user_id = get_authenticated_user_id(authorization)
    return get_meetings_for_user(user_id)


@app.get("/meetings/{meeting_id}")
def get_meeting(meeting_id: str, authorization: str = Header(None)):
    user_id = get_authenticated_user_id(authorization)

    meeting_response = (
        supabase.table("meetings")
        .select("id, user_id, title, meeting_date, created_at")
        .eq("id", meeting_id)
        .eq("user_id", user_id)
        .execute()
    )

    if not meeting_response.data:
        raise HTTPException(status_code=404, detail="Meeting not found")

    meeting = meeting_response.data[0]

    action_items_response = (
        supabase.table("action_items")
        .select("meeting_id, task, owner, deadline, deadline_date, confidence, is_completed")
        .eq("meeting_id", meeting_id)
        .eq("user_id", user_id)
        .execute()
    )

    key_points_response = (
        supabase.table("key_points")
        .select("meeting_id, point")
        .eq("meeting_id", meeting_id)
        .eq("user_id", user_id)
        .execute()
    )

    return build_meeting_payload(
        meeting,
        action_items_response.data or [],
        key_points_response.data or [],
    )


@app.post("/extract")
def extract(
    request: TranscriptRequest,
    authorization: str = Header(None),
):
    user_id = get_authenticated_user_id(authorization)

    data = extract_action_items(request.transcript)

    meeting = supabase.table("meetings").insert(
        {
            "user_id": user_id,
            "title": data["title"],
            "meeting_date": data["meeting_date"],
            "transcript": request.transcript,
        }
    ).execute()

    meeting_id = meeting.data[0]["id"]

    items_to_insert = [
        {
            "meeting_id": meeting_id,
            "user_id": user_id,
            "task": item["task"],
            "owner": item["owner"],
            "deadline": item["deadline"],
            "deadline_date": item["deadline_date"],
            "confidence": item["confidence"],
            "is_completed": False,
        }
        for item in data["action_items"]
    ]

    if data.get("key_points"):
        key_points_to_insert = [
            {
                "meeting_id": meeting_id,
                "user_id": user_id,
                "point": point,
            }
            for point in data["key_points"]
        ]

        supabase.table("key_points").insert(
            key_points_to_insert
        ).execute()

    supabase.table("action_items").insert(
        items_to_insert
    ).execute()

    return {
        "meeting_id": meeting_id,
        **data,
    }


@app.post("/extract-audio")
async def extract_audio(
    file: UploadFile = File(...),
    authorization: str = Header(None),
):
    user_id = get_authenticated_user_id(authorization)

    audio_bytes = await file.read()

    data = extract_action_items_from_audio(
        audio_bytes,
        file.content_type,
    )

    meeting = supabase.table("meetings").insert(
        {
            "user_id": user_id,
            "title": data["title"],
            "meeting_date": data["meeting_date"],
            "transcript": f"[Audio recording: {file.filename}]",
        }
    ).execute()

    meeting_id = meeting.data[0]["id"]

    items_to_insert = [
        {
            "meeting_id": meeting_id,
            "user_id": user_id,
            "task": item["task"],
            "owner": item["owner"],
            "deadline": item["deadline"],
            "deadline_date": item["deadline_date"],
            "confidence": item["confidence"],
            "is_completed": False,
        }
        for item in data["action_items"]
    ]

    if data.get("key_points"):
        key_points_to_insert = [
            {
                "meeting_id": meeting_id,
                "user_id": user_id,
                "point": point,
            }
            for point in data["key_points"]
        ]

        supabase.table("key_points").insert(
            key_points_to_insert
        ).execute()

    supabase.table("action_items").insert(
        items_to_insert
    ).execute()

    return {
        "meeting_id": meeting_id,
        **data,
    }