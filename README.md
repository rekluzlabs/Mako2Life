# MakoKolorize - AI Image Colorization

Transform black & white photographs into stunning color images using advanced deep learning. **MakoKolorize** brings old memories to life with intelligent AI-powered colorization that runs entirely on your Android device.

** Note: The app is currently undergoing changes. This will involve adding support for Denoise, Codeformer as well as RealESRGAN to the code. This will make MakoKolorize a one stop shop for image restoration.
---

## Overview

MakoKolorize leverages the cutting-edge **DDColor** model to automatically predict realistic colors for grayscale images.

Whether you're restoring a family heirloom photo, a vintage postcard, or a historical archive image, MakoKolorize delivers professional-quality colorization without requiring internet connectivity or cloud processing.

<p align="center">
  <img src="https://github.com/user-attachments/assets/5eb4ee69-1346-4106-8819-bd63d0de5703" width="300" />
  <img src="https://github.com/user-attachments/assets/eaf8f353-9a37-4d06-870d-8a94b239431b" width="300" />
  <img src="https://github.com/user-attachments/assets/c86b0dda-1886-4e5b-8890-7fe7b8cb1b12" width="300" />
</p>

---

# Key Features

##  State-of-the-Art AI Colorization

* Uses DDColor model (ICCV 2023) for photo-realistic results
* Dual decoder architecture for superior semantic understanding
* Learnable color tokens optimize results for each image
* Handles complex scenes with multiple objects and diverse contexts
* Works completely offline after initial model download

##  On-Device Processing

* All processing happens locally on your phone
* No cloud uploads, complete privacy protection
* Full control over your images
* Model downloaded once on first launch

##  Flexible Image Input

* Import images of virtually any size
* Works with Gallery and Google Photos
* Intelligent preprocessing handles any resolution
* Full-screen preview before processing

##  Side-by-Side Comparison

* Original grayscale image displayed above result
* Easy visual comparison of input versus output
* Zoom and pan support for detailed inspection

##  Multiple Export Formats

* Save results as JPG or PNG
* Choose your preferred format and location
* Original metadata preserved where applicable

##  Fast Processing

* Optimized for mobile devices
* Real-time progress indicator
* Efficient performance on modern Android hardware

##  Versatile Colorization

* Excellent results on historical photographs
* Handles indoor and outdoor scenes
* Works across diverse subjects and styles
* Can recolor stylized artwork and animations

---

# How It Works

## The Colorization Process

1. **Image Input** → Select a black & white image from your gallery.
2. **Preprocessing** → The image is resized and normalized.
3. **DDColor Inference** → The AI predicts realistic colors.
4. **Output Generation** → A colorized RGB image is created and upscaled.
5. **Comparison & Save** → Compare before and after, then save your result.

## The AI Model: DDColor

DDColor is an end-to-end image colorization method featuring dual decoders and multi-scale visual feature learning.

MakoKolorize uses an optimized mobile-friendly version of the model for fast on-device performance.

---

# Getting Started

> **Note:** MakoKolorize is currently in early beta. Some planned features may not yet be available.

## Installation

1. Download from Google Play (when available).
2. Grant storage permissions when prompted.
3. On first launch, the app downloads the DDColor model.

   * Ensure you have a stable internet connection.
   * This is a one-time download.
   * All future processing is fully offline.

## Basic Usage

1. Launch the app.
2. Select an image from Gallery or Google Photos.
3. Preview the image full-screen.
4. Tap **Done** to import it.
5. Press **Begin Colorization**.
6. Monitor progress.
7. Compare original and colorized results.
8. Save as JPG or PNG.

---

# Privacy & Security

##  Your Privacy is Protected

* All processing occurs locally on your device
* Images never leave your phone
* No tracking
* No analytics
* No cloud uploads
* No internet required after model download

---

# Advantages Over Previous Methods

## Compared to DeOldify

* ✅ Better semantic understanding through dual decoders
* ✅ More natural color distribution using learnable color tokens
* ✅ Reduced color artifacts
* ✅ Better handling of complex scenes
* ✅ Based on newer ICCV 2023 research

## Compared to Other Colorization Methods

* ✅ State-of-the-art benchmark performance
* ✅ Handles diverse image types and styles
* ✅ No manual color hints required
* ✅ Optimized for mobile inference

---

# Limitations & Known Issues

## ✅ Performs Well On

* Skin tones and faces
* Natural environments
* Clothing and everyday objects
* Historical photographs
* Complex multi-object scenes

##  Challenges

* Completely ambiguous source material
* Severely degraded originals
* Extremely low-contrast images
* Very fine details limited by mobile model size

---

# Performance

MakoKolorize is optimized using **ONNX Runtime** for efficient mobile inference.

Processing times vary depending on device hardware. Modern flagship devices will generally produce results significantly faster than older or entry-level hardware.

---

# Future Roadmap

## 🚀 Planned Enhancements (v1.1+)

* Real-time preview modes
* Additional DDColor variants
* Custom color adjustment tools
* Batch processing
* Enhanced upscaling options
* Undo / Redo history
* Before-and-after slider comparison
* Offline model selection
* Social sharing integration

---

# Contributing

This project is maintained by **Rekluz Labs** as part of the MakoKolorize image restoration suite.

For bug reports, feature requests, or technical questions:

* GitHub Issues: https://github.com/rekluzlabs/makokolorize/issues
* Contact: See the Rekluz Labs website

---

# License

This project is released under the **GPL v3 License**.

The DDColor model is based on research from Alibaba DAMO Academy and published at ICCV 2023. Refer to the original DDColor repository for licensing details.

---

# Credits

### Built With

* DDColor: Towards Photo-Realistic Image Colorization via Dual Decoders
* ONNX Runtime
* Jetpack Compose
* Hilt

### Research Paper

**DDColor: Towards Photo-Realistic Image Colorization via Dual Decoders**

Kang, Xiaoyang; Yang, Tao; Ouyang, Wenqi; Ren, Peiran; Li, Lingzhi; Xie, Xuansong

Published in the Proceedings of the IEEE/CVF International Conference on Computer Vision (ICCV), 2023.

---

# Citation

```bibtex
@inproceedings{kang2023ddcolor,
  title={DDColor: Towards Photo-Realistic Image Colorization via Dual Decoders},
  author={Kang, Xiaoyang and Yang, Tao and Ouyang, Wenqi and Ren, Peiran and Li, Lingzhi and Xie, Xuansong},
  booktitle={Proceedings of the IEEE/CVF International Conference on Computer Vision},
  pages={328--338},
  year={2023}
}
```

---

# Support

Have questions or issues?

* DDColor Documentation: https://github.com/piddnad/DDColor
* Rekluz Labs Website: https://rekluzlabs.github.io
* Report issues via GitHub Issues

---

## MakoKolorize

**Restoring color to your memories, powered by cutting-edge AI.**
