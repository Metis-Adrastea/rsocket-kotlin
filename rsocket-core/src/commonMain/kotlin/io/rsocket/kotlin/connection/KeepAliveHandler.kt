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

package io.rsocket.kotlin.connection

import io.rsocket.kotlin.*
import io.rsocket.kotlin.frame.io.*
import io.rsocket.kotlin.keepalive.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlin.time.*

internal class KeepAliveHandler(
    private val keepAlive: KeepAlive,
    private val outbound: ConnectionOutbound,
    private val connectionScope: CoroutineScope,
) {
    private val initial = TimeSource.Monotonic.markNow()
    private fun currentDelayMillis() = initial.elapsedNow().inWholeMilliseconds

    private val lastMark = atomic(currentDelayMillis()) // mark initial timestamp for keepalive

    init {
        // this could be moved to a function like `run` or `start`
        connectionScope.launch {
            launch {
                while (true) {
                    delay(keepAlive.intervalMillis.toLong())
                    outbound.sendKeepAlive(true, EmptyBuffer, 0)
                }
            }
            while (true) {
                delay(keepAlive.intervalMillis.toLong())
                if (currentDelayMillis() - lastMark.value >= keepAlive.maxLifetimeMillis)
                    throw RSocketError.ConnectionError("No keep-alive for ${keepAlive.maxLifetimeMillis} ms")
            }
        }
    }

    fun receive(data: Buffer, respond: Boolean) {
        lastMark.value = currentDelayMillis()
        // in most cases it will be possible to not suspend at all
        if (respond) connectionScope.launch(start = CoroutineStart.UNDISPATCHED) {
            outbound.sendKeepAlive(false, data, 0)
        }
    }
}
