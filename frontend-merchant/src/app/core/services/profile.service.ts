import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { UpdateUserProfileRequest, UserProfile } from '../models/user-profile.models';
import { Observable, tap, catchError, throwError, of } from 'rxjs';

@Service()
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  readonly currentProfile = signal<UserProfile | null>(null);
  readonly isLoading = signal<boolean>(false);

  readonly fullName = computed(() => {
    const profile = this.currentProfile();
    return profile ? `${profile.firstName} ${profile.lastName}`.trim() : 'Usuario';
  });

  readonly userInitials = computed(() => {
    const profile = this.currentProfile();
    if (!profile) return 'U';
    const first = profile.firstName?.[0]?.toUpperCase() ?? '';
    const last = profile.lastName?.[0]?.toUpperCase() ?? '';
    return `${first}${last}` || 'U';
  });

  getMyProfile(forceRefresh = false): Observable<UserProfile> {
    if (this.currentProfile() !== null && !forceRefresh) {
      return of(this.currentProfile()!);
    }
    
    this.isLoading.set(true);
    return this.http.get<UserProfile>(`${this.baseUrl}/me`).pipe(
      tap((profile) => {
        this.currentProfile.set(profile);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  updateProfile(request: UpdateUserProfileRequest): Observable<UserProfile> {
    this.isLoading.set(true);
    return this.http.put<UserProfile>(`${this.baseUrl}/me`, request).pipe(
      tap((updated) => {
        this.currentProfile.set(updated);
        this.isLoading.set(false);
      }),
      catchError((error) => {
        this.isLoading.set(false);
        return throwError(() => error);
      }),
    );
  }

  clearProfile(): void {
    this.currentProfile.set(null);
  }
}
