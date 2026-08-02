import { resolveMediaUrl } from "../api";

type UserAvatarProps = {
  name: string;
  username?: string;
  avatarUrl?: string | null;
  className?: string;
  size?: number;
};

function fallbackAvatarUrl(seed: string): string {
  return `https://api.dicebear.com/9.x/thumbs/svg?seed=${encodeURIComponent(seed)}`;
}

export function UserAvatar({
  name,
  username,
  avatarUrl,
  className = "user-cell-avatar",
  size,
}: UserAvatarProps) {
  const seed = username || name || "user";
  const resolved = resolveMediaUrl(avatarUrl);
  const initialSrc = resolved || fallbackAvatarUrl(seed);

  return (
    <img
      className={className}
      src={initialSrc}
      alt=""
      width={size}
      height={size}
      referrerPolicy="no-referrer"
      onError={(event) => {
        const img = event.currentTarget;
        const fallback = fallbackAvatarUrl(seed);
        if (!img.src.includes("api.dicebear.com")) {
          img.src = fallback;
        }
      }}
    />
  );
}
