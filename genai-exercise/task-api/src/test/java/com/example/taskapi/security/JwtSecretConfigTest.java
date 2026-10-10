package com.example.taskapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

/**
 * Review finding #1: the signing secret must never fall back to a value committed to the repo.
 * Loads the real application.yml (and profile files) and binds {@link JwtProperties}.
 */
class JwtSecretConfigTest {

    private static final String SECRET = "a-test-secret-that-is-at-least-32-bytes-long";

    // OS environment variables are removed so an APP_JWT_SECRET on the developer's machine can't mask
    // the check; the tests supply it explicitly where needed.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtPropertiesOnly.class);

    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesOnly {
    }

    @Test
    void startupFailsWhenNoSecretIsConfigured() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void secretComesFromTheEnvironmentVariable() {
        runner.withPropertyValues("APP_JWT_SECRET=" + SECRET)
                .run(context -> assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(SECRET));
    }

    @Test
    void devProfileProvidesALocalOnlySecret() {
        runner.withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context.getBean(JwtProperties.class).secret()).isNotBlank());
    }
}
