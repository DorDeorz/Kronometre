package com.dordeorz.kronometre.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OemProfileTest {

    private fun detect(manufacturer: String, brand: String = manufacturer, display: String = "", fingerprint: String = "") =
        OemProfile.detect(manufacturer, brand, display, fingerprint)

    @Test
    fun xiaomiFamily() {
        assertEquals(OemProfile.Xiaomi, detect("Xiaomi", "Redmi"))
        assertEquals(OemProfile.Xiaomi, detect("Xiaomi"))
        assertEquals(OemProfile.Xiaomi, detect("Redmi"))
        assertEquals(OemProfile.Xiaomi, detect("POCO"))
    }

    @Test
    fun otherAggressiveBrands() {
        assertEquals(OemProfile.Samsung, detect("samsung"))
        assertEquals(OemProfile.Huawei, detect("HUAWEI"))
        assertEquals(OemProfile.Huawei, detect("HONOR"))
        assertEquals(OemProfile.Oppo, detect("OPPO"))
        assertEquals(OemProfile.Oppo, detect("realme"))
        assertEquals(OemProfile.Oppo, detect("OnePlus"))
        assertEquals(OemProfile.Vivo, detect("vivo"))
        assertEquals(OemProfile.Vivo, detect("iQOO"))
        assertEquals(OemProfile.Asus, detect("asus"))
        assertEquals(OemProfile.Transsion, detect("TRANSSION", "Infinix"))
        assertEquals(OemProfile.Transsion, detect("TECNO MOBILE LIMITED", "TECNO"))
        assertEquals(OemProfile.Transsion, detect("itel"))
    }

    @Test
    fun matchingUsesContainsNotEquals() {
        assertEquals(OemProfile.Xiaomi, detect("Xiaomi Communications Co Ltd", "unknown"))
        assertEquals(OemProfile.Samsung, detect("SAMSUNG ELECTRONICS", "unknown"))
    }

    @Test
    fun romMarkersIdentifyRebrandedDevices() {
        assertEquals(OemProfile.Xiaomi, detect("unknown", fingerprint = "unknown/sweet/sweet:13/TKQ1/OS1.0.5.0.HYPEROS:user/release-keys"))
        assertEquals(OemProfile.Huawei, detect("unknown", display = "EMUI 12"))
        assertEquals(OemProfile.Oppo, detect("unknown", display = "ColorOS 13"))
        assertEquals(OemProfile.Vivo, detect("unknown", display = "Funtouch OS_13"))
    }

    @Test
    fun stockAndUnknownFallBack() {
        assertEquals(OemProfile.Unknown, detect("Google", "google", "UP1A.231005.007", "google/husky/husky:14/UP1A/1:user/release-keys"))
        assertEquals(OemProfile.Unknown, detect("motorola"))
        assertEquals(OemProfile.Unknown, detect("Nothing"))
        assertEquals(OemProfile.Unknown, OemProfile.detect(null, null, null, null))
        assertEquals(OemProfile.Unknown, detect(""))
        assertFalse(OemProfile.Unknown.aggressive)
    }
}
