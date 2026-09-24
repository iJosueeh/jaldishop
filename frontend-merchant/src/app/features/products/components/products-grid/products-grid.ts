import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matFilterAltOffOutline,
  matInventory2Outline,
} from '@ng-icons/material-symbols/outline';
import { ProductCard } from './product-card/product-card';
import { Product } from '../../../../core/models/product.models';

@Component({
  selector: 'app-products-grid',
  standalone: true,
  imports: [CommonModule, ProductCard, NgIcon],
  viewProviders: [
    provideIcons({
      matAddOutline,
      matFilterAltOffOutline,
      matInventory2Outline,
    }),
  ],
  templateUrl: './products-grid.html',
  styleUrl: './products-grid.css',
})
export class ProductsGrid {
  products = input<Product[]>([]);
  isLoading = input<boolean>(false);
  isEmptyCatalog = input<boolean>(false);
  isFilterEmpty = input<boolean>(false);

  editProduct = output<Product>();
  adjustQuota = output<Product>();
  toggleStatus = output<Product>();
  createNewProduct = output<void>();
  clearFilters = output<void>();
}
