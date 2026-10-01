import {
  Component,
  ElementRef,
  ChangeDetectionStrategy,
  input,
  output,
  viewChild,
  afterNextRender,
  DestroyRef,
  inject,
  effect,
  signal,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matSearchOutline,
  matCloseOutline,
  matLocationOnOutline,
  matMyLocationOutline,
} from '@ng-icons/material-symbols/outline';
import * as L from 'leaflet';
import { GeocodingResult, GeocodingService } from '../../../core/services/geocoding.service';

@Component({
  selector: 'app-location-picker-map',
  standalone: true,
  imports: [NgIcon],
  providers: [
    provideIcons({
      matSearchOutline,
      matCloseOutline,
      matLocationOnOutline,
      matMyLocationOutline,
    }),
  ],
  templateUrl: './location-picker-map.html',
  styleUrl: './location-picker-map.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationPickerMapComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly geocodingService = inject(GeocodingService);

  readonly mapContainer = viewChild<ElementRef<HTMLDivElement>>('mapContainer');

  // Signal Inputs declarativos
  readonly latitude = input<number | null | undefined>(null);
  readonly longitude = input<number | null | undefined>(null);
  readonly radiusKm = input<number | null | undefined>(null);
  readonly initialSearchQuery = input<string | null | undefined>(null);
  readonly interactive = input<boolean>(true);
  readonly zoom = input<number>(15);

  // Signal Outputs
  readonly coordinatesChange = output<{ lat: number; lng: number }>();
  readonly addressSelected = output<GeocodingResult>();

  // Estado reactivo de búsqueda
  readonly searchQuery = signal<string>('');
  readonly isSearching = this.geocodingService.isSearching;
  readonly searchResults = this.geocodingService.results;
  readonly showResults = signal<boolean>(false);

  // Instancias de Leaflet
  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private circle: L.Circle | null = null;
  private isReady = false;

  private readonly DEFAULT_LAT = -12.046374;
  private readonly DEFAULT_LNG = -77.042793;

  private readonly pinIcon = L.divIcon({
    className: 'jaldishop-map-pin',
    html: `
      <div style="width: 36px; height: 48px; cursor: grab; display: flex; align-items: center; justify-content: center;">
        <svg xmlns="http://www.w3.org/2000/svg" width="36" height="48" viewBox="0 0 36 48" fill="none" style="overflow: visible;">
          <defs>
            <filter id="jaldishop-pin-shadow" x="-30%" y="-15%" width="160%" height="150%">
              <feDropShadow dx="0" dy="4" stdDeviation="3.5" flood-color="#000000" flood-opacity="0.38"/>
            </filter>
          </defs>
          <path filter="url(#jaldishop-pin-shadow)" d="M18 2C9.163 2 2 9.163 2 18C2 28.5 18 46 18 46C18 46 34 28.5 34 18C34 9.163 26.837 2 18 2Z" fill="#ea580c" stroke="#ffffff" stroke-width="2.5" stroke-linejoin="round"/>
          <circle cx="18" cy="18" r="6" fill="#ffffff"/>
        </svg>
      </div>
    `,
    iconSize: [36, 48],
    iconAnchor: [18, 46],
  });

  private resizeObserver: ResizeObserver | null = null;
  private lastEmittedLat: number | null = null;
  private lastEmittedLng: number | null = null;

  constructor() {
    afterNextRender(() => {
      this.initLeaflet();
    });

    effect(() => {
      const initial = this.initialSearchQuery();
      if (initial && !this.searchQuery()) {
        this.searchQuery.set(initial);
      }
    });

    effect(() => {
      const lat = this.latitude();
      const lng = this.longitude();
      if (this.isReady && typeof lat === 'number' && typeof lng === 'number') {
        const isExternalUpdate = lat !== this.lastEmittedLat || lng !== this.lastEmittedLng;
        if (isExternalUpdate) {
          this.setMarker(lat, lng, true);
          this.fetchReverseGeocode(lat, lng);
        }
      }
    });

    effect(() => {
      const radius = this.radiusKm();
      const lat = this.latitude();
      const lng = this.longitude();
      if (this.isReady) {
        this.updateRadiusCircle(lat, lng, radius);
      }
    });

    this.destroyRef.onDestroy(() => {
      if (this.resizeObserver) {
        this.resizeObserver.disconnect();
        this.resizeObserver = null;
      }
      if (this.map) {
        this.map.remove();
        this.map = null;
      }
    });
  }

  recenterOnMarker(): void {
    if (!this.map || !this.marker) return;
    const pos = this.marker.getLatLng();
    this.map.flyTo(pos, Math.max(this.map.getZoom(), 15), { duration: 0.6 });
  }

  closeSearchResults(): void {
    this.showResults.set(false);
  }

  private initLeaflet(): void {
    const container = this.mapContainer()?.nativeElement;
    if (!container || this.map) return;

    const initialLat = this.latitude() ?? this.DEFAULT_LAT;
    const initialLng = this.longitude() ?? this.DEFAULT_LNG;

    this.map = L.map(container, {
      center: [initialLat, initialLng],
      zoom: this.zoom(),
      zoomControl: false,
      zoomAnimation: true,
      fadeAnimation: true,
      markerZoomAnimation: true,
      zoomSnap: 0.5,
      zoomDelta: 0.5,
      wheelPxPerZoomLevel: 100,
    });

    // Capa OpenStreetMap 100% gratuita y sin requerimiento de API Key
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
      maxZoom: 19,
      crossOrigin: true,
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);

    // Colocar el marcador de forma garantizada en el mapa
    this.setMarker(initialLat, initialLng, false);

    if (typeof ResizeObserver !== 'undefined') {
      this.resizeObserver = new ResizeObserver(() => {
        this.map?.invalidateSize();
      });
      this.resizeObserver.observe(container);
    }

    if (this.interactive()) {
      this.map.on('click', (e: L.LeafletMouseEvent) => {
        const { lat, lng } = e.latlng;
        const fixedLat = Number(lat.toFixed(6));
        const fixedLng = Number(lng.toFixed(6));
        this.lastEmittedLat = fixedLat;
        this.lastEmittedLng = fixedLng;
        this.setMarker(fixedLat, fixedLng, false);
        this.showResults.set(false);
        this.coordinatesChange.emit({
          lat: fixedLat,
          lng: fixedLng,
        });
        this.fetchReverseGeocode(fixedLat, fixedLng);
      });
    }

    this.isReady = true;

    const r = this.radiusKm();
    if (r != null && this.latitude() != null && this.longitude() != null) {
      this.updateRadiusCircle(this.latitude(), this.longitude(), r);
    }

    setTimeout(() => this.map?.invalidateSize(), 150);
  }

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery.set(target.value);
  }

  onSearchSubmit(): void {
    const query = this.searchQuery();
    if (query.trim().length >= 3) {
      this.showResults.set(true);
      const center = this.map ? this.map.getCenter() : { lat: this.DEFAULT_LAT, lng: this.DEFAULT_LNG };
      this.geocodingService.search(query, center.lat, center.lng).subscribe();
    }
  }

  onSelectResult(result: GeocodingResult): void {
    const fixedLat = Number(result.latitude.toFixed(6));
    const fixedLng = Number(result.longitude.toFixed(6));
    this.lastEmittedLat = fixedLat;
    this.lastEmittedLng = fixedLng;
    this.searchQuery.set(result.displayName);
    this.showResults.set(false);
    this.setMarker(fixedLat, fixedLng, true);
    this.coordinatesChange.emit({
      lat: fixedLat,
      lng: fixedLng,
    });
    this.addressSelected.emit(result);
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.showResults.set(false);
    this.geocodingService.clearResults();
  }

  private fetchReverseGeocode(lat: number, lng: number): void {
    this.geocodingService.reverseGeocode(lat, lng).subscribe((res) => {
      if (res) {
        this.searchQuery.set(res.displayName);
      }
    });
  }

  private setMarker(lat: number, lng: number, animate: boolean): void {
    if (!this.map) return;

    if (!this.marker) {
      this.marker = L.marker([lat, lng], {
        icon: this.pinIcon,
        draggable: this.interactive(),
        autoPan: true,
      }).addTo(this.map);

      this.marker.on('dragend', () => {
        const pos = this.marker?.getLatLng();
        if (pos) {
          const fixedLat = Number(pos.lat.toFixed(6));
          const fixedLng = Number(pos.lng.toFixed(6));
          this.lastEmittedLat = fixedLat;
          this.lastEmittedLng = fixedLng;
          this.coordinatesChange.emit({
            lat: fixedLat,
            lng: fixedLng,
          });
          this.fetchReverseGeocode(fixedLat, fixedLng);
        }
      });
    } else {
      this.marker.setLatLng([lat, lng]);
    }

    if (animate) {
      this.map.flyTo([lat, lng], 16, { duration: 0.8 });
    }
  }

  private updateRadiusCircle(
    lat: number | null | undefined,
    lng: number | null | undefined,
    radiusKm: number | null | undefined,
  ): void {
    if (!this.map) return;

    if (this.circle) {
      this.map.removeLayer(this.circle);
      this.circle = null;
    }

    if (lat != null && lng != null && radiusKm != null && radiusKm > 0) {
      this.circle = L.circle([lat, lng], {
        radius: radiusKm * 1000,
        color: '#ea580c',
        fillColor: '#ea580c',
        fillOpacity: 0.12,
        weight: 1.5,
        dashArray: '4, 6',
      }).addTo(this.map);
    }
  }
}
