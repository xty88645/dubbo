/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.remoting.transport.netty4.ssl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class SslClientTlsHandler extends ChannelInboundHandlerAdapter {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SslClientTlsHandler.class);
    private static final AttributeKey<SSLSession> SSL_SESSION_KEY = AttributeKey.valueOf(Constants.SSL_SESSION_KEY);
    private final SslContext sslContext;

    public SslClientTlsHandler(URL url) {
        this(SslContexts.buildClientSslContext(url));
    }

    public SslClientTlsHandler(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        SSLEngine sslEngine = sslContext.newEngine(ctx.alloc());
        ctx.pipeline().addAfter(ctx.name(), null, new SslHandler(sslEngine, false));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
            if (handshakeEvent.isSuccess()) {
                SSLSession session =
                        ctx.pipeline().get(SslHandler.class).engine().getSession();
                logger.info("TLS negotiation succeed with: " + session.getPeerHost());
                ctx.pipeline().remove(this);
                ctx.channel().attr(SSL_SESSION_KEY).set(session);
            } else {
                logger.error(
                        INTERNAL_ERROR,
                        "unknown error in remoting module",
                        "",
                        "TLS negotiation failed when trying to accept new connection.",
                        handshakeEvent.cause());
                ctx.fireExceptionCaught(handshakeEvent.cause());
            }
        }
    }
}
