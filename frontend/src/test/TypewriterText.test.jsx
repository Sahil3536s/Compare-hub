import React from "react";
import { render, screen, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import TypewriterText from "../components/TypewriterText";

// The component's internal PHRASES (must match for assertions)
const PHRASES = [
  "Decide smarter.",
  "Save more.",
  "Find better deals.",
  "Travel smarter.",
  "Shop smarter.",
  "Compare everything.",
];

// Timing constants (must match component)
const TYPING_SPEED = 80;
const HOLD_DURATION = 2500;
const DELETING_SPEED = 45;
const PAUSE_AFTER_DELETE = 120;

describe("TypewriterText", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function advance(ms) {
    act(() => {
      vi.advanceTimersByTime(ms);
    });
  }

  function typeFullPhrase(phrase) {
    for (let i = 0; i < phrase.length; i++) {
      advance(TYPING_SPEED);
    }
  }

  function deleteFullPhrase(phrase) {
    for (let i = 0; i < phrase.length; i++) {
      advance(DELETING_SPEED);
    }
  }

  it("starts typing the first phrase character by character", () => {
    const { container } = render(<TypewriterText />);

    // Initially empty (charIndex = 0)
    const textSpan = container.querySelector("span > span:first-child");
    expect(textSpan.textContent).toBe("");

    // After one tick, first char appears
    advance(TYPING_SPEED);
    expect(textSpan.textContent).toBe("D");

    // After two ticks
    advance(TYPING_SPEED);
    expect(textSpan.textContent).toBe("De");
  });

  it("completes the first phrase", () => {
    const { container } = render(<TypewriterText />);
    typeFullPhrase(PHRASES[0]);
    const textSpan = container.querySelector("span > span:first-child");
    expect(textSpan.textContent).toBe(PHRASES[0]);
  });

  it("holds the completed phrase for HOLD_DURATION", () => {
    const { container } = render(<TypewriterText />);
    typeFullPhrase(PHRASES[0]);

    const textSpan = container.querySelector("span > span:first-child");

    // Still showing after most of the hold
    advance(HOLD_DURATION - 100);
    expect(textSpan.textContent).toBe(PHRASES[0]);
  });

  it("starts deleting after hold completes", () => {
    const { container } = render(<TypewriterText />);
    typeFullPhrase(PHRASES[0]);

    const textSpan = container.querySelector("span > span:first-child");

    // Complete the hold → switches to "holding" phase
    advance(HOLD_DURATION);
    // "holding" phase has delay=0, instantly switches to "deleting"
    advance(0);

    // Delete one character
    advance(DELETING_SPEED);
    expect(textSpan.textContent).toBe(PHRASES[0].slice(0, -1));
  });

  it("switches to the second phrase after fully deleting the first", () => {
    const { container } = render(<TypewriterText />);
    const textSpan = container.querySelector("span > span:first-child");

    // Type first phrase
    typeFullPhrase(PHRASES[0]);
    // Hold
    advance(HOLD_DURATION);
    // holding→deleting (delay=0)
    advance(1);
    // Delete first phrase
    deleteFullPhrase(PHRASES[0]);
    // Pause
    advance(PAUSE_AFTER_DELETE);

    // Now starts typing second phrase — first character
    advance(TYPING_SPEED);
    expect(textSpan.textContent).toBe("S");
  });

  it("types the second phrase completely", () => {
    const { container } = render(<TypewriterText />);
    const textSpan = container.querySelector("span > span:first-child");

    // Phase 1: type → hold → delete → pause
    typeFullPhrase(PHRASES[0]);
    advance(HOLD_DURATION);
    advance(1); // holding→deleting
    deleteFullPhrase(PHRASES[0]);
    advance(PAUSE_AFTER_DELETE);

    // Phase 2: type
    typeFullPhrase(PHRASES[1]);
    expect(textSpan.textContent).toBe(PHRASES[1]);
  });

  it("continues to the third phrase", () => {
    const { container } = render(<TypewriterText />);
    const textSpan = container.querySelector("span > span:first-child");

    // Phrase 0
    typeFullPhrase(PHRASES[0]);
    advance(HOLD_DURATION);
    advance(1);
    deleteFullPhrase(PHRASES[0]);
    advance(PAUSE_AFTER_DELETE);

    // Phrase 1
    typeFullPhrase(PHRASES[1]);
    advance(HOLD_DURATION);
    advance(1);
    deleteFullPhrase(PHRASES[1]);
    advance(PAUSE_AFTER_DELETE);

    // Phrase 2
    typeFullPhrase(PHRASES[2]);
    expect(textSpan.textContent).toBe(PHRASES[2]);
  });

  it("wraps from last phrase back to the first", () => {
    const { container } = render(<TypewriterText />);
    const textSpan = container.querySelector("span > span:first-child");

    // Cycle through all phrases
    for (const phrase of PHRASES) {
      typeFullPhrase(phrase);
      advance(HOLD_DURATION);
      advance(1);
      deleteFullPhrase(phrase);
      advance(PAUSE_AFTER_DELETE);
    }

    // Back to first phrase
    typeFullPhrase(PHRASES[0]);
    expect(textSpan.textContent).toBe(PHRASES[0]);
  });

  it("clears timer on unmount without errors", () => {
    const { unmount } = render(<TypewriterText />);
    advance(TYPING_SPEED * 5);
    unmount();
    // Should not throw
    advance(10000);
  });

  it("renders a cursor element", () => {
    const { container } = render(<TypewriterText />);
    const cursor = container.querySelector('[aria-hidden="true"]');
    expect(cursor).toBeInTheDocument();
  });
});
