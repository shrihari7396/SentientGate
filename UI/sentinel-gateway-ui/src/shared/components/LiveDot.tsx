export function LiveDot({ active }: { active: boolean }) {
  if (!active) return null;

  return (
    <div className="flex items-center gap-1.5">
      <div className="relative flex h-1.5 w-1.5 items-center justify-center">
        <div className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-75"></div>
        <div className="relative inline-flex h-1.5 w-1.5 rounded-full bg-teal"></div>
      </div>
      <span className="text-xs font-mono text-teal">LIVE</span>
    </div>
  );
}
