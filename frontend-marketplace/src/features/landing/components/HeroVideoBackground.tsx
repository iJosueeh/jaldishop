'use client';

import React, { useState, useRef, useEffect } from 'react';
import Image from 'next/image';

interface HeroVideoBackgroundProps {
  /**
   * Fast CDN video source (MP4 / WebM)
   * High quality loop of artisanal bakery, dough making and warm kitchen
   */
  videoUrl?: string;
  posterUrl?: string;
}

// Ultra-optimized warm artisanal bakery CDN video loop (Mixkit CDN)
const DEFAULT_VIDEO_URL =
  'https://assets.mixkit.co/videos/preview/mixkit-baker-putting-bread-dough-into-the-oven-41484-large.mp4';

// High-res warm artisan kitchen poster fallback
const DEFAULT_POSTER_URL =
  'https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=1600&auto=format&fit=crop';

export function HeroVideoBackground({
  videoUrl = process.env.NEXT_PUBLIC_HERO_VIDEO_URL || DEFAULT_VIDEO_URL,
  posterUrl = process.env.NEXT_PUBLIC_HERO_POSTER_URL || DEFAULT_POSTER_URL,
}: HeroVideoBackgroundProps) {
  const [isVideoLoaded, setIsVideoLoaded] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.play().catch(() => {
        // Fallback to static poster if autoplay is restricted
      });
    }
  }, []);

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none -z-20 select-none">
      {/* 1. Static High-Res Poster Image (Always immediate for 0 ms LCP) */}
      <div
        className={`absolute inset-0 transition-opacity duration-1000 ${
          isVideoLoaded ? 'opacity-0' : 'opacity-100'
        }`}
      >
        <Image
          src={posterUrl}
          alt="JaldiShop Artisan Kitchen"
          fill
          priority
          sizes="100vw"
          className="object-cover object-center filter brightness-[0.98] saturate-[1.15]"
        />
      </div>

      {/* 2. Responsive Mixkit CDN Video Loop (Clear & vibrant playback) */}
      <video
        ref={videoRef}
        autoPlay
        muted
        loop
        playsInline
        preload="auto"
        onLoadedData={() => setIsVideoLoaded(true)}
        className={`hidden sm:block absolute inset-0 w-full h-full object-cover object-center transition-opacity duration-1000 filter brightness-[0.96] saturate-[1.2] ${
          isVideoLoaded ? 'opacity-100' : 'opacity-0'
        }`}
      >
        <source src={videoUrl} type="video/mp4" />
      </video>

      {/* 3. Refined Lateral Mask: Left is softly opaque for text readability, Right lets the video shine through */}
      <div className="absolute inset-0 bg-gradient-to-r from-[#faf7f2]/95 via-[#faf7f2]/70 to-[#faf7f2]/30" />

      {/* 4. Vertical Smooth Section Fade (Blends seamlessly into the next sections) */}
      <div className="absolute inset-0 bg-gradient-to-b from-[#faf7f2]/30 via-transparent to-[#faf7f2]" />

      {/* 5. Decorative Warm Ambient Glows (Jade & Honey) */}
      <div className="absolute top-10 right-10 w-[550px] h-[550px] bg-[#feae2c]/12 blur-[130px] rounded-full" />
      <div className="absolute bottom-0 left-10 w-[450px] h-[450px] bg-[#005141]/8 blur-[120px] rounded-full" />
    </div>
  );
}
