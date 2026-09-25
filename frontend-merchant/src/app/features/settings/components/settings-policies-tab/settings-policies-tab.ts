import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matDescriptionOutline,
  matBadgeOutline,
  matEditNoteOutline,
  matSaveOutline,
  matGavelOutline,
  matRestartAltOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matDescriptionOutline,
      matBadgeOutline,
      matEditNoteOutline,
      matSaveOutline,
      matGavelOutline,
      matRestartAltOutline,
    }),
  ],
  selector: 'app-settings-policies-tab',
  styleUrl: './settings-policies-tab.css',
  templateUrl: './settings-policies-tab.html',
})
export class SettingsPoliciesTab {
  private readonly settingsService = inject(SettingsService);

  readonly policies = computed(() => this.settingsService.settings().policies);

  readonly cancellationPolicy = signal<string>('');
  readonly refundPolicy = signal<string>('');
  readonly requireDni = signal<boolean>(false);
  readonly allowNotes = signal<boolean>(true);

  constructor() {
    this.syncFromService();
  }

  private syncFromService(): void {
    const p = this.policies();
    this.cancellationPolicy.set(p.cancellationPolicy);
    this.refundPolicy.set(p.refundPolicy);
    this.requireDni.set(p.requireCustomerDni);
    this.allowNotes.set(p.allowOrderNotes);
  }

  onSave(): void {
    this.settingsService.updatePolicies({
      cancellationPolicy: this.cancellationPolicy().trim(),
      refundPolicy: this.refundPolicy().trim(),
      requireCustomerDni: this.requireDni(),
      allowOrderNotes: this.allowNotes(),
    });
  }
}

