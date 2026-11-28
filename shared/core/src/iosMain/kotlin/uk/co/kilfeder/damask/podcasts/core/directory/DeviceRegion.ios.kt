package uk.co.kilfeder.damask.podcasts.core.directory

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale

actual fun deviceRegion(): String? =
    (NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String)?.ifBlank { null }
