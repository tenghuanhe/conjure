package network;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class TcpTunnelUtils {
    public static void closeAfterFlushPendingWrites(Channel channel) {
        if (channel.isConnected()) {
            channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
