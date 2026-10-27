/**
 * Utility to validate and sanitize redirect URLs, preventing Open Redirect vulnerabilities.
 * Only internal relative paths are permitted.
 */
export function getSafeRedirectUrl(target: string | null | undefined, fallback: string = '/'): string {
  if (!target || typeof target !== 'string') {
    return fallback;
  }

  const trimmed = target.trim();

  // Must begin with '/' and NOT with '//' (protocol-relative) or '/\' (Windows SMB path bypass)
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.startsWith('/\\')) {
    return fallback;
  }

  // Extract path component before any query string or hash
  const pathPart = trimmed.split(/[?#]/)[0];

  // Block any scheme/protocol injection (e.g. '/javascript:', '/data:', or encoded backslashes)
  if (pathPart.includes(':') || pathPart.includes('\\')) {
    return fallback;
  }

  return trimmed;
}
