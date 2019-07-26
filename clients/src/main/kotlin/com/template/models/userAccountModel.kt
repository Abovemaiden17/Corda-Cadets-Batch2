package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator

data class userAccountModel(

        val email : String,
        val username : String,
        val password : String


)

data class createUserAccount @JsonCreator constructor(
        val email : String,
        val username : String,
        val password : String

)