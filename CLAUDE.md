# Kronometre — Hedef Metni (CLAUDE.md olarak kullan)

> Bu dosyayı proje kökünde `CLAUDE.md` adıyla kaydet. Claude Code her oturumda
> otomatik okur. Ayrıntılı sözleşme `PROMPT.md`'de — buradaki amaç **niyet**,
> orası **teknik detay**.

---

## Amaç

Bir Android kronometre uygulaması yap. Kronometre çalışırken ekran kapalı olsa bile
süreyi doğru saymaya devam etsin, kullanıcı bunu **bildirimden** ve **ana ekran
widget'ından** kontrol edebilsin, ve bu iki yüzey de pil yiyerek çalışmasın.

Kısa cümleyle: **doğru, öngörülebilir, sıfır pil maliyetiyle süren bir sayaç.**

---

## Kim için

Kullanıcının elinde iki cihaz var ve ikisi de gerçek test cihazı:

- **Redmi Note 12 Pro 4G — HyperOS 1** (eski Android, saldırgan ROM)
- **Samsung Galaxy S22** (güncel Android, katı FGS/bildirim kuralları)

Bu ikisi bilinçli olarak zıt iki uç. Birinde çalışıyorsa diğerinde de çalışıyor
sayılma. Kullanıcı bu iki cihazda elle test edecek — "bende çalışmıyor" derse
kodda değil, hedefte hata vardır.

---

## Bitti sayılması için

Uygulama ancak **hepsi** doğruysa bittidir:

1. Kronometre **çalışırken** bildirim var, **duraklatılınca tamamen gidiyor**.
2. Bildirim **kilit ekranında gerçek süreyi** gösteriyor (redaction yok) ve
   aksiyonları kilit ekranından da çalışıyor.
3. Bildirim **kapatılamıyor**.
4. Süre **bildirimde ve widget'ta kendi kendi** ilerliyor — uygulamada saniyelik
   döngü yok. Kronometre çalışırken uygulamanın CPU kullanımı ~%0.
5. Bildirimden **ve** widget'tan Başlat / Duraklat / Tur / Sıfırla çalışıyor.
6. Uygulama kapatılsa (swipe) bile kronometre ve bildirim devam ediyor.
7. `minSdk 23` — API 23'ten 36'ya kadar çalışıyor, API 34+ FGS tipi hatası yok.
8. Pil optimizasyonu yardımı **tek markaya özel değil**, marka tablosuyla çalışıyor
   (Xiaomi, Samsung, Huawei, Oppo/Realme/OnePlus, Vivo, Asus, Transsion, stock) ve
   hiçbir derin link çökmüyor.
9. Release (R8) derlemesi geçiyor, lint temiz, unit testler yeşil.
10. **Optimizasyon ölçülmüş:** 60 dk ekran kapalı koşusunda wake lock **0**,
    CPU **< %0.5**, ekran kapalı pil düşümü boşta kullanımdan ayırt edilemez.
    APK (R8) **< 3.5 MB**, cold start **< 300 ms**. Ölçüm komutları, ham çıktı ve
    **üç koşunun yan yana karşılaştırması** (ekran kapalı / karartmalı / karartmasız)
    README'de.
11. README'de iki cihazdaki manuel test listesi, marka bazında pil optimizasyonu
    adımları ve ölçüm sonuçları yazılı.

`PROMPT.md`'deki "BÖLÜM 1 — Kabul kriteri" ile aynı şey. Biri tutmuyorsa iş bitmedi.

---

## Ödünleşmeler (teknik detay sessizse)

Öncelik sırası **değişmez**: doğruluk → güvenilirlik → pil → güzellik → kapsam.

- **Doğruluk her şeyi yener.** `System.currentTimeMillis()` yerine
  `SystemClock.elapsedRealtime()`. Saat değişimi kronometreyi bozmasın.
- **Pil, özellik gereği.** Bir özelliği eklemek 1 saniyelik döngü gerektiriyorsa
  özellik değil, çözüm değiştir. Bildirimde ve widget'ta `Chronometer` mekanizmasını
  kullan — bu bir tercih değil, gereklilik.
- **Hedef sıfır değil, ölçülebilir taban çizgisi.** Kullanıcı kararı: sıfır olması
  gerekmiyor; olabildiğince optimize, düşük tüketim. Bu "kabul edilebilir" demek
  değil, "ölçülmeyen hiçbir şey kabul edilmez" demektir. Hedef, boşta kullanımdan
  **ayırt edilemeyen** bir tabandır.
- **Her trade-off'u açıkça belgele.** Dakika modunda saatte ~60 uyanma var; bu hata
  değil, ölçülmüş bir bedel. Belgelenmemiş trade-off, sonradan "optimize etmedin"
  diye cezalandırılacak gizli maliyettir.
- **Hafiflik ölçülür, iddia edilmez.** "Hafif" geçer, "60 dakika ekran kapalı koşusunda
  wake lock 0, CPU %0.5" geçmez. Optimizasyon iddiası ölçümle desteklenmiyorsa
  yok sayılmalı ve kod değiştirilmeli.
- **Ekran paneli uygulamanın en büyük pil kalemi.** Bildirim, widget ve servisin
  maliyeti ekran açıkken önemsiz; ekranı açık tutmak ise telefonu ısıtan şey. En büyük
  kazanç buradan gelir: 10 saniyede bir karart, saniyeyi bedava göstermek.
- **Maliyeti doğru yere koy.** Isıyı üreten şey sayma değil, **yeniden yayınlama**.
  Saniyede bir `notify()` çağrısı, saniyede bir `delay()` döngüsünden daha pahalıdır
  ve daha az bariz görünür. Bildirimi saniyede bir yeniden gönderme.
- **Basitlik kazanır.** Bir şeyi 10 satırda çözebiliyorsan 10 satırda çöz.
  DI kütüphanesi, abstraction katmanı, generic repository, multi-modül yapı:
  bu projede gerekçeleri yok. `minSdk 23`'te hedef küçük ve okunabilir kod.
- **Emin değilsen tahmin etme.** Derlemesini bilmediğin bir API'yi kullanma, tahmini
  sürüm numarası yazma. `TODO` bırakıp sor.

---

## Sınırlar — bunları yapma

- Geri sayım, alarm, seans geçmişi, tur dışa aktarma, tema seçici, çoklu kronometre,
  nasıl kullanılır ekranı **yok**. Tek ekran, tek iş.
- Saniyelik `Handler`/`delay` döngüsü, `while(true)`, `Job` ile periyodik sayaç **yok**.
- **Saniyede bir `NotificationManagerCompat.notify()` yok.** `setUsesChronometer`
  sayesinde bildirim seans başına **bir kez** gönderilir; saniye işleyişini sistem çizer.
  Tek istisna: **ekran kapalıyken** dakika hassasiyetine düşülür ve dakikada **bir**
  tazeleme olur. Dakikada bir olabilir, saniyede bir olmayabilir.
- **`isInteractive` sorgusunu POLL YAPMA.** `ACTION_SCREEN_ON` / `SCREEN_OFF`
  broadcast'lerini servis içinde `registerReceiver` ile dinle. Sorgulamak, kaçınmaya
  çalıştığın şeyin ta kendisi. Manifest'te bildirilen receiver'a güvenme (API 26+ ulaştırmaz).
- **Saniyede bir `StopwatchWidget.updateAll()` yok.** Bu launcher sürecinde çalışır —
  pil sorunu senin uygulaman değil, launcher'ın pil sorunu olur.
- **Saniyede bir DataStore yazması yok.** Yalnızca durum değişiminde.
- `WakeLock` **alma**. Gerekmiyor; ekran kapalıyken çalışma FGS ile sağlanıyor.
  Google Play Android vitals kuralı: 24 saatte 2 saati aşan partial wake lock
  "aşırı pil tüketimi" uyarısı üretir.
- Hilt/Koin/Room/Navigation/Coil **ekleme**. Build süresi, APK boyutu, cold start.
- **Ayar ekranı yok.** `MoreVert`, bottom sheet, ayar sayfası, `SetupScreen` — hepsi yok.
  DataStore'da yalnızca 3 anahtar: `elapsed_ms`, `running_since`, `laps`.
- **Bildirim hassasiyeti ayar değil, `PowerManager.isInteractive`'dan türetilir.**
  Ekran açık → saniye (bedava, ekran zaten yenileniyor), ekran kapalı → dakika.
  Kullanıcı ayarı, ayar UI'ı, DataStore anahtarı yok. Ekran aydınlanınca saniye
  **kendiliğinden ve doğru süreden** geri gelir.
- **Ekran 10 saniye hareketsizlikte kararıyor** — `window.attributes.screenBrightness`
  ile (`PowerManager.goToSleep` imza izni istiyor, o yol kapalı). Dokunma geri
  getiriyor. Süre sabit, ayar yok. Parlaklığı **`-1f` ile geri yükle**, `0f` ile
  değil: `0f` cihazın parlaklık ayarını bozar ve ekran siyah kalır. `onStop`'ta geri yükle.
- `dataSync` FGS tipini kötüye kullanma. `specialUse` kullan.
- `isMiui()` gibi tek markaya özel kod **yazma.** Agresif görev katili Xiaomi'e özgü
  değil; One UI, EMUI, ColorOS, OxygenOS, Funtouch/OriginOS hepsinde var. Marka
  tablosu kur, tespiti `equals` değil `contains` ile yap. Stock Android'de (Pixel,
  Motorola, Nothing) gereksiz uyarı gösterme — gerçek sorunu olan kullanıcının
  ekranını gürültüyle doldurma.
- OEM derin link'lerini `try/catch` + fallback olmadan çağırma. Bu activity'ler her
  ROM sürümünde yok, sık sık taşınıyor. Çökerse uygulama açılmıyor.
- `SystemProperties` için reflection hack'i veya `getprop` exec **kullanma.**
  `Build.MANUFACTURER`/`BRAND`/`DISPLAY`/`FINGERPRINT` yeterli, her API seviyesinde çalışır.
- Kullanıcıyı ayar ekranına kilitleme. Yardım ekranı engelleyici olmamalı.
- Ham `startForeground(id, notification)` **çağırma**. `ServiceCompat` kullan.
- `setPublicVersion` atlama. Kilit ekranında boş metin çıkar.
- Widget aksiyonu için `actionRunCallback` kullanma. `actionStartService` kullan.
- Widget'ta `clickable { }` kullanma. `clickable(actionStartActivity<...>())` kullan.
- Kod yorumu yazma.

---

## Çalışma disiplini

1. Kod yazmadan önce `PROMPT.md`'yi baştan sona oku.
2. Emin olmadığın yerde sor — tahmin etme, sonra düzeltme turu atma.
3. Bir şeyi "bitti" demeden önce **çalıştır**: `./gradlew assembleDebug`,
   `./gradlew lint`, `./gradlew test`. Derlenmeyen kod bitti sayılmaz.
4. Kullanıcıya "şunu yaptım" derken, gerçekten yaptığından emin ol. Test edemediysen
   "test edilmedi" de, "çalışıyor" deme.
5. Bittiğinde README'yi güncelle: kurulum, build, iki cihazdaki manuel test listesi,
   HyperOS ve diğer OEM ayarları, bilinen sınırlamalar.

---

## Bunları bil — sessizce kırılır

Bunlar tecrübe gerektiren noktalar. "Neden çalışmıyor" diye saatler harcamak yerine
baştan doğru kurgula.

| Konu | Gerçek |
|---|---|
| Glance sürümü | `1.2.0` kullan. `1.3.0-alpha` compileSdk 37 istiyor, derlemeyi kırar. |
| Glance minSdk | `1.2.0` artık **minSdk 23** istiyor. Bu yüzden taban 23. |
| FGS tipi | `specialUse` + `FOREGROUND_SERVICE_SPECIAL_USE` izni +
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property'si. `specialUse`da süre limiti yok. |
| FGS çağrısı | `ServiceCompat.startForeground(...)`. Ham çağrı API 34'te
  `MissingForegroundServiceTypeException` fırlatır. |
| Kilit ekranı | `VISIBILITY_PUBLIC` **+** `setPublicVersion()`. İkincisi olmazsa sistem
  kendi kırmızı redaction'ını üretir. |
| Süre canlılığı | `setUsesChronometer(true)` + `setWhen(...)`. Sistem kendi çizer →
  saniyelik güncelleme gerekmez. Duraklatınca `setWhen(now - elapsed)`. |
| Sıfır döngü ilkesi | Kronometre **saymaz**; `accumulated + (elapsedRealtime() - runningSince)`
  aritmetiği. Doğru yazılmış kronometrenin çalışırken hiçbir döngüsü, wake lock'u
  veya periyodik işi yoktur. Servis boşta bekleyen bir nesnedir. |
| R8 + Glance | Glance ve `RemoteViews` reflection kullanır. R8 kuralı yanlışsa widget
  **derlenir ama çalışmaz** — sessiz hata. Release build'i gerçek cihazda test et. |
| Saniye gizleme tuzağı | `setUsesChronometer` **saniyeyi gizleyemez** — daima `MM:SS`/`H:MM:SS`
  üretir, format parametresi yoktur. Saniyeyi gizlemek mekanizmayı bırakmayı
  gerektirir: `setUsesChronometer(false)` + kendi metnin + dakikada bir tazeleme. |
| Bildirimde tek build fonksiyonu | Her gönderimde `setOngoing` / `setSilent` /
  `setPublicVersion` bayrakları ayrışırsa bildirim kapatılabilir olur ya da kilit
  ekranı eski süreyi gösterir. Tek `build(state, interactive)` fonksiyonu şart. |
| `isInteractive` poll tuzağı | Her saniye `isInteractive` sorgulamak, tam olarak kaçınmaya
  çalıştığın maliyeti üretir. `ACTION_SCREEN_ON`/`SCREEN_OFF` broadcast'i dinle. |
| `SCREEN_OFF` + manifest receiver | API 26+ implicit broadcast kısıtı: manifest'te bildirilen
  receiver'a `SCREEN_OFF` ulaşmaz. Servis içinde `registerReceiver` şart, ve API 34+
  `ContextCompat.RECEIVER_NOT_EXPORTED` bayrağı olmadan `SecurityException` verir. |
| Karartma `isInteractive`'ı değiştirmez | Pencere kararınca ekran fiziksel olarak açıktır, `isInteractive`
  `true` kalır. Bu **yanlış değil**: bildirim o an görünmüyor, render maliyeti zaten 0.
  Karartmayı hassasiyet tetikleyicisi sanma, yoksa gereksiz karmaşıklık olur. |
| `screenBrightness = 0f` | Cihazın parlaklık ayarını bozar, uygulamadan çıkınca ekran
  siyah kalır. Sistem değeri `-1f` ile geri yüklenir. `onStop`'ta geri yüklenmeyi
  unutmak aynı hatadır. |
| Zamanlayıcı eklenirse | `AlarmManager` **kullanma.** Aynı FGS + `setChronometerCountDown(true)`
  yeterli: sıfır ek kod, sıfır ek izin, sıfır ek pil. `SCHEDULE_EXACT_ALARM`
  (Android 13+'da Play'de kısıtlı) gereksiz yük. |
| Widget canlılığı | `AndroidRemoteViews` + klasik `Chronometer`. Glance kompozisyonu
  uzaktaki process'te **bir kez** çalışır, saniyede kendini yenilemez. |
| `updatePeriodMillis` | Sistem minimum 30 dakika zorlar. 1 dakika yapamazsın. |
| PendingIntent | Her birinde `FLAG_IMMUTABLE` şart. Farklı aksiyonlara **farklı
  `requestCode`** — aynı requestCode conflates olur, iki buton birbirinin yerine geçer. |
| Agresif ROM'lar | Ekran kapandıktan ~1-2 dk sonra FGS'i öldürürler. Uygulama hatası
  değil. HyperOS: Otomatik başlatma + "Pil: Kısıtlama yok" + Recents kilidi. One UI:
  "Hiç kısıtlama" + "Hiç uykuya dalmayan uygulamalar". EMUI: "Otomatik yönetimi kapat".
  ColorOS: "Arka plan etkinliği" + otomatik başlatma. Funtouch: beyaz liste. |
| Pil muafiyeti tespiti | `PowerManager.isIgnoringBatteryOptimizations(pkg)` — AOSP API'si,
  her üreticide var, marka tespitine gerek yok. Tek doğru/yanlış kaynak budur. |
| Widget aksiyonu | `actionRunCallback` WorkManager worker'ında çalışır, arka planda
  FGS başlatamaz ve kullanıcı etkileşimi sayılmaz. `actionStartService` kullan. |
| Widget tıklama | `clickable(action)` kullan, `clickable { }` değil — lambda sürümü
  activity başlatmaz. |
| Süreç ölümü | FGS süreci ayakta tutar. Ama süreç yine de ölürse kayıtlı `elapsed`
  geri yüklenir ve **duraklatılmış** devam eder. Bu kasıtlı, README'de belgele. |
| Build ortamı | Depo **claude.ai/code** ile bulutta derlenir. Yerel yol oluşturma, `mkdir`/`cd` çalıştırma — bulutta anlamsız. Cihaz testi, `adb` ölçümü ve OEM ayarları **kullanıcının** işidir; o sayıları **tahmin etme, kullanıcıdan iste.** |
