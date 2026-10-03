import { LegalDocument } from '../types/legal.types';
import { LEGAL_META } from './legalMeta';

export const TERMS_DOCUMENT: LegalDocument = {
  slug: 'terminos',
  title: 'Términos y Condiciones de Uso del Servicio',
  subtitle:
    'Condiciones contractuales aplicables al acceso, navegación y compras a través de la plataforma JaldiShop en la República del Perú.',
  badgeText: 'Conforme a Ley N° 29571 (Perú)',
  lastUpdated: LEGAL_META.lastUpdatedDate,
  effectiveDate: LEGAL_META.effectiveDate,
  version: LEGAL_META.version,
  jurisdiction: `${LEGAL_META.country} • Distrito Judicial de Lima`,
  clauses: [
    {
      id: 'naturaleza-del-servicio',
      title: '1. Naturaleza de la Plataforma e Intermediación Digital',
      shortTitle: '1. Rol de JaldiShop',
      badge: 'Intermediario SaaS',
      tldr: {
        title: 'JaldiShop es el motor tecnológico, no una cocina',
        summary:
          'JaldiShop brinda la infraestructura de software para conectar clientes con creadores gastronómicos locales. La elaboración de la comida y su calidad sanitaria son responsabilidad directa de cada tienda.',
        keyPoints: [
          'JaldiShop no cocina ni manipula físicamente los alimentos.',
          'Cada comercio gastronómico opera como una entidad comercial independiente.',
          'Proporcionamos el software de gestión de pedidos, catálogo digital y control de cupos.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Bienvenido a JaldiShop (en adelante, la "Plataforma"), servicio de tecnología y software provisto por JaldiShop Plataformas Digitales S.A.C., con domicilio en Lima, Perú. El presente documento regula los términos y condiciones de uso aplicables a cualquier usuario que navegue, cree un pedido o contrate servicios a través de nuestro sitio web o interfaces integradas.',
        },
        {
          type: 'paragraph',
          text: 'La Plataforma opera como un marketplace digital y herramienta de gestión para micro y pequeñas empresas que trabajan bajo pedido (gastronomía, repostería, floristerías, talleres artesanales y creadores locales). JaldiShop pone a disposición de los comercios herramientas claras para publicar sus productos, organizar sus horarios de entrega y atender sus pedidos sin saturarse.',
        },
        {
          type: 'callout',
          variant: 'shield',
          title: 'Responsabilidad Directa de Cada Comercio',
          text: 'JaldiShop brinda la tecnología para conectar a clientes y negocios. La elaboración, empaquetado, calidad de los productos y cumplimiento de las normativas sanitarias y comerciales vigentes es de directa y exclusiva responsabilidad de cada comercio afiliado.',
        },
      ],
    },
    {
      id: 'control-de-capacidad',
      title: '2. Horarios de Entrega y Límite de Pedidos (Sin Sobreventa)',
      shortTitle: '2. Horarios y Límites',
      badge: 'Sin Sobreventa',
      tldr: {
        title: 'Protección de calidad con pedidos limitados por hora',
        summary:
          'Cada negocio decide cuántos pedidos puede preparar bien en cada horario. Cuando se llega al límite, ese horario se cierra para evitar demoras y entregas a destiempo.',
        keyPoints: [
          'Eliges la hora en la que deseas recibir o recoger tu pedido.',
          'Si un horario ya está lleno, no se admiten más compras para esa hora.',
          'Tu pedido se prepara con dedicación para entregarse dentro del horario acordado.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'A diferencia de las aplicaciones masivas donde los negocios colapsan en horas pico, JaldiShop permite que cada negocio configure cuántos pedidos puede atender con dedicación en cada horario (como tandas de horneado, arreglos o pedidos preparados al momento). Cada comercio define autónomamente el volumen que su equipo puede preparar con calidad.',
        },
        {
          type: 'paragraph',
          text: 'Al completarse los cupos disponibles para una hora determinada, la plataforma cierra de manera automática la recepción de nuevos pedidos para ese horario. El usuario acepta que la disponibilidad está sujeta a la demanda y que el comercio puede ajustar sus horarios según su capacidad real de trabajo.',
        },
        {
          type: 'list',
          items: [
            'Los pedidos se solicitan para horarios activos y disponibles en la tienda.',
            'El comercio confirmará la entrega dentro del rango horario seleccionado en tu compra.',
            'Ante cualquier imprevisto de fuerza mayor en el negocio, este se comunicará de inmediato contigo para coordinar una reprogramación o la devolución de tu dinero.',
          ],
        },
      ],
    },
    {
      id: 'reserva-diez-minutos',
      title: '3. Tiempo de Reserva de 10 Minutos para Pagar',
      shortTitle: '3. 10 min para pagar',
      badge: 'Reserva Asegurada',
      tldr: {
        title: 'Tienes 10 minutos para pagar con total calma',
        summary:
          'Al avanzar al paso de pago, tu pedido y tu horario quedan apartados exclusivamente para ti durante 10 minutos para que nadie más tome tu lugar mientras transfieres o pagas.',
        keyPoints: [
          'Verás un reloj en pantalla con tus 10 minutos de reserva.',
          'Si el tiempo termina sin registrarse el pago, el pedido se libera para otros clientes.',
          'Asegura que todos tengan la oportunidad de comprar sin que se queden pedidos apartados sin pagar.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Para que todos los clientes tengan una oportunidad justa y nadie aparte pedidos sin concretar la compra, JaldiShop reserva tu pedido y tu horario durante diez (10) minutos mientras realizas tu pago por Yape, Plin o tarjeta.',
        },
        {
          type: 'callout',
          variant: 'clock',
          title: 'Tiempo para Completar tu Compra',
          text: 'Durante los 10 minutos posteriores a iniciar el pago, tus productos y tu turno de entrega quedan congelados exclusivamente para ti. Si transcurre ese plazo sin confirmar el pago, el turno se liberará de forma automática para que otros clientes puedan ordenar.',
        },
        {
          type: 'paragraph',
          text: 'Si realizas el pago después de los 10 minutos y el horario original ya fue tomado por otro cliente, el comercio coordinará contigo la entrega en el siguiente horario disponible o te devolverá el 100% de tu dinero abonado.',
        },
      ],
    },
    {
      id: 'alimentos-perecibles-retracto',
      title: '4. Bienes Perecibles y Excepción al Derecho de Retracto',
      shortTitle: '4. Perecibilidad y Cancelación',
      badge: 'Art. 59 Ley 29571',
      tldr: {
        title: 'Alimentos horneados bajo pedido no admiten devolución unilateral',
        summary:
          'Por ley peruana y seguridad sanitaria, los alimentos preparados bajo pedido no admiten devolución una vez que entraron a producción o cocina.',
        keyPoints: [
          'Aplica el Artículo 59 del Código de Consumo de Perú.',
          'Solo se aceptan cancelaciones si el comercio no ha iniciado la cocción/elaboración.',
          'En caso de producto en mal estado o erróneo, el cliente tiene derecho a sustitución o reembolso íntegro.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'De conformidad con el Artículo 59 de la Ley N° 29571 (Código de Protección y Defensa del Consumidor de la República del Perú), el derecho de restitución, revocación o desistimiento unilateral de la compra no resulta aplicable a bienes perecibles, alimentos preparados para consumo inmediato, productos personalizados o aquellos confeccionados bajo especificaciones expresas del consumidor.',
        },
        {
          type: 'callout',
          variant: 'warning',
          title: 'Condiciones de Cancelación y Reembolso',
          text: 'Una vez que el pedido pasa al estado "En Preparación" o "En Horno", el usuario no podrá revocar ni cancelar unilateralmente la orden, dado que los insumos y la capacidad del comercio han sido consumidos irreversiblemente.',
        },
        {
          type: 'table',
          headers: ['Estado del Pedido', '¿Admite Cancelación?', 'Procedimiento de Reembolso'],
          rows: [
            [
              'Pendiente de Pago (Ventana 10 min)',
              'Sí, en cualquier momento',
              'Automático e inmediato, no se efectúa cargo.',
            ],
            [
              'Confirmado (Antes de entrar a cocina)',
              'Sujeto a confirmación del comercio',
              'Reembolso coordinado con el comercio según sus tiempos.',
            ],
            [
              'En Preparación / Horneado',
              'No admite cancelación',
              'No aplica devolución salvo error atribuible al comercio.',
            ],
            [
              'En Camino / Despachado',
              'No admite cancelación',
              'Solo aplica reclamo por daño físico o entrega errónea.',
            ],
          ],
        },
        {
          type: 'paragraph',
          text: 'No obstante lo anterior, si el producto entregado presenta disconformidad manifiesta (producto vencido, ingredientes visiblemente deteriorados, empaque violentado o error imputable al comercio en el despacho), el cliente tiene derecho a solicitar la reposición inmediata del bien o el reembolso total del importe pagado, previa acreditación fotográfica razonable remitida dentro de las dos (2) horas siguientes a la recepción.',
        },
      ],
    },
    {
      id: 'seguridad-de-pagos',
      title: '5. Seguridad de Pagos y Pasarelas PCI-DSS',
      shortTitle: '5. Pagos Seguros',
      badge: 'PCI-DSS Nivel 1',
      tldr: {
        title: 'JaldiShop jamás guarda los datos de tus tarjetas',
        summary:
          'Todos los pagos con tarjeta o billeteras móviles se procesan a través de entidades reguladas y certificadas internacionalmente (Mercado Pago, Yape, Plin).',
        keyPoints: [
          'Cero almacenamiento de CVVs, números completos o PINs.',
          'Comunicaciones encriptadas mediante protocolo SSL/TLS de 256 bits.',
          'Los precios están expresados en Soles Peruanos (PEN / S/) con IGV incluido.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'La Plataforma facilita la recaudación y procesamiento de pagos a través de integraciones tecnológicas con operadores de pago autorizados (incluyendo Mercado Pago Perú, pasarelas bancarias y transferencias vía billeteras móviles como Yape y Plin).',
        },
        {
          type: 'paragraph',
          text: 'JaldiShop cumple con las mejores prácticas de la industria y estándares PCI-DSS: en ningún momento capturamos, registramos, almacenamos ni tenemos acceso a números completos de tarjetas de crédito o débito, códigos de seguridad (CVV) ni contraseñas bancarias de los usuarios.',
        },
        {
          type: 'list',
          items: [
            'Todos los precios visualizados en la Plataforma se indican en moneda de curso legal (Soles - S/) e incluyen los tributos aplicables de ley (IGV).',
            'El cargo por servicio de entrega (delivery) se desglosa explícitamente en el resumen de compra antes de confirmar el pago.',
            'En pagos por billetera móvil (Yape/Plin), la orden requiere la validación del comprobante mediante el WhatsApp Bridge oficial del comercio.',
          ],
        },
      ],
    },
    {
      id: 'despacho-y-entrega',
      title: '6. Modalidades de Entrega: Delivery y Retiro en Local (Pickup)',
      shortTitle: '6. Envíos y Retiros',
      badge: 'Cobertura Local',
      tldr: {
        title: 'Retiro en tienda o despacho coordinado por el comercio',
        summary:
          'Puedes recoger tu pedido en el local del artesano o recibirlo en tu dirección. Los tiempos son estimados y contemplan el horneado fresco.',
        keyPoints: [
          'En retiro en local (pickup), el cliente acude dentro de su franja horaria.',
          'En delivery, la cobertura y tarifas son establecidas por cada establecimiento.',
          'Si no hay nadie en el domicilio para recibir, el comercio esperará un máximo de 10 minutos.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'Los Comercios Afiliados pueden ofrecer dos modalidades de entrega: (a) Retiro en Local ("Pickup"), donde el cliente se apersona a la dirección física del establecimiento en la franja horaria acordada; y (b) Entrega a Domicilio ("Delivery"), gestionada directamente por el comercio o mediante sus repartidores aliados.',
        },
        {
          type: 'paragraph',
          text: 'El cliente es el único responsable de consignar una dirección de entrega verídica, completa y con referencias claras, así como de mantener encendido su teléfono de contacto al momento de la llegada del repartidor. Ante la imposibilidad de contactar al destinatario tras diez (10) minutos de espera en la dirección indicada, el repartidor continuará su ruta y el pedido retornará al local del comercio, sin derecho a reembolso por concepto de envío.',
        },
      ],
    },
    {
      id: 'libro-de-reclamaciones-jurisdiccion',
      title: '7. Libro de Reclamaciones, Resolución de Controversias y Ley Aplicable',
      shortTitle: '7. Reclamos y Ley',
      badge: 'D.S. 011-2011-PCM',
      tldr: {
        title: 'Respaldo legal en Perú y canal de reclamos oficial',
        summary:
          'Cuentas con nuestro Libro de Reclamaciones Virtual con respuesta obligatoria en máximo 15 días hábiles conforme a las normas de INDECOPI.',
        keyPoints: [
          'Acceso libre y permanente al Libro de Reclamaciones Virtual en la web.',
          'Plazo legal máximo de respuesta de 15 días hábiles.',
          'Sometimiento a la jurisdicción de los jueces y tribunales de Lima, Perú.',
        ],
      },
      content: [
        {
          type: 'paragraph',
          text: 'En cumplimiento estricto de la Ley N° 29571 y el D.S. 011-2011-PCM, JaldiShop pone a disposición de todos los usuarios su Libro de Reclamaciones Virtual accesible permanentemente en el pie de página de la Plataforma. Todas las quejas o reclamos debidamente registrados serán atendidos en un plazo no mayor a quince (15) días hábiles.',
        },
        {
          type: 'paragraph',
          text: 'Los presentes Términos y Condiciones se rigen e interpretan bajo las leyes de la República del Perú. Para cualquier controversia, discrepancia o litigio derivado de la interpretación, validez o ejecución del presente acuerdo, las partes se someten a la competencia de los jueces y tribunales del Distrito Judicial de Lima, renunciando a cualquier otro fuero que pudiera corresponderles.',
        },
        {
          type: 'callout',
          variant: 'info',
          title: 'Canal de Notificaciones Legales',
          text: `Para comunicaciones formales o consultas jurídicas, nuestro buzón oficial es: ${LEGAL_META.legalEmail}, con atención de lunes a viernes de 09:00 a 18:00 horas (GMT-5).`,
        },
      ],
    },
  ],
};
