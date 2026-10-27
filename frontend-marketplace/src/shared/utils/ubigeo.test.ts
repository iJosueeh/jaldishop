import {
  getDepartments,
  getProvinces,
  getDistricts,
  findUbigeoByCode,
  formatUbigeoLocation,
} from './ubigeo';

describe('ubigeo utility', () => {
  it('loads all departments including Lima and Callao', () => {
    const deps = getDepartments();
    expect(deps.length).toBeGreaterThanOrEqual(25);
    expect(deps).toContain('LIMA');
    expect(deps).toContain('CALLAO');
    expect(deps).toContain('AREQUIPA');
    expect(deps).toContain('CUSCO');
  });

  it('retrieves provinces for a department correctly', () => {
    const limaProvinces = getProvinces('LIMA');
    expect(limaProvinces).toContain('LIMA');
    expect(limaProvinces).toContain('CAÑETE');
    expect(limaProvinces).toContain('HUAROCHIRI');
  });

  it('retrieves districts for department and province', () => {
    const districts = getDistricts('LIMA', 'LIMA');
    expect(districts.length).toBeGreaterThan(40);
    const miraflores = districts.find((d) => d.name === 'MIRAFLORES');
    expect(miraflores).toBeDefined();
    expect(miraflores?.code).toBe('150122');
  });

  it('finds ubigeo by 6-digit code', () => {
    const found = findUbigeoByCode('150122');
    expect(found).not.toBeNull();
    expect(found?.d).toBe('LIMA');
    expect(found?.p).toBe('LIMA');
    expect(found?.di).toBe('MIRAFLORES');
  });

  it('returns null for unknown ubigeo code', () => {
    expect(findUbigeoByCode('999999')).toBeNull();
    expect(findUbigeoByCode('')).toBeNull();
  });

  it('formats location label without repeating province when same as department', () => {
    expect(formatUbigeoLocation('LIMA', 'LIMA', 'MIRAFLORES')).toBe('MIRAFLORES, LIMA');
    expect(formatUbigeoLocation('CUSCO', 'URUBAMBA', 'OLLANTAYTAMBO')).toBe(
      'OLLANTAYTAMBO, URUBAMBA, CUSCO'
    );
  });
});
