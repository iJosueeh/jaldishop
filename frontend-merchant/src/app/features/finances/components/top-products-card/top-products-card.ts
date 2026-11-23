import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matInventory2Outline,
  matTrendingUpOutline,
  matStarOutline,
} from '@ng-icons/material-symbols/outline';
import { AnalyticsService } from '../../../../core/services/analytics.service';
import { ProductSalesRank } from '../../../../core/models/analytics.models';

@Component({
  selector: 'app-top-products-card',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matInventory2Outline,
      matTrendingUpOutline,
      matStarOutline,
    }),
  ],
  template: `
    <div class="p-5 rounded-2xl bg-card border border-border shadow-xs flex flex-col gap-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
            <ng-icon name="matStarOutline" class="text-lg" />
          </div>
          <div>
            <h3 class="text-base font-bold text-foreground">Productos Más Vendidos</h3>
            <p class="text-xs text-muted-foreground">Ranking por volumen de ingresos en el período</p>
          </div>
        </div>
        <span class="text-xs font-medium text-muted-foreground">
          {{ topProducts().length }} {{ topProducts().length === 1 ? 'producto' : 'productos' }}
        </span>
      </div>

      @if (topProducts().length === 0) {
        <div class="flex flex-col items-center justify-center py-10 px-4 text-center">
          <div class="w-12 h-12 rounded-2xl bg-muted/60 text-muted-foreground flex items-center justify-center mb-2">
            <ng-icon name="matInventory2Outline" class="text-2xl" />
          </div>
          <p class="text-sm font-semibold text-foreground">Sin ventas registradas en este período</p>
          <p class="text-xs text-muted-foreground max-w-xs mt-0.5">
            Las ventas confirmadas y entregadas aparecerán clasificadas aquí de forma automática.
          </p>
        </div>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-left text-sm">
            <thead class="text-xs uppercase text-muted-foreground border-b border-border/60">
              <tr>
                <th scope="col" class="py-2.5 px-3 font-semibold">#</th>
                <th scope="col" class="py-2.5 px-3 font-semibold">Producto / Presentación</th>
                <th scope="col" class="py-2.5 px-3 font-semibold text-right">Unidades</th>
                <th scope="col" class="py-2.5 px-3 font-semibold text-right">Ingresos</th>
                <th scope="col" class="py-2.5 px-3 font-semibold w-1/4">Distribución</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-border/40">
              @for (item of topProducts(); track item.variantId; let idx = $index) {
                <tr class="hover:bg-muted/30 transition-colors">
                  <td class="py-3 px-3 font-bold text-xs">
                    <span
                      class="inline-flex items-center justify-center w-5 h-5 rounded-md text-[11px]"
                      [class.bg-amber-500/10]="idx === 0"
                      [class.text-amber-500]="idx === 0"
                      [class.bg-stone-500/10]="idx !== 0"
                      [class.text-stone-600]="idx !== 0"
                    >
                      {{ idx + 1 }}
                    </span>
                  </td>
                  <td class="py-3 px-3">
                    <div class="font-bold text-foreground text-xs leading-tight">{{ item.productName }}</div>
                    @if (item.variantName) {
                      <div class="text-[11px] text-muted-foreground mt-0.5">{{ item.variantName }}</div>
                    }
                  </td>
                  <td class="py-3 px-3 text-right font-semibold text-xs text-foreground">
                    {{ item.unitsSold }}
                  </td>
                  <td class="py-3 px-3 text-right font-mono font-bold text-xs text-primary">
                    S/ {{ item.totalRevenue.toFixed(2) }}
                  </td>
                  <td class="py-3 px-3">
                    <div class="flex items-center gap-2">
                      <div class="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                        <div
                          class="h-full rounded-full bg-emerald-500 transition-all duration-500"
                          [style.width.%]="getPercentage(item.totalRevenue)"
                        ></div>
                      </div>
                      <span class="text-[10px] font-mono text-muted-foreground w-8 text-right">
                        {{ getPercentage(item.totalRevenue) }}%
                      </span>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class TopProductsCard {
  private readonly analyticsService = inject(AnalyticsService);

  readonly topProducts = computed<ProductSalesRank[]>(() => {
    return this.analyticsService.summary()?.topProducts || [];
  });

  readonly maxRevenue = computed(() => {
    const products = this.topProducts();
    if (products.length === 0) return 1;
    return Math.max(...products.map((p) => p.totalRevenue), 1);
  });

  getPercentage(revenue: number): number {
    const max = this.maxRevenue();
    return Math.round((revenue / max) * 100);
  }
}
