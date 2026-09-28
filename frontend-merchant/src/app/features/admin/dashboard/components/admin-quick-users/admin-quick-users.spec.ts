import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminQuickUsers } from './admin-quick-users';
import { provideRouter } from '@angular/router';

describe('AdminQuickUsers', () => {
  let component: AdminQuickUsers;
  let fixture: ComponentFixture<AdminQuickUsers>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminQuickUsers],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminQuickUsers);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('users', [
      {
        id: 'usr-1',
        email: 'valeria@store.com',
        fullName: 'Valeria Ramos',
        status: 'ACTIVE',
        roles: ['MERCHANT'],
        createdAt: '2026-09-01T10:00:00Z',
      },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('debe crearse correctamente y renderizar la lista', () => {
    expect(component).toBeTruthy();
    expect(component.users().length).toBe(1);
  });
});
