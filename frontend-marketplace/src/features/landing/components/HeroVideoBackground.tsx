'use client';

import React, { useState, useRef, useEffect } from 'react';
import Image from 'next/image';

interface HeroVideoBackgroundProps {
  videoUrl?: string;
  posterUrl?: string;
}

// Local optimized video asset & CDN fallback
const LOCAL_VIDEO_URL = '/videos/hero-coffee.mp4';
const CDN_VIDEO_URL = 'https://assets.mixkit.co/videos/4350/4350-720.mp4';
const DEFAULT_POSTER_URL = '/videos/hero-coffee-poster.jpg';

export function HeroVideoBackground({
  videoUrl = process.env.NEXT_PUBLIC_HERO_VIDEO_URL || LOCAL_VIDEO_URL,
  posterUrl = process.env.NEXT_PUBLIC_HERO_POSTER_URL || DEFAULT_POSTER_URL,
}: HeroVideoBackgroundProps) {
  const [isVideoLoaded, setIsVideoLoaded] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.muted = true;
    video.defaultMuted = true;

    const playPromise = video.play();
    if (playPromise !== undefined) {
      playPromise
        .then(() => {
          setIsVideoLoaded(true);
        })
        .catch(() => {
          // Autoplay fallback
        });
    }
  }, []);

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 select-none bg-stone-900">
      {/* 1. Static High-Res Poster Image Placeholder */}
      <div
        className={`absolute inset-0 transition-opacity duration-1000 ${
          isVideoLoaded ? 'opacity-0' : 'opacity-100'
        }`}
      >
        <Image
          src={posterUrl}
          alt="JaldiShop Urban Store Background"
          fill
          priority
          sizes="100vw"
          className="object-cover object-center filter brightness-[0.7] contrast-[1.05]"
        />
      </div>

      {/* 2. Seamless Responsive Video Loop */}
      <video
        ref={videoRef}
        autoPlay
        muted
        loop
        playsInline
        preload="none"
        onLoadedData={() => setIsVideoLoaded(true)}
        onPlaying={() => setIsVideoLoaded(true)}
        className={`absolute inset-0 w-full h-full object-cover object-center transition-opacity duration-700 filter brightness-[0.7] contrast-[1.08] saturate-[1.1] ${
          isVideoLoaded ? 'opacity-100' : 'opacity-0'
        }`}
      >
        <source src={videoUrl} type="video/mp4" />
        <source src={CDN_VIDEO_URL} type="video/mp4" />
      </video>

      {/* 3. Clean, Uniform Cinematic Tint for Maximum Contrast (without murky gradients) */}
      <div className="absolute inset-0 bg-black/45 backdrop-contrast-105" />
    </div>
  );
}


