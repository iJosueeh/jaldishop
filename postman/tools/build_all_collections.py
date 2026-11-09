"""Genera las colecciones simplificadas y separadas para Postman y Bruno.
Garantiza:
1. Cero uso de pm en Bruno (usa APIs nativas bru, req, res, test, expect).
2. Estructura principal en 8 carpetas requeridas.
3. Separación de pruebas negativas en colección exhaustiva secundaria.
4. Captura automática de customerToken, merchantToken, adminToken, customerUserId,
   merchantUserId, storeId, productId, variantId, cartId, orderNumber.
5. Flujos customer y merchant ordenados y comprensibles.
6. Ningún requerimiento de runCase ni disposableFixturesReady en requests normales.
7. Postman y Bruno separados técnicamente sin mezclar APIs.
"""
import copy
import json
import os
import re
import yaml
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
POSTMAN_DIR = REPO_ROOT / 'postman'
BRUNO_DIR = REPO_ROOT / 'bruno'
USER_BRUNO_DIR = Path('/home/ijosueeh/Documentos/bruno')
BRUNO_WORKSPACE = Path('/home/ijosueeh/.config/bruno/default-workspace/workspace.yml')

BASE_URL = 'http://127.0.0.1:8080'

# Estructura principal de 8 carpetas
FOLDER_MAPPING = [
    ('01. Autenticación', [
        ('M039', '01. Registrar Customer'),
        ('M001', '02. Iniciar Sesión — Customer'),
        ('K025', '03. Registrar Merchant y Tienda'),
        ('M009', '04. Iniciar Sesión — Merchant'),
        ('M017', '05. Iniciar Sesión — Admin'),
        ('M025', '06. Refrescar Token'),
        ('M027', '07. Consultar Mi Perfil'),
        ('M029', '08. Actualizar Mi Perfil'),
    ]),
    ('02. Customer', [
        ('Direcciones', [
            ('M053', '01. Listar Mis Direcciones'),
            ('M056', '02. Crear Dirección'),
            ('M092', '03. Actualizar Dirección'),
            ('M131', '04. Marcar Dirección Predeterminada'),
            ('M135', '05. Eliminar Dirección'),
        ]),
        ('Favoritos', [
            ('M149', '01. Listar Mis Favoritos'),
            ('M156', '02. Agregar Producto a Favoritos'),
            ('M152', '03. Consultar Estado de Favorito'),
            ('M160', '04. Eliminar Producto de Favoritos'),
        ]),
        ('Reseñas', [
            ('M188', '01. Verificar Elegibilidad para Reseña'),
            ('M195', '02. Crear Reseña de Producto'),
            ('M204', '03. Actualizar Reseña de Producto'),
            ('M192', '04. Listar Mis Reseñas'),
            ('M213', '05. Eliminar Mi Reseña de Producto'),
        ]),
    ]),
    ('03. Tiendas y catálogo', [
        ('J001', '01. Estado del Servicio (Health)'),
        ('K003', '02. Listar Tiendas Destacadas'),
        ('K002', '03. Buscar Tiendas'),
        ('K004', '04. Obtener Tienda por Slug'),
        ('M141', '05. Listar Categorías por Slug'),
        ('M142', '06. Listar Productos por Slug'),
        ('M143', '07. Listar Categorías por ID de Tienda'),
        ('M145', '08. Listar Productos por ID de Tienda'),
        ('K001', '09. Listar Categorías Globales'),
        ('M147', '10. Consultar Reseñas Públicas de Producto'),
    ]),
    ('04. Carrito y checkout', [
        ('Carrito', [
            ('M164', '01. Obtener Carrito'),
            ('M166', '02. Agregar Ítem al Carrito'),
            ('M172', '03. Actualizar Cantidad de Ítem'),
            ('M177', '04. Eliminar Ítem del Carrito'),
            ('M180', '05. Vaciar Carrito'),
        ]),
        ('Capacidad y Franjas', [
            ('K005', '01. Consultar Franjas Horarias (Slots)'),
            ('K007', '02. Consultar Disponibilidad de Capacidad'),
            ('K264', '03. Consultar Capacidad Efectiva'),
            ('K009', '04. Crear Reserva de Capacidad'),
            ('K019', '05. Obtener Reserva por ID'),
            ('K022', '06. Cancelar Reserva de Capacidad'),
        ]),
        ('Checkout', [
            ('J002', '01. Iniciar Checkout (Crear Pedido / Reserva)'),
        ]),
    ]),
    ('05. Pedidos y pagos', [
        ('Pedidos Customer', [
            ('M182', '01. Listar Mis Pedidos'),
            ('M184', '02. Obtener Pedido por Número'),
            ('M186', '03. Cancelar Pedido'),
        ]),
        ('Pagos y Mercado Pago (Fake / Sandbox)', [
            ('J014', '01. Crear Preferencia de Pago Mercado Pago'),
            ('J071', '02. Webhook Mercado Pago — Payment'),
            ('J072', '03. Webhook Mercado Pago — Order'),
        ]),
        ('Admin Pagos', [
            ('J067', '01. Investigar Intento de Pago'),
            ('J063', '02. Reconciliar Intento de Pago'),
        ]),
    ]),
    ('06. Merchant', [
        ('Mi Tienda', [
            ('K073', '01. Obtener Mi Tienda'),
            ('K075', '02. Actualizar Mi Tienda'),
            ('K058', '03. Crear Tienda Adicional'),
            ('K089', '04. Eliminar Mi Tienda (Destructivo)'),
            ('K269', '05. Generar Firma Multimedia'),
        ]),
        ('Catálogo y Variantes', [
            ('K091', '01. Listar Categorías de la Tienda'),
            ('K095', '02. Crear Categoría en Tienda'),
            ('K103', '03. Obtener Categoría por ID'),
            ('K108', '04. Actualizar Categoría en Tienda'),
            ('K117', '05. Listar Productos de la Tienda'),
            ('K121', '06. Crear Producto en Tienda'),
            ('K132', '07. Obtener Producto por ID'),
            ('K137', '08. Actualizar Producto en Tienda'),
            ('K149', '09. Listar Variantes de Producto'),
            ('K154', '10. Crear Variante de Producto'),
            ('K180', '11. Obtener Variante por ID'),
            ('K186', '12. Actualizar Variante de Producto'),
        ]),
        ('Inventario', [
            ('K212', '01. Consultar Inventario de Tienda'),
            ('K231', '02. Consultar Variantes con Stock Bajo'),
            ('K215', '03. Consultar Inventario de Variante'),
            ('K219', '04. Ajustar Stock Físico'),
            ('K226', '05. Actualizar Umbral de Stock Bajo'),
        ]),
        ('Configuración de Capacidad', [
            ('K234', '01. Listar Configuraciones'),
            ('K236', '02. Crear Configuración'),
            ('K238', '03. Actualizar Configuración'),
            ('K241', '04. Activar Configuración'),
            ('K244', '05. Desactivar Configuración'),
            ('K247', '06. Listar Excepciones'),
            ('K249', '07. Crear Excepción'),
            ('K253', '08. Actualizar Excepción'),
            ('K258', '09. Activar Excepción'),
            ('K261', '10. Desactivar Excepción'),
        ]),
        ('Pedidos y Clientes', [
            ('M217', '01. Listar Pedidos de Tienda'),
            ('M221', '02. Obtener Detalle de Pedido'),
            ('M226', '03. Actualizar Estado de Pedido'),
            ('K266', '04. Listar Clientes de la Tienda'),
        ]),
        ('Administración de Plataforma', [
            ('J028', '01. Listar Usuarios'),
            ('J032', '02. Obtener Usuario por ID'),
            ('J036', '03. Actualizar Roles de Usuario'),
            ('J040', '04. Suspender Usuario'),
            ('J044', '05. Reactivar Usuario'),
            ('J048', '06. Listar Tiendas'),
            ('J051', '07. Obtener Tienda por ID'),
            ('J055', '08. Suspender Tienda'),
            ('J059', '09. Reactivar Tienda'),
        ]),
    ]),
    ('07. Notificaciones', [
        ('M231', '01. Listar Mis Notificaciones'),
        ('M233', '02. Contar Notificaciones No Leídas'),
        ('M235', '03. Marcar Notificación como Leída'),
    ]),
    ('08. Regresiones', [
        ('R001', '01. POST Dirección Duplicada (409)'),
        ('R002', '02. PUT hacia Otra Dirección Existente (409)'),
        ('R003', '03. PUT Conserva Ubicación Propia (200)'),
        ('R004', '04. Búsqueda sin Tilde (200)'),
        ('R005', '05. Búsqueda Porcentaje Literal (200)'),
        ('R006', '06. Búsqueda Guion Bajo Literal (200)'),
        ('R007', '07. Búsqueda Límite Inválido 0 (400)'),
        ('R008', '08. Búsqueda Límite Inválido 51 (400)'),
        ('R009', '09. Fechas de Clientes Merchant (200)'),
        ('R010', '10. Método PATCH no Soportado Conserva Allow (405)'),
        ('R011', '11. Ruta Inexistente Devuelve 404 Controlado'),
    ]),
]

def walk_collection(items):
    for it in items:
        if 'item' in it:
            yield from walk_collection(it['item'])
        else:
            yield it

def extract_case_id(name):
    # Soporta formatos "M001 | VALIDO | ..." y "... (M001)"
    m = re.search(r'\(([KMJR]\d{3})\)', name)
    if m:
        return m.group(1)
    m = re.search(r'^([KMJR]\d{3})\b', name)
    if m:
        return m.group(1)
    return name.split(' | ')[0].strip()

def build_postman_prerequest(case_id):
    lines = [
        '// Validación de seguridad local: solo localhost o 127.0.0.1 permitido',
        'const rawUrl = pm.variables.replaceIn(pm.request.url.toString());',
        'const isLocal = /^http:\\/\\/(localhost|127\\.0\\.0\\.1|\\[::1\\])(?::\\d+)?\\//.test(rawUrl);',
        'if (!isLocal) {',
        '    throw new Error("BLOQUEADO: Las peticiones solo están permitidas contra entornos locales (localhost / 127.0.0.1)");',
        '}',
    ]
    if case_id in {'M001', 'M039'}:
        lines.append('pm.environment.set("customerToken", "");')
    elif case_id in {'M009', 'K025'}:
        lines.append('pm.environment.set("merchantToken", "");')
    elif case_id == 'M017':
        lines.append('pm.environment.set("adminToken", "");')
    elif case_id in {'J014', 'J071', 'J072'}:
        lines.extend([
            'if (pm.environment.get("financialFakeReady") !== "true") {',
            '    throw new Error("BLOQUEADO: Operación de pago requiere financialFakeReady=true con gateway fake local verificado.");',
            '}'
        ])
    elif case_id == 'K089':
        lines.extend([
            'if (pm.environment.get("allowDestructiveOperations") !== "true") {',
            '    throw new Error("BLOQUEADO: Operación destructiva requiere allowDestructiveOperations=true en el entorno.");',
            '}'
        ])
    return lines

def build_postman_test(case_id, original_tests=None):
    expected_status = 200
    if case_id in {'M039', 'K025', 'M056', 'K058', 'K095', 'K121', 'K154', 'J002', 'J014', 'K236', 'K249', 'K009', 'M195'}:
        expected_status = 201
    elif case_id in {'M135', 'M160', 'M177', 'M180', 'K022', 'K089', 'M213'}:
        expected_status = 204

    # Regresiones
    status_map = {'R001': 409, 'R002': 409, 'R003': 200, 'R004': 200, 'R005': 200, 'R006': 200, 'R007': 400, 'R008': 400, 'R009': 200, 'R010': 405, 'R011': 404}
    if case_id in status_map:
        expected_status = status_map[case_id]

    lines = [f'pm.test("HTTP esperado", function () {{ pm.response.to.have.status({expected_status}); }});']

    # Capturas automáticas
    if case_id == 'M039':  # Register Customer
        lines.extend([
            'if (pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("customerUserId", String(data.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'M001':  # Login Customer
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.token) pm.environment.set("customerToken", data.token);',
            '        if (data && data.userId) pm.environment.set("customerUserId", String(data.userId));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'K025':  # Register Merchant
        lines.extend([
            'if (pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.token) pm.environment.set("merchantToken", data.token);',
            '        if (data && data.userId) pm.environment.set("merchantUserId", String(data.userId));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'M009':  # Login Merchant
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.token) pm.environment.set("merchantToken", data.token);',
            '        if (data && data.userId) pm.environment.set("merchantUserId", String(data.userId));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'M017':  # Login Admin
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.token) pm.environment.set("adminToken", data.token);',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'K058', 'K073', 'K075'}:  # Store endpoints
        lines.extend([
            'if (pm.response.code === 200 || pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("storeId", String(data.id));',
            '        if (data && data.slug) pm.environment.set("storeSlug", String(data.slug));',
            '        if (data && data.merchantUserId) pm.environment.set("merchantUserId", String(data.merchantUserId));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'K095', 'K091'}:  # Category endpoints
        lines.extend([
            'if (pm.response.code === 201 || pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var item = Array.isArray(data) ? data[0] : data;',
            '        if (item && item.id) pm.environment.set("categoryId", String(item.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'K121', 'K117'}:  # Product endpoints
        lines.extend([
            'if (pm.response.code === 201 || pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var item = Array.isArray(data) ? data[0] : data;',
            '        if (item && item.id) pm.environment.set("productId", String(item.id));',
            '        if (item && item.variants && item.variants.length > 0 && item.variants[0].id) {',
            '            pm.environment.set("variantId", String(item.variants[0].id));',
            '        }',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'K154', 'K149'}:  # Variant endpoints
        lines.extend([
            'if (pm.response.code === 201 || pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var item = Array.isArray(data) ? data[0] : data;',
            '        if (item && item.id) pm.environment.set("variantId", String(item.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'M164':  # Cart
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("cartId", String(data.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'J002':  # Checkout
        lines.extend([
            'if (pm.response.code === 201 || pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.reservationId) pm.environment.set("reservationId", String(data.reservationId));',
            '        if (data && data.orderNumber) pm.environment.set("orderNumber", String(data.orderNumber));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'M182', 'M217'}:  # Orders
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var items = Array.isArray(data) ? data : (data && data.content ? data.content : []);',
            '        if (items.length > 0) {',
            '            if (items[0].id) pm.environment.set("orderId", String(items[0].id));',
            '            if (items[0].orderNumber) pm.environment.set("orderNumber", String(items[0].orderNumber));',
            '        }',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id in {'M056', 'M053'}:  # Address
        lines.extend([
            'if (pm.response.code === 201 || pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var item = Array.isArray(data) ? data[0] : data;',
            '        if (item && item.id) pm.environment.set("addressId", String(item.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'K236':  # Capacity config
        lines.extend([
            'if (pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("capacityConfigId", String(data.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'K249':  # Capacity exception
        lines.extend([
            'if (pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("capacityExceptionId", String(data.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'K009':  # Capacity reservation
        lines.extend([
            'if (pm.response.code === 201) {',
            '    try {',
            '        var data = pm.response.json();',
            '        if (data && data.id) pm.environment.set("reservationId", String(data.id));',
            '    } catch (e) {}',
            '}'
        ])
    elif case_id == 'M231':  # Notifications
        lines.extend([
            'if (pm.response.code === 200) {',
            '    try {',
            '        var data = pm.response.json();',
            '        var items = Array.isArray(data) ? data : (data && data.content ? data.content : []);',
            '        if (items.length > 0 && items[0].id) pm.environment.set("notificationId", String(items[0].id));',
            '    } catch (e) {}',
            '}'
        ])

    # Extra regression tests
    if case_id.startswith('R') and original_tests:
        extra = [line for line in original_tests if not line.startswith('pm.test("HTTP esperado"')]
        lines.extend(extra)

    return lines


def build_bruno_scripts(case_id, expected_status=200):
    pre_script = [
        'const url = req.getUrl();',
        'if (!/^http:\\/\\/(localhost|127\\.0\\.0\\.1|\\[::1\\])(?::\\d+)?\\//.test(url)) {',
        '  throw new Error("Por seguridad local, baseUrl debe apuntar a localhost / 127.0.0.1");',
        '}',
    ]
    if case_id in {'M001', 'M039'}:
        pre_script.append('bru.setEnvVar("customerToken", "");')
    elif case_id in {'M009', 'K025'}:
        pre_script.append('bru.setEnvVar("merchantToken", "");')
    elif case_id == 'M017':
        pre_script.append('bru.setEnvVar("adminToken", "");')
    elif case_id in {'J014', 'J071', 'J072'}:
        pre_script.extend([
            'if (bru.getEnvVar("financialFakeReady") !== "true") {',
            '  throw new Error("BLOQUEADO: Operación de pago requiere financialFakeReady=true con gateway fake local verificado.");',
            '}'
        ])
    elif case_id == 'K089':
        pre_script.extend([
            'if (bru.getEnvVar("allowDestructiveOperations") !== "true") {',
            '  throw new Error("BLOQUEADO: Operación destructiva requiere allowDestructiveOperations=true en el entorno.");',
            '}'
        ])

    post_script = []
    if case_id == 'M039':
        post_script.extend([
            'if (res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("customerUserId", String(data.id));',
            '}'
        ])
    elif case_id == 'M001':
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  if (data && data.token) bru.setEnvVar("customerToken", data.token);',
            '  if (data && data.userId) bru.setEnvVar("customerUserId", String(data.userId));',
            '}'
        ])
    elif case_id == 'K025':
        post_script.extend([
            'if (res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.token) bru.setEnvVar("merchantToken", data.token);',
            '  if (data && data.userId) bru.setEnvVar("merchantUserId", String(data.userId));',
            '}'
        ])
    elif case_id == 'M009':
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  if (data && data.token) bru.setEnvVar("merchantToken", data.token);',
            '  if (data && data.userId) bru.setEnvVar("merchantUserId", String(data.userId));',
            '}'
        ])
    elif case_id == 'M017':
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  if (data && data.token) bru.setEnvVar("adminToken", data.token);',
            '}'
        ])
    elif case_id in {'K058', 'K073', 'K075'}:
        post_script.extend([
            'if (res.getStatus() === 200 || res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("storeId", String(data.id));',
            '  if (data && data.slug) bru.setEnvVar("storeSlug", String(data.slug));',
            '  if (data && data.merchantUserId) bru.setEnvVar("merchantUserId", String(data.merchantUserId));',
            '}'
        ])
    elif case_id in {'K095', 'K091'}:
        post_script.extend([
            'if (res.getStatus() === 201 || res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const item = Array.isArray(data) ? data[0] : data;',
            '  if (item && item.id) bru.setEnvVar("categoryId", String(item.id));',
            '}'
        ])
    elif case_id in {'K121', 'K117'}:
        post_script.extend([
            'if (res.getStatus() === 201 || res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const item = Array.isArray(data) ? data[0] : data;',
            '  if (item && item.id) bru.setEnvVar("productId", String(item.id));',
            '  if (item && item.variants && item.variants.length > 0 && item.variants[0].id) {',
            '    bru.setEnvVar("variantId", String(item.variants[0].id));',
            '  }',
            '}'
        ])
    elif case_id in {'K154', 'K149'}:
        post_script.extend([
            'if (res.getStatus() === 201 || res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const item = Array.isArray(data) ? data[0] : data;',
            '  if (item && item.id) bru.setEnvVar("variantId", String(item.id));',
            '}'
        ])
    elif case_id == 'M164':
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("cartId", String(data.id));',
            '}'
        ])
    elif case_id == 'J002':
        post_script.extend([
            'if (res.getStatus() === 201 || res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  if (data && data.reservationId) bru.setEnvVar("reservationId", String(data.reservationId));',
            '  if (data && data.orderNumber) bru.setEnvVar("orderNumber", String(data.orderNumber));',
            '}'
        ])
    elif case_id in {'M182', 'M217'}:
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const items = Array.isArray(data) ? data : (data && data.content ? data.content : []);',
            '  if (items.length > 0) {',
            '    if (items[0].id) bru.setEnvVar("orderId", String(items[0].id));',
            '    if (items[0].orderNumber) bru.setEnvVar("orderNumber", String(items[0].orderNumber));',
            '  }',
            '}'
        ])
    elif case_id in {'M056', 'M053'}:
        post_script.extend([
            'if (res.getStatus() === 201 || res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const item = Array.isArray(data) ? data[0] : data;',
            '  if (item && item.id) bru.setEnvVar("addressId", String(item.id));',
            '}'
        ])
    elif case_id == 'K236':
        post_script.extend([
            'if (res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("capacityConfigId", String(data.id));',
            '}'
        ])
    elif case_id == 'K249':
        post_script.extend([
            'if (res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("capacityExceptionId", String(data.id));',
            '}'
        ])
    elif case_id == 'K009':
        post_script.extend([
            'if (res.getStatus() === 201) {',
            '  const data = res.getBody();',
            '  if (data && data.id) bru.setEnvVar("reservationId", String(data.id));',
            '}'
        ])
    elif case_id == 'M231':
        post_script.extend([
            'if (res.getStatus() === 200) {',
            '  const data = res.getBody();',
            '  const items = Array.isArray(data) ? data : (data && data.content ? data.content : []);',
            '  if (items.length > 0 && items[0].id) bru.setEnvVar("notificationId", String(items[0].id));',
            '}'
        ])

    test_script = [
        f'test("HTTP esperado", function() {{ expect(res.getStatus()).to.equal({expected_status}); }});'
    ]
    if case_id == 'R001' or case_id == 'R002':
        test_script.append('test("Código ADDRESS_ALREADY_EXISTS", function() { expect(res.getBody().code).to.equal("ADDRESS_ALREADY_EXISTS"); });')
    elif case_id in {'R007', 'R008'}:
        test_script.append('test("Código VALIDATION_ERROR", function() { expect(res.getBody().code).to.equal("VALIDATION_ERROR"); });')
    elif case_id == 'R010':
        test_script.extend([
            'test("Código METHOD_NOT_ALLOWED", function() { expect(res.getBody().code).to.equal("METHOD_NOT_ALLOWED"); });',
            'test("Cabecera Allow presente", function() { expect(res.getHeaders()["allow"] || res.getHeaders()["Allow"]).to.include("GET"); });'
        ])
    elif case_id == 'R011':
        test_script.append('test("Código RESOURCE_NOT_FOUND", function() { expect(res.getBody().code).to.equal("RESOURCE_NOT_FOUND"); });')
    elif case_id in {'R004', 'R005', 'R006'}:
        test_script.append('test("Resultados no vacíos", function() { expect(res.getBody()).to.be.an("array").that.is.not.empty; });')
    elif case_id == 'R009':
        test_script.append('test("Clientes no vacíos", function() { expect(res.getBody()).to.be.an("array").that.is.not.empty; });')

    return pre_script, post_script, test_script


def item_to_bru(item, name, seq):
    req = item['request']
    method = req['method'].lower()
    url = req['url'] if isinstance(req['url'], str) else req['url']['raw']
    body_data = req.get('body', {}).get('raw')
    case_id = extract_case_id(item['name'])

    # Auth
    auth_type = req.get('auth', {}).get('type', 'none')
    auth_token = ''
    if auth_type == 'bearer':
        for b in req['auth'].get('bearer', []):
            if b.get('key') == 'token':
                auth_token = b.get('value', '')
    
    # Scripts
    pre_script, post_script, test_script = build_bruno_scripts(case_id)

    lines = [
        'meta {',
        f'  name: {name}',
        '  type: http',
        f'  seq: {seq}',
        '}',
        '',
        f'{method} {{',
        f'  url: {url}',
        '  body: json' if body_data else '  body: none',
        f'  auth: {auth_type if auth_token else "none"}',
        '}',
        '',
    ]

    if auth_type == 'bearer' and auth_token:
        lines.extend([
            'auth:bearer {',
            f'  token: {auth_token}',
            '}',
            '',
        ])

    lines.extend([
        'headers {',
        '  Accept: application/json',
    ])
    if body_data:
        lines.append('  Content-Type: application/json')
    # Regresión R010 o headers específicos
    for h in req.get('header', []):
        if h.get('key') not in {'Accept', 'Content-Type'}:
            lines.append(f'  {h["key"]}: {h["value"]}')
    lines.extend(['}', ''])

    if body_data:
        lines.extend([
            'body:json {',
            body_data,
            '}',
            '',
        ])

    if pre_script:
        lines.extend([
            'script:pre-request {',
            *[f'  {l}' for l in pre_script],
            '}',
            '',
        ])

    if post_script:
        lines.extend([
            'script:post-response {',
            *[f'  {l}' for l in post_script],
            '}',
            '',
        ])

    if test_script:
        lines.extend([
            'tests {',
            *[f'  {l}' for l in test_script],
            '}',
            '',
        ])

    return '\n'.join(lines)


def item_to_opencollection(item, name, seq):
    req = item['request']
    method = req['method']
    url = req['url'] if isinstance(req['url'], str) else req['url']['raw']
    body_data = req.get('body', {}).get('raw')
    case_id = extract_case_id(item['name'])

    pre_script, post_script, test_script = build_bruno_scripts(case_id)

    obj = {
        'info': {
            'name': name,
            'type': 'http',
            'seq': seq,
        },
        'http': {
            'method': method,
            'url': url,
            'headers': [
                {'name': 'Accept', 'value': 'application/json'}
            ]
        }
    }

    if body_data:
        obj['http']['headers'].append({'name': 'Content-Type', 'value': 'application/json'})
        obj['http']['body'] = {
            'type': 'json',
            'data': body_data
        }

    # Auth
    auth_type = req.get('auth', {}).get('type', 'none')
    if auth_type == 'bearer':
        for b in req['auth'].get('bearer', []):
            if b.get('key') == 'token':
                token = b.get('value', '')
                obj['http']['auth'] = {
                    'type': 'bearer',
                    'bearer': {'token': token}
                }

    scripts = []
    if pre_script:
        scripts.append({'type': 'before-request', 'code': '\n'.join(pre_script)})
    after_code = []
    if post_script:
        after_code.extend(post_script)
    if test_script:
        after_code.extend(test_script)
    if after_code:
        scripts.append({'type': 'after-response', 'code': '\n'.join(after_code)})

    if scripts:
        obj['runtime'] = {'scripts': scripts}

    obj['settings'] = {
        'encodeUrl': True,
        'timeout': 0,
        'followRedirects': False,
        'maxRedirects': 5,
        'forwardAuthorizationHeader': False,
    }

    return obj


def sanitize_filename(name):
    # Reemplaza caracteres inválidos para nombres de archivo
    return re.sub(r'[\\/*?:"<>|]', '_', name).strip()


def main():
    source_path = POSTMAN_DIR / 'jaldishop.postman_collection.json'
    reg_path = POSTMAN_DIR / 'jaldishop.regressions.postman_collection.json'
    
    col_data = json.loads(source_path.read_text())
    items_to_index = list(walk_collection(col_data['item']))
    if reg_path.exists():
        reg_data = json.loads(reg_path.read_text())
        items_to_index.extend(walk_collection(reg_data['item']))

    all_items = {}
    for it in items_to_index:
        cid = extract_case_id(it['name'])
        if cid not in all_items or 'VALIDO' in it['name']:
            all_items[cid] = it

    # 1. Armar Colección Principal Postman
    main_collection_items = []
    used_ids = set()

    for folder_name, contents in FOLDER_MAPPING:
        folder_item = {'name': folder_name, 'item': []}
        
        # Check if contents has subfolders
        if isinstance(contents[0][1], list):
            # Subfolders present
            for sub_name, sub_items in contents:
                sub_folder = {'name': sub_name, 'item': []}
                for cid, label in sub_items:
                    if cid not in all_items:
                        raise ValueError(f"ID faltante en items: {cid}")
                    it = copy.deepcopy(all_items[cid])
                    used_ids.add(cid)
                    it['name'] = f'{label} ({cid})'
                    it['request']['description'] = f'Caso {cid}: {label}. Flujo funcional para desarrollo local y demo.'
                    # Actualizar scripts Postman
                    pre = build_postman_prerequest(cid)
                    orig_tests = []
                    if len(it.get('event', [])) > 1:
                        orig_tests = it['event'][1]['script'].get('exec', [])
                    test = build_postman_test(cid, orig_tests)
                    it['event'] = [
                        {'listen': 'prerequest', 'script': {'type': 'text/javascript', 'exec': pre}},
                        {'listen': 'test', 'script': {'type': 'text/javascript', 'exec': test}}
                    ]
                    sub_folder['item'].append(it)
                folder_item['item'].append(sub_folder)
        else:
            for cid, label in contents:
                if cid not in all_items:
                    raise ValueError(f"ID faltante en items: {cid}")
                it = copy.deepcopy(all_items[cid])
                used_ids.add(cid)
                it['name'] = f'{label} ({cid})'
                it['request']['description'] = f'Caso {cid}: {label}. Flujo funcional para desarrollo local y demo.'
                pre = build_postman_prerequest(cid)
                orig_tests = []
                if len(it.get('event', [])) > 1:
                    orig_tests = it['event'][1]['script'].get('exec', [])
                test = build_postman_test(cid, orig_tests)
                it['event'] = [
                    {'listen': 'prerequest', 'script': {'type': 'text/javascript', 'exec': pre}},
                    {'listen': 'test', 'script': {'type': 'text/javascript', 'exec': test}}
                ]
                folder_item['item'].append(it)
        main_collection_items.append(folder_item)

    # Identificar negativos restantes
    negative_items = [it for cid, it in sorted(all_items.items()) if cid not in used_ids]
    print(f"Items principales: {len(used_ids)}, Items negativos restantes: {len(negative_items)}")

    # Variables de entorno completas y unificadas para toda la suite
    all_keys = set()
    for it in list(walk_collection(main_collection_items)) + negative_items:
        content = json.dumps(it, ensure_ascii=False)
        all_keys.update(re.findall(r'\{\{(\w+)\}\}', content))
        for ev in it.get('event', []):
            code = '\n'.join(ev['script'].get('exec', []))
            all_keys.update(re.findall(r'pm\.environment\.(?:get|set)\(["\x27](\w+)["\x27]', code))
            all_keys.update(re.findall(r'bru\.(?:getEnvVar|setEnvVar)\(["\x27](\w+)["\x27]', code))

    all_keys.update([
        'baseUrl',
        'allowDestructiveOperations',
        'customerUserId',
        'merchantUserId',
        'cartId',
        'runCase',
        'disposableFixturesReady',
        'financialFakeReady',
        'revokedAdminToken',
        'invalidShortPassword',
        'existingMerchantEmail',
        'otherStoreId',
        'merchantOnlyToken',
        'otherAddressId',
        'missingAddressId',
        'checkoutReferenceId',
        'paymentId',
        'preferenceId',
    ])

    # Valores por defecto para desarrollo local reproducible
    default_values = {
        'baseUrl': BASE_URL,
        'adminEmail': 'admin.demo@jaldishop.com',
        'adminPassword': 'Password123!',
        'merchantEmail': 'merchant.demo@jaldishop.com',
        'merchantPassword': 'Password123!',
        'customerEmail': 'cliente.demo@jaldishop.com',
        'customerPassword': 'Password123!',
        'fixtureEmail': 'nuevo.cliente@example.test',
        'fixturePassword': 'Password123!',
        'fixtureFirstName': 'Valeria',
        'fixtureLastName': 'Ríos',
        'fixturePhone': '+51965432109',
        'fixtureAddress': 'Av. José Larco 345',
        'fixtureAddressReference': 'Frente al Parque Kennedy',
        'fixtureStreetAddress': 'Av. José Larco 345',
        'fixtureDeliveryAddress': 'Av. José Larco 345, Dpto 402',
        'fixtureDeliveryReference': 'Cerca al Parque Kennedy',
        'fixtureCustomerName': 'Valeria Ríos',
        'fixtureCustomerPhone': '+51965432109',
        'fixtureRecipientName': 'Valeria Ríos',
        'fixtureRecipientPhone': '+51965432109',
        'fixtureReference': 'Portón verde',
        'fixtureImageUrl': 'https://images.example.test/producto.jpg',
        'storeSearchQuery': 'pasteleria',
        'serviceDate': '2026-11-20',
        'startTime': '10:00:00',
        'endTime': '12:00:00',
        'duplicateStreetAddress': 'Av. José Larco 345',
        'duplicateUbigeoCode': '150122',
        'existingStreetAddress': 'Av. José Larco 345',
        'existingUbigeoCode': '150122',
        'existingMerchantEmail': 'merchant.demo@jaldishop.com',
        'newRegistrationEmail': 'nuevo.usuario@example.test',
        'invalidShortPassword': 'short',
        'allowDestructiveOperations': 'false',
        'financialFakeReady': 'false',
        'disposableFixturesReady': 'true',
        'runCase': 'R001',
    }

    # Guardar jaldishop.postman_collection.json
    main_col = {
        'info': {
            'name': 'JaldiShop API',
            'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
            'description': 'Colección principal y simplificada de JaldiShop API para desarrollo local y demostración. '
                           'Organizada en 8 módulos funcionales. Automatización de tokens por rol y flujos ordenados.'
        },
        'auth': {'type': 'noauth'},
        'variable': [{'key': k, 'value': default_values.get(k, ''), 'type': 'string'} for k in sorted(all_keys)],
        'item': main_collection_items
    }
    (POSTMAN_DIR / 'jaldishop.postman_collection.json').write_text(json.dumps(main_col, ensure_ascii=False, indent=2) + '\n')

    # Guardar jaldishop.regressions.postman_collection.json (colección secundaria de pruebas negativas y exhaustivas)
    regressions_folder = [it for it in main_collection_items if it['name'] == '08. Regresiones'][0]
    reg_col = {
        'info': {
            'name': 'JaldiShop API — Pruebas Exhaustivas y Negativas',
            'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
            'description': 'Colección secundaria de regresiones, validaciones de campo y casos negativos de JaldiShop API.'
        },
        'auth': {'type': 'noauth'},
        'variable': [{'key': k, 'value': default_values.get(k, ''), 'type': 'string'} for k in sorted(all_keys)],
        'item': [
            regressions_folder,
            {'name': 'Escenarios Negativos y Validaciones', 'item': negative_items}
        ]
    }
    (POSTMAN_DIR / 'jaldishop.regressions.postman_collection.json').write_text(json.dumps(reg_col, ensure_ascii=False, indent=2) + '\n')

    # Guardar entornos Postman
    env_data = {
        'name': 'JaldiShop Local — Desarrollo y Demo',
        'values': [
            {
                'key': k,
                'value': default_values.get(k, ''),
                'type': 'secret' if re.search(r'token|password|signature', k, re.I) else 'default',
                'enabled': True
            }
            for k in sorted(all_keys)
        ],
        '_postman_variable_scope': 'environment'
    }
    (POSTMAN_DIR / 'jaldishop.local.env.json').write_text(json.dumps(env_data, ensure_ascii=False, indent=2) + '\n')
    (POSTMAN_DIR / 'jaldishop.postman_environment.json').write_text(json.dumps(env_data, ensure_ascii=False, indent=2) + '\n')

    # Guardar jaldishop.render.env.json limpio y sincronizado (sin claves duplicadas ni obsoletas)
    render_env = {
        'id': 'f2a3b4c5-d6e7-8f9a-0b1c-2d3e4f5a6b7c',
        'name': 'JaldiShop Render (Producción)',
        'values': [
            {
                'key': k,
                'value': 'https://jaldishop-api.onrender.com' if k == 'baseUrl' else '',
                'type': 'secret' if re.search(r'token|password|signature', k, re.I) else 'default',
                'enabled': True
            }
            for k in sorted(all_keys)
        ],
        '_postman_variable_scope': 'environment'
    }
    (POSTMAN_DIR / 'jaldishop.render.env.json').write_text(json.dumps(render_env, ensure_ascii=False, indent=2) + '\n')

    # 2. Generar colecciones nativas Bruno (.bru)
    target_bruno_main = BRUNO_DIR / 'jaldishop'
    target_bruno_exhaustive = BRUNO_DIR / 'jaldishop-exhaustive'

    for tdir in [target_bruno_main, target_bruno_exhaustive]:
        if tdir.exists():
            import shutil
            shutil.rmtree(tdir)
        tdir.mkdir(parents=True, exist_ok=True)

    # bruno.json para ambas
    (target_bruno_main / 'bruno.json').write_text(json.dumps({
        'version': '1',
        'name': 'JaldiShop API',
        'type': 'collection',
        'ignore': ['node_modules', '.git']
    }, indent=2) + '\n')

    (target_bruno_exhaustive / 'bruno.json').write_text(json.dumps({
        'version': '1',
        'name': 'JaldiShop API — Pruebas Exhaustivas y Negativas',
        'type': 'collection',
        'ignore': ['node_modules', '.git']
    }, indent=2) + '\n')

    # Environments en Bruno (.bru, .yml OpenCollection y .json importable)
    bruno_vars = []
    for k in sorted(all_keys):
        is_sec = bool(re.search(r'token|password|signature', k, re.I))
        val = default_values.get(k, '')
        var_entry = {'name': k, 'value': str(val)}
        if is_sec:
            var_entry['secret'] = True
        bruno_vars.append(var_entry)

    bruno_env_dict = {
        'name': 'JaldiShop Local',
        'variables': bruno_vars
    }
    bruno_env_yaml = yaml.dump(bruno_env_dict, sort_keys=False, allow_unicode=True)
    bruno_env_json = json.dumps(bruno_env_dict, indent=2) + '\n'

    # Guardar JSON importable de Bruno en bruno/ y postman/
    (REPO_ROOT / 'bruno' / 'jaldishop.local.environment.json').write_text(bruno_env_json)
    (POSTMAN_DIR / 'jaldishop.local.environment.json').write_text(bruno_env_json)

    # Render environment importable para Bruno
    render_env_path = POSTMAN_DIR / 'jaldishop.render.env.json'
    if render_env_path.exists():
        with open(render_env_path) as rf:
            render_postman = json.load(rf)
        render_bruno_vars = []
        for it in render_postman.get('values', []):
            entry = {'name': it.get('key'), 'value': it.get('value', '')}
            if it.get('type') == 'secret':
                entry['secret'] = True
            render_bruno_vars.append(entry)
        render_bruno_dict = {
            'name': 'JaldiShop Render (Producción)',
            'variables': render_bruno_vars
        }
        render_bruno_json = json.dumps(render_bruno_dict, indent=2) + '\n'
        render_bruno_yaml = yaml.dump(render_bruno_dict, sort_keys=False, allow_unicode=True)
        (REPO_ROOT / 'bruno' / 'jaldishop.render.environment.json').write_text(render_bruno_json)
        (POSTMAN_DIR / 'jaldishop.render.environment.json').write_text(render_bruno_json)
    else:
        render_bruno_yaml = None

    for tdir in [target_bruno_main, target_bruno_exhaustive]:
        env_dir = tdir / 'environments'
        env_dir.mkdir(parents=True, exist_ok=True)
        var_lines = ['vars {']
        for k in sorted(all_keys):
            if not re.search(r'token|password|signature', k, re.I):
                val = default_values.get(k, '')
                var_lines.append(f'  {k}: {val}')
        var_lines.append('}')
        var_lines.append('vars:secret [')
        secret_keys = [k for k in sorted(all_keys) if re.search(r'token|password|signature', k, re.I)]
        for i, sk in enumerate(secret_keys):
            comma = ',' if i < len(secret_keys) - 1 else ''
            var_lines.append(f'  {sk}{comma}')
        var_lines.append(']')
        (env_dir / 'Local.bru').write_text('\n'.join(var_lines) + '\n')
        (env_dir / 'JaldiShop Local.yml').write_text(bruno_env_yaml)
        if render_bruno_yaml:
            (env_dir / 'JaldiShop Render (Producción).yml').write_text(render_bruno_yaml)

    # Escribir requests .bru de la colección principal
    for folder_name, contents in FOLDER_MAPPING:
        fdir = target_bruno_main / sanitize_filename(folder_name)
        fdir.mkdir(parents=True, exist_ok=True)
        if isinstance(contents[0][1], list):
            for sub_name, sub_items in contents:
                sdir = fdir / sanitize_filename(sub_name)
                sdir.mkdir(parents=True, exist_ok=True)
                for seq, (cid, label) in enumerate(sub_items, 1):
                    it = all_items[cid]
                    fname = f"{sanitize_filename(label)}.bru"
                    bru_content = item_to_bru(it, label, seq)
                    (sdir / fname).write_text(bru_content)
        else:
            for seq, (cid, label) in enumerate(contents, 1):
                it = all_items[cid]
                fname = f"{sanitize_filename(label)}.bru"
                bru_content = item_to_bru(it, label, seq)
                (fdir / fname).write_text(bru_content)

    # Escribir requests .bru de la colección exhaustiva
    reg_dir = target_bruno_exhaustive / '08. Regresiones'
    reg_dir.mkdir(parents=True, exist_ok=True)
    for seq, (cid, label) in enumerate(FOLDER_MAPPING[-1][1], 1):
        it = all_items[cid]
        fname = f"{sanitize_filename(label)}.bru"
        (reg_dir / fname).write_text(item_to_bru(it, label, seq))

    neg_dir = target_bruno_exhaustive / 'Escenarios Negativos'
    neg_dir.mkdir(parents=True, exist_ok=True)
    for seq, it in enumerate(negative_items, 1):
        cid = extract_case_id(it['name'])
        label = it['name'].replace(' | ', ' - ')
        fname = f"{sanitize_filename(label[:80])}.bru"
        (neg_dir / fname).write_text(item_to_bru(it, label, seq))

    print(f"Colección Bruno generada en {target_bruno_main} y {target_bruno_exhaustive}")

    # 3. Sincronizar en Documentos/bruno
    if USER_BRUNO_DIR.exists():
        user_main = USER_BRUNO_DIR / 'JaldiShop API'
        user_exh = USER_BRUNO_DIR / 'JaldiShop API — Pruebas Exhaustivas'
        import shutil
        if user_main.exists():
            shutil.rmtree(user_main)
        if user_exh.exists():
            shutil.rmtree(user_exh)
        shutil.copytree(target_bruno_main, user_main)
        shutil.copytree(target_bruno_exhaustive, user_exh)
        print(f"Sincronizado a {user_main} y {user_exh}")

        # También asegurar que JaldiShop Local.yml exista en todas las carpetas de colecciones con environments
        for sub in USER_BRUNO_DIR.iterdir():
            if sub.is_dir():
                sub_env = sub / 'environments'
                if sub_env.exists():
                    (sub_env / 'JaldiShop Local.yml').write_text(bruno_env_yaml)
                    if render_bruno_yaml:
                        (sub_env / 'JaldiShop Render (Producción).yml').write_text(render_bruno_yaml)
                    print(f"Instalado JaldiShop Local.yml en {sub_env}")

        # También limpiar y corregir JaldiShop API — desarrollo local - 1 para eliminar pm. definitivamente
        dev_local_1 = USER_BRUNO_DIR / 'JaldiShop API — desarrollo local - 1'
        if dev_local_1.exists():
            shutil.rmtree(dev_local_1)
            shutil.copytree(target_bruno_main, dev_local_1)
            print(f"Reemplazado {dev_local_1} con versión limpia sin pm.")

        # Actualizar workspace.yml de Bruno manteniendo colecciones activas del usuario (ej. JaldiShop API - 3)
        if BRUNO_WORKSPACE.exists():
            active_collections = [
                {"name": "Sample API Collection", "path": "/home/ijosueeh/Documentos/bruno/Sample API Collection"},
                {"name": "EWro", "path": "/home/ijosueeh/Documentos/bruno/EWro"},
            ]
            for candidate in ["JaldiShop API - 3", "JaldiShop API - 2", "JaldiShop API", "JaldiShop API — Pruebas Exhaustivas", "JaldiShop API — desarrollo local - 1"]:
                cand_path = USER_BRUNO_DIR / candidate
                if cand_path.exists():
                    active_collections.append({"name": candidate, "path": str(cand_path)})

            ws_lines = ["opencollection: 1.0.0", "info:", '  name: "My Workspace"', "  type: workspace", "", "collections:"]
            for col in active_collections:
                ws_lines.append(f'  - name: "{col["name"]}"')
                ws_lines.append(f'    path: "{col["path"]}"')
            ws_lines.extend(["", "specs:", "", "docs: ''", ""])
            BRUNO_WORKSPACE.write_text("\n".join(ws_lines))
            print(f"Actualizado workspace.yml con colecciones activas.")

if __name__ == '__main__':
    main()
