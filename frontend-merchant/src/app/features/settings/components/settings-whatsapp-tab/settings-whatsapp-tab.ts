import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../../../../core/services/settings.service';
import { StoreService } from '../../../../core/services/store.service';
import { WhatsAppTemplate, WhatsAppTemplateId } from '../../../../core/models/settings.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  matChatOutline,
  matRestartAltOutline,
  matSaveOutline,
  matCheckCircleOutline,
  matAddOutline,
} from '@ng-icons/material-symbols/outline';

@Component({
  imports: [FormsModule, NgIcon],
  providers: [
    provideIcons({
      matChatOutline,
      matRestartAltOutline,
      matSaveOutline,
      matCheckCircleOutline,
      matAddOutline,
    }),
  ],
  selector: 'app-settings-whatsapp-tab',
  styleUrl: './settings-whatsapp-tab.css',
  templateUrl: './settings-whatsapp-tab.html',
})
export class SettingsWhatsappTab {
  private readonly settingsService = inject(SettingsService);
  private readonly storeService = inject(StoreService);

  readonly templates = computed(() => this.settingsService.settings().whatsappTemplates);
  readonly selectedTemplateId = signal<WhatsAppTemplateId>('in_preparation');

  readonly currentTemplate = computed(() => {
    const list = this.templates();
    return list.find((t) => t.id === this.selectedTemplateId()) ?? list[0];
  });

  readonly editedMessage = signal<string>('');

  constructor() {
    this.syncEditedMessage();
  }

  selectTemplate(id: WhatsAppTemplateId): void {
    this.selectedTemplateId.set(id);
    this.syncEditedMessage();
  }

  private syncEditedMessage(): void {
    const tpl = this.currentTemplate();
    if (tpl) {
      this.editedMessage.set(tpl.message);
    }
  }

  insertTag(tag: string): void {
    this.editedMessage.update((curr) => `${curr} ${tag}`.trim());
  }

  readonly previewText = computed(() => {
    const raw = this.editedMessage();
    const storeName = this.storeService.storeName() || 'Mi Tienda';
    return raw
      .replace(/{cliente}/g, 'Carlos')
      .replace(/{numero_pedido}/g, 'JAL-2026-0042')
      .replace(/{tienda}/g, storeName)
      .replace(/{total}/g, 'S/ 38.50')
      .replace(/{modalidad}/g, 'Delivery');
  });

  onSaveCurrentTemplate(): void {
    const tpl = this.currentTemplate();
    if (tpl) {
      this.settingsService.updateWhatsAppTemplate(tpl.id, this.editedMessage());
    }
  }

  onResetDefaults(): void {
    this.settingsService.resetWhatsAppTemplates();
    this.syncEditedMessage();
  }
}

