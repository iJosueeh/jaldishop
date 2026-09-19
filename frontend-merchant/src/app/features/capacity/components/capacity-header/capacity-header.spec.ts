import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CapacityHeader } from './capacity-header';

describe('CapacityHeader', () => {
  let component: CapacityHeader;
  let fixture: ComponentFixture<CapacityHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CapacityHeader],
    }).compileComponents();

    fixture = TestBed.createComponent(CapacityHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
