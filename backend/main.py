"""
FastAPI Backend Server for DermaScan AI.
Serves EfficientNetB0 inference, model readiness checks, and real HAM10000 evaluation metrics.
"""

from __future__ import annotations

import io
import time
from typing import Dict, List

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel

from dataset import CLASS_CODES, CLASS_LABELS, preprocess_pil_or_bytes
from model_loader import ModelBundle, load_evaluation_metrics, load_trained_model

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/jpg", "image/png"}
MAX_UPLOAD_BYTES = 15 * 1024 * 1024

app = FastAPI(
    title="DermaScan AI Prediction API",
    description=(
        "Educational skin lesion classification API powered by EfficientNetB0 Transfer Learning "
        "on the HAM10000 dataset. Not a medical diagnostic device."
    ),
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

model_bundle: ModelBundle = load_trained_model()


class CategoryProbability(BaseModel):
    code: str
    key: str
    name: str
    probability: float


class PredictionResponse(BaseModel):
    predicted_key: str
    predicted_code: str
    predicted_name: str
    confidence: float
    probabilities: List[CategoryProbability]
    inference_time_ms: int
    model_architecture: str
    disclaimer: str


@app.get("/api/health")
def health_check() -> Dict[str, object]:
    global model_bundle
    if not model_bundle.loaded:
        model_bundle = load_trained_model()
    return {
        "status": "ready" if model_bundle.loaded else "model_not_trained",
        "model_loaded": model_bundle.loaded,
        "model_architecture": "EfficientNetB0 (Transfer Learning)",
        "message": model_bundle.message,
        "categories": [CLASS_LABELS[c]["code"] for c in CLASS_CODES],
    }


@app.get("/api/metrics")
def get_evaluation_metrics() -> Dict[str, object]:
    metrics = load_evaluation_metrics()
    if metrics is None:
        return {
            "available": False,
            "message": (
                "No evaluation metrics found yet. Train the EfficientNetB0 model with `train.py` "
                "or run `evaluate.py` on the HAM10000 test split to generate verified metrics."
            ),
        }
    return metrics


@app.post("/api/predict", response_model=PredictionResponse)
async def predict_lesion(file: UploadFile = File(...)) -> PredictionResponse:
    global model_bundle
    if not model_bundle.loaded or model_bundle.model is None:
        model_bundle = load_trained_model()

    if not model_bundle.loaded or model_bundle.model is None:
        raise HTTPException(
            status_code=503,
            detail=(
                "EfficientNetB0 model is not trained yet. "
                "Run `python train.py --data-dir /path/to/HAM10000` to generate model weights "
                "before requesting real predictions."
            ),
        )

    if file.content_type and file.content_type.lower() not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=400,
            detail="Unsupported file format. Upload a JPG, JPEG, or PNG skin lesion image.",
        )

    raw_bytes = await file.read()
    if not raw_bytes:
        raise HTTPException(status_code=400, detail="Uploaded file is empty.")
    if len(raw_bytes) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="Image exceeds 15 MB upload limit.")

    try:
        pil_image = Image.open(io.BytesIO(raw_bytes))
        pil_image.verify()
        pil_image = Image.open(io.BytesIO(raw_bytes))
    except (UnidentifiedImageError, Exception) as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {exc}")

    start_time = time.perf_counter()
    input_tensor = preprocess_pil_or_bytes(pil_image)
    probs = model_bundle.model.predict(input_tensor, verbose=0)[0]
    elapsed_ms = int((time.perf_counter() - start_time) * 1000)

    ranked_indices = probs.argsort()[::-1]
    top_idx = int(ranked_indices[0])
    top_key = CLASS_CODES[top_idx]
    top_meta = CLASS_LABELS[top_key]

    distribution = [
        CategoryProbability(
            code=CLASS_LABELS[CLASS_CODES[i]]["code"],
            key=CLASS_CODES[i],
            name=CLASS_LABELS[CLASS_CODES[i]]["name"],
            probability=float(probs[i]),
        )
        for i in ranked_indices
    ]

    return PredictionResponse(
        predicted_key=top_key,
        predicted_code=top_meta["code"],
        predicted_name=top_meta["name"],
        confidence=float(probs[top_idx]),
        probabilities=distribution,
        inference_time_ms=elapsed_ms,
        model_architecture="EfficientNetB0 (ImageNet Transfer Learning)",
        disclaimer=(
            "This application is developed for educational and research purposes only. "
            "AI predictions are not medical diagnoses. Please consult a qualified dermatologist "
            "for professional evaluation."
        ),
    )
