import { Component, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';
import { environment } from '../../../../../../environments/environment';
import { isValidImageUrl } from '../../../../../core/utils/image.utils';

@Component({
  selector: 'app-register-step3-location-brand',
  standalone: true,
  imports: [CommonModule, BusinessAvatar],
  styleUrl: './register-step3-location-brand.css',
  templateUrl: './register-step3-location-brand.html',
})
export class RegisterStep3LocationBrand {
  readonly storeName = input<string>('');
  readonly businessType = input<string>('');
  readonly logoUrl = input<string | undefined>('');
  readonly bannerUrl = input<string | undefined>('');
  readonly pickupEnabled = input<boolean>(true);
  readonly deliveryEnabled = input<boolean>(true);
  readonly address = input<string>('');
  readonly addressReference = input<string>('');
  readonly latitude = input<number | null | undefined>(null);
  readonly longitude = input<number | null | undefined>(null);

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
