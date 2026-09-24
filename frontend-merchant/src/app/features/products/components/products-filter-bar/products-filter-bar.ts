import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matArrowDropDownOutline,
  matGridViewOutline,
  matSearchOutline,
  matStorefrontOutline,
  matViewAgendaOutline,
} from '@ng-icons/material-symbols/outline';
import { ProductCategory, ProductViewMode } from '../../../../core/models/product.models';

@Component({
  selector: 'app-products-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon],
  viewProviders: [
    provideIcons({
      matArrowDropDownOutline,
      matGridViewOutline,
      matSearchOutline,
      matStorefrontOutline,
      matViewAgendaOutline,
    }),
  ],
  templateUrl: './products-filter-bar.html',
  styleUrl: './products-filter-bar.css',
})
export class ProductsFilterBar {
  categories = input<ProductCategory[]>([]);
  selectedCategoryId = input<string | null>(null);
  totalCount = input<number>(0);
  searchQuery = input<string>('');
  viewMode = input<ProductViewMode>('grid');
  selectedStatus = input<string>('all');

  categoryChange = output<string | null>();
  searchChange = output<string>();
  statusChange = output<string>();
  viewModeChange = output<ProductViewMode>();

  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchChange.emit(target.value);
  }
}
