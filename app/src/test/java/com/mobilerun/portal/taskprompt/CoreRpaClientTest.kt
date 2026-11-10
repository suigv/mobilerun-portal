package com.mobilerun.portal.taskprompt

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreRpaClientTest {
    @Test
    fun packageUsesActualPhaseAndKeepsTheProposedAgreementIdentity() {
        val body = JSONObject(
            """
            {
              "package":{"package_id":"pkg_123","goal":"A complete goal"},
              "progress":{"phase":"awaiting_confirmation"},
              "model":"model-a",
              "contract":{"contract_id":"contract_2","status":"proposed",
                          "alignment":"Agreement","contract_json":"{\"output\":\"result\"}"}
            }
            """.trimIndent(),
        )
        val result = CoreRpaClient.parsePackage(body)
        assertEquals("pkg_123", result.id)
        assertEquals("awaiting_confirmation", result.phase)
        assertEquals("model-a", result.model)
        assertEquals("contract_2", result.contractId)
        assertEquals("proposed", result.contractStatus)
        assertEquals("""{"output":"result"}""", result.contractDetails)
    }

    @Test
    fun createdPackageDoesNotInventAContractOrCompletion() {
        val body = JSONObject(
            """{"package":{"package_id":"pkg_1","goal":"A goal"},"progress":{"phase":"intake"}}""",
        )
        val result = CoreRpaClient.parsePackage(body)
        assertEquals("intake", result.phase)
        assertNull(result.contractId)
        assertNull(result.model)
    }
}
