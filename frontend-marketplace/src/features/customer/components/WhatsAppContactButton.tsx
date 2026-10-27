import React from 'react';
import { MessageCircle } from 'lucide-react';

interface WhatsAppContactButtonProps {
  phone?: string | null;
  orderNumber: string;
  storeName: string;
  className?: string;
  variant?: 'primary' | 'secondary' | 'compact';
}

export const WhatsAppContactButton: React.FC<WhatsAppContactButtonProps> = ({
  phone,
  orderNumber,
  storeName,
  className = '',
  variant = 'secondary',
}) => {
  if (!phone || !phone.trim()) {
    return null;
  }

  // Sanitize phone number (strip spaces, dashes, + if already present, ensuring clean digits)
  const cleanDigits = phone.replace(/\D/g, '');
  // If digits are 9 length (Peru mobile standard), prepend country code 51
  const fullPhone = cleanDigits.length === 9 ? `51${cleanDigits}` : cleanDigits;

  const defaultMessage = `Hola ${storeName}, te escribo por mi pedido ${orderNumber} en JaldiShop.`;
  const whatsappUrl = `https://wa.me/${fullPhone}?text=${encodeURIComponent(defaultMessage)}`;

  if (variant === 'compact') {
    return (
      <a
        href={whatsappUrl}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Escribir a ${storeName} por WhatsApp sobre el pedido ${orderNumber}`}
        className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${className}`}
      >
        <MessageCircle className="w-3.5 h-3.5 text-emerald-700" />
        <span>WhatsApp</span>
      </a>
    );
  }

  if (variant === 'primary') {
    return (
      <a
        href={whatsappUrl}
        target="_blank"
        rel="noopener noreferrer"
        className={`inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium text-white bg-emerald-700 hover:bg-emerald-800 rounded-xl transition-all shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${className}`}
      >
        <MessageCircle className="w-4 h-4" />
        <span>Contactar por WhatsApp</span>
      </a>
    );
  }

  return (
    <a
      href={whatsappUrl}
      target="_blank"
      rel="noopener noreferrer"
      className={`inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-stone-700 bg-white hover:bg-stone-50 border border-stone-300 rounded-xl transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${className}`}
    >
      <MessageCircle className="w-4 h-4 text-emerald-600" />
      <span>Consultar a la tienda</span>
    </a>
  );
};
