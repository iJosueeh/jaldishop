import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matMoreHorizOutline,
  matTuneOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  selector: 'app-products-header',
  standalone: true,
  imports: [CommonModule, NgIcon],
  viewProviders: [
    provideIcons({
      matAddOutline,
      matMoreHorizOutline,
      matTuneOutline,
    }),
  ],
  templateUrl: './products-header.html',
  styleUrl: './products-header.css',
})
export class ProductsHeader {
  activeCount = input<number>(0);

  createNewProduct = output<void>();
  adjustAvailability = output<void>();
}
