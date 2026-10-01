import { Component, inject, input, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocationOnOutline,
  matPinDropOutline,
  matNearMeOutline,
  matWarningOutline,
  matMyLocationOutline,
  matTuneOutline,
  matCheckCircleOutline,
} from '@ng-icons/material-symbols/outline';
import { LocationPickerMapComponent } from '../../../../shared/components/location-picker-map/location-picker-map';
import { GeolocationService } from '../../../../core/services/geolocation.service';

import { GeocodingResult } from '../../../../core/services/geocoding.service';

@Component({
  selector: 'app-store-location-card',
  standalone: true,
  imports: [ReactiveFormsModule, NgIcon, LocationPickerMapComponent],
  providers: [
    provideIcons({
      matLocationOnOutline,
      matPinDropOutline,
      matNearMeOutline,
      matWarningOutline,
      matMyLocationOutline,
      matTuneOutline,
      matCheckCircleOutline,
    }),
  ],
  styleUrl: './store-location-card.css',
  templateUrl: './store-location-card.html',
})
export class StoreLocationCard {
  private readonly geoService = inject(GeolocationService);

  readonly form = input<FormGroup>(new FormGroup({}));
  readonly deliveryRadiusKm = input<number | null | undefined>(null);

  readonly isLocating = this.geoService.isLocating;
  readonly geoError = this.geoService.errorMessage;
  readonly showManualGps = signal<boolean>(false);

  toggleManualGps(): void {
    this.showManualGps.update((prev) => !prev);
  }

  async onUseCurrentLocation(): Promise<void> {
    try {
      const pos = await this.geoService.getCurrentPosition();
      this.form().patchValue({
        latitude: Number(pos.latitude.toFixed(6)),
        longitude: Number(pos.longitude.toFixed(6)),
      });
      this.form().markAsDirty();
    } catch {
      // Error manejado en el signal geoError
    }
  }

  onCoordinatesChange(coords: { lat: number; lng: number }): void {
    this.form().patchValue({
      latitude: coords.lat,
      longitude: coords.lng,
    });
    this.form().markAsDirty();
  }

  onAddressSelected(result: GeocodingResult): void {
    const addressControl = this.form().get('address');
    if (addressControl && !addressControl.value) {
      addressControl.setValue(result.displayName);
      this.form().markAsDirty();
    }
  }
}
