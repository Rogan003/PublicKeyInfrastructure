import React, { useState } from 'react';
import { apiService } from '../../services/api';
import toast from 'react-hot-toast';

interface CRLViewerProps {
    onClose: () => void;
}

const CRLViewer: React.FC<CRLViewerProps> = ({ onClose }) => {
    const [certId, setCertId] = useState<string>('');
    const [crlContent, setCrlContent] = useState<string>('');
    const [loading, setLoading] = useState(false);

    const handleViewCRL = async () => {
        if (!certId || isNaN(Number(certId))) {
            toast.error('Please enter a valid certificate ID');
            return;
        }

        setLoading(true);
        try {
            const crl = await apiService.getCRLForCA(Number(certId));
            setCrlContent(crl);
        } catch (error: unknown) {
            const errorMessage = (error as { response?: { data?: string } })?.response?.data || 'Failed to fetch CRL';
            toast.error(errorMessage);
            setCrlContent('');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-backdrop" role="dialog" aria-modal="true" onClick={onClose}>
            <div
                className="modal-card surface-card"
                onClick={(e) => e.stopPropagation()}
                style={{ width: '100%', maxWidth: 800, height: '80vh', display: 'flex', flexDirection: 'column' }}
            >
                <div className="modal-header">
                    <h3 className="modal-title">Certificate Revocation List (CRL)</h3>
                    <button className="button-outlined" onClick={onClose} aria-label="Close">
                        <span className="material-symbols-outlined" aria-hidden>close</span>
                        Close
                    </button>
                </div>

                <div className="modal-content" style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 16, overflow: 'hidden' }}>
                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
                        <div style={{ flex: 1 }}>
                            <label htmlFor="cert-id" style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
                                CA Certificate ID:
                            </label>
                            <input
                                id="cert-id"
                                type="number"
                                value={certId}
                                onChange={(e) => setCertId(e.target.value)}
                                placeholder="Enter certificate ID"
                                style={{
                                    width: '100%',
                                    padding: '8px 12px',
                                    borderRadius: 8,
                                    border: '1px solid #ccc',
                                    fontSize: 14,
                                    color: 'inherit'
                                }}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') {
                                        handleViewCRL();
                                    }
                                }}
                            />
                        </div>
                        <button
                            className="button-primary"
                            onClick={handleViewCRL}
                            disabled={loading}
                            style={{ whiteSpace: 'nowrap' }}
                        >
                            <span className="material-symbols-outlined" aria-hidden>search</span>
                            {loading ? 'Loading...' : 'View CRL'}
                        </button>
                    </div>

                    {crlContent && (
                        <div style={{
                            flex: 1,
                            display: 'flex',
                            flexDirection: 'column',
                            minHeight: 0,
                            border: '1px solid #ddd',
                            borderRadius: 8,
                            overflow: 'hidden'
                        }}>
                            <div style={{
                                padding: '8px 12px',
                                backgroundColor: '#1a1a1a',
                                borderBottom: '1px solid #333',
                                fontWeight: 500,
                                fontSize: 14,
                                color: '#fff'
                            }}>
                                CRL Content (PEM Format):
                            </div>
                            <textarea
                                readOnly
                                value={crlContent}
                                style={{
                                    flex: 1,
                                    width: '100%',
                                    padding: 12,
                                    border: 'none',
                                    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
                                    fontSize: 12,
                                    resize: 'none',
                                    backgroundColor: '#000',
                                    color: '#0f0',
                                    whiteSpace: 'pre',
                                    overflow: 'auto'
                                }}
                            />
                        </div>
                    )}

                    {!crlContent && !loading && (
                        <div style={{
                            flex: 1,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: '#999',
                            fontSize: 14
                        }}>
                            Enter a CA certificate ID and click "View CRL" to display the revocation list
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CRLViewer;