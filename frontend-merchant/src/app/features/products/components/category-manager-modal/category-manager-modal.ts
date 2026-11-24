import { ChangeDetectionStrategy, Component, HostListener, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matAddOutline,
  matCheckOutline,
  matCloseOutline,
  matDeleteOutline,
  matEditOutline,
  matCategoryOutline,
} from '@ng-icons/material-symbols/outline';
import { ProductCategory } from '../../../../core/models/product.models';
import { ProductService } from '../../../../core/services/product.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';
import { ApiErrorMetadata } from '../../../../core/models/api-error.models';
import { DestructiveActionModal } from '../../../../shared/components/destructive-action-modal/destructive-action-modal';

@Component({
  selector: 'app-category-manager-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIcon, DestructiveActionModal],
  providers: [
    provideIcons({
      matAddOutline,
      matCheckOutline,
      matCloseOutline,
      matDeleteOutline,
      matEditOutline,
      matCategoryOutline,
    }),
  ],
  templateUrl: './category-manager-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryManagerModal {
  private readonly productService = inject(ProductService);
  private readonly toast = inject(ToastService);
  private readonly errorHandler = inject(ErrorHandlerService);

  readonly isOpen = input<boolean>(false);
  readonly storeId = input<string>('');
  readonly categories = input<ProductCategory[]>([]);
  readonly closeModal = output<void>();

  readonly newName = signal<string>('');
  readonly isCreating = signal<boolean>(false);

  readonly editingId = signal<string | null>(null);
  readonly editName = signal<string>('');
  readonly isSavingEdit = signal<boolean>(false);

  readonly deleteTarget = signal<ProductCategory | null>(null);
  readonly isDeleting = signal<boolean>(false);
  readonly deleteErrorStatus = signal<number | null>(null);
  readonly deleteErrorMessage = signal<string | null>(null);
  readonly deleteConflictMetadata = signal<ApiErrorMetadata | null>(null);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen() && !this.deleteTarget()) {
      this.closeModal.emit();
    }
  }

  createCategory(): void {
    const name = this.newName().trim();
    const sid = this.storeId();
    if (!name || !sid) return;
    this.isCreating.set(true);
    this.productService.createCategory(sid, { name }).subscribe({
      next: () => {
        this.isCreating.set(false);
        this.newName.set('');
        this.toast.success(`Categoría "${name}" creada.`);
      },
      error: (err) => {
        this.isCreating.set(false);
        this.toast.error(this.errorHandler.normalize(err).message);
      },
    });
  }

  startEdit(cat: ProductCategory): void {
    this.editingId.set(cat.id);
    this.editName.set(cat.name);
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.editName.set('');
  }

  saveEdit(cat: ProductCategory): void {
    const name = this.editName().trim();
    const sid = this.storeId();
    if (!name || !sid) return;
    this.isSavingEdit.set(true);
    this.productService
      .updateCategory(sid, cat.id, {
        name,
        description: cat.description,
        status: (cat.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE'),
      })
      .subscribe({
        next: () => {
          this.isSavingEdit.set(false);
          this.editingId.set(null);
          this.toast.success(`Categoría actualizada a "${name}".`);
        },
        error: (err) => {
          this.isSavingEdit.set(false);
          this.toast.error(this.errorHandler.normalize(err).message);
        },
      });
  }

  toggleStatus(cat: ProductCategory): void {
    const sid = this.storeId();
    if (!sid) return;
    const nextStatus = cat.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.productService
      .updateCategory(sid, cat.id, {
        name: cat.name,
        description: cat.description,
        status: nextStatus,
      })
      .subscribe({
        next: () => {
          this.toast.info(
            nextStatus === 'ACTIVE'
              ? `Categoría "${cat.name}" activada.`
              : `Categoría "${cat.name}" desactivada.`,
          );
        },
        error: (err) => {
          this.toast.error(this.errorHandler.normalize(err).message);
        },
      });
  }

  requestDelete(cat: ProductCategory): void {
    this.deleteErrorStatus.set(null);
    this.deleteErrorMessage.set(null);
    this.deleteConflictMetadata.set(null);
    this.deleteTarget.set(cat);
  }

  cancelDelete(): void {
    if (!this.isDeleting()) {
      this.deleteTarget.set(null);
    }
  }

  confirmDelete(): void {
    const cat = this.deleteTarget();
    const sid = this.storeId();
    if (!cat || !sid) return;
    this.isDeleting.set(true);
    this.productService.deleteCategory(sid, cat.id).subscribe({
      next: (res) => {
        this.isDeleting.set(false);
        this.deleteTarget.set(null);
        this.toast.success(res.message);
      },
      error: (err) => {
        this.isDeleting.set(false);
        const normalized = this.errorHandler.normalize(err);
        if (normalized.status === 409 || normalized.status === 403) {
          this.deleteErrorStatus.set(normalized.status);
          this.deleteErrorMessage.set(normalized.message);
          this.deleteConflictMetadata.set(normalized.metadata ?? null);
        } else {
          this.deleteTarget.set(null);
          this.toast.error(normalized.message);
        }
      },
    });
  }

  fallbackDeactivate(): void {
    const cat = this.deleteTarget();
    const sid = this.storeId();
    if (!cat || !sid) return;
    this.isDeleting.set(true);
    this.productService
      .updateCategory(sid, cat.id, {
        name: cat.name,
        description: cat.description,
        status: 'INACTIVE',
      })
      .subscribe({
        next: () => {
          this.isDeleting.set(false);
          this.deleteTarget.set(null);
          this.toast.info(`Categoría "${cat.name}" desactivada.`);
        },
        error: (err) => {
          this.isDeleting.set(false);
          this.toast.error(this.errorHandler.normalize(err).message);
        },
      });
  }
}
