# Mako2Life- AI Image Colorization

Transform black & white photographs into stunning color images using advanced deep learning. makokolorize brings old memories to life with intelligent AI-powered image processing that runs entirely on your Android device.

## Note: The site for the AI Models are now being hosted on Hugging Face site. All new builds will use this link going forward starting June 6, 2026

## Overview

Mako2Life leverages the cutting-edge AI models to automatically predict realistic colors for black and white images and to repair and upscale images Whether you're restoring a family heirloom photo, a vintage postcard, or a historical archive image, makokolorize delivers professional-quality photo restoration without requiring internet connectivity or cloud processing.

<img width="300" height="640" alt="image2" src="https://github.com/user-attachments/assets/5eb4ee69-1346-4106-8819-bd63d0de5703" />
<img width="300" height="640" alt="image3" src="https://github.com/user-attachments/assets/eaf8f353-9a37-4d06-870d-8a94b239431b" />
<img width="300" height="640" alt="image1" src="https://github.com/user-attachments/assets/c86b0dda-1886-4e5b-8890-7fe7b8cb1b12" />


## Key Features

 **State-of-the-Art AI Colorization**
- Uses DDColor model (ICCV 2023) for photo-realistic results
- Dual decoder architecture for superior semantic understanding
- Learnable color tokens optimize results for each image
- Handles complex scenes with multiple objects and diverse contexts
- Works completely offline after initial model download

**On-Device Processing**
- All processing happens locally on your phone
- No cloud uploads, complete privacy protection
- Full control over your images
- Model downloaded once on first launch

**Flexible Image Input**
- Import images from any size (small postcards to large family photos)
- Works with gallery or Google Photos
- Intelligent preprocessing handles any resolution
- Full-screen preview before processing

**Side-by-Side Comparison**
- Original grayscale image displayed above result
- Easy visual comparison of input vs. output
- Zoom and pan support for detailed inspection

**Multiple Export Formats**
- Save results as JPG or PNG
- Choose your preferred format and location
- Original metadata preserved where applicable

**Fast Processing**
- Optimized for mobile devices with fast inference times
- Real-time progress indicator with percentage completion
- Efficient performance on modern Android devices

 **Versatile Colorization**
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

DDColor is an end-to-end method with dual decoders for image colorization that uses multi-scale visual features to optimize learnable color tokens.

The app uses an optimized tiny version of the model for faster on-device performance.

## Getting Started
as this is an early beta, not all features are yet available or implemented. 
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
5. **Colorize** → Tap "Begin Colorization" button
6. **Monitor** → Watch progress bar
7. **Compare** → View original (top) and colorized result (bottom)
8. **Save** → Choose JPG or PNG format and location


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

## Performance

The app is optimized for mobile inference using ONNX Runtime. Performance may vary depending on your device's hardware capabilities. Older or mid-range devices may experience longer processing times.

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

This project is maintained by Rekluz Labs as part of the makokolorize suite of image restoration tools.

For bug reports, feature requests, or technical questions:
- **GitHub Issues:** [rekluzlabs/makokolorize/issues](https://github.com/rekluzlabs/makokolorize/issues)
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
-  [DDColor Documentation](https://github.com/piddnad/DDColor)
-  [Rekluz Labs Website](https://rekluzlabs.github.io)
-  Report issues on GitHub

---

**makokolorize** - Restoring color to your memories, powered by cutting-edge AI.
