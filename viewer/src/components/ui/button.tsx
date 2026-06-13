import type { ButtonHTMLAttributes } from "react";

type ButtonVariant = "default" | "secondary" | "outline" | "ghost" | "danger";
type ButtonSize = "sm" | "md" | "icon";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

export function Button({ className = "", variant = "default", size = "md", type = "button", ...props }: ButtonProps) {
  return <button type={type} className={["button", `button-${variant}`, `button-${size}`, className].join(" ")} {...props} />;
}
