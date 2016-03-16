package balancing;

import utils.HostPort;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class RoundRobinBalancingStrategy implements BalancingStrategy {
    private final List<HostPort> targets;
    private final AtomicInteger currentTarget;

    public RoundRobinBalancingStrategy(List<HostPort> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("Target list can not be null or empty");
        }

        this.targets = new CopyOnWriteArrayList<HostPort>(targets);
        this.currentTarget = new AtomicInteger(0);
    }

    // It seems that the paramters originHost and originPort are never used
    @Override
    public HostPort selectTarget(String originHost, int originPort) {
        int currentTarget;
        synchronized (this.currentTarget) {
            currentTarget = this.currentTarget.getAndIncrement();
            if (currentTarget >= this.targets.size()) {
                currentTarget = 0;
                this.currentTarget.set(0);
            }
        }

        return this.targets.get(currentTarget);
    }

    @Override
    public List<HostPort> getTargetAddresses() {
        return Collections.unmodifiableList(this.targets);
    }
}
