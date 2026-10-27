import rawUbigeoData from '../data/ubigeo-peru.json';

export interface RawUbigeoEntry {
  c: string; // Ubigeo Code (6 digits)
  d: string; // Department
  p: string; // Province
  di: string; // District
}

export interface DistrictOption {
  code: string;
  name: string;
}

const entries = rawUbigeoData as RawUbigeoEntry[];

// Pre-compute unique departments, sorted alphabetically
const departments: string[] = Array.from(
  new Set(entries.map((e) => e.d))
).sort((a, b) => a.localeCompare(b, 'es'));

// Cache maps for provinces and districts
const provincesByDepartment = new Map<string, string[]>();
const districtsByDepartmentAndProvince = new Map<string, DistrictOption[]>();
const entryByCode = new Map<string, RawUbigeoEntry>();

entries.forEach((e) => {
  entryByCode.set(e.c, e);

  // Provinces per department
  if (!provincesByDepartment.has(e.d)) {
    provincesByDepartment.set(e.d, []);
  }
  const provList = provincesByDepartment.get(e.d)!;
  if (!provList.includes(e.p)) {
    provList.push(e.p);
  }

  // Districts per department + province key
  const depProvKey = `${e.d}::${e.p}`;
  if (!districtsByDepartmentAndProvince.has(depProvKey)) {
    districtsByDepartmentAndProvince.set(depProvKey, []);
  }
  districtsByDepartmentAndProvince.get(depProvKey)!.push({
    code: e.c,
    name: e.di,
  });
});

// Sort province lists and district lists
provincesByDepartment.forEach((provinces) => {
  provinces.sort((a, b) => a.localeCompare(b, 'es'));
});

districtsByDepartmentAndProvince.forEach((districts) => {
  districts.sort((a, b) => a.name.localeCompare(b.name, 'es'));
});

/**
 * Returns all departments in Peru (25, including Prov. Const. del Callao).
 */
export function getDepartments(): string[] {
  return departments;
}

/**
 * Returns all provinces belonging to a given department.
 */
export function getProvinces(department: string): string[] {
  if (!department) return [];
  return provincesByDepartment.get(department) || [];
}

/**
 * Returns all districts (with code and name) for a given department and province.
 */
export function getDistricts(department: string, province: string): DistrictOption[] {
  if (!department || !province) return [];
  const key = `${department}::${province}`;
  return districtsByDepartmentAndProvince.get(key) || [];
}

/**
 * Finds an official entry by its 6-digit UBIGEO code.
 */
export function findUbigeoByCode(code: string): RawUbigeoEntry | null {
  if (!code || code.length !== 6) return null;
  return entryByCode.get(code) || null;
}

/**
 * Formats department, province, district into a readable Peru location string.
 */
export function formatUbigeoLocation(
  department: string,
  province: string,
  district: string
): string {
  if (department === province) {
    return `${district}, ${department}`;
  }
  return `${district}, ${province}, ${department}`;
}
