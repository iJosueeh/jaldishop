import { Component, input } from '@angular/core';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';

@Component({
  imports: [BusinessAvatar],
  selector: 'app-register-step3-brand',
  styleUrl: './register-step3-brand.css',
  templateUrl: './register-step3-brand.html',
})
export class RegisterStep3Brand {
  readonly storeName = input<string>('Dulces Clara');
  readonly businessType = input<string>('Reposteria y pasteleria');
  readonly dailyOrderLimit = input<number | null>(12);
  readonly prepTime = input<string>('30 - 60 min');
  readonly openingTime = input<string>('09:00');
  readonly closingTime = input<string>('19:00');

  readonly maxCapacity = 20;
  readonly segmentBlocks = Array.from({ length: 20 }, (_, i) => i);
}

