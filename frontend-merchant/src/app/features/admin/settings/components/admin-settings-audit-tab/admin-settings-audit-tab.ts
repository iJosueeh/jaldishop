import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../../core/services/admin.service';
import { AdminAuditLog } from '../../../../../core/models/admin.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matHistoryEduOutline,
  matCheckCircleOutline,
  matCancelOutline,
  matVisibilityOutline,
  matCloseOutline,
  matRefreshOutline,
  matSearchOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon],
  providers: [
    provideIcons({
      matHistoryEduOutline,
      matCheckCircleOutline,
      matCancelOutline,
      matVisibilityOutline,
      matCloseOutline,
      matRefreshOutline,
      matSearchOutline,
    }),
  ],
  selector: 'app-admin-settings-audit-tab',
  styleUrl: './admin-settings-audit-tab.css',
  templateUrl: './admin-settings-audit-tab.html',
})
export class AdminSettingsAuditTab implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly auditLogs = this.adminService.auditLogs;
  readonly isLoading = this.adminService.isLoadingAuditLogs;

  readonly selectedLog = signal<AdminAuditLog | null>(null);
  readonly actionFilter = signal<string>('');

  ngOnInit(): void {
    if (this.auditLogs().length === 0) {
      this.refreshLogs();
    }
  }

  refreshLogs(): void {
    this.adminService.loadAuditLogs({ page: 0, size: 30 }).subscribe();
  }

  inspectLog(log: AdminAuditLog): void {
    this.selectedLog.set(log);
  }

  closeModal(): void {
    this.selectedLog.set(null);
  }

  formatJson(data: Record<string, unknown> | null | undefined): string {
    if (!data) return 'Sin datos adicionales';
    try {
      return JSON.stringify(data, null, 2);
    } catch {
      return String(data);
    }
  }
}
