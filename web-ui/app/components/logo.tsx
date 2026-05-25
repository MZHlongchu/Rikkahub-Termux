import type { ComponentPropsWithoutRef } from "react";

export default function Logo({
  alt = "RikkaHub-Lune",
  ...props
}: ComponentPropsWithoutRef<"img">) {
  return (
    <img
      alt={alt}
      src="/app-icon.png"
      width={256}
      height={256}
      {...props}
    />
  );
}
