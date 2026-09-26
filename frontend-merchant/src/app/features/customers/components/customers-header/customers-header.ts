import { Component, input } from '@angular/core';

@Component({
  selector: 'app-customers-header',
  imports: [],
  templateUrl: './customers-header.html',
  styleUrl: './customers-header.css',
})
export class CustomersHeader {
  readonly totalCustomers = input<number>(0);
}
