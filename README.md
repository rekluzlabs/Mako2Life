# Mako2Life- AI Image Colorization

Transform black & white photographs into stunning color images using advanced deep learning. Mako2Life offers to bring old memories to life with intelligent AI-powered image processing that runs entirely on your Android device.

## Note: The site for the AI Models are now being hosted on Hugging Face site. All new builds will use this link going forward starting June 6, 2026

## Overview

Mako2Life leverages the cutting-edge AI models to automatically predict realistic colors for black and white images and to repair and upscale images Whether you're restoring a family heirloom photo, a vintage postcard, or a historical archive image, makokolorize delivers professional-quality photo restoration without requiring internet connectivity or cloud processing.

<img width="300" height="640" alt="image2" src="https://github.com/user-attachments/assets/5eb4ee69-1346-4106-8819-bd63d0de5703" />
<img width="300" height="640" alt="image3" src="https://github.com/user-attachments/assets/eaf8f353-9a37-4d06-870d-8a94b239431b" />
<img width="300" height="640" alt="image1" src="https://github.com/user-attachments/assets/c86b0dda-1886-4e5b-8890-7fe7b8cb1b12" />


## Key Features

 **State-of-the-Art AI Colorization**
- Uses DDColor model (ICCV 2023) for photo-realistic results
- Codeformer to enhance faces
- SCUNet to remove grain and noise from images
- Real-ESRGAN to improve quality and upscale image
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

### Mako2Life User Manual
========================

Welcome to Mako2Life, your all-in-one AI-powered photo restoration and colorization suite. This guide will help you transform your old, blurry, or black-and-white photos into vibrant, high-definition memories.

---

1. GETTING STARTED
------------------
When you open the app, you'll start at the Home Screen.
- Tap "Select Photo" to choose an image from your device's gallery.
- You can also access Settings to manage app preferences.

2. THE RESTORE SCREEN
---------------------
Once a photo is selected, you are presented with several quick options:

A. Auto Restore (One-Click)
   The simplest way to get results. This enables all AI models with balanced settings to automatically clean, colorize, sharpen, and enlarge your photo in one go.

B. Individual Toggles
   - Denoise: Removes grain and digital noise from low-light or old photos.
   - Colorize: Adds natural-looking color to black-and-white images.
   - Face Restore: Specifically fixes blurry or damaged faces.
   - Upscale: Increases the photo's resolution (makes it 2x larger and sharper).

3. ADVANCED SETTINGS (PRE-FLIGHT)
---------------------------------
For power users, tap the "Advanced" button to fine-tune how the AI processes your image:

AI Model Parameters:
- DDColor Input Size: Choose how much detail the AI "sees" for colorization.
  - 128px (Ultra Fast): Best for quick previews.
  - 256px (Fast): Balanced default.
  - 512px (Detail): Best for complex scenes with many objects.
- Color Vibrancy: Adjust how "vivid" the colors appear (0.5x to 2.0x).
- SCUNet Denoising Strength: Control how aggressively the grain is smoothed out (0% to 100%).
- Advanced Noise Removal: Runs a double-pass for extremely grainy photos.
- CodeFormer Fidelity: Controls the "AI interference" on faces.
  - Lower (near 0.0): Strongest repair, but faces may look "AI-generated."
  - Higher (near 1.0): Keeps more of the original person's features.
- High Accuracy Mode (ML Kit): Slower, but better at finding small or partially hidden faces.

4. THE PROCESSING STAGE
-----------------------
After clicking "Restore," you will see a live progress overlay. Makokolorize processes your image in a professional pipeline:
1. DDColor (Colorize)
2. RealESRGAN (Upscale)
3. CodeFormer (Face Restore)
4. SCUNet (Final Denoise)

5. THE RESULT SCREEN
--------------------
Once finished, you can:
- Compare: Drag the slider left and right to see a "Before vs After" comparison.
- Save: Export your final image as a JPEG (with quality control) or a lossless PNG.
- Share: Send your masterpiece directly to social media or friends.

6. MAKO EDIT (MANUAL TOUCH-UPS)
-------------------------------
Tap the "MAKO Edit" button to manually fine-tune the AI's work:
- Brightness/Contrast: Adjust light and depth.
- Saturation/Warmth/Tint: Perfect the color temperature and mood.
- Highlights/Shadows: Recover details hidden in bright skies or dark corners.
- Sharpness: Add an extra "crunch" to the edges.
- Crop: Reframe your photo by dragging the corners of the interactive box.

---
Thank you for using Mako2Life!
Relive your memories in high definition.

1. Download from Google Play (when released)
2. Grant storage permissions when prompted
3. On first launch, the app will download the DDColor model
   - Ensure you have a stable internet connection
   - This is a one-time download; subsequent uses are fully offline


## Privacy & Security

**Your Privacy is Protected**
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
- grayscale images not converting to color with DDcolor alone (needs to run with 2 AI models to work)

## Performance

The app is optimized for mobile inference using ONNX Runtime. Performance may vary depending on your device's hardware capabilities. It is not recommended to use with older or mid-range devices as they may experience longer processing times and overheating issues.

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

This project is maintained by Rekluz Labs as a one stop offline image restoration tool.

For bug reports, feature requests, or technical questions:
- **GitHub Issues:** [rekluzlabs/mako2life/issues](https://github.com/rekluzlabs/mako2life/issues)
- **Contact:** contact via email rekluzlabs@gmail.com

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

**mako2life** - Restoring life and color to your memories, powered by cutting-edge AI.
