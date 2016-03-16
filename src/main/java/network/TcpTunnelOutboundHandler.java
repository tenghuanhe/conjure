package network;

import logging.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class TcpTunnelOutboundHandler extends SimpleChannelHandler {
    private static final Logger LOG = Logger.getLogger(TcpTunnelOutboundHandler.class);
    private final String id;
    private final Channel inboundChannel;
    private final String host;
    private final int port;


    public TcpTunnelOutboundHandler(String id, String host, int port, Channel inboundChannel) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        this.inboundChannel.write(msg);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelConnected(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        String remoteAddress;
        if (e.getChannel().getRemoteAddress() == null) {
            remoteAddress = "/" + this.host + ":" + this.port;
        } else {
            remoteAddress = e.getChannel().getRemoteAddress().toString();
        }

        LOG.debug("Outbound tunnel from {} to {} (TcpTunnel with id '{}') closed; closing matching inbound channel.",
                this.inboundChannel.getRemoteAddress(), remoteAddress, this.id);
        TcpTunnelUtils.closeAfterFlushPendingWrites(this.inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getChannel().isConnected()) {
            LOG.debug("Exception caught on tunnel from {} to {} (TcpTunnel with id '{}'); closing channel.",
                    e.getCause(), this.inboundChannel.getRemoteAddress(), e.getChannel().getRemoteAddress(), this.id);
        } else {
            LOG.debug("Exception caught on tunnel connecting to {}:{} (TcpTunnel with id '{}').",
                    e.getCause(), this.host, this.port, this.id);
        }

        TcpTunnelUtils.closeAfterFlushPendingWrites(e.getChannel());
    }
}
