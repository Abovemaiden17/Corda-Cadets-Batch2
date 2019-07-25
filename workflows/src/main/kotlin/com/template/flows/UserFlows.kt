package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UserFlows(private val email: String,
                private val username: String,
                private val password: String) : FlowLogic<SignedTransaction>() {


    override val progressTracker = ProgressTracker(GETTING_NOTARY, GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION, SIGNING_TRANSACTION, FINALISING_TRANSACTION)

    @Suspendable
    override fun call():SignedTransaction {
        // Initiator flow logic goes here.
        progressTracker.currentStep = GETTING_NOTARY
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = GENERATING_TRANSACTION

        val userState = UserState(email,username,password, ourIdentity)
        val txCommand = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(userState, UserContract.User_ID)
                .addCommand(txCommand)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = SIGNING_TRANSACTION
        val partySignedTx =
                serviceHub.signInitialTransaction(txBuilder)
        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(FinalityFlow(partySignedTx, listOf()))
    }
}

//@InitiatedBy(UserFlows::class)
////class UserFlowsResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
////    @Suspendable
////    override fun call() {
////        // Responder flow logic goes here.
////    }
////}