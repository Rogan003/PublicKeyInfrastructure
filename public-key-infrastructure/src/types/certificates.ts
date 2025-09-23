export interface CreateCertificate {
    issuerCertificateId?: number;
    subject: Subject;
    startDate: Date;
    endDate: Date;
    /**
     * Supported values:
     * BASIC_CA, BASIC_END, KEY_USAGE, EXT_KEY_SERVER, EXT_KEY_CLIENT, SAN
     */
    extensions: string[];
    ownerId: number;
}

export interface Subject {
    commonName: string;
    surname: string;
    givenName: string;
    organizationName: string;
    organizationUnit: string;
    country: string;
    email: string;
}

export interface CACertificateInfo {
    id: number;
    name: string;
}

export interface UserPotentialOwner {
    id: number;
    nameWithOrg: string;
}

export interface Certificate {
    id: number;
    subject: string;
    issuer: string;
    validFrom: string;
    validTo: string;
    extensions: string[];
}