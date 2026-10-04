import { Component } from '@angular/core';
import { environment } from '../../../../../../environments/environment';

import { BusinessAvatar } from '../../../../../shared/components/business-avatar/business-avatar';

@Component({
  imports: [BusinessAvatar],
  selector: 'app-register-brand-panel',
  styleUrl: './register-brand-panel.css',
  templateUrl: './register-brand-panel.html',
})
export class RegisterBrandPanel {
  readonly marketplaceUrl = environment.marketplaceUrl;
}
