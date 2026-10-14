# NeonMusic

**NeonMusic**, Android cihazdaki müzik dosyalarını keşfetmek ve çalmak için geliştirilen, koyu temalı ve neon vurgulu bir müzik çalar projesidir. Arayüz HTML, CSS ve JavaScript ile; Android'e özgü medya erişimi ve oynatma katmanı ise Kotlin ile hazırlanmıştır.

> **Proje durumu:** Bu depo geliştirme aşamasındadır. 14 Ekim 2026 tarihli QA raporunda JavaScript sözdizimi, XML, ZIP bütünlüğü ve bazı arama kontrolleri başarılıdır. Ancak tam Android derlemesi, APK üretimi ve gerçek cihaz testleri bu ortamda doğrulanamamıştır. Ayrıntılar için [`QA_REPORT_2026-10-14.txt`](QA_REPORT_2026-10-14.txt) dosyasına bakın.

## İçindekiler

- [Özellikler](#özellikler)
- [Teknoloji yığını](#teknoloji-yığını)
- [Mimari](#mimari)
- [Gereksinimler](#gereksinimler)
- [Projeyi çalıştırma ve derleme](#projeyi-çalıştırma-ve-derleme)
- [Android izinleri](#android-izinleri)
- [Test ve kalite kontrolleri](#test-ve-kalite-kontrolleri)
- [Proje yapısı](#proje-yapısı)
- [Bilinen sınırlamalar](#bilinen-sınırlamalar)
- [Katkıda bulunma](#katkıda-bulunma)
- [Lisans](#lisans)

## Özellikler

Projede bulunan altyapı ve uygulama hedefleri şunlardır:

- **Cihazdaki müzik kitaplığı:** Android `MediaStore` üzerinden ses dosyalarını tarama.
- **Klasör seçimi:** Android Storage Access Framework (SAF) ile kullanıcı tarafından seçilen klasörlere erişim.
- **Yerel oynatma:** AndroidX Media3 / ExoPlayer tabanlı oynatma servisi.
- **Arka plan oynatma:** `MediaSessionService` entegrasyonu ve medya bildirimleri için Android altyapısı.
- **Müzik bilgileri ve kapaklar:** Parça metaverileri ve albüm kapağı yönetimi.
- **Arama:** Şarkı bilgileri üzerinde Unicode karakterleri ve Türkçe karakterleri gözeten arama altyapısı.
- **Kitaplık verileri:** Room veritabanı ve DAO katmanı üzerinden müzik, liste, klasör, geçmiş ve ayar verilerini yönetmeye yönelik yapı.
- **Ses efektleri:** Android ses efektleri/equalizer yönetimi için yerel katman.
- **Web tabanlı arayüz:** HTML, CSS ve JavaScript ile hazırlanan arayüz; Kotlin ile JavaScript arasında köprü.

> Bu liste, kaynak kodda bulunan altyapıyı ve proje kapsamını açıklar; her özelliğin tüm Android sürümlerinde ve cihazlarda uçtan uca test edildiği anlamına gelmez. Doğrulanmış kontroller için QA raporunu inceleyin.

## Teknoloji yığını

| Katman | Teknoloji |
|---|---|
| Android uygulaması | Kotlin, Android SDK |
| Arayüz | HTML, CSS, JavaScript, Android WebView |
| Ses oynatma | AndroidX Media3 / ExoPlayer |
| Medya oturumu | AndroidX Media3 Session |
| Cihaz kitaplığı | Android MediaStore, Storage Access Framework |
| Yerel veri | Room, Kotlin Coroutines |
| Kod üretimi | KSP |
| Derleme sistemi | Gradle, Kotlin DSL |
| Otomatik iş akışı | GitHub Actions |

## Mimari

NeonMusic, WebView arayüzünü Android'in yerel medya özelliklerinden ayıran hibrit bir yapıya sahiptir.

1. **Web arayüzü** kullanıcı etkileşimlerini gösterir ve JavaScript üzerinden yerel köprüye istek gönderir.
2. **`MusicBridge`** arayüz ile Kotlin katmanı arasındaki işlemleri koordine eder.
3. **`MediaStoreScanner`** cihazın medya kitaplığını tarar; SAF ile seçilen klasörler için de tarama akışı bulunur.
4. **`MusicPlaybackService`** Media3 ile oynatma ve medya oturumu işlerini yürütür.
5. **Room** kitaplıkla ilişkili kalıcı verilerin saklanması için kullanılır.
6. **`AlbumArtManager` ve `SearchEngine`** kapak yönetimi ve arama davranışlarını destekler.

Başlıca kaynak sınıfları `app/src/main/java/com/neonmusic/player/` altındadır. Web arayüzü `app/src/main/assets/web/` içinde bulunur.

## Gereksinimler

Projeyi geliştirme ortamında derlemek için:

- JDK 21
- Android SDK ve projede tanımlanan Android platformu/build-tools bileşenleri
- İnternet bağlantısı (Gradle ve bağımlılıkların ilk indirilmesi için)
- Android Studio veya Gradle Wrapper kullanabilen bir terminal

Projenin `minSdk` değeri **24**, `targetSdk` değeri **36** olarak yapılandırılmıştır. Gerçek cihaz davranışı Android sürümüne, üreticiye ve kullanıcı izinlerine göre değişebilir.

## Projeyi çalıştırma ve derleme

### 1. Kaynak kodu açın

Depoyu klonlayın veya ZIP arşivini çıkarın. Proje kök dizininde `settings.gradle.kts`, `build.gradle.kts` ve `gradlew` dosyaları bulunmalıdır.

### 2. Debug APK oluşturun

Linux veya macOS terminalinde proje kök dizininden:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

Windows komut satırında:

```bat
gradlew.bat assembleDebug
```

Başarılı derleme sonrasında debug APK normalde şu konumda oluşur:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Testleri çalıştırın

```bash
./gradlew testDebugUnitTest
```

### 4. Android Lint çalıştırın

```bash
./gradlew lintDebug
```

### 5. Release derlemesi

```bash
./gradlew assembleRelease
```

Release derlemesinin başarılı olması tek başına mağazaya yüklemeye hazır, imzalanmış bir APK üretildiğini garanti etmez. Yayın için imzalama yapılandırmasını, küçültme kurallarını ve cihaz testlerini ayrıca doğrulayın.

## Android izinleri

Uygulama manifestinde tanımlı başlıca izinler:

- `READ_MEDIA_AUDIO`: Android 13 ve üzerindeki cihazlarda ses kitaplığına erişim.
- `READ_EXTERNAL_STORAGE`: Eski Android sürümleri için sınırlı depolama izni.
- `POST_NOTIFICATIONS`: Desteklenen Android sürümlerinde bildirim gösterebilmek için.
- `FOREGROUND_SERVICE` ve `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Arka plan medya oynatma servisi için.
- `INTERNET` ve `ACCESS_NETWORK_STATE`: Ağ erişimi kullanan özellikler için.

İzinler gerektiğinde kullanıcıdan istenmelidir. Bildirim izni, ses dosyalarına erişim izninin yerine geçmez. Kullanıcı izin vermediğinde uygulamanın bu durumu doğru ele alması gerekir.

## Test ve kalite kontrolleri

Depodaki GitHub Actions iş akışı aşağıdaki adımları çalıştıracak şekilde yapılandırılmıştır:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- `assembleRelease`

14 Ekim 2026 tarihli [`QA_REPORT_2026-10-14.txt`](QA_REPORT_2026-10-14.txt) raporuna göre:

- ZIP bütünlüğü kontrol edildi.
- `app.js` ve `i18n.js` için JavaScript sözdizimi kontrolleri geçti.
- XML dosyaları ayrıştırıldı.
- JavaScript–Kotlin köprü çağrılarının adları statik olarak kontrol edildi.
- `SearchEngine` için bağımsız Kotlin kontrolleri çalıştırıldı.
- Gradle testleri, bu ortamda Gradle 9.3.1 indirilemediği için tamamlanamadı.

**Önemli:** Tam Android/Kotlin derlemesi, Android Lint, Room/KSP kod üretimi, tüm birim testleri, APK oluşturma, release küçültücü davranışı ve fiziksel cihaz testleri raporda doğrulanmamıştır. Yayın öncesi bu kontrollerin internet erişimi olan Android geliştirme ortamında tamamlanması gerekir.

## Proje yapısı

```text
NeonMusic/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/web/       # HTML, CSS ve JavaScript arayüzü
│   │   │   ├── java/com/neonmusic/player/
│   │   │   │   ├── bridge/       # Web–Android köprüsü
│   │   │   │   ├── data/         # Modeller, MediaStore ve Room
│   │   │   │   ├── service/      # Oynatma servisi ve ses efektleri
│   │   │   │   └── util/         # Arama ve albüm kapağı yardımcıları
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Yerel testler
│   │   └── androidTest/          # Enstrümantasyon testleri
│   └── build.gradle.kts
├── gradle/                       # Gradle Wrapper ve sürüm kataloğu
├── .github/workflows/            # CI iş akışı
├── QA_REPORT_2026-10-14.txt      # Son QA raporu
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Bilinen sınırlamalar

- Tam Android derlemesi ve APK üretimi mevcut QA raporunda doğrulanmış değildir.
- Gerçek cihazda oynatma, arka plan davranışı, bildirimler, izin akışları ve farklı Android sürümleri ayrıca test edilmelidir.
- Release derlemesi, imzalama ve küçültme davranışı yayın öncesinde kontrol edilmelidir.
- Arayüzde görünen her özelliğin tüm cihazlarda çalıştığı varsayılmamalıdır.

Hata bildirirken mümkünse Android sürümünü, cihaz modelini, adımları, beklenen sonucu, gerçekleşen sonucu ve ilgili logcat çıktısını ekleyin. Gizli anahtarları, parolaları veya kişisel dosyaları rapora koymayın.

## Katkıda bulunma

1. Projeyi kendi çalışma kopyanıza alın.
2. Değişikliği küçük ve açıklayıcı bir kapsamda yapın.
3. Uygun testleri ve statik kontrolleri çalıştırın.
4. Değişikliğin doğrulanamayan yönlerini açıkça belirtin.
5. Hata düzeltmelerinde mevcut gerçek cihaz medya akışını ve kullanıcı izinlerini koruyun.

## Lisans

`package.json` dosyasında proje lisansı `MIT` olarak belirtilmiştir. Depoda ayrıca bir `LICENSE` dosyası bulunmuyorsa dağıtımdan önce lisans metnini ekleyin ve bağımlılıkların lisanslarını kontrol edin.

---

**NeonMusic** — Android için neon temalı yerel müzik çalar.
