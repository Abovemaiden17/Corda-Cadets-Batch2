package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.contracts.UserContract.Companion.User_ID
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import sun.security.util.Password


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UserFlowsValidate(private val user2: Party,
                        private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {


    override val progressTracker = ProgressTracker(GETTING_NOTARY, GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION, SIGNING_TRANSACTION, FINALISING_TRANSACTION)

    @Suspendable
    override fun call():SignedTransaction {
        // Initiator flow logic goes here.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val inputUserStateAndRef = serviceHub.vaultService.queryBy<UserState>(queryCriteria).states.single()
        val inputStateData = inputUserStateAndRef.state.data
        progressTracker.currentStep = GETTING_NOTARY
        val notary = inputUserStateAndRef.state.notary
        progressTracker.currentStep = GENERATING_TRANSACTION
        val outputState = UserState(inputStateData.email,inputStateData.username,inputStateData.password,ourIdentity,user2,true,inputStateData.linearId)//, listOf(ourIdentity)
        println(outputState)
        val txCommand =
                Command(UserContract.Commands.Validate(),listOf(ourIdentity.owningKey,user2.owningKey ))

        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputUserStateAndRef)
                .addOutputState(outputState, User_ID)
                .addCommand(txCommand)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = SIGNING_TRANSACTION
        val partySignedTx =
                serviceHub.signInitialTransaction(txBuilder)
        progressTracker.currentStep = FINALISING_TRANSACTION

        //val sessions = (inputStateData.participants - ourIdentity + user2).map { initiateFlow(it) }.toSet()
        val sessions = initiateFlow(user2)
        //val stx = subFlow(CollectSignaturesFlow(partySignedTx, listOf(sessions)))
        val stx = subFlow(CollectSignaturesFlow(partySignedTx, listOf(sessions)))
        return subFlow(FinalityFlow(stx,sessions))
    }
}


@InitiatedBy(UserFlowsValidate::class)
class UserFlowsValidateResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {
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