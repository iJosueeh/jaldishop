import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityTabSelector } from './capacity-tab-selector';

describe('CapacityTabSelector', () => {
  let component: CapacityTabSelector;
  let fixture: ComponentFixture<CapacityTabSelector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityTabSelector],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityTabSelector);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe emitir tabChange con EXCEPTIONS al pulsar la pestaña de excepciones', () => {
    let emittedTab: any = null;
    component.tabChange.subscribe((tab) => {
      emittedTab = tab;
    });

    const buttons = fixture.nativeElement.querySelectorAll('button');
    buttons[1].click();

    expect(emittedTab).toBe('EXCEPTIONS');
  });

  it('debe mostrar el contador de excepciones cuando es mayor a 0', () => {
    fixture.componentRef.setInput('exceptionsCount', 3);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('3');
  });
});
