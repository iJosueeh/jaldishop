export type MediaTargetType = 'STORE_LOGO' | 'STORE_BANNER' | 'PRODUCT_IMAGE';

export interface UploadSignatureRequest {
  targetType: MediaTargetType;
  storeId?: string;
}

export interface UploadSignatureResponse {
  cloudName: string;
  apiKey: string;
  timestamp: number;
  folder: string;
  signature: string;
  uploadUrl: string;
}

export interface CloudinaryUploadResult {
  public_id: string;
  secure_url: string;
  format: string;
  width: number;
  height: number;
  bytes: number;
  original_filename?: string;
}

export interface MediaUploadProgress {
  state: 'VALIDATING' | 'REQUESTING_SIGNATURE' | 'UPLOADING' | 'COMPLETED' | 'ERROR';
  progress: number;
  result?: CloudinaryUploadResult;
  error?: string;
}
