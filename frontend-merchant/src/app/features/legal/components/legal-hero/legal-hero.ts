import { Component, input, output } from '@angular/core';
import { LegalContent } from '../legal-content/legal-content';
import { PrivacyContent } from '../privacy-content/privacy-content';

@Component({
  imports: [LegalContent, PrivacyContent],
  selector: 'app-legal-hero',
  styleUrl: './legal-hero.css',
  templateUrl: './legal-hero.html',
})
export class LegalHero {
  readonly activeTab = input<'terms' | 'privacy'>('terms');
  readonly tabChange = output<'terms' | 'privacy'>();

  setTab(tab: 'terms' | 'privacy'): void {
    this.tabChange.emit(tab);
  }
}
