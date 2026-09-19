import { Component, effect, inject, OnInit } from '@angular/core';
import { NonNullableFormBuilder, Validators } from '@angular/forms';
import { ProfileService } from '../../core/services/profile.service';
import { ToastService } from '../../core/services/toast.service';
import { ErrorHandlerService } from '../../core/services/error-handler.service';
import { UpdateUserProfileRequest } from '../../core/models/user-profile.models';
import { ProfileInfoCard } from './components/profile-info-card/profile-info-card';
import { ProfileSecurityCard } from './components/profile-security-card/profile-security-card';
import { StoreService } from '../../core/services/store.service';

import { NgIcon, provideIcons } from '@ng-icons/core';
import { matCheckOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [ProfileInfoCard, ProfileSecurityCard, NgIcon],
  providers: [
    provideIcons({
      matCheckOutline,
    }),
  ],
  selector: 'app-profile',
  styleUrl: './profile.css',
  templateUrl: './profile.html',
})
export class Profile implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  readonly profileService = inject(ProfileService);
  readonly storeService = inject(StoreService);
  private readonly toastService = inject(ToastService);
  private readonly errorHandler = inject(ErrorHandlerService);

  readonly profileForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.pattern('^9\\d{8}$')]],
  });

  private readonly _syncProfileEffect = effect(() => {
    const profile = this.profileService.currentProfile();
    if (profile) {
      this.profileForm.patchValue({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        phone: profile.phone || '',
      });
    }
  });

  ngOnInit(): void {
    if (!this.profileService.currentProfile()) {
      this.profileService.getMyProfile().subscribe({
        error: (err) => {
          const normalized = this.errorHandler.normalize(err);
          this.toastService.error(normalized.message);
        },
      });
    }
  }

  onDiscard(): void {
    const profile = this.profileService.currentProfile();
    if (profile) {
      this.profileForm.reset({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        phone: profile.phone || '',
      })
    }
  }

  onSaveProfile(): void {
    if (this.profileForm.invalid || this.profileService.isLoading()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const val = this.profileForm.getRawValue();
    const request: UpdateUserProfileRequest = {
      firstName: val.firstName.trim(),
      lastName: val.lastName.trim(),
      phone: val.phone.trim() || null,
    };

    this.profileService.updateProfile(request).subscribe({
      next: () => {
        this.toastService.success('Tu informacion personal se ha guardado correctamente.');
      },
      error: (err) => {
        const normalized = this.errorHandler.normalize(err);
        this.toastService.error(normalized.message);
      },
    });
  }
}
