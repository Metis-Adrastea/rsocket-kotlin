/*
 * Copyright 2015-2025 the original author or authors.
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

@file:OptIn(ExperimentalMetadataApi::class)

package io.rsocket.kotlin.test

import io.ktor.server.cio.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.metadata.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.transport.ktor.websocket.server.*
import kotlinx.coroutines.*
import kotlinx.io.Buffer
import kotlinx.serialization.json.*
import kotlin.coroutines.*

val json = Json {
    encodeDefaults = true
}

fun Payload.route(): String = metadata
    ?.read(RoutingMetadata)
    ?.tags
    ?.first()
    ?: error("No route provided")

inline fun <reified T> payload(route: String, t: T): Payload = buildPayload {
    metadata(RoutingMetadata(route).toBuffer())
    if (t == Unit) data(Buffer()) else data(json.encodeToString(t))
}

inline fun <reified T> Payload.data(): T = json.decodeFromString(data.readText())

inline fun <reified T> payload(t: T) =
    if (t == Unit) Payload.Empty else buildPayload { data(json.encodeToString(t)) }

suspend fun main() {
    RSocketServer {
        loggerFactory = Slf4jLoggerFactory
    }
        .startServer(
            KtorWebSocketServerTransport(coroutineContext) { httpEngine(CIO) }.target { port = 7777 }
        ) {
            RSocketRequestHandler {
                requestResponse { payload ->
                    when (val route = payload.route()) {
                        "test" -> payload("test")
                        else   -> error("Wrong route: $route")
                    }
                }
            }
        }.coroutineContext.job.join()
}