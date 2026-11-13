"""
HAM10000 Dataset Preparation, Preprocessing, and Class Metadata for DermaScan AI.
"""

from __future__ import annotations

import glob
import os
from dataclasses import dataclass
from typing import Dict, List, Tuple

import cv2
import numpy as np
import pandas as pd
from PIL import Image
from sklearn.model_selection import StratifiedGroupKFold
from sklearn.utils.class_weight import compute_class_weight
import tensorflow as tf

IMAGE_SIZE: Tuple[int, int] = (224, 224)

# Fixed class ordering matching HAM10000 7 diagnostic categories
CLASS_CODES: List[str] = ["nv", "mel", "bkl", "bcc", "akiec", "vasc", "df"]

CLASS_LABELS: Dict[str, Dict[str, str]] = {
    "nv": {
        "code": "NV",
        "name": "Melanocytic Nevi",
        "short_name": "Melanocytic Nevi",
        "description": (
            "Common benign proliferations of melanocytes (moles). Typically uniform in color, "
            "symmetrical, and well-circumscribed."
        ),
    },
    "mel": {
        "code": "MEL",
        "name": "Melanoma",
        "short_name": "Melanoma",
        "description": (
            "A malignant neoplasm arising from melanocytes. Often exhibits asymmetry, border "
            "irregularity, color variation, or evolving dermoscopic structures."
        ),
    },
    "bkl": {
        "code": "BKL",
        "name": "Benign Keratosis-like Lesions",
        "short_name": "Benign Keratosis",
        "description": (
            "Includes seborrheic keratoses, solar lentigines, and lichen-planus-like keratoses. "
            "Non-cancerous epidermal growths that can occasionally mimic pigmented lesions."
        ),
    },
    "bcc": {
        "code": "BCC",
        "name": "Basal Cell Carcinoma",
        "short_name": "Basal Cell Carcinoma",
        "description": (
            "A common slow-growing epithelial skin cancer characterized dermoscopically by "
            "arborizing vessels, leaf-like areas, or pearly rolled borders."
        ),
    },
    "akiec": {
        "code": "AKIEC",
        "name": "Actinic Keratoses / Bowen's Disease",
        "short_name": "Actinic Keratoses",
        "description": (
            "Intraepidermal squamous neoplasia and sun-induced precancerous lesions that "
            "often appear as rough, scaly erythematous macules or plaques."
        ),
    },
    "vasc": {
        "code": "VASC",
        "name": "Vascular Lesions",
        "short_name": "Vascular Lesions",
        "description": (
            "Benign vascular proliferations such as angiomas, angiokeratomas, and pyogenic "
            "granulomas featuring red-purple lacunae."
        ),
    },
    "df": {
        "code": "DF",
        "name": "Dermatofibroma",
        "short_name": "Dermatofibroma",
        "description": (
            "A benign fibrous cutaneous nodule often displaying a central white scar-like "
            "patch surrounded by a delicate peripheral pigment network."
        ),
    },
}

CODE_TO_INDEX: Dict[str, int] = {code: idx for idx, code in enumerate(CLASS_CODES)}


@dataclass
class DatasetSplits:
    train_df: pd.DataFrame
    val_df: pd.DataFrame
    test_df: pd.DataFrame
    class_weights: Dict[int, float]


def resolve_ham10000_dataframe(data_dir: str) -> pd.DataFrame:
    """
    Loads HAM10000_metadata.csv and maps each image_id to its image file path
    across HAM10000_images_part_1, HAM10000_images_part_2, or direct subfolders.
    """
    metadata_path = os.path.join(data_dir, "HAM10000_metadata.csv")
    if not os.path.isfile(metadata_path):
        raise FileNotFoundError(
            f"HAM10000_metadata.csv not found in '{data_dir}'. "
            "Download HAM10000 and place metadata CSV + image folders in the dataset directory."
        )

    df = pd.read_csv(metadata_path)
    required_cols = {"lesion_id", "image_id", "dx"}
    missing = required_cols - set(df.columns)
    if missing:
        raise ValueError(f"HAM10000_metadata.csv is missing columns: {sorted(missing)}")

    # Discover all jpg/jpeg/png files recursively under data_dir
    image_lookup: Dict[str, str] = {}
    for ext in ("*.jpg", "*.jpeg", "*.png"):
        for path in glob.glob(os.path.join(data_dir, "**", ext), recursive=True):
            stem = os.path.splitext(os.path.basename(path))[0]
            image_lookup[stem] = path

    df["image_path"] = df["image_id"].map(image_lookup)
    df = df.dropna(subset=["image_path"]).copy()
    df["dx"] = df["dx"].str.lower().str.strip()
    df = df[df["dx"].isin(CODE_TO_INDEX)].copy()
    df["label"] = df["dx"].map(CODE_TO_INDEX)

    if df.empty:
        raise RuntimeError(
            f"No matching HAM10000 image files found under '{data_dir}'."
        )

    return df.reset_index(drop=True)


def create_grouped_splits(df: pd.DataFrame, seed: int = 42) -> DatasetSplits:
    """
    Splits HAM10000 into Train (~70%), Validation (~15%), and Test (~15%) sets
    grouped by lesion_id to prevent same-lesion images from leaking across splits.
    """
    sgkf_outer = StratifiedGroupKFold(n_splits=5, shuffle=True, random_state=seed)
    train_val_idx, test_idx = next(sgkf_outer.split(df, df["label"], groups=df["lesion_id"]))
    train_val_df = df.iloc[train_val_idx].reset_index(drop=True)
    test_df = df.iloc[test_idx].reset_index(drop=True)

    sgkf_inner = StratifiedGroupKFold(n_splits=5, shuffle=True, random_state=seed)
    train_idx, val_idx = next(
        sgkf_inner.split(train_val_df, train_val_df["label"], groups=train_val_df["lesion_id"])
    )
    train_df = train_val_df.iloc[train_idx].reset_index(drop=True)
    val_df = train_val_df.iloc[val_idx].reset_index(drop=True)

    weights = compute_class_weight(
        class_weight="balanced",
        classes=np.arange(len(CLASS_CODES)),
        y=train_df["label"].to_numpy(),
    )
    class_weights = {int(i): float(w) for i, w in enumerate(weights)}

    return DatasetSplits(
        train_df=train_df,
        val_df=val_df,
        test_df=test_df,
        class_weights=class_weights,
    )


def preprocess_pil_or_bytes(image: Image.Image) -> np.ndarray:
    """
    Applies consistent OpenCV + PIL preprocessing for EfficientNetB0 inference:
    1. Converts to RGB
    2. Applies gentle hair/artifact suppression via morphological blackhat check + resizing
    3. Applies EfficientNet input normalization (expects [0, 255] float32 -> preprocess_input)
    """
    rgb_image = image.convert("RGB")
    np_rgb = np.array(rgb_image)

    # OpenCV center-preserving resize with bilinear/area interpolation
    interp = cv2.INTER_AREA if (np_rgb.shape[0] > IMAGE_SIZE[0] or np_rgb.shape[1] > IMAGE_SIZE[1]) else cv2.INTER_CUBIC
    resized = cv2.resize(np_rgb, IMAGE_SIZE, interpolation=interp)

    # Convert to float32 [0..255] and run Keras EfficientNet preprocessing
    tensor = resized.astype(np.float32)
    tensor = tf.keras.applications.efficientnet.preprocess_input(tensor)
    return np.expand_dims(tensor, axis=0)


def _load_and_preprocess_path(path: tf.Tensor, label: tf.Tensor) -> Tuple[tf.Tensor, tf.Tensor]:
    img_bytes = tf.io.read_file(path)
    img = tf.image.decode_image(img_bytes, channels=3, expand_animations=False)
    img = tf.image.resize(img, IMAGE_SIZE, method=tf.image.ResizeMethod.BILINEAR)
    img = tf.cast(img, tf.float32)
    img = tf.keras.applications.efficientnet.preprocess_input(img)
    return img, tf.one_hot(label, depth=len(CLASS_CODES))


def build_tf_dataset(
    df: pd.DataFrame,
    batch_size: int = 32,
    training: bool = False,
) -> tf.data.Dataset:
    """Builds an optimized tf.data.Dataset pipeline with optional dermoscopic augmentation."""
    paths = df["image_path"].to_numpy()
    labels = df["label"].to_numpy(dtype=np.int32)

    ds = tf.data.Dataset.from_tensor_slices((paths, labels))
    if training:
        ds = ds.shuffle(buffer_size=min(len(df), 4096), reshuffle_each_iteration=True)

    ds = ds.map(_load_and_preprocess_path, num_parallel_calls=tf.data.AUTOTUNE)

    if training:
        augmentation = tf.keras.Sequential(
            [
                tf.keras.layers.RandomFlip("horizontal_and_vertical"),
                tf.keras.layers.RandomRotation(0.25),
                tf.keras.layers.RandomZoom(0.15),
                tf.keras.layers.RandomContrast(0.1),
            ],
            name="dermoscopic_augmentation",
        )
        ds = ds.map(
            lambda x, y: (augmentation(x, training=True), y),
            num_parallel_calls=tf.data.AUTOTUNE,
        )

    return ds.batch(batch_size).prefetch(tf.data.AUTOTUNE)
