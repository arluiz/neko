package lse.neko.abcast;


public class PrivilegeBasedNUForLatencyMetricInitializer
    extends PrivilegeBasedForLatencyMetricInitializer
{
    protected ABCastInitializer createDelegate() {
        return new PrivilegeBasedNUInitializer();
    }
}
