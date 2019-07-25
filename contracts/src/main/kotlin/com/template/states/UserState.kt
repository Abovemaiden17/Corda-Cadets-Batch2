package com.template.states

import com.template.contracts.UserContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(UserContract::class)
data class UserState(val email: String,
                     val username: String,
                     val password: String,
                     val user: Party,
                     val verification : Boolean,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     override val participants: List<Party> = listOf(user)
): LinearState