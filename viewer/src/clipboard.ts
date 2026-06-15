export type ClipboardCopyState = "copied" | "blocked";

interface ClipboardWriter {
  writeText(text: string): Promise<void> | void;
}

export async function copyTextToClipboard(text: string, clipboard: ClipboardWriter | undefined | null): Promise<ClipboardCopyState> {
  if (!clipboard?.writeText) {
    return "blocked";
  }

  try {
    await clipboard.writeText(text);
    return "copied";
  } catch {
    return "blocked";
  }
}
