'use client';

import React, { useState } from 'react';
import { useCustomerAddresses } from '../hooks/useCustomerAddresses';
import { CreateCustomerAddressPayload, CustomerAddress } from '../types/customer.types';
import { AddressCard } from './AddressCard';
import { AddressModal } from './AddressModal';
import { DeleteAddressModal } from './DeleteAddressModal';
import { Button } from '@/shared/components/ui/Button';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { MapPin, Plus, AlertCircle, Sparkles, ShieldCheck } from 'lucide-react';
import { toast } from 'sonner';

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

  // Modal Handlers
  const handleOpenCreateModal = () => {
    if (!canAddAddress) {
      toast.error('Has alcanzado el límite máximo de 5 direcciones guardadas.');
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
        toast.success('Dirección actualizada correctamente.');
      } else {
        await createAddress(data);
        toast.success('Dirección agregada a tu cuenta.');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo guardar la dirección.';
      toast.error(msg);
      throw err;
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultAddress(id);
      toast.success('Dirección predeterminada actualizada.');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo actualizar la dirección predeterminada.';
      toast.error(msg);
    }
  };

  const handleConfirmDelete = async (id: string) => {
    try {
      await deleteAddress(id);
      toast.success('Dirección eliminada correctamente.');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar la dirección.';
      toast.error(msg);
      throw err;
    }
  };

  // Loading State
  if (isLoading) {
    return (
      <div className="space-y-6 w-full">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-stone-200/80">
          <Skeleton className="h-6 w-48 bg-stone-200" />
          <Skeleton className="h-10 w-36 bg-stone-200" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
            <Skeleton className="h-6 w-32 bg-stone-200" />
            <Skeleton className="h-4 w-48 bg-stone-200" />
            <Skeleton className="h-4 w-full bg-stone-200" />
          </div>
          <div className="p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
            <Skeleton className="h-6 w-32 bg-stone-200" />
            <Skeleton className="h-4 w-48 bg-stone-200" />
            <Skeleton className="h-4 w-full bg-stone-200" />
          </div>
        </div>
      </div>
    );
  }

  // Error State
  if (error) {
    return (
      <div className="p-8 rounded-2xl bg-white border border-rose-200 text-center max-w-xl mx-auto shadow-xs">
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-600 flex items-center justify-center mx-auto mb-3">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h3 className="text-base font-bold text-stone-900 mb-1">
          No pudimos consultar tus direcciones
        </h3>
        <p className="text-xs sm:text-sm text-rose-600 font-medium mb-4">{error}</p>
        <Button variant="outline" size="sm" onClick={() => reload()}>
          Reintentar
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 w-full">
      {/* Action Header & Counter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-stone-200/80">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-lg font-bold text-stone-900">Direcciones registradas</h3>
            <span
              className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                total >= 5
                  ? 'bg-amber-100 text-amber-800'
                  : 'bg-[#faf7f2] border border-stone-200 text-stone-700'
              }`}
            >
              {total} de 5 direcciones
            </span>
          </div>
          <p className="text-xs text-stone-500 mt-1">
            Puedes registrar hasta 5 direcciones de entrega frecuentes para tus compras en JaldiShop.
          </p>
        </div>

        <div>
          <Button
            type="button"
            variant="primary"
            size="md"
            onClick={handleOpenCreateModal}
            disabled={!canAddAddress}
            className="w-full sm:w-auto rounded-full font-bold shadow-md shadow-[#005141]/10 text-xs px-5"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            <span>Agregar dirección</span>
          </Button>
        </div>
      </div>

      {/* Limit Notice if 5 reached */}
      {!canAddAddress && (
        <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200/80 flex items-center gap-2.5 text-xs text-amber-900 font-medium">
          <AlertCircle className="w-4 h-4 text-amber-700 shrink-0" />
          <span>
            Has alcanzado el límite máximo permitido de 5 direcciones. Si deseas agregar una nueva, edita o elimina una existente.
          </span>
        </div>
      )}

      {/* Empty State */}
      {total === 0 ? (
        <div className="p-8 sm:p-12 rounded-2xl bg-white border border-stone-200/90 text-center max-w-lg mx-auto shadow-xs space-y-4">
          <div className="w-14 h-14 rounded-2xl bg-[#005141]/10 flex items-center justify-center text-[#005141] mx-auto">
            <MapPin className="w-7 h-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-stone-900">
              Aún no tienes direcciones guardadas
            </h3>
            <p className="text-xs sm:text-sm text-stone-500 leading-relaxed">
              Guarda tus direcciones habituales de casa, oficina o familiares para agilizar la entrega programada de tus compras.
            </p>
          </div>
          <div className="pt-2">
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={handleOpenCreateModal}
              className="rounded-full px-6 text-xs font-bold shadow-md shadow-[#005141]/10"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              <span>Registrar primera dirección</span>
            </Button>
          </div>
        </div>
      ) : (
        /* Address Cards Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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

      {/* Info Card: Seguridad & Cobertura */}
      <div className="p-5 rounded-2xl bg-[#faf7f2] border border-stone-200/80 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-xs text-stone-600">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-[#005141]/10 flex items-center justify-center text-[#005141] shrink-0">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <div>
            <p className="font-bold text-stone-800">Cobertura y Validación</p>
            <p className="text-[11px] text-stone-500">
              Las direcciones se validan con la base oficial del INEI. La disponibilidad de entrega depende del radio de cobertura configurado por cada comercio.
            </p>
          </div>
        </div>
      </div>

      {/* Create / Edit Modal */}
      <AddressModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        addressToEdit={addressToEdit}
        onSubmit={handleSaveAddress}
        isSaving={isCreating || isUpdating}
        isFirstAddress={total === 0}
      />

      {/* Delete Confirmation Modal */}
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
