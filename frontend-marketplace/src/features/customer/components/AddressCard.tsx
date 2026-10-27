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
    <div
      className={cn(
        'relative flex flex-col justify-between p-5 sm:p-6 rounded-2xl bg-white border transition-all duration-200 shadow-xs hover:shadow-md',
        address.isDefault
          ? 'border-[#005141] ring-1 ring-[#005141]/20'
          : 'border-stone-200/90 hover:border-stone-300'
      )}
    >
      <div className="space-y-4">
        {/* Header: Alias + Default Badge */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-[#faf7f2] border border-stone-200 text-stone-800">
              <MapPin className="w-3.5 h-3.5 text-[#005141]" />
              <span>{address.alias}</span>
            </span>

            {address.isDefault && (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 border border-emerald-200 text-[#005141]">
                <CheckCircle2 className="w-3 h-3 text-[#005141]" />
                <span>Predeterminada</span>
              </span>
            )}
          </div>

          {/* Quick Actions (Desktop & Mobile) */}
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => onEdit(address)}
              className="p-1.5 text-stone-500 hover:text-stone-900 hover:bg-stone-100 rounded-lg transition-colors"
              title="Editar dirección"
              aria-label={`Editar dirección ${address.alias}`}
            >
              <Pencil className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => onDelete(address)}
              className="p-1.5 text-stone-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
              title="Eliminar dirección"
              aria-label={`Eliminar dirección ${address.alias}`}
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Address & Ubigeo */}
        <div className="space-y-1">
          <p className="text-sm sm:text-base font-bold text-stone-900 leading-snug break-words">
            {address.addressLine}
          </p>
          <p className="text-xs sm:text-sm font-medium text-stone-600">
            {locationLabel}
          </p>
          {address.reference && (
            <div className="flex items-start gap-1.5 pt-1 text-xs text-stone-500">
              <Compass className="w-3.5 h-3.5 mt-0.5 shrink-0 text-stone-400" />
              <span className="italic leading-relaxed">{address.reference}</span>
            </div>
          )}
        </div>

        {/* Recipient Details */}
        <div className="pt-3 border-t border-stone-100 text-xs text-stone-600 space-y-1">
          <div className="flex items-center gap-2">
            <User className="w-3.5 h-3.5 text-stone-400 shrink-0" />
            <span className="font-semibold text-stone-800">{address.recipientName}</span>
          </div>
          <div className="flex items-center gap-2">
            <Phone className="w-3.5 h-3.5 text-stone-400 shrink-0" />
            <span>{address.recipientPhone}</span>
          </div>
        </div>
      </div>

      {/* Footer: Set Default Action if not default */}
      {!address.isDefault && (
        <div className="pt-4 mt-4 border-t border-stone-100">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onSetDefault(address.id)}
            disabled={isSettingDefault}
            className="w-full text-xs font-semibold text-stone-700 hover:text-[#005141] hover:border-[#005141]/30 py-1.5"
          >
            {isSettingDefault ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
            ) : (
              <Star className="w-3.5 h-3.5 text-stone-400 mr-1.5" />
            )}
            <span>Establecer como predeterminada</span>
          </Button>
        </div>
      )}
    </div>
  );
}
