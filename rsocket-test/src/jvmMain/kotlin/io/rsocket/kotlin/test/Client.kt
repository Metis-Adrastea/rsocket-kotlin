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

package io.rsocket.kotlin.test

import io.ktor.client.request.*
import io.rsocket.kotlin.*
import io.rsocket.kotlin.core.*
import io.rsocket.kotlin.keepalive.*
import io.rsocket.kotlin.logging.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.transport.ktor.websocket.client.*
import kotlinx.coroutines.*
import org.slf4j.event.*
import kotlin.coroutines.*
import kotlin.time.Duration.Companion.seconds

lateinit var rsocket: RSocket

val Slf4jLoggerFactory = object : LoggerFactory {
    private fun LoggingLevel.sl4jLevel() = when (this) {
        LoggingLevel.TRACE -> Level.TRACE
        LoggingLevel.DEBUG -> Level.DEBUG
        LoggingLevel.INFO  -> Level.INFO
        LoggingLevel.WARN  -> Level.WARN
        LoggingLevel.ERROR -> Level.ERROR
    }

    override fun logger(tag: String): Logger = org.slf4j.LoggerFactory.getLogger(tag).let { logger ->
        return object : Logger {
            override val tag: String
                get() = logger.name

            override fun isLoggable(level: LoggingLevel) = logger.isEnabledForLevel(level.sl4jLevel())

            override fun rawLog(
                level: LoggingLevel,
                throwable: Throwable?,
                message: Any?,
            ) {
                logger.atLevel(level.sl4jLevel()).setCause(throwable).log(message?.toString())
            }
        }
    }
}

suspend fun main() {
    rsocket = RSocketConnector {
        connectionConfig {
            payloadMimeType = PayloadMimeType(
                WellKnownMimeType.ApplicationJson,
                WellKnownMimeType.MessageRSocketRouting
            )
            keepAlive = KeepAlive(5.seconds, 10.seconds)
        }
        loggerFactory = Slf4jLoggerFactory
        reconnectable { cause, attempt ->
            println("$attempt ${cause.message}")
            delay(5.seconds)
            true
        }
    }.connect(KtorWebSocketClientTransport(coroutineContext).target {
        this.host = "localhost"
        this.port = 7777
    })
    while (true) {
        delay(5.seconds)
        try {
            //println(rsocket.requestResponse(payload("find-rates", Unit)).data.readText())
            //println(requestResponse<Unit, String>("find-rates", Unit))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}