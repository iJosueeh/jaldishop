import { Component, computed, input } from '@angular/core';

export interface RegisterStep2Data {
  name: string;
  businessType: string;
  contactPhone: string;
  pickupEnabled: boolean;
  deliveryEnabled: boolean;
  address?: string;
}

@Component({
  imports: [],
  selector: 'app-register-step2-brand',
  styleUrl: './register-step2-brand.css',
  templateUrl: './register-step2-brand.html',
})
export class RegisterStep2Brand {
  readonly storeName = input<string>('Dulces Clara');
  readonly businessType = input<string>('Reposteria y pasteleria');
  readonly contactPhone = input<string>('999 999 999');
  readonly pickupEnabled = input<boolean>(true);
  readonly deliveryEnabled = input<boolean>(true);

  readonly initialLetter = computed(() => 
    this.storeName()?.trim().charAt(0).toUpperCase() || 'B'
  );
}
