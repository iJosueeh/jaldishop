import { Component, input } from '@angular/core';
import { CustomerKpis } from '../../../../core/models/customer.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matGroupOutline,
  matRepeatOutline,
  matReceiptOutline,
  matTrendingUpOutline,
} from '@ng-icons/material-symbols/outline';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-customers-kpis',
  imports: [NgIcon, DecimalPipe],
  providers: [
    provideIcons({
      matGroupOutline,
      matRepeatOutline,
      matReceiptOutline,
      matTrendingUpOutline,
    }),
  ],
  templateUrl: './customers-kpis.html',
  styleUrl: './customers-kpis.css',
})
export class CustomersKpis {
  readonly kpis = input.required<CustomerKpis>();
}
