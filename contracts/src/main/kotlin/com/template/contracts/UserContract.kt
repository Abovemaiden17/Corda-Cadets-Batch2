package com.template.contracts

import com.template.states.UserState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract {

    companion object{
        const val User_ID = "com.template.contracts.UserContract"
    }

    interface Commands : CommandData{
        class Register :TypeOnlyCommandData(), Commands
        class Validate : TypeOnlyCommandData(), Commands
        class Update : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value){
            is Commands.Register -> requireThat {

            }

            is Commands.Validate -> requireThat{


            }

            is Commands.Update -> requireThat {

            }

        }

    }
}

