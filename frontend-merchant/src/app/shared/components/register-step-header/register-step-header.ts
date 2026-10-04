import { Component, computed, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-register-step-header',
  styleUrl: './register-step-header.css',
  templateUrl: './register-step-header.html',
})
export class RegisterStepHeader {
  readonly currentStep = input.required<1 | 2 | 3 | 4>();
  readonly isExistingUser = input<boolean>(false);
  readonly back = output<void>();

  readonly progressWidth = computed(() => {
    if (this.isExistingUser()) {
      switch (this.currentStep()) {
        case 2:
          return 'w-1/3';
        case 3:
          return 'w-2/3';
        case 4:
          return 'w-full';
        default:
          return 'w-0';
      }
    }

    switch (this.currentStep()) {
      case 1:
        return 'w-1/4';
      case 2:
        return 'w-2/4';
      case 3:
        return 'w-3/4';
      case 4:
        return 'w-full';
    }
  });

  readonly previousStepLabel = computed(() => {
    if (this.isExistingUser()) {
      switch (this.currentStep()) {
        case 2:
          return 'Cerrar sesión';
        case 3:
          return 'Volver al paso 1 (Marca)';
        case 4:
          return 'Volver al paso 2 (Ubicación)';
        default:
          return '';
      }
    }

    switch (this.currentStep()) {
      case 2:
        return 'Volver al paso 1 (Cuenta)';
      case 3:
        return 'Volver al paso 2 (Marca)';
      case 4:
        return 'Volver al paso 3 (Ubicación)';
      default:
        return '';
    }
  });

}
