import {
  centerText,
  divider,
  generateAsciiTicket,
  generateBatchAsciiTickets,
  mapOrderToThermalData,
  padBetween,
  ThermalTicketOrderData,
  wrapText,
} from './thermal-ticket.utils';

describe('thermal-ticket.utils', () => {
  const sampleOrder: ThermalTicketOrderData = {
    orderNumber: '#ORD-00000042',
    customerName: 'Lucía Méndez',
    customerPhone: '987654321',
    deliveryMode: 'DELIVERY',
    deliveryAddress: 'Av. Brasil 123',
    deliveryReference: 'Frente a Metro',
    channel: 'WHATSAPP',
    channelLabel: 'WhatsApp',
    paymentMethod: 'YAPE',
    serviceDate: '2026-11-20',
    scheduledTime: '13:30',
    notes: 'Enviar con velita dorada y dedicatoria',
    productsSubtotal: 192.0,
    deliveryFee: 5.0,
    totalAmount: 197.0,
    items: [
      {
        name: 'Torta de Chocolate',
        variant: '12 porciones',
        quantity: 2,
        unitPrice: 78.0,
        totalPrice: 156.0,
      },
      {
        name: 'Caja Brownies Melcochosos',
        quantity: 1,
        unitPrice: 36.0,
        totalPrice: 36.0,
      },
    ],
  };

  it('debe centrar texto correctamente dentro del ancho dado', () => {
    const centered = centerText('HOLA', 10);
    expect(centered.length).toBe(10);
    expect(centered.trim()).toBe('HOLA');
    expect(centered).toBe('   HOLA   ');
  });

  it('debe generar divisor con el caracter y ancho especificado', () => {
    expect(divider('=', 10)).toBe('==========');
    expect(divider('-', 5)).toBe('-----');
  });

  it('debe alinear texto a los extremos con padBetween', () => {
    const res = padBetween('Izq', 'Der', 15);
    expect(res.length).toBe(15);
    expect(res.startsWith('Izq')).toBe(true);
    expect(res.endsWith('Der')).toBe(true);
  });

  it('debe dividir textos largos en líneas sin cortar palabras con wrapText', () => {
    const text = 'Esta es una dirección muy larga para el reparto en moto';
    const lines = wrapText(text, 20);
    expect(lines.length).toBeGreaterThan(1);
    expect(lines.every((l) => l.length <= 20)).toBe(true);
  });

  it('debe generar comanda ASCII completa para 80mm (42 columnas)', () => {
    const ticket = generateAsciiTicket(sampleOrder, 'Pastelería Delicia', 42);

    expect(ticket).toContain('PASTELERÍA DELICIA');
    expect(ticket).toContain('COMANDA OPERATIVA #ORD-00000042');
    expect(ticket).toContain('Lucía Méndez');
    expect(ticket).toContain('987654321');
    expect(ticket).toContain('Av. Brasil 123');
    expect(ticket).toContain('Torta de Chocolate');
    expect(ticket).toContain('(12 porciones)');
    expect(ticket).toContain('2x');
    expect(ticket).toContain('156.00');
    expect(ticket).toContain('Subtotal productos:');
    expect(ticket).toContain('Costo delivery:');
    expect(ticket).toContain('TOTAL A COBRAR:');
    expect(ticket).toContain('S/ 197.00');
    expect(ticket).toContain('NOTAS / INDICACIONES:');
    expect(ticket).toContain('Enviar con velita dorada y dedicatoria');
    expect(ticket).toContain('*** FIN DE COMANDA ***');
  });

  it('debe generar comanda ASCII reducida para 58mm (32 columnas)', () => {
    const ticket = generateAsciiTicket(sampleOrder, 'Pastelería Delicia', 32);

    expect(ticket).toContain('PASTELERÍA DELICIA');
    expect(ticket).toContain('#ORD-00000042');
    expect(ticket).toContain('S/ 197.00');
  });

  it('debe generar batch de comandas con separador de corte', () => {
    const batch = generateBatchAsciiTickets([sampleOrder, sampleOrder], 'Tienda', 42);
    expect(batch).toContain('CORTE DE COMANDA');
    expect(batch.split('*** FIN DE COMANDA ***').length).toBe(3);
  });

  it('debe mapear objetos heterogéneos de orden con mapOrderToThermalData', () => {
    const rawOrder = {
      orderNumber: '#ORD-111',
      customerName: 'Carlos Gómez',
      deliveryMode: 'PICKUP',
      totalAmount: 50.0,
      items: [
        {
          productName: 'Alfajor',
          quantity: 5,
          unitPriceAmount: 10.0,
          subtotalAmount: 50.0,
        },
      ],
    };

    const mapped = mapOrderToThermalData(rawOrder);
    expect(mapped.orderNumber).toBe('#ORD-111');
    expect(mapped.customerName).toBe('Carlos Gómez');
    expect(mapped.items[0].name).toBe('Alfajor');
    expect(mapped.items[0].quantity).toBe(5);
    expect(mapped.items[0].totalPrice).toBe(50.0);
  });

  it('debe incluir datos SUNAT de Boleta Nuevo RUS (IGV 0%) y Factura MYPE (IGV 18%)', () => {
    const rusTicket = generateAsciiTicket(
      {
        ...sampleOrder,
        emitterLegalName: 'Bodega Luz',
        emitterTaxIdType: 'RUC_10',
        emitterTaxIdNumber: '10456789019',
        emitterTaxRegime: 'RUS',
        receiptTypeCode: '03',
        receiptTypeName: 'Boleta de Venta',
        receiptFullNumber: 'B001-00000012',
        igvRate: 0,
        taxableAmount: 197,
        igvAmount: 0,
      },
      'Bodega Luz',
      42,
    );

    expect(rusTicket).toContain('RUC: 10456789019');
    expect(rusTicket).toContain('BOLETA DE VENTA');
    expect(rusTicket).toContain('B001-00000012');
    expect(rusTicket).toContain('IGV (0%):');

    const facturaTicket = generateAsciiTicket(
      {
        ...sampleOrder,
        emitterLegalName: 'Gourmet SAC',
        emitterTaxIdType: 'RUC_20',
        emitterTaxIdNumber: '20100070970',
        emitterTaxRegime: 'MYPE',
        receiptTypeCode: '01',
        receiptTypeName: 'Factura Electrónica',
        receiptFullNumber: 'F001-00000005',
        customerDocType: 'RUC',
        customerDocNumber: '20131312955',
        igvRate: 18,
        taxableAmount: 166.95,
        igvAmount: 30.05,
      },
      'Gourmet SAC',
      42,
    );

    expect(facturaTicket).toContain('RUC: 20100070970');
    expect(facturaTicket).toContain('F001-00000005');
    expect(facturaTicket).toContain('RUC: 20131312955');
    expect(facturaTicket).toContain('OP. GRAVADA:');
    expect(facturaTicket).toContain('IGV (18%):');
  });
});
