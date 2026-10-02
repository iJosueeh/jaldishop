import React from 'react';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { Container } from '@/shared/components/ui/Container';

export function StorefrontSkeleton() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 pb-20">
      {/* Header Skeleton */}
      <div className="bg-slate-900 pt-24 pb-12">
        <Container size="lg">
          <div className="flex items-center gap-6">
            <Skeleton className="w-24 h-24 rounded-3xl bg-slate-800" />
            <div className="space-y-3 flex-1">
              <Skeleton className="h-8 w-64 bg-slate-800" />
              <Skeleton className="h-4 w-96 bg-slate-800" />
              <div className="flex gap-3">
                <Skeleton className="h-5 w-24 bg-slate-800" />
                <Skeleton className="h-5 w-32 bg-slate-800" />
              </div>
            </div>
          </div>
        </Container>
      </div>

      {/* Main Content Skeleton */}
      <Container size="lg" className="pt-8 space-y-6">
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
