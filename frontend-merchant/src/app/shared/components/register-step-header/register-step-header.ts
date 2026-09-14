import { Component, computed, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-register-step-header',
  styleUrl: './register-step-header.css',
  templateUrl: './register-step-header.html',
})
export class RegisterStepHeader {
  readonly currentStep = input.required<1 | 2 | 3>();
  readonly back = output<void>();

  readonly progressWidth = computed(() => {
    switch (this.currentStep()) {
      case 1:
        return 'w-1/3';
      case 2:
        return 'w-2/3';
      case 3:
        return 'w-full';
    }
  });

  readonly previousStepLabel = computed(() => {
    switch (this.currentStep()) {
      case 2:
        return 'Volver al paso 1';
      case 3:
        return 'Volver al paso 2';
      default:
        return '';
    }
  });

}
