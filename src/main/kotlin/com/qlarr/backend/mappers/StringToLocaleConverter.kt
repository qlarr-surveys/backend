package com.qlarr.backend.mappers

import com.fasterxml.jackson.databind.util.StdConverter
import java.util.*


class StringToLocaleConverter : StdConverter<String, Locale>() {

    override fun convert(value: String): Locale {
        return Locale.forLanguageTag(value)
    }
}
