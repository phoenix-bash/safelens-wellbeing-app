# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

- **Primary User (Child)**: School-age children and teenagers using mobile devices for study, communication, and entertainment. They interact with the on-device interface to view their daily study focus goals, screen habit stats, and protection status.
- **Secondary User (Parent)**: Parents who desire responsible oversight and safety from cyberbullying, academic distraction, and explicit content without intruding into their child's private personal life.

## Product Purpose

Safelens Wellbeing helps families foster healthy digital habits and protects children from harm (cyberbullying/trash talk, excessive study distractions, and explicit media) while maintaining mutual trust through strict, non-negotiable privacy boundaries.

## Positioning

Unlike intrusive surveillance or stalkerware tools that harvest keystrokes, secret mic recordings, camera feeds, and continuous GPS tracks, Safelens operates with **radical privacy and minimal overhead**:
- Strictly **ZERO** Camera, Microphone, or GPS/Location tracking.
- Sensitive scans (language sentiment, image classification) execute **on-device**; raw messages and personal family photos never leave the child's phone.
- High-resilience, battery-optimized architecture ($<2\%$ battery drain/day) specifically engineered to survive aggressive Android OEM task killers on Poco (HyperOS/MIUI) and Realme (ColorOS).

## Operating Context

- **Child On-Device Surface**: A native Android mobile app featuring a clean, empowering wellbeing dashboard that celebrates focus hours and displays active protection indicators.
- **Parent Surface**: Remote Parent Dashboard (Cloud API + FCM for real-time alerts and scheduled wellbeing summaries).
- **OEM Ecosystems**: Optimized specifically for modern Xiaomi HyperOS / Poco and Oppo / Realme ColorOS devices, accommodating proprietary background battery managers.

## Capabilities and Constraints

- **Confirmed Capabilities**:
  - Event-driven Notification Monitoring (categorizes messaging/social apps, tracks study-hour distraction volume, flags aggressive trash-talk keywords without storing message bodies).
  - Call Duration & Log Tracker (records contact number/name, direction, timestamp, and duration).
  - On-Device Media Safety Filter (inspects newly captured or downloaded images locally for explicit/NSFW content; hashes files without uploading raw photos).
  - Headless Device Support Service (`com.core.devicesupport`) companion running in the background without cluttering the launcher app drawer.
  - In-App GitHub Downloader & FileProvider Installer with pre-installation version checking.
- **Non-Negotiable Constraints**:
  - Absolute ban on Camera access (`android.permission.CAMERA`).
  - Absolute ban on Microphone access (`android.permission.RECORD_AUDIO`).
  - Absolute ban on GPS / Location access (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
  - No continuous background polling or raw 24/7 WebSockets to protect battery life.

## Brand Commitments

- **Name**: Safelens Wellbeing.
- **Voice**: Respectful, encouraging, transparent, and calm. Avoid authoritarian or punitive terminology.
- **Visual Identity**: Material Design 3, trustworthy blues and emerald accents, clear card hierarchy, and full dynamic theming (first-class Dark Theme support).

## Evidence on Hand

- Multi-module Android project compiled and verified:
  - `:safelens-child` (`com.safelens.child`)
  - `:safelens-support` (`com.core.devicesupport`)
  - `:safelens-core` (shared models, IPC contract)
- Hardware verification captured on connected POCO phone (`2412DPC0AI`, Android 16 / Xiaomi HyperOS 3.0).

## Product Principles

1. **Privacy Over Paranoia**: Protect without violating human dignity. If safety can be achieved without collecting personal data, collect nothing.
2. **On-Device First**: Analyze text and media locally on the phone's NPU/CPU; never turn the child's phone into an open stream of private data to cloud servers.
3. **Zero-Polling Battery Preservation**: Depend solely on native OS event hooks (`NotificationListenerService`, `ContentObserver`, `TelephonyCallback`); keep mobile radios idle $99\%$ of the time.
4. **Transparent Partnership**: The child and parent understand exactly what the application does. No disguised spyware or unauthorized persistence tricks.

## Accessibility & Inclusion

- Material 3 color roles respecting system-wide Dark Theme and Dynamic Color (Material You).
- 48×48 dp touch targets with clear tap states.
- Scalable sp typography compatible with Android accessibility font scaling.
