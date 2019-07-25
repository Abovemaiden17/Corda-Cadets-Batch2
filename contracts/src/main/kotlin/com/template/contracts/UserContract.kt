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
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value){
            is Commands.Register -> requireThat {
                "No inputs should be consumed when creating registration." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val outputRegister = tx.outputsOfType<UserState>().single()
                "Email must not be empty" using (outputRegister.email.isNotEmpty())
                "Username must not be empty" using (outputRegister.username.isNotEmpty())
                "Password must not be empty" using (outputRegister.password.isNotEmpty())
            }

            is Commands.Validate -> requireThat{
                val inputValidate = tx.inputsOfType<UserState>()
                val outputValidate = tx.outputsOfType<UserState>()
                "Only one input should be consumed when validating" using (inputValidate.size == 1)
                "Only one output should be consumed when validating" using (outputValidate.size == 1)

                "Input must be UserState" using (tx.inputStates[0] is UserState)
                "Output must be UserState" using (tx.outputStates[0] is UserState)

                "Input Verified must be false" using (!inputValidate.single().verification)
                "Output Verified must be true" using (outputValidate.single().verification)
                val inputValidateState = inputValidate.single()
                val outputValidateState = inputValidate.single()
                "Email inputState and outputState" using (inputValidateState.email == outputValidateState.email)
                "Username inputState and outputState" using (inputValidateState.username == outputValidateState.username)
                "Password inputState and outputState" using (inputValidateState.password == outputValidateState.password)

            }

        }

    }
}

