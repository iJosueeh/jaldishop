import React from 'react';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { Container } from '@/shared/components/ui/Container';

export function StorefrontSkeleton() {
  return (
    <div className="min-h-screen min-w-0 overflow-x-clip bg-[#faf7f2] pb-20">
      {/* Header Skeleton */}
      <div className="overflow-hidden bg-[#141413] pb-10 pt-24 sm:pb-12">
        <Container size="lg">
          <div className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-center sm:gap-6">
            <Skeleton className="h-20 w-20 shrink-0 rounded-3xl bg-white/10 sm:h-24 sm:w-24" />
            <div className="min-w-0 flex-1 space-y-3">
              <Skeleton className="h-7 w-48 max-w-full bg-white/10 sm:h-8 sm:w-64" />
              <Skeleton className="h-4 w-full max-w-96 bg-white/10" />
              <div className="flex max-w-full flex-wrap gap-3">
                <Skeleton className="h-5 w-24 max-w-full bg-white/10" />
                <Skeleton className="h-5 w-32 max-w-full bg-white/10" />
              </div>
            </div>
          </div>
        </Container>
      </div>

      {/* Main Content Skeleton */}
      <Container size="lg" className="min-w-0 space-y-6 pt-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-16 rounded-2xl" />
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 pt-6">
          <div className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-4">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <Skeleton key={i} className="h-52 rounded-2xl" />
            ))}
          </div>
          <div className="lg:col-span-4">
            <Skeleton className="h-72 rounded-2xl" />
          </div>
        </div>
      </Container>
    </div>
  );
}
