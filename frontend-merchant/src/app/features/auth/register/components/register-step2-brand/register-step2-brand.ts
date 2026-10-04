import { Component, input, signal } from '@angular/core';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';
import { environment } from '../../../../../../environments/environment';
import { isValidImageUrl } from '../../../../../core/utils/image.utils';

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
  readonly logoUrl = input<string>('');
  readonly bannerUrl = input<string>('');

  readonly lastFailedLogoUrl = signal<string | null>(null);
  readonly lastFailedBannerUrl = signal<string | null>(null);

  readonly marketplaceUrl = environment.marketplaceUrl;

  hasValidLogo(): boolean {
    const url = this.logoUrl();
    return isValidImageUrl(url) && this.lastFailedLogoUrl() !== url;
  }

  hasValidBanner(): boolean {
    const url = this.bannerUrl();
    return isValidImageUrl(url) && this.lastFailedBannerUrl() !== url;
  }

  onLogoError(): void {
    this.lastFailedLogoUrl.set(this.logoUrl());
  }

  onBannerError(): void {
    this.lastFailedBannerUrl.set(this.bannerUrl());
  }
}
