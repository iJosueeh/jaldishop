"""Sanea y genera las colecciones separadas y simplificadas para Postman y Bruno.
Garantiza:
1. Cero referencias a 'pm' en Bruno (usa APIs nativas bru, req, res, test, expect).
2. Estructura principal en 8 módulos requeridos.
3. Separación de pruebas negativas en colección exhaustiva secundaria.
4. Automatización de tokens e identificadores por rol (customer, merchant, admin).
5. Flujos claros para desarrollo local y demo sin imposición de runCase ni disposableFixturesReady.
6. Sincronización de contratos HTTP entre Postman y Bruno.
"""
from pathlib import Path
import sys

# Importar lógica central de construcción de colecciones
from build_all_collections import main as build_collections

def main():
    print("Iniciando generación y saneamiento de colecciones JaldiShop (Postman + Bruno)...")
    build_collections()
    print("Generación completada exitosamente.")

if __name__ == '__main__':
    main()
