import { useState, useEffect } from "react";

// ──────────────────────────────────────────────────────────────
// PHRASES — defined OUTSIDE the component (stable reference)
// ──────────────────────────────────────────────────────────────
const PHRASES = [
  "Decide smarter.",
  "Save more.",
  "Find better deals.",
  "Travel smarter.",
  "Shop smarter.",
  "Compare everything.",
];

// ──────────────────────────────────────────────────────────────
// TypewriterText — minimal, self-contained typewriter animation
// ──────────────────────────────────────────────────────────────
export default function TypewriterText() {
  const [phraseIndex, setPhraseIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [phase, setPhase] = useState("typing");

  // ── Single useEffect drives the entire state machine ──
  useEffect(() => {
    const currentPhrase = PHRASES[phraseIndex];
    let delay;

    if (phase === "typing") {
      if (charIndex < currentPhrase.length) {
        delay = 80;
      } else {
        // Typing complete → hold
        delay = 2500;
      }
    } else if (phase === "holding") {
      // holding phase entered → immediately start deleting
      delay = 0;
    } else if (phase === "deleting") {
      if (charIndex > 0) {
        delay = 45;
      } else {
        // Deletion complete → next phrase, restart typing
        delay = 120; // brief pause before next phrase
      }
    }

    const timer = setTimeout(() => {
      if (phase === "typing") {
        if (charIndex < currentPhrase.length) {
          // Type next character
          setCharIndex((c) => c + 1);
        } else {
          // Full phrase typed → switch to holding
          setPhase("holding");
        }
      } else if (phase === "holding") {
        // Hold complete → start deleting
        setPhase("deleting");
      } else if (phase === "deleting") {
        if (charIndex > 0) {
          // Delete one character
          setCharIndex((c) => c - 1);
        } else {
          // All deleted → next phrase
          setPhraseIndex((p) => (p + 1) % PHRASES.length);
          setPhase("typing");
        }
      }
    }, delay);

    return () => clearTimeout(timer);
  }, [phraseIndex, charIndex, phase]);

  // ── Derive displayed text from state ──
  const text = PHRASES[phraseIndex].slice(0, charIndex);

  return (
    <span className="inline-flex items-baseline text-red-500">
      <span>{text}</span>
      <span
        className="inline-block w-[2px] sm:w-[3px] h-[0.85em] bg-red-500 ml-0.5 align-baseline shrink-0 animate-pulse"
        aria-hidden="true"
      />
    </span>
  );
}
