import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AdminAuditLog } from '../../../core/models/admin.models';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matHistoryEduOutline,
  matCheckCircleOutline,
  matCancelOutline,
  matVisibilityOutline,
  matCloseOutline,
  matRefreshOutline,
  matSearchOutline,
  matShieldOutline,
  matBlockOutline,
  matPersonOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [CommonModule, NgIcon, Pagination],
  providers: [
    provideIcons({
      matHistoryEduOutline,
      matCheckCircleOutline,
      matCancelOutline,
      matVisibilityOutline,
      matCloseOutline,
      matRefreshOutline,
      matSearchOutline,
      matShieldOutline,
      matBlockOutline,
      matPersonOutline,
    }),
  ],
  selector: 'app-admin-audit-logs',
  styleUrl: './admin-audit-logs.css',
  templateUrl: './admin-audit-logs.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAuditLogs implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  readonly auditLogs = this.adminService.auditLogs;
  readonly isLoading = this.adminService.isLoadingAuditLogs;
  readonly totalItems = this.adminService.totalAuditLogs;

  readonly currentPage = signal<number>(1);
  readonly pageSize = signal<number>(20);
  readonly actionFilter = signal<string>('');
  readonly statusFilter = signal<string>('');

  readonly selectedLog = signal<AdminAuditLog | null>(null);
  readonly isClosingModal = signal<boolean>(false);
  private closeTimeoutId?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.closeTimeoutId) {
        clearTimeout(this.closeTimeoutId);
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.selectedLog() && !this.isClosingModal()) {
      this.closeModal();
    }
  }

  ngOnInit(): void {
    const initialAction = this.route.snapshot.queryParamMap.get('action');
    if (initialAction) {
      this.actionFilter.set(initialAction);
      this.refreshLogs();
      return;
    }
    if (this.auditLogs().length === 0) {
      this.refreshLogs();
    }
  }

  refreshLogs(): void {
    this.adminService
      .loadAuditLogs({
        page: this.currentPage() - 1,
        size: this.pageSize(),
        action: this.actionFilter() || undefined,
        status: this.statusFilter() || undefined,
      })
      .subscribe();
  }

  onActionFilterChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.actionFilter.set(val);
    this.currentPage.set(1);
    this.refreshLogs();
  }

  onStatusFilterChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.statusFilter.set(val);
    this.currentPage.set(1);
    this.refreshLogs();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.adminService
      .loadAuditLogs({ page: page - 1, size: this.pageSize() })
      .subscribe();
  }

  inspectLog(log: AdminAuditLog): void {
    if (this.closeTimeoutId) {
      clearTimeout(this.closeTimeoutId);
      this.closeTimeoutId = undefined;
    }
    this.isClosingModal.set(false);
    this.selectedLog.set(log);
  }

  closeModal(): void {
    if (!this.selectedLog() || this.isClosingModal()) return;
    this.isClosingModal.set(true);
    this.closeTimeoutId = setTimeout(() => {
      this.selectedLog.set(null);
      this.isClosingModal.set(false);
      this.closeTimeoutId = undefined;
    }, 180);
  }

  getActorDisplay(log: AdminAuditLog): { label: string; isAnonymous: boolean } {
    if (!log.actorEmail || log.actorEmail === 'ANONYMOUS' || log.actorEmail.trim() === '') {
      return { label: 'Anónimo / No autenticado', isAnonymous: true };
    }
    return { label: log.actorEmail, isAnonymous: false };
  }

  getTargetDisplay(log: AdminAuditLog): { type: string; idOrUri: string | null } {
    if (log.targetId) {
      return { type: log.targetType, idOrUri: log.targetId };
    }
    if (log.targetType === 'ENDPOINT' || log.targetType === 'ADMIN_ROUTE') {
      const uri = this.extractUri(log.details);
      if (uri) {
        return { type: 'ENDPOINT', idOrUri: uri };
      }
    }
    return { type: log.targetType, idOrUri: null };
  }

  private extractUri(details: unknown): string | null {
    if (!details) return null;
    if (typeof details === 'string') {
      try {
        const parsed = JSON.parse(details);
        return parsed?.uri || null;
      } catch {
        return null;
      }
    }
    if (typeof details === 'object' && details !== null && 'uri' in details) {
      return String((details as Record<string, unknown>)['uri'] || '');
    }
    return null;
  }

  formatJson(data: unknown): string {
    if (!data) return 'Sin datos adicionales';
    try {
      if (typeof data === 'string') {
        const parsed = JSON.parse(data);
        return JSON.stringify(parsed, null, 2);
      }
      return JSON.stringify(data, null, 2);
    } catch {
      return String(data);
    }
  }
}
