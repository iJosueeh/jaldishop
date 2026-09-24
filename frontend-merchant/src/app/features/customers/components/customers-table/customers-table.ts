import { Component, input, output } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { StoreCustomer } from '../../../../core/models/customer.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matPersonOutline,
  matChatOutline,
  matReceiptLongOutline,
  matCalendarTodayOutline,
  matCallOutline,
  matMailOutline,
  matStarOutline,
  matSearchOffOutline,
  matOpenInNewOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-customers-table',
  imports: [CommonModule, NgIcon, CurrencyPipe, DatePipe],
  providers: [
    provideIcons({
      matPersonOutline,
      matChatOutline,
      matReceiptLongOutline,
      matCalendarTodayOutline,
      matCallOutline,
      matMailOutline,
      matStarOutline,
      matSearchOffOutline,
      matOpenInNewOutline,
    }),
  ],
  templateUrl: './customers-table.html',
  styleUrl: './customers-table.css',
})
export class CustomersTable {
  readonly customers = input.required<StoreCustomer[]>();
  readonly isLoading = input<boolean>(false);

  readonly openWhatsApp = output<StoreCustomer>();

  getInitials(firstName: string, lastName: string): string {
    const f = firstName?.trim().charAt(0) || '';
    const l = lastName?.trim().charAt(0) || '';
    return (f + l).toUpperCase() || 'CL';
  }

  getSegment(customer: StoreCustomer): { label: string; class: string } {
    if (customer.ordersCount >= 5 || customer.totalSpentAmount >= 150) {
      return {
        label: 'VIP',
        class: 'bg-amber-50 text-amber-800 border-amber-200/60 font-semibold',
      };
    }
    if (customer.ordersCount >= 2) {
      return {
        label: 'Frecuente',
        class: 'bg-emerald-50 text-emerald-800 border-emerald-200/60 font-medium',
      };
    }
    return {
      label: 'Nuevo',
      class: 'bg-stone-100 text-stone-700 border-stone-200 font-normal',
    };
  }

  getWhatsAppLink(phone: string | null, firstName: string): string {
    if (!phone) return '#';
    const cleanPhone = phone.replace(/\D/g, '');
    const phoneWithCountry = cleanPhone.startsWith('51') ? cleanPhone : `51${cleanPhone}`;
    const text = encodeURIComponent(`¡Hola ${firstName}! Te saludamos de la tienda.`);
    return `https://wa.me/${phoneWithCountry}?text=${text}`;
  }

  onWhatsAppClick(customer: StoreCustomer, event: MouseEvent): void {
    if (!customer.phone) {
      event.preventDefault();
      return;
    }
    this.openWhatsApp.emit(customer);
  }
}
