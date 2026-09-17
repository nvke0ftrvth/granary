import { useState } from 'react';

interface ShareButtonProps {
  recipeId: number;
}

async function copyToClipboard(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

const shareBaseUrl = import.meta.env.VITE_SHARE_BASE_URL ?? window.location.origin;

function ShareIcon() {
  return (
    <svg
      className="share-icon"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <line x1="8.6" y1="13.5" x2="15.4" y2="17.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <line x1="15.4" y1="6.5" x2="8.6" y2="10.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="18" cy="5" r="3" fill="currentColor" />
      <circle cx="6" cy="12" r="3" fill="currentColor" />
      <circle cx="18" cy="19" r="3" fill="currentColor" />
    </svg>
  );
}

export function ShareButton({ recipeId }: ShareButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleShare = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await copyToClipboard(`${shareBaseUrl}/recipes/${recipeId}`);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  };

  return (
    <button
      type="button"
      className={`share-btn ${copied ? 'copied' : ''}`}
      onClick={handleShare}
      aria-label="Copy link to this recipe"
    >
      {copied ? 'Link copied!' : <ShareIcon />}
    </button>
  );
}
