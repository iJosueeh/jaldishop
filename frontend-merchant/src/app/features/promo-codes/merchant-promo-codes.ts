import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { PromoCodesManager } from '../../shared/components/promo-codes-manager/promo-codes-manager';
import { StoreService } from '../../core/services/store.service';

@Component({
  selector: 'app-merchant-promo-codes',
  imports: [PromoCodesManager],
  template: `
    @if (storeId()) {
      <app-promo-codes-manager mode="MERCHANT" [storeId]="storeId()" />
    } @else {
      <div class="p-8 text-center text-sm text-on-surface-variant">
        Cargando tienda...
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MerchantPromoCodes {
  private readonly storeService = inject(StoreService);
  readonly storeId = computed(() => this.storeService.currentStore()?.id ?? null);
}
