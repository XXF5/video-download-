# Video Downloader Pro - Android Application

تطبيق Android قوي لتحميل الفيديوهات من الإنترنت.

## المميزات

### ✅ ميزات رئيسية
- **اكتشاف تلقائي للفيديوهات** من أي صفحة ويب
- **تحميل من منصات متعددة**: YouTube, TikTok, Instagram, Facebook, Twitter, Vimeo, Dailymotion, Reddit
- **دعم جودات متعددة**: 4K, 2K, 1080p, 720p, 480p, 360p
- **استخراج الصوت فقط**: تحويل الفيديو إلى MP3
- **تحميل متعدد**: تحميل عدة فيديوهات في نفس الوقت
- **إدارة التحميلات**: إيقاف، استئناف، إلغاء

### ✅ ميزات متقدمة
- **متصفح مدمج**: تصفح واكتشاف الفيديوهات
- **كشف الفيديو من الحافظة**: لصق تلقائي للروابط
- **حساب السرعة والوقت المتبقي**
- **إشعارات التحميل**
- **واجهة Material Design 3**

## هيكل المشروع

```
app/
├── src/main/
│   ├── java/com/videodownloader/
│   │   ├── ui/
│   │   │   ├── activities/          # الأنشطة
│   │   │   ├── adapters/            # المحولات
│   │   │   └── viewmodels/          # ViewModels
│   │   ├── core/
│   │   │   ├── detector/            # كاشف الفيديوهات
│   │   │   ├── downloader/          # مدير التحميل
│   │   │   └── extractor/           # مستخرج الفيديو
│   │   ├── data/
│   │   │   ├── database/            # قاعدة البيانات
│   │   │   ├── models/              # النماذج
│   │   │   └── repository/          # المستودعات
│   │   ├── di/                      # حقن التبعيات
│   │   └── utils/                   # الأدوات
│   ├── res/                         # الموارد
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## التقنيات المستخدمة

| التقنية | الاستخدام |
|---------|----------|
| Kotlin | لغة البرمجة |
| Hilt | حقن التبعيات |
| Room | قاعدة البيانات |
| OkHttp | الشبكات |
| Coroutines | العمليات غير المتزامنة |
| ExoPlayer | تشغيل الفيديو |
| JSoup | تحليل HTML |
| Material Design 3 | واجهة المستخدم |

## بناء المشروع

### المتطلبات
- Android Studio Hedgehog أو أحدث
- JDK 17
- Android SDK 34

### الخطوات

1. **فتح المشروع في Android Studio**

2. **Sync Gradle**
```bash
./gradlew sync
```

3. **Build APK**
```bash
./gradlew assembleRelease
```

4. **APK سيكون في:**
```
app/build/outputs/apk/release/app-release.apk
```

## التوقيع (Signing)

لإنشاء APK موقّع للنشر:

1. إنشاء keystore:
```bash
keytool -genkey -v -keystore video-downloader.keystore -alias video-downloader -keyalg RSA -keysize 2048 -validity 10000
```

2. إضافة إلى `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("video-downloader.keystore")
            storePassword = "your_password"
            keyAlias = "video-downloader"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## ملاحظات مهمة

⚠️ **تنبيه**: هذا المشروع للأغراض التعليمية. تأكد من:
- احترام حقوق النشر
- الالتزام بشروط استخدام المنصات
- استخدام المحتوى المحمّل للأغراض الشخصية فقط

## الترخيص

MIT License - استخدم بحرية للأغراض التعليمية والشخصية.
