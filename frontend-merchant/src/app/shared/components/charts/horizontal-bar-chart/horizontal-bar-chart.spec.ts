import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HorizontalBarChartComponent } from './horizontal-bar-chart';

describe('HorizontalBarChartComponent', () => {
  let component: HorizontalBarChartComponent;
  let fixture: ComponentFixture<HorizontalBarChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HorizontalBarChartComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HorizontalBarChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe estructurar categorías y series para el ranking horizontal', () => {
    const mockItems = [
      { id: 'store-1', label: 'Bodega San Martín', value: 12500 },
      { id: 'store-2', label: 'Minimarket Los Andes', value: 8900 },
    ];

    fixture.componentRef.setInput('items', mockItems);
    fixture.detectChanges();

    expect(component.categories()).toEqual(['Bodega San Martín', 'Minimarket Los Andes']);
    expect(component.series()[0].data).toEqual([12500, 8900]);
  });
});
