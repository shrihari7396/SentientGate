import { useState } from 'react';
import { Check, Copy } from 'lucide-react';
import clsx from 'clsx';

export function CopyableUUID({ uuid, maxChars = 12 }: { uuid: string; maxChars?: number }) {
  const [copied, setCopied] = useState(false);

  const displayId = uuid.length > maxChars ? `${uuid.slice(0, maxChars)}…` : uuid;

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(uuid);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  const openContext = () => window.dispatchEvent(new CustomEvent('open-user-context', { detail: uuid }));

  return (
    <div className="flex items-center gap-2">
      <span 
        onClick={openContext}
        className="font-mono text-text-primary hover:text-teal cursor-pointer transition-colors" 
        title={uuid}
      >
        {displayId}
      </span>
      <button 
        onClick={handleCopy}
        className="text-text-muted hover:text-text-primary transition-colors"
        title="Copy UUID"
      >
        {copied ? <Check size={14} className="text-teal" /> : <Copy size={14} />}
      </button>
    </div>
  );
}
