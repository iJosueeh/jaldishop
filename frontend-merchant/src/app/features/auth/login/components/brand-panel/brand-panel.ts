import { Component, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matCheckCircleOutline } from '@ng-icons/material-symbols/outline';
import { environment } from '../../../../../../environments/environment';

@Component({
  imports: [NgIcon],
  providers: [
    provideIcons({
      matCheckCircleOutline,
    }),
  ],
  selector: 'app-brand-panel',
  styleUrl: './brand-panel.css',
  templateUrl: './brand-panel.html',
})
export class BrandPanel implements AfterViewInit {
  @ViewChild('videoPlayer') videoPlayer?: ElementRef<HTMLVideoElement>;
  readonly marketplaceUrl = environment.marketplaceUrl;

  ngAfterViewInit(): void {
    if (this.videoPlayer?.nativeElement) {
      const video = this.videoPlayer.nativeElement;
      video.muted = true;
      const playPromise = video.play();
      if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch(() => {
          // Autoplay policy fallback: poster remains visible
        });
      }
    }
  }
}


