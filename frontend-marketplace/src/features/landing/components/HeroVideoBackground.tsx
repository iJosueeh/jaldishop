'use client';

import React, { useState, useRef, useEffect } from 'react';
import Image from 'next/image';

interface HeroVideoBackgroundProps {
  /**
   * Fast CDN video source (MP4 / WebM)
   * High quality loop of urban coffee shop & food establishment
   */
  videoUrl?: string;
  posterUrl?: string;
}

// Direct CDN Video URL: Urban coffee shop & artisan retail (Mixkit CDN 4350)
const DEFAULT_VIDEO_URL =
  'https://assets.mixkit.co/videos/4350/4350-720.mp4';

// High-res Urban coffee shop poster thumbnail
const DEFAULT_POSTER_URL =
  'https://assets.mixkit.co/videos/4350/4350-thumb-720-0.jpg';

export function HeroVideoBackground({
  videoUrl = process.env.NEXT_PUBLIC_HERO_VIDEO_URL || DEFAULT_VIDEO_URL,
  posterUrl = process.env.NEXT_PUBLIC_HERO_POSTER_URL || DEFAULT_POSTER_URL,
}: HeroVideoBackgroundProps) {
  const [isVideoLoaded, setIsVideoLoaded] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.play().catch(() => {
        // Fallback to static poster if browser restricts autoplay
      });
    }
  }, []);

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none -z-20 select-none">
      {/* 1. Static High-Res Poster Image */}
      <div
        className={`absolute inset-0 transition-opacity duration-700 ${
          isVideoLoaded ? 'opacity-0' : 'opacity-100'
        }`}
      >
        <Image
          src={posterUrl}
          alt="JaldiShop Urban Store Background"
          fill
          priority
          sizes="100vw"
          className="object-cover object-center filter brightness-[0.98] contrast-[1.05]"
        />
      </div>

      {/* 2. Responsive Mixkit CDN Video Loop (Active on all viewports) */}
      <video
        ref={videoRef}
        autoPlay
        muted
        loop
        playsInline
        preload="auto"
        onLoadedData={() => setIsVideoLoaded(true)}
        className={`absolute inset-0 w-full h-full object-cover object-center transition-opacity duration-700 filter brightness-[0.95] contrast-[1.08] saturate-[1.15] ${
          isVideoLoaded ? 'opacity-100' : 'opacity-0'
        }`}
      >
        <source src={videoUrl} type="video/mp4" />
      </video>

      {/* 3. Subtle Lateral Readable Gradient Mask (Lightweight: text is readable on left, video is clearly visible on right & center) */}
      <div className="absolute inset-0 bg-gradient-to-r from-[#faf7f2]/92 via-[#faf7f2]/50 to-transparent" />

      {/* 4. Bottom Blend Transition into the next section */}
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-[#faf7f2]" />
    </div>
  );
}
