export function formatCurrency(amount: number | undefined | null, currency: string = 'PEN'): string {
  if (amount === undefined || amount === null || isNaN(amount)) {
    return 'S/ 0.00';
  }
  return new Intl.NumberFormat('es-PE', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
  }).format(amount);
}

export function formatDate(dateString: string | Date): string {
  const date = typeof dateString === 'string' ? new Date(dateString) : dateString;
  return new Intl.DateTimeFormat('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
  }).format(date);
}

export function formatTime(timeString?: string): string {
  if (!timeString) return '';
  return timeString.substring(0, 5);
}

export function formatPhoneNumber(phone?: string): string {
  if (!phone) return '';
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 9) {
    return `${cleaned.slice(0, 3)} ${cleaned.slice(3, 6)} ${cleaned.slice(6)}`;
  }
  return phone;
}

export function getWhatsAppShareUrl(phoneNumber: string, message: string): string {
  const cleaned = phoneNumber.replace(/\D/g, '');
  const prefix = cleaned.startsWith('51') ? cleaned : `51${cleaned}`;
  return `https://wa.me/${prefix}?text=${encodeURIComponent(message)}`;
}
