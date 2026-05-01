package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class Goal(
    val id: Int = 0,
    val name: String?,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("current_amount")
    val currentAmount: Double,
    val deadline: String?,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("goal_type")
    val goalType: String = "TOTAL_SAVINGS",
    val month: String? = null,
    val year: String? = null
)