package com.frankie.expressionmanager.model.adapters

import com.frankie.expressionmanager.ext.isAnswerCode
import com.frankie.expressionmanager.ext.isGroupCode
import com.frankie.expressionmanager.ext.isQuestionCode
import com.frankie.expressionmanager.ext.isSurveyCode
import com.frankie.expressionmanager.model.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class SurveyComponentGson : JsonDeserializer<SurveyComponent>, JsonSerializer<SurveyComponent> {
    val gson = Gson()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SurveyComponent {

        var code: String? = null
        var instructionList: List<Instruction> = listOf()
        var errors: List<ComponentError> = listOf()
        var questions: List<Question> = listOf()
        var groups: List<Group> = listOf()
        var answers: List<Answer> = listOf()
        var groupType: GroupType = GroupType.GROUP
        val jsonObject: JsonObject = json.asJsonObject

        jsonObject.keySet().forEach { key ->
            when (key) {
                "code" -> code = jsonObject[key].asString
                "instructionList" -> {
                    val type = object : TypeToken<ArrayList<Instruction>>() {}.type
                    instructionList = gson.fromJson(jsonObject[key], type)
                }

                "questions" -> {
                    val type = object : TypeToken<ArrayList<Question>>() {}.type
                    questions = gson.fromJson(jsonObject[key], type)
                }

                "groups" -> {
                    val type = object : TypeToken<ArrayList<Group>>() {}.type
                    groups = gson.fromJson(jsonObject[key], type)
                }

                "answers" -> {
                    val type = object : TypeToken<ArrayList<Answer>>() {}.type
                    answers = gson.fromJson(jsonObject[key], type)
                }

                "errors" -> {
                    val type = object : TypeToken<ArrayList<ComponentError>>() {}.type
                    errors = gson.fromJson(jsonObject[key], type)
                }

                "groupType" -> {
                    groupType = gson.fromJson(jsonObject[key], GroupType::class.java)
                }
            }

        }
        return when {
            code!!.isSurveyCode() -> Survey(instructionList, groups, errors)
            code!!.isGroupCode() -> Group(code!!, instructionList, questions, groupType, errors)
            code!!.isQuestionCode() -> Question(code!!, instructionList, answers, errors)
            code!!.isAnswerCode() -> Answer(code!!, instructionList, answers, errors)
            else -> {
                throw IllegalArgumentException("Invalid code for SurveyComponent: $code")
            }
        }
    }


    override fun serialize(src: SurveyComponent, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("code", src.code)
        jsonObject.add("instructionList", gson.toJsonTree(src.instructionList))

        when (src) {
            is Group -> {
                jsonObject.add("questions", gson.toJsonTree(src.questions))
                jsonObject.add("groupType", gson.toJsonTree(src.groupType))
            }

            is Answer -> {
                jsonObject.add("answers", gson.toJsonTree(src.answers))
            }

            is Question -> {
                jsonObject.add("answers", gson.toJsonTree(src.answers))
            }

            is Survey -> {
                // do nothing
            }
        }
        jsonObject.add("errors", gson.toJsonTree(src.errors))
        return jsonObject
    }

}