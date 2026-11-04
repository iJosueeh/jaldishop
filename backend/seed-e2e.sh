#!/usr/bin/env bash
# ==============================================================================
# JaldiShop — Script Reproducible de Carga de Seed E2E (Desarrollo y Demo)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_FILE="${SCRIPT_DIR}/src/main/resources/db/seed/seed_e2e_demo_data.sql"

if [ ! -f "${SEED_FILE}" ]; then
    echo "ERROR: Archivo de seed no encontrado en: ${SEED_FILE}" >&2
    exit 1
fi

# 1. Variables de conexión (exige parámetros explícitos, sin valores predeterminados)
DB_URL="${DB_URL:-}"
DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-}"
DB_NAME="${DB_NAME:-}"
DB_USERNAME="${DB_USERNAME:-${DB_USER:-}}"
DB_PASSWORD="${DB_PASSWORD:-${PGPASSWORD:-}}"

if [ -z "${DB_URL}" ] && { [ -z "${DB_HOST}" ] || [ -z "${DB_PORT}" ] || [ -z "${DB_NAME}" ]; }; then
    echo "ERROR: Se requieren parámetros explícitos de conexión a base de datos de pruebas." >&2
    echo "No se permiten conexiones predeterminadas por motivos de seguridad." >&2
    echo "Uso:" >&2
    echo "  DB_URL=\"jdbc:postgresql://127.0.0.1:<puerto>/jaldishop_test\" DB_USERNAME=<usuario> DB_PASSWORD=<clave> $0" >&2
    echo "  o bien:" >&2
    echo "  DB_HOST=127.0.0.1 DB_PORT=<puerto> DB_NAME=jaldishop_test DB_USER=<usuario> PGPASSWORD=<clave> $0" >&2
    exit 1
fi

if [ -z "${DB_USERNAME}" ] || [ -z "${DB_PASSWORD}" ]; then
    echo "ERROR: Debe proporcionar usuario y contraseña explícitos (DB_USERNAME/DB_USER y DB_PASSWORD/PGPASSWORD)." >&2
    exit 1
fi

# 2. Parsear DB_URL si fue provista
if [ -n "${DB_URL}" ]; then
    if [[ ! "${DB_URL}" =~ ^jdbc:postgresql://([^:/]+):([0-9]+)/([^?]+) ]]; then
        echo "ERROR: Formato de DB_URL inválido. Debe ser: jdbc:postgresql://<host>:<puerto>/<database>" >&2
        exit 1
    fi
    PARSED_HOST="${BASH_REMATCH[1]}"
    PARSED_PORT="${BASH_REMATCH[2]}"
    PARSED_DB="${BASH_REMATCH[3]}"
    DB_HOST="${DB_HOST:-$PARSED_HOST}"
    DB_PORT="${DB_PORT:-$PARSED_PORT}"
    DB_NAME="${DB_NAME:-$PARSED_DB}"
fi

# 3. Validación estricta de seguridad: únicamente destinos locales de prueba autorizados
# Rechaza hosts externos
if [[ "${DB_HOST}" != "127.0.0.1" && "${DB_HOST}" != "localhost" && "${DB_HOST}" != "::1" && "${DB_HOST}" != "[::1]" ]]; then
    echo "ERROR DE SEGURIDAD: Host '${DB_HOST}' no autorizado." >&2
    echo "El seed E2E está restringido exclusivamente a interfaces de loopback locales (127.0.0.1, localhost, ::1)." >&2
    exit 1
fi

if [[ "${DB_URL}" =~ (neon\.tech|amazonaws\.com|supabase\.co) ]]; then
    echo "ERROR DE SEGURIDAD: Conexión externa remota rechazada: ${DB_URL}" >&2
    exit 1
fi

# Rechaza entorno demo o de producción
if [[ "${DB_NAME}" =~ (demo|prod|live|staging) ]] || [ "${DB_NAME}" = "jaldishop" ]; then
    echo "ERROR DE SEGURIDAD: Entorno demo o productivo rechazado ('${DB_NAME}')." >&2
    echo "El seed E2E no debe aplicarse a bases de datos demo o productivas." >&2
    exit 1
fi

# Autorizar únicamente bases de datos de prueba
if [[ "${DB_NAME}" != "jaldishop_test" && ! "${DB_NAME}" =~ _test$ ]]; then
    echo "ERROR: Base de datos destino '${DB_NAME}' no autorizada." >&2
    echo "El seed E2E solo puede ejecutarse contra una base de datos local de prueba (ej: jaldishop_test)." >&2
    exit 1
fi

echo "======================================================================="
echo "JaldiShop — Aplicando Seed E2E reproducible"
echo "Destino: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "Usuario: ${DB_USERNAME}"
echo "======================================================================="

export PGPASSWORD="${DB_PASSWORD}"

# 4. Ejecutar seed con psql
if ! command -v psql &> /dev/null; then
    echo "ERROR: 'psql' no está instalado en el sistema." >&2
    exit 1
fi

psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${SEED_FILE}"

echo ""
echo "======================================================================="
echo " Seed E2E aplicado exitosamente en ${DB_NAME}."
echo " Entidades listas para demostración y pruebas E2E:"
echo "   • Usuarios: Admin, Comerciantes (Carlos, Lucía) y Clientes (Valeria, Juan)"
echo "   • Contraseña universal demo: Password123!"
echo "   • Catálogo: Tiendas, Categorías, Productos, Variantes y Stock inicial"
echo "   • Franjas de capacidad activas y slots reservables"
echo "   • Carrito activo para cliente.demo@jaldishop.com"
echo "   • Pedidos en estados COMPLETED e IN_PREPARATION con historial"
echo "   • Pagos y auditoría de incidencias técnicas simuladas"
echo "   • Notificaciones deduplicadas en bandeja"
echo "======================================================================="
