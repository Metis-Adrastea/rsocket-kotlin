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

package io.rsocket.kotlin.transport.ktor.websocket.internal

import io.ktor.websocket.*
import io.rsocket.kotlin.*
import kotlinx.coroutines.*
import kotlinx.io.*

@Suppress("DEPRECATION_ERROR")
@Deprecated(level = DeprecationLevel.ERROR, message = "Deprecated in favor of new Transport API")
public class WebSocketConnection(
    private val session: WebSocketSession,
) : Connection, CoroutineScope by session {
    override suspend fun send(packet: Buffer) {
        session.send(packet.readByteArray())
    }

    override suspend fun receive(): Buffer {
        val frame = session.incoming.receive()
        return Buffer().apply { write(frame.data) }
    }

}
