import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuickActions } from './quick-actions';
import { provideRouter } from '@angular/router';

describe('QuickActions', () => {
  let component: QuickActions;
  let fixture: ComponentFixture<QuickActions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuickActions],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(QuickActions);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
