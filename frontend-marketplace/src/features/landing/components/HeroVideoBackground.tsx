"use client";

import React, { useState, useRef, useEffect } from "react";
import Image from "next/image";

interface HeroVideoBackgroundProps {
  videoUrl?: string;
  posterUrl?: string;
}

const DEFAULT_VIDEO_URL =
  "https://assets.mixkit.co/videos/preview/mixkit-baker-putting-bread-dough-into-the-oven-41484-large.mp4";

const DEFAULT_POSTER_URL =
  "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=1600&auto=format&fit=crop";

export function HeroVideoBackground({
  videoUrl = DEFAULT_VIDEO_URL,
  posterUrl = DEFAULT_POSTER_URL,
}: HeroVideoBackgroundProps) {
  const [isVideoLoaded, setIsVideoLoaded] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.play().catch(() => {});
    }
  }, []);

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none -z-20 select-none">
      <div
        className={`absolute inset-0 transition-opacity duration-1000 ${
          isVideoLoaded ? "opacity-0" : "opacity-100"
        }`}
      >
        <Image
          src={posterUrl}
          alt="JaldiShop Artisan Kitchen"
          fill
          priority
          sizes="100vw"
          className="object-cover object-center filter brightness-[0.95] saturate-[1.1]"
        />
      </div>

      <video
        ref={videoRef}
        autoPlay
        muted
        loop
        playsInline
        preload="metadata"
        onLoadedData={() => setIsVideoLoaded(true)}
        className={`hidden sm:block absolute inset-0 w-full h-full object-cover object-center transition-opacity duration-1000 filter brightness-[0.92] saturate-[1.15] ${
          isVideoLoaded ? "opacity-100" : "opacity-0"
        }`}
      >
        <source src={videoUrl} type="video/mp4" />
      </video>

      <div className="absolute inset-0 bg-gradient-to-r from-[#faf7f2]/95 via-[#faf7f2]/88 to-[#faf7f2]/75" />

      <div className="absolute inset-0 bg-gradient-to-b from-[#faf7f2]/40 via-transparent to-[#faf7f2]" />

      <div className="absolute top-10 right-10 w-[550px] h-[550px] bg-[#feae2c]/15 blur-[140px] rounded-full" />
      <div className="absolute bottom-0 left-10 w-[500px] h-[500px] bg-[#005141]/10 blur-[130px] rounded-full" />
    </div>
  );
}
