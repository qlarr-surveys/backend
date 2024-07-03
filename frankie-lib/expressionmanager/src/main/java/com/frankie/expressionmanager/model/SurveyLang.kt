package com.frankie.expressionmanager.model


data class SurveyLang(val code: String, val name: String) {

    companion object {
        val EN = SurveyLang("en", "English")

        val DE = SurveyLang("de", "Deutsch")

        val AR = SurveyLang("ar", "العربية")
    }


}

