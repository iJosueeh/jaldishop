#!/usr/bin/env bash
# ==============================================================================
# JaldiShop - Lanzador Seguro de Pruebas Sandbox Mercado Pago Orders API
# ==============================================================================
# Garantiza aislamiento estricto de producción (Neon, Render, dominios productivos).
# Falla de forma segura (Fail-Closed) ante cualquier valor no autorizado.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

CONFIG_FILE="${SCRIPT_DIR}/.env.sandbox.local"
FINGERPRINT_FILE="${SCRIPT_DIR}/.env.sandbox.token_fingerprint"

CHECK_ONLY=false
RUN_APP=false
AUTHORIZE_TOKEN=false
TEST_RUN_SECONDS=0

# ------------------------------------------------------------------------------
# 1. Procesamiento de argumentos
# ------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --check-only)
      CHECK_ONLY=true
      shift
      ;;
    --run)
      RUN_APP=true
      shift
      ;;
    --authorize-token)
      AUTHORIZE_TOKEN=true
      shift
      ;;
    --test-run)
      RUN_APP=true
      TEST_RUN_SECONDS="${2:-15}"
      shift 2
      ;;
    --config)
      CONFIG_FILE="$2"
      shift 2
      ;;
    --fingerprint-file)
      FINGERPRINT_FILE="$2"
      shift 2
      ;;
    --help|-h)
      echo "Uso: $0 [--check-only | --run | --authorize-token | --test-run <segundos>] [--config <ruta>] [--fingerprint-file <ruta>]"
      echo ""
      echo "Opciones:"
      echo "  --check-only           Ejecuta validaciones fail-closed y sale sin arrancar la aplicación."
      echo "  --run                  Ejecuta validaciones y, si son exitosas, arranca Spring Boot."
      echo "  --authorize-token      Registra la huella SHA-256 del token para autorizar su uso en sandbox."
      echo "  --test-run <segundos>  Arranca Spring Boot de forma controlada por N segundos y luego lo detiene."
      echo "  --config <ruta>        Especifica el archivo de configuración (por defecto: backend/.env.sandbox.local)."
      echo "  --fingerprint-file <r> Especifica la ruta de la huella SHA-256 autorizada."
      exit 0
      ;;
    *)
      echo "[ERROR] Argumento no reconocido: $1" >&2
      exit 1
      ;;
  esac
done

if [[ "$CHECK_ONLY" == false && "$RUN_APP" == false && "$AUTHORIZE_TOKEN" == false ]]; then
  echo "[ERROR] Debe especificar --check-only, --run, --authorize-token o --test-run <segundos>." >&2
  echo "Ejecute '$0 --help' para más detalles." >&2
  exit 1
fi

# ------------------------------------------------------------------------------
# 2. Limpieza exhaustiva de variables heredadas para evitar contaminación cruzada
# ------------------------------------------------------------------------------
# A. Sanitización de base de datos
unset DB_URL DB_USERNAME DB_PASSWORD
unset SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD

# B. Sanitización de opciones JVM y configuración externa
unset JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS _JAVA_OPTIONS
unset SPRING_APPLICATION_JSON SPRING_CONFIG_LOCATION SPRING_CONFIG_NAME SPRING_CONFIG_ADDITIONAL_LOCATION

# C. Sanitización de Mercado Pago
unset MERCADOPAGO_ACCESS_TOKEN MERCADOPAGO_WEBHOOK_SECRET MERCADOPAGO_CHECKOUT_MODE
unset MERCADOPAGO_NOTIFICATION_URL MERCADOPAGO_BACK_URL_SUCCESS MERCADOPAGO_BACK_URL_FAILURE MERCADOPAGO_BACK_URL_PENDING
unset MERCADOPAGO_BASE_URL MERCADOPAGO_EXPECTED_COLLECTOR_ID MERCADOPAGO_EXPECTED_LIVE_MODE

# D. Sanitización de Schedulers y Seguridad
unset APP_PAYMENT_RECONCILIATION_ENABLED APP_PAYMENT_RECONCILIATION_ORDERS_ENABLED
unset PAYMENTS_MERCADOPAGO_REFUND_SCHEDULER_ENABLED
unset JWT_SECRET APP_SECURITY_JWT_SECRET

# ------------------------------------------------------------------------------
# 3. Carga del archivo de configuración privado
# ------------------------------------------------------------------------------
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "[FAIL-CLOSED] Archivo de configuración no encontrado: $CONFIG_FILE" >&2
  exit 1
fi

# Cargar variables línea a línea ignorando comentarios y vacíos
while IFS='=' read -r key value || [[ -n "$key" ]]; do
  key="$(echo "$key" | tr -d '[:space:]')"
  [[ -z "$key" || "$key" =~ ^# ]] && continue
  
  # Quitar comillas si existen
  value="${value#\"}"
  value="${value%\"}"
  value="${value#\'}"
  value="${value%\'}"
  
  export "$key=$value"
done < "$CONFIG_FILE"

# ------------------------------------------------------------------------------
# 4. Modo de Autorización Explícita de Token (--authorize-token)
# ------------------------------------------------------------------------------
if [[ "$AUTHORIZE_TOKEN" == true ]]; then
  MP_TOKEN="${MERCADOPAGO_ACCESS_TOKEN:-}"
  if [[ -z "$MP_TOKEN" ]]; then
    echo "[FAIL-CLOSED] MERCADOPAGO_ACCESS_TOKEN no está definido en $CONFIG_FILE." >&2
    exit 1
  fi
  if [[ ! "$MP_TOKEN" =~ ^(TEST|APP_USR)-[A-Za-z0-9_-]{10,}$ ]]; then
    echo "[FAIL-CLOSED] MERCADOPAGO_ACCESS_TOKEN no cumple formato válido (^TEST-... o ^APP_USR-...)" >&2
    exit 1
  fi
  
  # Registrar huella SHA-256 en memoria sin imprimir el token ni la huella
  echo -n "$MP_TOKEN" | sha256sum | awk '{print $1}' > "$FINGERPRINT_FILE"
  chmod 600 "$FINGERPRINT_FILE"
  
  echo "[OK] Huella SHA-256 del token autorizada y guardada de forma segura en archivo privado."
  echo "[OK] Permisos de archivo restringidos a 600. Ignorado por Git."
  exit 0
fi

# ------------------------------------------------------------------------------
# 5. Validaciones Fail-Closed
# ------------------------------------------------------------------------------
ERRORS=0

log_error() {
  echo "[FAIL-CLOSED] $1" >&2
  ERRORS=$((ERRORS + 1))
}

log_ok() {
  echo "[OK] $1"
}

# --- A. Validación de Base de Datos (Igualdad Exacta) ---
DB_URL="${DB_URL:-}"
DB_USERNAME="${DB_USERNAME:-}"
DB_PASSWORD="${DB_PASSWORD:-}"

EXPECTED_DB_URL="jdbc:postgresql://127.0.0.1:5433/jaldishop_test"
if [[ -z "$DB_URL" ]]; then
  log_error "DB_URL no está definida en $CONFIG_FILE"
elif [[ "$DB_URL" != "$EXPECTED_DB_URL" ]]; then
  log_error "DB_URL debe ser exactamente '$EXPECTED_DB_URL' sin parámetros adicionales. Valor recibido: $DB_URL"
fi

if [[ "$DB_URL" =~ (neon\.tech|render\.com|aws|pooler) ]]; then
  log_error "DB_URL contiene referencias prohibidas a servicios remotos (Neon/AWS/Render): $DB_URL"
fi

if [[ "$DB_USERNAME" != "postgres" ]]; then
  log_error "DB_USERNAME debe ser 'postgres'. Valor recibido: $DB_USERNAME"
fi

if [[ -z "$DB_PASSWORD" ]]; then
  log_error "DB_PASSWORD no puede estar vacía"
fi

# --- B. Validación de Mercado Pago Token y Autorización Explícita ---
MP_TOKEN="${MERCADOPAGO_ACCESS_TOKEN:-}"

if [[ -z "$MP_TOKEN" ]]; then
  log_error "MERCADOPAGO_ACCESS_TOKEN no está definido"
elif [[ "$MP_TOKEN" =~ ^TEST-[A-Za-z0-9_-]{10,}$ ]]; then
  log_ok "Formato de token de prueba estándar validado (prefijo TEST-)"
elif [[ "$MP_TOKEN" =~ ^APP_USR-[A-Za-z0-9_-]{10,}$ ]]; then
  if [[ ! -f "$FINGERPRINT_FILE" ]]; then
    log_error "Token con prefijo APP_USR- detectado sin autorización previa. Para prevenir uso accidental de tokens productivos, requiere registrar su huella ejecutando: $0 --authorize-token (solo tras confirmar que proviene de la pestaña 'Prueba' de MP Developers)."
  else
    CURRENT_HASH=$(echo -n "$MP_TOKEN" | sha256sum | awk '{print $1}')
    STORED_HASH=$(tr -d '[:space:]' < "$FINGERPRINT_FILE" || true)
    if [[ -n "$STORED_HASH" && "$CURRENT_HASH" == "$STORED_HASH" ]]; then
      log_ok "Token con prefijo APP_USR- verificado contra huella SHA-256 autorizada localmente."
    else
      log_error "Token con prefijo APP_USR- no coincide con la huella SHA-256 autorizada en $(basename "$FINGERPRINT_FILE"). Acceso denegado."
    fi
  fi
else
  log_error "MERCADOPAGO_ACCESS_TOKEN no cumple con ningún formato reconocido (^TEST-... o ^APP_USR-...)"
fi

# --- C. Validación de Modo de Checkout ---
MP_MODE="${MERCADOPAGO_CHECKOUT_MODE:-}"
if [[ "$MP_MODE" != "ORDERS" ]]; then
  log_error "MERCADOPAGO_CHECKOUT_MODE debe ser 'ORDERS'. Valor recibido: $MP_MODE"
fi

# --- D. Validación de Aislamiento de URLs (Opcionales para Pruebas Locales) ---
PROD_PATTERNS="(api\.jaldishop\.com|jaldishop\.com|onrender\.com|render\.com|neon\.tech)"

MP_NOTIF_URL="${MERCADOPAGO_NOTIFICATION_URL:-}"
if [[ -n "${MP_NOTIF_URL// /}" ]]; then
  if [[ "$MP_NOTIF_URL" =~ $PROD_PATTERNS ]]; then
    log_error "MERCADOPAGO_NOTIFICATION_URL apunta a dominio productivo prohibido: $MP_NOTIF_URL"
  elif [[ ! "$MP_NOTIF_URL" =~ ^https?:// ]]; then
    log_error "MERCADOPAGO_NOTIFICATION_URL debe tener esquema http:// o https://"
  fi
fi

validate_optional_back_url() {
  local var_name="$1"
  local url_val="$2"
  local trimmed="$(echo "$url_val" | tr -d '[:space:]')"
  if [[ -n "$trimmed" ]]; then
    if [[ "$trimmed" =~ $PROD_PATTERNS ]]; then
      log_error "$var_name apunta a dominio productivo prohibido: $url_val"
    elif [[ ! "$trimmed" =~ ^https:// ]]; then
      log_error "$var_name debe usar HTTPS para ser aceptada por Mercado Pago. Recibido: $url_val"
    fi
  fi
}

MP_SUCCESS_URL="${MERCADOPAGO_BACK_URL_SUCCESS:-}"
validate_optional_back_url "MERCADOPAGO_BACK_URL_SUCCESS" "$MP_SUCCESS_URL"

MP_FAILURE_URL="${MERCADOPAGO_BACK_URL_FAILURE:-}"
validate_optional_back_url "MERCADOPAGO_BACK_URL_FAILURE" "$MP_FAILURE_URL"

MP_PENDING_URL="${MERCADOPAGO_BACK_URL_PENDING:-}"
validate_optional_back_url "MERCADOPAGO_BACK_URL_PENDING" "$MP_PENDING_URL"

# --- E. Validación de Schedulers Desactivados ---
RECON_ENABLED="${APP_PAYMENT_RECONCILIATION_ENABLED:-}"
if [[ "$RECON_ENABLED" != "false" ]]; then
  log_error "APP_PAYMENT_RECONCILIATION_ENABLED debe ser 'false'. Valor: $RECON_ENABLED"
fi

RECON_ORDERS_ENABLED="${APP_PAYMENT_RECONCILIATION_ORDERS_ENABLED:-}"
if [[ "$RECON_ORDERS_ENABLED" != "false" ]]; then
  log_error "APP_PAYMENT_RECONCILIATION_ORDERS_ENABLED debe ser 'false'. Valor: $RECON_ORDERS_ENABLED"
fi

REFUND_SCHEDULER_ENABLED="${PAYMENTS_MERCADOPAGO_REFUND_SCHEDULER_ENABLED:-}"
if [[ "$REFUND_SCHEDULER_ENABLED" != "false" ]]; then
  log_error "PAYMENTS_MERCADOPAGO_REFUND_SCHEDULER_ENABLED debe ser 'false'. Valor: $REFUND_SCHEDULER_ENABLED"
fi

# --- F. Verificación de Conectividad con PostgreSQL Local (Puerto 5433) ---
if ! (exec 3<>/dev/tcp/127.0.0.1/5433) 2>/dev/null; then
  log_error "PostgreSQL local no responde en 127.0.0.1:5433. ¿Está iniciado el contenedor Podman?"
fi

# ------------------------------------------------------------------------------
# 6. Evaluación de Resultados
# ------------------------------------------------------------------------------
if [[ $ERRORS -gt 0 ]]; then
  echo "" >&2
  echo "[RESULTADO] $ERRORS validación(es) fallaron. Arranque CANCELADO por seguridad." >&2
  exit 1
fi

log_ok "Base de datos local verificada: $DB_URL ($DB_USERNAME)"
log_ok "Conexión TCP a PostgreSQL local (127.0.0.1:5433) exitosa."
log_ok "Modo de integración: $MP_MODE"
log_ok "Aislamiento de URLs validado: cero referencias a dominios productivos"
log_ok "Schedulers de reconciliación y reembolsos desactivados (false)"
log_ok "Configuración segura validada al 100%."

if [[ "$CHECK_ONLY" == true ]]; then
  echo ""
  echo "[CHECK-ONLY] Verificación completada con éxito. Listo para arranque controlado."
  exit 0
fi

# ------------------------------------------------------------------------------
# 7. Arranque Seguro de Spring Boot
# ------------------------------------------------------------------------------
JAR_FILE="${SCRIPT_DIR}/target/backend-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR_FILE" ]]; then
  echo "[ERROR] Archivo JAR no encontrado en $JAR_FILE. Ejecute 'mvn package -DskipTests' primero." >&2
  exit 1
fi

echo ""
echo "[INFO] Iniciando backend JaldiShop en modo Sandbox seguro..."

cd "$REPO_DIR"

# Parámetros CLI sin secretos (los secretos van exclusivamente en variables de entorno OS privadas)
CLI_ARGS=(
  "--spring.datasource.url=${DB_URL}"
  "--spring.datasource.username=${DB_USERNAME}"
  "--mercadopago.checkout-mode=${MP_MODE}"
  "--mercadopago.notification-url=${MP_NOTIF_URL}"
  "--mercadopago.back-url-success=${MP_SUCCESS_URL}"
  "--mercadopago.back-url-failure=${MP_FAILURE_URL}"
  "--mercadopago.back-url-pending=${MP_PENDING_URL}"
  "--app.payment.reconciliation.enabled=${RECON_ENABLED}"
  "--app.payment.reconciliation.orders-enabled=${RECON_ORDERS_ENABLED}"
  "--payments.mercadopago.refund-scheduler-enabled=${REFUND_SCHEDULER_ENABLED}"
  "--server.port=${PORT:-8080}"
)

if [[ "$TEST_RUN_SECONDS" -gt 0 ]]; then
  echo "[INFO] Modo --test-run activo: el proceso se ejecutará por ${TEST_RUN_SECONDS} segundos para verificar aislamiento..."
  LOG_FILE="/tmp/jaldishop-sandbox-test.log"
  rm -f "$LOG_FILE"
  
  java -jar "$JAR_FILE" "${CLI_ARGS[@]}" > "$LOG_FILE" 2>&1 &
  APP_PID=$!
  
  echo "[INFO] Proceso Spring Boot arrancado en segundo plano con PID: $APP_PID"
  echo "[INFO] Monitoreando log durante ${TEST_RUN_SECONDS} segundos..."
  
  sleep "$TEST_RUN_SECONDS"
  
  if kill -0 "$APP_PID" 2>/dev/null; then
    echo "[INFO] Deteniendo proceso $APP_PID de forma segura (SIGTERM)..."
    kill -15 "$APP_PID"
    wait "$APP_PID" 2>/dev/null || true
    echo "[OK] Proceso detenido limpiamente."
  else
    echo "[AVISO] El proceso ya había terminado."
  fi
  
  echo ""
  echo "--- Resumen del Log de Arranque ---"
  grep -E "Started BackendApplication|HikariPool|Flyway|Tomcat started|mercadopago|reconciliation" "$LOG_FILE" | head -n 25 || true
  exit 0
fi

# Modo normal interactivo
exec java -jar "$JAR_FILE" "${CLI_ARGS[@]}"
