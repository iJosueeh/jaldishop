import { ClaimFormData } from '../../types/legal.types';

export interface ClaimValidationErrors {
  fullName?: string;
  documentType?: string;
  documentNumber?: string;
  email?: string;
  phone?: string;
  address?: string;
  department?: string;
  province?: string;
  district?: string;
  parentName?: string;
  goodType?: string;
  claimedAmount?: string;
  goodDescription?: string;
  claimType?: string;
  claimDetail?: string;
  consumerRequest?: string;
  acceptTerms?: string;
}

export function validateClaimForm(data: Partial<ClaimFormData>): {
  isValid: boolean;
  errors: ClaimValidationErrors;
} {
  const errors: ClaimValidationErrors = {};

  // 1. Identificación del Consumidor
  if (!data.fullName || data.fullName.trim().length < 3) {
    errors.fullName = 'Ingresa tu nombre y apellido completo (mínimo 3 caracteres).';
  }

  if (!data.documentType) {
    errors.documentType = 'Selecciona un tipo de documento.';
  }

  const docNumber = data.documentNumber?.trim() || '';
  if (!docNumber) {
    errors.documentNumber = 'El número de documento es obligatorio.';
  } else if (data.documentType === 'DNI' && !/^\d{8}$/.test(docNumber)) {
    errors.documentNumber = 'El DNI debe contener exactamente 8 dígitos numéricos.';
  } else if (data.documentType === 'RUC' && !/^\d{11}$/.test(docNumber)) {
    errors.documentNumber = 'El RUC debe contener exactamente 11 dígitos numéricos.';
  } else if (data.documentType === 'CE' && docNumber.length < 8) {
    errors.documentNumber = 'El Carné de Extranjería debe tener al menos 8 caracteres.';
  } else if (data.documentType === 'PASAPORTE' && docNumber.length < 6) {
    errors.documentNumber = 'El Pasaporte debe tener al menos 6 caracteres.';
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!data.email || !emailRegex.test(data.email.trim())) {
    errors.email = 'Ingresa un correo electrónico válido para enviar tu constancia oficial.';
  }

  if (!data.phone || !/^\d{9}$/.test(data.phone.replace(/\s+/g, ''))) {
    errors.phone = 'Ingresa un número telefónico de 9 dígitos para coordinar.';
  }

  if (!data.address || data.address.trim().length < 5) {
    errors.address = 'Ingresa tu dirección domiciliaria completa con calle y número.';
  }

  if (!data.department || data.department.trim() === '') {
    errors.department = 'Selecciona el departamento.';
  }
  if (!data.province || data.province.trim() === '') {
    errors.province = 'Ingresa la provincia.';
  }
  if (!data.district || data.district.trim() === '') {
    errors.district = 'Ingresa el distrito.';
  }

  if (data.isMinor && (!data.parentName || data.parentName.trim().length < 3)) {
    errors.parentName = 'Como menor de edad, debes indicar el nombre del padre, madre o apoderado.';
  }

  // 2. Identificación del Bien Contratado
  if (!data.goodType) {
    errors.goodType = 'Especifica si la disconformidad es sobre un Producto o un Servicio.';
  }

  if (data.claimedAmount === undefined || isNaN(Number(data.claimedAmount)) || Number(data.claimedAmount) < 0) {
    errors.claimedAmount = 'Indica el monto reclamado en Soles (puede ser 0 si no involucra cobro).';
  }

  if (!data.goodDescription || data.goodDescription.trim().length < 5) {
    errors.goodDescription = 'Describe brevemente el producto o servicio contratado (mínimo 5 caracteres).';
  }

  // 3. Detalle de la Reclamación
  if (!data.claimType) {
    errors.claimType = 'Selecciona si corresponde a un Reclamo o una Queja.';
  }

  if (!data.claimDetail || data.claimDetail.trim().length < 15) {
    errors.claimDetail = 'Explica detalladamente los hechos ocurridos (mínimo 15 caracteres).';
  }

  if (!data.consumerRequest || data.consumerRequest.trim().length < 10) {
    errors.consumerRequest = 'Detalla qué solución concreta solicitas (mínimo 10 caracteres).';
  }

  // 4. Aceptación legal
  if (!data.acceptTerms) {
    errors.acceptTerms = 'Debes declarar que los datos consignados son verdaderos conforme a ley.';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}
