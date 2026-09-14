import { Component, input } from '@angular/core';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';

@Component({
  imports: [BusinessAvatar],
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
}
