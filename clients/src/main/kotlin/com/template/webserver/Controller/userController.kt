package com.template.webserver


import com.template.flows.UserFlows
import com.template.webserver.NodeRPCConnection
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.template.models.*
import com.template.states.UserState
import net.corda.core.contracts.StateAndRef
import javax.servlet.http.HttpServletRequest


private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class KYCController(
        private val rpc: NodeRPCConnection
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val proxy = rpc.proxy

    /**
     * Return all KYCState
     */
    @GetMapping(value = "/states/User", produces = arrayOf("application/json"))
    private fun getUserStates() : ResponseEntity<Map<String,Any>>{
        val (status, result ) = try {
            val userStateRef = rpc.proxy.vaultQueryBy<UserState>().states
            val userStates = userStateRef.map { it.state.data }
            val list = userStates.map {
                userModel(
                        email = it.email,
                        username = it.username,
                        password = it.password,
                        user = it.user.name.toString(),
                        verification = it.verification,
                        linearId = it.linearId.toString())
            }
            HttpStatus.CREATED to list
        }catch( e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in getting ContractState of type KYCState"}
        else{ "message" to "Failed to get ContractState of type KYCState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))


    }

    /**
     * REGISTER - KYCRegister
     */

    @PostMapping(value = "/states/User/create", produces = arrayOf("application/json"))
    private fun createUser(@RequestBody createUser: createUser) : ResponseEntity<Map<String,Any>> {

        val (status, result) = try {
            val user = createUser(
                    email = createUser.email,
                    username = createUser.username,
                    password = createUser.password

            )
            proxy.startFlowDynamic(
                    UserFlows::class.java,
                    user.email,
                    user.username,
                    user.password

            )
//            val out = registerFlow.use { it.returnValue.getOrThrow() }
            val userStateRef = proxy.vaultQueryBy<UserState>().states.last()
            val userStateData = userStateRef.state.data
            val list = userModel(
                    email = userStateData.email,
                    username = userStateData.username,
                    password = userStateData.password,
                    user = userStateData.user.name.toString(),
                    verification = userStateData.verification,
                    linearId = userStateData.linearId.toString()
            )
            HttpStatus.CREATED to list
        }catch (e: Exception){
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status==HttpStatus.CREATED){
            "message" to "Successful in creating ContractState of type UserState"}
        else{ "message" to "Failed to create ContractState of type UserState"}
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))

    }

    /**
     * KYC UPDATE
     */


}