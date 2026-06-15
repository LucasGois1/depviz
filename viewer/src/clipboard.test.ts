import { describe, expect, it, vi } from "vitest";
import { copyTextToClipboard } from "./clipboard";

describe("copyTextToClipboard", () => {
  it("returns copied when clipboard write succeeds", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);

    await expect(copyTextToClipboard("org.example:demo:jar::1.0.0", { writeText })).resolves.toBe("copied");
    expect(writeText).toHaveBeenCalledWith("org.example:demo:jar::1.0.0");
  });

  it("returns blocked when clipboard permission is denied", async () => {
    const writeText = vi.fn().mockRejectedValue(new Error("denied"));

    await expect(copyTextToClipboard("org.example:demo:jar::1.0.0", { writeText })).resolves.toBe("blocked");
  });

  it("returns blocked when the clipboard API is unavailable", async () => {
    await expect(copyTextToClipboard("org.example:demo:jar::1.0.0", undefined)).resolves.toBe("blocked");
  });
});
