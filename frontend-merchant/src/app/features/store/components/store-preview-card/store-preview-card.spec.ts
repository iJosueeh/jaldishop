import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StorePreviewCard } from './store-preview-card';

describe('StorePreviewCard', () => {
  let component: StorePreviewCard;
  let fixture: ComponentFixture<StorePreviewCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StorePreviewCard],
    }).compileComponents();

    fixture = TestBed.createComponent(StorePreviewCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
