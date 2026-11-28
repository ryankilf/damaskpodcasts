package uk.co.kilfeder.damask.podcasts.core.directory

import java.util.Locale

actual fun deviceRegion(): String? = Locale.getDefault().country.ifBlank { null }
