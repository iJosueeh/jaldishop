import { Service, signal } from '@angular/core';

export interface GeoCoordinates {
  latitude: number;
  longitude: number;
  accuracy?: number;
}

@Service()
export class GeolocationService {
  readonly isLocating = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  async getCurrentPosition(): Promise<GeoCoordinates> {
    this.isLocating.set(true);
    this.errorMessage.set(null);

    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      const msg = 'La geolocalización no es compatible con este navegador.';
      this.errorMessage.set(msg);
      this.isLocating.set(false);
      throw new Error(msg);
    }

    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.isLocating.set(false);
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
          });
        },
        (error) => {
          this.isLocating.set(false);
          let userMsg = 'No se pudo obtener la ubicación actual.';
          if (error.code === error.PERMISSION_DENIED) {
            userMsg = 'Permiso de ubicación denegado por el navegador.';
          } else if (error.code === error.POSITION_UNAVAILABLE) {
            userMsg = 'Información de ubicación no disponible en tu dispositivo.';
          } else if (error.code === error.TIMEOUT) {
            userMsg = 'El tiempo de espera para obtener la ubicación se ha agotado.';
          }
          this.errorMessage.set(userMsg);
          reject(new Error(userMsg));
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
      );
    });
  }
}
