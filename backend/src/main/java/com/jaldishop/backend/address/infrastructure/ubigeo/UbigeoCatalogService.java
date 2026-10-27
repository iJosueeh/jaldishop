package com.jaldishop.backend.address.infrastructure.ubigeo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UbigeoCatalogService {

    private static final Logger log = LoggerFactory.getLogger(UbigeoCatalogService.class);

    private final ObjectMapper objectMapper;
    private final Map<String, UbigeoEntry> ubigeoByCode = new ConcurrentHashMap<>();

    public UbigeoCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private record RawEntry(String c, String d, String p, String di) {}

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data/ubigeo-peru.json");
            try (InputStream is = resource.getInputStream()) {
                List<RawEntry> rawEntries = objectMapper.readValue(is, new TypeReference<>() {});
                for (RawEntry raw : rawEntries) {
                    if (raw.c() != null && !raw.c().isBlank()) {
                        ubigeoByCode.put(raw.c().trim(), new UbigeoEntry(
                                raw.c().trim(),
                                raw.d() != null ? raw.d().trim() : "",
                                raw.p() != null ? raw.p().trim() : "",
                                raw.di() != null ? raw.di().trim() : ""
                        ));
                    }
                }
            }
            log.info("Catálogo oficial de UBIGEO inicializado exitosamente con {} registros de INEI.", ubigeoByCode.size());
        } catch (Exception e) {
            log.error("No se pudo cargar el archivo data/ubigeo-peru.json", e);
            throw new IllegalStateException("Error al inicializar catálogo oficial de UBIGEO.", e);
        }
    }

    public boolean isValidCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return ubigeoByCode.containsKey(code.trim());
    }

    public boolean isValidHierarchy(String code, String department, String province, String district) {
        if (!isValidCode(code)) {
            return false;
        }
        UbigeoEntry entry = ubigeoByCode.get(code.trim());
        if (entry == null) {
            return false;
        }

        String normReqDept = normalizeText(department);
        String normReqProv = normalizeText(province);
        String normReqDist = normalizeText(district);

        String normEntryDept = normalizeText(entry.department());
        String normEntryProv = normalizeText(entry.province());
        String normEntryDist = normalizeText(entry.district());

        return normReqDept.equals(normEntryDept)
                && normReqProv.equals(normEntryProv)
                && normReqDist.equals(normEntryDist);
    }

    public Optional<UbigeoEntry> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ubigeoByCode.get(code.trim()));
    }

    public int size() {
        return ubigeoByCode.size();
    }

    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
