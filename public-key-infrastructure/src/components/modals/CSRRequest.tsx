import React, { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { apiService } from '../../services/api';
import type {CAInfo, CSRPreview, CSRUploadRequest, CSRAutogenRequest, KeystoreFormat, IssuerConstraints, IssueCertificateResponse} from '../../types/csr';
import { useAuth } from '../../contexts/AuthContext';
import type { User } from '../types/auth';
import type {UserInfo} from "../../types/auth.ts";

interface CSRRequestModalProps {
    onClose: () => void;
    onIssued: (issued: IssueCertificateResponse) => void; // to update dashboard list
}

type Tab = 'UPLOAD' | 'AUTOGENERATE';
const MAX_PEM_SIZE_BYTES = 32 * 1024; // 32KB

function withinIssuerLimits(notBefore: string, notAfter: string, constraints?: IssuerConstraints): string | null {
    if (constraints) {
        const nb = new Date(notBefore).getTime();
        const na = new Date(notAfter).getTime();
        const ivf = new Date(constraints.issuerValidFrom).getTime();
        const ivt = new Date(constraints.issuerValidTo).getTime();
        if (nb < ivf) return 'NotBefore is before issuer validity period';
        if (na > ivt) return 'NotAfter exceeds issuer validity period';
        if (constraints.issuerRevoked) return 'Selected issuer is revoked';
        if (constraints.maxTTLDays) {
            const maxNa = new Date(new Date(notBefore).getTime() + constraints.maxTTLDays * 86400_000);
            if (new Date(notAfter) > maxNa) return `Requested TTL exceeds issuer limit of ${constraints.maxTTLDays} days`;
        }
    }
    return null;
}

const CSRRequestModal: React.FC<CSRRequestModalProps> = ({ onClose, onIssued }) => {
    const [activeTab, setActiveTab] = useState<Tab>('UPLOAD');
    const [cas, setCAs] = useState<CAInfo[]>([]);
    const [issuerCAId, setIssuerCAId] = useState<string>('');
    const [constraints, setConstraints] = useState<IssuerConstraints | undefined>(undefined);
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

    const [notBefore, setNotBefore] = useState<string>('');
    const [notAfter, setNotAfter] = useState<string>('');

    // Upload CSR state
    const [csrPem, setCsrPem] = useState<string>('');
    const [csrPreview, setCsrPreview] = useState<CSRPreview | null>(null);
    const [isParsing, setIsParsing] = useState(false);

    // Autogen state
    const [CN, setCN] = useState('');
    const [O, setO] = useState('');
    const [OU, setOU] = useState('');
    const [C, setC] = useState('');
    const [E, setE] = useState('');
    const [keystoreFormat, setKeystoreFormat] = useState<KeystoreFormat>('PKCS12');
    const [keystorePassword, setKeystorePassword] = useState('');

    const isDateRangeValid = useMemo(() => {
        if (!notBefore || !notAfter) return false;
        return new Date(notAfter) > new Date(notBefore);
    }, [notBefore, notAfter]);

    useEffect(() => {
        const load = async () => {
            try {
                const casResp = await apiService.getCAs();
                setCAs(casResp);

                const userInfo: UserInfo = await apiService.getUserInfo();
                setUserInfo(userInfo);

            } catch (e) {
                toast.error('Failed to load CA list: ' + e);
            }
        };
        load();
    }, []);

    useEffect(() => {
        const loadIssuerData = async () => {
            if (!issuerCAId) {
                setConstraints(undefined);
                return;
            }
            try {
                const cons = await apiService.getIssuerConstraints(issuerCAId);
                setConstraints(cons);
            } catch {
                toast.error('Failed to load issuer constraints');
            }
        };
        loadIssuerData();
    }, [issuerCAId]);

    useEffect(() => {
        if (activeTab !== 'AUTOGENERATE' || !userInfo) return;
        setE((prev) => prev || userInfo.email || '');
        setO((prev) => prev || userInfo.organization || '');
        setOU((prev) => prev || userInfo.unit || '');
        setC((prev) => prev || userInfo.country || '');

    }, [activeTab, userInfo]);

    const onSelectCSR = async (file?: File) => {
        setCsrPreview(null);
        setCsrPem('');
        if (!file) return;
        if (file.size > MAX_PEM_SIZE_BYTES) {
            toast.error('CSR file is too large');
            return;
        }
        if (!file.name.toLowerCase().endsWith('.pem')) {
            toast.error('Please upload a .pem file');
            return;
        }
        const text = await file.text();
        if (!text.includes('-----BEGIN CERTIFICATE REQUEST-----') || !text.includes('-----END CERTIFICATE REQUEST-----')) {
            toast.error('Invalid PEM CSR format');
            return;
        }
        setCsrPem(text);
    };

    const parseCSR = async () => {
        if (!csrPem) return;
        setIsParsing(true);
        try {
            const preview = await apiService.parseCSR(csrPem);
            setCsrPreview(preview);
            toast.success('CSR parsed');
        } catch {
            toast.error('Failed to parse CSR');
        } finally {
            setIsParsing(false);
        }
    };

    const canSubmitUpload = !!issuerCAId && !!csrPem && isDateRangeValid && !withinIssuerLimits(notBefore, notAfter, constraints);

    const submitUpload = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!canSubmitUpload) return;

        try {
            const payload: CSRUploadRequest = {
                issuerCAId,
                csrPem,
                notBefore,
                notAfter,
            };
            const res = await apiService.issueFromCSR(payload);
            toast.success('Certificate issued from CSR');
            onIssued(res);
            onClose();

        } catch (err) {
            toast.error('Issue from CSR failed: ' + err);
        }
    };

    const canSubmitAutogen =
        !!issuerCAId && !!CN && !!O && !!E && !!keystorePassword && isDateRangeValid &&
        !withinIssuerLimits(notBefore, notAfter, constraints);

    const submitAutogen = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!canSubmitAutogen) return;

        try {
            const payload: CSRAutogenRequest = {
                issuerCAId,
                subjectCN: CN,
                subjectO: O,
                subjectOU: OU || undefined,
                subjectC: C || undefined,
                subjectE: E,
                notBefore,
                notAfter,
                keystoreFormat,
                keystorePassword,
            };

            const res = await apiService.issueAutogenerated(payload);
            toast.success('Certificate and keystore generated');

            onIssued(res);
            onClose();
        } catch {
            toast.error('Autogenerate issuance failed');
        }
    };

    return (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
            <div className="modal-card surface-card" style={{paddingBottom: '10px'}}>
                <div className="modal-header">
                    <h3 className="modal-title">Request End-Entity Certificate</h3>
                    <button className="button-outlined" onClick={onClose} aria-label="Close">
                        <span className="material-symbols-outlined" aria-hidden>close</span>
                        Close
                    </button>
                </div>

                <div className="modal-content">
                    <div className="tabs" style={{marginBottom: '15px'}}>
                        <button style={{marginRight: '13px'}} className={`tab ${activeTab === 'UPLOAD' ? 'active' : ''}`} onClick={() => setActiveTab('UPLOAD')}>Upload CSR</button>
                        <button className={`tab ${activeTab === 'AUTOGENERATE' ? 'active' : ''}`} onClick={() => setActiveTab('AUTOGENERATE')}>Autogenerate</button>
                    </div>

                    {/* Common issuer/date controls */}
                    <div className="form-grid" style={{marginBottom: '15px'}}>
                        <label className="form-group">
                            <span className="meta-label" style={{marginBottom: '7px'}}>Issuer CA</span>
                            <select className="m3-select" value={issuerCAId} onChange={(e) => setIssuerCAId(e.target.value)} required>
                                <option value="">Select issuer CA</option>
                                {cas.map(ca => (
                                    <option key={ca.id} value={ca.id} disabled={ca.isRevoked}>
                                        {ca.commonName} (validTo: {ca.validTo}) {ca.isRevoked ? ' - revoked' : ''}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>
                    <div className="form-grid-two-columns" style={{marginTop: '25px'}}>
                        <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                            <span className="meta-label">Not Before</span>
                            <input type="date" value={notBefore} onChange={(e) => setNotBefore(e.target.value)} required />
                        </label>
                        <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                            <span className="meta-label">Not After</span>
                            <input type="date" value={notAfter} onChange={(e) => setNotAfter(e.target.value)} required />
                        </label>
                    </div>

                    {(() => {
                        const err = notBefore && notAfter ? withinIssuerLimits(notBefore, notAfter, constraints) : null;
                        return err ? <div className="error" style={{marginBottom: '20px'}} role="alert">{err}</div> : null;
                    })()}

                    {activeTab === 'UPLOAD' && (
                        <form onSubmit={submitUpload} >
                            {/* Row: CSR .pem */}
                            <div className="form-grid">
                                <label className="form-group">
                                    <span className="meta-label">CSR (.pem)</span>
                                    <input type="file" accept=".pem" onChange={(e) => onSelectCSR(e.target.files?.[0] || undefined)} />
                                </label>
                            </div>

                            {/* Row: Preview CSR and Issue from CSR in the same row */}
                            <div className="inline-actions" style={{ display: 'flex', gap: 12, marginTop: '30px'}}>
                                <button type="button" className="button-tonal" onClick={parseCSR} disabled={!csrPem || isParsing}>
                                    {isParsing ? 'Parsing...' : 'Preview CSR'}
                                </button>
                                <button type="submit" disabled={!canSubmitUpload}>
                                    Issue from CSR
                                </button>
                            </div>

                            {csrPreview && (
                                <div className="surface-card" style={{ padding: 12, marginTop: 0 }}>
                                    <h4>CSR Preview</h4>
                                        <div
                                            style={{
                                                display: 'grid',
                                                gridTemplateColumns: '160px 1fr',
                                                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                                                fontSize: 14,
                                                background: 'transparent',
                                                whiteSpace: 'pre-wrap',
                                                rowGap: 8,
                                                columnGap: 12,
                                                alignItems: 'start',
                                            }}>
                                        <span className="meta-label" style={{ alignSelf: 'center' }}>Subject</span>
                                        <span
                                            className="meta-value"
                                            title={csrPreview.subject}
                                            style={{
                                                overflowWrap: 'anywhere',
                                                wordBreak: 'break-word',
                                                whiteSpace: 'pre-wrap',
                                                lineHeight: 1.35,
                                            }}>
                                        {csrPreview.subject}
                                            </span>
                                            <span className="meta-label" style={{ alignSelf: 'start' }}>Public Key</span>
                                            <pre
                                                title={csrPreview.publicKey}
                                                style={{
                                                    margin: 0,
                                                    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                                                    fontSize: 12,
                                                    background: 'transparent',
                                                    whiteSpace: 'pre-wrap',
                                                    wordBreak: 'break-word',
                                                    overflowWrap: 'anywhere',
                                                    lineHeight: 1.35,
                                                }}
                                            >
                                            {csrPreview.publicKey}
                                        </pre>
                                    </div>
                                </div>
                            )}
                        </form>
                    )}

                    {activeTab === 'AUTOGENERATE' && (
                        <form onSubmit={submitAutogen}>
                            <div
                                className="form-grid-two-columns"
                                style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 24 }}
                            >
                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Common Name (CN)</span>
                                    <input type="text" value={CN} onChange={(e) => setCN(e.target.value)} required />
                                </label>
                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Organization (O)</span>
                                    <input style={{opacity: '0.5'}} disabled type="text" value={O} onChange={(e) => setO(e.target.value)} required />
                                </label>
                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Org Unit (OU)</span>
                                    <input style={{opacity: '0.5'}} disabled type="text" value={OU} onChange={(e) => setOU(e.target.value)} />
                                </label>

                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Country (C)</span>
                                    <input style={{opacity: '0.5'}} disabled type="text" value={C} onChange={(e) => setC(e.target.value)} />
                                </label>
                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Email (E)</span>
                                    <input style={{opacity: '0.5'}} disabled type="email" value={E} onChange={(e) => setE(e.target.value)} required />
                                </label>
                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Keystore format</span>
                                    <select className="m3-select" value={keystoreFormat} onChange={(e) => setKeystoreFormat(e.target.value as KeystoreFormat)}>
                                        <option value="PKCS12">PKCS12 (.p12)</option>
                                        <option value="JKS">JKS (.jks)</option>
                                    </select>
                                </label>

                                <label className="form-group" style={{display: 'flex', flexDirection: 'column', gap: '7px'}}>
                                    <span className="meta-label">Keystore password</span>
                                    <input type="password" value={keystorePassword} onChange={(e) => setKeystorePassword(e.target.value)} required />
                                </label>
                                <div style={{ alignSelf: 'end' }}>
                                    <button type="submit" disabled={!canSubmitAutogen}>
                                        Issue (Autogenerate)
                                    </button>
                                </div>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CSRRequestModal;