package balancing;

import utils.HostPort;

import java.util.List;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public interface LoadBalancer {
    boolean init();

    void terminate();

    HostPort getBalancerAddress();

    List<HostPort> getTargetAddresses();

    BalancingStrategy getBalancingStrategy();
}
