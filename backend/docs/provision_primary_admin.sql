-- ==============================================================================
-- JaldiShop — Procedimiento Manual Seguro de Asignación de Administrador Principal
-- Archivo: backend/docs/provision_primary_admin.sql
--
-- INSTRUCCIONES DE USO:
-- 1. Reemplazar 'admin.demo@jaldishop.com' con el correo de la cuenta que se desea designar.
-- 2. Ejecutar este bloque DO en la base de datos (psql / Neon console).
-- 3. El procedimiento es transaccional, idempotente y valida identidad, rol y estado.
-- ==============================================================================

DO $$
DECLARE
    -- Ingrese aquí el correo del administrador a designar:
    v_target_email TEXT := 'admin.demo@jaldishop.com';
    v_current_primary_email TEXT;
    v_target_user_id UUID;
    v_target_status TEXT;
    v_admin_role_id SMALLINT;
    v_has_admin_role BOOLEAN;
BEGIN
    -- 1. Verificar si ya existe un administrador principal asignado
    SELECT email INTO v_current_primary_email
    FROM users
    WHERE is_primary_admin = true
    LIMIT 1;

    IF v_current_primary_email IS NOT NULL THEN
        IF LOWER(v_current_primary_email) = LOWER(v_target_email) THEN
            RAISE NOTICE 'El usuario % ya es el administrador principal actualmente. Sin cambios necesarios.', v_current_primary_email;
            RETURN;
        ELSE
            RAISE EXCEPTION 'Operación abortada: Ya existe un Administrador Principal registrado con el correo %. Transfiera o revoque esa cuenta antes de asignar una nueva.', v_current_primary_email;
        END IF;
    END IF;

    -- 2. Localizar la cuenta objetivo y validar su existencia
    SELECT id, status INTO v_target_user_id, v_target_status
    FROM users
    WHERE LOWER(email) = LOWER(v_target_email);

    IF v_target_user_id IS NULL THEN
        RAISE EXCEPTION 'Operación abortada: No existe ningún usuario registrado con el correo %.', v_target_email;
    END IF;

    -- 3. Validar estado de la cuenta (debe estar ACTIVE)
    IF v_target_status != 'ACTIVE' THEN
        RAISE EXCEPTION 'Operación abortada: El usuario % se encuentra en estado %, pero se requiere ACTIVE.', v_target_email, v_target_status;
    END IF;

    -- 4. Validar que la cuenta cuente con el rol ADMIN
    SELECT id INTO v_admin_role_id FROM roles WHERE name = 'ADMIN';
    IF v_admin_role_id IS NULL THEN
        RAISE EXCEPTION 'Inconsistencia en el sistema: El rol ADMIN no existe en la tabla roles.';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM user_roles WHERE user_id = v_target_user_id AND role_id = v_admin_role_id
    ) INTO v_has_admin_role;

    IF NOT v_has_admin_role THEN
        RAISE EXCEPTION 'Operación abortada: El usuario % no tiene asignado el rol ADMIN.', v_target_email;
    END IF;

    -- 5. Designar como Administrador Principal e invalidar sesiones anteriores
    UPDATE users
    SET is_primary_admin = true,
        credentials_version = credentials_version + 1,
        updated_at = NOW()
    WHERE id = v_target_user_id;

    RAISE NOTICE 'ÉXITO: El usuario % (ID: %) fue designado como Administrador Principal de JaldiShop.', v_target_email, v_target_user_id;
END $$;
