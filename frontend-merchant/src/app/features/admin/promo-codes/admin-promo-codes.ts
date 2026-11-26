import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PromoCodesManager } from '../../../shared/components/promo-codes-manager/promo-codes-manager';

@Component({
  selector: 'app-admin-promo-codes',
  imports: [PromoCodesManager],
  template: `<app-promo-codes-manager mode="ADMIN" />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminPromoCodes {}
