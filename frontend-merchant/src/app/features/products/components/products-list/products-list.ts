import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matEditOutline,
  matFilterAltOffOutline,
  matInventory2Outline,
  matMoreVertOutline,
  matVisibilityOffOutline,
  matVisibilityOutline,
} from '@ng-icons/material-symbols/outline';
import { Product } from '../../../../core/models/product.models';

@Component({
  selector: 'app-products-list',
  standalone: true,
  imports: [CommonModule, NgIcon],
  viewProviders: [
    provideIcons({
      matAddOutline,
      matEditOutline,
      matFilterAltOffOutline,
      matInventory2Outline,
      matMoreVertOutline,
      matVisibilityOffOutline,
      matVisibilityOutline,
    }),
  ],
  templateUrl: './products-list.html',
  styleUrl: './products-list.css',
})
export class ProductsList {
  products = input<Product[]>([]);
  isLoading = input<boolean>(false);
  isEmptyCatalog = input<boolean>(false);
  isFilterEmpty = input<boolean>(false);

  editProduct = output<Product>();
  toggleStatus = output<Product>();
  createNewProduct = output<void>();
  clearFilters = output<void>();

  getPrice(p: Product): string {
    if (p.minPrice != null) return `S/ ${p.minPrice.toFixed(2)}`;
    if (p.variants && p.variants.length > 0) return `S/ ${p.variants[0].priceAmount.toFixed(2)}`;
    return 'S/ 0.00';
  }
}
