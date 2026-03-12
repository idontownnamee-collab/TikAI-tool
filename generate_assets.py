import os, struct, zlib

print("=== Fixing Gradle files ===")

# Fix settings.gradle
with open("settings.gradle", "w") as f:
    f.write("""pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "TikAI Tool"
include ':app'
""")
print("Fixed settings.gradle")

# Fix root build.gradle - NO allprojects block
with open("build.gradle", "w") as f:
    f.write("""buildscript {
    ext.kotlin_version = "1.9.22"
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:8.2.2"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
task clean(type: Delete) {
    delete rootProject.buildDir
}
""")
print("Fixed build.gradle")

# Fix app/build.gradle - all deps, no repositories block
with open("app/build.gradle", "w") as f:
    f.write("""plugins {
    id 'com.android.application'
    id 'kotlin-android'
}
android {
    namespace 'com.tiktokaitool'
    compileSdk 34
    defaultConfig {
        applicationId "com.tiktokaitool"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    buildTypes {
        release { minifyEnabled false }
    }
    buildFeatures { viewBinding true }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
}
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    implementation 'com.google.gson:gson:2.10.1'
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
    implementation 'io.noties.markwon:core:4.6.2'
}
""")
print("Fixed app/build.gradle")

print("\n=== Generating drawable assets ===")

os.makedirs('app/src/main/res/drawable', exist_ok=True)
os.makedirs('app/src/main/res/mipmap-anydpi-v26', exist_ok=True)

drawables = {
  'ic_settings': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z"/></vector>',
  'ic_back': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/></vector>',
  'ic_download': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"/></vector>',
  'ic_ai_finder': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/></vector>',
  'ic_close': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"/></vector>',
  'ic_arrow_right': '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z"/></vector>',
}

for name, xml in drawables.items():
    with open(f'app/src/main/res/drawable/{name}.xml', 'w') as f:
        f.write(xml)
    print(f'Created {name}.xml')

launcher_xml = '<?xml version="1.0" encoding="utf-8"?><adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/tiktok_red"/><foreground android:drawable="@drawable/ic_ai_finder"/></adaptive-icon>'
with open('app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml', 'w') as f:
    f.write(launcher_xml)
with open('app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml', 'w') as f:
    f.write(launcher_xml)

def make_png(size, r, g, b):
    def chunk(name, data):
        c = zlib.crc32(name + data) & 0xffffffff
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', c)
    ihdr = struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)
    raw = b''.join(b'\x00' + bytes([r, g, b]) * size for _ in range(size))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')

for folder, size in [('mipmap-mdpi',48),('mipmap-hdpi',72),('mipmap-xhdpi',96),('mipmap-xxhdpi',144)]:
    p = f'app/src/main/res/{folder}'
    os.makedirs(p, exist_ok=True)
    d = make_png(size, 254, 44, 85)
    with open(f'{p}/ic_launcher.png','wb') as f: f.write(d)
    with open(f'{p}/ic_launcher_round.png','wb') as f: f.write(d)
    print(f'Created {folder} icons')

print("\nAll done!")
