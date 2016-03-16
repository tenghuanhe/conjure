import balancing.DefaultLoadBalancer;
import balancing.RoundRobinBalancingStrategy;
import utils.HostPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class Conjure {

    public static void main(String[] args) {
        CommandLineOptions options = CommandLineOptions.parseCommandLineOptions(args);
        if (options.getBalancerAddress() == null) {
            System.err.println("Balancer address required");
            printUsage();
        }

        if (options.getDestinationAddresses() == null || options.getDestinationAddresses().isEmpty()) {
            System.err.println("At least one destination required");
            printUsage();
        }

        RoundRobinBalancingStrategy strategy = new RoundRobinBalancingStrategy(options.getDestinationAddresses());
        final DefaultLoadBalancer loadBalancer =
                new DefaultLoadBalancer("defalutBalancer", options.getBalancerAddress(), strategy);

        if (!loadBalancer.init()) {
            System.err.println("Failed to launch LoadBalancer with options: " + options);
            return;
        }

        System.out.println("The load balancer is running...");

        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                loadBalancer.terminate();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static void printUsage() {
        System.err.println("Usage:");
        System.err.println(Conjure.class.getSimpleName());
        System.err.println("    -B<ipv4>:<port>: Balancer address (mandatory) - Examples: '-B10.0.0.1:80' or '-B*:80'");
        System.err.println("    -T<ipv4>:<port>: Target address (at least one provided)");
        System.err.println("    -S<string>: Target selection strategy ('rr' = Round Robin, " +
                "'fair' = Fair (favor equal number of connections per target) balance)");
        System.err.println("A more concrete example: -b*:9487 -t127.0.0.1:50051 -t127.0.0.1:50052 -t127.0.0.1:50053 -sRR");
    }

    private static class CommandLineOptions {
        private HostPort balancerAddress;
        private List<HostPort> destinationAddresses;
        private String selectionStrategy;

        private CommandLineOptions() {
        }

        public static CommandLineOptions parseCommandLineOptions(String[] args) {
            CommandLineOptions options = new CommandLineOptions();
            HostPort address;

            for (String arg : args) {
                if (arg.length() < 3) {
                    System.err.println("Invalid argument ignored: " + arg);
                    continue;
                }
                switch (arg.charAt(1)) {
                    case 'b':
                    case 'B':
                        address = HostPort.decode(arg.substring(2));
                        if (address == null) {
                            continue;
                        }
                        options.balancerAddress = address;
                        break;
                    case 't':
                    case 'T':
                        address = HostPort.decode(arg.substring(2));
                        if (address == null) {
                            continue;
                        }
                        if (options.destinationAddresses == null) {
                            options.destinationAddresses = new ArrayList<HostPort>();
                        }
                        options.destinationAddresses.add(address);
                        break;
                    case 's':
                    case 'S':
                        options.selectionStrategy = arg.substring(2);
                        break;
                    default:
                        System.err.println("Unknown command line option ignored: " + arg);
                }
            }
            return options;
        }

        public HostPort getBalancerAddress() {
            return balancerAddress;
        }

        public List<HostPort> getDestinationAddresses() {
            return destinationAddresses;
        }

        public String getSelectionStrategy() {
            return selectionStrategy;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("CommandLineOptions{")
                    .append("balancerAddress=").append(balancerAddress)
                    .append(", destinationAddress=").append(destinationAddresses)
                    .append(", selectionStrategy='").append(selectionStrategy).append("'")
                    .append("}").toString();
        }
    }
}
