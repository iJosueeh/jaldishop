import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../core/services/admin.service';
import { AdminPlatformSettings } from '../../../core/models/admin.models';
import { AdminSettingsHeader } from './components/admin-settings-header/admin-settings-header';
import { AdminSettingsGeneralTab } from './components/admin-settings-general-tab/admin-settings-general-tab';
import { AdminSettingsSystemTab } from './components/admin-settings-system-tab/admin-settings-system-tab';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matTuneOutline,
  matPaymentsOutline,
  matWarningOutline,
} from '@ng-icons/material-symbols/outline';

export type AdminSettingsTabType = 'general' | 'system';

export interface SettingsDiffItem {
  label: string;
  before: string;
  after: string;
}

@Component({
  imports: [
    CommonModule,
    AdminSettingsHeader,
    AdminSettingsGeneralTab,
    AdminSettingsSystemTab,
    NgIcon,
  ],
  providers: [
    provideIcons({
      matTuneOutline,
      matPaymentsOutline,
      matWarningOutline,
    }),
  ],
  selector: 'app-admin-settings',
  styleUrl: './admin-settings.css',
  templateUrl: './admin-settings.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSettings implements OnInit {
  readonly adminService = inject(AdminService);

  readonly currentTab = signal<AdminSettingsTabType>('general');
  readonly pendingDiffs = signal<SettingsDiffItem[]>([]);
  readonly showConfirmModal = signal<boolean>(false);
  private baseline: AdminPlatformSettings | null = null;

  ngOnInit(): void {
    this.adminService.loadSettings().subscribe((loaded) => {
      if (loaded) {
        this.baseline = { ...loaded };
      }
    });
  }

  setTab(tab: AdminSettingsTabType): void {
    this.currentTab.set(tab);
  }

  onUpdateSettings(partial: Partial<AdminPlatformSettings>): void {
    this.adminService.updateSettings(partial);
  }

  onSaveSettings(): void {
    const current = this.adminService.settings();
    const base = this.baseline;
    if (!base) {
      this.executeSave();
      return;
    }

    const diffs: SettingsDiffItem[] = [];
    if (current.maintenanceMode !== base.maintenanceMode) {
      diffs.push({
        label: 'Modo Mantenimiento',
        before: base.maintenanceMode ? 'Activo' : 'Inactivo',
        after: current.maintenanceMode ? 'Activo' : 'Inactivo',
      });
    }
    if (current.allowMerchantRegistration !== base.allowMerchantRegistration) {
      diffs.push({
        label: 'Registro Público de Comerciantes',
        before: base.allowMerchantRegistration ? 'Abierto' : 'Cerrado',
        after: current.allowMerchantRegistration ? 'Abierto' : 'Cerrado',
      });
    }
    if (current.marketplaceCommissionRate !== base.marketplaceCommissionRate) {
      diffs.push({
        label: 'Tasa de Comisión Base',
        before: `${base.marketplaceCommissionRate}%`,
        after: `${current.marketplaceCommissionRate}%`,
      });
    }

    if (diffs.length > 0) {
      this.pendingDiffs.set(diffs);
      this.showConfirmModal.set(true);
      return;
    }

    this.executeSave();
  }

  confirmSave(): void {
    this.showConfirmModal.set(false);
    this.pendingDiffs.set([]);
    this.executeSave();
  }

  cancelConfirm(): void {
    this.showConfirmModal.set(false);
    this.pendingDiffs.set([]);
  }

  private executeSave(): void {
    this.adminService.saveSettings();
    this.baseline = { ...this.adminService.settings() };
  }
}
