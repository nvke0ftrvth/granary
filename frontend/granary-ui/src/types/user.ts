// Mirrors com.example.granary.dto.UserProfileDto
export interface UserProfileDto {
  username: string;
  description: string | null;
  avatarUrl: string | null;
}

// Must match UserService.MAX_AVATAR_SIZE_BYTES / ALLOWED_AVATAR_TYPES and
// UpdateProfileRequestDto's @Size(max = 500) on the backend
export const MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
export const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
export const MAX_DESCRIPTION_LENGTH = 500;
