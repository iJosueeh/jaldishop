import { Component, signal } from '@angular/core';
import { LegalFooter } from '../../shared/components/legal-footer/legal-footer';
import { LegalHeader } from '../../shared/components/legal-header/legal-header';
import { LegalHero } from './components/legal-hero/legal-hero';

@Component({
  imports: [LegalFooter, LegalHeader, LegalHero],
  selector: 'app-legal',
  styleUrl: './legal.css',
  templateUrl: './legal.html',
})
export class Legal {
  readonly activeTab = signal<'terms' | 'privacy'>('terms');

  setTab(tab: 'terms' | 'privacy'): void {
    this.activeTab.set(tab);
  }
}
