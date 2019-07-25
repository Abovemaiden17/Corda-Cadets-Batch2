package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UserFlowsUpdate(private val email: String,
                      private val username: String,
                      private val password: String,
                      private val linearId: UniqueIdentifier,
                      private val user2: Party) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call():SignedTransaction {
        // Initiator flow logic goes here.
        val inputUserCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val inputStateandRef = serviceHub.vaultService.queryBy<UserState>(inputUserCriteria).states.single()
        val input = inputStateandRef.state.data
        val notary = inputStateandRef.state.notary
        val userState = UserState(email,username,password, input.user,input.user2,true,input.linearId)
        val txCommand = Command(UserContract.Commands.Update(), listOf(ourIdentity.owningKey,user2.owningKey ))
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateandRef)
                .addOutputState(userState, UserContract.User_ID)
                .addCommand(txCommand)
        txBuilder.verify(serviceHub)
        val partySignedTx =
                serviceHub.signInitialTransaction(txBuilder)
        val sessions = (userState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        //val stx = subFlow(CollectSignaturesFlow(partySignedTx, listOf(sessions)))
        val stx = subFlow(CollectSignaturesFlow(partySignedTx, sessions))
        return subFlow(FinalityFlow(stx,sessions))
    }
}
@InitiatedBy(UserFlowsUpdate::class)
class UserFlowsUpdateResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a transaction" using (output is UserState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}