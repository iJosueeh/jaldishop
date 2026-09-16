import { TestBed } from '@angular/core/testing';
import { TokenService } from './token-service';

describe('TokenService', () => {
  let service: TokenService;
  const TOKEN_KEY = 'jaldi_merchant_token';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TokenService],
    });
    service = TestBed.inject(TokenService);
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe guardar y obtener el token de sessionStorage', () => {
    const fakeToken = 'header.payload.signature';
    service.setToken(fakeToken);

    expect(sessionStorage.getItem(TOKEN_KEY)).toBe(fakeToken);
    expect(service.getToken()).toBe(fakeToken);
  });

  it('debe remover el token de sessionStorage', () => {
    service.setToken('test-token');
    service.removeToken();

    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(service.getToken()).toBeNull();
  });

  it('debe decodificar el payload y extraer roles y userId de un JWT válido', () => {
    const payloadObj = {
      sub: 'user-uuid-123',
      email: 'comerciante@jaldishop.com',
      roles: ['MERCHANT', 'CUSTOMER'],
    };
    const base64Payload = btoa(JSON.stringify(payloadObj));
    const fakeJwt = `eyJhbGciOiJIUzI1NiJ9.${base64Payload}.dummySignature`;

    service.setToken(fakeJwt);

    expect(service.getPayload()).toEqual(payloadObj);
    expect(service.getRoles()).toEqual(['MERCHANT', 'CUSTOMER']);
    expect(service.getUserId()).toBe('user-uuid-123');
  });

  it('debe retornar arreglo vacío de roles si no hay token o es inválido', () => {
    service.removeToken();
    expect(service.getRoles()).toEqual([]);
    expect(service.getUserId()).toBeNull();

    service.setToken('token-invalido');
    expect(service.getRoles()).toEqual([]);
    expect(service.getUserId()).toBeNull();
  });
});
