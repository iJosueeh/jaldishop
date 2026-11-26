export interface ThermalTicketItemData {
  name: string;
  variant?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface ThermalTicketOrderData {
  orderNumber: string;
  customerName: string;
  customerPhone?: string;
  deliveryMode: string;
  deliveryAddress?: string;
  deliveryReference?: string;
  channel?: string;
  channelLabel?: string;
  paymentMethod?: string;
  serviceDate?: string;
  serviceStartTime?: string;
  scheduledTime?: string;
  notes?: string;
  productsSubtotal?: number;
  deliveryFee?: number;
  totalAmount: number;
  items: ThermalTicketItemData[];
  // Datos fiscales SUNAT opcionales
  emitterLegalName?: string;
  emitterTaxIdType?: string;
  emitterTaxIdNumber?: string;
  emitterTaxRegime?: string;
  receiptTypeCode?: string;
  receiptTypeName?: string;
  receiptFullNumber?: string;
  customerDocType?: string;
  customerDocNumber?: string;
  taxableAmount?: number;
  igvRate?: number;
  igvAmount?: number;
}

/**
 * Centra un texto dentro de un ancho fijo rellenando con espacios.
 */
export function centerText(text: string, width: number): string {
  const clean = text.trim();
  if (clean.length >= width) return clean.slice(0, width);
  const leftPad = Math.floor((width - clean.length) / 2);
  const rightPad = width - clean.length - leftPad;
  return ' '.repeat(leftPad) + clean + ' '.repeat(rightPad);
}

/**
 * Genera una línea divisoria continua con el carácter especificado.
 */
export function divider(char: string, width: number): string {
  return char.repeat(width);
}

/**
 * Alinea texto a la izquierda y derecha rellenando el espacio intermedio.
 */
export function padBetween(left: string, right: string, width: number): string {
  const spaceNeeded = width - left.length - right.length;
  if (spaceNeeded <= 0) {
    const truncatedLeft = left.slice(0, Math.max(1, width - right.length - 1));
    return truncatedLeft + ' ' + right;
  }
  return left + ' '.repeat(spaceNeeded) + right;
}

/**
 * Divide un texto en múltiples líneas sin cortar palabras dentro del ancho máximo.
 */
export function wrapText(text: string, maxWidth: number): string[] {
  if (!text) return [];
  const words = text.split(/\s+/);
  const lines: string[] = [];
  let currentLine = '';

  for (const w of words) {
    if (!currentLine) {
      currentLine = w;
    } else if (currentLine.length + 1 + w.length <= maxWidth) {
      currentLine += ' ' + w;
    } else {
      lines.push(currentLine);
      currentLine = w;
    }
  }
  if (currentLine) {
    lines.push(currentLine);
  }
  return lines;
}

/**
 * Genera el texto ASCII formateado de una comanda térmica monospaciada.
 * Ancho estándar 80mm: 42 columnas | 58mm: 32 columnas.
 */
export function generateAsciiTicket(
  order: ThermalTicketOrderData,
  storeName = 'MI COMERCIO',
  widthCols = 42,
): string {
  const w = widthCols;
  const lines: string[] = [];

  // Encabezado
  lines.push(divider('=', w));
  const headerTitle = (order.emitterLegalName || storeName).toUpperCase();
  lines.push(centerText(headerTitle, w));
  if (order.emitterTaxIdNumber) {
    const docLabel = order.emitterTaxIdType === 'DNI' ? 'DNI' : 'RUC';
    const regimeTag = order.emitterTaxRegime ? ` · ${order.emitterTaxRegime}` : '';
    lines.push(centerText(`${docLabel}: ${order.emitterTaxIdNumber}${regimeTag}`, w));
  }
  if (order.receiptFullNumber) {
    const docTitle = (order.receiptTypeName || 'COMPROBANTE DE PAGO').toUpperCase();
    lines.push(centerText(`${docTitle}`, w));
    lines.push(centerText(`${order.receiptFullNumber} (Ref ${order.orderNumber})`, w));
  } else {
    lines.push(centerText(`COMANDA OPERATIVA ${order.orderNumber}`, w));
  }
  lines.push(divider('=', w));

  // Metadatos operativos
  const dateStr = order.serviceDate || new Date().toISOString().slice(0, 10);
  const timeStr = order.scheduledTime || order.serviceStartTime || '';
  const dateTimeStr = timeStr ? `${dateStr} ${timeStr}` : dateStr;
  const channelStr = order.channelLabel || order.channel || 'DIRECTO';

  const paymentStr = order.paymentMethod === 'MERCADO_PAGO'
    ? 'MERCADO PAGO'
    : (order.paymentMethod || 'EFECTIVO');

  lines.push(padBetween(`Fecha: ${dateTimeStr}`, `Canal: ${channelStr}`, w));
  lines.push(padBetween(`Entrega: ${order.deliveryMode}`, `Pago: ${paymentStr}`, w));
  lines.push(`Cliente: ${order.customerName}`);
  if (order.customerDocNumber) {
    lines.push(`${order.customerDocType || 'Doc'}: ${order.customerDocNumber}`);
  }
  if (order.customerPhone) {
    lines.push(`Teléfono: ${order.customerPhone}`);
  }
  if (order.deliveryAddress) {
    const addressLines = wrapText(`Dir: ${order.deliveryAddress}`, w);
    lines.push(...addressLines);
  }
  if (order.deliveryReference) {
    const refLines = wrapText(`Ref: ${order.deliveryReference}`, w);
    lines.push(...refLines);
  }

  // Detalle de ítems
  lines.push(divider('-', w));
  // Encabezado columnas: CANT (5) + TOTAL (8) + DESCRIPCION (resto)
  const qtyColWidth = 5;
  const priceColWidth = 8;
  const descColWidth = w - qtyColWidth - priceColWidth;

  const headerLeft = 'CANT '.padEnd(qtyColWidth) + 'DESCRIPCIÓN'.padEnd(descColWidth);
  lines.push(headerLeft + 'TOTAL'.padStart(priceColWidth));
  lines.push(divider('-', w));

  for (const item of order.items) {
    const qtyStr = `${item.quantity}x`.padEnd(qtyColWidth);
    const totalStr = item.totalPrice.toFixed(2).padStart(priceColWidth);

    const descLines = wrapText(item.name, descColWidth - 1);
    const firstLineDesc = (descLines[0] || '').padEnd(descColWidth);
    lines.push(qtyStr + firstLineDesc + totalStr);

    for (let i = 1; i < descLines.length; i++) {
      lines.push(' '.repeat(qtyColWidth) + descLines[i]);
    }

    if (item.variant && item.variant.trim()) {
      const varLines = wrapText(`(${item.variant.trim()})`, descColWidth - 2);
      for (const vl of varLines) {
        lines.push(' '.repeat(qtyColWidth + 1) + vl);
      }
    }
  }

  // Totales
  lines.push(divider('-', w));
  if (order.productsSubtotal != null && order.productsSubtotal > 0) {
    lines.push(padBetween('Subtotal productos:', `S/ ${order.productsSubtotal.toFixed(2)}`, w));
  }
  if (order.deliveryFee != null && order.deliveryFee > 0) {
    lines.push(padBetween('Costo delivery:', `S/ ${order.deliveryFee.toFixed(2)}`, w));
  }
  if (order.igvRate != null) {
    if (order.igvRate === 0) {
      const base = order.taxableAmount ?? order.totalAmount;
      lines.push(padBetween('OP. NRUS / EXON.:', `S/ ${base.toFixed(2)}`, w));
      lines.push(padBetween('IGV (0%):', 'S/ 0.00', w));
    } else {
      const base = order.taxableAmount ?? order.totalAmount / (1 + order.igvRate / 100);
      const igv = order.igvAmount ?? order.totalAmount - base;
      lines.push(padBetween('OP. GRAVADA:', `S/ ${base.toFixed(2)}`, w));
      lines.push(padBetween(`IGV (${order.igvRate}%):`, `S/ ${igv.toFixed(2)}`, w));
    }
  }
  lines.push(padBetween('TOTAL A COBRAR:', `S/ ${order.totalAmount.toFixed(2)}`, w));

  // Notas de preparación o pedido
  if (order.notes && order.notes.trim()) {
    lines.push(divider('-', w));
    lines.push('NOTAS / INDICACIONES:');
    const noteLines = wrapText(order.notes.trim(), w);
    lines.push(...noteLines);
  }

  // Pie de comanda
  lines.push(divider('=', w));
  lines.push(centerText('*** FIN DE COMANDA ***', w));
  lines.push('\n\n'); // Espacio de avance para corte de papel

  return lines.join('\n');
}

/**
 * Concatena múltiples órdenes en un único documento continuo listo para corte.
 */
export function generateBatchAsciiTickets(
  orders: ThermalTicketOrderData[],
  storeName = 'MI COMERCIO',
  widthCols = 42,
): string {
  if (!orders || orders.length === 0) return '';
  return orders
    .map((o) => generateAsciiTicket(o, storeName, widthCols))
    .join(`\n${divider('- - - CORTE DE COMANDA - - -', widthCols)}\n\n`);
}

/**
 * Dispara la impresión térmica de texto ASCII mediante un iframe invisible con tamaño de página milimétrico.
 */
export function printThermalText(asciiText: string, widthMm: 80 | 58 = 80): void {
  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.top = '-9999px';
  iframe.style.left = '-9999px';
  iframe.style.width = `${widthMm}mm`;
  iframe.style.height = '100px';
  iframe.style.border = 'none';

  document.body.appendChild(iframe);

  const doc = iframe.contentWindow?.document;
  if (!doc) {
    document.body.removeChild(iframe);
    window.print();
    return;
  }

  const escapedText = asciiText
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  doc.open();
  doc.write(`
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8">
        <title>Impresión de Comanda Térmica</title>
        <style>
          @page {
            size: ${widthMm}mm auto;
            margin: 0;
          }
          @media print {
            html, body {
              margin: 0;
              padding: 0;
            }
          }
          body {
            margin: 0;
            padding: 3mm 2mm;
            font-family: 'Courier New', Courier, monospace, monospace;
            font-size: ${widthMm === 58 ? '10px' : '12px'};
            line-height: 1.25;
            white-space: pre;
            color: #000;
            background: #fff;
          }
        </style>
      </head>
      <body>${escapedText}</body>
    </html>
  `);
  doc.close();

  iframe.contentWindow?.focus();
  setTimeout(() => {
    try {
      iframe.contentWindow?.print();
    } finally {
      setTimeout(() => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      }, 1000);
    }
  }, 250);
}

/**
 * Mapea cualquier modelo de pedido del frontend a la estructura canónica del generador de tickets.
 */
export function mapOrderToThermalData(order: any): ThermalTicketOrderData {
  const items: ThermalTicketItemData[] = (order.items || []).map((i: any) => {
    const qty = Number(i.quantity) || 1;
    const unit = Number(i.unitPrice || i.unitPriceAmount || 0);
    const tot = Number(i.totalPrice || i.subtotalAmount || qty * unit);
    return {
      name: i.name || i.productName || 'Producto',
      variant: i.variant || i.variantName || undefined,
      quantity: qty,
      unitPrice: unit,
      totalPrice: tot,
    };
  });

  return {
    orderNumber: order.orderNumber || '#ORD-0000',
    customerName: order.customerName || 'Cliente mostrador',
    customerPhone: order.customerPhone || undefined,
    deliveryMode: order.deliveryMode || 'DIRECT_SALE',
    deliveryAddress: order.deliveryAddress || undefined,
    deliveryReference: order.deliveryReference || undefined,
    channel: order.channel || undefined,
    channelLabel: order.channelLabel || undefined,
    paymentMethod: order.paymentMethod || undefined,
    serviceDate: order.serviceDate || undefined,
    serviceStartTime: order.serviceStartTime || undefined,
    scheduledTime: order.scheduledTime || undefined,
    notes: order.notes || undefined,
    productsSubtotal: Number(order.productsSubtotalAmount ?? order.productsSubtotal) || undefined,
    deliveryFee: Number(order.deliveryFeeAmount ?? order.deliveryFee) || undefined,
    totalAmount: Number(order.totalAmount) || items.reduce((sum, it) => sum + it.totalPrice, 0),
    items,
  };
}
