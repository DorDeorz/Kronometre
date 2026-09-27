# Kronometre — Claude Code Görev Promptu

> Bu dosyayı Claude Code'da proje kökünde aç, sonra "bu dosyayı uygula" de.
> Ya da içindeki **BÖLÜM 0**'ı tek seferlik talimat olarak yapıştır.

---

## BÖLÜM 0 — Tek seferlik talimat (Claude Code'a yapıştırılacak)

Aşağıdaki tamamı tek prompt olarak yapıştırılabilir.

---

Aşağıdaki spesifikasyonu eksiksiz uygula. Belirtilen dosya yapısına sadık kal, kapsam
kaydırma, ek özellik ekleme. Emin olmadığın noktaları **tahmin etme, kod içinde `TODO`
bırakma** — bu durumda bana sor. Kod yazma. Bu prompt'taki her madde bir gerekliliktir.

### Ortam ön koşulları

**Bu depo claude.ai/code üzerinden bulutta çalıştırılacak.** Depoyu sanal makineye
klonlar, değişiklikleri branch'e push'lar. Bu yüzden:

- **Yerel dosya yolu oluşturma.** `C:\dev\Kronometre`, `mkdir`, `cd` gibi komutlar
  bulutta anlamsızdır. Depo kökü çalışma dizinidir, yol senin belirleyeceğin bir şey
  değildir. Böyle bir komut çalıştırma.
- **Yerel Android SDK / JDK aramak yerine** Gradle toolchain'i ve
  `compileSdk`'ı `build.gradle.kts` içinde tanımla; bulut ortamı bunları indirir.
- `local.properties` **commit edilmez** (gitignore'da). Bulutta `sdk.dir` çalışma
  zamanında ayarlanır; dosyayı elle yazıp commit etme.

**İş bölümü — bunu baştan anla, karmasıklık buradan gelir:**

| İş | Nerede |
|---|---|
| Kod yazma, derleme, lint, unit test | **Bulut** (claude.ai/code) |
| Cihaza kurma, elle test, pil ölçümü, OEM ayarları | **Kullanıcının bilgisayarı ve telefonu** |

Kullanıcının elinde iki cihaz var: **Galaxy S22** (API 34+ yolu) ve
**Note 12 Pro 4G** (HyperOS 1, eski API + agresif ROM). Bunlara `adb` ile
erişimin **bulutta olmadığını** unutma. `adb shell` komutu çalıştırmaya çalışma;
ölçüm ve cihaz testi için kullanıcıya ölçüm komutlarını **README'ye yazmasını** ve
sonuçları **kullanıcıdan isteyip** commit etmesini söyle. Ölçüm sayılarını tahmin
etme, uydurma — bu belgede "optimize" iddiası ölçümle desteklenmeli.

- Sürümler: derlemeden önce kurulu Android Studio'nun SDK'sını doğrula. Güvenli varsayımlar:
  - AGP `9.3.x` (stable), `compileSdk` 36, `targetSdk` 36, `minSdk` 23
  - Kotlin: Compose Compiler plugin ile eşleşen stable sürüm
  - `androidx.compose:compose-bom:2026.08.00`
  - `androidx.glance:glance-appwidget:1.2.0` (**1.3.0-alpha KULLANMA** — compileSdk 37
    ve AGP 9.2+ istiyor, kırılır)
  - `androidx.glance:glance-material3:1.2.0`
  - `androidx.core:core-ktx` (en güncel stable)
  - `androidx.datastore:datastore-preferences`
  - `androidx.lifecycle:lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`
  - DI kütüphanesi **kullanma** (Hilt/Koin yok). Manuel singleton + interface. Gerekçe:
    cold-start süresi, APK boyutu, derleme süresi — bu proje için DI'ye gerek yok.

### Paket & kimlik

- `applicationId` / `namespace`: `com.oguzh.kronometre`
- Uygulama adı (Türkçe): **Kronometre**
- Tüm kullanıcıya görünen metinler Türkçe. Kod yorumları yazma.

### Dosya yapısı (birebir bu, fazlası değil)

```
Kronometre/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradle/wrapper/...
├── local.properties            (gitignore)
├── .gitignore
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── drawable/           (ic_stopwatch, ic_play, ic_pause, ic_reset, ic_lap, widget_preview)
        │   ├── layout/
        │   │   ├── activity_main.xml          (sadece ComposeHostView)
        │   │   └── widget_remote_chronometer.xml  (AndroidRemoteViews için Chronometer)
        │   ├── values/
        │   │   ├── strings.xml
        │   │   ├── colors.xml
        │   │   └── themes.xml
        │   ├── xml/
        │   │   ├── backup_rules.xml
        │   │   ├── data_extraction_rules.xml
        │   │   └── stopwatch_widget_info.xml
        │   └── mipmap-anydpi-v26/  (adaptive launcher icon)
        └── java/com/oguzh/kronometre/
            ├── MainActivity.kt
            ├── data/
            │   ├── StopwatchState.kt
            │   ├── StopwatchRepository.kt
            │   └── OemProfile.kt
            ├── service/
            │   ├── StopwatchService.kt
            │   ├── StopwatchNotifier.kt
            │   └── StopwatchActions.kt
            ├── ui/
            │   ├── StopwatchViewModel.kt
            │   ├── StopwatchScreen.kt
            │   ├── StopwatchComponents.kt
            │   ├── ScreenDimController.kt      (karartma durum makinesi, GEREKLİLİK 5)
            │   ├── theme/{Color,Theme,Type}.kt
            │   └── oem/BatteryOptimizationHelper.kt
            └── widget/
                ├── StopwatchWidget.kt
                └── StopwatchWidgetReceiver.kt
```

---

## GEREKLİLİK 1 — Kronometre çekirdeği

### Durum modeli

`data/StopwatchState.kt`:

```kotlin
data class StopwatchState(
    val elapsedMs: Long,          // birikmiş geçen süre (durdurulmuş toplam)
    val runningSinceElapsedRealtime: Long?,  // null = duraklatılmış
    val laps: List<Long> = emptyList()
)
```

- `currentElapsedMs = elapsedMs + (runningSinceElapsedRealtime?.let { SystemClock.elapsedRealtime() - it } ?: 0)`
- `elapsedRealtime()` kullan, `currentTimeMillis()` **kullanma** (saat değişimi/NTP kayması kronometreyi bozar).
- `elapsedRealtime` API 17+, sorun yok.

### Komutlar

`Start`, `Pause`, `Reset`, `Lap`. Her komut sonucu `StateFlow<StopwatchState>` yayınlanır.

- `Start`: `runningSince = elapsedRealtime()` (idempotent: zaten çalışıyorsa no-op)
- `Pause`: `elapsedMs = currentElapsedMs()`, `runningSince = null`
- `Reset`: `elapsedMs = 0`, `runningSince = null`, `laps = emptyList()`
- `Lap`: yalnızca çalışırken; `laps + currentElapsedMs()`

### Persistans

`StopwatchRepository`: `DataStore Preferences`.

- Anahtarlar: `elapsed_ms`, `running_since`, `laps` (LongArray/CSV olarak).
  **Üç anahtar.** Bildirim hassasiyeti bilerek **ayar değildir** — `isInteractive`'dan
  türetilir, bkz. GEREKLİLİK 2. Kullanıcı ayarı kaldırıldı.
- **Her durum değişiminde bir kez yaz, saniyede yazma.** (optimizasyon şartı)
- Uygulama yeniden açılışta: kayıtlı `elapsed_ms` yüklensin, `running_since = null` kabul
  edilip duraklatılmış olarak devam etsin. Kullanıcı "kaldığı yerden devam et" bekler.
  Süreç yeniden başlatıldığında çalışan bir kronometre kaldırılamaz; bu davranış
  **kasıtlı** ve README'de belgelenmeli.

### Zamanlayıcı yok — çok önemli

Uygulamada **`Handler`/`while(true)`/1 saniyelik `delay` döngüsü olmayacak.**
Süre arayüzü kendi kendine çizilir (`Chronometer` mekanizması, bkz. GEREKLİLİK 2/3).
Tek istisna: UI'da saniye sayacının **hundreds** hanesi gerekirse, o zaman
`LaunchedEffect` + `delay(100)` ile yalnızca **görünürken** (`Lifecycle` STARTED) çalışan
bir ticker kullan ve `onStop`'ta durdur. Sıfır kesir gösterimi şart değilse hiç kullanma.

---

## GEREKLİLİK 2 — Sürekli bildirim (en kritik kısım)

**Kural: bildirim yalnızca kronometre çalışırken vardır. Duraklatılınca tamamen kaybolur.**

### Foreground service tipi (API 34+ kritik)

`AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<service
    android:name=".service.StopwatchService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="User-started stopwatch timer that must keep counting while the screen is off" />
</service>
```

- `startForeground` çağrısında **`ServiceCompat.startForeground(this, ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)`** kullan (androidx.core).
  Ham `startForeground(id, n)` API 34'te `MissingForegroundServiceTypeException` fırlatır —
  bu yüzden asla ham çağırma.
- `ServiceCompat` API 34 öncesinde tipi yok sayar → tek kod yolu eski Android'de de çalışır.
- `specialUse` nedeni: kronometre için ayrılmış bir FGS tipi **yok**. `dataSync`'i
  kötüye kullanma (Play politikası ihlali + 6 saatlik kısıt). `shortService` 3 dakikalık
  sınıra takılır. `specialUse` tek doğru seçim.
- `onTimeout` / süre sınırı `specialUse` için **yok** — sınırsız çalışır.

### Bildirim kanalı

```kotlin
val channel = NotificationChannel(
    CHANNEL_ID,
    "Kronometre",
    NotificationManager.IMPORTANCE_LOW,   // ses/titreşim yok
).apply {
    description = "Çalışan kronometre süresi ve kontrolleri"
    setShowBadge(false)                  // rozet meselesini tamamen ortadan kaldır
    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    enableVibration(false)
    setSound(null, null)
}
```

`importance = LOW` şart: saniyelik güncellemelerde ses/titreşim üretmemek için
`setOnlyAlertOnce(true)` de gerekiyor (ikisi birlikte).

### Bildirim içeriği

```kotlin
NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_stopwatch)   // monochrome, beyaz siluet, ASLA renkli/alpha'lı
    .setContentTitle("Kronometre çalışıyor")
    .setContentText("Duraklatmak için dokun")   // çalışırken gösterilecek ipucu metni
    .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setPublicVersion(publicVersionBuilder.build())   // aşağıya bak
    .setOngoing(true)                            // kapatılamaz, swipe edilemez
    .setOnlyAlertOnce(true)
    .setSilent(true)
    .setShowWhen(false)
    .setUsesChronometer(true)                    // KİLİT NOKTA: kendi kendine çizer
    .setWhen(System.currentTimeMillis() - state.currentElapsedMs())  // kayan sayaç
    .setContentIntent(openAppPendingIntent)
    .addAction(0, "Duraklat", pausePendingIntent)
    .build()
```

### "Kilit ekranında da görünsün" — nasıl sağlanır

1. `.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)` → kullanıcı kilit ekranı
   bildirim gizlemeyi seçse bile içerik görünür kalır.
2. `.setPublicVersion(...)` → **şart**. Vermezsen sistem kendi kırmızı redaction'ını
   üretir ve kilit ekranında "Devam eden bir uygulama" gibi boş bir şey çıkar.
   `publicVersion` = gerçek başlık + gerçek `setWhen` + aynı aksiyonlar, **süreyi
   göstererek**. Kilit ekranındaki sürükleme/eylem kısıtları sistem tarafından yönetilir.
3. `.setOngoing(true)` → kullanıcı bildirimi kapatamaz. "Bildirim aşağı kaldırıldığında da
   sürekli gözükecek" şartının karşılığı budur; panel çekildiğinde de kalır çünkü
   kapatılamayan bir bildirimdir.
4. `.setCategory(CATEGORY_STOPWATCH)` → Android 13+ sistem kararlarında kronometre
   bildirimlerini gruplamaya/özetlemeye daha az meyilli kılar.
5. **Kanal `IMPORTANCE_LOW` + `setSound(null)`** → her güncellemede ses çalmaz.

### Süre güncellemesi — `setUsesChronometer` tekniği (optimizasyon kalbi)

Bu, "çok iyi optimize" şartının en önemli parçası.

- `setUsesChronometer(true)` + `setWhen(...)` → **sistem kronometreyi kendi çizer**,
  saniyede yeniden bildirim göndermez. Pil: sıfır. Doğruluk: cihaz saati değil,
  monotonic kernel sayacı kullanılır.
- `setShowWhen(false)` → sabit "şu saatte başladı" damgası gizlenir, sadece kronometre görünür.
- **Duraklatma:** `elapsed = currentElapsedMs()` hesapla, servisi durdur, bildirimi **kaldır**.
- **Devam etme:** `setWhen(System.currentTimeMillis() - elapsed)` ile bildirimi tekrar gönder.
  Kronometre kaldığı yerden devam eder. Bu yüzden 1 saniyelik `Handler` döngüsüne
  **hiç gerek yok**.

### Otomatik bildirim hassasiyeti — saniye/dakika, kullanıcı ayarı olmadan (KRİTİK)

Kullanıcı kararı: **ayar yok, otomatik.** Mod tek bir değişkenden türetilir.

### Temel gözlem: saniye her yerde aynı değil

Saniye göstermenin maliyeti, **ekranın durumuna** bağlı olarak iki farklı şey:

| Durum | Maliyet | Neden |
|---|---|---|
| Ekran açık, kullanıcı bakıyor | **~0** | Ekran zaten 60–120 Hz yenileniyor, SystemUI zaten uyanık. Kronometre redraw'ı bunun içine giriyor, ayrı bir uyanma yaratmıyor |
| Ekran kapalı / Always-on Display, bildirim görünür | **gerçek** | Saniyede bir Display uyanması |
| Bildirim hiç görünmüyor | **0** | Render edilmiyor, maliyet yok |

Yani pahalı olan tek senaryo: **telefon ekranı kapalıyken bildirimin görünür olması.**
Kronometrenin en tipik kullanımı da tam olarak bu ("telefonu masaya bırak, bir
süre sonra bak"). Bu yüzden karar kullanıcıya sorulmadan **otomatik** alınabilir:
kullanıcı bakarken bedava olanı göster, bakmadığında pahalı olanı.

### Kural

```
PowerManager.isInteractive  == true   →  saniye   (setUsesChronometer)
PowerManager.isInteractive  == false  →  dakika   (statik metin + dakikalık tazeleme)
```

Tek değişken, tek dal, kullanıcı kararı yok, ayar ekranı yok, DataStore anahtarı yok.

### Neden `setUsesChronometer` doğrudan kullanılamıyor

Platform kronometresi **saniyeyi gizleyemez** — daima `MM:SS` veya `H:MM:SS`
üretir, format parametresi yoktur. Bu yüzden dakika modu mekanizmayı bırakmayı
gerektirir: `setUsesChronometer(false)` + kendi metnimiz + onu **kendimizin**
tazelemesi.

Bu, GEREKLİLİK 5'teki "sıfır döngü" ilkesinin tek istisnasıdır ve **dakikalıktır.**

### Ekran durumu nasıl öğrenilir — iki tuzak

1. **`isInteractive` ile POLL YAPMA.** Saniyede bir sormak, tam olarak kaçınmaya
   çalıştığın şeyi yapar. Bunun yerine `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF`
   broadcast'lerini dinle. Bunlar yalnızca gerçek geçişlerde tetiklenir.
2. **Broadcast'i MANIFEST'te değil, `registerReceiver` ile kaydet.** Android 8+
   (API 26) implicit broadcast kısıtı, manifest'te bildirilen receiver'lara
   `SCREEN_OFF` ulaştırmaz. Servis içinde dinamik kayıt şart.

```kotlin
// API 34+ zorunluluk: RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED bayrağı şart,
// aksi halde SecurityException. Sistem broadcast'leri için NOT_EXPORTED doğru olan.
ContextCompat.registerReceiver(
    context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
)
```

- Servis `onCreate`'de kaydet, `onDestroy`'da **mutlaka** `unregisterReceiver` çağır
  (sızıntı olur ve `IllegalArgumentException` atar).
- Servis başlarken mevcut durumu da hesaba kat: `isInteractive` çağır, `SCREEN_OFF`
  olayını beklemeden doğru modla başla. Widget'tan ekran kapalıyken başlatıldığında
  bu fark yaratır.
- `isInteractive` API 20+; `minSdk 23` olduğu için ek guard gerekmez.

### Geçişler — olay başına tek iş, saniyede hiçbir iş

| Olay | Aksiyon |
|---|---|
| `ACTION_SCREEN_ON` | dakika zamanlayıcısını iptal et → `notify()` bir kez, `setUsesChronometer(true)` |
| `ACTION_SCREEN_OFF` | `notify()` bir kez, statik `H:MM` → dakika zamanlayıcısını başlat |
| Servis başlangıcı | `isInteractive`'a göre doğru modu kur |

Her geçiş **tek bir `notify()`** demektir. Geçişler saatte birkaç kez olur, saniyede
bir kez değil. Kritik davranış: ekran aydınlandığı anda bildirim **kendiliğinden
saniyeye döner** — kullanıcı ekrana baktığı için tam olarak istediği şey.

**Sık yapılan yanlış anlama:** uygulama ekranı karartıldığında `isInteractive` hâlâ
`true` kalır (ekran fiziksel olarak açıktır). Bunu "ekran kapandı" sanıp
karartmayı da tetikleyici yapma. **Gerekmez ve yanlış olur:** o durumda bildirim
zaten görünmüyor, dolayısıyla render edilmiyor, saniye maliyeti zaten 0. Karartma
bir görsel karardır, `isInteractive`'ı değiştirmez.

### Statik metin biçimi

```
.setUsesChronometer(false)
.setShowWhen(false)
.setContentText(DateUtils.formatElapsedTime(elapsedMs / 1000).substringBeforeLast(':'))
```

- `DateUtils.formatElapsedTime` doğru seçim — `H:MM:SS` üretir ve 24 saati aşınca
  `G:SS:DD:SS` biçimine geçer. `substringBeforeLast(':')` bunu doğru keser
  (`1:02:03:04` → `1:02:03`). Elle `String.format` yazma, bu kenar durumu kaçar.
- `publicVersion` **aynı metinle yeniden kurulmalı.** Kilit ekranında eski süreyi
  göstermek, hiç göstermemekten kötüdür.
- Zamanlayıcı serviste `Handler.postDelayed` ile, **dakika sınırına hizalı:**
  `60000 - (SystemClock.elapsedRealtime() % 60000) + 50`
  (50 ms pay: tam sınırda post edilirse bir saniye geç kalır).
  `elapsedRealtime`'a hizala, duvar saatine değil — gösterilen değer geçen süre.
- Zamanlayıcıyı `pause`/`reset`/`onDestroy`/ekran açılışında **iptal et**, yoksa sızar.
- `SCREEN_ON` geldiğinde `when`'ı **yeniden kurmayı unutma**:
  `setWhen(System.currentTimeMillis() - state.currentElapsedMs())`. Aksi halde
  saniye moduna geçince bildirim eski değerden başlar.

### Tek build fonksiyonu — şart

Bildirim birden fazla yoldan üretiliyor (saniye modu, dakika modu, tur, sıfırla,
ekran geçişleri). Her biri ayrı yazılırsa bayraklar ayrışır:

- `setOngoing` bir güncellemede unutulursa → bildirim **kapatılabilir** hale gelir
  (kullanıcının temel şartı ihlal edilir).
- `setPublicVersion` unutulursa → kilit ekranı **eski süreyi** gösterir.
- `setSilent`/`setOnlyAlertOnce` unutulursa → güncellemede ses çalar.

```kotlin
fun build(state: StopwatchState, interactive: Boolean): Notification
```

Bu fonksiyon hem `startForeground` çağrısında hem her tazelemede tek kaynak olsun.

### Beklenen maliyet (dürüst)

| Mod | Tetikleyen | `notify()`/saat | Uygulama CPU'su | Sistem redraw'ı |
|---|---|---|---|---|
| Ekran açık → saniye | `SCREEN_ON` + tur/sıfırla | **~1** | ~%0 | 1 Hz (bedava) |
| Ekran kapalı → dakika | `SCREEN_OFF` + dakikalık | **~60** | < %0.5 | yok |

Dakika modunda uygulama tarafı uyanma sayısı artar (0 → ~60/saat), ama kazanılan
şey **saatte 3600 SystemUI redraw'ının ortadan kalkması**. Gerçek pil ölçümü iki
modda da boşta kullanımdan ayırt edilemez kalmalı — kabul kriteri bu.

---


### Aksiyon PendingIntent'leri

- `Duraklat` → `PendingIntent.getService(..., Intent(ACTION_PAUSE).setClass(this, StopwatchService::class.java), FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`
- `Devam Et` → aynı şekilde `ACTION_RESUME`
- `ContentIntent` → `PendingIntent.getActivity(..., Intent(this, MainActivity::class.java), ...)`, `Intent.FLAG_ACTIVITY_SINGLE_TOP`

Kritik kurallar:
- **Her PendingIntent'te `FLAG_IMMUTABLE` zorunlu** (API 31+ zorunluluk).
- **Farklı aksiyonlar için farklı `requestCode` kullan.** Glance ve doküman notu: aynı
  `PendingIntent` (aynı `filterEquals`) conflates olur — yani iki buton birbirinin
  yerine geçer. `requestCode` = aksiyonun `hashCode()` gibi benzersiz bir değer olsun.
- Bildirim aksiyonundan FGS başlatmak **izinlidir** (kullanıcı bildirim aksiyonuna dokundu,
  bu bir kullanıcı etkileşimi sayılır). Android 12 trampoline kısıtı burada devreye girmez.
- `ForegroundServiceStartNotAllowedException` yakala; kullanıcıya "başlatılamadı" diye
  sessizce yutmadan, uygulama içi bir hata mesajı göster.

### Servisin yaşam döngüsü

`StopwatchService : LifecycleService` (veya düz `Service` + kendi scope'u).

| Olay | Aksiyon |
|---|---|
| `onStartCommand(ACTION_RESUME)` | `startForeground(ID, notif, SPECIAL_USE)`, state güncelle, widget'ı güncelle, `START_STICKY` |
| `onStartCommand(ACTION_PAUSE)` | `ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)`, `stopSelf()`, state güncelle, widget güncelle |
| `onStartCommand(ACTION_LAP/RESET)` | bildirimi `NotificationManagerCompat.notify()` ile tazele (FGS zaten çalışıyor) |
| `onDestroy` | repo'yu kapat, **dakikalık zamanlayıcıyı iptal et**, widget'ı güncelle |

- Servis, Activity'den bağımsız olarak **tek başına** çalışır. Uygulama kapatılsa bile
  kronometre doğru sürmeye devam eder.
- `START_STICKY`: süreç öldürülüp yeniden başlatılırsa `onStartCommand(null intent)` gelir →
  repo'daki `elapsed`i kullanıp duraklatılmış duruma geç ve bildirimi yeniden kur.
- **WakeLock alma.** Kronometre arayüz tarafında çizildiği için uyku gerekmez; ekran
  kapalıyken çalışması FGS ile sağlanır. `PARTIAL_WAKE_LOCK` almak boşa pil yakar.
  (Manifest'te izin dursun, kullanma.)

### `POST_NOTIFICATIONS` izni (API 33+)

- Activity'de ilk açılışta `rememberLauncherForActivityResult(RequestPermission())` ile iste.
- **Reddedilse de çökme.** FGS çalışmaya devam eder, sadece bildirim görünmez.
  Reddedildiğinde uygulama içinde kalıcı bir uyarı şeridi göster: "Kronometre bildirimi
  kapalı — süreyi göremiyoruz, izin ver" + ayarlara yönlendiren buton.
- API 33 altında (Xiaomi cihazın) bu izin hiç yok — kod `Build.VERSION.SDK_INT >= 33`
  guard'ıyla çalışmalı.

---

## GEREKLİLİK 3 — Ana menü widget'ı (Glance)

`StopwatchWidget : GlanceAppWidget` + `StopwatchWidgetReceiver : GlanceAppWidgetReceiver`.

### Kayıt

`AndroidManifest.xml`:

```xml
<receiver
    android:name=".widget.StopwatchWidgetReceiver"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/stopwatch_widget_info" />
</receiver>
```

`res/xml/stopwatch_widget_info.xml`:
- `minWidth` 250dp, `minHeight` 110dp, `targetCellWidth`/`targetCellHeight` (API 31+)
- `updatePeriodMillis="1800000"` (30 dk — sistem **minimum** 30 dk zorlar, 1dk'ya
  düşüremezsin; buna güvenme, aşağıdaki canlı sayaç yöntemini kullan)
- `widgetCategory="home_screen"`, `resizeMode="horizontal|vertical"`, `initialLayout` Glance'ın
  önerdiği layout, `previewImage`, `description`

### "Ana menü" içeriği (tek ekran olduğu için)

Widget'ın kendisi ana menüdür. İçinde:
1. Büyük süre göstergesi
2. Durum rozeti (Çalışıyor / Duraklatıldı)
3. Birincil buton: **Başlat** (çalışırken) / **Duraklat** (durmuşken)
4. İkincil buton: **Tur** (yalnızca çalışırken), **Sıfırla** (yalnızca durmuşken)
5. Widget'ın **geri kalanına** tıklayınca uygulamayı aç: `actionStartActivity<MainActivity>()`

### Canlı sayaç — `AndroidRemoteViews` + klasik `Chronometer` (KRİTİK)

Glance kompozisyonu uzaktaki process'te **tek sefer** çalışır; saniyede bir yeniden
kendi içinde çalışmaz. Dolayısıyla widget'ta saniye saniye güncellemek için:

- `AndroidRemoteViews(remoteViews = chronometerRemoteViews)` kullan ve içine
  `RemoteViews` ile bir `android.widget.Chronometer` koy
  (`res/layout/widget_remote_chronometer.xml`).
- Kronometreye `setChronometerCountDown(false)`,
  `setBase(System.currentTimeMillis() - elapsed)`, `start()`.
- Bu sayede sayaç **launcher process'inde kendi kendine akar** — uygulamamız
  çalışmıyorken bile doğru süreyi gösterir, sıfır pil maliyeti, `updatePeriodMillis`'e
  bağlı değil.
- Duraklatılmışken `setChronometerCountDown` yerine kronometre `stop()` edilir ve
  metin statik olarak yazılır (`android:visibility` ile gizle/göster veya
  `RemoteViews.setTextViewText`).
- **Alternatif (kabul edilebilir ama daha kötü):** `actionRunCallback` + WorkManager.
  `updateAppWidgetState` ile 1 sn'de bir güncelleme. Bu yol arka planda WorkManager
  job'ları çalıştırır (pil), 15 dk minimum periyot kısıtına takılır ve launcher
  widget'ı güncellemeyi geciktirebilir. **Tercih etme;** ancak `Chronometer`
  RemoteViews bir cihazda sorun çıkararsa geçici geri dönüş olarak kullan.

### Widget aksiyonları

- `Başlat` / `Duraklat` → **`actionStartService(intent)`** (Glance bunu
  foreground-service PendingIntent'e sarar ve `ActionTrampolineType.FOREGROUND_SERVICE`
  kullanır). `intent` = `Intent(context, StopwatchService::class.java).setAction(ACTION_RESUME)`.
  `actionRunCallback` **kullanma** — WorkManager worker'ı arka planda çalışır, FGS
  başlatamaz ve kullanıcı etkileşimi sayılmaz.
- `Tur` / `Sıfırla` → yine `actionStartService` (servis çalışırken kolay), çalışmıyorsa
  gizli.
- `actionStartActivity` kullanırken **`clickable(action)`, `clickable { }` değil.**
  Lambda (callback) sürümü activity başlatmaz — bu bilinen bir Glance tuzağıdır.

### Senkronizasyon

- Servis her durum değişiminde `StopwatchWidget.updateAll(context)` çağırsın
  (`CoroutineScope(Dispatchers.IO).launch` içinde, `goAsync` benzeri güvenli çağrı).
- Widget, servis **çalışmıyorken** de güncel `elapsed`i göstermeli → widget içeriği
  repo/DataStore'dan okumalı, sadece servisten veri almamalı.
- Widget'a `GlanceAppWidgetReceiver`'ın `onEnabled`/`onDisabled`/`onRestored` override'ları
  yazılması şart değil ama `onAppWidgetOptionsChanged` ile `SizeMode` doğru beslenmeli.

---

## GEREKLİLİK 4 — Eski Android desteği

`minSdk = 23` (Android 6.0). Bunu aşağı çekme: **Glance 1.2.0 `minSdk 23` istiyor**
(1.2.0-beta01'de 21'den yükseltildi). Yani Compose tarafı 21'e inebilirdi ama widget
yüzünden 23 taban doğru seçim.

Android 23–32 için **ayrı kod yolu yazma.** Şunlar yeterli:

| Özellik | Uyum yolu |
|---|---|
| FGS tipi zorunluluğu (34+) | `ServiceCompat` → eski sürümlerde tip yok sayılır |
| `POST_NOTIFICATIONS` (33+) | `Build.VERSION.SDK_INT >= 33` guard |
| `NotificationCompat` | Her şey `NotificationCompat` üzerinden, asla platform `Notification.Builder` değil |
| Kanal (26+) | `NotificationManagerCompat.createNotificationChannel` — 26 altında no-op |
| `setPublicVersion` (21+) | Doğrudan desteklenir |
| `Chronometer` (24+) | API 24 altında kendi kendine akan sayaç yok. `minSdk 23` olduğu için **23'te** statik metin göster, gerisinde `Chronometer`. `Build.VERSION.SDK_INT >= 24` guard |
| `PendingIntent.FLAG_IMMUTABLE` (23+) | `minSdk 23` olduğu için her yerde güvenli |
| Material You dinamik renk (31+) | `dynamicColor = Build.VERSION.SDK_INT >= 31`, altında sabit tema |
| Widget önizleme (12+) | `previewImage` + `previewLayout` guard |
| `setExtras`/bundle | Kaçın |

Ayrıca:
- `desugar` açık olsun (`isCoreLibraryDesugaringEnabled = true` +
  `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")`) — `java.time`
  veya stream API kullanacaksan gerekir. Kullanmayacaksan da Compose/Glance
  bağımlılıklarında bazıları ister; açık bırak, hatasız.
- API 23/24'te `Notification.Action` ikonu zorunlu; API 31+'de ise
  `Icon.createWithResource` daha temiz ama eski yol da hâlâ çalışır → **`addAction(int, ...)`**
  kullan (ikon kaynak ID'si).
- RTL desteği: `supportsRtl="true"`, manifest'te. Sağ/sol sabit kodlama.
- `supports-screens` / `compatibleScreens` gerekmiyor, hedefimiz telefon.

---

## GEREKLİLİK 5 — Güç ve ısı optimizasyonu

Pilot cihazlar: **Redmi Note 12 Pro 4G (HyperOS 1)** ve **Samsung Galaxy S22**.
İkisi de modern donanımlı; asıl fark **sistem davranışı**, donanım değil.

### Temel fikir — ölçülmesi gereken şey "hafiflik" değil, güç çekimidir

Bir kronometre **saymaz**. Saymak iki sayının aritmetiği:

```
currentElapsedMs = accumulatedMs + (SystemClock.elapsedRealtime() - runningSince)
```

Bu hesap **hiçbir zaman çalıştırılmak zorunda değildir** — yalnızca bir değer
okunduğunda hesaplanır. Doğru yazılmış bir kronometre, çalışırken **hiçbir CPU
döngüsüne, hiçbir wake lock'a, hiçbir periyodik işe sahip değildir.**

Isıyı/pili üreten şey "sayma" değil, **saniyeyi yeniden yayınlamaya çalışmak**. Aşağıdaki
sıralama, gerçek maliyet büyüklüğüne göredir.

### Hedef sıfır değil, "ölçülebilir taban çizgisi"

Kullanıcı kararı: **sıfır olması gerekmiyor; olabildiğince optimize, düşük tüketim.**

Bu, "kabul edilebilir" demek değil, "ölçülmeyen hiçbir şey kabul edilmez" demektir.
Hedef sıfırın kendisi değil, **boşta kullanımdan ayırt edilemeyen bir taban çizgisi**dir.
Bunun için:

- Her optimizasyon kararı bir **sayı**la desteklenmeli. "Hafif" geçmez, "saatte 1
  bildirim" geçer.
- Trade-off'lar **açıkça belgelenmeli**, sessizce kabul edilmemeli. Dakika modunda
  saatte 60 uyanma var; bu bir hata değil, ölçülmüş bir bedel. Belgelenmemiş her
  trade-off, sonradan "optimize etmedin" diye cezalandırılacak gizli bir maliyettir.
- **Kazanılan her mikroyararlama ölçülerek kabul edilsin.** "%2 daha iyi" ölçülmüşse
  alınır; tahminse alınmaz. Ölçüm pahalıysa ve kazanç sınırdaysa, yapma ve
  README'ye "denendi, ihmal edildi" diye yaz.
- Aşağıdaki tabloda tek bir satır tutmuyorsa çalışma bitmedi. Tutuyorsa, daha da
  iyileştirme için makul bir sebep yok — dur ve kullanıcıya sor.

### Maliyet sıralaması (büyükten küçüğe)

| # | Hata | Neden pahalı | Bunun yerine |
|---|---|---|---|
| 1 | Saniyede bir `NotificationManagerCompat.notify()` | Her çağrı SystemUI'a binder IPC, view yeniden şişirme, yeniden ölçüm, yeniden çizim. 1 saat = 3600 post. Bildirim görünmese bile gider. | `setUsesChronometer(true)` — toplam **1** post, sonra SystemUI kendi `TextView`'ını kendi çizer |
| 2 | `Handler` / `while(true) { delay(1000) }` / `LaunchedEffect` ile saniye döngüsü | Derin uyku ucuz, **uyanmak pahalı**. Saatte 3600 kez CPU'yu suspend'tan çıkarıp geri yatırırsın; her seferinde ısı. | Döngü hiç kurma. Gerekirse UI'da görünürlüğe bağlı, 100 ms ticker |
| 3 | `PARTIAL_WAKE_LOCK` alma | CPU hiç uyumaz → ısı. Ayrıca **Google Play Android vitals** kuralı: 24 saatte 2 saatten fazla partial wake lock = "aşırı pil tüketimi" uyarısı (Mart 2026'dan beri). | **Hiç alma.** Manifest'te izin dursun, kodda kullanma |
| 4 | Saniyede bir DataStore yazma | Her yazma = serileştirme + disk I/O + `fsync` benzeri bekleyiş. Sürekli disk erişimi hem ısı hem flash aşınması. | Yalnızca **durum değişiminde** yaz (başlat/durdur/tur/sıfırla) |
| 5 | `FLAG_KEEP_SCREEN_ON` süresiz | Ekran açık kalmak kronometre uygulamalarında **en büyük pil kalemi** (uygulamadan çok, kullanıcıdan kaynaklanır). | Yalnızca `isRunning` iken, ve kullanıcı görebilsin diye. Kapatılabilir ayar düşün (bkz. aşağıda) |
| 6 | Widget'ı saniyede bir `updateAll()` | Her çağrı **launcher sürecinde** Glance kompozisyonu + `RemoteViews` yeniden şişirme. Senin pil sorunun, senin uygulamanın değil — launcher'ın pil sorunu olur. | Yalnızca durum değişiminde. Sayıyı `AndroidRemoteViews` + `Chronometer` yapsın |
| 7 | Ekranda saniye saniye recomposition | Activity görünürken 1 recomposition/sn. Küçük ama gerçek. | Ticker metnini **kendi composable'ına** izole et, `GlanceModifier`/iptal davranışı ile durdur. Veya kuruş (ondalık yok) |
| 8 | Ağ izni / Firebase / analitik | Gereksiz. | Dependency ekleme |

### Kırk elli altı satır önce gelen üç karar

1. **Bildirimde `setUsesChronometer(true)`** → 1 post, sistem çizer. (Satır 1'in
   çözümü; `PROMPT.md` GEREKLİLİK 2'de tanımlı.)
2. **Widget'ta `AndroidRemoteViews` + `Chronometer`** → launcher çizer, uygulama
   hiçbir şey yapmaz. (Satır 6'nın çözümü; GEREKLİLİK 3'te tanımlı.)
3. **Servis boşta bekleyen bir nesne.** `onStartCommand` durumu kurar, bildirimi
   bir kez gönderir ve çıkar. Serviste **hiçbir coroutine döngüsü olmasın** —
   `isRunning` bir `val` olsun, bir `StateFlow` olsun, döngü olmasın.

**Tek istisna:** kullanıcı "bildirimde saniye gösterimi" ayarını kapatırsa
(GEREKLİLİK 2) serviste bir **dakikalık** `Handler.postDelayed` bulunur. Bu bilinçli
bir istisnadır ve dakikalıktır — saniyelik olmamalıdır.

### Bilinçli ödünleşme: kilit ekranı ve saniye

Dürüstlük notu: `setUsesChronometer` kullanıldığında bildirim **görünür olduğunda**
(panel açık veya kilit ekranında) SystemUI kronometre metnini **saniyede bir
yeniden çizer.** Bu bizim CPU'muz değil, ama kilit ekranında saniye başına bir
görüntü/Display uyanması demektir.

Bu, saniyeli bir kronometreyi kilit ekranında göstermenin **kaçınılamaz** maliyetidir
ve Google Clock'un da yaptığı şeydir. Daha ucuz alternatif — bildirimde saniyeyi
göstermemek — kullanıcı isteğiyle **ayar olarak eklendi** (GEREKLİLİK 2).

**Karar: saniye gösterimi varsayılan, tek dokunuşla kapatılabilir.** Ayar
GEREKLİLİK 2'de tanımlı. Bilinen sınırlamalara şunu yaz: "ekran kapalıyken
bildirim görünürse (AOD/kilit ekranı) saniye açık modda saniyede bir SystemUI
redraw olur; ayar kapatılırsa bu ortadan kalkar, karşılığında uygulama dakikada bir
kendi bildirimini tazeler."

### Ekran açık kalması — EN BÜYÜK pil kalemi

Kronometre çalışırken ekranı açık tutmak, uygulamanın yapabileceği **tek başına en
büyük** pil tüketimi. Uygulamanın geri kalanı (bildirim, widget, servis) ekran kapalıyken
neredeyse sıfır maliyetli; ekran açıkken ise telefonu ısıtan şey neredeyse tamamen
ekran panelidir. Buna göre davranmak gerekir.

Kullanıcı kararı: **süreli.** Gerçek bir kronometre yüzü gibi — çalışırken birkaç
saniye sonra ekran söner, kullanıcı dokununca geri gelir.

**Süre: 10 saniye, sabit.** Ayar olarak sunma, `const` ile tut. Değiştirmek
tek satırlık iş.

### Neden `goToSleep` değil

`PowerManager.goToSleep()` imza seviyesi `DEVICE_POWER` izni istiyor — normal uygulama
 alamaz. Bu yol kapalı. Doğru çözüm **pencerenin parlaklığını düşürmek**.

### Uygulama

| Faz | Davranış |
|---|---|
| Aktif | `keepScreenOn = true`, `window.attributes.screenBrightness = BRIGHTNESS_DEFAULT` |
| 10 sn hareketsizlik sonrası | parlaklığı ~1 sn'de kademeli düşür → neredeyse siyah |
| Karartılmış | `keepScreenOn = false`, `screenBrightness = BRIGHTNESS_RESTORE` (sistem değeri) |
| Herhangi bir kullanıcı etkileşimi | parlaklığı geri getir, `keepScreenOn = true`, sayacı sıfırla |

Kritik ayrıntılar:

- **Parlaklığı `-1f` ile geri yükle, sıfırla.** `BRIGHTNESS_DEFAULT = -1f` sistem
  parlaklığına döner. Yanlışlıkla `0f` bırakırsan cihazın parlaklık ayarı bozulur ve
  kullanıcı uygulamayı kapattığında ekran siyah kalır. Bu bir destek talebidir.
- Karartmayı **anlık zıplama değil, kısa animasyonla** yap (yaklaşık 1 sn). Gerçek
  kronometre yüzü gibi hissettirir ve kırık gibi görünmez.
- **Hareket algılama:** kök `Modifier.pointerInput(Unit) { awaitPointerEventScope
  { while (true) { awaitPointerEvent(); onInteraction() } } }`. Bu tüm işaretçi
  olaylarını **tüketmeden** gözlemler, dokunulan her şeyi yutmayı engellemez.
  Klavye girişi ve `onResume` da sayacı sıfırlamalı.
- **Kronometre durduğunda karartma iptal olsun** ve ekran normal kalsın. Sıfır
  süreli bir ekranda kararmak saçma.
- **Activity `onStop`/`onDestroy`'da parlaklığı mutlaka geri yükle.** Pencere
  yok edilirken karartılmış kalırsa cihaz siyah ekranla açılır.
- `window.addFlags(FLAG_KEEP_SCREEN_ON)` yerine Compose tarafında
  `view.keepScreenOn = true` kullan — daha temiz ve temizlenmesi kolay.
- Ekran zaten karartıldıysa `KEEP_SCREEN_ON` bayrağını da bırak. Sistem, bayrak
  kalktıktan sonra kendi ekran zaman aşımını saymaya başlar; ikisini birlikte
  bırakırsan bekleme süresi gereksiz uzar.

### Beklenen etki

Bu tek başına, uygulamanın geri kalanındaki tüm optimizasyonlardan daha fazla pil
kazandırır. Ölçüm protokolüne **üçüncü bir koşu** ekle: 10 dakika boyunca ekranı
açık bırak, saniye modunda ne kadar pil gittiğini ölç. Sonucu README'ye yaz —
kullanıcı bu sayıyı görmek ister.

### Dikkat: karartma `isInteractive`'ı değiştirmez

Pencere karartıldığında ekran fiziksel olarak açıktır, dolayısıyla
`PowerManager.isInteractive` **hâlâ `true` döner** ve bildirim saniye modunda kalır.
Bu **yanlış değil, doğru davranış:** o anda bildirim zaten hiç görünmüyor, yani
render edilmiyor, yani saniyenin maliyeti zaten 0. Otomatik modun karartmayı da
tetiklemesi gerekmez. GEREKLİLİK 2'deki bu notu burada da tekrarla.

### R8 ve Glance — sessiz kırılma riski

Glance ve `RemoteViews` **reflection** kullanır. R8 yanlış kural yazılırsa widget
**derlenir ama çalışmaz** (sessiz hata, logcat'te `ClassNotFoundException`).
`proguard-rules.pro`'de şunlar **şart**:

```
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keepclassmembers class * extends android.widget.RemoteViews { <init>(...); }
```

Release derlemesini kurduktan sonra widget'ı **gerçek cihazda** test et. Debug
build'de çalışıp release'de kırılması en olası regresyondur.

### Build ölçüleri

- Release: `isMinifyEnabled = true`, `isShrinkResources = true`, R8 full mode.
- `Application` sınıfı **yok** (DataStore lazy açılsın), DI yok, ilk kare öncesi
  disk I/O yok.
- `androidx.profileinstaller` ekle. Baseline profile'ı bu boyutta elle yazma —
  kazanım cold start'ta birkaç yüz ms, APK'ya ~30 KB. Düşük öncelik.
- APK hedefi: **R8 release < 3.5 MB.**

### Ölçüm protokolü — "optimize" iddiası doğrulanabilir olmalı

> **Bu bölüm senin işin değil — kullanıcının.** Bulutta `adb` yok, cihaz yok.
> Aşağıdaki komutları **README'ye yaz**, sonra kullanıcıdan çıktıları iste ve
> commit et. Sayı uydurma. Ölçülmemiş bir hedef "geçti" sayılmaz; belgede
> "ölçülmedi" olarak kalır ve iş bitmiş sayılmaz.

İddialar ölçülmeli. Bu testi iki cihazda da yap ve sonuçları README'ye yaz.

**Hazırlık:**
```
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats --enable full-wake-history
```

**Koşu (her cihazda ayrı):**
1. Telefonu **ekran kapalı** durumda bırakılacak şekilde hazırla.
2. Uygulamayı aç → Başlat → uygulamayı kapat (swipe) → **ekranı kapat**.
3. **60 dakika** bekle. Telefonu prize takma.
4. `adb shell dumpsys batterystats com.oguzh.kronometre > bs.txt`
5. Kontroller:
```
adb shell dumpsys cpuinfo | findstr /i kronometre     # ~0% olmalı
adb shell dumpsys meminfo com.oguzh.kronometre        # PSS
findstr /i "wakeup" bs.txt                            # 0 olmalı
findstr /i "wakelock" bs.txt                          # 0 olmalı
```
6. `batterystats.txt` → **Battery Historian** ile görselleştir
   (`github.com/google/battery-historian` → `python historian.py bs.txt bs.html`).
   Wakelock bölümü **boş** olmalı.
7. **İkinci koşu — ekran açık.** 10 dakika boyunca uygulamada kal, 10 saniyelik
   karartmayı bekle, sonra 8 dakika boyunca ölç. Ekran paneli bu arada 10. dakikadan
   itibaren kapalı olacak. Sonucu ayrıca yaz.
8. **Üçüncü koşu — karartma kapalı** (geçici olarak sabiti devre dışı bırak).
   10 dakika boyunca ekran açık, ölç. Bu, karartmanın kazandırdığı sayıyı verir ve
   **kullanıcıya göstereceğin en anlamlı rakamdır.** Üç sayıyı yan yana yaz.

**Baz karşılaştırma:** Aynı cihazda uygulama **force-stop** edilmiş halde 60 dakika
ölç. `Screen off discharge` değerleri (mAh/saat) birbirine **ayırt edilemez** olmalı.
Ayrılıyorsa bir şey yanlış.

### Sayısal hedefler (kabul kriteri)

Mod, `PowerManager.isInteractive`'dan otomatik türetilir (GEREKLİLİK 2). İki mod için:

| Metrik | Ekran açık → saniye | Ekran kapalı → dakika |
|---|---|---|
| CPU — çalışırken | **< %0.5** | **< %0.5** |
| Kullanıcı alanı wake lock | **0** | **0** |
| Uygulama kaynaklı wakeup / saat | **0** | **~60** |
| `notify()` / saat | **~1** | **~60** |
| Widget `updateAll()` / saat | **< 5** | **< 5** |
| Ekran kapalı 1 saat pil düşümü | **< %1** | **< %1** |

Ortak hedefler:

| Metrik | Hedef |
|---|---|
| PSS — çalışırken | **< 60 MB** |
| APK (R8 release) | **< 3.5 MB** |
| Cold start | **< 300 ms** |
| Karartma süresi | **10 sn**, sabit, ayar yok |

### Zamanlayıcı (geri sayım) eklenirse

Şimdi kapsam dışı — ama ekleme kararını verirsen **yeni bir mekanizma kurma.**
Aynı FGS'i ve aynı `Chronometer` altyapısını kullan:

- Bildirimde `setChronometerCountDown(true)` + `setWhen(bitişZamanı)`. Geri sayım
  aynı sıfır-maliyetli mekanizmayla çalışır, **sıfır ek kod, sıfır ek izin**.
- Süre dolunca: `setChronometerCountDown(false)`, bildirimi güncelle, kullanıcı
  dokunana kadar bekle (veya `setTimeoutAfter` ile otomatik kaldır).
- Aynı widget, aynı repo, aynı servis. `StopwatchState`'e bir `mode`
  (`STOPWATCH` / `COUNTDOWN`) alanı eklemek yeterli.

**AlarmManager'a gitme.** Gereksiz `SCHEDULE_EXACT_ALARM` izni (Android 13+'da
Play'de kısıtlı), gereksiz `BroadcastReceiver`, gereksiz bir hata yüzeyi. FGS zaten
çalışıyor. Yalnızca **saatlerce süren** ve bildirimde tutulmak istenmeyen bir
zamanlayıcı varsa AlarmManager düşün — o durum bu uygulamanın kapsamı dışında.

---

## GEREKLİLİK 5b — Ek yapılandırma

- **APK boyutu:** R8 sonrası ~3–4 MB hedefle. Compose ~2 MB, Glance ~0.5 MB.
  Gereksiz dependency (Hilt, Room, Navigation, coil) **ekleme**.
- **Cold start:** tek Activity, tek ekran, `Application` sınıfı **yok** (DataStore lazy
  açılsın), DI yok, ilk kare öncesi hiçbir disk I/O yok. İlk ekran açılışı <300 ms.
- **Uygulama arka planda:** sadece FGS süreci ayakta. Activity yok edilince `isRunning`
  hâlâ doğru.
- **Saf Kotlin.** Native kütüphane, `extractNativeLibs` ayarı gerekmiyor.

---

## GEREKLİLİK 6 — Pil optimizasyonu / OEM yardım ekranı (TÜM MARKALAR için ZORUNLU)

### Problem nedir

FGS'in öldürülmesi **Xiaomi'e özgü bir sorun değil.** Neredeyse her üretici
(One UI, EMUI, ColorOS, OxygenOS, Funtouch/OriginOS, ZenUI, Transsion) kendi
agresif görev katilini koyar ve çoğu FGS durumunu görmezden gelir. Stock Android
(Pixel, Motorola, Nothing, Nokia) genelde sorun yaşamaz.

Sonuç: **Xiaomi'e özel kod yazma, markaya göre yapılandırılan bir tablo yaz.**
Aksi halde S22'de, Oppo'da, Huawei'de aynı hata tekrarlanır.

Ayrıca: uygulama bunu kullanıcıdan tek başına çözemez. Yapabileceğin şey
(1) durumu tespit etmek, (2) kullanıcıya markaya özel doğru ekranı göstermek.

### Tespit — AOSP yolu, markadan bağımsız

`ui/oem/BatteryOptimizationHelper.kt`:

- **Ana durum sinyali (markadan bağımsız, her cihazda çalışır):**
  `PowerManager.isIgnoringBatteryOptimizations(pkg)`
  Bu AOSP API'si (23+) tüm üreticilerde vardır. Doğru/yanlış tek kaynak budur.
- **Marka tespiti** — üreticiye göre tablo, `data/OemProfile.kt`:

| Marka | Tespit | Yönlendirme |
|---|---|---|
| Xiaomi / Redmi / POCO | `MANUFACTURER` in (xiaomi, redmi, poco) **veya** sistem özelliği `ro.miui.ui.version.name` dolu | Otomatik başlatma yönetimi |
| Huawei / Honor | `MANUFACTURER` in (huawei, honor) **veya** özellik `ro.build.version.emui` / `ro.build.hw_emui_api_level` dolu | Başlatma yöneticisi |
| Oppo / Realme / OnePlus | `MANUFACTURER` in (oppo, realme, oneplus) **veya** özellik `ro.build.version.opporom` / `ro.build.version.oplusrom` dolu | "Arka plan etkinliğine izin ver" + otomatik başlatma |
| Vivo / iQOO | `MANUFACTURER` in (vivo, iqoo) **veya** özellik `ro.vivo.os.version` dolu | Arka plan yönetimi beyaz listesi |
| Samsung | `MANUFACTURER` == "samsung" | "Kısıtlama yok" + "Hiç uykuya dalmayan uygulamalar" |
| Asus | `MANUFACTURER` == "asus" | Otomatik başlatma |
| Transsion (Infinix/Tecno/itel) | `MANUFACTURER` in (transsion, infinix, tecno, itel) | Otomatik başlatma |
| Diğer / stock | hiçbiri eşleşmedi | Sadece AOSP pil optimizasyonu ekranı |

- Sistem özelliği okuma: `SystemProperties` reflection hack'i **kullanma** —
  `java.lang.Runtime.exec("getprop")` çağrısı gerekmez. `Build.MANUFACTURER` +
  `Build.BRAND` + `Build.DISPLAY` + `Build.FINGERPRINT` üzerinden `contains` eşleştirmesi
  yeterli ve her API seviyesinde çalışır. (Örnek: fingerprint'de `"miui"`/`"hyperos"`,
  display'da `"EMUI"`/`"MagicOS"`/`"ColorOS"`/`"Funtouch"`/`"OxygenOS"`.)
- **Eşleşme `contains` ile yapılsın, `equals` ile değil.** Üretici adları
  varyant gösteriyor ("Xiaomi", "Redmi", "POCO"), sürümden sürüme değişiyor.

### Otomatik başlatma deep link'leri

Kullanıcıyı doğrudan ilgili ekrana at. **Liste (en az iki deneme + fallback):**

```
Xiaomi:      com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity
Huawei:      com.huawei.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity
Oppo/Realme: com.coloros.safecenter/com.coloros.safecenter.permission.startup.StartupAppListActivity
Vivo:        com.vivo.permissionmanager/.activity.BgStartUpManagerActivity
OnePlus:     com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity
Asus:        com.asus.mobilemanager/autostart.AutoStartActivity
Transsion:   com.cyin.himgr/.AutobootManageActivity
```

**Kritik: her deep link'i `try { startActivity(intent) } catch (e: ActivityNotFoundException)
{ fallback }` ile sarmala.** Bu activity'ler her ROM sürümünde var değil ve
sık sık taşınıyor. Fallback sırası:
1. `try` — markanın otomatik başlatma ekranı
2. `try` — `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` URI
3. `try` — `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`
4. Hiçbiri açılamıyorsa → ekranda **manuel talimat metni** göster, buton yerine
   sadece metin. (Bazı Çin ROM'larında ayarlar activity'si bile engelli.)

### Ekran davranışı

`SettingsActivity` / "Kronometre arka planda çalışsın" ekranı:

- **Engelleyici değil.** Kullanıcı istediği an kapatabilir, geri dönebilir.
  Uygulamayı kilitlemek kötü UX ve Play'da de reddedilir.
- Her adımda "Bu adımı yaptım" tick işareti + o adımın **gerçek** öneminde
  marka metni (bkz. aşağıdaki tablo).
- Her adımın yanında `context.startActivity` ile ilgili ekrana giden buton.
- `onResume`'da `isIgnoringBatteryOptimizations` tekrar okunur; kullanıcı dışarıdan
  ayarladıysa ekran otomatik kapanır.
- **Ne zaman göster:**
  - İlk açılışta: marka agresif listedeyse **ve** pil muafiyeti yoksa → bir kez
    göster (DataStore'da "gösterildi" bayrağı).
  - Sonrasında: her açılışta sessizce kontrol et. Hâlâ muafiyet yoksa ve kullanıcı
    3+ kez açmışsa, ana ekranda **kapatılabilir** bir uyarı şeridi göster.
  - Muafiyet verildiyse → **bir daha hiç gösterme.**
- **Stock Android'de (Pixel/Motorola/Nothing) gereksiz uyarı gösterme.** Sadece
  "pil optimizasyonu muafiyeti" satırını nötr bir bilgi olarak göster ve geç.
  Her cihazda uyarı göstermek, gerçek sorunu olan kullanıcının ekranını
  gürültüyle doldurur ve ciddiyeti düşürür.

### Markaya göre talimat metinleri

Her marka için kullanıcıya **ne yapacağını** söyle. Genel metin işe yaramaz,
çünkü menü adları markadan markaya değişir:

- **Xiaomi/HyperOS:** Güvenlik → Otomatik başlatma → Kronometre → **Aç**
  · Ayarlar → Uygulamalar → Kronometre → Pil → **Kısıtlama yok**
  · Son uygulamalar'da kartı **kilitle**
- **Samsung One UI:** Pil → Arka plan kullanımı → **Hiç kısıtlama**
  · Pil → Arka plan kullanımı → **Hiç uykuya dalmayan uygulamalar** → Kronometre ekle
  · Bildirimler → **"Uyarı bildirimlerine izin ver"** aç (sistem gizleyebiliyor)
- **Huawei/EMUI:** Pil → Uygulama başlatma → **Otomatik yönetimi kapat**, üçünü de
  elle aç (Otomatik başlatma / İkincil başlatma / Üçüncül başlatma)
- **Oppo/Realme/OnePlus:** Pil → **Arka plan etkinliğine izin ver** → "Kronometre:
  Arka planda çalıştırmaya izin ver" → **Otomatik başlatma** → aç
  · Telefon Yöneticisi → Beyaz liste → Kronometre'yi ekle
- **Vivo:** Pil → Arka plan yönetimi → **Yüksek arka plan tüketimi** → Beyaz listeye al
  · Otomatik başlatma → aç
- **Asus:** Otomatik başlatma → **Kronometre'ye izin ver**
- **Transsion:** Otomatik yönetim → uygulama listesinden **Kronometre'yi** seç, üç
  izni de aç
- **Stock:** Ayarlar → Uygulamalar → Kronometre → **Pil → Kısıtlama yok**

### İzinler ve politika notları

- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  (`package:` URI). Bu AOSP'un desteklediği **resmi** muafiyet yoludur ve
  `isIgnoringBatteryOptimizations` ile doğrulanabilir tek yoldur.
- `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (liste ekranı) **son çare** —
  kullanıcı kendisi bulmak zorunda kalır, dönüşüm oranı düşüktür.
- Google Play, bu izni "uygulamanın çekirdek işlevi bozuluyorsa" ile sınırlar.
  Kronometre için bu savunma **geçerli**: ekran kapalıyken sayma işlevi FGS olmadan
  durur. (Kişisel dağıtım olduğu için pratikte sorun değil, ama README'de gerekçeyi yaz.)
- `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` doğrudan dialog açtığı için
  kullanıcıyı **önce** kendi uygulama ayarları ekranına götür, sonra oradan dialoga.
  Aksi halde uygulama Play politikasına takılır.

---

## GEREKLİLİK 7 — UI

- Tek ekran, Material 3, `MaterialTheme` + dinamik renk (31+).
- Ortada büyük `HH:MM:SS` (ve `SS.ms` — ondalık gösterimi istersen, bkz. GEREKLİLİK 1).
- Altında durum: `Çalışıyor` / `Duraklatıldı` / `Sıfırlandı`.
- Butonlar: **Başlat/Devam** (geniş, birincil) + **Tur** + **Sıfırla** (ikincil).
  `Sıfırla` yalnızca süre > 0 iken aktif.
- Tur listesi: en yenisi üstte, her satırda `Tur N` + birikmiş süre + o turun
  kendi süresi (`tur - önceki tur`).
- Ekran **kapanmaz** (`FLAG_KEEP_SCREEN_ON`) — ama **sadece çalışırken**. Aksi halde
  pil israfı. `DisposableEffect`/`LaunchedEffect` ile `isRunning` değişiminde
  `window.addFlags/clearFlags`.
- `ViewModel` + `StateFlow` ile state topla; Activity'de doğrudan repo okuma.
- `StopwatchViewModel`, servisi başlatır/yakalar, repo state'ini `collect` eder.
- Preview'lar ekle (`@Preview`) — Compose'da beklenen.
- Tasarım sistemi: tek renk vurgusu (yeşil = başlat, kırmızı = duraklat), ikonlar
  Material Symbols'ın path'lerini kullan (dependency ekleme, `ImageVector` olarak elle
  yaz veya vektör drawable üret).
- **Ayar ekranı yok.** `MoreVert`, bottom sheet, hiçbir ayar menüsü, `SetupScreen`
  dosyası silinir. Bildirim hassasiyeti otomatiktir, karartma sabit 10 saniyedir —
  ayarlanacak bir şey kalmadı. Tek ekran, sadece kronometre.

---

## GEREKLİLİK 8 — Doğrulama

Build edip **ikisinde de** elle test et. Test listesini README'ye de yaz.

### Otomatik
- `./gradlew assembleDebug` ve `./gradlew assembleRelease` hatasız
- `./gradlew lint` — **fatal/severe hata yok** (özellikle `ForegroundServiceType`,
  `BatteryLife`, `NotificationPermission` kontrolleri)
- `StopwatchViewModel` için basit unit test: Start/Pause/Lap/Reset geçişleri ve
  `currentElapsedMs` matematiği. `kotlinx-coroutines-test` + JUnit4.
- Repo/DataStore için basit test (geçici dosya dizini).

### Manuel — S22 (API 34+ yolu)
1. Uygulamayı aç, izin iste, **Başlat**.
2. Bildirim çubuğunu aç → kronometre görünüyor, saniye saniye ilerliyor.
3. **Kilit ekranına git** → bildirim görünüyor ve gerçek süreyi gösteriyor
   (redaction yok). Aksiyonlar kilit ekranında da çalışıyor.
4. Bildirimden **Duraklat** → bildirim tamamen kayboluyor.
5. Bildirimden **Devam Et** → bildirim geri geliyor, kaldığı yerden devam ediyor.
6. **Uygulamayı tamamen kapat** (swipe) → bildirim ve sayaç çalışmaya devam ediyor.
7. **Bildirimden kapatmayı dene** (kilit ekranında çek) → silinmiyor (`ongoing`).
8. Widget'ı ana ekrana ekle → çalışırken doğru süreyi gösteriyor, widget'tan
   Duraklat/İleri çalışıyor, widget'a dokununca uygulama açılıyor.
9. Ekran kapalı 2 dk bekle → yeniden aç, süre doğru.

### Manuel — Note 12 Pro 4G (HyperOS 1, eski API + agresif ROM)
1. GEREKLİLİK 6'daki ayar ekranını tamamla (Automatik başlatma + Pil: Kısıtlama yok
   + Recents kilidi).
2. Ayarları yapmadan önce **kontrol**: Başlat → uygulamayı kapat → ekranı kapat → 2 dk bekle →
   bildirim hâlâ var mı? (Yoksa ayar ekranının işe yaradığını kanıtlar)
3. Ayar ekranı tamamlandıktan sonra aynı test **geçmeli**.
4. `adb shell dumpsys deviceidle whitelist | grep kronometre` → beyaz listede görünmeli.
5. Bildirim kilit ekranında görünüyor ve aksiyonlar çalışıyor.
6. Widget doğru süreyi gösteriyor.

### Manuel — S22 (One UI) — OEM akışının kendisi
1. Uygulamayı kur, aç. `isIgnoringBatteryOptimizations` kontrolü çalışıyor mu?
   `adb shell dumpsys deviceidle whitelist | grep kronometre`.
2. Samsung'a özel "Hiç uykuya dalmayan uygulamalar" adımı doğru metni ve
   yönlendirmeyi gösteriyor mu?
3. **Derin linkler** açılıyor mu, yoksa fallback metnine düşüyor mu? (Biri bile
   `ActivityNotFoundException` fırlatmamalı — logcat'e bak.)
4. Muafiyet verildikten sonra ekran kendini kapatıyor mu?

### Otomatik — marka tablosu için
- `OemProfile` tespitini birim test et: bilinen `MANUFACTURER`/`BRAND`/`DISPLAY`/
  `FINGERPRINT` kombinasyonları doğru markaya düşmeli. En az: Xiaomi, Redmi, POCO,
  samsung, huawei, honor, oppo, realme, oneplus, vivo, iqoo, asus, transsion, Google.
- **Kritik:** eşleşme `equals` ile değil `contains`/case-insensitive yapılıyor testi.
  Üretici adları varyant.
- Bilinmeyen marka → `OemProfile.Unknown` düşmeli ve uygulama çökmemeli.

### Manuel — her iki cihaz
- `adb shell dumpsys notification --noredact | grep -A5 com.oguzh.kronometre` →
  `visibility=0` (PUBLIC) ve `ongoing=1` olduğunu doğrula
- Uygulama arka pladayken **1 saat** çalıştır, iki cihazda da pil farkını ölç
- Ekran kapalıyken bildirim **kilit ekranında** ve panel çekilince de görünüyor mu?

### Kapsam dışı (yapma)
Geri sayım, alarm, seans geçmişi, tema seçici, Material You dışı tema, tur dışa aktarma,
nasıl kullanılır ekranı, çoklu kronometre. Tek ekran, tek iş. Bunlar istenirse
sonradan eklenecek — şimdi ekleme.

---

## BÖLÜM 1 — Kabul kriteri özeti (bitince hepsi doğru olmalı)

1. Kronometre **çalışırken** bildirim var, **duraklatılınca yok**.
2. Bildirim kilit ekranında **gerçek süreyi** gösteriyor ve aksiyonları kilit ekranında
   çalışıyor.
3. Bildirim kapatılamıyor (`ongoing`).
4. Süre bildirimde ve widget'ta **kendi kendi** ilerliyor — uygulamada saniyelik
   döngü yok. Çalışırken CPU ~%0.
5. Bildirimden Başlat/Duraklat/İleri/Sıfırla **ve** widget'tan aynı kontroller çalışıyor.
6. Uygulama Activity'si yok edilse bile kronometre ve bildirim devam ediyor.
7. `minSdk 23`; API 23–36 arasında çalışıyor; 34+ FGS tipi hatası yok.
8. Pil optimizasyonu yardımı **marka tablosuyla** çalışıyor (Xiaomi, Samsung, Huawei,
   Oppo/Realme/OnePlus, Vivo, Asus, Transsion, stock) — tek markaya özel kod yok.
   Derin linkler çökmüyor, hepsi fallback'e düşüyor.
9. R8 release derlemesi geçiyor, `lint` temiz, unit testler yeşil.
10. **Ölçülmüş optimizasyon:** 60 dk ekran kapalı koşusunda uygulamanın wake lock'u
    **0**, CPU **< %0.5**, ekran kapalı pil düşümü boşta kullanımdan ayırt edilemez.
    APK (R8) **< 3.5 MB**, cold start **< 300 ms**. Ölçüm komutları, ham çıktı ve
    **üç koşunun (ekran kapalı / karartmalı / karartmasız) yan yana karşılaştırması**
    README'de.
11. Release build'de widget **gerçek cihazda** çalışıyor (R8 + Glance reflection
    kırılması kontrolü).
12. **Otomatik hassasiyet çalışıyor:** ekran açıkken bildirim **saniye** gösteriyor
    (sistem kronometresi, `notify()` yok); ekran kapalıyken **dakika** gösteriyor
    (statik metin, dakikada bir `notify()`). Ekranı aydınlatınca bildirim **kendiliğinden**
    saniyeye dönüyor **ve doğru süreden** başlıyor (eski değerden başlamıyor). Kilit
    ekranı her iki modda da doğru ve taze. Duraklatınca dakikalık zamanlayıcı iptal
    oluyor, sızma yok. Servis ekran kapalıyken widget'tan başlatılırsa doğru modda
    başlıyor. Servis `isInteractive` **sorgulanmadan** (poll) çalışıyor.
13. **Ekran karartma çalışıyor:** çalışırken 10 saniye hareketsizlikte ekran yumuşakça
    kararıyor, her dokunuşta geri geliyor. **Parlaklık sistem değerine (`-1f`) geri
    yükleniyor** — cihaz parlaklığı bozulmuyor. Activity `onStop`'ta parlaklık geri
    yükleniyor: uygulamadan çıkınca ekran siyah kalmıyor. Kronometre durunca
    karartma geri alınıyor.
14. **Ayar ekranı yok:** hiçbir menü, `MoreVert`, bottom sheet veya ayar sayfası
    bulunmuyor. Tek ekran, sadece kronometre. DataStore'da yalnızca 3 anahtar var.
15. README: kurulum, build, iki cihazdaki manuel test listesi, **marka bazında pil
    optimizasyonu adımları**, **ölçüm sonuçları (üç koşu yan yana)**, bilinen
    sınırlamalar (süreç yeniden başlatılınca duraklatılmış devam eder; ekran kapalı
    modda saatte ~60 uygulama uyanması ve dakika hassasiyeti).

---

## BÖLÜM 2 — Bilinçli kararlar (uygulama bunları "yanlış" sayabilir, sözleşme)

| Karar | Neden |
|---|---|
| `specialUse` FGS tipi | Kronometreye özel tip yok; `dataSync` Play politikası ihlali + 6 saat limiti; `shortService` 3 dk limiti |
| `setUsesChronometer(true)` | Sistem çizdiği için saniyelik güncelleme gerekmez → pil ve doğruluk kazanılır |
| Duraklatınca bildirim kaldırılıyor | Kullanıcı şartı: "sadece başlatıldığında görünsün" |
| Sıfırlama sırasında bildirim yok | Sıfırla ancak duraklatılmışken aktif; zaten bildirim yok |
| HyperOS ayar ekranı | ROM tasarımı; uygulama tek başına çözemez, yönlendirebilir |
| DI kütüphanesi yok | Bu boyutta proje için build süresi + cold start maliyeti haklı değil |
| Kalıcı durum yok | Süreç ölürken çalışan bir kronometre kurtarılamaz; FGS bunu zaten engeller |
| AlarmManager yok | Zamanlayıcı da aynı FGS + `setChronometerCountDown` ile çözülür. AlarmManager ek izin (`SCHEDULE_EXACT_ALARM`, Play'de kısıtlı) ve hata yüzeyi getirir, kazandırdığı bir şey yok |
| Hassasiyet otomatik, ayar değil | Maliyeti SystemUI öder, uygulama değil. Ekran açıkken saniye bedava (ekran zaten yenileniyor), ekran kapalıyken dakika göstererek saatte 3600 redraw kazanılıyor. Ekran durumu bu bilgiyi bedava verdiği için ayar gereksiz — kullanıcıya karar yüklemiyoruz |
| `setUsesChronometer(false)` + dakikalık `Handler` | `setUsesChronometer` saniyeyi gizleyemez (format parametresi yok). Bu, "sıfır döngü" ilkesinin tek istisnası ve **dakikalıktır**: saatte ~60 uyanma karşılığında 3600 SystemUI redraw'ı kazanılır. Ölçülmüş bedel, README'de |
| Ekran 10 sn sonra kararıyor | Ekran paneli tek başına uygulamanın en büyük pil kalemi. Gerçek kronometre yüzü gibi davranmak hem doğru hem kullanıcıya memnuniyet veriyor. `goToSleep` imza izni istediği için parlaklık düşürme kullanılıyor |
| `Application` sınıfı yok | DataStore lazy açılsın; her sınıf cold start'a maliyet ekler |

---

## BÖLÜM 3 — Bilinen sınırlamalar (README'ye yaz)

- Uygulama **kronometre süresince** çalışan bir FGS'dir. Android 15+ ve özellikle
  MIUI/HyperOS kullanıcıyı zorlayabilir. Bu bir uygulama hatası değildir.
- Cihaz yeniden başlatıldığında kronometre duraklatılmış olarak geri gelir
  (boot'ta otomatik devam etme **yok** — `BOOT_COMPLETED` + FGS kısıtları).
- Widget'ta API 23'te canlı sayaç yok (statik metin); 24+ `Chronometer` ile canlı.
- `ACTION_BATTERY_OPTIMIZATION_SETTINGS` değişiklikleri kullanıcıya bağlı; uygulama
  bunu zorlayamaz.
- **Ekran kapalıyken bildirim dakika hassasiyetindedir, saniye değil.** Bilinçli
  takas: `setUsesChronometer` saniyeyi gizleyemediği için bu modda saatte ~60 uyanma
  ve ~60 `notify()` vardır. Kullanıcı ekrana baktığı anda saniye kendiliğinden geri
  gelir. Beklenen pil kazancı büyüktür ama **ölçülmüş olmalıdır** — README'de üç
  koşunun karşılaştırması yer alır.
- **Karartma 10 saniyedir, ayarlanamaz.** Kullanıcı uzun süre ekranı açık isterse
  dokunarak uyanık tutabilir. Ayar ekranı bilinçli olarak yok.
- Karartma `isInteractive`'ı **değiştirmez** (ekran fiziksel olarak açıktır), bu
  yüzden uygulama içi ekran kararındayken bildirim saniye modunda kalır. Doğru
  davranış: o anda bildirim görünmüyor, dolayısıyla render maliyeti zaten yok.
