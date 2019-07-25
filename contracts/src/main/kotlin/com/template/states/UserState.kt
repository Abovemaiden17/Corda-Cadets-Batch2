package com.template.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.Party

@BelongsToContract(UserContract::class)
data class UserState(val email: String,
                     val username: String,
                     val password: String,
                     val user: Party,
                     override val )
