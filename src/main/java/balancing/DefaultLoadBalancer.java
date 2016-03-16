package balancing;

import logging.Logger;
import network.TcpTunnelInboundHandler;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import utils.HostPort;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class DefaultLoadBalancer implements LoadBalancer {

    public static final int TIMEOUT_IN_MILLIS = 3000;
    private static final Logger LOG = Logger.getLogger(DefaultLoadBalancer.class);

    private final String id;
    private final HostPort balancerAddress;
    private final BalancingStrategy balancingStrategy;
    private final Executor bossPool;
    private final Executor workerPool;
    private final boolean internalPools;
    private int timeoutInMillis;
    private volatile boolean running;
    private Channel acceptor;
    private ChannelGroup allChannels;
    private ServerBootstrap bootstrap;

    public DefaultLoadBalancer(String id, HostPort balancerAddress, BalancingStrategy balancingStrategy) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (balancerAddress == null) {
            throw new IllegalArgumentException("Balancer local address cannot be null");
        }
        if (balancingStrategy == null) {
            throw new IllegalArgumentException("Balancing strategy cannot be null");
        }

        this.id = id;
        this.balancerAddress = balancerAddress;
        this.balancingStrategy = balancingStrategy;

        this.internalPools = true;
        this.bossPool = Executors.newCachedThreadPool();
        this.workerPool = Executors.newCachedThreadPool();

        this.timeoutInMillis = TIMEOUT_IN_MILLIS;
    }

    // There is some problem with the origin code in this DefaultLoadBalancer constructor
    public DefaultLoadBalancer(String id, HostPort balancerAddress, BalancingStrategy balancingStrategy,
                               Executor bossPool, Executor workerPool) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (balancerAddress == null) {
            throw new IllegalArgumentException("Balancer local address cannot be null");
        }
        if (balancingStrategy == null) {
            throw new IllegalArgumentException("Balancing strategy cannot be null");
        }

        if (bossPool == null) {
            throw new IllegalArgumentException("BossPoll cannot be null");
        }
        if (workerPool == null) {
            throw new IllegalArgumentException("WorkerPool cannot be null");
        }

        this.id = id;
        this.balancerAddress = balancerAddress;
        this.balancingStrategy = balancingStrategy;
        this.internalPools = true;
        this.bossPool = bossPool;
        this.workerPool = workerPool;
        this.timeoutInMillis = TIMEOUT_IN_MILLIS;
    }

    @Override
    public synchronized boolean init() {
        if (this.running) {
            return true;
        }

        LOG.info("Launching {} on {}...", this, this.balancerAddress);

        this.bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(this.bossPool, this.workerPool));
        final ClientSocketChannelFactory clientSocketChannelFactory =
                new NioClientSocketChannelFactory(this.bossPool, this.workerPool);
        this.bootstrap.setOption("child.tcpNoDelay", true);
        this.allChannels = new DefaultChannelGroup(this.id + "-all-channels-" + Integer.toHexString(this.hashCode()));
        this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new TcpTunnelInboundHandler(id, allChannels, clientSocketChannelFactory,
                        balancingStrategy, timeoutInMillis));
            }
        });

        if (this.balancerAddress.isAnyHost()) {
            this.acceptor = this.bootstrap.bind(new InetSocketAddress(this.balancerAddress.getPort()));
        } else {
            this.acceptor = this.bootstrap.bind(new InetSocketAddress(this.balancerAddress.getHost(),
                    this.balancerAddress.getPort()));
        }

        boolean bound = this.acceptor.isBound();

        if (!bound) {
            LOG.error("Failed to bound {} to {}.", this, this.balancerAddress);
        } else {
            LOG.info("Successfully bound {} to {}.", this, this.balancerAddress);
        }

        this.running = true;
        return true;
    }


    @Override
    public void terminate() {
        if (!this.running) {
            return;
        }

        LOG.info("Shutting down {}...", this.id);
        this.running = false;

        this.allChannels.close().awaitUninterruptibly();
        this.acceptor.close().awaitUninterruptibly();

        if (this.internalPools) {
            this.bootstrap.releaseExternalResources();
        }

        LOG.info("{} stopped.", this.id);
    }

    @Override
    public HostPort getBalancerAddress() {
        return this.balancerAddress;
    }

    @Override
    public List<HostPort> getTargetAddresses() {
        return this.balancingStrategy.getTargetAddresses();
    }

    @Override
    public BalancingStrategy getBalancingStrategy() {
        return this.balancingStrategy;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode());
    }
}
