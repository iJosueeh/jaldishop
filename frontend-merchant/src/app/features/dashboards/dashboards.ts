import { Component } from '@angular/core';
import { DashboardHeader } from './components/dashboard-header/dashboard-header';
import { DashboardKpis } from './components/dashboard-kpis/dashboard-kpis';
import { PriorityOrders } from './components/priority-orders/priority-orders';
import { LogisticsAlerts } from './components/logistics-alerts/logistics-alerts';
import { QuickActions } from './components/quick-actions/quick-actions';

@Component({
  imports: [DashboardHeader, DashboardKpis, PriorityOrders, LogisticsAlerts, QuickActions],
  selector: 'app-dashboards',
  styleUrl: './dashboards.css',
  templateUrl: './dashboards.html',
})
export class Dashboards {}
