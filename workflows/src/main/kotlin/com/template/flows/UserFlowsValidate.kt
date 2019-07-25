package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.UserContract
import com.template.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import sun.security.util.Password


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class UserFlowsValidate(private val email: String,
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
        val userState = UserState(email,username,password, ourIdentity,verification = true)
        val txCommand = Command(UserContract.Commands.Validate(),ourIdentity.owningKey)
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(userState, UserContract.User_ID)
                .addCommand(txCommand)
        progressTracker.currentStep = VERIFYING_TRANSACTION
        txBuilder.verify(serviceHub)
        progressTracker.currentStep = SIGNING_TRANSACTION
        val partySignedTx =
                serviceHub.signInitialTransaction(txBuilder)
        progressTracker.currentStep = FINALISING_TRANSACTION

        val sessions = (userState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(partySignedTx, sessions))
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
                "This must be an IOU transaction" using (output is UserState)
            }
        }
        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}