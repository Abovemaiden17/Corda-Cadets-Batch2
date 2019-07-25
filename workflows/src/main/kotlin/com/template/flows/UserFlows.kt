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
                private val password: String
                ) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call():SignedTransaction {
        // Initiator flow logic goes here.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val userState = UserState(email,username,password, ourIdentity,ourIdentity,verification = false)
        val txCommand = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(userState, UserContract.User_ID)
                .addCommand(txCommand)
        txBuilder.verify(serviceHub)
        val partySignedTx =
                serviceHub.signInitialTransaction(txBuilder)
        return subFlow(FinalityFlow(partySignedTx, listOf()))
    }
}