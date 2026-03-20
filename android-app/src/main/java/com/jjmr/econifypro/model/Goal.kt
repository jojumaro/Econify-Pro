package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class Goal(
    val id: Int,
    val name: String?,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("current_amount")
    val currentAmount: Double,
    val deadline: String?
)