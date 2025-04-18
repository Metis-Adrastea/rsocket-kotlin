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

package io.rsocket.kotlin.transport.ktor.tcp

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.rsocket.kotlin.internal.io.*
import io.rsocket.kotlin.transport.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

@OptIn(RSocketTransportApi::class)
public sealed interface KtorTcpClientTransport : RSocketTransport {
    public fun target(remoteAddress: SocketAddress): RSocketClientTarget
    public fun target(host: String, port: Int): RSocketClientTarget

    public companion object Factory :
        RSocketTransportFactory<KtorTcpClientTransport, KtorTcpClientTransportBuilder>(::KtorTcpClientTransportBuilderImpl)
}

@OptIn(RSocketTransportApi::class)
public sealed interface KtorTcpClientTransportBuilder : RSocketTransportBuilder<KtorTcpClientTransport> {
    public fun selectorManager(manager: SelectorManager, manage: Boolean)
    public fun socketOptions(block: SocketOptions.TCPClientSocketOptions.() -> Unit)
}

private class KtorTcpClientTransportBuilderImpl : KtorTcpClientTransportBuilder {
    private var selectorManager: SelectorManager? = null
    private var manageSelectorManager: Boolean = true
    private var socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {}

    override fun socketOptions(block: SocketOptions.TCPClientSocketOptions.() -> Unit) {
        this.socketOptions = block
    }

    override fun selectorManager(manager: SelectorManager, manage: Boolean) {
        this.selectorManager = manager
        this.manageSelectorManager = manage
    }

    @RSocketTransportApi
    override fun buildTransport(context: CoroutineContext): KtorTcpClientTransport = KtorTcpClientTransportImpl(
        coroutineContext = context.supervisorContext() + Dispatchers.Default,
        socketOptions = socketOptions,
        selectorManager = selectorManager ?: SelectorManager(Dispatchers.IoCompatible),
        manageSelectorManager = manageSelectorManager
    )
}

private class KtorTcpClientTransportImpl(
    override val coroutineContext: CoroutineContext,
    private val socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit,
    private val selectorManager: SelectorManager,
    manageSelectorManager: Boolean,
) : KtorTcpClientTransport {
    init {
        if (manageSelectorManager) coroutineContext.job.invokeOnCompletion { selectorManager.close() }
    }

    override fun target(remoteAddress: SocketAddress): RSocketClientTarget = KtorTcpClientTargetImpl(
        coroutineContext = coroutineContext.supervisorContext(),
        socketOptions = socketOptions,
        selectorManager = selectorManager,
        remoteAddress = remoteAddress
    )

    override fun target(host: String, port: Int): RSocketClientTarget = target(InetSocketAddress(host, port))
}

@OptIn(RSocketTransportApi::class)
private class KtorTcpClientTargetImpl(
    override val coroutineContext: CoroutineContext,
    private val socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit,
    private val selectorManager: SelectorManager,
    private val remoteAddress: SocketAddress,
) : RSocketClientTarget {
    @RSocketTransportApi
    override suspend fun connectClient(): RSocketConnection {
        currentCoroutineContext().ensureActive()
        coroutineContext.ensureActive()

        return withContext(Dispatchers.IoCompatible) {
            val socket = aSocket(selectorManager).tcp().connect(remoteAddress, socketOptions)
            KtorTcpConnection(
                parentContext = this@KtorTcpClientTargetImpl.coroutineContext,
                socket = socket
            )
        }
    }
}
