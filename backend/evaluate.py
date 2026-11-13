"""
Evaluation Script for DermaScan AI EfficientNetB0 Model.
Computes Accuracy, Precision, Recall, F1-Score, and Confusion Matrix on HAM10000 Test Split.
"""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone
from typing import Any, Dict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    precision_recall_fscore_support,
)
import tensorflow as tf

from dataset import CLASS_CODES, CLASS_LABELS, build_tf_dataset, create_grouped_splits, resolve_ham10000_dataframe
from model_loader import ARTIFACTS_DIR, DEFAULT_METRICS_PATH, DEFAULT_MODEL_PATH


def evaluate_model(
    model: tf.keras.Model,
    test_ds: tf.data.Dataset,
    output_dir: str = ARTIFACTS_DIR,
) -> Dict[str, Any]:
    os.makedirs(output_dir, exist_ok=True)

    y_true_batches = []
    y_pred_probs_batches = []

    for images, labels in test_ds:
        probs = model.predict(images, verbose=0)
        y_true_batches.append(np.argmax(labels.numpy(), axis=1))
        y_pred_probs_batches.append(probs)

    y_true = np.concatenate(y_true_batches, axis=0)
    y_probs = np.concatenate(y_pred_probs_batches, axis=0)
    y_pred = np.argmax(y_probs, axis=1)

    accuracy = float(accuracy_score(y_true, y_pred))
    precision_macro, recall_macro, f1_macro, _ = precision_recall_fscore_support(
        y_true, y_pred, average="macro", zero_division=0
    )
    precision_weighted, recall_weighted, f1_weighted, _ = precision_recall_fscore_support(
        y_true, y_pred, average="weighted", zero_division=0
    )

    cm = confusion_matrix(y_true, y_pred, labels=list(range(len(CLASS_CODES))))
    report = classification_report(
        y_true,
        y_pred,
        labels=list(range(len(CLASS_CODES))),
        target_names=[CLASS_LABELS[c]["code"] for c in CLASS_CODES],
        output_dict=True,
        zero_division=0,
    )

    # Save Confusion Matrix Plot
    cm_image_path = os.path.join(output_dir, "confusion_matrix.png")
    plt.figure(figsize=(8, 6))
    sns.heatmap(
        cm,
        annot=True,
        fmt="d",
        cmap="YlGnBu",
        xticklabels=[CLASS_LABELS[c]["code"] for c in CLASS_CODES],
        yticklabels=[CLASS_LABELS[c]["code"] for c in CLASS_CODES],
    )
    plt.title("DermaScan AI - EfficientNetB0 Confusion Matrix (HAM10000 Test Split)")
    plt.xlabel("Predicted Label")
    plt.ylabel("True Label")
    plt.tight_layout()
    plt.savefig(cm_image_path, dpi=180)
    plt.close()

    metrics_payload: Dict[str, Any] = {
        "available": True,
        "model_architecture": "EfficientNetB0 (ImageNet Transfer Learning)",
        "dataset": "HAM10000",
        "evaluated_samples": int(len(y_true)),
        "evaluated_at_utc": datetime.now(timezone.utc).isoformat(),
        "accuracy": round(accuracy, 4),
        "precision_macro": round(float(precision_macro), 4),
        "recall_macro": round(float(recall_macro), 4),
        "f1_macro": round(float(f1_macro), 4),
        "precision_weighted": round(float(precision_weighted), 4),
        "recall_weighted": round(float(recall_weighted), 4),
        "f1_weighted": round(float(f1_weighted), 4),
        "class_labels": [CLASS_LABELS[c]["code"] for c in CLASS_CODES],
        "confusion_matrix": cm.tolist(),
        "per_class_report": report,
    }

    metrics_path = os.path.join(output_dir, os.path.basename(DEFAULT_METRICS_PATH))
    with open(metrics_path, "w", encoding="utf-8") as f:
        json.dump(metrics_payload, f, indent=2)

    return metrics_payload


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate trained DermaScan AI EfficientNetB0 model on HAM10000.")
    parser.add_argument("--data-dir", required=True, help="Directory containing HAM10000_metadata.csv and images")
    parser.add_argument("--model-path", default=DEFAULT_MODEL_PATH, help="Path to saved .keras model")
    parser.add_argument("--batch-size", type=int, default=32)
    args = parser.parse_args()

    if not os.path.isfile(args.model_path):
        raise FileNotFoundError(f"Model file not found: {args.model_path}")

    df = resolve_ham10000_dataframe(args.data_dir)
    splits = create_grouped_splits(df)
    test_ds = build_tf_dataset(splits.test_df, batch_size=args.batch_size, training=False)

    model = tf.keras.models.load_model(args.model_path)
    metrics = evaluate_model(model, test_ds)
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
