package network;

import balancing.BalancingStrategy;
import logging.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import utils.HostPort;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class TcpTunnelInboundHandler extends SimpleChannelHandler {
    private static final Logger LOG = Logger.getLogger(TcpTunnelInboundHandler.class);

    private final ClientSocketChannelFactory factory;
    private final ChannelGroup channelGroup;
    private final int connectTime;
    private final String id;
    private final AtomicLong outboundCounter = new AtomicLong();
    private final AtomicLong inboundCounter = new AtomicLong();
    private BalancingStrategy strategy;
    private String remoteHost;
    private int remotePort;
    private volatile Channel outboundChannel;

    public TcpTunnelInboundHandler(String id, ChannelGroup channelGroup, ClientSocketChannelFactory factory,
                                   BalancingStrategy strategy, int connectTime) {
        this.id = id;
        this.channelGroup = channelGroup;
        this.factory = factory;
        this.strategy = strategy;
        this.connectTime = connectTime;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.channelGroup.add(e.getChannel());
        InetSocketAddress address = (InetSocketAddress) e.getChannel().getRemoteAddress();
        HostPort target = this.strategy.selectTarget(address.getHostName(), address.getPort());
        this.remoteHost = target.getHost();
        this.remotePort = target.getPort();

        final Channel inboundChannel = e.getChannel();
        inboundChannel.setReadable(false);

        ClientBootstrap cb = new ClientBootstrap(factory);
        cb.setOption("tcpNoDelay", true);
        cb.setOption("connectTimeMillis", this.connectTime);

        cb.getPipeline().addLast("handler", (ChannelHandler) new TcpTunnelOutboundHandler(this.id, this.remoteHost, this.remotePort, e.getChannel()));
        ChannelFuture f = cb.connect(new InetSocketAddress(this.remoteHost, this.remotePort));

        this.outboundChannel = f.getChannel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    inboundChannel.setReadable(true);
                    LOG.debug("Successfully created tunnel from {} to {} on LoadBalancer with id '{}'.",
                            inboundChannel.getRemoteAddress(), future.getChannel().getRemoteAddress(), id);
                } else {
                    LOG.debug("Failed to create tunnel from {} to {}:{} on LoadBalancer with id '{}'.",
                            inboundChannel.getRemoteAddress(), remoteHost, remotePort, id);
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        this.inboundCounter.addAndGet(msg.readableBytes());
        this.outboundChannel.write(msg);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        super.writeComplete(ctx, e);
        this.outboundCounter.addAndGet(e.getWrittenAmount());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (this.outboundChannel != null) {
            TcpTunnelUtils.closeAfterFlushPendingWrites(this.outboundChannel);
            LOG.debug("Tunnel from {} to {} (LoadBalancer with id '{}') closed (a->b: {}b, b->: {}b).",
                    e.getChannel().getRemoteAddress(), this.outboundChannel.getRemoteAddress(), this.id,
                    this.inboundCounter.get(), this.outboundCounter.get());
        } else {
            LOG.debug("Tunnel from {} to {}:{} on LoadBalancer with id '{}' closed (a-b: {}b, b->a: {}b).",
                    e.getChannel().getRemoteAddress(), this.remoteHost, this.remotePort, this.id,
                    this.inboundCounter.get(), this.outboundCounter.get());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.debug("Exception caught on tunnel from {} to {} (LoadBalancer with id '{}'); closing channel.",
                e.getCause(), e.getChannel().getRemoteAddress(), this.outboundChannel.getRemoteAddress(), this.id);
        TcpTunnelUtils.closeAfterFlushPendingWrites(e.getChannel());
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }
}
