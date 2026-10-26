# NeonMusic — Baştan Sona Kaynak ve Derleme Denetimi

**Tarih:** 20 Ekim 2026  
**Denetlenen taban:** `NeonMusic-Final-Fixed12-LintFix.zip` (`com.neonmusic.player`)  
**Kullanıcının paylaştığı CI hatası:** `:app:lintDebug` / `NewApi` / `windowLightNavigationBar`  
**Önemli sınır:** Tam Gradle derlemesi bu ortamda doğrulanamadı. Bu belge tam başarılı-build sertifikası değildir.

## 1. Özet

Projede yapılandırma sürümleri ilk bakışta gelişigüzel veya birbirini açıkça tutmayan bir set değil. AGP `9.1.1`, Gradle Wrapper `9.3.1`, yerleşik Kotlin desteği/KGP `2.2.10`, KSP `2.3.5`, `compileSdk 36.1`, `minSdk 24`, `targetSdk 36` mevcut CI ile uyumlu bir başlangıç matrisi oluşturuyor. AGP 9.1.1'in resmi matrisi Gradle 9.3.1 ve JDK 17 ile eşleşiyor; CI'daki JDK 21, sırf sürümü büyük diye hatalı kabul edilmemeli. citeturn271028search2turn271028search0turn549839search6

Kullanıcının gönderdiği CI günlüğünde Kotlin ve Java compile görevleri çalışmış; durma nedeni `lintDebug` içindeki tek hata. İlk hata, API 27'de sunulan `android:windowLightNavigationBar` ayarının ortak `values/` kaynağında bulunmasıydı. Doğru yaklaşım `minSdk 24` değerini korumak ve öğeyi yalnızca `values-v27/` tema varyantında tanımlamaktır. Sonraki Fixed12 denemesinde bu öğe yanlışlıkla iki kez konduğu için bu denetimde bir kopyası kaldırıldı.

Tam Android derlemesi/testleri bu ortamda çalıştırılamadı: Gradle dağıtımı indirilemedi (DNS/ağ erişimi yok) ve Android SDK çevre değişkenleri bulunmuyordu. Bu nedenle kodun bütünü compile/test geçmiş olarak nitelenemez.

## 2. Kaynak tabanı karmaşası — önce bunu kontrol et

İncelenen arşivler aynı uygulamanın aynı sürümü değildir:

| Arşiv | Paket kimliği / uygulama kimliği | Not |
|---|---|---|
| `music-player-main (3).zip` | `namespace = com.example`, `applicationId = com.aistudio.musicplayer.trwv`, version `1.0` | Farklı/önceki kaynak ağacı; Firebase/Secrets bağlantılı kalıntılar ve `.gradle` klasörü içeriyor. Gönderilen CI `themes.xml` hatasıyla aynı kaynak dosyası değil. |
| `NeonMusic-Final-Fixed11.zip` | `com.neonmusic.player` | Kullanıcının gösterdiği lint hatasının kaynak çizgisiyle eşleşiyor. |
| `NeonMusic-Final-Fixed12-LintFix.zip` | `com.neonmusic.player` | API-27 tema düzenlemesini içeren taban; tekrar eden tema öğesi bu denetimde bulundu. |

Bu nedenle eski `com.example` projesinden kodu `com.neonmusic.player` projesine rastgele taşımak doğru değildir. En güncel GitHub kaynak ağacı bu arşivlerden farklı olabileceğinden büyük düzenlemelerden önce kullanıcıdan güncel kaynak ZIP'ini istemek veya sağladığı ZIP'in `applicationId`/CI dosyalarıyla eşleşmesini kontrol etmek gerekir. GitHub'a herhangi bir yazma yapılmadı.

## 3. Derleme sürümleri ve karar

| Bileşen | İncelenen değer | Değerlendirme |
|---|---|---|
| Gradle Wrapper | `9.3.1` | AGP `9.1.1` için beklenen sürüm |
| Android Gradle Plugin | `9.1.1` | API 37.0 ve önceki düzeyleri destekler |
| Kotlin | `2.2.10` | AGP 9 yerleşik Kotlin desteğinde varsayılan KGP sürümü |
| KSP | `2.3.5` | KSP2 kullanımı; Room compiler `ksp` üzerinden bağlanıyor; yalnızca yenisi var diye değiştirilmemeli |
| CI Java | Temurin JDK `21` | AGP tablosundaki JDK 17 minimumundan yüksek; başlı başına hata kanıtı yok |
| `compileSdk` | `36`, minor `1` | API 36.1 hedefi |
| `minSdk` | `24` | Android 7.0+ desteği; lint hatası için yükseltilmemeli |
| `targetSdk` | `36` | Manifest/izin davranışlarının API 36 kurallarıyla test edilmesi gerekir |
| Java bytecode source/target | `11` | `compileOptions` değeri; native kaynaklar Kotlin ağırlıklı, `.java` kaynak bulunmadı |
| Namespace/applicationId | `com.neonmusic.player` | Fixed11/12 tabanının kimliği |
| Room | `2.7.0`, DB version `4` | Migration geçmişini koru; destructive fallback ekleme |
| Media3 | `1.5.1` | ExoPlayer + Session altyapısı |
| WebKit | `1.17.1` | WebView/asset işlevleri |

AGP 9.0+ Kotlin derlemesi için `org.jetbrains.kotlin.android` eklentisini ayrıca eklememek gerekir; bu projede Kotlin'in bulunup ayrıca plugin alias'ının olmaması tek başına eksiklik değildir. Bu pluginin tekrar eklenmesi AGP 9'da çift Kotlin extension hatasına yol açabilir. citeturn271028search0

**Öneri:** Sürüm setini şimdilik koru. CI'da görülen hata tema kaynak hatasıdır; sürüm yükseltmesiyle ilgisi gösterilmemiştir. İleride herhangi bir yükseltmeden önce resmi AGP, Gradle, KGP/KSP uyumluluğunu doğrula. citeturn271028search2turn549839search3turn549839search6

## 4. CI hatası ve kaynak düzeltmesi

Paylaşılan Actions çıktısı:

```text
Task :app:compileDebugKotlin              (başarılı)
Task :app:compileDebugJavaWithJavac        (başarılı)
Task :app:lintReportDebug                  (rapor üretildi)
Lint found 1 error and 52 warnings.
app/src/main/res/values/themes.xml:6:
android:windowLightNavigationBar requires API level 27 (current min is 24) [NewApi]
Task :app:lintDebug FAILED
```

Düzeltme kuralı:

- `app/src/main/res/values/themes.xml` ortak tema içinde `android:windowLightNavigationBar` olmamalı.
- `app/src/main/res/values-v27/themes.xml` aynı `Theme.NeonMusic` stilini API 27+ için tanımlamalı; `windowLightNavigationBar` orada bir kez bulunmalı.
- `minSdk = 24` kalmalı.
- Lint'i susturmak veya baseline almak yerine neden olan API özelliği doğru kaynak qualifier'ına taşınmalı.

**Denetim bulgusu:** Fixed12 `values-v27/themes.xml` dosyasında `windowLightNavigationBar` aynı değerle iki kez bulunuyordu. Audit-Fix13 kopyasında bir tekrar çıkarıldı. XML parser'ın dosyayı parse edebilmesi, aynı stil öğesinin semantik olarak yinelenmesini yakalamaz; bu yüzden hedefli duplicate kontrolü ayrıca yapıldı.

52 uyarı, paylaşılan rapordaki bilgilerdir; hepsi bir anda hata olarak ele alınmamalı. İlk hata çözüldükten sonra Lint yeniden çalıştırılmalı ve yeni çıktıda kalan hata/uyarılar ayrı önceliklendirilmelidir.

## 5. Mimari ve kaynak denetimi

### Native/Android katmanı

- `MainActivity.kt`: WebView, JS bridge bağlama, runtime izin akışı, Activity Result/SAF seçimi, yaşam döngüsü.
- `bridge/MusicBridge.kt`: 56 `@JavascriptInterface` tanımı; JS'nin çağırdığı yöntemleri native API'lerle eşleştirir; cache revision/lock, çalma listesi, favoriler, geçmiş ve medya yardımcıları bulunur.
- `data/mediastore/MediaStoreScanner.kt`: MediaStore, SAF/DocumentFile ve Room ile kütüphane taraması.
- `data/db/AppMusicDatabase.kt`: Room database version 4; migration 1→2, 2→3 ve 3→4; destructive migration fallback görünmüyor.
- `service/MusicPlaybackService.kt`: Media3 oynatma ve MediaSession.
- `service/AudioEffectManager.kt`: Android `Equalizer`/`BassBoost` ve ilgili ses efektleri.
- `util/AlbumArtManager.kt`, `util/SearchEngine.kt`: kapak ve native arama yardımcıları.

### Web katmanı

- `app/src/main/assets/web/index.html`, `styles.css`, `app.js`, `i18n.js`.
- WebView AssetLoader ve yerel asset origin yaklaşımı bulunuyor; arbitrary external URL navigation ve dosya/content erişimi kısıtlamaları kaynakta yer alıyor.
- Statik kontrol: `app.js` ve `i18n.js` için `node --check` geçti.
- Statik köprü isim kontrolü: Web içeriğinde 53 benzersiz doğrudan `AndroidMusicBridge.*` çağrısı bulundu; bu adların native `@JavascriptInterface` metotlarında karşılığı kontrol edildi ve eksik ad bulunmadı. Bu test, parametre tiplerini veya runtime davranışını garanti etmez.
- HTML'de 205 benzersiz ID kontrol edildi; duplicate ID saptanmadı.

### İzinler ve servis

Manifestte `READ_MEDIA_AUDIO`, eski sürümler için `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `INTERNET` ve `ACCESS_NETWORK_STATE` mevcut. `MediaSessionService` manifest kaydı da mevcut. Manifest varlığı tek başına izinlerin gerçek cihazda doğru istem edildiğini veya bildirimlerin çalıştığını ispatlamaz; cihaz/emülatör testleri gereklidir.

## 6. Öncelikli fonksiyonel riskler

### P1 — şarkı silme için kullanıcı onayı akışı

`app/src/main/java/com/neonmusic/player/bridge/MusicBridge.kt` yaklaşık satır 699:

```kotlin
activity.contentResolver.delete(Uri.parse(song.uri), null, null)
```

Bu doğrudan silme girişimi `SecurityException` halinde `false` döndürüyor. Scoped storage kullanan Android 10+ cihazlarda, uygulamanın sahip olmadığı medya öğelerini silmek için geri kazanılabilir erişim/onay akışı gerekir; Android 11+ API'si kullanıcıya toplu onay göstermek için `MediaStore.createDeleteRequest()` sunar. citeturn750239search6turn750239search0

JS tarafındaki köprü şu anda senkron `Boolean` döndürdüğünden, doğru çözüm sadece Kotlin'e yeni API satırı eklemek değildir: kullanıcı onayını başlatan Activity Result akışı, istek kimliği/sonuç callback'i, reddedilme durumu ve kitaplık/cache güncellemesi Web katmanıyla birlikte tasarlanmalıdır. SAF URI'leri ile MediaStore URI'leri de ayrı ayrı ele alınmalıdır.

### P1 — kaynağın hangi ZIP olduğu

Kullanıcıdaki hata `com.neonmusic.player` ağacına aittir; yüklenmiş `music-player-main (3).zip` daha eski/farklı `com.example` tabanıdır. Build hatası çözülmeden önce ve her seferinde dosya kimliğini karşılaştır.

### P2 — MediaStore / SAF çift kayıt riski

Scanner hem MediaStore hem de kullanıcı tarafından seçilen SAF klasörlerini tarar. SAF kimliği Document URI, MediaStore kimliği ise MediaStore content URI olabilir. Aynı şarkı iki yoldan görünürse duplicate sonuç oluşup oluşmadığı; dosya kimliği, persist edilen SAF grant'leri, çıkarılabilir depolama ve yeniden tarama senaryolarıyla test edilmeli. Bu raporda gerçek cihazda çift kayıt oluştuğu doğrulanmış değildir; test edilmesi gereken bir risk olarak işaretlenmiştir.

### P2 — coroutine iptali ile genel hata yakalama

`MainActivity.kt` SAF callback'inde genel `catch (Exception)` bulunuyor. Coroutine scope kapanırken `CancellationException` normal hata gibi raporlanabilir. Kapsamlı düzeltmede cancellation açıkça yeniden fırlatılmalı/yutulmalı ve UI hata callback'i yalnız gerçek tarama hatalarında çağrılmalı.

### P2 — iki arama uygulaması

Native `SearchEngine.kt` Unicode/Türkçe karşılaştırma kuralları içeriyor; `app.js` içinde ayrıca `searchLibrarySongs()` kullanılıyor. Böylece arama için iki ayrı implementasyon var. Aktif arama ekranının JS fonksiyonunu kullandığı görünüyor; native `MusicBridge.searchSongs` wrapper'ı da mevcut. Bunların gerçekten nerelerde kullanıldığı trace edilip tek davranış sözleşmesine ve ortak testlere yönlendirilmeli. Bu, derleme hatasının nedeni değil; arama sonuçlarının tutarlılığı açısından ürün riski.

### P2 — yayın/teknik borç

- `package.json` build scripti global `gradle assembleDebug` kullanıyor; global Gradle yüklü değilse başarısız olur. Wrapper kullanılmalı.
- `package.json` lisansı MIT olarak belirtiyor fakat kaynak ağacında `LICENSE` dosyası görünmüyor. Yayın öncesi lisans sahipliği ve metni teyit edilmeli.
- ProGuard kuralları release/minify derlemesinde doğrulanmalı; eski/unused Hilt satırları varsa ancak kullanım kontrol edildikten sonra temizlenmeli.
- Room migration testleri gerçek eski şema senaryolarını kapsamlı karşılamalı; `exportSchema=false` nedeniyle migration geçmişini test ve dokümantasyonla koru.
- Playback/MediaSession/notification, API 24/26/27/29/30/33/34+, izin retleri, medya silme ve SAF için uçtan uca test kapsamı sınırlı.

## 7. Gerçekte hangi kontroller yapıldı?

| Kontrol | Sonuç | Anlamı |
|---|---|---|
| Kaynak ZIP byte boyutu | Geçti; Fixed12 728,422 bayt civarı | Arşiv sıfır bayt değil |
| `unzip -t` Fixed12 | Geçti | Zip CRC/bütünlük testi |
| XML parse | 12 XML dosyası ayrıştırıldı | XML sözdizimi açısından temel kontrol; duplicate stil öğesini tek başına yakalamaz |
| JS syntax (`node --check`) | `app.js`, `i18n.js` geçti | Sözdizimi kontrolü; tarayıcı/Android davranışı değil |
| JS → Kotlin bridge isimleri | 53 benzersiz çağrı için eksik native method bulunmadı | Sadece ad sözleşmesi; imza/runtime doğrulaması değil |
| HTML ID kontrolü | 205 benzersiz ID, duplicate yok | UI ID bütünlüğünün statik kontrolü |
| Gradle wrapper / Android SDK | Engellendi | `services.gradle.org` DNS/ağ erişimi yok; `ANDROID_HOME`/`ANDROID_SDK_ROOT` yapılandırılmamış |
| `lintDebug`, `testDebugUnitTest`, APK assembly | Bu ortamda doğrulanamadı | Başarılı derleme iddiası yapılamaz |
| Gerçek cihaz testleri | Yapılmadı | Oynatma, bildirim, permission, SAF ve silme davranışı kesinleştirilmedi |

## 8. Audit-Fix13 içeriği

Audit-Fix13, Fixed12'den türetilmiştir ve dar kapsamlı değişiklik yapar:

1. `values-v27/themes.xml` içindeki yinelenen `windowLightNavigationBar` girdilerinden biri kaldırıldı.
2. Android/Gradle/IDE çıktılarını, gizli yapılandırma ve imzalama materyallerini Git'e göndermeyi engelleyen `.gitignore` eklendi.
3. `NEONMUSIC_MASTER_INSTRUCTIONS.md` ve bu audit dosyası proje köküne eklendi.

Bu ZIP, **tam Android build/test doğrulamasından geçmiş değildir**; yalnızca statik kontrolleri yapılmış kaynak paketidir. `lintDebug` ve devamındaki CI adımlarını gerçek GitHub Actions çıktısında ayrıca doğrulayın.

## 9. Başvurulan resmi belgeler

- [AGP 9.1.1 sürüm notları ve uyumluluk matrisi](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [AGP 9 built-in Kotlin'e geçiş](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Gradle/AGP sürüm tablosu](https://developer.android.com/build/releases/about-agp)
- [Kotlin Symbol Processing (KSP) gereksinimleri](https://kotlinlang.org/docs/ksp-overview.html)
- [Android MediaStore medya erişimi ve silme davranışı](https://developer.android.com/training/data-storage/shared/media)
- [MediaStore.createDeleteRequest API belgesi](https://developer.android.com/reference/android/provider/MediaStore#createDeleteRequest(android.content.ContentResolver,java.util.Collection))
