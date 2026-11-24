package com.jaldishop.backend.identity.infrastructure.configuration;

import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.domain.repository.PlatformSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class PlatformSettingsProviderImpl implements PlatformSettingsProvider {

    private static final Logger log = LoggerFactory.getLogger(PlatformSettingsProviderImpl.class);

    private final PlatformSettingsRepository repository;
    private final long cacheTtlMs;
    private final AtomicReference<PlatformSettings> cache = new AtomicReference<>(PlatformSettings.defaultSettings());
    private volatile long lastSyncTimestamp = 0L;

    @Autowired
    public PlatformSettingsProviderImpl(
            PlatformSettingsRepository repository,
            @Value("${app.platform-settings.cache-ttl-ms:5000}") long cacheTtlMs
    ) {
        this.repository = repository;
        this.cacheTtlMs = Math.max(0L, cacheTtlMs);
    }

    public PlatformSettingsProviderImpl(PlatformSettingsRepository repository) {
        this(repository, 5000L);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadInitialSettings() {
        try {
            repository.findSingleton().ifPresentOrElse(
                    settings -> {
                        cache.set(settings);
                        lastSyncTimestamp = System.currentTimeMillis();
                        log.info("Platform settings cargadas en memoria exitosamente (maintenanceMode={})", settings.maintenanceMode());
                    },
                    () -> {
                        PlatformSettings defaults = PlatformSettings.defaultSettings();
                        PlatformSettings saved = repository.save(defaults);
                        cache.set(saved);
                        lastSyncTimestamp = System.currentTimeMillis();
                        log.info("Platform settings inicializadas con valores por defecto.");
                    }
            );
        } catch (Exception e) {
            log.warn("No se pudieron cargar platform settings al iniciar la app (fallback a default): {}", e.getMessage());
        }
    }

    @Override
    public PlatformSettings getSettings() {
        long now = System.currentTimeMillis();
        if (now - lastSyncTimestamp > cacheTtlMs) {
            refreshFromDbIfExpired(now);
        }
        return cache.get();
    }

    private synchronized void refreshFromDbIfExpired(long checkTime) {
        if (checkTime - lastSyncTimestamp <= cacheTtlMs) {
            return;
        }
        try {
            repository.findSingleton().ifPresent(settings -> {
                cache.set(settings);
                lastSyncTimestamp = System.currentTimeMillis();
            });
        } catch (Exception e) {
            log.warn("Fallo al refrescar caché de platform settings (se usa en memoria): {}", e.getMessage());
        }
    }

    @Override
    public void updateSettingsCache(PlatformSettings settings) {
        cache.set(settings);
        lastSyncTimestamp = System.currentTimeMillis();
    }
}
