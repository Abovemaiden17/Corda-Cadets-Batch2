package com.template.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {

    companion object{
        const val User_ID = "com.template.contracts.UserContract"
    }

    interface Commands : CommandData{
        class Register :TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<UserContract.Commands>()

        when (command.value){
            is UserContract.Commands.Register -> requireThat {

            }
        }

    }
}

