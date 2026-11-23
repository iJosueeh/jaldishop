import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgApexchartsModule, ApexAxisChartSeries, ApexChart, ApexXAxis, ApexYAxis, ApexDataLabels, ApexPlotOptions, ApexTooltip } from 'ng-apexcharts';

export interface BarCategoryItem {
  id: string;
  label: string;
  value: number;
}

@Component({
  selector: 'app-horizontal-bar-chart',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  template: `
    <div class="w-full h-full min-h-[260px] relative">
      @if (series().length > 0 && categories().length > 0) {
        <apx-chart
          [series]="series()"
          [chart]="chartOptions()"
          [xaxis]="xaxisOptions()"
          [yaxis]="yaxisOptions()"
          [plotOptions]="plotOptions()"
          [dataLabels]="dataLabelsOptions()"
          [tooltip]="tooltipOptions()"
        />
      } @else {
        <div class="flex items-center justify-center h-48 text-on-surface-variant text-xs">
          No hay datos registrados para la comparación.
        </div>
      }
    </div>
  `,
})
export class HorizontalBarChartComponent {
  readonly items = input<BarCategoryItem[]>([]);
  readonly metricName = input<string>('Volumen GMV');
  readonly currencyPrefix = input<string>('S/ ');
  readonly selectItem = output<string>();

  readonly categories = computed(() => this.items().map((i) => i.label));

  readonly series = computed<ApexAxisChartSeries>(() => [
    {
      name: this.metricName(),
      data: this.items().map((i) => i.value),
      color: '#4f46e5',
    },
  ]);

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'bar',
    height: Math.max(260, this.items().length * 36),
    fontFamily: 'inherit',
    background: 'transparent',
    toolbar: { show: false },
    events: {
      dataPointSelection: (_event, _chartContext, config?: { dataPointIndex?: number }) => {
        if (config && typeof config.dataPointIndex === 'number') {
          const item = this.items()[config.dataPointIndex];
          if (item) {
            this.selectItem.emit(item.id);
          }
        }
      },
    },
  }));

  readonly plotOptions = computed<ApexPlotOptions>(() => ({
    bar: {
      horizontal: true,
      borderRadius: 6,
      barHeight: '55%',
      distributed: false,
      dataLabels: { position: 'top' },
    },
  }));

  readonly dataLabelsOptions = computed<ApexDataLabels>(() => ({
    enabled: true,
    offsetX: 28,
    style: {
      fontSize: '11px',
      fontFamily: 'monospace',
      colors: ['#475569'],
    },
    formatter: (val: number) =>
      `${this.currencyPrefix()}${val.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`,
  }));

  readonly xaxisOptions = computed<ApexXAxis>(() => ({
    categories: this.categories(),
    labels: {
      style: {
        fontSize: '11px',
        colors: '#64748b',
        fontFamily: 'monospace',
      },
      formatter: (val: string) =>
        `${this.currencyPrefix()}${parseFloat(val || '0').toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`,
    },
    axisBorder: { show: false },
    axisTicks: { show: false },
  }));

  readonly yaxisOptions = computed<ApexYAxis>(() => ({
    labels: {
      style: {
        fontSize: '12px',
        fontWeight: 600,
        colors: '#334155',
      },
    },
  }));

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: 'dark',
    y: {
      formatter: (val: number) =>
        `${this.currencyPrefix()}${val.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    },
  }));
}
