package com.jaldishop.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita el soporte de tareas programadas (@Scheduled) en el contenedor de Spring.
 * Los schedulers individuales se mantienen controlados mediante @ConditionalOnProperty
 * y permanecen deshabilitados por defecto.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
