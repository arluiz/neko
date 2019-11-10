package lse.neko.abcast;


public class PrivilegeBasedUForLatencyMetricInitializer
    extends PrivilegeBasedForLatencyMetricInitializer
{
    protected ABCastInitializer createDelegate() {
        return new PrivilegeBasedUInitializer();
    }
}
