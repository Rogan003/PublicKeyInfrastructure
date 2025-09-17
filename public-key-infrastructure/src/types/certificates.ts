export interface CertificateItem {
  id: string;
  subject: string;
  issuer: string;
  validFrom: string;
  validTo: string;
  status: 'Valid' | 'Revoked' | 'Expired';
  basicConstraints: string;
  keyUsage: string;
  extendedKeyUsage: string;
}


