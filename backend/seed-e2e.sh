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

# 1. Resolver variables de entorno
DB_URL="${DB_URL:-}"
DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"

# Cargar .env opcional si existe y faltan variables
if [ -f "${SCRIPT_DIR}/.env" ] && [ -z "${DB_URL}" ]; then
    echo "[INFO] Leyendo variables de conexión desde .env..."
    # shellcheck disable=SC1091
    source <(grep -E '^(DB_URL|DB_USERNAME|DB_PASSWORD)=' "${SCRIPT_DIR}/.env" || true)
fi

DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/jaldishop_test}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgrespassword}"

# 2. Protección estricta contra ejecución accidental en entornos de producción o demo alojada
if [[ "${DB_URL}" =~ (neon\.tech|amazonaws\.com|prod|live) ]] && [ "${ALLOW_PRODUCTION_SEED:-false}" != "true" ]; then
    echo "=======================================================================" >&2
    echo "ERROR DE SEGURIDAD: La URL de base de datos apunta a un entorno externo:" >&2
    echo "  ${DB_URL}" >&2
    echo "El seed E2E está restringido a entornos locales de desarrollo y pruebas." >&2
    echo "Para forzar deliberadamente, exporta ALLOW_PRODUCTION_SEED=true." >&2
    echo "=======================================================================" >&2
    exit 1
fi

echo "======================================================================="
echo "JaldiShop — Aplicando Seed E2E reproducible"
echo "URL destino: ${DB_URL}"
echo "Usuario:     ${DB_USERNAME}"
echo "======================================================================="

# 3. Parsear JDBC URL para psql
# Formato: jdbc:postgresql://host:port/database?params
CLEAN_URL="${DB_URL#jdbc:postgresql://}"
CLEAN_URL="${CLEAN_URL%%\?*}"

HOST_PORT="${CLEAN_URL%%/*}"
DB_NAME="${CLEAN_URL##*/}"

if [[ "${HOST_PORT}" == *:* ]]; then
    DB_HOST="${HOST_PORT%%:*}"
    DB_PORT="${HOST_PORT##*:}"
else
    DB_HOST="${HOST_PORT}"
    DB_PORT="5432"
fi

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
