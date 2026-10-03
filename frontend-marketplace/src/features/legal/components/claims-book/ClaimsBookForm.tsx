'use client';

import React, { useState } from 'react';
import {
  HelpCircle,
  ShieldCheck,
  Send,
  User,
  ShoppingBag,
  MessageSquare,
} from 'lucide-react';
import {
  ClaimFormData,
  ClaimSubmissionResult,
} from '../../types/legal.types';
import { validateClaimForm, ClaimValidationErrors } from './claimsBook.schema';
import { ClaimsBookReceipt } from './ClaimsBookReceipt';

const INITIAL_FORM_DATA: ClaimFormData = {
  fullName: '',
  documentType: 'DNI',
  documentNumber: '',
  email: '',
  phone: '',
  address: '',
  department: 'Lima',
  province: 'Lima',
  district: '',
  isMinor: false,
  parentName: '',
  goodType: 'PRODUCTO',
  claimedAmount: 0,
  orderNumber: '',
  storeName: '',
  goodDescription: '',
  claimType: 'RECLAMO',
  claimDetail: '',
  consumerRequest: '',
  acceptTerms: false,
};

export function ClaimsBookForm() {
  const [formData, setFormData] = useState<ClaimFormData>(INITIAL_FORM_DATA);
  const [errors, setErrors] = useState<ClaimValidationErrors>({});
  const [receipt, setReceipt] = useState<ClaimSubmissionResult | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    const checked = (e.target as HTMLInputElement).checked;

    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));

    if (errors[name as keyof ClaimValidationErrors]) {
      setErrors((prev) => ({
        ...prev,
        [name]: undefined,
      }));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const validation = validateClaimForm(formData);

    if (!validation.isValid) {
      setErrors(validation.errors);
      // Scroll to the first error smoothly
      const firstErrorKey = Object.keys(validation.errors)[0];
      const errorEl = document.getElementById(`field-${firstErrorKey}`);
      if (errorEl) {
        errorEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    setIsSubmitting(true);

    // Simulate async submission and correlative generation
    setTimeout(() => {
      const now = new Date();
      const codeNumber = Math.floor(1000 + Math.random() * 9000);
      const generatedCode = `JALDI-REC-${now.getFullYear()}-${codeNumber}`;

      // Calculate 15 working days deadline
      const responseDate = new Date(now);
      let workingDaysAdded = 0;
      while (workingDaysAdded < 15) {
        responseDate.setDate(responseDate.getDate() + 1);
        const dayOfWeek = responseDate.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6) {
          workingDaysAdded++;
        }
      }

      const submissionResult: ClaimSubmissionResult = {
        claimCode: generatedCode,
        submittedAt: now.toLocaleDateString('es-PE', {
          day: '2-digit',
          month: 'long',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        }),
        maxResponseDate: responseDate.toLocaleDateString('es-PE', {
          day: '2-digit',
          month: 'long',
          year: 'numeric',
        }),
        data: { ...formData },
      };

      setReceipt(submissionResult);
      setIsSubmitting(false);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }, 400);
  };

  if (receipt) {
    return (
      <ClaimsBookReceipt
        receipt={receipt}
        onReset={() => {
          setReceipt(null);
          setFormData(INITIAL_FORM_DATA);
          setErrors({});
        }}
      />
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-8" noValidate>
      {/* Informative Guidance Card (Diferencia legal entre Reclamo y Queja) */}
      <div className="p-4 sm:p-5 rounded-2xl bg-amber-50/80 border border-amber-200/90 text-amber-950 space-y-2 text-xs sm:text-sm">
        <div className="flex items-center gap-2 font-bold text-amber-900">
          <HelpCircle className="w-4 h-4 text-amber-700" />
          <span>Información Legal para el Consumidor (Ley N° 29571)</span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
          <div className="bg-white/80 p-3 rounded-xl border border-amber-200">
            <strong className="block text-rose-800 font-bold mb-0.5">Reclamo:</strong>
            <span className="text-[11px] sm:text-xs text-stone-600 leading-relaxed block">
              Disconformidad relacionada directamente a los productos o servicios adquiridos (ej. producto incompleto, en mal estado, error en la orden).
            </span>
          </div>
          <div className="bg-white/80 p-3 rounded-xl border border-amber-200">
            <strong className="block text-amber-800 font-bold mb-0.5">Queja:</strong>
            <span className="text-[11px] sm:text-xs text-stone-600 leading-relaxed block">
              Malestar o descontento respecto a la atención al público brindada por el personal o la plataforma (sin vincular directamente al bien adquirido).
            </span>
          </div>
        </div>
      </div>

      {/* BLOQUE 1: IDENTIFICACIÓN DEL CONSUMIDOR */}
      <div className="bg-white rounded-3xl border border-stone-200 p-5 sm:p-7 space-y-5 shadow-xs">
        <div className="flex items-center gap-2.5 pb-3 border-b border-stone-100">
          <div className="w-8 h-8 rounded-xl bg-emerald-50 text-[#005141] flex items-center justify-center font-bold">
            <User className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base sm:text-lg font-bold text-[#1c1917]">
              1. Identificación del Consumidor Reclamante
            </h3>
            <p className="text-xs text-stone-500">Datos personales conforme a la Ley N° 29733</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Nombre completo */}
          <div id="field-fullName" className="sm:col-span-2 space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Nombre y Apellido Completo <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="Ej. Carmen Rosa Rodríguez Flores"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden ${
                errors.fullName
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.fullName && <p className="text-[11px] text-rose-600 font-medium">{errors.fullName}</p>}
          </div>

          {/* Tipo Documento */}
          <div id="field-documentType" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Tipo de Documento <span className="text-rose-500">*</span>
            </label>
            <select
              name="documentType"
              value={formData.documentType}
              onChange={handleChange}
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm bg-white focus:border-[#005141] outline-hidden cursor-pointer"
            >
              <option value="DNI">DNI (Documento Nacional de Identidad)</option>
              <option value="CE">Carné de Extranjería (CE)</option>
              <option value="PASAPORTE">Pasaporte</option>
              <option value="RUC">RUC (Persona Natural con Negocio)</option>
            </select>
          </div>

          {/* Número Documento */}
          <div id="field-documentNumber" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Número de Documento <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              name="documentNumber"
              value={formData.documentNumber}
              onChange={handleChange}
              placeholder={formData.documentType === 'DNI' ? '8 dígitos' : 'N° Documento'}
              maxLength={formData.documentType === 'DNI' ? 8 : 12}
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm font-mono transition-all outline-hidden ${
                errors.documentNumber
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.documentNumber && (
              <p className="text-[11px] text-rose-600 font-medium">{errors.documentNumber}</p>
            )}
          </div>

          {/* Teléfono */}
          <div id="field-phone" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Teléfono de Contacto (9 dígitos) <span className="text-rose-500">*</span>
            </label>
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              placeholder="987654321"
              maxLength={9}
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm font-mono transition-all outline-hidden ${
                errors.phone
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.phone && <p className="text-[11px] text-rose-600 font-medium">{errors.phone}</p>}
          </div>

          {/* Correo Electrónico */}
          <div id="field-email" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Correo Electrónico (donde recibirás la constancia) <span className="text-rose-500">*</span>
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="tucorreo@ejemplo.com"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden ${
                errors.email
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.email && <p className="text-[11px] text-rose-600 font-medium">{errors.email}</p>}
          </div>

          {/* Domicilio */}
          <div id="field-address" className="sm:col-span-2 space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Dirección de Domicilio <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              name="address"
              value={formData.address}
              onChange={handleChange}
              placeholder="Av. Larco 1234, Dpto. 401"
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden ${
                errors.address
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.address && <p className="text-[11px] text-rose-600 font-medium">{errors.address}</p>}
          </div>

          {/* Ubigeo básico: Departamento, Provincia, Distrito */}
          <div id="field-department" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">Departamento</label>
            <input
              type="text"
              name="department"
              value={formData.department}
              onChange={handleChange}
              placeholder="Lima"
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm outline-hidden focus:border-[#005141]"
            />
          </div>

          <div id="field-province" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">Provincia</label>
            <input
              type="text"
              name="province"
              value={formData.province}
              onChange={handleChange}
              placeholder="Lima"
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm outline-hidden focus:border-[#005141]"
            />
          </div>

          <div id="field-district" className="sm:col-span-2 space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Distrito <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              name="district"
              value={formData.district}
              onChange={handleChange}
              placeholder="Ej. Miraflores, San Isidro, Surco..."
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden ${
                errors.district
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141] focus:ring-1 focus:ring-[#005141]'
              }`}
            />
            {errors.district && <p className="text-[11px] text-rose-600 font-medium">{errors.district}</p>}
          </div>

          {/* Casilla Menor de Edad */}
          <div className="sm:col-span-2 pt-2">
            <label className="flex items-center gap-2 cursor-pointer select-none text-xs text-stone-700 font-semibold">
              <input
                type="checkbox"
                name="isMinor"
                checked={formData.isMinor}
                onChange={handleChange}
                className="w-4 h-4 rounded text-[#005141] border-stone-300 focus:ring-[#005141]"
              />
              <span>El consumidor es menor de edad</span>
            </label>
          </div>

          {formData.isMinor && (
            <div id="field-parentName" className="sm:col-span-2 space-y-1 bg-stone-50 p-3.5 rounded-xl border border-stone-200">
              <label className="text-xs font-bold text-stone-800">
                Nombre del Padre, Madre o Apoderado <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                name="parentName"
                value={formData.parentName}
                onChange={handleChange}
                placeholder="Nombre completo del representante legal"
                className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm bg-white outline-hidden focus:border-[#005141]"
              />
              {errors.parentName && <p className="text-[11px] text-rose-600 font-medium">{errors.parentName}</p>}
            </div>
          )}
        </div>
      </div>

      {/* BLOQUE 2: BIEN CONTRATADO */}
      <div className="bg-white rounded-3xl border border-stone-200 p-5 sm:p-7 space-y-5 shadow-xs">
        <div className="flex items-center gap-2.5 pb-3 border-b border-stone-100">
          <div className="w-8 h-8 rounded-xl bg-orange-50 text-[#ea580c] flex items-center justify-center font-bold">
            <ShoppingBag className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base sm:text-lg font-bold text-[#1c1917]">
              2. Identificación del Bien Contratado
            </h3>
            <p className="text-xs text-stone-500">Producto gastronómico o servicio de intermediación</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Tipo de bien: Producto vs Servicio */}
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs font-bold text-stone-700 block mb-1">
              Tipo de Bien <span className="text-rose-500">*</span>
            </label>
            <div className="flex items-center gap-4">
              <label className="inline-flex items-center gap-2 cursor-pointer text-xs sm:text-sm font-semibold text-stone-700 bg-stone-50 px-4 py-2 rounded-xl border border-stone-200 hover:bg-stone-100 transition-colors">
                <input
                  type="radio"
                  name="goodType"
                  value="PRODUCTO"
                  checked={formData.goodType === 'PRODUCTO'}
                  onChange={handleChange}
                  className="text-[#005141] focus:ring-[#005141]"
                />
                <span>Producto (Alimentos / Bebidas)</span>
              </label>

              <label className="inline-flex items-center gap-2 cursor-pointer text-xs sm:text-sm font-semibold text-stone-700 bg-stone-50 px-4 py-2 rounded-xl border border-stone-200 hover:bg-stone-100 transition-colors">
                <input
                  type="radio"
                  name="goodType"
                  value="SERVICIO"
                  checked={formData.goodType === 'SERVICIO'}
                  onChange={handleChange}
                  className="text-[#005141] focus:ring-[#005141]"
                />
                <span>Servicio (Plataforma / Delivery)</span>
              </label>
            </div>
          </div>

          {/* Monto Reclamado */}
          <div id="field-claimedAmount" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Monto Reclamado (Soles S/) <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              name="claimedAmount"
              value={formData.claimedAmount}
              onChange={handleChange}
              step="0.10"
              min="0"
              placeholder="0.00"
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm font-mono outline-hidden focus:border-[#005141]"
            />
            {errors.claimedAmount && <p className="text-[11px] text-rose-600 font-medium">{errors.claimedAmount}</p>}
          </div>

          {/* N° Pedido / Tienda (Opcional) */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              N° de Pedido o Nombre de la Tienda (Opcional)
            </label>
            <input
              type="text"
              name="orderNumber"
              value={formData.orderNumber}
              onChange={handleChange}
              placeholder="Ej. #JALDI-1048 o Panadería Don Pepe"
              className="w-full px-3.5 py-2.5 rounded-xl border border-stone-200 text-xs sm:text-sm outline-hidden focus:border-[#005141]"
            />
          </div>

          {/* Descripción del Bien */}
          <div id="field-goodDescription" className="sm:col-span-2 space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Descripción del Producto o Servicio Adquirido <span className="text-rose-500">*</span>
            </label>
            <textarea
              name="goodDescription"
              rows={2}
              value={formData.goodDescription}
              onChange={handleChange}
              placeholder="Ej. 2 hogazas de masa madre y 1 café filtrado solicitados para la franja 10:00 - 11:00 AM..."
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden resize-none ${
                errors.goodDescription
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141]'
              }`}
            />
            {errors.goodDescription && (
              <p className="text-[11px] text-rose-600 font-medium">{errors.goodDescription}</p>
            )}
          </div>
        </div>
      </div>

      {/* BLOQUE 3: DETALLE DE LA RECLAMACIÓN Y PEDIDO */}
      <div className="bg-white rounded-3xl border border-stone-200 p-5 sm:p-7 space-y-5 shadow-xs">
        <div className="flex items-center gap-2.5 pb-3 border-b border-stone-100">
          <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
            <MessageSquare className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base sm:text-lg font-bold text-[#1c1917]">
              3. Detalle de la Reclamación y Pedido del Consumidor
            </h3>
            <p className="text-xs text-stone-500">Hechos ocurridos y solución solicitada</p>
          </div>
        </div>

        <div className="space-y-4">
          {/* Selector de Reclamo vs Queja */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-stone-700 block">
              Tipo de Reclamación <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-start gap-3 ${
                  formData.claimType === 'RECLAMO'
                    ? 'border-rose-500 bg-rose-50/40 text-[#1c1917]'
                    : 'border-stone-200 bg-white hover:bg-stone-50 text-stone-600'
                }`}
              >
                <input
                  type="radio"
                  name="claimType"
                  value="RECLAMO"
                  checked={formData.claimType === 'RECLAMO'}
                  onChange={handleChange}
                  className="mt-1 text-rose-600 focus:ring-rose-500"
                />
                <div className="space-y-1">
                  <strong className="block text-sm font-bold text-rose-900">RECLAMO</strong>
                  <span className="text-xs text-stone-600 leading-relaxed block">
                    Disconformidad relacionada a los productos o servicios contratados (ej. calidad del alimento, retraso excesivo o cobro no reconocido).
                  </span>
                </div>
              </label>

              <label
                className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex items-start gap-3 ${
                  formData.claimType === 'QUEJA'
                    ? 'border-amber-500 bg-amber-50/40 text-[#1c1917]'
                    : 'border-stone-200 bg-white hover:bg-stone-50 text-stone-600'
                }`}
              >
                <input
                  type="radio"
                  name="claimType"
                  value="QUEJA"
                  checked={formData.claimType === 'QUEJA'}
                  onChange={handleChange}
                  className="mt-1 text-amber-600 focus:ring-amber-500"
                />
                <div className="space-y-1">
                  <strong className="block text-sm font-bold text-amber-900">QUEJA</strong>
                  <span className="text-xs text-stone-600 leading-relaxed block">
                    Malestar o descontento respecto a la atención al cliente brindada por el personal (trato descortés, demoras en contestar consultas).
                  </span>
                </div>
              </label>
            </div>
          </div>

          {/* Detalle de los Hechos */}
          <div id="field-claimDetail" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Detalle de los Hechos (¿Qué sucedió?) <span className="text-rose-500">*</span>
            </label>
            <textarea
              name="claimDetail"
              rows={4}
              value={formData.claimDetail}
              onChange={handleChange}
              placeholder="Describe de manera cronológica y precisa lo sucedido..."
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden resize-none ${
                errors.claimDetail
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141]'
              }`}
            />
            {errors.claimDetail && <p className="text-[11px] text-rose-600 font-medium">{errors.claimDetail}</p>}
          </div>

          {/* Pedido del Consumidor */}
          <div id="field-consumerRequest" className="space-y-1">
            <label className="text-xs font-bold text-stone-700">
              Pedido Concreto del Consumidor (¿Qué solución solicitas?) <span className="text-rose-500">*</span>
            </label>
            <textarea
              name="consumerRequest"
              rows={3}
              value={formData.consumerRequest}
              onChange={handleChange}
              placeholder="Indica con claridad qué solicitas: reposición del producto, reembolso económico, disculpas formales, etc."
              className={`w-full px-3.5 py-2.5 rounded-xl border text-xs sm:text-sm transition-all outline-hidden resize-none ${
                errors.consumerRequest
                  ? 'border-rose-300 bg-rose-50/50 focus:border-rose-500'
                  : 'border-stone-200 focus:border-[#005141]'
              }`}
            />
            {errors.consumerRequest && (
              <p className="text-[11px] text-rose-600 font-medium">{errors.consumerRequest}</p>
            )}
          </div>
        </div>
      </div>

      {/* BLOQUE 4: DECLARACIÓN JURADA Y BOTÓN DE ENVÍO */}
      <div className="p-5 sm:p-6 rounded-3xl bg-stone-100 border border-stone-200/90 space-y-4">
        <label id="field-acceptTerms" className="flex items-start gap-3 cursor-pointer select-none">
          <input
            type="checkbox"
            name="acceptTerms"
            checked={formData.acceptTerms}
            onChange={handleChange}
            className="w-5 h-5 rounded text-[#005141] border-stone-300 focus:ring-[#005141] mt-0.5"
          />
          <span className="text-xs sm:text-sm text-stone-700 leading-relaxed font-medium">
            Declaro bajo juramento que los datos consignados en la presente Hoja de Reclamación son verdaderos y concuerdan con la realidad, conforme a lo establecido en la Ley N° 29571 (Código de Protección y Defensa del Consumidor del Perú).
          </span>
        </label>
        {errors.acceptTerms && <p className="text-[11px] text-rose-600 font-bold pl-8">{errors.acceptTerms}</p>}

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4">
          <span className="text-[11px] text-stone-500 flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-[#005141]" />
            <span>Plazo legal máximo de respuesta: 15 días hábiles improrrogables</span>
          </span>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-[#005141] text-white text-xs sm:text-sm font-bold shadow-md shadow-[#005141]/20 hover:bg-[#00382d] transition-all cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <Send className="w-4 h-4" />
            <span>{isSubmitting ? 'Registrando en Libro...' : 'Enviar Hoja de Reclamación'}</span>
          </button>
        </div>
      </div>
    </form>
  );
}
