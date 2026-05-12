import type { ReactNode } from "react";
import { MaterialIcon } from "./MaterialIcon";

export type AppTab = "dashboard" | "assets" | "trading" | "profile";

export function AppShell({
  active,
  onTab,
  children,
}: {
  active: AppTab;
  onTab: (t: AppTab) => void;
  children: ReactNode;
}) {
  const nav = (
    id: AppTab,
    icon: string,
    label: string,
    filled?: boolean,
  ) => (
    <button
      key={id}
      type="button"
      onClick={() => onTab(id)}
      className={`flex flex-1 flex-col items-center justify-center py-2 transition-transform duration-100 active:scale-95 ${
        active === id
          ? "font-bold text-secondary"
          : "text-on-surface-variant hover:bg-surface-container-lowest"
      }`}
    >
      <MaterialIcon name={icon} className="mb-1" filled={filled && active === id} />
      <span className="font-label-caps text-label-caps">{label}</span>
    </button>
  );

  return (
    <div className="min-h-[100dvh] bg-background pb-[max(5rem,env(safe-area-inset-bottom,0px))] font-body-md text-on-background selection:bg-secondary-container">
      <header className="fixed left-0 right-0 top-0 z-50 flex h-16 w-full items-center justify-between border-b border-outline-variant bg-surface px-margin-mobile">
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 overflow-hidden rounded-full border border-outline-variant bg-surface-container-high" />
          <span className="font-headline-md text-headline-md font-bold text-on-background">FinRisk</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="rounded-lg p-2 text-primary">
            <MaterialIcon name="account_balance_wallet" />
          </span>
        </div>
      </header>

      <div className="pt-16">{children}</div>

      <nav className="fixed bottom-0 z-50 flex h-20 w-full items-center justify-around border-t border-outline-variant bg-surface px-unit pb-[max(0.25rem,env(safe-area-inset-bottom,0px))]">
        {nav("dashboard", "dashboard", "Dashboard", true)}
        {nav("assets", "show_chart", "Assets", true)}
        {nav("trading", "swap_horiz", "Trading", true)}
        {nav("profile", "person", "Profile", true)}
      </nav>
    </div>
  );
}
