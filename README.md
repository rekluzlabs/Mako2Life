# MakoKolorize - AI Image Colorization

Transform black & white photographs into stunning color images using advanced deep learning. MakoKolorize brings old memories to life with intelligent AI-powered colorization that runs entirely on your Android device.

## Overview

MakoKolorize leverages the cutting-edge DDColor model to automatically predict realistic colors for grayscale images. Whether you're restoring a family heirloom photo, a vintage postcard, or a historical archive image, MakoKolorize delivers professional-quality colorization without requiring internet connectivity or cloud processing.

## Key Features

✨ **State-of-the-Art AI Colorization**
- Uses DDColor model (ICCV 2023) for photo-realistic results
- Dual decoder architecture for superior semantic understanding
- Learnable color tokens optimize results for each image
- Handles complex scenes with multiple objects and diverse contexts
- Works completely offline after initial model download

📱 **On-Device Processing**
- All processing happens locally on your phone
- No cloud uploads, complete privacy protection
- Full control over your images
- Model downloaded once on first launch

🖼️ **Flexible Image Input**
- Import images from any size (small postcards to large family photos)
- Works with gallery or Google Photos
- Intelligent preprocessing handles any resolution
- Full-screen preview before processing

👁️ **Side-by-Side Comparison**
- Original grayscale image displayed above result
- Easy visual comparison of input vs. output
- Zoom and pan support for detailed inspection

💾 **Multiple Export Formats**
- Save results as JPG or PNG
- Choose your preferred format and location
- Original metadata preserved where applicable

⚡ **Fast Processing**
- Typical colorization completes in under 30 seconds
- Real-time progress indicator with percentage completion
- Optimized for Samsung Galaxy S20 and newer devices

🎨 **Versatile Colorization**
- Excellent results on historical photos
- Handles complex indoor and outdoor scenes
- Works on diverse subjects and styles
- Can even recolor stylized artwork and animations

## How It Works

### The Colorization Process

1. **Image Input** → You select a black & white image from your gallery
2. **Preprocessing** → App intelligently resizes and normalizes the image
3. **DDColor Inference** → Dual decoder network generates realistic colors
4. **Output Generation** → Colorized RGB image is assembled and upscaled
5. **Comparison & Save** → View original vs. colorized side-by-side, then save

### The AI Model: DDColor

DDColor is an end-to-end method with dual decoders for image colorization that uses multi-scale visual features to optimize learnable color tokens. The architecture consists of:

**Pixel Decoder** — Restores spatial resolution and preserves fine details  
**Color Decoder** — Uses attention-based color queries to generate semantic-aware colors  
**Colorfulness Loss** — Enhances color vibrancy and visual appeal

The two decoders incorporate to learn semantic-aware color embedding by leveraging the multiscale visual features, producing semantically consistent and visually plausible colorization results.

Unlike traditional colorization methods, DDColor avoids manual color priors and instead learns to understand the semantic relationship between image content and appropriate color distributions. This enables it to handle complex scenes with varied objects and lighting conditions.

## Technical Specifications

**Platform:** Android 9.0+ (minSDK 28)  
**Target:** Android 15 (targetSDK 35)  
**Architecture:** Kotlin + Jetpack Compose (MVVM)  
**Dependency Injection:** Hilt  
**AI Runtime:** ONNX Runtime  
**Model:** DDColor (ICCV 2023) ONNX export  
**Orientation:** Portrait-only  
**UI Mode:** Immersive fullscreen  

**Recommended Devices:**
- Samsung Galaxy S20 and newer
- Phones with 6GB+ RAM
- Snapdragon 865 or equivalent and higher

**Storage Requirements:**
- Variable based on model size (typically 300-500MB)
- ~500MB free space for working directory

## Getting Started

### Installation

1. Download from Google Play (when released)
2. Grant storage permissions when prompted
3. On first launch, the app will download the DDColor model
   - Ensure you have a stable internet connection
   - This is a one-time download; subsequent uses are fully offline

### Basic Usage

1. **Launch** the app
2. **Select Image** → Open gallery/Google Photos
3. **Preview** → View full-screen before proceeding
4. **Confirm** → Click "Done" to import into main screen
5. **Colorize** → Tap "Begin Colorization & Restore" button
6. **Monitor** → Watch progress bar (typically 10-25 seconds)
7. **Compare** → View original (top) and colorized result (bottom)
8. **Save** → Choose JPG or PNG format and location

## Project Structure

```
com.rekluzlabs.makokolorize/
├── data/
│   ├── model/
│   │   ├── ModelRepository.kt          # Model download, caching, loading
│   │   └── ModelDownloadManager.kt     # Download progress tracking
│   └── image/
│       └── ImageRepository.kt          # Gallery integration, file operations
├── domain/
│   └── ColorizeUseCase.kt              # Core colorization logic
├── ui/
│   ├── screens/
│   │   ├── SplashScreen.kt             # Model download UI
│   │   ├── PickerScreen.kt             # Gallery selection & preview
│   │   ├── MainScreen.kt               # Image import & processing
│   │   └── ResultScreen.kt             # Comparison & save
│   └── components/
│       ├── ProgressIndicator.kt
│       └── ImageComparison.kt
├── MainActivity.kt
└── App.kt                              # Hilt setup
```

## Privacy & Security

🔒 **Your Privacy is Protected**
- All image processing happens locally on your device
- Images never leave your phone
- No tracking, no analytics, no cloud uploads
- No internet required after initial model download

## Advantages Over Previous Methods

**vs. DeOldify:**
- ✅ Better semantic understanding (dual decoder architecture)
- ✅ More natural color distribution (learnable color tokens)
- ✅ Fewer color artifacts (optimized training with colorfulness loss)
- ✅ Better results on complex scenes with multiple objects
- ✅ Newer research (ICCV 2023 vs. earlier models)

**vs. Other Colorization Methods:**
- ✅ State-of-the-art performance on multiple benchmarks
- ✅ Handles diverse image types and styles
- ✅ No manual color priors required
- ✅ Optimized for mobile inference

## Limitations & Known Issues

⚠️ **What It Can Do Well:**
- Skin tones and faces (semantic understanding)
- Natural scenes (skies, grass, water)
- Clothing and everyday objects
- Historical and vintage photographs
- Complex multi-object scenes

⚠️ **What It Struggles With:**
- Completely ambiguous images (very rare)
- Heavily degraded/extremely low contrast originals
- Very fine details (limited by ONNX model size)

## Performance Benchmarks

Tested on Galaxy S20 (8GB RAM, Snapdragon 865):
- Model Download: ~2-5 minutes (on typical home WiFi)
- Model Load: ~50-100ms (cached)
- Inference (512×512): ~8-15 seconds
- Upscaling: ~500-1000ms
- **Total End-to-End:** ~10-20 seconds

Performance on older/mid-range devices will be slower (20-30 seconds typical).

## Future Roadmap

🚀 **Planned Enhancements (v1.1+)**
- Real-time preview with different DDColor variants
- Custom color adjustment tools
- Batch processing for multiple images
- Enhanced upscaling options
- Undo/redo history
- Before/after slider widget
- Offline model variant selection
- Social sharing integration

## Contributing

This project is maintained by Rekluz Labs as part of the MakoKolorize suite of image restoration tools.

For bug reports, feature requests, or technical questions:
- **GitHub Issues:** [rekluzlabs/MakoKolorize/issues](https://github.com/rekluzlabs/MakoKolorize/issues)
- **Contact:** See Rekluz Labs website

## License

This project is open source and available under the **GPL v3 License**.

The DDColor model is based on research from Alibaba's DAMO Academy and published in ICCV 2023. See the original DDColor repository for licensing details.

## Credits

**Built with:**
- [DDColor: Towards Photo-Realistic Image Colorization via Dual Decoders](https://github.com/piddnad/DDColor) - Colorization model by Kang et al.
- [ONNX Runtime](https://onnxruntime.ai/) - Model inference
- [Jetpack Compose](https://developer.android.com/compose) - UI framework
- [Hilt](https://dagger.dev/hilt/) - Dependency injection

## Citation

If you use DDColor in your research, please cite:

```bibtex
@inproceedings{kang2023ddcolor,
  title={DDColor: Towards Photo-Realistic Image Colorization via Dual Decoders},
  author={Kang, Xiaoyang and Yang, Tao and Ouyang, Wenqi and Ren, Peiran and Li, Lingzhi and Xie, Xuansong},
  booktitle={Proceedings of the IEEE/CVF International Conference on Computer Vision},
  pages={328--338},
  year={2023}
}
```

## Support

Have questions or issues? Check out:
- [DDColor Documentation](https://github.com/piddnad/DDColor)
- [Rekluz Labs Website](https://rekluzlabs.github.io)
- Report issues on GitHub

---

**MakoKolorize** - Restoring color to your memories, powered by cutting-edge AI.
