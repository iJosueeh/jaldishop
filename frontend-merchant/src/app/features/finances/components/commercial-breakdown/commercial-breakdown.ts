import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matLocalShippingOutline,
  matSellOutline,
  matCancelOutline,
  matViewWeekOutline,
  matInfoOutline,
} from '@ng-icons/material-symbols/outline';
import { AnalyticsService } from '../../../../core/services/analytics.service';

@Component({
  selector: 'app-commercial-breakdown',
  standalone: true,
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matLocalShippingOutline,
      matSellOutline,
      matCancelOutline,
      matViewWeekOutline,
      matInfoOutline,
    }),
  ],
  template: `
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- 1. Desglose Operativo y Logístico -->
      <div class="p-5 rounded-2xl bg-card border border-border shadow-xs flex flex-col justify-between gap-4">
        <div>
          <h3 class="text-base font-bold text-foreground">Operaciones & Logística</h3>
          <p class="text-xs text-muted-foreground mt-0.5">Métricas complementarias del flujo comercial</p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <!-- Tarifas de Envío Cobradas -->
          <div class="p-3.5 rounded-xl bg-muted/40 border border-border/60 flex items-center justify-between">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-blue-500/10 text-blue-500 flex items-center justify-center">
                <ng-icon name="matLocalShippingOutline" class="text-base" />
              </div>
              <div>
                <span class="text-[11px] font-semibold text-muted-foreground uppercase">Envíos Cobrados</span>
                <div class="text-sm font-bold text-foreground font-mono">
                  S/ {{ (summary()?.commercial?.totalDeliveryFees || 0).toFixed(2) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Descuentos Aplicados -->
          <div class="p-3.5 rounded-xl bg-muted/40 border border-border/60 flex items-center justify-between">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-500 flex items-center justify-center">
                <ng-icon name="matSellOutline" class="text-base" />
              </div>
              <div>
                <span class="text-[11px] font-semibold text-muted-foreground uppercase">Descuentos</span>
                <div class="text-sm font-bold text-foreground font-mono">
                  S/ {{ (summary()?.commercial?.totalDiscounts || 0).toFixed(2) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Pedidos Cancelados -->
          <div class="p-3.5 rounded-xl bg-muted/40 border border-border/60 flex items-center justify-between">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-rose-500/10 text-rose-500 flex items-center justify-center">
                <ng-icon name="matCancelOutline" class="text-base" />
              </div>
              <div>
                <span class="text-[11px] font-semibold text-muted-foreground uppercase">Cancelaciones</span>
                <div class="text-sm font-bold text-foreground">
                  {{ summary()?.commercial?.cancelledOrdersCount || 0 }} pedidos
                </div>
                <div class="text-[10px] text-muted-foreground font-mono">
                  S/ {{ (summary()?.commercial?.cancelledOrdersVolume || 0).toFixed(2) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Cupos de Capacidad -->
          <div class="p-3.5 rounded-xl bg-muted/40 border border-border/60 flex items-center justify-between">
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-500 flex items-center justify-center">
                <ng-icon name="matViewWeekOutline" class="text-base" />
              </div>
              <div>
                <span class="text-[11px] font-semibold text-muted-foreground uppercase">Cupos Asignados</span>
                <div class="text-sm font-bold text-foreground">
                  {{ summary()?.capacityDemand?.committedReservationsCount || 0 }} reservas
                </div>
                <div class="text-[10px] text-muted-foreground">
                  {{ summary()?.capacityDemand?.releasedReservationsCount || 0 }} liberadas
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. Transparencia y Aviso Legal de Liquidación -->
      <div class="p-5 rounded-2xl bg-card border border-border shadow-xs flex flex-col justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-500 flex items-center justify-center">
              <ng-icon name="matInfoOutline" class="text-lg" />
            </div>
            <div>
              <h3 class="text-base font-bold text-foreground">Transparencia Financiera</h3>
              <p class="text-xs text-muted-foreground mt-0.5">Condiciones y capturas de pasarela</p>
            </div>
          </div>
        </div>

        <div class="p-4 rounded-xl bg-muted/40 border border-border/60 flex flex-col gap-2.5">
          <p class="text-xs text-foreground/80 leading-relaxed font-sans">
            {{ summary()?.financial?.disclosure || 'Fondos capturados y restituidos en pasarela de pagos (Mercado Pago). No representa liquidación bancaria ni desembolso en cuentas corrientes.' }}
          </p>
          <div class="text-[11px] text-muted-foreground flex flex-col gap-1 pt-2 border-t border-border/50">
            <div>
              • <strong>Pagos vinculados a pedidos:</strong> S/ {{ (summary()?.financial?.orderLinkedPaymentsAmount || 0).toFixed(2) }} ({{ summary()?.financial?.orderLinkedPaymentsCount || 0 }} transacciones)
            </div>
            <div>
              • <strong>Zona horaria de consolidación:</strong> {{ summary()?.period?.timeZone || 'America/Lima' }} (UTC-5)
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class CommercialBreakdown {
  private readonly analyticsService = inject(AnalyticsService);

  readonly summary = computed(() => this.analyticsService.summary());
}
