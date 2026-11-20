import { useState } from 'react';

// ---------------------------------------------------------------------------
// Tooltip that follows the pointer (or sits over the focused element).
// const tip = useTooltip(); <div {...tip.bind('text')} />; {tip.layer}
// ---------------------------------------------------------------------------
export function useTooltip() {
  const [tip, setTip] = useState(null);

  const bind = (content) => ({
    onMouseEnter: (e) => setTip({ content, x: e.clientX, y: e.clientY }),
    onMouseMove: (e) => setTip({ content, x: e.clientX, y: e.clientY }),
    onMouseLeave: () => setTip(null),
    onFocus: (e) => {
      const r = e.currentTarget.getBoundingClientRect();
      setTip({ content, x: r.left + r.width / 2, y: r.top });
    },
    onBlur: () => setTip(null),
  });

  const layer = tip ? (
    <div className="tooltip" role="tooltip" style={{ left: tip.x, top: tip.y }}>
      {tip.content}
    </div>
  ) : null;

  return { bind, layer, active: tip };
}
