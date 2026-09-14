import { Component, computed, input } from '@angular/core';

@Component({
  imports: [],
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

  readonly initialLetter = computed(() => 
    this.storeName()?.trim().charAt(0).toUpperCase() || 'B'
  );

  readonly segmentBlocks = Array.from({ length: 12 }, (_, i) => i);
}
