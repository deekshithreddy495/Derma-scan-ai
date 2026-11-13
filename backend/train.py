"""
Two-Stage Transfer Learning Training Pipeline for DermaScan AI (EfficientNetB0 on HAM10000).
Stage 1: Frozen ImageNet backbone + custom GAP/Dense/Dropout/Softmax head.
Stage 2: Fine-tuning top convolutional blocks at low learning rate.
"""

from __future__ import annotations

import argparse
import json
import os

import tensorflow as tf

from dataset import build_tf_dataset, create_grouped_splits, resolve_ham10000_dataframe
from evaluate import evaluate_model
from model import build_efficientnet_b0_model, unfreeze_for_fine_tuning
from model_loader import ARTIFACTS_DIR, DEFAULT_MODEL_PATH


def train(
    data_dir: str,
    output_model_path: str = DEFAULT_MODEL_PATH,
    batch_size: int = 32,
    head_epochs: int = 12,
    fine_tune_epochs: int = 10,
) -> None:
    os.makedirs(os.path.dirname(output_model_path), exist_ok=True)

    print(f"[DermaScan AI] Loading HAM10000 dataset from: {data_dir}")
    df = resolve_ham10000_dataframe(data_dir)
    splits = create_grouped_splits(df)
    print(
        f"[DermaScan AI] Lesion-grouped splits -> "
        f"Train: {len(splits.train_df)}, Val: {len(splits.val_df)}, Test: {len(splits.test_df)}"
    )

    train_ds = build_tf_dataset(splits.train_df, batch_size=batch_size, training=True)
    val_ds = build_tf_dataset(splits.val_df, batch_size=batch_size, training=False)
    test_ds = build_tf_dataset(splits.test_df, batch_size=batch_size, training=False)

    model, base_model = build_efficientnet_b0_model(learning_rate=1e-3)

    callbacks_stage1 = [
        tf.keras.callbacks.ModelCheckpoint(
            filepath=output_model_path,
            monitor="val_loss",
            save_best_only=True,
            verbose=1,
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss",
            patience=4,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=2,
            min_lr=1e-6,
        ),
    ]

    print("[DermaScan AI] Stage 1: Training custom classification head (frozen EfficientNetB0)...")
    history_1 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=head_epochs,
        class_weight=splits.class_weights,
        callbacks=callbacks_stage1,
    )

    print("[DermaScan AI] Stage 2: Fine-tuning top EfficientNetB0 layers...")
    model = unfreeze_for_fine_tuning(
        model=model,
        base_model=base_model,
        fine_tune_at_layers_from_end=40,
        learning_rate=1e-5,
    )

    callbacks_stage2 = [
        tf.keras.callbacks.ModelCheckpoint(
            filepath=output_model_path,
            monitor="val_loss",
            save_best_only=True,
            verbose=1,
        ),
        tf.keras.callbacks.EarlyStopping(
            monitor="val_loss",
            patience=4,
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=2,
            min_lr=1e-7,
        ),
    ]

    history_2 = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=fine_tune_epochs,
        class_weight=splits.class_weights,
        callbacks=callbacks_stage2,
    )

    model.save(output_model_path)
    print(f"[DermaScan AI] Saved trained model to: {output_model_path}")

    history_payload = {
        "stage_1": {k: [float(v) for v in vals] for k, vals in history_1.history.items()},
        "stage_2": {k: [float(v) for v in vals] for k, vals in history_2.history.items()},
    }
    with open(os.path.join(ARTIFACTS_DIR, "training_history.json"), "w", encoding="utf-8") as f:
        json.dump(history_payload, f, indent=2)

    print("[DermaScan AI] Running final evaluation on held-out HAM10000 test split...")
    metrics = evaluate_model(model, test_ds, output_dir=ARTIFACTS_DIR)
    print(f"[DermaScan AI] Test Accuracy: {metrics['accuracy']:.4f} | Macro F1: {metrics['f1_macro']:.4f}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Train EfficientNetB0 on HAM10000 for DermaScan AI.")
    parser.add_argument("--data-dir", required=True, help="Path to HAM10000 dataset directory")
    parser.add_argument("--output-model", default=DEFAULT_MODEL_PATH, help="Output .keras model path")
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--head-epochs", type=int, default=12)
    parser.add_argument("--fine-tune-epochs", type=int, default=10)
    args = parser.parse_args()

    train(
        data_dir=args.data_dir,
        output_model_path=args.output_model,
        batch_size=args.batch_size,
        head_epochs=args.head_epochs,
        fine_tune_epochs=args.fine_tune_epochs,
    )


if __name__ == "__main__":
    main()
