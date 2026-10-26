# NeonMusic — Kalıcı Proje Talimatı (Master Instructions)

**Amaç:** NeonMusic üzerinde devam eden her inceleme, hata düzeltme ve ZIP üretiminde aynı proje kimliğini, sürüm kararlarını ve doğrulama kurallarını korumak. Bu dosya proje kökünde tutulmalıdır. Yeni bir sohbette/çalışma oturumunda önce bu dosyayı ve `NEONMUSIC_FULL_AUDIT_2026-10-20.md` dosyasını oku.

## 1. Değişmez çalışma kuralları

1. **GitHub'a yazma.** Commit, push, PR açma, issue kapatma veya repoda doğrudan dosya değiştirme. Yalnızca yerel çalışma kopyasında ilerle ve kullanıcıya ZIP/dosya ver. Kullanıcı GitHub yüklemesini kendisi yapar.
2. **Başarılı derleme uydurma.** “Derlendi”, “lint temiz”, “APK hazır” veya “tüm hatalar giderildi” ifadelerini sadece komut gerçekten çalıştırılıp çıktı kontrol edilmişse kullan. Statik kontrolü gerçek Android derlemesinden açıkça ayır.
3. **Teslim dosyasını doğrula.** Sandbox bağlantısı vermeden önce dosyanın aktif ortamda bulunduğunu, bayt boyutunun sıfırdan büyük olduğunu, ZIP ise `unzip -t` sonucunun başarılı olduğunu ve içindeki proje kökünün doğru olduğunu doğrula. Önceki oturumdan/path'ten link varsayma.
4. **Kaynak sürümlerini karıştırma.** Paket adını, `applicationId`, `namespace`, `settings.gradle.kts`, CI workflow ve kullanıcıdaki hata dosyasıyla karşılaştır. Sadece dosya adına göre “en güncel” kabul etme.
5. **Küçük ve izlenebilir değişiklikler.** Önce başlangıç ZIP'inin SHA/size/manifestini kaydet; değişikliği tek amaçla yap; diff'i incele; ilgili kontrolleri tekrar çalıştır. Gereksiz sürüm yükseltme, geniş refactor, lint baseline veya lint suppression yapma.
6. Kullanıcının onayı olmadan veri tabanı sürümünü, `applicationId`, `namespace`, `minSdk`, izinler, medya kaynağı, JS köprü isimleri veya oynatma mimarisini değiştirme.

## 2. Kaynak doğruluğu ve sürüm kimliği

Bu incelemenin eşleşen çalışma tabanı `com.neonmusic.player` olan **NeonMusic-Final-Fixed12-LintFix.zip** içeriğidir. Kullanıcının yüklediği `music-player-main (3).zip` farklı bir kaynak anlık görüntüsüdür: `namespace = com.example`, `applicationId = com.aistudio.musicplayer.trwv`, `versionCode = 1`, `versionName = 1.0`; Firebase/Secrets kalıntıları ve farklı paket yapısı içerir. Bu iki ağacı birleştirme veya birinin dosyalarını öbürüne kopyalama.

En son GitHub dalındaki gerçek kaynak farklı olabilir. Büyük değişiklikten önce kullanıcıdan alınan en yeni ZIP'i incele veya kullanıcı kendisi indirdiği dal arşivini yüklesin. GitHub'a doğrudan erişim/yazma yerine verilen dosyaya göre çalış.

## 3. Bu tabanın derleme matrisi

- Gradle Wrapper: `9.3.1` (`gradle/wrapper/gradle-wrapper.properties`)
- Android Gradle Plugin (AGP): `9.1.1`
- AGP 9 yerleşik Kotlin desteği kullanılır; `org.jetbrains.kotlin.android` / `kotlin-android` pluginini tekrar ekleme.
- AGP 9.1.1'in beklenen varsayılan KGP sürümü: `2.2.10`; sürüm kataloğundaki `kotlin = "2.2.10"` bunu belgeliyor.
- KSP: `2.3.5`; Room açıklama işlemcisi `ksp(libs.androidx.room.compiler)` ile bağlıdır.
- CI JDK: `21` (AGP'nin belgelenen minimumu JDK 17'dir; JDK 21'i salt varsayımla 17'ye düşürme).
- `compileSdk`: API `36`, `minorApiLevel = 1`.
- `minSdk = 24`; bu değeri düşürüp yükseltme, uyumluluk kararını kullanıcıyla konuşmadan değiştirme.
- `targetSdk = 36`.
- `namespace` ve `applicationId`: `com.neonmusic.player`.
- Java compile source/target: `JavaVersion.VERSION_11`. Kaynak ağacı **Kotlin ağırlıklı / Kotlin-only** olup gerçek Java `.java` dosyaları bu tabanda yoktur; “Java kodu” diye kast edilen native katman Kotlin dosyalarıdır.
- `versionCode = 2`, `versionName = "1.0.1"`.
- UI: HTML/CSS/JavaScript + WebView. Native media/data layer: Kotlin, MediaStore, SAF, Media3, MediaSession, Room, Coroutines, KSP, WebKit.

Bu eşleşme AGP 9.1.1/Gradle 9.3.1'in resmi uyumluluk matrisiyle tutarlıdır. Sürüm değiştirmeden önce resmi uyumluluk tablosunu kontrol et; yalnızca “daha yeni” olduğu için paket yükseltme.

## 4. Bilinen CI hatası ve tema kuralı

Kullanıcının paylaştığı CI logu `:app:lintDebug` görevinde şu hatayla durmuştu:

`app/src/main/res/values/themes.xml: android:windowLightNavigationBar requires API level 27 (current min is 24) [NewApi]`

Doğru kaynak düzeni:

- `app/src/main/res/values/themes.xml`: API 24+ ortak stil; `android:windowLightNavigationBar` burada bulunmamalı.
- `app/src/main/res/values-v27/themes.xml`: `Theme.NeonMusic` stilini API 27+ için tanımlayabilir; `android:windowLightNavigationBar` öğesi burada **yalnızca bir kez** yer almalı.
- `minSdk=24` korunmalı.
- Hata gizlemek için Lint suppression veya baseline ekleme; önce hatalı API kullanımını kaynağa göre düzelt.

Önceki Fixed12 arşivinde `values-v27/themes.xml` içine aynı öğe yanlışlıkla iki kez eklenmişti. Audit-Fix13 tabanında bu tekrar kaldırıldı. Bundan sonraki düzenlemede XML'i tekrar kontrol et.

## 5. Korunacak ürün davranışları

- Şarkı kitaplığı örnek/demo liste yerine cihazın gerçek ses dosyalarından gelir.
- `MediaStoreScanner.kt` cihaz medya kitaplığını tarar; kullanıcı seçtiği klasörlere SAF/DocumentFile akışıyla erişilir.
- `MusicPlaybackService.kt` Media3 ExoPlayer ve MediaSession katmanını yürütür.
- `MusicBridge.kt` WebView JavaScript ile native Kotlin arasında köprüdür. JS'nin çağırdığı `@JavascriptInterface` method isimlerini izinsiz değiştirme; Kotlin ve `app/src/main/assets/web/app.js` sözleşmesini birlikte denetle.
- Room veritabanı `AppMusicDatabase.kt` içinde version `4` ve tanımlı migration'lar taşır. Destructive migration ekleme; veri kaybı riski açıkça değerlendirilmeden şema sürümü veya migration değiştirme.
- Gerçek metadata/kapak, arama, çalma listeleri, favoriler, geçmiş, klasör/SAF, bildirim/arka plan oynatma, equalizer ve sleep timer davranışlarını koru.
- Proje kökündeki/konuşmada verilen `index-3.html` yalnızca görsel referanstır. Onun sahte şarkı verilerini, sabit sayıları, uzak demo kapaklarını veya mock işlevlerini native uygulamanın gerçek kitaplığına taşımak yasaktır.
- Şu anki arayüz dosyaları `app/src/main/assets/web/` içindedir.

## 6. Denetimde bulunan konular — ileride unutma

### Öncelik 0 — her teslimden önce
- ZIP boş olmamalı; `unzip -t` ve proje kökü kontrol edilmeli.
- `values-v27/themes.xml` içinde `windowLightNavigationBar` tek olmalı.
- CI'daki ilk hata giderildikten sonra tam workflow yeniden çalıştırılmalı; eski log, yeni test sonucu yerine geçmez.

### Öncelik 1 — gerçek davranış/izin akışları
- `MusicBridge.kt` içindeki `deleteSong()` `ContentResolver.delete()` çağrısını doğrudan yapıyor ve `SecurityException` durumunda `false` dönüyor. Android 10+ ve özellikle API 30+ için kullanıcı onayı/`MediaStore.createDeleteRequest` akışı gerekiyor olabilir. Mevcut JavaScript API senkron `Boolean` döndürdüğü için uygun çözüm callback/asenkron durum akışını birlikte tasarlamalı; native API'yi ekleyip JS tarafını unutma.
- SAF klasör taraması ile MediaStore sonuçlarının aynı fiziksel parçayı farklı URI/kimlikle temsil ederek çift kayıt oluşturup oluşturmadığı gerçek cihaz/Android sürümlerinde doğrulanmalı; yalnızca isim/path eşitliğiyle dosya kimliği kurma.
- `MainActivity.kt` SAF taraması sırasında genel `catch (Exception)` yolları, lifecycle cancellation durumlarını kullanıcıya normal tarama hatası gibi yansıtabilir. `CancellationException` semantiğini kontrol et; iptali hata olarak gösterme.

### Öncelik 2 — sürdürülebilirlik ve test
- `SearchEngine.kt` native arama ile `app.js` içindeki `searchLibrarySongs()` ayrı uygulamalardır. Aktif arama akışının hangisini kullandığını doğrula; Türkçe/Unicode eşleşmeleri için tek kaynağa yönelmeyi veya iki implementasyonu ortak testlerle eşitlemeyi değerlendir.
- Room DB v4 migration'ları, özellikle 1→2 no-op ve 2→3/3→4 değişimleri gerçek eski veritabanları ile test edilmelidir. `exportSchema=false`; schema history/test stratejisi planla ama sırf lint için şema davranışını değiştirme.
- Media3 gerçek playback, Activity lifecycle, bildirim kontrolleri, API 24/26/27/29/30/33/34+ izin ve medya işlemleri için cihaz/emülatör testleri ekle.
- `package.json` içindeki `build` şu anda global `gradle assembleDebug` çağırır; global Gradle kurulmamış ortamda bozulabilir. Daha sonra wrapper kullanan komuta (`./gradlew assembleDebug`) geçir veya scripti kaldır. Bu değişikliği Gradle doğrulaması yapmadan “test edildi” diye yazma.
- Projede `package.json` MIT lisansı belirtiyor fakat bu ağaçta kök `LICENSE` dosyası görünmüyor. Dağıtım öncesi lisansı sahiplik/izinlerle teyit et ve gerekiyorsa LICENSE ekle.
- Bu tabanda `.gitignore` yoktu; Audit-Fix13'e Android/Gradle/IDE/build/secret/signing-ignore kuralları eklenmiştir. Gerçek secrets veya keystore asla arşive/commit'e dahil edilmemeli.
- `proguard-rules.pro` kapsamını release küçültme ile test et. Kullanılmayan Hilt kuralları gibi kalıntılar varsa güvenle temizleme kararını kullanım ve release build doğrulamasıyla ver.

## 7. Zorunlu doğrulama sırası

Her değişiklikte bu sıra izlenir ve raporlanır:

1. **Kaynak seçimi:** arşiv boyutları, ZIP giriş listesi, package/namespace/applicationId ve CI dosyası karşılaştır.
2. **Build yapılandırması:** wrapper/AGP/JDK/Kotlin built-in/KSP/SDK/Room/Media3 sürüm uyumunu kontrol et. Kullanıcı logunda görev adlarını ve ilk asıl `What went wrong` hatasını belirle.
3. **Kaynak/API incelemesi:** manifest, runtime permissions, API seviyesi guard'ları, foreground service ve MediaSession, lifecycle/coroutine cancel, WebView güvenliği, ContentResolver/SAF.
4. **Veri ve medya:** Room migration, cache/concurrency, MediaStore ID ve URI, SAF persisted grants, delete consent, metadata/artwork.
5. **Web/native sözleşmesi:** JS syntax (`node --check`), bütün bridge çağrılarının native `@JavascriptInterface` karşılıkları, JSON escaping, async sonuçlar, HTML ID tekrarı.
6. **Statik kaynaklar:** bütün XML'leri parse et; API qualifier'larını kontrol et; duplicate resource item'ları ve eksik resource'ları kontrol et; `git diff --check` uygunsa çalıştır.
7. **Gerçek Android doğrulaması:** ağ+SDK varsa şu komutlar proje kökünden çalıştırılır ve sonuçları okunur:
   - `./gradlew --version`
   - `./gradlew lintDebug --stacktrace --no-daemon`
   - `./gradlew testDebugUnitTest --stacktrace --no-daemon`
   - `./gradlew assembleDebug --stacktrace --no-daemon`
   - `./gradlew assembleRelease --stacktrace --no-daemon`
   Lint önce, sonra testler ve APK görevleri. Bir aşama başarısızsa sonraki aşamalar yapılmış gibi bildirme.
8. **ZIP teslimi:** geçici kopya üzerinde çalış; gizli anahtar/keystore/local.properties/.env/build cache dahil etme; ZIP'i oluştur; boyut, giriş listesi, `unzip -t` kontrol et; yalnızca doğrulanmış path için link oluştur.
9. **Durum raporu:** “Doğrulandı”, “statik olarak kontrol edildi”, “çalıştırılamadı” ayrımını net tut; kalan riskleri ve bir sonraki test komutunu yaz.

## 8. Bu oturumun sınırı

Bu ortamda Android SDK ve Gradle kurulu görünmüyor; Gradle Wrapper dağıtımı `services.gradle.org` için DNS/ağ erişimi olmadığı için indirilemiyor. Bu nedenle tam Gradle Lint, Room/KSP, Android compile, unit/instrumentation test, APK assemble ve cihaz testi doğrulanmış değildir. Bunları “başarılı” diye raporlama. JavaScript sözdizimi, XML ayrıştırması, ZIP bütünlüğü ve kaynak-köprü statik kontrolleri ayrı ayrı belirtilebilir.
