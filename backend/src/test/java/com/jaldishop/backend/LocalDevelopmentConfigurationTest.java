package com.jaldishop.backend;

import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.media.infrastructure.cloudinary.CloudinaryConfig;
import com.jaldishop.backend.media.infrastructure.cloudinary.CloudinaryProperties;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationScheduler;
import com.jaldishop.backend.payment.application.refund.PaymentRefundScheduler;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoRestClientAdapter;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.net.InetAddress;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDevelopmentConfigurationTest {

    @Test
    void templateBuildsLocalConfigurationWithoutDatabaseOrExternalCalls() throws Exception {
        Properties template = new Properties();
        Dotenv.configure().directory(".").filename(".env.template").load().entries()
                .forEach(entry -> template.setProperty(entry.getKey(), entry.getValue()));
        var application = new ResourcePropertySource("classpath:application.properties");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(JwtService.class, MercadoPagoProperties.class,
                        CloudinaryProperties.class, CloudinaryConfig.class,
                        PaymentReconciliationScheduler.class, PaymentRefundScheduler.class)
                .withInitializer(context -> {
                    var sources = context.getEnvironment().getPropertySources();
                    sources.addFirst(application);
                    // main() exposes dotenv values as system properties before Spring starts.
                    sources.addFirst(new PropertiesPropertySource("local-template", template));
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://127.0.0.1:5433/jaldishop_dev");
                    assertThat(Binder.get(environment).bind("server.address", InetAddress.class).get()
                            .isLoopbackAddress()).isTrue();
                    assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8080);
                    assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("app.payment.reconciliation.orders-enabled", Boolean.class))
                            .isFalse();
                    assertThat(context).doesNotHaveBean(PaymentReconciliationScheduler.class);
                    assertThat(context).doesNotHaveBean(PaymentRefundScheduler.class);
                    assertThat(context).hasSingleBean(com.cloudinary.Cloudinary.class);

                    var jwt = context.getBean(JwtService.class);
                    UUID userId = UUID.randomUUID();
                    assertThat(jwt.parseToken(jwt.generateToken(userId, Set.of("CUSTOMER"))).getSubject())
                            .isEqualTo(userId.toString());

                    var payments = context.getBean(MercadoPagoProperties.class);
                    assertThat(payments.getBaseUrl()).isEqualTo("http://127.0.0.1:9");
                    assertThat(payments.getCheckoutMode()).isEqualTo(MercadoPagoProperties.CheckoutIntegrationMode.ORDERS);
                    assertThat(payments.getAccessToken()).isEmpty();
                    assertThat(payments.getWebhookSecret()).isEmpty();
                    assertThat(payments.getNotificationUrl()).isEmpty();
                    assertThat(payments.getBackUrlSuccess()).isEmpty();
                    assertThat(payments.getBackUrlFailure()).isEmpty();
                    assertThat(payments.getBackUrlPending()).isEmpty();
                    var client = new MercadoPagoRestClientAdapter(payments);
                    assertThatThrownBy(() -> client.queryPayment("local-test"))
                            .isInstanceOf(MercadoPagoException.class)
                            .hasMessageContaining("access token");
                });
    }
}
