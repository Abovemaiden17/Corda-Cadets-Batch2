package com.template.flow

import co.paralleluniverse.fibers.Suspendable
import com.template.contract.KYCContract
import com.template.contract.KYCContract.Companion.KYC_ID
import com.template.states.KYCState
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object KYCValidateFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator (private val name: String) : FlowLogic<SignedTransaction>(){

        override val progressTracker = ProgressTracker(GETTING_NOTARY, GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION, SIGNING_TRANSACTION, FINALISING_TRANSACTION)

        @Suspendable
        override fun call(): SignedTransaction {
            //Get first notary
            progressTracker.currentStep = GETTING_NOTARY
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep = GENERATING_TRANSACTION

            //Get all states with type KYCState
            val userStates = serviceHub.vaultService.queryBy<KYCState>().states

            //Get StateAndRef that matches the data name with the input name
            val inputUserStateAndRef = userStates.find { stateAndRef -> stateAndRef.state.data.name == this.name }
                    ?: throw java.lang.IllegalArgumentException("No User state that matches with name")

            println(inputUserStateAndRef)

            //Access UserStateAndRef data
            val inputStateData = inputUserStateAndRef.state.data

            //Copy data from the accessed UserStateAndRef
            val node = inputStateData.node
            val name = inputStateData.name
            val age = inputStateData.age
            val address = inputStateData.address
            val birthday = inputStateData.birthDate
            val status = inputStateData.status
            val religion = inputStateData.religion
            //Verified = Changed to true
            val isVerified = true
            val list = listOf(ourIdentity)

            val outputState = KYCState(node,name,age,address,birthday,status,religion,isVerified,list)//, listOf(ourIdentity)

            println(outputState)
            val txCommand =
                    Command(KYCContract.Commands.Validate(),ourIdentity.owningKey)

            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputUserStateAndRef)
                    .addOutputState(outputState, KYC_ID)
                    .addCommand(txCommand)



            progressTracker.currentStep = VERIFYING_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION
            val partySignedTx =
                    serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = FINALISING_TRANSACTION
            return subFlow(FinalityFlow(partySignedTx))



        }

    }
}