import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AreaTrendChartComponent } from './area-trend-chart';

describe('AreaTrendChartComponent', () => {
  let component: AreaTrendChartComponent;
  let fixture: ComponentFixture<AreaTrendChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AreaTrendChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AreaTrendChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe computar categorías y series reactivas a partir de los datos recibidos', () => {
    const mockData = [
      { date: '2026-11-20', gmv: 1500, inflow: 75 },
      { date: '2026-11-21', gmv: 2300, inflow: 115 },
    ];

    fixture.componentRef.setInput('data', mockData);
    fixture.detectChanges();

    expect(component.categories()).toEqual(['2026-11-20', '2026-11-21']);
    expect(component.series().length).toBe(2);
    expect(component.series()[0].name).toBe('GMV Bruto');
    expect(component.series()[1].name).toBe('Ingreso Plataforma');
  });
});
