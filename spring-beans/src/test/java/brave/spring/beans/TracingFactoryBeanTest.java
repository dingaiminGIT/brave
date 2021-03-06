package brave.spring.beans;

import brave.Clock;
import brave.Tracing;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.StrictCurrentTraceContext;
import brave.sampler.Sampler;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import zipkin2.Endpoint;
import zipkin2.reporter.Reporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TracingFactoryBeanTest {
  public static Clock CLOCK = mock(Clock.class);

  XmlBeans context;

  @After public void close() {
    if (context != null) context.close();
  }

  @Test public void autoCloses() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\"/>\n"
    );
    context.refresh();

    assertThat(Tracing.current()).isNotNull();

    context.close();

    assertThat(Tracing.current()).isNull();

    context = null;
  }

  @Test public void localServiceName() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"localServiceName\" value=\"brave-webmvc-example\"/>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.recorder.spanMap.localEndpoint")
        .extracting("serviceName")
        .containsExactly("brave-webmvc-example");
  }

  @Test public void localEndpoint() {
    context = new XmlBeans(""
        + "<bean id=\"localEndpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n"
        + "  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n"
        + "  <property name=\"ip\" value=\"1.2.3.4\"/>\n"
        + "  <property name=\"port\" value=\"8080\"/>\n"
        + "</bean>"
        , ""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"localEndpoint\" ref=\"localEndpoint\"/>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.recorder.spanMap.localEndpoint")
        .containsExactly(Endpoint.newBuilder()
            .serviceName("brave-webmvc-example")
            .ip("1.2.3.4")
            .port(8080).build());
  }

  @Test public void spanReporter() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"spanReporter\">\n"
        + "    <util:constant static-field=\"zipkin2.reporter.Reporter.CONSOLE\"/>\n"
        + "  </property>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.recorder.reporter")
        .containsExactly(Reporter.CONSOLE);
  }

  @Test public void clock() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"clock\">\n"
        + "    <util:constant static-field=\"" + getClass().getName() + ".CLOCK\"/>\n"
        + "  </property>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.clock")
        .containsExactly(CLOCK);
  }

  @Test public void sampler() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"sampler\">\n"
        + "    <util:constant static-field=\"brave.sampler.Sampler.NEVER_SAMPLE\"/>\n"
        + "  </property>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.sampler")
        .containsExactly(Sampler.NEVER_SAMPLE);
  }

  @Test public void currentTraceContext() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"currentTraceContext\">\n"
        + "    <bean class=\"brave.propagation.StrictCurrentTraceContext\"/>\n"
        + "  </property>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.currentTraceContext")
        .allMatch(o -> o instanceof StrictCurrentTraceContext);
  }

  @Test public void propagationFactory() {
    context = new XmlBeans(""
        + "<bean id=\"propagationFactory\" class=\"brave.propagation.ExtraFieldPropagation\" factory-method=\"newFactory\">\n"
        + "  <constructor-arg index=\"0\">\n"
        + "    <util:constant static-field=\"brave.propagation.B3Propagation.FACTORY\"/>\n"
        + "  </constructor-arg>\n"
        + "  <constructor-arg index=\"1\">\n"
        + "    <list>\n"
        + "      <value>x-vcap-request-id</value>\n"
        + "      <value>x-amzn-trace-id</value>\n"
        + "    </list>"
        + "  </constructor-arg>\n"
        + "</bean>", ""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"propagationFactory\" ref=\"propagationFactory\"/>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class).propagation())
        .isInstanceOf(ExtraFieldPropagation.class)
        .extracting("nameToKey")
        .allSatisfy(m -> assertThat((Map) m)
            .containsOnlyKeys("x-vcap-request-id", "x-amzn-trace-id"));
  }

  @Test public void traceId128Bit() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"traceId128Bit\" value=\"true\"/>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.traceId128Bit")
        .containsExactly(true);
  }

  @Test public void supportsJoin() {
    context = new XmlBeans(""
        + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n"
        + "  <property name=\"supportsJoin\" value=\"true\"/>\n"
        + "</bean>"
    );
    context.refresh();

    assertThat(context.getBean(Tracing.class))
        .extracting("tracer.supportsJoin")
        .containsExactly(true);
  }
}
