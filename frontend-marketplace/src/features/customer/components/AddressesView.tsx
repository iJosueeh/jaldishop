'use client';

import React, { useState } from 'react';
import { useCustomerAddresses } from '../hooks/useCustomerAddresses';
import { CreateCustomerAddressPayload, CustomerAddress } from '../types/customer.types';
import { AddressCard } from './AddressCard';
import { AddressModal } from './AddressModal';
import { DeleteAddressModal } from './DeleteAddressModal';
import { AccountPageHeader } from './AccountPageHeader';
import { AccountEmptyState } from './AccountEmptyState';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { MapPin, Plus, AlertCircle, ShieldCheck } from 'lucide-react';
import { notify, notifyKeys } from '@/shared/utils/notify';

export function AddressesView() {
  const {
    addresses,
    total,
    canAddAddress,
    isLoading,
    error,
    createAddress,
    isCreating,
    updateAddress,
    isUpdating,
    setDefaultAddress,
    isSettingDefault,
    deleteAddress,
    isDeleting,
    reload,
  } = useCustomerAddresses();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [addressToEdit, setAddressToEdit] = useState<CustomerAddress | null>(null);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [addressToDelete, setAddressToDelete] = useState<CustomerAddress | null>(null);

  const handleOpenCreateModal = () => {
    if (!canAddAddress) {
      notify.error('Has alcanzado el límite máximo de 5 direcciones guardadas.', {
        id: notifyKeys.address('limit'),
      });
      return;
    }
    setAddressToEdit(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (addr: CustomerAddress) => {
    setAddressToEdit(addr);
    setIsModalOpen(true);
  };

  const handleOpenDeleteModal = (addr: CustomerAddress) => {
    setAddressToDelete(addr);
    setIsDeleteModalOpen(true);
  };

  const handleSaveAddress = async (data: CreateCustomerAddressPayload) => {
    try {
      if (addressToEdit) {
        await updateAddress({ id: addressToEdit.id, payload: data });
        notify.success('Dirección actualizada correctamente.', {
          id: notifyKeys.address('save'),
        });
      } else {
        await createAddress(data);
        notify.success('Dirección agregada a tu cuenta.', {
          id: notifyKeys.address('save'),
        });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo guardar la dirección.';
      notify.error(msg, { id: notifyKeys.address('save') });
      throw err;
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultAddress(id);
      notify.success('Dirección predeterminada actualizada.', {
        id: notifyKeys.address('default'),
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo actualizar la dirección predeterminada.';
      notify.error(msg, { id: notifyKeys.address('default') });
    }
  };

  const handleConfirmDelete = async (id: string) => {
    try {
      await deleteAddress(id);
      notify.success('Dirección eliminada correctamente.', {
        id: notifyKeys.address('delete'),
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar la dirección.';
      notify.error(msg, { id: notifyKeys.address('delete') });
      throw err;
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6 w-full" role="status" aria-busy="true" aria-label="Cargando direcciones">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-[#e7e0d6]">
          <Skeleton className="h-7 w-48 bg-[#f0eae0]" />
          <Skeleton className="h-11 w-40 bg-[#f0eae0] rounded-full" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <div className="p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-4">
            <Skeleton className="h-6 w-32 bg-[#f0eae0]" />
            <Skeleton className="h-4 w-48 bg-[#f0eae0]" />
            <Skeleton className="h-4 w-full bg-[#f0eae0]" />
          </div>
          <div className="p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-4">
            <Skeleton className="h-6 w-32 bg-[#f0eae0]" />
            <Skeleton className="h-4 w-48 bg-[#f0eae0]" />
            <Skeleton className="h-4 w-full bg-[#f0eae0]" />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        role="alert"
        className="p-8 rounded-xl bg-white border border-rose-200 text-center max-w-xl mx-auto"
      >
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h2 className="text-base font-bold text-[#1c1917] mb-1">
          No pudimos consultar tus direcciones
        </h2>
        <p className="text-xs sm:text-sm text-rose-700 font-medium mb-4">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()} className="min-h-[44px]">
          Reintentar
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 w-full">
      <AccountPageHeader
        title="Mis Direcciones"
        description="Puedes registrar hasta 5 direcciones de entrega frecuentes para tus compras en JaldiShop."
        badge={
          <Badge
            variant={total >= 5 ? 'warning' : 'default'}
            size="sm"
            className="tabular-nums font-bold"
          >
            {total} de 5 direcciones
          </Badge>
        }
        actions={
          <Button
            type="button"
            variant="primary"
            size="md"
            onClick={handleOpenCreateModal}
            disabled={!canAddAddress}
            className="min-h-[44px] w-full sm:w-auto rounded-full font-bold text-xs px-5"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            <span>Agregar dirección</span>
          </Button>
        }
      />

      {!canAddAddress && (
        <div className="p-3.5 rounded-xl bg-[#fffbeb] border border-[#fde68a] flex items-center gap-2.5 text-xs text-[#92400e] font-medium">
          <AlertCircle className="w-4 h-4 text-[#b45309] shrink-0" />
          <span>
            Has alcanzado el límite máximo permitido de 5 direcciones. Si deseas agregar una nueva, edita o elimina una existente.
          </span>
        </div>
      )}

      {total === 0 ? (
        <AccountEmptyState
          icon={<MapPin className="w-6 h-6" />}
          title="Aún no tienes direcciones guardadas"
          description="Guarda tus direcciones habituales de casa, oficina o familiares para agilizar la entrega programada de tus compras."
          action={
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={handleOpenCreateModal}
              className="min-h-[44px] rounded-full px-6 text-xs font-bold"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              <span>Registrar primera dirección</span>
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {addresses.map((address) => (
            <AddressCard
              key={address.id}
              address={address}
              onEdit={handleOpenEditModal}
              onDelete={handleOpenDeleteModal}
              onSetDefault={handleSetDefault}
              isSettingDefault={isSettingDefault}
            />
          ))}
        </div>
      )}

      <div className="p-4 sm:p-5 rounded-xl bg-white border border-[#e7e0d6] flex items-start sm:items-center gap-3 text-xs text-[#57534e]">
        <ShieldCheck className="w-4 h-4 text-[#005141] shrink-0 mt-0.5 sm:mt-0" />
        <div>
          <p className="font-bold text-[#1c1917]">Cobertura y Validación</p>
          <p className="text-xs text-[#57534e]">
            Las direcciones se validan con la base oficial del INEI. La disponibilidad de entrega depende del radio de cobertura configurado por cada comercio.
          </p>
        </div>
      </div>

      <AddressModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        addressToEdit={addressToEdit}
        onSubmit={handleSaveAddress}
        isSaving={isCreating || isUpdating}
        isFirstAddress={total === 0}
      />

      <DeleteAddressModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        address={addressToDelete}
        onConfirm={handleConfirmDelete}
        isDeleting={isDeleting}
        hasMultipleAddresses={total > 1}
      />
    </div>
  );
}
