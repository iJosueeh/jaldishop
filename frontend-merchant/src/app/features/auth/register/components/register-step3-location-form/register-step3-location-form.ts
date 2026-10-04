import { Component, inject, input, OnInit, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { outputFromObservable, toSignal } from '@angular/core/rxjs-interop';
import { RegisterFooter } from '../../../../../shared/components/register-footer/register-footer';
import { RegisterStepHeader } from '../../../../../shared/components/register-step-header/register-step-header';
import { AlertError } from '../../../../../shared/components/alert-error/alert-error';
import { RegisterStep3Data } from '../../../../../core/models/register.models';
import { LocationPickerMapComponent } from '../../../../../shared/components/location-picker-map/location-picker-map';
import { GeolocationService } from '../../../../../core/services/geolocation.service';
import { GeocodingResult } from '../../../../../core/services/geocoding.service';

@Component({
  selector: 'app-register-step3-location-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RegisterFooter,
    RegisterStepHeader,
    AlertError,
    LocationPickerMapComponent,
  ],
  styleUrl: './register-step3-location-form.css',
  templateUrl: './register-step3-location-form.html',
})
export class RegisterStep3LocationForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly geoService = inject(GeolocationService);

  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);
  readonly initialData = input<RegisterStep3Data | null>(null);
  readonly isExistingUser = input<boolean>(false);

  readonly back = output<void>();
  readonly step3Submit = output<RegisterStep3Data>();
  readonly skip = output<void>();

  readonly isLocating = this.geoService.isLocating;
  readonly geoError = this.geoService.errorMessage;

  readonly step3Form: FormGroup = this.fb.group(
    {
      pickupEnabled: [true],
      deliveryEnabled: [true],
      address: [''],
      addressReference: [''],
      latitude: [null as number | null],
      longitude: [null as number | null],
    },
    { validators: atLeastOneDeliveryMethodValidator },
  );

  ngOnInit(): void {
    const data = this.initialData();
    if (data) {
      this.step3Form.patchValue(data);
    }
  }

  readonly formChange = outputFromObservable<RegisterStep3Data>(this.step3Form.valueChanges);

  readonly formValues = toSignal(this.step3Form.valueChanges, {
    initialValue: this.step3Form.getRawValue(),
  });

  async onUseCurrentLocation(): Promise<void> {
    try {
      const pos = await this.geoService.getCurrentPosition();
      this.step3Form.patchValue({
        latitude: Number(pos.latitude.toFixed(6)),
        longitude: Number(pos.longitude.toFixed(6)),
      });
      this.step3Form.markAsDirty();
    } catch {
      // Error manejado en el signal geoError de GeolocationService
    }
  }

  onCoordinatesChange(coords: { lat: number; lng: number }): void {
    this.step3Form.patchValue({
      latitude: Number(coords.lat.toFixed(6)),
      longitude: Number(coords.lng.toFixed(6)),
    });
    this.step3Form.markAsDirty();
  }

  onAddressSelected(result: GeocodingResult): void {
    const currentAddress = this.step3Form.get('address')?.value;
    if (!currentAddress || currentAddress.trim() === '') {
      this.step3Form.patchValue({ address: result.displayName });
      this.step3Form.markAsDirty();
    }
  }

  onBack(): void {
    this.back.emit();
  }

  handleContinue(): void {
    if (this.step3Form.invalid || this.isLoading()) {
      this.step3Form.markAllAsTouched();
      return;
    }
    this.step3Submit.emit(this.step3Form.getRawValue());
  }

  handleSkip(): void {
    this.skip.emit();
  }
}

export const atLeastOneDeliveryMethodValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const pickup = control.get('pickupEnabled')?.value;
  const delivery = control.get('deliveryEnabled')?.value;
  return !pickup && !delivery ? { noDeliveryMethod: true } : null;
};
