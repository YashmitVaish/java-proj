import { useEffect, useId, useRef, useState } from 'react';
import { monthLabel, shiftMonth } from '../lib/format.js';

// ---------------------------------------------------------------------------
// Modal built on <dialog>: focus trapping, Esc and the backdrop come for free.
// ---------------------------------------------------------------------------
export function Modal({ title, onClose, children, width = 460 }) {
  const ref = useRef(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.open && dialog.close();
  }, []);

  return (
    <dialog
      ref={ref}
      className="modal"
      style={{ '--modal-width': `${width}px` }}
      aria-labelledby={titleId}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onMouseDown={(e) => e.target === ref.current && onClose()}
    >
      <div className="modal-inner">
        <div className="modal-head">
          <h2 id={titleId}>{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">
            <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
              <path d="M4 4l10 10M14 4L4 14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </button>
        </div>
        {children}
      </div>
    </dialog>
  );
}

/** "Are you sure?" dialog. onConfirm may return a promise; errors are shown inline. */
export function ConfirmDialog({ title, children, confirmLabel, onConfirm, onClose }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (e) {
      setError(e);
      setBusy(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose} width={420}>
      <div className="confirm-text">{children}</div>
      <FormError error={error} />
      <div className="modal-actions">
        <button type="button" className="btn" onClick={onClose}>
          Cancel
        </button>
        <button type="button" className="btn btn-danger" onClick={confirm} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}

// ---------------------------------------------------------------------------
// Form bits
// ---------------------------------------------------------------------------
export function Field({ label, error, hint, children, className = '' }) {
  return (
    <label className={`field ${error ? 'has-error' : ''} ${className}`}>
      <span className="field-label">{label}</span>
      {children}
      {error ? <span className="field-error">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  );
}

/** Shows an API error. Field-level validation errors are shown next to their fields instead. */
export function FormError({ error }) {
  if (!error) return null;
  const hasFields = error.fieldErrors && Object.keys(error.fieldErrors).length > 0;
  return (
    <p className="form-error" role="alert">
      {hasFields ? 'Some fields need fixing. Check the messages above.' : error.message}
    </p>
  );
}

/** Segmented control backed by radio buttons, so arrow keys work. */
export function Segmented({ label, value, onChange, options, name }) {
  const autoName = useId();
  return (
    <fieldset className="segmented">
      <legend className="sr-only">{label}</legend>
      {options.map((opt) => (
        <label key={opt.value} className={value === opt.value ? 'is-on' : ''}>
          <input
            type="radio"
            name={name || autoName}
            value={opt.value}
            checked={value === opt.value}
            onChange={() => onChange(opt.value)}
          />
          {opt.label}
        </label>
      ))}
    </fieldset>
  );
}

// ---------------------------------------------------------------------------
// Month / year stepper
// ---------------------------------------------------------------------------
function Chevron({ dir }) {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
      <path
        d={dir === 'left' ? 'M10 3L5 8l5 5' : 'M6 3l5 5-5 5'}
        stroke="currentColor"
        strokeWidth="1.8"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function Stepper({ label, onPrev, onNext, prevLabel, nextLabel, disabled }) {
  return (
    <div className={`stepper ${disabled ? 'is-disabled' : ''}`}>
      <button type="button" className="icon-btn" onClick={onPrev} aria-label={prevLabel} disabled={disabled}>
        <Chevron dir="left" />
      </button>
      <span className="stepper-label" aria-live="polite">
        {label}
      </span>
      <button type="button" className="icon-btn" onClick={onNext} aria-label={nextLabel} disabled={disabled}>
        <Chevron dir="right" />
      </button>
    </div>
  );
}

export function MonthStepper({ month, setMonth, disabled }) {
  return (
    <Stepper
      label={monthLabel(month)}
      onPrev={() => setMonth(shiftMonth(month, -1))}
      onNext={() => setMonth(shiftMonth(month, 1))}
      prevLabel="Previous month"
      nextLabel="Next month"
      disabled={disabled}
    />
  );
}

// ---------------------------------------------------------------------------
// Loading / error / empty
// ---------------------------------------------------------------------------
export function LoadError({ error, onRetry }) {
  return (
    <div className="load-error" role="alert">
      <p>{error.message}</p>
      {onRetry && (
        <button type="button" className="btn" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function Loading({ label = 'Loading' }) {
  return (
    <p className="loading" role="status">
      {label}…
    </p>
  );
}

export function Swatch({ color }) {
  return <span className="swatch" style={{ background: color }} aria-hidden="true" />;
}
