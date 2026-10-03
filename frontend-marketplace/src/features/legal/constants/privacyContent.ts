import { LegalDocument } from '../types/legal.types';
import { LEGAL_META } from './legalMeta';

export const PRIVACY_DOCUMENT: LegalDocument = {
  slug: 'privacidad',
  title: 'Política de Privacidad y Tratamiento de Datos Personales',
  subtitle:
    'Garantizamos la confidencialidad, seguridad y estricta protección de tus datos personales conforme a la Ley N° 29733 de la República del Perú.',
  badgeText: 'Conforme a Ley N° 29733 y D.S. 003-2013-JUS',
  lastUpdated: LEGAL_META.lastUpdatedDate,
  effectiveDate: LEGAL_META.effectiveDate,
  version: LEGAL_META.version,
  jurisdiction: `${LEGAL_META.country} • Autoridad Nacional de Protección de Datos Personales (ANPDP)`,
  clauses: [
    {
      id: 'responsable-del-tratamiento',
      title: '1. Responsable del Tratamiento de tus Datos Personales',
      shortTitle: '1. Responsable Legal',
      badge: 'Identidad Legal',
      tldr: {
        title: 'Quién resguarda tu información',
        summary:
          'JaldiShop Plataformas Digitales S.A.C. es la empresa responsable de custodiar tus datos en Lima, Perú, bajo estándares de confidencialidad estrictos.',
        keyPoints: [
          'Entidad: JaldiShop Plataformas Digitales S.A.C.',
          'Buzón de privacidad exclusivo: privacidad@jaldishop.com',
          'Sujeto a la fiscalización de la Autoridad Nacional de Protección de Datos Personales (MINJUSDH).',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: `El presente documento constituye la Política de Privacidad de JaldiShop (en adelante, la "Plataforma"), operada por ${LEGAL_META.legalEntity}, identificada legalmente en Lima, Perú. Nos comprometemos de manera irrestricta a salvaguardar la privacidad de nuestros clientes, visitantes y comerciantes asociados.`,
        },
        {
          type: 'paragraph',
          text: 'Toda la información personal recabada es incorporada a los Bancos de Datos Personales de titularidad de JaldiShop, debidamente declarados y gestionados con arreglo a lo dispuesto en la Ley N° 29733 (Ley de Protección de Datos Personales de Perú) y su Reglamento aprobado por Decreto Supremo N° 003-2013-JUS.',
        },
      ],
    },
    {
      id: 'datos-recopilados',
      title: '2. Qué Información Recopilamos y Cómo la Obtenemos',
      shortTitle: '2. Datos Recopilados',
      badge: 'Minimización de Datos',
      tldr: {
        title: 'Solo pedimos los datos estrictamente necesarios para tu pedido',
        summary:
          'Recopilamos tu nombre, teléfono WhatsApp y dirección de entrega únicamente para coordinar tu compra y la entrega puntual con la tienda correspondiente.',
        keyPoints: [
          'No recopilamos datos sensibles (salud, origen, ideología).',
          'No almacenamos números de tarjetas de crédito o débito ni CVV.',
          'Las coordenadas GPS se usan únicamente para calcular el costo y rango de delivery.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Solo solicitamos los datos indispensables para coordinar y hacer llegar tu pedido a tiempo: tu nombre, tu teléfono de contacto y tu dirección de entrega.',
        },
        {
          type: 'table',
          headers: ['Categoría de Dato', 'Información Específica', 'Finalidad Principal'],
          rows: [
            [
              'Datos Identificativos',
              'Nombres y apellidos completos.',
              'Identificar al titular de la orden y rotular el empaque.',
            ],
            [
              'Datos de Contacto',
              'Número de teléfono celular (WhatsApp) y correo electrónico.',
              'Envío de la comanda en 1 clic, avisos de horneado y enlace de seguimiento.',
            ],
            [
              'Datos de Despacho',
              'Dirección física, referencias de llegada y geolocalización voluntaria.',
              'Calcular la distancia de entrega y facilitar la ruta al repartidor.',
            ],
            [
              'Datos Transaccionales',
              'ID de comanda, monto pagado, método elegido (Yape / Mercado Pago).',
              'Emisión de comprobante, liquidación al comercio y conciliación contable.',
            ],
          ],
        },
      ],
    },
    {
      id: 'finalidad-del-tratamiento',
      title: '3. Finalidades del Tratamiento de Datos',
      shortTitle: '3. Para qué los usamos',
      badge: 'Uso Transparente',
      tldr: {
        title: 'Tus datos son solo para tu comida, jamás para spam o reventa',
        summary:
          'Toda la información se utiliza exclusivamente para preparar y entregar tus alimentos, validar pagos y atender consultas. No vendemos tus datos a terceros.',
        keyPoints: [
          'Procesamiento y seguimiento en tiempo real de tu pedido.',
          'Comunicación directa con la cocina vía WhatsApp.',
          'JaldiShop prohíbe de forma tajante la reventa o cesión comercial de datos.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Tus datos personales son tratados con las siguientes finalidades legítimas y directas:',
        },
        {
          type: 'list',
          items: [
            'Procesar, preparar, agendar en la franja horaria correspondiente y despachar tus alimentos artesanales.',
            'Generar el ticket de comanda digital y transmitirlo a través de nuestro WhatsApp Order Bridge al comercio que seleccionaste.',
            'Mantenerte informado en tiempo real sobre el estado del pedido (Confirmado, En Horno/Preparación, En Camino, Entregado).',
            'Atender oportunamente cualquier consulta, sugerencia, queja o reclamo ingresado en nuestro Libro de Reclamaciones Virtual.',
            'Prevenir fraudes, sobreventas de inventario y accesos indebidos a la infraestructura de la Plataforma.',
          ],
        },
        {
          type: 'callout',
          variant: 'shield',
          title: 'Garantía Estricta contra la Venta de Datos',
          text: 'JaldiShop no vende, no alquila, no cede ni comercializa bajo ninguna modalidad tus datos personales a empresas de publicidad masiva, agencias de telemarketing ni terceras partes no vinculadas a la prestación del servicio.',
        },
      ],
    },
    {
      id: 'transferencia-a-terceros',
      title: '4. Transferencia y Comunicación de Datos a Terceros',
      shortTitle: '4. Terceros Involucrados',
      badge: 'Proveedores Seguros',
      tldr: {
        title: 'Quiénes acceden de forma indispensable a tus datos',
        summary:
          'Solo compartimos la información necesaria con el comercio que cocina tu orden, la pasarela de pagos (Mercado Pago) y los servicios de infraestructura en la nube.',
        keyPoints: [
          'El comercio gastronómico recibe tu nombre, teléfono y dirección de entrega.',
          'Mercado Pago procesa el pago bajo certificación PCI-DSS.',
          'La infraestructura opera en servidores cifrados de alta disponibilidad.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Para la correcta ejecución del servicio, JaldiShop comunica determinados datos a terceros en calidad de Encargados del Tratamiento:',
        },
        {
          type: 'list',
          items: [
            'Comercio Gastronómico Afiliado: Se le proporciona tu nombre, teléfono y dirección para que prepare el producto con los estándares acordados y coordine el despacho.',
            'Pasarelas de Pago Autorizadas (Mercado Pago Perú): Encargadas del procesamiento transaccional financiero bajo estrictos estándares bancarios.',
            'Proveedores de Infraestructura Tecnológica y Nube: Servidores de hosting, bases de datos y CDN (ej. AWS, Vercel, Cloudinary) que operan bajo protocolos de seguridad de clase mundial y cifrado en tránsito y en reposo.',
          ],
        },
      ],
    },
    {
      id: 'derechos-arco',
      title: '5. Ejercicio de Derechos ARCO (Acceso, Rectificación, Cancelación y Oposición)',
      shortTitle: '5. Derechos ARCO',
      badge: 'Tus Derechos',
      tldr: {
        title: 'Tú tienes el control total sobre tus datos',
        summary:
          'Puedes pedir en cualquier momento ver qué datos tenemos de ti, actualizarlos o solicitar su eliminación definitiva escribiéndonos a privacidad@jaldishop.com.',
        keyPoints: [
          'Derechos: Acceso, Rectificación, Cancelación y Oposición.',
          'Procedimiento 100% gratuito sin intermediarios.',
          'Plazo de respuesta: 10 días hábiles (Ley N° 29733).',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'De acuerdo con la Ley N° 29733, eres titular de los derechos ARCO respecto de tus datos personales almacenados en nuestros sistemas:',
        },
        {
          type: 'table',
          headers: ['Derecho ARCO', '¿En qué consiste?', 'Plazo Legal de Respuesta'],
          rows: [
            [
              'Acceso',
              'Conocer qué información personal tenemos registrada sobre ti.',
              '20 días hábiles',
            ],
            [
              'Rectificación',
              'Actualizar datos inexactos, desactualizados o erróneos.',
              '10 días hábiles',
            ],
            [
              'Cancelación',
              'Solicitar la eliminación o supresión definitiva de tus datos.',
              '10 días hábiles',
            ],
            [
              'Oposición',
              'Oponerte al tratamiento de tus datos para finalidades específicas.',
              '10 días hábiles',
            ],
          ],
        },
        {
          type: 'callout',
          variant: 'info',
          title: 'Cómo solicitar el ejercicio de tus derechos ARCO',
          text: `Envía un correo electrónico a ${LEGAL_META.privacyEmail} con el asunto "Derechos ARCO - [Tu Nombre]", adjuntando copia legible de tu DNI/CE para validar tu identidad y la descripción clara de tu solicitud. No tiene ningún costo.`,
        },
      ],
    },
    {
      id: 'cookies-y-almacenamiento-local',
      title: '6. Política de Cookies y Almacenamiento Local (Local Storage)',
      shortTitle: '6. Cookies y Almacenamiento',
      badge: 'Transparencia Técnica',
      tldr: {
        title: 'Almacenamiento técnico mínimo para que funcione tu carrito',
        summary:
          'Usamos LocalStorage en tu navegador para recordar los productos de tu carrito y el temporizador de 10 minutos. No usamos rastreadores intrusivos.',
        keyPoints: [
          'No utilizamos cookies de espionaje o rastreo publicitario entre sitios.',
          'LocalStorage guarda tu pedido en curso para que no lo pierdas si recargas.',
          'Puedes borrarlo en cualquier momento desde la configuración de tu navegador.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'JaldiShop emplea tecnologías de almacenamiento local en el navegador del cliente ("localStorage") y cookies estrictamente técnicas destinadas exclusivamente a posibilitar la experiencia de usuario:',
        },
        {
          type: 'list',
          items: [
            'Preservar los productos seleccionados en el carrito de compras mientras navegas entre tiendas.',
            'Mantener activo el temporizador del ticket de reserva de 10 minutos.',
            'Recordar tus preferencias de visualización (ej. filtros de categoría o modo de comparación).',
          ],
        },
        {
          type: 'paragraph',
          text: 'No implementamos scripts de perfilamiento publicitario masivo ni vendemos el historial de navegación a redes publicitarias externas.',
        },
      ],
    },
    {
      id: 'seguridad-y-contacto',
      title: '7. Medidas de Seguridad y Contacto Oficial',
      shortTitle: '7. Seguridad y Contacto',
      badge: 'Encriptación 256 bits',
      tldr: {
        title: 'Protección con candado verde en todas tus compras',
        summary:
          'Todas las comunicaciones entre tu teléfono/PC y JaldiShop viajan cifradas con SSL/TLS de 256 bits para impedir interceptaciones.',
        keyPoints: [
          'Canales cifrados extremo a extremo con HTTPS.',
          'Políticas de acceso restringido solo para personal autorizado.',
          'Dudas de privacidad: privacidad@jaldishop.com.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: `En cumplimiento de la Directiva de Seguridad de la Ley N° 29733, JaldiShop implementa medidas de índole técnica, organizativa y legal orientadas a resguardar los datos frente a accesos no autorizados, pérdidas o alteraciones. Toda la transferencia de datos en la web se efectúa mediante canales seguros cifrados con protocolo TLS/HTTPS de 256 bits.`,
        },
        {
          type: 'paragraph',
          text: `Para cualquier duda, aclaración o consulta sobre esta política, puedes dirigirte a nuestro Oficial de Privacidad a través de ${LEGAL_META.privacyEmail}.`,
        },
      ],
    },
  ],
};
