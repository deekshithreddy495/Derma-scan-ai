"""
EfficientNetB0 Transfer Learning Architecture for 7-Class HAM10000 Skin Lesion Classification.
"""

from __future__ import annotations

from typing import Tuple

import tensorflow as tf
from tensorflow.keras import layers, models

from dataset import CLASS_CODES, IMAGE_SIZE


def build_efficientnet_b0_model(
    num_classes: int = len(CLASS_CODES),
    dropout_rate: float = 0.4,
    learning_rate: float = 1e-3,
) -> Tuple[tf.keras.Model, tf.keras.Model]:
    """
    Builds the Transfer Learning classifier:
    - Pretrained ImageNet EfficientNetB0 backbone (initially frozen)
    - GlobalAveragePooling2D
    - BatchNormalization + Dropout
    - Dense(256, activation='relu') + Dropout
    - Dense(7, activation='softmax')
    """
    inputs = layers.Input(shape=(IMAGE_SIZE[0], IMAGE_SIZE[1], 3), name="lesion_image")

    base_model = tf.keras.applications.EfficientNetB0(
        include_top=False,
        weights="imagenet",
        input_tensor=inputs,
    )
    base_model.trainable = False

    x = base_model.output
    x = layers.GlobalAveragePooling2D(name="global_avg_pool")(x)
    x = layers.BatchNormalization(name="head_bn")(x)
    x = layers.Dropout(0.3, name="head_dropout_1")(x)
    x = layers.Dense(256, activation="relu", name="head_dense_256")(x)
    x = layers.Dropout(dropout_rate, name="head_dropout_2")(x)
    outputs = layers.Dense(num_classes, activation="softmax", name="lesion_probabilities")(x)

    model = models.Model(inputs=inputs, outputs=outputs, name="dermascan_efficientnetb0")

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="categorical_crossentropy",
        metrics=[
            "accuracy",
            tf.keras.metrics.AUC(name="auc", multi_label=False),
        ],
    )
    return model, base_model


def unfreeze_for_fine_tuning(
    model: tf.keras.Model,
    base_model: tf.keras.Model,
    fine_tune_at_layers_from_end: int = 40,
    learning_rate: float = 1e-5,
) -> tf.keras.Model:
    """
    Unfreezes the top convolutional layers of EfficientNetB0 for Stage 2 fine-tuning
    while keeping BatchNormalization layers frozen for training stability.
    """
    base_model.trainable = True
    cutoff = max(0, len(base_model.layers) - fine_tune_at_layers_from_end)

    for idx, layer in enumerate(base_model.layers):
        if idx < cutoff or isinstance(layer, layers.BatchNormalization):
            layer.trainable = False
        else:
            layer.trainable = True

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
        loss="categorical_crossentropy",
        metrics=[
            "accuracy",
            tf.keras.metrics.AUC(name="auc", multi_label=False),
        ],
    )
    return model
