import { Component } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-legal-footer',
  styleUrl: './legal-footer.css',
  templateUrl: './legal-footer.html',
})
export class LegalFooter {
  currentYear: number = new Date().getFullYear();
}
