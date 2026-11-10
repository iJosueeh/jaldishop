package com.jaldishop.backend.identity.infrastructure.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class PasswordRecoveryConfiguration {
    @Bean public Clock passwordRecoveryClock() { return Clock.systemUTC(); }
    @Bean public TransactionTemplate passwordRecoveryTransaction(PlatformTransactionManager manager) {
        return new TransactionTemplate(manager);
    }
    @Bean public ThreadPoolTaskExecutor passwordRecoveryExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); executor.setMaxPoolSize(1); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-recovery-");
        return executor;
    }
}
