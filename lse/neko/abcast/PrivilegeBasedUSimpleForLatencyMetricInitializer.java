package lse.neko.abcast;


public class PrivilegeBasedUSimpleForLatencyMetricInitializer
    extends PrivilegeBasedForLatencyMetricInitializer
{
    protected ABCastInitializer createDelegate() {
        return new PrivilegeBasedUSimpleInitializer();
    }
}
