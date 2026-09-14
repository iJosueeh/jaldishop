import { Component, computed, input } from '@angular/core';

export type AvatarSize = 'sm' | 'md' | 'lg';

@Component({
  imports: [],
  selector: 'app-business-avatar',
  styleUrl: './business-avatar.css',
  templateUrl: './business-avatar.html',
})
export class BusinessAvatar {
  readonly name = input<string>('');
  readonly size = input<AvatarSize>('md');

  readonly initial = computed(() => {
    return this.name()?.trim().charAt(0).toUpperCase() || 'J';
  });

  readonly sizeClasses = computed(() => {
    switch (this.size()) {
      case 'sm':
        return 'w-10 h-10 rounded-xl text-base';
      case 'lg':
        return 'w-14 h-14 rounded-2xl text-2xl border-2';
      case 'md':
      default:
        return 'w-12 h-12 rounded-xl text-lg border';
    }
  });
}
