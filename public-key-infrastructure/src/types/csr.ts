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
    subject: {
        CN: string;
        O: string;
        OU?: string;
        C?: string;
        E?: string;
    };
    notBefore: string;
    notAfter: string;
    keystoreFormat: KeystoreFormat;
    keystorePassword: string;
}

// backend parsed the uploaded CSR
export interface CSRPreview {
    subject: string;
    publicKey: string;
    //extensions: Record<string, any>;
    //san?: string[];
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