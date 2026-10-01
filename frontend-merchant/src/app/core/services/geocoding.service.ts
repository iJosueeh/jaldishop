import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';

export interface GeocodingResult {
  displayName: string;
  latitude: number;
  longitude: number;
  road?: string;
  suburb?: string;
  city?: string;
  state?: string;
}

@Service()
export class GeocodingService {
  private readonly http = inject(HttpClient);

  readonly isSearching = signal<boolean>(false);
  readonly results = signal<GeocodingResult[]>([]);
  readonly searchError = signal<string | null>(null);

  /**
   * Normaliza abreviaciones y formatos de texto peruanos (ej. "C.Coronel 521Villa" -> "Calle Coronel 521 Villa")
   */
  public normalizeQuery(rawQuery: string): string {
    let clean = rawQuery?.trim() ?? '';

    // 1. Separar números pegados a palabras o viceversa (ej. "521Villa" -> "521 Villa")
    clean = clean.replace(/(\d+)([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)/g, '$1 $2');
    clean = clean.replace(/([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)(\d+)/g, '$1 $2');

    // 2. Normalizar prefijos y abreviaturas comunes en direcciones de Perú
    clean = clean.replace(/\bC\.\s*/gi, 'Calle ');
    clean = clean.replace(/\bCl\.\s*/gi, 'Calle ');
    clean = clean.replace(/\bAv\.\s*/gi, 'Avenida ');
    clean = clean.replace(/\bAvda\.\s*/gi, 'Avenida ');
    clean = clean.replace(/\bJr\.\s*/gi, 'Jirón ');
    clean = clean.replace(/\bPje\.\s*/gi, 'Pasaje ');
    clean = clean.replace(/\bMz\.\s*/gi, 'Manzana ');
    clean = clean.replace(/\bLt\.\s*/gi, 'Lote ');
    clean = clean.replace(/\bUrb\.\s*/gi, 'Urbanización ');
    clean = clean.replace(/\bVMT\b/gi, 'Villa María del Triunfo');
    clean = clean.replace(/\bSJM\b/gi, 'San Juan de Miraflores');
    clean = clean.replace(/\bSJL\b/gi, 'San Juan de Lurigancho');

    // 3. Limpiar espacios redundantes
    clean = clean.replace(/\s+/g, ' ').trim();

    return clean;
  }

  /**
   * Busca direcciones con estrategia multi-fase:
   * 1. Normalización de consulta
   * 2. Búsqueda difusa en Photon (con sesgo geográfico de Perú)
   * 3. Fallback a Nominatim OSM
   * 4. Búsqueda por vía/distrito sin numeración si la dirección exacta no figura en el mapa
   */
  search(query: string, userLat = -12.046374, userLon = -77.042793): Observable<GeocodingResult[]> {
    const normalized = this.normalizeQuery(query);
    if (normalized.length < 3) {
      this.results.set([]);
      this.searchError.set(null);
      return of([]);
    }

    this.isSearching.set(true);
    this.searchError.set(null);

    const encoded = encodeURIComponent(normalized);
    const photonUrl = `https://photon.komoot.io/api/?q=${encoded}&limit=6&lat=${userLat}&lon=${userLon}`;

    return this.http.get<any>(photonUrl).pipe(
      map((response) => this.mapPhotonFeatures(response?.features, normalized, userLat, userLon)),
      switchMap((photonResults) => {
        if (photonResults.length > 0) {
          return of(photonResults);
        }

        const nominatimUrl = `https://nominatim.openstreetmap.org/search?format=json&q=${encoded}&countrycodes=pe&limit=6&addressdetails=1`;
        return this.http.get<any[]>(nominatimUrl).pipe(
          map((items) => this.mapNominatimItems(items)),
          switchMap((nomResults) => {
            if (nomResults.length > 0) {
              return of(nomResults);
            }

            const queryWithoutNumbers = normalized.replace(/\b\d{1,6}\b/g, '').replace(/\s+/g, ' ').trim();
            if (queryWithoutNumbers.length >= 4 && queryWithoutNumbers !== normalized) {
              const encFallback = encodeURIComponent(queryWithoutNumbers);
              const fallbackPhoton = `https://photon.komoot.io/api/?q=${encFallback}&limit=5&lat=${userLat}&lon=${userLon}`;
              return this.http.get<any>(fallbackPhoton).pipe(
                map((fbRes) => this.mapPhotonFeatures(fbRes?.features, queryWithoutNumbers, userLat, userLon)),
                catchError(() => of([])),
              );
            }

            return of([]);
          }),
          catchError(() => of([])),
        );
      }),
      tap((results) => {
        this.isSearching.set(false);
        this.results.set(results);
      }),
      catchError(() => {
        this.isSearching.set(false);
        this.searchError.set('No se pudo completar la búsqueda de dirección.');
        this.results.set([]);
        return of([]);
      }),
    );
  }

  reverseGeocode(latitude: number, longitude: number): Observable<GeocodingResult | null> {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`;
    return this.http.get<any>(url).pipe(
      map((item) => {
        if (!item || !item.display_name) return null;
        const addr = item.address || {};

        const street = addr.road || addr.pedestrian || addr.footway || addr.path || addr.amenity || addr.building || '';
        const houseNumber = addr.house_number ? ` ${addr.house_number}` : '';
        const streetWithNumber = street ? `${street}${houseNumber}` : '';
        const zone = addr.neighbourhood || addr.quarter || addr.residential || '';
        const district = addr.suburb || addr.city_district || addr.district || addr.town || addr.village || '';
        const city = addr.city || addr.county || addr.province || addr.state || 'Lima';
        const state = addr.state;

        let formatted: string;
        if (streetWithNumber || zone || district) {
          formatted = this.cleanAddressSegments([
            streetWithNumber,
            zone,
            district,
            city,
            addr.country || 'Perú',
          ]);
        } else {
          formatted = this.cleanDisplayName(item.display_name);
        }

        return {
          displayName: formatted,
          latitude: parseFloat(item.lat),
          longitude: parseFloat(item.lon),
          road: street,
          suburb: district || zone,
          city: city,
          state: state,
        };
      }),
      catchError(() => of(null)),
    );
  }

  clearResults(): void {
    this.results.set([]);
    this.searchError.set(null);
  }

  /**
   * Limpia y deduplica segmentos de dirección eliminando códigos postales,
   * repeticiones de Lima / Lima Metropolitana y términos redundantes.
   */
  public cleanAddressSegments(segments: (string | undefined | null)[]): string {
    const validSegments = segments
      .filter((s): s is string => typeof s === 'string' && s.trim().length > 0)
      .map((s) => s.trim());

    const seenNormalized = new Set<string>();
    const result: string[] = [];

    for (const seg of validSegments) {
      // Omitir códigos postales (ej. 15818)
      if (/^\d{4,6}$/.test(seg)) {
        continue;
      }

      // Normalizar para comparación
      const normalized = seg
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\b(provincia|departamento|region|municipalidad|metropolitana|gobierno regional)\b/g, '')
        .replace(/\s+/g, ' ')
        .trim();

      if (!normalized) continue;

      // Si ya tenemos una coincidencia similar (ej. "lima" vs "lima metropolitana"), omitir
      let isDuplicate = false;
      for (const seen of seenNormalized) {
        if (seen === normalized || (seen.length > 2 && normalized.length > 2 && (seen.includes(normalized) || normalized.includes(seen)))) {
          isDuplicate = true;
          break;
        }
      }

      if (!isDuplicate) {
        seenNormalized.add(normalized);
        result.push(seg);
      }
    }

    return result.join(', ');
  }

  /**
   * Limpia una cadena display_name completa de Nominatim u OSM.
   */
  public cleanDisplayName(rawDisplayName: string): string {
    if (!rawDisplayName) return '';
    const rawParts = rawDisplayName.split(',').map((p) => p.trim());
    return this.cleanAddressSegments(rawParts);
  }

  private mapPhotonFeatures(features: any[], fallbackQuery: string, userLat: number, userLon: number): GeocodingResult[] {
    if (!Array.isArray(features) || features.length === 0) return [];
    return features.map((f: any) => {
      const props = f.properties || {};
      const coords = f.geometry?.coordinates || [userLon, userLat];
      const street = props.street || props.name;
      const houseNumber = props.housenumber ? ` ${props.housenumber}` : '';
      const district = props.district || props.suburb;
      const city = props.city || props.state || 'Lima';
      const country = props.country || 'Perú';

      const fullStreet = street ? `${street}${houseNumber}` : '';
      const formatted = this.cleanAddressSegments([fullStreet, district, city, country]);

      return {
        displayName: formatted || props.name || fallbackQuery,
        latitude: Number(coords[1]),
        longitude: Number(coords[0]),
        road: street,
        suburb: props.district || props.suburb,
        city: props.city || props.state,
        state: props.state,
      };
    });
  }

  private mapNominatimItems(items: any[]): GeocodingResult[] {
    if (!Array.isArray(items)) return [];
    return items.map((item) => ({
      displayName: this.cleanDisplayName(item.display_name),
      latitude: parseFloat(item.lat),
      longitude: parseFloat(item.lon),
      road: item.address?.road,
      suburb: item.address?.suburb ?? item.address?.neighbourhood,
      city: item.address?.city ?? item.address?.town ?? item.address?.village,
      state: item.address?.state,
    }));
  }
}
