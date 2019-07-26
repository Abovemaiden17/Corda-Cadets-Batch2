package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier

data class userModel(
        val email : String,
        val username : String,
        val password : String,
        val user : String,

        val verification : Boolean,

        val linearId : Any
)

data class createUser @JsonCreator constructor(
        val email : String,
        val username : String,
        val password : String


)