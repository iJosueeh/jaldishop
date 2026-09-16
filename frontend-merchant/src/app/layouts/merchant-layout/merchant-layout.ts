import { Component } from '@angular/core';
import { Siderbar } from './components/siderbar/siderbar';
import { Header } from './components/header/header';
import { RouterOutlet } from '@angular/router';

@Component({
  imports: [Siderbar, Header, RouterOutlet],
  selector: 'app-merchant-layout',
  styleUrl: './merchant-layout.css',
  templateUrl: './merchant-layout.html',
})
export class MerchantLayout {}
