/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.kotlin.transport.ktor.tcp

import io.rsocket.kotlin.*
import io.rsocket.kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlin.test.*

class TcpServerTest : SuspendTest {
    private val testJob = Job()
    private val testContext = testJob + TestExceptionHandler
    private val serverTransport = KtorTcpServerTransport(testContext).target("127.0.0.1")
    private fun KtorTcpServerInstance.clientTransport() =
        KtorTcpClientTransport(testContext).target(localAddress)

    override suspend fun after() {
        testJob.cancelAndJoin()
    }

    @Test
    fun testFailedConnection() = test {
        val server = TestServer().startServer(serverTransport) {
            if (config.setupPayload.data.readString() == "ok") {
                RSocketRequestHandler {
                    requestResponse { it }
                }
            } else error("FAILED")
        }

        suspend fun newClient(text: String) = TestConnector {
            connectionConfig {
                setupPayload {
                    payload(text)
                }
            }
        }.connect(server.clientTransport())

        val client1 = newClient("ok")
        client1.requestResponse(payload("ok")).close()

        val client2 = newClient("not ok")
        assertFails {
            client2.requestResponse(payload("not ok"))
        }

        val client3 = newClient("ok")

        client3.requestResponse(payload("ok")).close()
        client1.requestResponse(payload("ok")).close()

        assertTrue(client1.isActive)
        assertFalse(client2.isActive)
        assertTrue(client3.isActive)

        assertTrue(server.isActive)

        client1.coroutineContext.job.cancelAndJoin()
        client2.coroutineContext.job.cancelAndJoin()
        client3.coroutineContext.job.cancelAndJoin()
    }

    @Test
    fun testFailedHandler() = test {
        val handlers = mutableListOf<RSocket>()
        val server = TestServer().startServer(serverTransport) {
            RSocketRequestHandler {
                requestResponse { it }
            }.also { handlers += it }
        }

        suspend fun newClient() = TestConnector().connect(server.clientTransport())

        val client1 = newClient()

        client1.requestResponse(payload("1")).close()

        val client2 = newClient()

        client2.requestResponse(payload("1")).close()

        handlers[1].coroutineContext.job.apply {
            cancel("FAILED")
            join()
        }

        client1.requestResponse(payload("1")).close()

        assertFails {
            client2.requestResponse(payload("1"))
        }

        val client3 = newClient()

        client3.requestResponse(payload("1")).close()

        client1.requestResponse(payload("1")).close()

        assertTrue(client1.isActive)
        assertFalse(client2.isActive)
        assertTrue(client3.isActive)

        assertTrue(server.isActive)

        client1.coroutineContext.job.cancelAndJoin()
        client2.coroutineContext.job.cancelAndJoin()
        client3.coroutineContext.job.cancelAndJoin()
    }
}
