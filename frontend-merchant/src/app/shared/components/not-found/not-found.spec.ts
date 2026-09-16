import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotFound } from './not-found';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('NotFound (Página 404)', () => {
  let component: NotFound;
  let fixture: ComponentFixture<NotFound>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFound],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotFound);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe renderizar el título de error 404', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('404');
    expect(compiled.textContent).toContain('Página no encontrada');
  });
});
