package com.frankie.expressionmanager.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.frankie.expressionmanager.model.adapters.BindingErrorsGson
import com.google.gson.annotations.JsonAdapter

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "name",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(BindingErrors.ForwardDependency::class, name = "ForwardDependency"),
    JsonSubTypes.Type(BindingErrors.ScriptFailure::class, name = "ScriptFailure"),
    JsonSubTypes.Type(BindingErrors.InvalidSkipReference::class, name = "InvalidSkipReference"),
    JsonSubTypes.Type(BindingErrors.InvalidReference::class, name = "InvalidReference"),
    JsonSubTypes.Type(BindingErrors.InvalidChildReferences::class, name = "InvalidChildReferences"),
    JsonSubTypes.Type(BindingErrors.DuplicateRandomGroupItems::class, name = "DuplicateRandomGroupItems"),
    JsonSubTypes.Type(BindingErrors.DuplicatePriorityGroupItems::class, name = "DuplicatePriorityGroupItems"),
    JsonSubTypes.Type(BindingErrors.PriorityLimitMismatch::class, name = "PriorityLimitMismatch"),
    JsonSubTypes.Type(BindingErrors.PriorityGroupItemNotChild::class, name = "PriorityGroupItemNotChild"),
    JsonSubTypes.Type(BindingErrors.RandomGroupItemNotChild::class, name = "RandomGroupItemNotChild"),
    JsonSubTypes.Type(BindingErrors.InvalidRandomItem::class, name = "InvalidRandomItem"),
    JsonSubTypes.Type(BindingErrors.InvalidPriorityItem::class, name = "InvalidPriorityItem"),
    JsonSubTypes.Type(BindingErrors.InvalidInstructionInEndGroup::class, name = "InvalidInstructionInEndGroup"),
    JsonSubTypes.Type(BindingErrors.SkipToEndOfEndGroup::class, name = "SkipToEndOfEndGroup"),
    JsonSubTypes.Type(BindingErrors.DuplicateInstructionCode::class, name = "DuplicateInstructionCode")
)
@JsonAdapter(BindingErrorsGson::class)
sealed class BindingErrors(val name: String = "") {

    @JsonAdapter(BindingErrorsGson::class)
    data class ForwardDependency(val dependency: Dependency) : BindingErrors("ForwardDependency")
    @JsonAdapter(BindingErrorsGson::class)
    data class ScriptFailure(val scriptFailure: ScriptResult) : BindingErrors("ScriptFailure")
    @JsonAdapter(BindingErrorsGson::class)
    data class InvalidSkipReference(val component: String) : BindingErrors("InvalidSkipReference")
    @JsonAdapter(BindingErrorsGson::class)
    object SkipToEndOfEndGroup : BindingErrors("SkipToEndOfEndGroup")
    @JsonAdapter(BindingErrorsGson::class)
    data class InvalidReference(val reference: String, val invalidComponent: Boolean) :
        BindingErrors("InvalidReference")

    @JsonAdapter(BindingErrorsGson::class)
    data class InvalidChildReferences(val children: List<String>) : BindingErrors("InvalidChildReferences")
    @JsonAdapter(BindingErrorsGson::class)
    object PriorityLimitMismatch : BindingErrors("DuplicateLimitMismatch")
    @JsonAdapter(BindingErrorsGson::class)
    data class DuplicatePriorityGroupItems(val items: List<String>) : BindingErrors("DuplicatePriorityGroupItems")
    @JsonAdapter(BindingErrorsGson::class)
    data class PriorityGroupItemNotChild(val items: List<String>) : BindingErrors("PriorityGroupItemNotChild")
    @JsonAdapter(BindingErrorsGson::class)
    data class InvalidPriorityItem(val items: List<String>) : BindingErrors("InvalidPriorityItem")
    @JsonAdapter(BindingErrorsGson::class)
    data class InvalidRandomItem(val items: List<String>) : BindingErrors("InvalidRandomItem")
    @JsonAdapter(BindingErrorsGson::class)
    data class DuplicateRandomGroupItems(val items: List<String>) : BindingErrors("DuplicateRandomGroupItems")
    @JsonAdapter(BindingErrorsGson::class)
    data class RandomGroupItemNotChild(val items: List<String>) : BindingErrors("RandomGroupItemNotChild")
    @JsonAdapter(BindingErrorsGson::class)
    object DuplicateInstructionCode : BindingErrors("DuplicateInstructionCode")
    @JsonAdapter(BindingErrorsGson::class)
    object InvalidInstructionInEndGroup : BindingErrors("InvalidInstructionInEndGroup")

}
