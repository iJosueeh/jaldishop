import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CustomersFilterBar } from './customers-filter-bar';
import { vi } from 'vitest';

describe('CustomersFilterBar', () => {
  let component: CustomersFilterBar;
  let fixture: ComponentFixture<CustomersFilterBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomersFilterBar],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersFilterBar);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('activeTab', 'all');
    fixture.componentRef.setInput('searchQuery', '');
    fixture.componentRef.setInput('totalCount', 10);
    fixture.componentRef.setInput('vipCount', 2);
    fixture.componentRef.setInput('frequentCount', 3);
    fixture.componentRef.setInput('newCount', 5);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render tabs with correct counts', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Todos (10)');
    expect(compiled.textContent).toContain('VIP / Fidelizados (2)');
    expect(compiled.textContent).toContain('Frecuentes (3)');
    expect(compiled.textContent).toContain('Nuevos (5)');
  });

  it('should emit tabChange when a tab is clicked', () => {
    const tabSpy = vi.spyOn(component.tabChange, 'emit');
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const vipButton = Array.from(buttons).find((b: any) =>
      b.textContent.includes('VIP / Fidelizados'),
    ) as HTMLButtonElement;
    vipButton.click();
    expect(tabSpy).toHaveBeenCalledWith('vip');
  });

  it('should emit searchChange on input and clear', () => {
    const searchSpy = vi.spyOn(component.searchChange, 'emit');
    component.onSearchInput('Juan');
    expect(searchSpy).toHaveBeenCalledWith('Juan');

    component.onClearSearch();
    expect(searchSpy).toHaveBeenCalledWith('');
  });
});
