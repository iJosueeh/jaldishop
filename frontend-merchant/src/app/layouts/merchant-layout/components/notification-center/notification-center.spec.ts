import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Notification } from '../../../../core/models/notification.models';
import { NotificationService } from '../../../../core/services/notification.service';
import { NotificationCenter } from './notification-center';

describe('NotificationCenter', () => {
  let component: NotificationCenter;
  let fixture: ComponentFixture<NotificationCenter>;
  let notificationServiceMock: any;

  const mockItems: Notification[] = [
    {
      id: 'notif-1',
      userId: 'user-1',
      type: 'NEW_ORDER',
      title: 'Nuevo pedido #ORD-2026-001',
      message: 'Tienes un nuevo pedido para despachar.',
      status: 'UNREAD',
      createdAt: '2026-10-31T12:00:00Z',
      readAt: null,
    },
    {
      id: 'notif-2',
      userId: 'user-1',
      type: 'SYSTEM',
      title: 'Mantenimiento preventivo',
      message: 'Ventana de actualización concluida.',
      status: 'READ',
      createdAt: '2026-10-30T10:00:00Z',
      readAt: '2026-10-30T11:00:00Z',
    },
  ];

  beforeEach(async () => {
    const { signal, computed } = await import('@angular/core');
    const notifsSig = signal<Notification[]>([]);
    const unreadSig = signal<number>(0);
    const loadingSig = signal<boolean>(false);
    const errorSig = signal<string | null>(null);

    notificationServiceMock = {
      notifications: notifsSig,
      unreadCount: unreadSig,
      isLoading: loadingSig,
      error: errorSig,
      hasUnread: computed(() => unreadSig() > 0),
      unreadBadgeText: computed(() => (unreadSig() > 0 ? `${unreadSig()}` : '')),
      isMarkingRead: () => false,
      getUnreadCount: () => of({ unreadCount: 1 }),
      getNotifications: () => of(mockItems),
      markAsRead: (id: string) =>
        of({
          ...mockItems[0],
          status: 'READ',
          readAt: '2026-11-01T12:00:00Z',
        }),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationCenter],
      providers: [
        { provide: NotificationService, useValue: notificationServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationCenter);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e invocar getUnreadCount al iniciar', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(false);
  });

  it('debe renderizar el badge de no leídas cuando unreadCount > 0', () => {
    notificationServiceMock.unreadCount.set(3);
    fixture.detectChanges();

    const badgeEl = fixture.nativeElement.querySelector('span.bg-emerald-700');
    expect(badgeEl).toBeTruthy();
    expect(badgeEl.textContent.trim()).toBe('3');
  });

  it('debe alternar el panel al hacer clic en el botón de campana', () => {
    const button = fixture.nativeElement.querySelector('button');
    button.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);
    const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(dialog).toBeTruthy();

    button.click();
    fixture.detectChanges();
    expect(component.isOpen()).toBe(false);
  });

  it('debe renderizar estado vacío cuando no hay notificaciones', () => {
    component.isOpen.set(true);
    notificationServiceMock.notifications.set([]);
    notificationServiceMock.isLoading.set(false);
    fixture.detectChanges();

    const emptyText = fixture.nativeElement.textContent;
    expect(emptyText).toContain('Bandeja al día');
  });

  it('debe renderizar skeletons de carga cuando isLoading = true y lista vacía', () => {
    component.isOpen.set(true);
    notificationServiceMock.notifications.set([]);
    notificationServiceMock.isLoading.set(true);
    fixture.detectChanges();

    const pulses = fixture.nativeElement.querySelectorAll('.animate-pulse');
    expect(pulses.length).toBeGreaterThan(0);
  });

  it('debe renderizar error y permitir reintentar', () => {
    component.isOpen.set(true);
    notificationServiceMock.error.set('Error al conectar con el servidor.');
    fixture.detectChanges();

    const errorContainer = fixture.nativeElement.textContent;
    expect(errorContainer).toContain('Error al conectar con el servidor.');

    const retryBtn = fixture.nativeElement.querySelector('button.bg-stone-900');
    expect(retryBtn).toBeTruthy();

    let loaded = false;
    notificationServiceMock.getNotifications = () => {
      loaded = true;
      return of([]);
    };

    retryBtn.click();
    expect(loaded).toBe(true);
  });

  it('debe renderizar la lista de notificaciones con formato y estado de lectura', () => {
    component.isOpen.set(true);
    notificationServiceMock.notifications.set(mockItems);
    fixture.detectChanges();

    const articles = fixture.nativeElement.querySelectorAll('article');
    expect(articles.length).toBe(2);

    expect(articles[0].textContent).toContain('Nuevo pedido #ORD-2026-001');
    expect(articles[1].textContent).toContain('Mantenimiento preventivo');
  });

  it('debe invocar markAsRead al hacer clic en el botón de marcar como leída', () => {
    component.isOpen.set(true);
    notificationServiceMock.notifications.set(mockItems);
    fixture.detectChanges();

    let markedId = '';
    notificationServiceMock.markAsRead = (id: string) => {
      markedId = id;
      return of({ ...mockItems[0], status: 'READ' });
    };

    const markBtn = fixture.nativeElement.querySelector('button[title="Marcar como leída"]');
    expect(markBtn).toBeTruthy();

    markBtn.click();
    expect(markedId).toBe('notif-1');
  });

  it('debe cerrar el panel al presionar tecla Escape', () => {
    component.isOpen.set(true);
    fixture.detectChanges();

    component.handleEscapeKey();
    expect(component.isOpen()).toBe(false);
  });

  it('debe cerrar el panel al hacer clic fuera del componente', () => {
    component.isOpen.set(true);
    fixture.detectChanges();

    const externalDiv = document.createElement('div');
    document.body.appendChild(externalDiv);

    component.handleClickOutside({ target: externalDiv } as any);
    expect(component.isOpen()).toBe(false);

    document.body.removeChild(externalDiv);
  });
});
