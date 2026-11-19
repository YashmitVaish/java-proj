import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Loads data with an async function and re-runs it whenever `deps` change.
 * Returns { data, error, loading, reload }. Stale responses are ignored.
 */
export function useLoad(fn, deps) {
  const [state, setState] = useState({ data: undefined, error: null, loading: true });
  const runId = useRef(0);
  const fnRef = useRef(fn);
  useEffect(() => {
    fnRef.current = fn;
  });

  const reload = useCallback(() => {
    const id = ++runId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    fnRef.current()
      .then((data) => id === runId.current && setState({ data, error: null, loading: false }))
      .catch((error) => id === runId.current && setState((s) => ({ ...s, error, loading: false })));
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(reload, deps);

  return { ...state, reload };
}

/** Sets document.title for a page. */
export function useTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} – Khata` : 'Khata';
  }, [title]);
}
