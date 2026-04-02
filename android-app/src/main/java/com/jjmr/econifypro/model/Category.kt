package com.jjmr.econifypro.model
import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("icon_ref") val iconRef: String? = null
)