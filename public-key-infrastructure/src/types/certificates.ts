
export interface CertificateItem {
  id: string;
  subject: string;
  issuer: string;
  commonName: string;
  givenName: string;
  surname: string;
  organizationName: string;
  organizationUnit: string;
  country: string;
  email: string;
  owner: string;
  validFrom: string;
  validTo: string;
  status: 'Valid' | 'Revoked' | 'Expired' | 'Pending';
  basicConstraints: string;
  keyUsage: string;
  extendedKeyUsage: string;
  certificateType: string;
}


