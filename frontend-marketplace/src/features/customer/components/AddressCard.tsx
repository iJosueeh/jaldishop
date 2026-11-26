'use client';

import React from 'react';
import { CustomerAddress } from '../types/customer.types';
import { formatUbigeoLocation } from '@/shared/utils/ubigeo';
import { Button } from '@/shared/components/ui/Button';
import {
  MapPin,
  CheckCircle2,
  Pencil,
  Trash2,
  User,
  Phone,
  Compass,
  Star,
  Loader2,
} from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface AddressCardProps {
  address: CustomerAddress;
  onEdit: (address: CustomerAddress) => void;
  onDelete: (address: CustomerAddress) => void;
  onSetDefault: (id: string) => void;
  isSettingDefault?: boolean;
}

export function AddressCard({
  address,
  onEdit,
  onDelete,
  onSetDefault,
  isSettingDefault = false,
}: AddressCardProps) {
  const locationLabel = formatUbigeoLocation(
    address.department,
    address.province,
    address.district
  );

  return (
    <article
      className={cn(
        'relative flex flex-col justify-between p-5 rounded-xl bg-white border transition-colors duration-150',
        address.isDefault
          ? 'border-[#005141]'
          : 'border-[#e7e0d6] hover:border-[#005141]/40'
      )}
    >
      <div className="space-y-4">
        {/* Header: Alias + Default Badge */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-[#faf7f2] border border-[#e7e0d6] text-[#1c1917]">
              <MapPin className="w-3.5 h-3.5 text-[#005141]" />
              <span>{address.alias}</span>
            </span>

            {address.isDefault && (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#f0fdfa] border border-[#ccfbf1] text-[#005141]">
                <CheckCircle2 className="w-3 h-3 text-[#005141]" />
                <span>Predeterminada</span>
              </span>
            )}
          </div>

          {/* Quick Actions (min 44px touch targets) */}
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => onEdit(address)}
              className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center text-[#57534e] hover:text-[#1c1917] hover:bg-[#f7f3ec] rounded-lg transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#005141]"
              title="Editar dirección"
              aria-label={`Editar dirección ${address.alias}`}
            >
              <Pencil className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => onDelete(address)}
              className="min-h-[44px] min-w-[44px] inline-flex items-center justify-center text-[#57534e] hover:text-rose-700 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-600"
              title="Eliminar dirección"
              aria-label={`Eliminar dirección ${address.alias}`}
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Address & Ubigeo */}
        <div className="space-y-1">
          <p className="text-sm sm:text-base font-bold text-[#1c1917] leading-snug break-words">
            {address.streetAddress || address.addressLine}
          </p>
          <p className="text-xs sm:text-sm font-medium text-[#57534e]">
            {locationLabel}
          </p>
          {address.reference && (
            <div className="flex items-start gap-1.5 pt-1 text-xs text-[#57534e]">
              <Compass className="w-3.5 h-3.5 mt-0.5 shrink-0 text-[#78716c]" />
              <span className="italic leading-relaxed">{address.reference}</span>
            </div>
          )}
        </div>

        {/* Recipient Details */}
        <div className="pt-3 border-t border-[#e7e0d6]/70 text-xs text-[#57534e] space-y-1">
          <div className="flex items-center gap-2">
            <User className="w-3.5 h-3.5 text-[#78716c] shrink-0" />
            <span className="font-semibold text-[#1c1917]">{address.recipientName}</span>
          </div>
          <div className="flex items-center gap-2">
            <Phone className="w-3.5 h-3.5 text-[#78716c] shrink-0" />
            <span className="tabular-nums">{address.recipientPhone}</span>
          </div>
        </div>
      </div>

      {/* Footer: Set Default Action */}
      {!address.isDefault && (
        <div className="pt-4 mt-4 border-t border-[#e7e0d6]/70">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onSetDefault(address.id)}
            disabled={isSettingDefault}
            className="min-h-[44px] w-full rounded-lg border-[#e7e0d6] text-xs font-semibold text-[#1c1917] hover:text-[#005141] hover:border-[#005141]/40"
          >
            {isSettingDefault ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
            ) : (
              <Star className="w-3.5 h-3.5 text-[#57534e] mr-1.5" />
            )}
            <span>Establecer como predeterminada</span>
          </Button>
        </div>
      )}
    </article>
  );
}
