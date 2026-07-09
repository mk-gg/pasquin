/**
 * Spam honeypot: a field hidden from real users but that bots tend to fill.
 * The backend silently drops any submission where `website` is non-empty.
 * Hidden with inline styles + aria-hidden + tabIndex so assistive tech and
 * keyboard users skip it, while remaining in the DOM for bots to find.
 */
export function HoneypotField() {
  return (
    <div
      aria-hidden="true"
      style={{
        position: "absolute",
        width: 1,
        height: 1,
        overflow: "hidden",
        clip: "rect(0 0 0 0)",
        whiteSpace: "nowrap",
      }}
    >
      <label htmlFor="website">Leave this field empty</label>
      <input
        id="website"
        name="website"
        type="text"
        tabIndex={-1}
        autoComplete="off"
      />
    </div>
  )
}

export default HoneypotField
