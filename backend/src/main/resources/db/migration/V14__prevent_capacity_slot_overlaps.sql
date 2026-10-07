-- ==============================================================================
-- JaldiShop Migration V14: Prevent Capacity Slot Overlaps
-- Version: V14__prevent_capacity_slot_overlaps.sql
-- Description:
--   1. Enable btree_gist extension for PostgreSQL GiST scalar indexing.
--   2. Define IMMUTABLE helper function fn_capacity_time_to_int8range mapping [start, end)
--      into seconds since midnight (supporting full-day slots as [0, 86400)).
--   3. Validate that diagnosed secondary test slot matches expected state and deactivate it.
--   4. Perform strict general pre-validation for capacity_configurations & capacity_exceptions.
--   5. Add EXCLUDE USING gist constraints preventing overlapping ACTIVE slots.
-- ==============================================================================

-- 1. Enable GiST btree extension
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 2. Define strictly IMMUTABLE function to convert TIME range to int8range in seconds
CREATE OR REPLACE FUNCTION fn_capacity_time_to_int8range(p_start_time TIME, p_end_time TIME)
RETURNS int8range AS $$
BEGIN
    IF p_start_time IS NULL AND p_end_time IS NULL THEN
        -- Dia completo (00:00:00 a 24:00:00 = 86400 segundos)
        RETURN int8range(0::bigint, 86400::bigint, '[)');
    ELSIF p_start_time IS NOT NULL AND p_end_time IS NOT NULL THEN
        RETURN int8range(
            (EXTRACT(HOUR FROM p_start_time) * 3600 + EXTRACT(MINUTE FROM p_start_time) * 60 + EXTRACT(SECOND FROM p_start_time))::bigint,
            (EXTRACT(HOUR FROM p_end_time) * 3600 + EXTRACT(MINUTE FROM p_end_time) * 60 + EXTRACT(SECOND FROM p_end_time))::bigint,
            '[)'
        );
    ELSE
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 3. Targeted Remediation: Verify diagnosed secondary test slot and deactivate it
DO $$
DECLARE
    v_matches INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO v_matches
    FROM capacity_configurations
    WHERE id = 'daf162d3-8f57-4c69-b587-1b81d6364524'
      AND store_id = '80acf0ba-5a79-4888-b00d-b5aef8fcf9e5'
      AND day_of_week = 6
      AND start_time = TIME '14:00:00'
      AND end_time = TIME '16:00:00'
      AND max_capacity = 8
      AND status = 'ACTIVE';

    IF v_matches <> 1 THEN
        RAISE EXCEPTION 'El slot secundario esperado para saneamiento no coincide con el estado diagnosticado. Abortando V14.';
    END IF;
END $$;

UPDATE capacity_configurations
SET status = 'INACTIVE'
WHERE id = 'daf162d3-8f57-4c69-b587-1b81d6364524';

-- 4. General Pre-Validation: Detect any remaining overlaps among ACTIVE slots before applying EXCLUDE
DO $$
DECLARE
    v_config_overlaps INTEGER;
    v_exc_overlaps    INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_config_overlaps
    FROM capacity_configurations c1
    JOIN capacity_configurations c2 ON c1.store_id = c2.store_id
        AND c1.day_of_week = c2.day_of_week
        AND c1.id < c2.id
        AND c1.status = 'ACTIVE'
        AND c2.status = 'ACTIVE'
        AND fn_capacity_time_to_int8range(c1.start_time, c1.end_time) && fn_capacity_time_to_int8range(c2.start_time, c2.end_time);

    IF v_config_overlaps > 0 THEN
        RAISE EXCEPTION 'Existen % configuraciones de capacidad activas solapadas tras saneamiento. Se requiere resolucion manual previa.', v_config_overlaps;
    END IF;

    SELECT COUNT(*) INTO v_exc_overlaps
    FROM capacity_exceptions e1
    JOIN capacity_exceptions e2 ON e1.store_id = e2.store_id
        AND e1.service_date = e2.service_date
        AND e1.id < e2.id
        AND e1.status = 'ACTIVE'
        AND e2.status = 'ACTIVE'
        AND fn_capacity_time_to_int8range(e1.start_time, e1.end_time) && fn_capacity_time_to_int8range(e2.start_time, e2.end_time);

    IF v_exc_overlaps > 0 THEN
        RAISE EXCEPTION 'Existen % excepciones de capacidad activas solapadas. Se requiere resolucion manual previa.', v_exc_overlaps;
    END IF;
END $$;

-- 5. Exclude constraint for capacity_configurations (store_id, day_of_week, time_range)
ALTER TABLE capacity_configurations
    ADD CONSTRAINT ex_capacity_configurations_no_overlap
    EXCLUDE USING gist (
        store_id WITH =,
        day_of_week WITH =,
        (fn_capacity_time_to_int8range(start_time, end_time)) WITH &&
    )
    WHERE (status = 'ACTIVE');

-- 6. Exclude constraint for capacity_exceptions (store_id, service_date, time_range)
ALTER TABLE capacity_exceptions
    ADD CONSTRAINT ex_capacity_exceptions_no_overlap
    EXCLUDE USING gist (
        store_id WITH =,
        service_date WITH =,
        (fn_capacity_time_to_int8range(start_time, end_time)) WITH &&
    )
    WHERE (status = 'ACTIVE');
