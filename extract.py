import os
import json
import re
from datetime import date
from dotenv import load_dotenv
from google import genai
from google.genai import types

load_dotenv()

client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))


def extract_action_items(transcript: str):
    prompt = f"""
Today's date is {date.today()}. If a deadline is relative (e.g. "next Friday", "in two weeks"), calculate deadline_date as an actual YYYY-MM-DD date based on today. If no deadline is mentioned, both deadline and deadline_date should be null.

Extract meeting info and action items from this transcript.
Return ONLY a JSON object, no other text, in this exact format:
{{
  "title": "short descriptive title, or null if unclear",
  "meeting_date": "YYYY-MM-DD if mentioned, or null if not mentioned",
  "key_points": ["any notable fact, announcement, or context mentioned — e.g. schedule changes, personal notes like a birthday, overtime/workload mentions, heads-ups, or anything worth remembering later, even if casual or unrelated to action items"],
  "action_items": [
    {{"task": "...", "owner": "... or null", "deadline": "... or null", "deadline_date": "YYYY-MM-DD or null", "confidence": "high or low"}}
  ]
}}

Transcript:
{transcript}
"""
    response = client.models.generate_content(
        model="gemini-flash-lite-latest",
        contents=prompt
    )

    raw = response.text.strip()
    raw = re.sub(r"^```json|```$", "", raw, flags=re.MULTILINE).strip()
    data = json.loads(raw)

    if not data.get("meeting_date"):
        data["meeting_date"] = str(date.today())

    if not data.get("title"):
        data["title"] = f"Meeting on {data['meeting_date']}"

    for item in data["action_items"]:
        if item["owner"] is None or item["deadline"] is None:
            item["confidence"] = "low"

    return data


def extract_action_items_from_audio(audio_bytes: bytes, mime_type: str):
    prompt = f"""
Today's date is {date.today()}. If a deadline is relative (e.g. "next Friday", "in two weeks"), calculate deadline_date as an actual YYYY-MM-DD date based on today. If no deadline is mentioned, both deadline and deadline_date should be null.

Listen to this meeting recording. Extract meeting info and action items.
Return ONLY a JSON object, no other text, in this exact format:
{{
  "title": "short descriptive title, or null if unclear",
  "meeting_date": "YYYY-MM-DD if mentioned, or null if not mentioned",
  "key_points": ["any notable fact, announcement, or context mentioned — e.g. schedule changes, personal notes like a birthday, overtime/workload mentions, heads-ups, or anything worth remembering later, even if casual or unrelated to action items"],
  "action_items": [
    {{"task": "...", "owner": "... or null", "deadline": "... or null", "deadline_date": "YYYY-MM-DD or null", "confidence": "high or low"}}
  ]
}}
"""
    response = client.models.generate_content(
        model="gemini-flash-latest",
        contents=[
            prompt,
            types.Part.from_bytes(data=audio_bytes, mime_type=mime_type)
        ]
    )

    raw = response.text.strip()
    raw = re.sub(r"^```json|```$", "", raw, flags=re.MULTILINE).strip()
    data = json.loads(raw)

    if not data.get("meeting_date"):
        data["meeting_date"] = str(date.today())

    if not data.get("title"):
        data["title"] = f"Meeting on {data['meeting_date']}"

    for item in data["action_items"]:
        if item["owner"] is None or item["deadline"] is None:
            item["confidence"] = "low"

    return data


if __name__ == "__main__":
    sample_transcript = """
    John: We need to finalize the vendor contract by next Friday.
    Sarah: I'll follow up with legal on that.
    John: Also, someone should update the client on the delay.
    Mike: I can do a quick review of the budget numbers, no rush on that one.
    """
    result = extract_action_items(sample_transcript)
    print(result)