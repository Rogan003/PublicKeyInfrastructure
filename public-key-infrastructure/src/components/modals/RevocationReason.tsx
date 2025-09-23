import React, { useState } from 'react';

interface RevocationReasonModalProps {
    onClose: () => void;
    onConfirm: (reason: string, comment?: string) => void;
}

const RevocationReason: React.FC<RevocationReasonModalProps> = ({ onClose, onConfirm }) => {
    // X.509 standard reasons for certificate revocation
    const revocationReasons = [
        { label: 'Key Compromise', value: 'key_compromise' },
        { label: 'CA Compromise', value: 'ca_compromise' },
        { label: 'Affiliation Changed', value: 'affiliation_changed' },
        { label: 'Superseded', value: 'superseded' },
        { label: 'Cease Operation', value: 'cease_operation' },
    ];

    const [selectedReason, setSelectedReason] = useState<string | null>(null);
    const [customEnabled, setCustomEnabled] = useState(false);
    const [comment, setComment] = useState<string>('');

    const handleSubmit = () => {
        if (!selectedReason) {
            alert('Please select a reason for revocation.');
            return;
        }
        onConfirm(selectedReason, customEnabled ? comment : undefined);
        onClose();
    };

    return (
        <div className="modal-backdrop" role="dialog" aria-modal="true" onClick={onClose}>
            <div
                className="modal-card surface-card"
                onClick={(e) => e.stopPropagation()}
                style={{ width: '100%', maxWidth: 520 }}
            >
                <div className="modal-header">
                    <h3 className="modal-title">Certificate Revocation</h3>
                    <button className="button-outlined" onClick={onClose} aria-label="Close">
                        <span className="material-symbols-outlined" aria-hidden>close</span>
                        Close
                    </button>
                </div>

                <div className="modal-content">
                    <p>Please select a reason for revoking the certificate:</p>

                    <div className="form-grid" style={{ gridTemplateColumns: '1fr' }}>
                        {revocationReasons.map((reason) => (
                            <label key={reason.value} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                <input
                                    type="radio"
                                    name="revocationReason"
                                    value={reason.value}
                                    checked={selectedReason === reason.value}
                                    onChange={() => setSelectedReason(reason.value)}
                                />
                                <span>{reason.label}</span>
                            </label>
                        ))}
                    </div>

                    <div style={{ marginTop: 16 }}>
                        <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input
                                type="checkbox"
                                checked={customEnabled}
                                onChange={() => setCustomEnabled((prev) => !prev)}
                            />
                            <span>Add your comment</span>
                        </label>
                        {customEnabled && (
                            <textarea
                                placeholder="Enter your comment..."
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                                style={{
                                    width: '100%',
                                    marginTop: 8,
                                    minHeight: 90,
                                    padding: 10,
                                    borderRadius: 8,
                                }}
                            />
                        )}
                    </div>

                    <div className="modal-actions">
                        <button className="button-outlined" onClick={onClose}>
                            Cancel
                        </button>
                        <button className="button-danger" onClick={handleSubmit}>
                            Revoke Certificate
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RevocationReason;