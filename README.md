# Kronometre

Tek ekranlı, tek işli bir Android kronometresi. Ekran kapalıyken de doğru sayar;
bildirimden ve ana ekran widget'ından kontrol edilir; çalışırken uygulamanın kendi
saniyelik döngüsü, wake lock'u veya periyodik işi yoktur.

- Paket: `com.oguzh.kronometre` · `minSdk 23` · `targetSdk 36` · `compileSdk 37`
- Teknik sözleşme: [`PROMPT.md`](PROMPT.md) · niyet: [`CLAUDE.md`](CLAUDE.md)

> **Durum:** GitHub Actions'ta `assembleDebug assembleRelease lint testDebugUnitTest` yeşil
> (lint: 0 hata, 11 uyarı; release APK 2 630 979 bayt). APK'lar her koşunun
> `kronometre-apk-ve-raporlar` artifact'ında. Bulut oturumunda `dl.google.com` engelli
> olduğu için derleme yerelde değil CI'da doğrulanıyor. Cihaz testleri ve ölçümler
> kullanıcının işidir; sonuçlar aşağıdaki tablolara girilene kadar "ölçülmedi" kalır.

---

## Kurulum ve derleme

Gereken: JDK 17+ (CI JDK 21 kullanıyor), Android SDK (platform 37, build-tools 36+).
`local.properties` commit edilmez; Android Studio veya `ANDROID_HOME` bunu sağlar.

```
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # R8 + shrinkResources, debug anahtarıyla imzalı
./gradlew lint
./gradlew testDebugUnitTest
adb install -r app/build/outputs/apk/release/app-release.apk
```

Release derlemesi kişisel dağıtım için **debug anahtarıyla** imzalanır, böylece
doğrudan kurulabilir. Play'e yüklenecekse kendi `signingConfig`'ini tanımla.

### Sürümler

| Bileşen | Sürüm | Not |
|---|---|---|
| Gradle | 9.6.0 | AGP 9.4'ün asgari sürümü |
| AGP | 9.4.0 | PROMPT "9.3.x" diyor; eylül 2026 itibarıyla güncel stable 9.4.0 |
| Kotlin (KGP + Compose compiler) | 2.4.10 | AGP 9 built-in Kotlin; KGP `buildscript` classpath'inde |
| Compose BOM | 2026.08.00 | |
| Glance appwidget / material3 | 1.2.0 | 1.3.0-alpha kullanılmadı |
| core-ktx | 1.19.1 | |
| activity-compose | 1.13.0 | |
| lifecycle | 2.11.0 | |
| datastore-preferences | 1.2.1 | |
| profileinstaller | 1.4.1 | |
| desugar_jdk_libs | 2.1.5 | |

**`compileSdk` neden 36 değil 37:** Güncel stable AndroidX sürümleri 36'yla derlenmiyor —
core 1.18+ `compileSdk 36.1`, lifecycle 2.11 ve Compose 1.12 hattı "Compose compileSdk 37,
asgari AGP 9.2" notunu taşıyor (developer.android.com sürüm notları). AGP 9.4 API 37'yi
destekliyor. `targetSdk` 36'da kaldı; çalışma zamanı davranışı değişmiyor.

---

## Mimari

```
app/src/main/java/com/oguzh/kronometre/
├── MainActivity.kt                 tek Activity, ComposeView (activity_main.xml)
├── data/StopwatchState.kt          saf durum + geçişler (start/pause/lap/reset)
├── data/StopwatchRepository.kt     DataStore, 3 anahtar, StateFlow, manuel singleton
├── data/OemProfile.kt              marka tablosu (contains eşleşmesi)
├── service/StopwatchService.kt     specialUse FGS, SCREEN_ON/OFF receiver, dakikalık tik
├── service/StopwatchNotifier.kt    tek build(state, interactive) fonksiyonu
├── service/StopwatchActions.kt     aksiyon sabitleri, intent/requestCode, startForegroundService
├── ui/StopwatchViewModel.kt        komutları servise gönderir, state'i yansıtır
├── ui/StopwatchScreen.kt           tek ekran, izin şeridi, OEM kartı
├── ui/StopwatchComponents.kt       süre (android.widget.Chronometer), butonlar, tur listesi
├── ui/ScreenDimController.kt       10 sn karartma durum makinesi
├── ui/oem/BatteryOptimizationHelper.kt  derin linkler + fallback zinciri + yardım kartı
└── widget/StopwatchWidget.kt, StopwatchWidgetReceiver.kt   Glance + AndroidRemoteViews
```

### Sıfır döngü ilkesi — nerede ne sayıyor

| Yüzey | Süreyi kim çiziyor | Uygulamanın işi |
|---|---|---|
| Bildirim, ekran açık | SystemUI (özel görünümdeki `Chronometer`, `RemoteViews.setChronometer`) | Durum değişiminde **1** gönderim |
| Bildirim, ekran kapalı | Statik `H:MM` metni | Dakika sınırına hizalı **1** `Handler.postDelayed` / dakika |
| Widget | Launcher sürecindeki `android.widget.Chronometer` (`RemoteViews.setChronometer`) | Durum değişiminde **1** `updateAll()` |
| Uygulama ekranı | `android.widget.Chronometer` (`AndroidView`) — yalnızca görünürken tıklar | Yok |
| Servis | — | Komutu uygular, bildirimi bir kez gönderir, boşta bekler |

- Süre hesabı: `elapsedMs + (SystemClock.elapsedRealtime() - runningSince)`.
  `currentTimeMillis` yalnızca bildirimin `setWhen` alanı için kullanılır (API bunu istiyor).
- DataStore yalnızca durum değişiminde yazılır. Anahtarlar: `elapsed_ms`, `running_since`, `laps` (CSV).
- Wake lock alınmaz (izin manifest'te durur, kodda kullanılmaz).
- Ekran durumu `ACTION_SCREEN_ON/OFF` ile, servis içinde `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`
  ile öğrenilir; `isInteractive` yalnızca servis `onCreate`'de bir kez okunur.
- Uygulamada `Application` sınıfı, DI, ağ izni, analitik yok.

### Bildirim

- `specialUse` FGS tipi + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; çağrı `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`.
- Kanal `stopwatch_v2`, `IMPORTANCE_DEFAULT` ama ses/titreşim/rozet yok, `VISIBILITY_PUBLIC`. `IMPORTANCE_LOW` "sessiz" sayılır ve birçok ROM'da ("sessiz bildirimleri kilit ekranında gizle") kilit ekranında gösterilmiyordu. Eski `stopwatch` kanalı silinir.
- Her gönderim aynı `build(state, interactive)` fonksiyonundan geçer: `setOngoing`, `setSilent`,
  `setOnlyAlertOnce`, `VISIBILITY_PUBLIC` ve **aynı içerikle kurulmuş** `setPublicVersion` her seferinde birlikte.
- İçerik `DecoratedCustomViewStyle` + `notification_time.xml`: büyük (32sp) `Chronometer` RemoteViews'i, Google Saat bildirimindeki gibi yalnızca süre ve altında küçük bir satır (tur sayısı / "duraklatıldı" / ekran kapalıyken "sa:dk"). `Chronometer` SystemUI sürecinde kendi kendine tıklar; bildirim yine seans başına bir kez gönderilir. Başlık/metin yalnızca özel görünümü desteklemeyen yüzeyler için doldurulur.
- Çalışırken aksiyonlar: **Duraklat**, **Tur**. Duraklatılmışken: **Sürdür**, **Sıfırla**. Her `PendingIntent` `FLAG_IMMUTABLE`, aksiyona özel `requestCode` taşır ve API 26+'da `getForegroundService` kullanır (servis duraklatılmışken çalışmıyor).
- Duraklatınca bildirim "Kronometre duraklatıldı" + statik süreyle kalır, servis `STOP_FOREGROUND_DETACH` ile durur (duraklatılmışken çalışan servis, döngü, uyanma yok). **Sıfırla** bildirimi kaldırır.

### Widget

`SizeMode.Exact`: süre yazısı ve düğmeler widget'ın gerçek boyutuna göre hesaplanır, boşluk kalmaz.
Yükseklik 100 dp'nin altındaysa tek satır (süre + düğmeler; dar ise yalnızca birincil düğme),
üstündeyse sütun (150 dp'den yüksekse üstte durum satırı, ortada büyük süre, altta yuvarlak düğmeler).
Düğmeler yuvarlak köşeli ve widget genişliğini doldurur; dar sütunda iki satır (üstte büyük birincil düğme, altta Sıfırla ve Tur). Tur atılınca sürenin altında "Tur N · süre" satırı çıkar. Düğmeler: çalışırken **Sıfırla · Duraklat · Tur**, duraklatılmışken **Sıfırla · Sürdür**, sıfırdayken **Başlat**.
Boyut değişince yalnızca bir kez yeniden çizilir; saniyelik güncelleme yok.
Widget 110×40 dp'ye kadar küçültülüp büyütülebilir. Butonlar `actionStartService(..., isForegroundService = true)`,
widget'ın geri kalanı `clickable(actionStartActivity<MainActivity>())`. Widget içeriği
repository'den okunur; servis çalışmıyorken de doğru değeri gösterir.

### Ekran karartma

Kronometre çalışırken 10 sn hareketsizlikte pencere parlaklığı ~1 sn'lik animasyonla
`0.01`'e iner ve `keepScreenOn` bırakılır (sistem kendi zaman aşımını saymaya başlar).
Herhangi bir dokunuş/tuş parlaklığı `BRIGHTNESS_OVERRIDE_NONE` (`-1f`) ile geri getirir;
kararmış ekrana yapılan ilk dokunuş yalnızca ekranı uyandırır, butona basmaz.
Kronometre durunca, `onStop`'ta ve `onDestroy`'da parlaklık `-1f`'e geri yüklenir.
Karartma `isInteractive`'ı değiştirmez; bildirim o sırada saniye modunda kalır ve bu doğrudur
(bildirim görünmüyor, render maliyeti yok).

---

## PROMPT.md'den bilinçli sapmalar

| Konu | PROMPT | Uygulanan | Neden |
|---|---|---|---|
| `compileSdk` | 36 | 37 | Güncel stable AndroidX 36 ile derlenmiyor (yukarıda) |
| AGP | 9.3.x | 9.4.0 | Doğrulanabilen güncel stable |
| Dakika metni | `formatElapsedTime(...).substringBeforeLast(':')` | `H:MM` elle | 1 saatin altında `formatElapsedTime` `MM:SS` üretir, kesince yalnızca dakika (`05`) kalıyordu |
| Widget canlı sayaç API 23 | statik | canlı | `RemoteViews.setChronometer` API 1'den beri var; 24+ gereken yalnızca geri sayım |
| Widget `setBase` | `currentTimeMillis - elapsed` | `elapsedRealtime - elapsed` | `Chronometer` tabanı `elapsedRealtime` ölçeğinde |
| Duraklatınca bildirim | tamamen kalkar | Sürdür / Sıfırla ile kalır | Kullanıcı kararı (2026-09-27). Sıfırla bildirimi kapatır. Android 14+'da FGS olmayan bildirim kullanıcı tarafından kaydırılarak kapatılabilir; duraklatılmışken servis çalıştırmamak için bu kabul edildi |
| Bildirim kanalı önemi | `IMPORTANCE_LOW` | `IMPORTANCE_DEFAULT` (sessiz) | LOW kanallar kilit ekranında gizlenebiliyor; ses ve titreşim kanal düzeyinde kapalı |
| OEM yardım ekranı | ayrı ekran, "gösterildi" bayrağı, 3+ açılış sayacı | ana ekranda kapatılabilir kart | "Ayar ekranı yok" ve "DataStore'da yalnızca 3 anahtar" kuralları. Kart, marka agresifse ve muafiyet yoksa görünür; "Şimdilik kapat" o oturum için gizler; muafiyet verilince (`onResume`'da okunur) bir daha görünmez. Stock Android'de yalnızca nötr tek adım gösterilir |
| Dokunma algılama | `pointerInput` | `Activity.dispatchTouchEvent` | Hiçbir olayı tüketmeden tüm pencereyi gözlüyor, kararmış ekrandaki ilk dokunuşu yutabiliyor |
| Bildirim güncelleme çağrısı | `notify()` | `ServiceCompat.startForeground` | Aynı IPC, tek kod yolu, `POST_NOTIFICATIONS` lint uyarısı yok |

---

## Bilinen sınırlamalar

- **Süreç ölümü / yeniden başlatma:** Kronometre çalışırken süreç öldürülürse (veya cihaz
  yeniden başlarsa) açılışta kayıtlı `elapsed_ms` yüklenir ve **duraklatılmış** devam eder.
  Son başlatma/turdan sonraki çalışan süre kaybolur. Bu kasıtlı: `elapsedRealtime` yeniden
  başlatmada sıfırlandığı için o segment güvenle hesaplanamaz. Boot'ta otomatik devam yok.
- **Ekran kapalıyken bildirim dakika hassasiyetindedir.** `Chronometer` saniyeyi
  gizleyemez; bu modda uygulama dakikada bir bildirimi tazeler (saatte ~60 `startForeground`
  çağrısı). Tik `Handler` ile kurulur ve wake lock almaz; CPU derin uykudayken tik de
  bekler. Sonuç: AOD/kilit ekranındaki dakika metni gecikebilir, ama ekran açıldığı anda
  `SCREEN_ON` saniye moduna **doğru süreden** döner. Gerçek uyanma sayısı ölçümle belirlenecek.
- Ekran açıkken bildirim görünürse (panel/kilit ekranı) SystemUI kronometreyi saniyede bir
  yeniden çizer. Bu SystemUI'nin maliyetidir, uygulamanın değil; Google Saat de aynısını yapar.
- **Karartma 10 saniyedir, ayarlanamaz.** Değiştirmek için `ScreenDimController.DIM_DELAY_MS`.
- Agresif ROM'lar (HyperOS, One UI, EMUI, ColorOS, Funtouch...) FGS'i ekran kapandıktan
  1–2 dk sonra öldürebilir. Bu uygulama hatası değildir; aşağıdaki marka adımları gerekir.
- Pil optimizasyonu muafiyeti kullanıcıya bağlıdır, uygulama zorlayamaz.
- Android 12+ FGS'i arka plandan başlatmayı kısıtlar. Widget ve bildirim dokunuşları
  kullanıcı etkileşimi sayılır; başlatma yine de reddedilirse uygulama içinde hata şeridi çıkar.

### Politika notu — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

Play bu izni "çekirdek işlev bozuluyorsa" sınırlar. Kronometre için gerekçe geçerli:
ekran kapalıyken sayım FGS'e bağlı ve OEM görev katilleri FGS'i öldürüyor. Dialog doğrudan
açılmaz; kullanıcı önce uygulama içindeki açıklama kartını görür, oradan "Aç" ile dialoga gider.

---

## Marka bazında pil optimizasyonu adımları

Uygulama markayı `Build.MANUFACTURER` / `BRAND` / `DISPLAY` / `FINGERPRINT` üzerinde
`contains` ile tespit eder; tek doğru/yanlış kaynağı `PowerManager.isIgnoringBatteryOptimizations`.
Her "Aç" butonu şu zinciri dener: markanın ekranı → uygulama ayrıntıları →
pil optimizasyonu listesi → hiçbiri açılmazsa elle yapılacak adım metni.

| Marka | Adımlar |
|---|---|
| **Xiaomi / Redmi / POCO (HyperOS, MIUI)** | Güvenlik → Otomatik başlatma → Kronometre → **Aç** · Ayarlar → Uygulamalar → Kronometre → Pil → **Kısıtlama yok** · Bildirimler → Kilit ekranı bildirimleri → Kronometre → **Aç, içeriği göster** · Son uygulamalar'da kartı **kilitle** |
| **Samsung (One UI)** | Pil → Arka plan kullanımı → **Hiç kısıtlama** · Arka plan kullanımı sınırları → **Hiç uykuya dalmayan uygulamalar** → Kronometre ekle · Bildirimler → Kronometre bildirimlerine izin ver |
| **Huawei / Honor (EMUI, MagicOS)** | Pil → Uygulama başlatma → **Otomatik yönetimi kapat**, üç anahtarı da elle aç · Pil → Kısıtlama yok |
| **Oppo / Realme / OnePlus (ColorOS, OxygenOS)** | Pil → **Arka plan etkinliğine izin ver** · **Otomatik başlatma** → aç (Telefon Yöneticisi → Beyaz liste) · Pil → Kısıtlama yok |
| **Vivo / iQOO (Funtouch, OriginOS)** | Pil → Arka plan yönetimi → **Yüksek arka plan tüketimi** → beyaz liste · Otomatik başlatma → aç · Pil → Kısıtlama yok |
| **Asus** | Otomatik başlatma → **Kronometre'ye izin ver** · Pil → Kısıtlama yok |
| **Transsion (Infinix, Tecno, itel)** | Otomatik yönetim → Kronometre → üç izni de aç · Pil → Kısıtlama yok |
| **Stock (Pixel, Motorola, Nothing, Nokia)** | Ayarlar → Uygulamalar → Kronometre → Pil → **Kısıtlama yok** (genelde gerekmez) |

---

## Manuel test listesi

### Galaxy S22 (API 34+ yolu)
1. Uygulamayı aç, bildirim iznini ver, **Başlat**.
2. Bildirim panelini aç → kronometre saniye saniye ilerliyor.
3. Kilit ekranına geç → bildirim görünüyor, gerçek süreyi gösteriyor (redaction yok); Duraklat/Tur kilit ekranında çalışıyor.
4. Bildirimden **Duraklat** → bildirim "duraklatıldı" olarak kalıyor, süre duruyor, Sürdür ve Sıfırla görünüyor. **Sıfırla** → bildirim kalkıyor.
5. Bildirimden **Sürdür** → kaldığı yerden devam ediyor.
6. Uygulamayı son uygulamalardan kaydırarak kapat → bildirim ve sayaç devam ediyor.
7. Bildirimi kaydırarak kapatmayı dene (kilit ekranında da) → silinmiyor.
8. Widget'ı ekle → çalışırken süre akıyor; Duraklat / Tur / Sürdür / Sıfırla çalışıyor; widget'a dokununca uygulama açılıyor; widget'ı küçültüp büyütünce düğmeler görünür kalıyor.
9. Ekranı kapat, 2 dk bekle, aç → süre doğru ve bildirim hemen saniye moduna dönüyor.
10. Uygulamada 10 sn dokunma → ekran yumuşakça kararıyor; dokun → geri geliyor. Duraklat → karartma iptal. Uygulamadan çık → ekran parlaklığı normal.
11. **Release APK** ile 8. adımı tekrarla (R8 + Glance kontrolü).
12. OEM kartı: Samsung adımları görünüyor mu, "Aç" butonları çöküyor mu (logcat'te `ActivityNotFoundException` olmamalı), muafiyet verilince kart kayboluyor mu? `adb shell dumpsys deviceidle whitelist | grep kronometre`.

### Redmi Note 12 Pro 4G (HyperOS 1)
1. **Ayar yapmadan önce:** Başlat → uygulamayı kaydırarak kapat → ekranı kapat → 2 dk bekle → bildirim hâlâ var mı? (Yoksa ROM öldürüyor demektir.)
2. OEM kartındaki üç adımı tamamla (Otomatik başlatma + Pil: Kısıtlama yok + Son uygulamalar kilidi).
3. 1. adımı tekrarla → bu kez **geçmeli**.
4. `adb shell dumpsys deviceidle whitelist | grep kronometre` → listede görünmeli.
5. Bildirim kilit ekranında görünüyor, aksiyonlar çalışıyor.
6. Widget doğru süreyi gösteriyor (release APK ile de).

### Her iki cihaz
```
adb shell dumpsys notification --noredact | grep -A5 com.oguzh.kronometre   # visibility=0 (PUBLIC) ve ongoing olmalı
```

---

## Ölçüm protokolü

Bulutta cihaz ve `adb` yok; bu komutları **sen** çalıştırıp çıktıları paylaşmalısın.
Sayılar gelene kadar tablo "ölçülmedi" kalır ve kabul kriteri 10 tamamlanmış sayılmaz.
Windows'ta `grep -i` yerine `findstr /i` kullan.

**Hazırlık (her koşudan önce):**
```
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats --enable full-wake-history
```

**Koşu A — ekran kapalı, 60 dk:** Uygulamayı aç → Başlat → uygulamayı kaydırarak kapat →
ekranı kapat → 60 dk bekle (şarja takma). Sonra:
```
adb shell dumpsys batterystats com.oguzh.kronometre > bs_A.txt
adb shell dumpsys cpuinfo | grep -i kronometre          # ~%0 olmalı
adb shell dumpsys meminfo com.oguzh.kronometre          # PSS < 60 MB
grep -i "wake" bs_A.txt                                 # uygulamaya ait wake lock 0 olmalı
grep -i "Screen off discharge" bs_A.txt
adb bugreport bugreport_A.zip                           # Battery Historian için
```

**Koşu B — ekran açık, karartmalı, 10 dk:** Uygulamada Başlat, dokunmadan bırak (10. saniyede
kararır), 10 dk sonra `bs_B.txt` al.

**Koşu C — ekran açık, karartmasız, 10 dk:** `DIM_DELAY_MS`'i geçici olarak çok büyük bir
değere çek, debug APK kur, aynı 10 dk'yı ölç (`bs_C.txt`). Karartmanın kazancını bu verir.

**Baz:** `adb shell am force-stop com.oguzh.kronometre` sonrası 60 dk ekran kapalı ölç.
A ile baz arasındaki `Screen off discharge` farkı ayırt edilemez olmalı.

**Boyut ve açılış:**
```
ls -l app/build/outputs/apk/release/app-release.apk      # < 3.5 MB
adb shell am force-stop com.oguzh.kronometre
adb shell am start -W -n com.oguzh.kronometre/.MainActivity   # TotalTime < 300 ms
```

### Sonuçlar

| Metrik | Hedef | S22 | Redmi Note 12 Pro 4G |
|---|---|---|---|
| A: CPU (60 dk, ekran kapalı) | < %0.5 | ölçülmedi | ölçülmedi |
| A: uygulama wake lock | 0 | ölçülmedi | ölçülmedi |
| A: uygulama kaynaklı wakeup / saat | ≤ ~60 | ölçülmedi | ölçülmedi |
| A: Screen off discharge (mAh/sa) | baz ile aynı | ölçülmedi | ölçülmedi |
| Baz: Screen off discharge (mAh/sa) | — | ölçülmedi | ölçülmedi |
| PSS çalışırken | < 60 MB | ölçülmedi | ölçülmedi |
| Cold start (`TotalTime`) | < 300 ms | ölçülmedi | ölçülmedi |
| APK (R8 release) | < 3.5 MB | 2.63 MB (2 630 979 bayt, CI) | aynı APK |

**Üç koşu yan yana (pil düşüşü, mAh / %):**

| Cihaz | A: ekran kapalı 60 dk | B: karartmalı 10 dk | C: karartmasız 10 dk |
|---|---|---|---|
| S22 | ölçülmedi | ölçülmedi | ölçülmedi |
| Redmi Note 12 Pro 4G | ölçülmedi | ölçülmedi | ölçülmedi |

Ham çıktılar geldikçe `measurements/` altına eklenecek.

## Sürüm ve yayın

- `versionName` / `versionCode` Gradle özelliklerinden gelir (`-PappVersionName`, `-PappVersionCode`); yerelde varsayılan `1.0.0` / `1`.
- `KRONOMETRE_KEYSTORE` (+ `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`) ortam değişkenleri varsa release yükleme anahtarıyla, yoksa debug anahtarıyla imzalanır.
- `v*` etiketi itilince `.github/workflows/release.yml` imzalı `.aab` ve `.apk` üretip GitHub Release'e ekler. `versionCode = 100 + run_number` her çalıştırmada artar. Gereken secret'lar: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
