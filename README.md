# DermaScan AI – Skin Cancer Classification Using Transfer Learning

Educational dermatoscopic skin lesion analysis platform combining an **Android Jetpack Compose client** (`/app`) with a **Python FastAPI + TensorFlow/Keras EfficientNetB0 backend** (`/backend`) trained on the **HAM10000** dataset.

> **Medical Disclaimer:** This application is developed for educational and research purposes only. AI predictions are not medical diagnoses. Please consult a qualified dermatologist for professional evaluation.

---

## Project Structure

- `app/` — Android (Kotlin + Jetpack Compose + Material 3 + Room + Retrofit/Moshi + Coil) application
- `backend/` — Python FastAPI API, HAM10000 dataset preparation (`dataset.py`), EfficientNetB0 Transfer Learning (`model.py`, `train.py`), evaluation (`evaluate.py`), and saved model loader (`model_loader.py`)

---

## Backend Setup & Model Training

1. Create and activate a Python 3.10+ virtual environment inside `backend/`:
   ```bash
   cd backend
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   ```

2. Download the **HAM10000** dataset (`HAM10000_metadata.csv` and image folders) and run the two-stage Transfer Learning training pipeline:
   ```bash
   python train.py --data-dir /path/to/HAM10000 --head-epochs 12 --fine-tune-epochs 10
   ```
   This saves:
   - `backend/artifacts/dermascan_efficientnetb0.keras`
   - `backend/artifacts/evaluation_metrics.json`
   - `backend/artifacts/confusion_matrix.png`

3. Start the FastAPI inference server:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```

4. In the Android app, the default backend URL is `http://10.0.2.2:8000/` (Android Emulator host loopback). Use the server configuration button in the **Analyze** or **Model & Info** screen to point to a LAN or cloud FastAPI deployment URL.
