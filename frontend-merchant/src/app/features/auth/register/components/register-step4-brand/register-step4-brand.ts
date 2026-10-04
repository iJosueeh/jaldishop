import { Component, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';
import { environment } from '../../../../../../environments/environment';
import { isValidImageUrl } from '../../../../../core/utils/image.utils';

@Component({
  selector: 'app-register-step4-brand',
  standalone: true,
  imports: [CommonModule, BusinessAvatar],
  styleUrl: './register-step4-brand.css',
  templateUrl: './register-step4-brand.html',
})
export class RegisterStep4Brand {
  readonly storeName = input<string>('Dulces Clara');
  readonly businessType = input<string>('Repostería y pastelería');
  readonly logoUrl = input<string | undefined>('');
  readonly bannerUrl = input<string | undefined>('');
  readonly dailyOrderLimit = input<number | null>(12);
  readonly prepTime = input<string>('30 - 60 min');
  readonly openingTime = input<string>('09:00');
  readonly closingTime = input<string>('19:00');
  readonly operatingDays = input<string[]>(['L', 'M', 'X', 'J', 'V', 'S']);

  readonly maxCapacity = 20;
  readonly segmentBlocks = Array.from({ length: 20 }, (_, i) => i);

  readonly lastFailedLogoUrl = signal<string | null>(null);
  readonly lastFailedBannerUrl = signal<string | null>(null);

  readonly marketplaceUrl = environment.marketplaceUrl || 'http://localhost:3000';

  hasValidLogo(): boolean {
    const url = this.logoUrl();
    return isValidImageUrl(url) && this.lastFailedLogoUrl() !== url;
  }

  hasValidBanner(): boolean {
    const url = this.bannerUrl();
    return isValidImageUrl(url) && this.lastFailedBannerUrl() !== url;
  }

  onLogoError(): void {
    this.lastFailedLogoUrl.set(this.logoUrl() ?? null);
  }

  onBannerError(): void {
    this.lastFailedBannerUrl.set(this.bannerUrl() ?? null);
  }
}
