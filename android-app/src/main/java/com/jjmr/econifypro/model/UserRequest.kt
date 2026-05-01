package com.jjmr.econifypro.model

data class UserRequest(
    val username: String = "",
    val email: String,
    val password: String,
    val firstname: String = "",
    val lastname: String = "",
    val security_question_1: Int,
    val security_answer_1: String,
    val security_question_2: Int,
    val security_answer_2: String
)