export type KeystoreFormat = 'PKCS12' | 'JKS';

// which CA can sign my certificate
export interface CAInfo {
    id: string;
    serialNumber: string;
    commonName: string;
    subjectDN: string;
    issuerDN: string;
    validFrom: string;
    validTo: string;
    isRevoked: boolean;
    keystoreFormat: string;
}

// CA's rules
export interface IssuerConstraints {
    issuerId: string;
    issuerValidFrom: string;
    issuerValidTo: string;
    issuerRevoked: boolean;
    maxTTLDays?: number;
}

// when user uploads a CSR
export interface CSRUploadRequest {
    issuerCAId: string;
    csrPem: string;
    notBefore: string;
    notAfter: string;
}

// when user wants to generate a CSR automatically
export interface CSRAutogenRequest {
    issuerCAId: string;
    subjectCN: string;
    subjectO: string;
    subjectOU?: string;
    subjectC?: string;
    subjectE?: string;
    notBefore: string;
    notAfter: string;
    keystoreFormat: KeystoreFormat;
    keystorePassword: string;
}

// backend parsed the uploaded CSR
export interface CSRPreview {
    subject: string;
    publicKey: string;
    extensions: string[];
}

// server's response after user issues a certificate
export interface IssueResponse {
    certificateId: string;
    certificatePem?: string;
    // for autogen: backend may return download token/URL for keystore
    keystoreDownloadUrl?: string;
}

export interface RevocationReason {
    code: number;
    name: string;
    description?: string;
}

export interface IssueCertificateResponse {
    id: number;
    serialNumber: string;
    subjectDN: string;
    issuerDN: string;
    validFrom: string; // Dates formatted as strings (e.g., "YYYY-MM-DD")
    validTo: string;
    certificateType: string;
    extensions: string[]; // Array of extension strings
}
