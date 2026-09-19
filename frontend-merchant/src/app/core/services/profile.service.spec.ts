import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProfileService } from './profile.service';
import { UserProfile } from '../models/user-profile.models';
import { environment } from '../../../environments/environment';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpTesting: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/users`;

  const mockProfile: UserProfile = {
    id: 'user-1',
    email: 'josue@jaldishop.com',
    firstName: 'Josue',
    lastName: 'Tanta',
    phone: '999888777',
    status: 'ACTIVE',
    roles: ['MERCHANT'],
    createdAt: '2026-09-19T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProfileService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProfileService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente con perfil inicial nulo', () => {
    expect(service).toBeTruthy();
    expect(service.currentProfile()).toBeNull();
    expect(service.fullName()).toBe('Usuario');
    expect(service.userInitials()).toBe('U');
  });

  it('debe obtener perfil por HTTP y actualizar signals y caché', () => {
    service.getMyProfile().subscribe((profile) => {
      expect(profile).toEqual(mockProfile);
      expect(service.currentProfile()).toEqual(mockProfile);
      expect(service.fullName()).toBe('Josue Tanta');
      expect(service.userInitials()).toBe('JT');
      expect(service.isLoading()).toBe(false);
    });

    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('debe retornar datos desde memoria en llamadas posteriores sin repetir HTTP', () => {
    // Primera llamada
    service.getMyProfile().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockProfile);

    // Segunda llamada desde memoria
    let cachedResult: UserProfile | null = null;
    service.getMyProfile(false).subscribe((profile) => {
      cachedResult = profile;
    });

    expect(cachedResult).toEqual(mockProfile);
    httpTesting.expectNone(`${baseUrl}/me`);
  });

  it('debe forzar llamada HTTP cuando forceRefresh es true', () => {
    service.getMyProfile().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockProfile);

    service.getMyProfile(true).subscribe();
    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('debe actualizar perfil y sincronizar signal tras updateProfile()', () => {
    const updated = { ...mockProfile, firstName: 'Josue Alexander' };
    service.updateProfile({ firstName: 'Josue Alexander', lastName: 'Tanta', phone: '999888777' }).subscribe((res) => {
      expect(res.firstName).toBe('Josue Alexander');
      expect(service.fullName()).toBe('Josue Alexander Tanta');
    });

    const req = httpTesting.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('PUT');
    req.flush(updated);
  });

  it('debe limpiar memoria tras clearProfile()', () => {
    service.getMyProfile().subscribe();
    httpTesting.expectOne(`${baseUrl}/me`).flush(mockProfile);

    service.clearProfile();
    expect(service.currentProfile()).toBeNull();
    expect(service.fullName()).toBe('Usuario');
  });
});
