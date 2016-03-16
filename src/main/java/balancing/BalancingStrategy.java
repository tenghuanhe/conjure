package balancing;

import utils.HostPort;

import java.util.List;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public interface BalancingStrategy {
    HostPort selectTarget(String originHost, int originPort);

    List<HostPort> getTargetAddresses();
}
